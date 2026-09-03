package com.shipgame.nanhai.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.shipgame.nanhai.ui.UpdateChecker;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-app update check against GitHub Releases (public repo, no token).
 *
 * Flow: background thread fetches .../releases/latest, compares tag_name with
 * this package's versionName; if newer, shows a system dialog. Confirming
 * downloads the release APK into cacheDir/update with a progress dialog
 * (cancellable), then installs it via FileProvider + ACTION_VIEW. Android 8+
 * needs REQUEST_INSTALL_PACKAGES; the user is guided to the settings page on
 * first use. Any network/parse failure is silent — login is never blocked.
 */
public class AndroidUpdateChecker implements UpdateChecker {

    private static final String LATEST_URL =
            "https://api.github.com/repos/xuhxsummer/NanHaiVoyage/releases/latest";
    private static final int REQ_UNKNOWN_SOURCES = 7001;

    private final Activity activity;
    private final AtomicBoolean checked = new AtomicBoolean(false);
    private volatile boolean downloadCancelled;

    /** Latest release tag ("v0.24.3") and APK asset URL, from the check phase. */
    private volatile String pendingTag;
    private volatile String pendingApkUrl;

    public AndroidUpdateChecker(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void checkForUpdate() {
        if (!checked.compareAndSet(false, true)) {
            return; // only one check per app run
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String json = httpGet(LATEST_URL);
                    if (json == null) {
                        return; // offline / rate-limited / any failure: silent
                    }
                    JSONObject obj = new JSONObject(json);
                    String tag = obj.optString("tag_name", "");
                    String url = findApkAsset(obj);
                    if (tag == null || tag.isEmpty() || url == null) {
                        return;
                    }
                    String local = currentVersionName();
                    if (local == null || !isNewer(tag, local)) {
                        return;
                    }
                    pendingTag = tag;
                    pendingApkUrl = url;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                showUpdateDialog(clean(tag));
                            } catch (Throwable ignored) {
                            }
                        }
                    });
                } catch (Throwable ignored) {
                    // never block login, never crash
                }
            }
        }, "update-check").start();
    }

    /** Called from AndroidLauncher.onActivityResult when returning from the
     * unknown-sources settings page. */
    public void onSettingsResult(int requestCode, int resultCode) {
        if (requestCode == REQ_UNKNOWN_SOURCES && canRequestInstalls()) {
            String tag = pendingTag;
            if (tag != null) {
                startDownload(tag);
            }
        }
    }

    // ------------------------------------------------------------- compare

    /** True if tag "vX.Y.Z..." is strictly newer than local "X.Y.Z...". */
    static boolean isNewer(String tag, String localVersion) {
        int[] a = parse(tag);
        int[] b = parse(localVersion);
        for (int i = 0; i < 3; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i];
            }
        }
        return false;
    }

    /** "v0.24.3" / "0.24.3" -> {0, 24, 3}. Unparsable -> {0, 0, -1} (never newer). */
    private static int[] parse(String v) {
        int[] out = {0, 0, -1};
        if (v == null) {
            return out;
        }
        String t = v.trim();
        if (t.startsWith("v") || t.startsWith("V")) {
            t = t.substring(1);
        }
        int end = t.length();
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (!Character.isDigit(c) && c != '.') {
                end = i;
                break;
            }
        }
        String[] parts = t.substring(0, end).split("\\.");
        try {
            for (int i = 0; i < parts.length && i < 3; i++) {
                out[i] = Integer.parseInt(parts[i].trim());
            }
        } catch (NumberFormatException ignored) {
            out[2] = -1;
        }
        return out;
    }

    private static String clean(String tag) {
        String t = tag.trim();
        return (t.startsWith("v") || t.startsWith("V")) ? t.substring(1) : t;
    }

    // --------------------------------------------------------------- dialog

    private void showUpdateDialog(String ver) {
        new AlertDialog.Builder(activity)
                .setTitle("发现新版本")
                .setMessage("发现新版本 " + ver + "，下载安装？")
                .setPositiveButton("下载安装", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        startDownload(pendingTag);
                    }
                })
                .setNegativeButton("以后再说", null)
                .show();
    }

    // ------------------------------------------------------------- download

    private void startDownload(final String tag) {
        final String url = pendingApkUrl;
        if (tag == null || url == null) {
            return;
        }
        downloadCancelled = false;

        File dir = new File(activity.getCacheDir(), "update");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        final File out = new File(dir, "NanHaiVoyage-update.apk");
        //noinspection ResultOfMethodCallIgnored
        out.delete();

        // Progress dialog with a determinate bar and a cancel button.
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
        panel.setPadding(pad, pad, pad, pad);
        final TextView status = new TextView(activity);
        status.setText("正在下载…");
        final ProgressBar bar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(0);
        panel.addView(status);
        panel.addView(bar);

        final AlertDialog dlg = new AlertDialog.Builder(activity)
                .setTitle("下载更新")
                .setView(panel)
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        downloadCancelled = true;
                    }
                })
                .setCancelable(false)
                .create();
        dlg.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                    c.setConnectTimeout(8000);
                    c.setReadTimeout(15000);
                    c.setRequestProperty("User-Agent", "NanHaiVoyage-app");
                    c.connect();
                    int len = c.getContentLength();
                    InputStream in = c.getInputStream();
                    FileOutputStream fout = new FileOutputStream(out);
                    byte[] buf = new byte[32 * 1024];
                    long total = 0;
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        if (downloadCancelled) {
                            break;
                        }
                        total += n;
                        fout.write(buf, 0, n);
                        if (len > 0) {
                            final int pct = (int) (100 * total / len);
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        bar.setProgress(pct);
                                        status.setText("正在下载… " + pct + "%");
                                    } catch (Throwable ignored) {
                                    }
                                }
                            });
                        }
                    }
                    fout.close();
                    in.close();
                    c.disconnect();

                    final boolean ok = !downloadCancelled && out.length() > 0;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                dlg.dismiss();
                            } catch (Throwable ignored) {
                            }
                            if (ok) {
                                promptInstall(out);
                            } else {
                                //noinspection ResultOfMethodCallIgnored
                                out.delete();
                                Toast.makeText(activity, "下载已取消", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (Throwable e) {
                    //noinspection ResultOfMethodCallIgnored
                    out.delete();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                dlg.dismiss();
                                Toast.makeText(activity, "下载失败", Toast.LENGTH_SHORT).show();
                            } catch (Throwable ignored) {
                            }
                        }
                    });
                }
            }
        }, "update-download").start();
    }

    // -------------------------------------------------------------- install

    private void promptInstall(File apk) {
        try {
            if (!canRequestInstalls()) {
                // Android 8+: guide the user to allow installs from this app.
                new AlertDialog.Builder(activity)
                        .setTitle("需要权限")
                        .setMessage("安装更新需要允许本应用安装未知应用，请在接下来的页面中开启权限后重试。")
                        .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                try {
                                    Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                            Uri.parse("package:" + activity.getPackageName()));
                                    activity.startActivityForResult(i, REQ_UNKNOWN_SOURCES);
                                } catch (Throwable e) {
                                    Toast.makeText(activity, "无法打开设置", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
                return;
            }
            Uri uri = FileProvider.getUriForFile(activity,
                    activity.getPackageName() + ".fileprovider", apk);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, "application/vnd.android.package-archive");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
        } catch (Throwable e) {
            Toast.makeText(activity, "无法启动安装", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean canRequestInstalls() {
        if (Build.VERSION.SDK_INT < 26) {
            return true; // pre-O: REQUEST_INSTALL_PACKAGES not required
        }
        try {
            return activity.getPackageManager().canRequestPackageInstalls();
        } catch (Throwable ignored) {
            return false;
        }
    }

    // ----------------------------------------------------------------- http

    private String httpGet(String url) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(5000);
            c.setReadTimeout(8000);
            c.setRequestProperty("Accept", "application/vnd.github+json");
            c.setRequestProperty("User-Agent", "NanHaiVoyage-app"); // GitHub requires UA
            int code = c.getResponseCode();
            if (code != 200) {
                return null;
            }
            InputStream in = c.getInputStream();
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            in.close();
            return bos.toString("UTF-8");
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (c != null) {
                c.disconnect();
            }
        }
    }

    /** Picks the first release asset whose name ends with ".apk". */
    private static String findApkAsset(JSONObject release) {
        try {
            org.json.JSONArray assets = release.getJSONArray("assets");
            for (int i = 0; i < assets.length(); i++) {
                JSONObject a = assets.getJSONObject(i);
                String name = a.optString("name", "");
                String url = a.optString("browser_download_url", "");
                if (name.toLowerCase().endsWith(".apk") && !url.isEmpty()) {
                    return url;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private String currentVersionName() {
        try {
            PackageInfo pi = activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0);
            return pi.versionName;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
