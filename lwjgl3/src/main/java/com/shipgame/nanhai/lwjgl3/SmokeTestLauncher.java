package com.shipgame.nanhai.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
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
 * Desktop smoke test driving the real 0.25.2 UI through the input pipeline.
 *
 * Flow (mode "register", run with a wiped account store):
 *   docked at 广州 (port popup auto-opens) ->
 *     1. port popup 关闭 closes and stays closed; far-left circular rail
 *        (货物/图鉴/港口) is present and usable while docked
 *     2. 港口 rail reopens the popup, 关闭 closes it again
 *     3. **docked + popup CLOSED: joystick drag undocks the ship (dockedPort
 *        becomes -1) and steers it; holding 加速 raises speed > 6 — the 0.25.2
 *        fix for dead controls after loading a docked save**
 *     4. cargo / codex popups open and close at sea
 *     5. full-map modal: stage hidden (rail/joystick/accel invisible and not
 *        hit-testable), screenshots saved; its 关闭 button closes it
 *     6. minimap reopens the modal; tapping 合浦 auto-sails and closes the map
 *   Exits 0 on success, non-zero with a reason otherwise.
 */
public class SmokeTestLauncher {

    private static final String USER = "boxer";
    private static final String PASS = "pw123456";
    private static final int HUD_H = 720;
    // Geometry mirrors VoyageScreen (hud y-up converted to screen y-down).
    private static final int STICK_X = 170, STICK_Y = HUD_H - 180;   // joystick center
    private static final int MM_X = 1174, MM_Y = HUD_H - 616;        // round minimap center
    private static final int FM_CLOSE_X = 90 + 1100 - 124 + 52;      // modal 关闭 center
    private static final int FM_CLOSE_Y = HUD_H - (90 + 540 - 36 + 14);
    private static final int TARGET_PORT = 5;                        // 合浦 (not 广州)

    private static int frame;
    private static int exitCode = 97;
    private static String mode = "register";
    private static Stage stage;
    private static boolean flowDone;
    private static String failReason;
    private static int nextStepFrame;
    private static int step;
    private static float h0;

