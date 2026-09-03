package com.shipgame.nanhai.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.shipgame.nanhai.NanHaiVoyage;
import com.shipgame.nanhai.data.Catalog;
import com.shipgame.nanhai.data.GameState;
import com.shipgame.nanhai.screen.LoginScreen;
import com.shipgame.nanhai.screen.VoyageScreen;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Desktop smoke test that drives the real UI through the input pipeline.
 *
 * Flow (mode "login", run after a "register" pass so the account exists):
 *   docked at 广州 (port popup auto-opens) ->
 *     1. tap the port popup's 关闭 -> popup closes and stays closed
 *     2. tap 货物 (right-center) -> cargo popup opens, contains tabs; tap its 关闭
 *     3. tap 图鉴 -> codex popup opens; tap its 关闭
 *     4. tap 港口 -> port popup; tap 离港 -> sailing
 *     5. drag the virtual joystick -> steerInput reacts; press/release 加速
 *     6. tap the round minimap -> full map opens
 *     7. tap a far port (合浦) -> auto-sail target set, map closed
 *   Exits 0 on success, non-zero with a reason otherwise.
 */
public class SmokeTestLauncher {

    private static final String USER = "boxer";
    private static final String PASS = "pw123456";
    private static final int HUD_H = 720;
    private static final int STICK_X = 165, STICK_Y = HUD_H - 205;   // screen (y-down)
    private static final int MM_X = 1174, MM_Y = HUD_H - 616;        // round minimap center
    private static final int TARGET_PORT = 5;                        // 合浦 (not 广州)

    private static int frame;
    private static int exitCode = 97;
    private static String mode = "register";
    private static Stage stage;
    private static boolean flowDone;
    private static String failReason;
    private static int nextStepFrame;
    private static int step;

