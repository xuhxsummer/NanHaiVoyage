package com.shipgame.nanhai.android;

import android.content.Intent;
import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.shipgame.nanhai.NanHaiVoyage;

/** Android entry. APK build needs the Android SDK (see README.md). */
public class AndroidLauncher extends AndroidApplication {

    private AndroidUpdateChecker updateChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 0.28.22: 游戏内不熄屏 —— 整个应用会话保持屏幕常亮（登录/航行/更新下载
        // 都不会因闲置熄屏中断；与其他游戏的做法一致）。
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getWindow().addFlags(
                        android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useImmersiveMode = true;
        NanHaiVoyage game = new NanHaiVoyage();
        updateChecker = new AndroidUpdateChecker(this);
        game.updateChecker = updateChecker;
        initialize(game, config);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (updateChecker != null) {
            updateChecker.onSettingsResult(requestCode, resultCode);
        }
    }
}