    public static void main(String[] args) {
        if (args.length > 0) {
            mode = args[0];
        }
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("nanhai-smoke");
        config.setWindowedMode(1280, 720);
        config.setForegroundFPS(60);
        config.disableAudio(true); // headless CI box has no audio device
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
                if (frame > 1100) {
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
                    case 1:
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
                    case 3: // docked at 广州 with the port popup auto-open
                        if (!(getScreen() instanceof VoyageScreen)) {
                            dumpLabels();
                        }
                        require(getScreen() instanceof VoyageScreen, "expected VoyageScreen, got " + getScreen());
                        refreshStageFrom(getScreen().getClass());
                        GameState st = voyageState();
                        require(st.dockedPort == 0, "expected docked at 广州, dockedPort=" + st.dockedPort);
                        require(voyageOverlay() != null && voyageOverlay().name().equals("PORT"),
                                "port popup should auto-open, got " + voyageOverlay());
                        require(countText("关闭") >= 1, "port popup 关闭 not found");
                        require(stageVisible(), "stage should be visible while docked");
                        require(railPresent(), "left circular rail (货物/图鉴/港口) missing");
                        System.out.println("SMOKE: docked at 广州, port popup open, rail present");
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
                        require(voyageOverlay() == null || voyageOverlay().name().equals("NONE"),
                                "overlay should be NONE after closing, got " + voyageOverlay());
                        require(railPresent(), "rail buttons vanished after closing the popup");
                        saveShot("docked-hud");
                        System.out.println("SMOKE: port popup closed and stays closed; rail visible");
                        step = 6;
                        nextStepFrame = frame + 6;
                        break;
                    case 6: // 港口 rail reopens the popup while still docked
                        require(tapButton("港口"), "could not tap rail 港口");
                        step = 7;
                        nextStepFrame = frame + 10;
                        break;
                    case 7:
                        require(voyageOverlay() != null && voyageOverlay().name().equals("PORT"),
                                "port popup not reopened via rail, got " + voyageOverlay());
                        require(tapButton("关闭"), "could not close reopened port popup");
                        step = 8;
                        nextStepFrame = frame + 10;
                        break;
                    case 8:
                        require(voyageOverlay() == null || voyageOverlay().name().equals("NONE"),
                                "overlay should be NONE after 2nd close, got " + voyageOverlay());
                        System.out.println("SMOKE: 港口 rail reopens the popup; 关闭 works (docked, no menu)");
                        step = 9;
                        nextStepFrame = frame + 6;
                        break;
                    case 9: // 0.25.2 fix: joystick while docked + menu CLOSED must undock
                        h0 = voyageState().headingDeg;
                        require(voyageState().dockedPort == 0, "expected still docked before joystick, got "
                                + voyageState().dockedPort);
                        joystickDragRight();
                        step = 10;
                        nextStepFrame = frame + 4;
                        break;
                    case 10:
                        GameState st2 = voyageState();
                        require(st2.dockedPort == -1,
                                "joystick did not undock the ship (dockedPort=" + st2.dockedPort + ")");
                        require(Math.abs(st2.steerInput) > 0.05f,
                                "joystick drag did not steer (steerInput=" + st2.steerInput + ")");
                        System.out.println("SMOKE: docked+menu-closed joystick undocked & steered (dockedPort=-1)");
                        step = 11;
                        nextStepFrame = frame + 16; // let the turn accumulate
                        break;
                    case 11:
                        float h1 = voyageState().headingDeg;
                        float turn = Math.abs(h1 - h0);
                        if (turn > 180f) turn = 360f - turn;
                        require(turn > 3f, "joystick drag did not turn the ship (h0=" + h0 + " h1=" + h1 + ")");
                        System.out.println("SMOKE: joystick turned ship by " + (int) turn + " deg");
                        require(voyageState().holdAccel == false, "unexpected accel state");
                        pressAccel();
                        step = 12;
                        nextStepFrame = frame + 16;
                        break;
                    case 12:
                        float sp = voyageState().speed;
                        require(sp > 6f, "holding 加速 did not raise speed (speed=" + sp + ")");
                        System.out.println("SMOKE: 加速 raised speed to " + (int) sp);
                        releaseAccel();
                        pressDecel();
                        step = 13;
                        nextStepFrame = frame + 12;
                        break;
                    case 13:
                        float spd = voyageState().speed;
                        require(spd < 12f, "holding 减速 did not cut speed (speed=" + spd + ")");
                        System.out.println("SMOKE: 减速 cut speed to " + (int) spd);
                        releaseDecel();
                        joystickRelease();
                        step = 14;
                        nextStepFrame = frame + 6;
                        break;
                    case 14: // open cargo popup from the rail (at sea)
                        require(tapButton("货物"), "could not tap rail 货物");
                        step = 15;
                        nextStepFrame = frame + 10;
                        break;
                    case 15:
                        require(voyageOverlay() != null && voyageOverlay().name().equals("CARGO"),
                                "cargo popup not open, got " + voyageOverlay());
                        int closes = countText("关闭");
                        if (closes != 1) dumpButtons();
                        require(closes == 1, "cargo popup should have exactly one 关闭, got " + closes);
                        require(findText("商货") != null || findText("[商货]") != null, "cargo tabs missing");
                        System.out.println("SMOKE: cargo popup open with tabs");
                        require(tapButton("关闭"), "could not tap cargo 关闭");
                        step = 16;
                        nextStepFrame = frame + 10;
                        break;
                    case 16:
                        require(countText("关闭") == 0, "cargo popup did not close");
                        step = 17;
                        nextStepFrame = frame + 6;
                        break;
                    case 17: // open codex popup
                        require(tapButton("图鉴"), "could not tap rail 图鉴");
                        step = 18;
                        nextStepFrame = frame + 10;
                        break;
                    case 18:
                        require(voyageOverlay() != null && voyageOverlay().name().equals("CODEX"),
                                "codex popup not open, got " + voyageOverlay());
                        require(findText("异兽") != null, "codex section 异兽 missing");
                        require(tapButton("关闭"), "could not tap codex 关闭");
                        step = 19;
                        nextStepFrame = frame + 10;
                        break;
                    case 19:
                        require(countText("关闭") == 0, "codex popup did not close");
                        System.out.println("SMOKE: cargo/codex open+close OK");
                        step = 20;
                        nextStepFrame = frame + 6;
                        break;
                    case 20: // open the full map via the round minimap
                        tapScreen(MM_X, MM_Y);
                        step = 21;
                        nextStepFrame = frame + 8;
                        break;
                    case 21:
                        require(voyageOverlay() != null && voyageOverlay().name().equals("MAP"),
                                "full map not open, got " + voyageOverlay());
                        require(!stageVisible(), "HUD stage must be hidden under the full-map modal");
                        require(noStageHitAtRail(), "rail/accel must not be clickable under the modal");
                        System.out.println("SMOKE: full-map modal covers the HUD (stage hidden)");
                        saveShot("map-modal");
                        step = 22;
                        nextStepFrame = frame + 6;
                        break;
                    case 22: // modal 关闭 button closes it
                        tapScreen(FM_CLOSE_X, FM_CLOSE_Y);
                        step = 23;
                        nextStepFrame = frame + 8;
                        break;
                    case 23:
                        require(voyageOverlay() == null || voyageOverlay().name().equals("NONE"),
                                "map modal 关闭 did not close it, got " + voyageOverlay());
                        require(stageVisible(), "stage should be visible again after closing the modal");
                        System.out.println("SMOKE: map modal 关闭 works, HUD back");
                        step = 24;
                        nextStepFrame = frame + 6;
                        break;
                    case 24: // reopen and tap a far port -> auto-sail
                        tapScreen(MM_X, MM_Y);
                        step = 25;
                        nextStepFrame = frame + 8;
                        break;
                    case 25:
                        require(voyageOverlay() != null && voyageOverlay().name().equals("MAP"),
                                "full map not reopened, got " + voyageOverlay());
                        float[] xy = mapPortToScreen(TARGET_PORT);
                        tapScreen((long) xy[0], (long) xy[1]);
                        step = 26;
                        nextStepFrame = frame + 8;
                        break;
                    case 26:
                        GameState st3 = voyageState();
                        require(st3.autoSail && st3.autoSailPort == TARGET_PORT,
                                "auto-sail not set for port " + TARGET_PORT + " (autoSail=" + st3.autoSail
                                        + " port=" + st3.autoSailPort + ")");
                        require(voyageOverlay() == null || voyageOverlay().name().equals("NONE"),
                                "full map should be closed after target tap, got " + voyageOverlay());
                        require(stageVisible(), "stage should be visible after the map closed");
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

            // ---------------------------------------------------- helpers

            /** Re-grab the current screen's stage (screen switches create a new one). */
            private void refreshStageFrom(Class<?> screenClass) throws Exception {
                Field f = screenClass.getDeclaredField("stage");
                f.setAccessible(true);
                stage = (Stage) f.get(getScreen());
            }

            private boolean stageVisible() {
                return stage.getRoot().isVisible();
            }

            /** All three rail buttons present anywhere in the stage. */
            private boolean railPresent() throws Exception {
                return findExactButton("货物") != null
                        && findExactButton("图鉴") != null
                        && findExactButton("港口") != null;
            }

            /** Under the modal the stage root is invisible, so hits on the rail /
             * accel areas must return null (nothing below is clickable). */
            private boolean noStageHitAtRail() throws Exception {
                int[][] pts = {{55, 140}, {55, 216}, {55, 292}, {1200, 620}};
                for (int[] p : pts) {
                    Vector2 sp = stage.screenToStageCoordinates(new Vector2(p[0], p[1]));
                    if (stage.hit(sp.x, sp.y, true) != null) {
                        System.out.println("SMOKE: modal still hit-testable at " + p[0] + "," + p[1]
                                + " -> " + stage.hit(sp.x, sp.y, true));
                        return false;
                    }
                }
                return true;
            }

            private void saveShot(String name) {
                try {
                    int w = Gdx.graphics.getBackBufferWidth();
                    int h = Gdx.graphics.getBackBufferHeight();
                    Pixmap pm = Pixmap.createFromFrameBuffer(0, 0, w, h);
                    PixmapIO.writePNG(Gdx.files.absolute("/tmp/shots/" + name + ".png"), pm, 1, true);
                    pm.dispose();
                    System.out.println("SMOKE: shot /tmp/shots/" + name + ".png (" + w + "x" + h + ")");
                } catch (Throwable t) {
                    System.out.println("SMOKE: shot failed: " + t);
                }
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

            private void joystickDragRight() {
                com.badlogic.gdx.InputProcessor p = Gdx.input.getInputProcessor();
                p.touchDown(STICK_X, STICK_Y, 0, 0);
                p.touchDragged(STICK_X + 50, STICK_Y, 0);
            }

            private void joystickRelease() {
                com.badlogic.gdx.InputProcessor p = Gdx.input.getInputProcessor();
                p.touchUp(STICK_X + 50, STICK_Y, 0, 0);
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

            private void pressDecel() throws Exception {
                Actor decel = findExactText("减速");
                Vector2 c = center(decel);
                com.badlogic.gdx.InputProcessor p = Gdx.input.getInputProcessor();
                p.touchDown((int) c.x, (int) (HUD_H - c.y), 0, 0);
            }

            private void releaseDecel() throws Exception {
                Actor decel = findExactText("减速");
                Vector2 c = center(decel);
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

            /** World -> full-map screen pixel for port i (modal rect from VoyageScreen). */
            private float[] mapPortToScreen(int port) {
                float x = 90f, y = 90f, w = 1100f, h = 540f;
                float ix = x + 10f, iy = y + 10f, iw = w - 20f, ih = h - 20f;
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
