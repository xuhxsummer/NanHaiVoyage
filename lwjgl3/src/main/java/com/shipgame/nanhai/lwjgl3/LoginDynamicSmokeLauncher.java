package com.shipgame.nanhai.lwjgl3;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.shipgame.nanhai.NanHaiVoyage;
import com.shipgame.nanhai.data.AccountStore;
import com.shipgame.nanhai.screen.LoginScreen;
import com.shipgame.nanhai.screen.VoyageScreen;
import com.shipgame.nanhai.ui.LoginHarbor;
import com.shipgame.nanhai.ui.UpdateChecker;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Path;

/** Real GL, Scene2D input, resource reload and account round-trip; all saves are isolated. */
public final class LoginDynamicSmokeLauncher {
    private static int result = 1;

    public static void main(String[] args) throws Exception {
        Path temp = java.nio.file.Files.createTempDirectory("nanhai-login-dynamic-");
        Path output = Path.of("../Builds/login2814").toAbsolutePath();
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(1280, 720);
        config.disableAudio(true);
        config.setPreferencesConfig(temp.resolve("prefs").toString(), Files.FileType.Absolute);
        new Lwjgl3Application(new NanHaiVoyage() {
            final FakeUpdates updates = new FakeUpdates();
            Stage stage;
            LoginScreen login;
            int step;
            int transitionFrames;
            boolean failed;

            @Override public void create() {
                Files original = Gdx.files;
                Gdx.files = (Files) Proxy.newProxyInstance(Files.class.getClassLoader(), new Class[]{Files.class}, (proxy, method, a) -> {
                    if (method.getName().equals("local")) return new FileHandle(temp.resolve((String) a[0]).toFile());
                    return method.invoke(original, a);
                });
                try {
                    updateChecker = updates;
                    super.create();
                    refreshLogin();
                    artwork();
                    helperPanels();
                    updatePanels();
                    fields("", ""); tap("注册");
                    require(message().contains("请输入"), "empty registration validation");
                    fields("login-smoke", "bad"); tap("登录");
                    require(message().contains("用户名或密码不对"), "missing account validation");
                    fields("login-smoke", "local-password"); tap("注册");
                    require(getScreen() == login, "registration switch must be deferred");
                } catch (Throwable t) { fail(t); }
            }

            @Override public void render() {
                if (failed) return;
                try {
                    super.render();
                    // LWJGL dispatches create()'s postRunnable after the first render.
                    if (getScreen() instanceof LoginScreen && ++transitionFrames < 120) return;
                    if (step == 0) {
                        require(getScreen() instanceof VoyageScreen, "registration enters voyage");
                        require(accounts.load("login-smoke") != null, "registration saved a new game");
                        require(!oldHarbor.hasSeaShader(), "hidden login releases shader");
                        require(Gdx.input.getInputProcessor() != stage, "voyage owns input");
                        java.lang.reflect.Method quests = VoyageScreen.class.getDeclaredMethod("toggleQuestOverlay");
                        quests.setAccessible(true); quests.invoke(getScreen());
                        getScreen().render(0);
                        Field voyageStage = VoyageScreen.class.getDeclaredField("stage"); voyageStage.setAccessible(true);
                        Label story = ((Stage) voyageStage.get(getScreen())).getRoot().findActor("questStory");
                        require(story != null && story.getText().toString().contains("武周"), "authored story appears in quest details");
                        capture("quest-story").dispose();
                        state.silver = 4321;
                        accounts.save(currentUser, state.toSave());
                        // Reopen the same screen: exercises hide/show and disposal twice.
                        setScreen(login);
                        refreshLogin();
                        login.render(0);
                        fields("login-smoke", "local-password"); tap("注册");
                        require(message().contains("账号已存在"), "duplicate account validation");
                        fields("login-smoke", "wrong"); tap("登录");
                        require(message().contains("用户名或密码不对"), "wrong password validation");
                        accounts = new AccountStore(); // Reload persisted credentials and save.
                        fields("login-smoke", "local-password"); tap("登录");
                        require(getScreen() == login, "login switch must be deferred");
                        step++;
                        transitionFrames = 0;
                    } else {
                        require(getScreen() instanceof VoyageScreen, "existing account enters voyage");
                        require("login-smoke".equals(currentUser) && state.silver == 4321, "existing save restored");
                        require(Gdx.gl.glGetError() == GL20.GL_NO_ERROR, "clean GL after both transitions");
                        login.resize(1600, 720); login.hide(); login.dispose();
                        System.out.println("LOGIN DYNAMIC PASS: animated sea/cloth, still sky/UI, pause, wide/tall resize, shader fallback, managed reload, hide/show/dispose, helpers/settings, update callbacks/cancel/resize, input validation, register/login and persisted save");
                        result = 0;
                        Gdx.app.exit();
                    }
                } catch (Throwable t) { fail(t); }
            }

            LoginHarbor oldHarbor;
            private void artwork() throws Exception {
                Pixmap mask = new Pixmap(Gdx.files.internal("textures/login/sea-mask.png"));
                require(mask.getFormat() == Pixmap.Format.Alpha, "grayscale mask uses the alpha channel");
                mask.dispose();
                LoginHarbor harbor = stage.getRoot().findActor("loginHarbor");
                oldHarbor = harbor;
                require(harbor.hasSeaShader(), "sea GLSL must compile");
                stage.setKeyboardFocus(null);
                login.render(0);
                Pixmap before = capture("still");
                // Observe a few seconds, including both extremes: subpixel shimmer passed
                // the old changed-pixel check but was too subtle on the user's phone.
                float[] min = new float[6], max = new float[6];
                java.util.Arrays.fill(min, Float.POSITIVE_INFINITY);
                java.util.Arrays.fill(max, Float.NEGATIVE_INFINITY);
                for (int i = 0; i < 72; i++) {
                    login.render(1f / 15);
                    if (i == 0 || i == 35 || i == 71) capture(String.format("motion-%03d", i)).dispose();
                    for (int c = 0; c < 6; c++) {
                        Actor cloth = harbor.getChildren().get(c + 1);
                        boolean sail = cloth.getName().startsWith("sail");
                        Vector2 tip = cloth.localToStageCoordinates(new Vector2(
                                sail ? cloth.getWidth() / 2 : cloth.getWidth(),
                                sail ? cloth.getHeight() : cloth.getHeight() / 2));
                        stage.stageToScreenCoordinates(tip);
                        float position = sail ? tip.x : tip.y;
                        min[c] = Math.min(min[c], position); max[c] = Math.max(max[c], position);
                    }
                }
                for (int c = 0; c < 6; c++) {
                    String name = harbor.getChildren().get(c + 1).getName();
                    System.out.println("LOGIN cloth travel: " + name + "=" + (max[c] - min[c]) + "px");
                    require(max[c] - min[c] > 8, "readable wind travel for " + name);
                }
                Pixmap after = capture("wind-water");
                try {
                    int sea = different(before, after, 930, 145, 1160, 200);
                    int cloth = different(before, after, 1010, 370, 1180, 650);
                    System.out.println("LOGIN pixels changed: sea=" + sea + " cloth=" + cloth);
                    require(sea > 500, "visible moving water");
                    require(cloth > 500, "visible wind in sails and flags");
                    require(different(before, after, 300, 520, 800, 690) == 0, "sky and title remain still");
                    require(different(before, after, 370, 260, 900, 380) == 0, "fields remain still under the UI shader");
                    login.render(0);
                    Pixmap paused = capture("paused");
                    try { require(different(after, paused, 0, 0, 1280, 720) == 0, "zero delta pauses animation"); }
                    finally { paused.dispose(); }
                } finally { before.dispose(); after.dispose(); }

                Texture.invalidateAllTextures(Gdx.app);
                Mesh.invalidateAllMeshes(Gdx.app);
                ShaderProgram.invalidateAllShaderPrograms(Gdx.app);
                login.render(0); capture("reloaded").dispose();
                for (int[] size : new int[][]{{1600, 720}, {960, 720}, {1280, 720}}) {
                    login.resize(size[0], size[1]);
                    require(Math.abs(harbor.getWidth() * harbor.getScaleX() - stage.getViewport().getWorldWidth()) < .01f,
                            "art covers resized viewport width");
                    require(Math.abs(harbor.getHeight() * harbor.getScaleY() - stage.getViewport().getWorldHeight()) < .01f,
                            "art covers resized viewport height");
                    login.render(.016f);
                    tap("公告"); require(stage.getRoot().findActor("loginModal") != null, "helper opens after resize");
                    tap("关闭");
                }
                // Exercise unsupported-shader behavior without altering the production API.
                Field shader = LoginHarbor.class.getDeclaredField("seaShader"); shader.setAccessible(true);
                ((ShaderProgram) shader.get(harbor)).dispose(); shader.set(harbor, null);
                login.render(.1f); capture("fallback").dispose();
                login.hide(); login.resize(1280, 720); login.dispose(); login.show();
                refreshLogin();
                oldHarbor = stage.getRoot().findActor("loginHarbor");
                require(oldHarbor.hasSeaShader(), "show rebuilds disposed resources");
                login.render(0);
            }

            private void helperPanels() throws Exception {
                for (String name : new String[]{"公告", "客服", "设置"}) {
                    tap(name); login.render(0); capture("helper-" + name).dispose();
                    require(stage.getRoot().findActor("loginModal") != null, "helper panel: " + name);
                    // Taps outside the modal cannot reach the underlying account form.
                    tap("登录"); require(currentUser == null, "modal blocks login click-through");
                    tap("关闭");
                    require(stage.getRoot().findActor("loginModal") == null, "helper closes");
                }
                tap("设置"); tap("登录动效：开启"); tap("关闭");
                stage.setKeyboardFocus(null); login.render(0);
                Pixmap still = capture("motion-off");
                for (int i = 0; i < 8; i++) login.render(.1f);
                Pixmap later = capture("motion-off-later");
                // Exclude the helper row (pressed feedback) and the bottom version-gate
                // strip (0.28.19: gate bar keeps finishing its 90→100 fill by design).
                try { require(different(still, later, 0, 0, 1280, 580) == 0, "motion setting actually pauses rendering"); }
                finally { still.dispose(); later.dispose(); }
                tap("设置"); tap("海水波纹：开启"); tap("关闭");
                login.hide(); login.show(); refreshLogin();
                tap("设置");
                require(findButton(stage.getRoot(), "登录动效：关闭") != null, "motion preference persists across rebuild");
                require(findButton(stage.getRoot(), "海水波纹：关闭") != null, "water preference persists across rebuild");
                tap("登录动效：关闭"); tap("海水波纹：关闭"); tap("关闭");
                oldHarbor = stage.getRoot().findActor("loginHarbor");
            }

            private void updatePanels() throws Exception {
                updates.offer(); login.render(0); capture("update-offer").dispose();
                require(stage.getRoot().findActor("loginModal") != null, "update prompt");
                tap("稍后再说"); require(updates.declined == 1, "decline callback");
                updates.offer(); tap("下载更新"); login.render(.1f); capture("download-4").dispose();
                ProgressBar bar = stage.getRoot().findActor("updateProgress");
                require(bar != null && bar.getValue() == 4, "synchronous first progress is retained");
                updates.listener.onDownloadProgress(66); login.render(.1f); capture("download-66").dispose();
                require(bar.getValue() == 66, "download progress updates");
                login.resize(1600, 720);
                Actor panel = stage.getRoot().findActor("loginModal");
                require(Math.abs(panel.getX() + panel.getWidth() / 2 - stage.getWidth() / 2) < 1, "modal centers after resize");
                login.resize(1280, 720);
                stage.keyDown(Input.Keys.BACK);
                require(updates.cancelled == 1 && stage.getRoot().findActor("loginModal") == null, "back cancels download");
                tap("客服"); updates.listener.onDownloadProgress(99); updates.listener.onDownloadFinished(false, "cancelled");
                require(stage.getRoot().findActor("loginModal") != null, "late cancelled callback cannot close helper");
                tap("关闭");
                updates.offer(); tap("下载更新"); updates.listener.onDownloadFinished(false, "下载失败");
                require(message().contains("下载失败"), "download error leaves usable login");
                updates.offer(); tap("下载更新"); updates.listener.onDownloadProgress(100);
                updates.listener.onDownloadFinished(true, "");
                require(stage.getRoot().findActor("loginModal") == null && message().contains("安装"), "success hands off to installer");
                UpdateChecker.Listener obsolete = updates.listener;
                login.hide(); require(updates.listener == null, "hide detaches update listener");
                login.show(); refreshLogin();
                obsolete.onUpdateAvailable("old", () -> {}, () -> {});
                require(stage.getRoot().findActor("loginModal") == null, "old callbacks cannot reopen a rebuilt login");
                oldHarbor = stage.getRoot().findActor("loginHarbor");
            }

            private void refreshLogin() throws Exception {
                login = (LoginScreen) getScreen();
                Field field = LoginScreen.class.getDeclaredField("stage"); field.setAccessible(true);
                stage = (Stage) field.get(login);
            }

            private void fields(String user, String pass) {
                ((TextField) stage.getRoot().findActor("loginUsername")).setText(user);
                ((TextField) stage.getRoot().findActor("loginPassword")).setText(pass);
            }

            private String message() throws Exception {
                Field field = LoginScreen.class.getDeclaredField("msg"); field.setAccessible(true);
                return ((Label) field.get(login)).getText().toString();
            }

            private void tap(String text) {
                TextButton button = findButton(stage.getRoot(), text);
                require(button != null, "button exists: " + text);
                Vector2 point = button.localToStageCoordinates(new Vector2(button.getWidth() / 2, button.getHeight() / 2));
                stage.stageToScreenCoordinates(point);
                require(Gdx.input.getInputProcessor() == stage, "login owns input");
                stage.touchDown(Math.round(point.x), Math.round(point.y), 0, 0);
                stage.touchUp(Math.round(point.x), Math.round(point.y), 0, 0);
            }

            private Pixmap capture(String name) {
                require(Gdx.gl.glGetError() == GL20.GL_NO_ERROR, "GL error at " + name);
                Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, 1280, 720);
                PixmapIO.writePNG(new FileHandle(output.resolve(name + ".png").toFile()), pixmap, -1, true);
                return pixmap;
            }

            private void fail(Throwable t) { t.printStackTrace(); failed = true; result = 1; Gdx.app.exit(); }
        }, config);
        System.exit(result);
    }

    private static final class FakeUpdates implements UpdateChecker {
        Listener listener;
        int declined, cancelled, checks;
        @Override public void setListener(Listener listener) { this.listener = listener; }
        // 0.28.19 contract: every check terminates with onCheckFinished(updateAvailable).
        @Override public void checkForUpdate() { checks++; if (listener != null) listener.onCheckFinished(false); }
        @Override public void cancelDownload() { cancelled++; }
        void offer() {
            listener.onUpdateAvailable("0.28.14", () -> listener.onDownloadProgress(4), () -> declined++);
        }
    }

    private static TextButton findButton(Group group, String text) {
        for (Actor child : group.getChildren()) {
            if (child instanceof TextButton && text.equals(((TextButton) child).getText().toString())) return (TextButton) child;
            if (child instanceof Group) {
                TextButton found = findButton((Group) child, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static int different(Pixmap a, Pixmap b, int x0, int y0, int x1, int y1) {
        int changed = 0;
        for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) {
            int p = a.getPixel(x, y), q = b.getPixel(x, y);
            int delta = Math.abs((p >>> 24) - (q >>> 24)) + Math.abs(((p >>> 16) & 255) - ((q >>> 16) & 255))
                    + Math.abs(((p >>> 8) & 255) - ((q >>> 8) & 255));
            if (delta > 6) changed++;
        }
        return changed;
    }

    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