    public static void main(String[] args) {
        if (args.length > 0) {
            mode = args[0];
        }
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("nanhai-smoke");
        config.setWindowedMode(1280, 720);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new NanHaiVoyage() {
            @Override
            public void render() {
                super.render();
                frame++;
                if (!flowDone && frame % 2 == 0) {
                    try {
                        drive();
                    } catch (Throwable t) {
                        System.err.println("SMOKE: EXCEPTION during drive():");
                        t.printStackTrace();
                        exitCode = 2;
                        Gdx.app.exit();
                    }
                }
                if (frame > 1200) {
                    System.err.println("SMOKE: TIMEOUT - flow never completed" + (failReason != null ? " (" + failReason + ")" : ""));
                    exitCode = 3;
                    Gdx.app.exit();
                }
            }

            private void drive() throws Exception {
                if (step == 0) {
                    if (getScreen() instanceof LoginScreen) {
                        loginScreenSetup();
                        step = 1;
                        nextStepFrame = frame + 8;
                    }
                    return;
                }
                if (frame < nextStepFrame) {
                    return;
                }
                switch (step) {
                    case 1: // typing credentials
                        step = 2;
                        nextStepFrame = frame + 20;
                        break;
                    case 2: // tap 注册 (register mode) / 登录 (login mode)
                        String target = mode.equals("register") ? "注册" : "登录";
                        require(tapButton(target), "could not tap " + target);
                        System.out.println("SMOKE: tapped " + target);
                        step = 3;
                        nextStepFrame = frame + 25;
                        break;
                    case 3: // should now be VoyageScreen docked at 广州 with port popup open
                        if (!(getScreen() instanceof VoyageScreen)) {
                            dumpLabels();
                        }
                        require(getScreen() instanceof VoyageScreen, "expected VoyageScreen, got " + getScreen());
                        refreshStageFrom(getScreen().getClass());
                        GameState st = voyageState();
                        require(st.dockedPort == 0, "expected docked at 广州, dockedPort=" + st.dockedPort);
                        Enum<?> ov = voyageOverlay();
                        System.out.println("SMOKE: overlay=" + ov + " 关闭-count=" + countText("关闭"));
                        dumpButtons();
                        require(countText("关闭") >= 1, "port popup 关闭 not found");
                        System.out.println("SMOKE: docked at 广州, port popup open");
                        step = 4;
                        nextStepFrame = frame + 6;
                        break;
                    case 4: // close the port popup
                        require(tapButton("关闭"), "could not tap port popup 关闭");
                        step = 5;
                        nextStepFrame = frame + 10;
                        break;
                    case 5:
                        require(countText("关闭") == 0, "port popup did not close (关闭 still present)");
                        require(voyageOverlay() == null ? false : voyageOverlay().name().equals("NONE"),
                                "overlay should be NONE after closing, got " + voyageOverlay());
                        System.out.println("SMOKE: port popup closed, stays closed");
                        step = 6;
                        nextStepFrame = frame + 6;
                        break;
                    case 6: // open cargo popup
                        require(tapButton("货物"), "could not tap 货物");
                        step = 7;
                        nextStepFrame = frame + 10;
                        break;
                    case 7:
                        require(voyageOverlay() != null && voyageOverlay().name().equals("CARGO"), "cargo popup not open, got " + voyageOverlay());
                        int closes = countText("关闭");
                        if (closes != 1) dumpButtons();
                        require(closes == 1, "cargo popup should have exactly one 关闭, got " + closes);
                        require(findText("商货") != null || findText("[商货]") != null, "cargo tabs missing");
                        System.out.println("SMOKE: cargo popup open with tabs");
                        require(tapButton("关闭"), "could not tap cargo 关闭");
                        step = 8;
                        nextStepFrame = frame + 10;
                        break;
                    case 8:
                        require(countText("关闭") == 0, "cargo popup did not close");
                        step = 9;
                        nextStepFrame = frame + 6;
                        break;
                    case 9: // open codex popup
                        require(tapButton("图鉴"), "could not tap 图鉴");
                        step = 10;
                        nextStepFrame = frame + 10;
                        break;
                    case 10:
                        require(voyageOverlay() != null && voyageOverlay().name().equals("CODEX"), "codex popup not open, got " + voyageOverlay());
                        require(findText("异兽") != null, "codex section 异兽 missing");
                        require(tapButton("关闭"), "could not tap codex 关闭");
                        step = 11;
                        nextStepFrame = frame + 10;
                        break;
                    case 11:
                        require(countText("关闭") == 0, "codex popup did not close");
                        System.out.println("SMOKE: cargo/codex open+close OK");
                        step = 12;
                        nextStepFrame = frame + 6;
                        break;
                    case 12: // reopen port popup and sail out
                        require(tapButton("港口"), "could not tap 港口");
                        step = 13;
                        nextStepFrame = frame + 10;
                        break;
                    case 13:
                        require(voyageOverlay() != null && voyageOverlay().name().equals("PORT"), "port popup not reopened, got " + voyageOverlay());
                        require(tapButton("离港"), "could not tap 离港");
                        step = 14;
                        nextStepFrame = frame + 15;
                        break;
                    case 14:
                        GameState st2 = voyageState();
                        require(st2.dockedPort == -1, "should be at sea after 离港, dockedPort=" + st2.dockedPort);
                        System.out.println("SMOKE: sailing (left 广州)");
                        step = 15;
                        nextStepFrame = frame + 6;
                        break;
                    case 15: // joystick drag reacts
                        dragJoystick();
                        step = 16;
                        nextStepFrame = frame + 4;
                        break;
                    case 16:
                        require(Math.abs(voyageState().steerInput) > 0.05f, "joystick drag did not steer (steerInput=" + voyageState().steerInput + ")");
                        System.out.println("SMOKE: joystick steering works");
                        step = 17;
                        nextStepFrame = frame + 4;
                        break;
                    case 17: // accel button works
                        pressAccel();
                        step = 18;
                        nextStepFrame = frame + 4;
                        break;
                    case 18:
                        require(voyageState().holdAccel, "加速 did not set holdAccel");
                        releaseAccel();
                        System.out.println("SMOKE: 加速 button works");
                        step = 19;
                        nextStepFrame = frame + 6;
                        break;
                    case 19: // open full map via round minimap
                        tapScreen(MM_X, MM_Y);
                        step = 20;
                        nextStepFrame = frame + 8;
                        break;
                    case 20:
                        require(voyageOverlay() != null && voyageOverlay().name().equals("MAP"), "full map not open, got " + voyageOverlay());
                        System.out.println("SMOKE: full map open");
                        step = 21;
                        nextStepFrame = frame + 6;
                        break;
                    case 21: // tap a far port on the full map
                        float[] xy = mapPortToScreen(TARGET_PORT);
                        tapScreen((long) xy[0], (long) xy[1]);
                        step = 22;
                        nextStepFrame = frame + 8;
                        break;
                    case 22:
                        GameState st3 = voyageState();
                        require(st3.autoSail && st3.autoSailPort == TARGET_PORT,
                                "auto-sail not set for port " + TARGET_PORT + " (autoSail=" + st3.autoSail
                                        + " port=" + st3.autoSailPort + ")");
                        require(voyageOverlay() == null || voyageOverlay().name().equals("NONE"),
                                "full map should be closed after target tap, got " + voyageOverlay());
                        System.out.println("SMOKE: PASS - auto-sail to port " + Catalog.PORTS[TARGET_PORT] + " set");
                        exitCode = 0;
                        flowDone = true;
                        Gdx.app.exit();
                        break;
                    default:
                        break;
                }
            }

            private void require(boolean cond, String why) throws Exception {
                if (!cond) {
                    failReason = why;
                    System.err.println("SMOKE: FAIL - " + why);
                    exitCode = 4;
                    Gdx.app.exit();
                    throw new IllegalStateException(why);
                }
            }

            /** Re-grab the current screen's stage (screen switches create a new stage). */
            private void refreshStageFrom(Class<?> screenClass) throws Exception {
                Field f = screenClass.getDeclaredField("stage");
                f.setAccessible(true);
                stage = (Stage) f.get(getScreen());
            }

            private void loginScreenSetup() throws Exception {
                Field f = LoginScreen.class.getDeclaredField("stage");
                f.setAccessible(true);
                stage = (Stage) f.get(getScreen());
                List<TextField> tfs = new ArrayList<>();
                collect(stage.getRoot(), tfs, TextField.class);
                TextField user = tfs.get(0);
                TextField pass = tfs.get(1);
                type(user, USER);
                type(pass, PASS);
                System.out.println("SMOKE: typed " + USER + "/" + PASS);
            }

            private void type(TextField field, String s) {
                field.setText("");
                stage.setKeyboardFocus(field);
                for (char c : s.toCharArray()) {
                    Gdx.input.getInputProcessor().keyTyped(c);
                }
            }

            private boolean tapButton(String text) throws Exception {
                Actor a = findExactButton(text);
                if (a == null) {
                    return false;
                }
                Vector2 c = center(a);
                tapScreen((long) c.x, (long) (HUD_H - c.y)); // stage y-up -> screen y-down
                return true;
            }

            private Actor findExactButton(String text) throws Exception {
                for (Actor a : stage.getRoot().getChildren()) {
                    Actor hit = findExactButtonIn(text, a);
                    if (hit != null) return hit;
                }
                return null;
            }

            private Actor findExactButtonIn(String text, Actor a) {
                if (a instanceof TextButton && text.equals(((TextButton) a).getText().toString())) return a;
                if (a instanceof Group) {
                    for (Actor c : ((Group) a).getChildren()) {
                        Actor hit = findExactButtonIn(text, c);
                        if (hit != null) return hit;
                    }
                }
                return null;
            }

            private void tapScreen(long sx, long sy) {
                Vector2 stagePt = stage.screenToStageCoordinates(new Vector2(sx, sy));
                Actor hit = stage.hit(stagePt.x, stagePt.y, true);
                String what = hit == null ? "<null>" : (hit.getClass().getSimpleName()
                        + (hit instanceof TextButton ? ":" + ((TextButton) hit).getText() : ""));
                System.out.println("SMOKE: tap(" + sx + "," + sy + ") hit=" + what);
                com.badlogic.gdx.InputProcessor p = Gdx.input.getInputProcessor();
                p.touchDown((int) sx, (int) sy, 0, 0);
                p.touchUp((int) sx, (int) sy, 0, 0);
            }

            private void dragJoystick() {
                com.badlogic.gdx.InputProcessor p = Gdx.input.getInputProcessor();
                p.touchDown(STICK_X, STICK_Y, 0, 0);
                p.touchDragged(STICK_X + 45, STICK_Y, 0);
            }

            private void pressAccel() throws Exception {
                Actor accel = findExactText("加速");
                Vector2 c = center(accel);
                com.badlogic.gdx.InputProcessor p = Gdx.input.getInputProcessor();
                p.touchDown((int) c.x, (int) (HUD_H - c.y), 0, 0);
            }

            private void releaseAccel() throws Exception {
                Actor accel = findExactText("加速");
                Vector2 c = center(accel);
                com.badlogic.gdx.InputProcessor p = Gdx.input.getInputProcessor();
                p.touchUp((int) c.x, (int) (HUD_H - c.y), 0, 0);
            }

            private GameState voyageState() throws Exception {
                Field f = VoyageScreen.class.getDeclaredField("g");
                f.setAccessible(true);
                return (GameState) f.get(getScreen());
            }

            private Enum<?> voyageOverlay() throws Exception {
                Field f = VoyageScreen.class.getDeclaredField("overlay");
                f.setAccessible(true);
                return (Enum<?>) f.get(getScreen());
            }

            /** World -> full-map screen pixel for port i (map rect from VoyageScreen). */
            private float[] mapPortToScreen(int port) {
                float x = 80, y = 70, w = 1120, h = 560;
                float ix = x + 10, iy = y + 10, iw = w - 20, ih = h - 20;
                float px = ix + (Catalog.PORT_X[port] / Catalog.WORLD_W) * iw;
                float py = iy + (Catalog.PORT_Y[port] / Catalog.WORLD_H) * ih;
                return new float[] {px, HUD_H - py};
            }

            private Vector2 center(Actor a) {
                Vector2 v = new Vector2(a.getWidth() / 2f, a.getHeight() / 2f);
                return a.localToStageCoordinates(v);
            }

            private int countText(String text) throws Exception {
                int n = 0;
                java.util.Iterator<Actor> it = stage.getRoot().getChildren().iterator();
                while (it.hasNext()) {
                    n += countIn(text, it.next());
                }
                return n;
            }

            private int countIn(String text, Actor a) {
                int n = 0;
                if (a instanceof TextButton && text.equals(((TextButton) a).getText().toString())) {
                    return 1; // TextButton is a Group containing its own label; count once
                }
                if (a instanceof Label && text.equals(((Label) a).getText().toString())) n++;
                if (a instanceof Group) {
                    for (Actor c : ((Group) a).getChildren()) n += countIn(text, c);
                }
                return n;
            }

            private Actor findText(String text) throws Exception {
                for (Actor a : stage.getRoot().getChildren()) {
                    Actor hit = findIn(text, a);
                    if (hit != null) return hit;
                }
                return null;
            }

            private Actor findIn(String text, Actor a) {
                if (a instanceof TextButton && ((TextButton) a).getText().toString().contains(text)) return a;
                if (a instanceof Label && ((Label) a).getText().toString().contains(text)) return a;
                if (a instanceof Group) {
                    for (Actor c : ((Group) a).getChildren()) {
                        Actor hit = findIn(text, c);
                        if (hit != null) return hit;
                    }
                }
                return null;
            }

            private Actor findExactText(String text) throws Exception {
                for (Actor a : stage.getRoot().getChildren()) {
                    Actor hit = findExactIn(text, a);
                    if (hit != null) return hit;
                }
                return null;
            }

            private Actor findExactIn(String text, Actor a) {
                if (a instanceof TextButton && text.equals(((TextButton) a).getText().toString())) return a;
                if (a instanceof Label && text.equals(((Label) a).getText().toString())) return a;
                if (a instanceof Group) {
                    for (Actor c : ((Group) a).getChildren()) {
                        Actor hit = findExactIn(text, c);
                        if (hit != null) return hit;
                    }
                }
                return null;
            }

            private void dumpButtons() throws Exception {
                StringBuilder sb = new StringBuilder("buttons: ");
                for (Actor a : stage.getRoot().getChildren()) {
                    collectButtons(a, sb);
                }
                System.out.println(sb.toString());
            }

            private void collectButtons(Actor a, StringBuilder sb) {
                if (a instanceof TextButton) {
                    String t = ((TextButton) a).getText().toString();
                    sb.append('[').append(t).append("] ");
                }
                if (a instanceof Group) {
                    for (Actor c : ((Group) a).getChildren()) collectButtons(c, sb);
                }
            }

            private void dumpLabels() throws Exception {
                StringBuilder sb = new StringBuilder("labels: ");
                for (Actor a : stage.getRoot().getChildren()) {
                    collectLabels("", a, sb);
                }
                System.out.println(sb.toString());
            }

            private void collectLabels(String indent, Actor a, StringBuilder sb) {
                if (a instanceof Label) {
                    String t = ((Label) a).getText().toString();
                    if (t.length() > 0 && t.length() < 60) sb.append('[').append(t).append("] ");
                }
                if (a instanceof Group) {
                    for (Actor c : ((Group) a).getChildren()) collectLabels(indent, c, sb);
                }
            }

            private void collect(Actor a, List<TextField> out, Class<TextField> type) {
                if (a instanceof TextField) out.add((TextField) a);
                if (a instanceof Group) {
                    for (Actor c : ((Group) a).getChildren()) collect(c, out, type);
                }
            }
        }, config);
        System.out.println("SMOKE: app exited with code " + exitCode);
        System.exit(exitCode);
    }
}
