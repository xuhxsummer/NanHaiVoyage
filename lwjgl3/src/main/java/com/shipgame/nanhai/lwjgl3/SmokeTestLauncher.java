package com.shipgame.nanhai.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.shipgame.nanhai.NanHaiVoyage;
import com.shipgame.nanhai.screen.LoginScreen;
import com.shipgame.nanhai.screen.VoyageScreen;

import java.lang.reflect.Field;
import java.util.Iterator;

/**
 * Headless-friendly smoke test: replays the Android repro by firing real
 * scene2d input events (keyTyped into the focused TextField, touchDown/up on
 * the Register / Login buttons) inside the render loop. Exits 0 on success.
 */
public class SmokeTestLauncher {

    private static int frame = 0;
    private static int exitCode = 97;
    private static Stage stage;
    private static TextField userField;
    private static TextField passField;
    private static Actor loginBtn;
    private static Actor regBtn;

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("nanhai-smoke");
        config.setWindowedMode(1280, 720);
        config.setForegroundFPS(60);
        Lwjgl3Application app = new Lwjgl3Application(new NanHaiVoyage() {
            @Override
            public void render() {
                super.render();
                frame++;
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
                }
                if (frame == 60) {
                    Gdx.input.setInputProcessor(stage); // focus the user field
                    type(userField, "boxer");
                    stage.setKeyboardFocus(passField);
                    type(passField, "pw123456");
                    System.out.println("SMOKE: typed user/pass");
                }
                if (frame == 90) {
                    System.out.println("SMOKE: TAP REGISTER");
                    click(regBtn);
                }
                if (frame == 130) {
                    System.out.println("SMOKE: TAP LOGIN");
                    click(loginBtn);
                }
                if (frame == 180) {
                    if (getScreen() instanceof VoyageScreen) {
                        System.out.println("SMOKE: PASS - reached VoyageScreen");
                        exitCode = 0;
                    } else {
                        System.err.println("SMOKE: FAIL - still on " + getScreen());
                        exitCode = 4;
                    }
                    Gdx.app.exit();
                }
            }

            private void snapshotStage() throws Exception {
                Field f = LoginScreen.class.getDeclaredField("stage");
                f.setAccessible(true);
                stage = (Stage) f.get(getScreen());
                Group root = (Group) stage.getActors().first();
                userField = findTextField(root);
                // second TextField is the password field
                for (Actor a : root.getChildren()) {
                    collect(a);
                }
            }

            private void collect(Actor a) {
                if (a instanceof TextField) {
                    if (userField == null) userField = (TextField) a;
                    else if (passField == null) passField = (TextField) a;
                } else if (a instanceof Group) {
                    for (Actor c : ((Group) a).getChildren()) {
                        collect(c);
                    }
                }
            }

            private TextField findTextField(Group g) {
                for (Actor a : g.getChildren()) {
                    if (a instanceof TextField) return (TextField) a;
                    if (a instanceof Group) {
                        TextField t = findTextField((Group) a);
                        if (t != null) return t;
                    }
                }
                return null;
            }

            private void click(Actor actor) {
                // find a ClickListener and call clicked() directly (same code path
                // as a real tap once hit-testing has selected the actor)
                for (Iterator<com.badlogic.gdx.scenes.scene2d.EventListener> it =
                     actor.getListeners().iterator(); it.hasNext(); ) {
                    com.badlogic.gdx.scenes.scene2d.EventListener l = it.next();
                    if (l instanceof ClickListener) {
                        com.badlogic.gdx.scenes.scene2d.InputEvent ev =
                                new com.badlogic.gdx.scenes.scene2d.InputEvent();
                        ev.setType(com.badlogic.gdx.scenes.scene2d.InputEvent.Type.touchDown);
                        ev.setStage(stage);
                        ev.setListenerActor(actor);
                        ev.setTarget(actor);
                        ((ClickListener) l).clicked(ev, 0, 0);
                        return;
                    }
                }
                throw new IllegalStateException("no ClickListener on " + actor);
            }

            private void type(TextField field, String s) {
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
