package com.shipgame.nanhai.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.shipgame.nanhai.NanHaiVoyage;
import com.shipgame.nanhai.screen.LoginScreen;
import com.shipgame.nanhai.screen.VoyageScreen;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Headless-friendly smoke test: replays the Android repro by firing real
 * scene2d input events (keyTyped into the focused TextField, touchDown/up on
 * the Register / Login buttons) inside the render loop. Exits 0 on success.
 *
 * Mode (arg 0):
 *   register (default) - creates account "boxer"/"pw123456", then must reach VoyageScreen.
 *   login             - logs "boxer"/"pw123456" in (account must already exist), then must
 *                       reach VoyageScreen. Run register first so the account exists.
 */
public class SmokeTestLauncher {

    private static final String USER = "boxer";
    private static final String PASS = "pw123456";

    private static int frame = 0;
    private static int exitCode = 97;
    private static String mode = "register";
    private static Stage stage;
    private static TextField userField;
    private static TextField passField;
    private static TextButton loginBtn;
    private static TextButton regBtn;

    public static void main(String[] args) {
        if (args.length > 0) {
            mode = args[0];
        }
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("nanhai-smoke");
        config.setWindowedMode(1280, 720);
        config.setForegroundFPS(60);
        Lwjgl3Application app = new Lwjgl3Application(new NanHaiVoyage() {
            @Override
            public void render() {
                super.render();
                frame++;
                if (frame % 5 == 0 && getScreen() instanceof VoyageScreen) {
                    System.err.println("SMOKE: voyage frame " + frame);
                }
                try {
                    drive();
                } catch (Throwable t) {
                    System.err.println("SMOKE: EXCEPTION during drive():");
                    t.printStackTrace();
                    exitCode = 2;
                    Gdx.app.exit();
                }
                if (frame > 1800) {
                    System.err.println("SMOKE: TIMEOUT - flow never completed");
                    exitCode = 3;
                    Gdx.app.exit();
                }
            }

            private void drive() throws Exception {
                if (frame == 40) {
                    if (!(getScreen() instanceof LoginScreen)) {
                        throw new IllegalStateException("expected LoginScreen, got " + getScreen());
                    }
                    snapshotStage();
                    System.out.println("SMOKE: stage captured, user=" + (userField != null)
                            + " pass=" + (passField != null)
                            + " loginBtn=" + (loginBtn != null)
                            + " regBtn=" + (regBtn != null));
                    if (userField == null || passField == null || loginBtn == null || regBtn == null) {
                        throw new IllegalStateException("login widgets not all found");
                    }
                }
                if (frame == 60) {
                    Gdx.input.setInputProcessor(stage); // ensure stage handles typed keys
                    type(userField, USER);
                    type(passField, PASS);
                    System.out.println("SMOKE: typed user/pass (" + USER + "/" + PASS + ")");
                }
                if (frame == 90) {
                    if (mode.equals("register")) {
                        System.out.println("SMOKE: TAP REGISTER");
                        click(regBtn);
                    } else {
                        System.out.println("SMOKE: TAP LOGIN");
                        click(loginBtn);
                    }
                }
                if (frame == 180) {
                    if (getScreen() instanceof VoyageScreen) {
                        System.out.println("SMOKE: PASS - reached VoyageScreen (" + mode + ")");
                        exitCode = 0;
                    } else {
                        System.err.println("SMOKE: FAIL - still on " + getScreen());
                        exitCode = 4;
                    }
                    Gdx.app.exit();
                }
            }

            /** Walks the whole login tree: 2 TextFields + the 登录/注册 TextButtons. */
            private void snapshotStage() throws Exception {
                Field f = LoginScreen.class.getDeclaredField("stage");
                f.setAccessible(true);
                stage = (Stage) f.get(getScreen());
                List<TextField> tfs = new ArrayList<>();
                collect(stage.getRoot(), tfs);
                if (tfs.size() >= 1) userField = tfs.get(0);
                if (tfs.size() >= 2) passField = tfs.get(1);
                loginBtn = findButton(stage.getRoot(), "登录");
                regBtn = findButton(stage.getRoot(), "注册");
            }

            private void collect(Actor a, List<TextField> out) {
                if (a instanceof TextField) {
                    out.add((TextField) a);
                } else if (a instanceof Group) {
                    for (Actor c : ((Group) a).getChildren()) {
                        collect(c, out);
                    }
                }
            }

            private TextButton findButton(Actor a, String text) {
                if (a instanceof TextButton) {
                    TextButton b = (TextButton) a;
                    if (text.equals(b.getText().toString())) {
                        return b;
                    }
                }
                if (a instanceof Group) {
                    for (Actor c : ((Group) a).getChildren()) {
                        TextButton b = findButton(c, text);
                        if (b != null) return b;
                    }
                }
                return null;
            }

            private void click(TextButton button) {
                // call the LAST ClickListener's clicked() directly: Button adds its
                // internal toggle listener in its constructor, and LoginScreen adds
                // the doLogin/doRegister listener afterwards, so the app handler is
                // the last ClickListener on the actor.
                ClickListener last = null;
                for (Iterator<com.badlogic.gdx.scenes.scene2d.EventListener> it =
                     button.getListeners().iterator(); it.hasNext(); ) {
                    com.badlogic.gdx.scenes.scene2d.EventListener l = it.next();
                    if (l instanceof ClickListener) {
                        last = (ClickListener) l;
                    }
                }
                if (last == null) {
                    throw new IllegalStateException("no ClickListener on " + button);
                }
                com.badlogic.gdx.scenes.scene2d.InputEvent ev =
                        new com.badlogic.gdx.scenes.scene2d.InputEvent();
                ev.setType(com.badlogic.gdx.scenes.scene2d.InputEvent.Type.touchDown);
                ev.setStage(stage);
                ev.setListenerActor(button);
                ev.setTarget(button);
                last.clicked(ev, 0, 0);
            }

            private void type(TextField field, String s) {
                field.setText(""); // fields are prefilled "summer"; clear before typing
                stage.setKeyboardFocus(field);
                for (char c : s.toCharArray()) {
                    Gdx.input.getInputProcessor().keyTyped(c);
                }
            }
        }, config);
        System.out.println("SMOKE: app exited with code " + exitCode);
        System.exit(exitCode);
    }
}
