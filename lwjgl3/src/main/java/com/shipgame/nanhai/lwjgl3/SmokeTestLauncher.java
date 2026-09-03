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
 *     1. port popup 关闭 closes and stays closed; the 0.26.2 top HUD is present:
 *        circular 船长 avatar (top-left), the four stat cells 银两/补给/耐久/船员
 *        with live numbers, the sun icon + 今日：晴 tip, and the top-right icon
 *        rail 货物/图鉴/港口/情报/任务 left of the round minimap
 *     2. 港口 rail reopens the popup, 关闭 closes it again
 *     3. **docked + popup CLOSED: joystick drag undocks the ship (dockedPort
 *        becomes -1) and steers it; holding 加速 raises speed > 6 — the 0.25.2
 *        fix for dead controls after loading a docked save**
 *     4. cargo / codex popups open and close at sea
 *     5. top-left HUD: tapping stat cells opens the 说明 popup (switching cells
 *        switches the popup); tapping the avatar opens 船长菜单 with 保存进度 /
 *        读取存档; tapping 情报/任务 rail buttons opens their popups at sea
 *     6. full-map modal: stage hidden (rail/joystick/accel invisible and not
 *        hit-testable), screenshots saved; its 关闭 button closes it
 *     7. minimap reopens the modal; tapping 合浦 auto-sails and closes the map
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
    // 0.26.2 top HUD mirrors VoyageScreen (stage y-up, 720 tall): the stat
    // panel spans stage y 654..710 (screen y-down 10..66) at x 82..474, the
    // avatar circle centers on stage (44,682) -> screen (44,38), and the icon
    // rail column sits left of the minimap (stage x 1023, y-down 106..326).
    private static final int STAT_X0 = (int) (44 + 28 + 10);         // panel left (screen x)
    private static final int STAT_YC = HUD_H - (654 + 56 / 2);       // row center (y-down)
    private static final int STAT_W = 98;
    private static final int AVATAR_X = 44, AVATAR_YC = HUD_H - 682; // 船长 circle center
    private static final int RAIL_X = (int) (1174 - 80 - 44);        // column center (screen x)
    private static final int RAIL_TOP_Y = HUD_H - 664;               // 货物 center (y-down)

    private static int frame;
    private static int exitCode = 97;
    private static String mode = "register";
    private static Stage stage;
    private static boolean flowDone;
    private static String failReason;
    private static int nextStepFrame;
    private static int step;
    private static float h0;
    private static float pirateHp0;
    private static float hull0;

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
                        require(avatarAndStatsPresent(), "top-left avatar/stat HUD missing");
                        require(railPresent(), "top-right icon rail (货物/图鉴/港口/情报/任务) missing");
                        require(findText("今日：晴") != null, "今日：晴 weather tip missing");
                        System.out.println("SMOKE: docked at 广州, port popup open, 0.26.2 top HUD present");
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
                        require(avatarAndStatsPresent(), "stat HUD vanished after closing the popup");
                        require(railPresent(), "rail buttons vanished after closing the popup");
                        // 0.26.2: each top stat cell is tappable and opens 说明.
                        tapScreen(STAT_X0 + STAT_W / 2, STAT_YC); // 银两
                        step = 6;
                        nextStepFrame = frame + 10;
                        break;
                    case 6: // stat detail popup open (银两)
                        require(voyageOverlay() != null && voyageOverlay().name().equals("STAT"),
                                "stat popup not open, got " + voyageOverlay());
                        require(findText("银两 · 说明") != null, "银两 detail popup missing");
                        // tapping the NEXT cell switches the popup content
                        tapScreen(STAT_X0 + STAT_W + STAT_W / 2, STAT_YC); // 补给
                        step = 7;
                        nextStepFrame = frame + 10;
                        break;
                    case 7:
                        require(voyageOverlay() != null && voyageOverlay().name().equals("STAT"),
                                "stat popup closed unexpectedly, got " + voyageOverlay());
                        require(findText("补给 · 说明") != null, "stat popup did not switch to 补给");
                        require(tapButton("关闭"), "could not close stat popup");
                        step = 8;
                        nextStepFrame = frame + 10;
                        break;
                    case 8:
                        require(voyageOverlay() == null || voyageOverlay().name().equals("NONE"),
                                "stat popup did not close, got " + voyageOverlay());
                        // tap the 船长 avatar -> save/load menu
                        tapScreen(AVATAR_X, AVATAR_YC);
                        step = 9;
                        nextStepFrame = frame + 10;
                        break;
                    case 9: // avatar menu open
                        require(voyageOverlay() != null && voyageOverlay().name().equals("AVATAR"),
                                "avatar menu not open, got " + voyageOverlay());
                        require(findText("保存进度") != null && findText("读取存档") != null,
                                "avatar save/load buttons missing");
                        require(tapButton("关闭"), "could not close avatar menu");
                        step = 10;
                        nextStepFrame = frame + 10;
                        break;
                    case 10:
                        require(voyageOverlay() == null || voyageOverlay().name().equals("NONE"),
                                "avatar menu did not close, got " + voyageOverlay());
                        System.out.println("SMOKE: stat detail popups + avatar save/load menu OK");
                        step = 11;
                        nextStepFrame = frame + 6;
                        break;
                    case 11: // 港口 rail reopens the popup while still docked
                        require(tapButton("港口"), "could not tap rail 港口");
                        step = 12;
                        nextStepFrame = frame + 10;
                        break;
                    case 12:
                        require(voyageOverlay() != null && voyageOverlay().name().equals("PORT"),
                                "port popup not reopened via rail, got " + voyageOverlay());
                        require(tapButton("关闭"), "could not close reopened port popup");
                        step = 13;
                        nextStepFrame = frame + 10;
                        break;
                    case 13:
                        require(voyageOverlay() == null || voyageOverlay().name().equals("NONE"),
                                "overlay should be NONE after 2nd close, got " + voyageOverlay());
                        System.out.println("SMOKE: 港口 rail reopens the popup; 关闭 works (docked, no menu)");
                        step = 14;
                        nextStepFrame = frame + 6;
                        break;
                    case 14: // 0.25.2 fix: joystick while docked + menu CLOSED must undock
                        h0 = voyageState().headingDeg;
                        require(voyageState().dockedPort == 0, "expected still docked before joystick, got "
                                + voyageState().dockedPort);
                        // 0.25.5 heading joystick: the stick aims an absolute compass
                        // heading (manualHeadingActive), not an angular steerInput.
                        // Aim 60 deg off the current heading so the turn is certain.
                        joystickAimAt(h0 + 60f);
                        step = 15;
                        nextStepFrame = frame + 4;
                        break;
                    case 15:
                        GameState st2 = voyageState();
                        require(st2.dockedPort == -1,
                                "joystick did not undock the ship (dockedPort=" + st2.dockedPort + ")");
                        require(st2.manualHeadingActive,
                                "joystick drag did not arm the heading aim (manualHeadingActive=false)");
                        System.out.println("SMOKE: docked+menu-closed joystick undocked & aiming (dockedPort=-1)");
                        step = 16;
                        nextStepFrame = frame + 16; // let the turn accumulate
                        break;
                    case 16:
                        float h1 = voyageState().headingDeg;
                        float turn = Math.abs(h1 - h0);
                        if (turn > 180f) turn = 360f - turn;
                        require(turn > 3f, "joystick drag did not turn the ship (h0=" + h0 + " h1=" + h1 + ")");
                        System.out.println("SMOKE: joystick turned ship by " + (int) turn + " deg");
                        joystickRelease();
                        require(voyageState().holdAccel == false, "unexpected accel state");
                        pressAccel();
                        step = 17;
                        nextStepFrame = frame + 16;
                        break;
                    case 17:
                        float sp = voyageState().speed;
                        require(sp > 6f, "holding 加速 did not raise speed (speed=" + sp + ")");
                        System.out.println("SMOKE: 加速 raised speed to " + (int) sp);
                        releaseAccel();
                        pressDecel();
                        step = 18;
                        nextStepFrame = frame + 12;
                        break;
                    case 18:
                        float spd = voyageState().speed;
                        require(spd < 12f, "holding 减速 did not cut speed (speed=" + spd + ")");
                        System.out.println("SMOKE: 减速 cut speed to " + (int) spd);
                        releaseDecel();
                        step = 19;
                        nextStepFrame = frame + 6;
                        break;
                    case 19: // open cargo popup from the rail (at sea)
                        require(tapButton("货物"), "could not tap rail 货物");
                        step = 20;
                        nextStepFrame = frame + 10;
                        break;
                    case 20:
                        require(voyageOverlay() != null && voyageOverlay().name().equals("CARGO"),
                                "cargo popup not open, got " + voyageOverlay());
                        int closes = countText("关闭");
                        if (closes != 1) dumpButtons();
                        require(closes == 1, "cargo popup should have exactly one 关闭, got " + closes);
                        require(findText("商货") != null || findText("[商货]") != null, "cargo tabs missing");
                        System.out.println("SMOKE: cargo popup open with tabs");
                        require(tapButton("关闭"), "could not tap cargo 关闭");
                        step = 21;
                        nextStepFrame = frame + 10;
                        break;
                    case 21:
                        require(countText("关闭") == 0, "cargo popup did not close");
                        step = 22;
                        nextStepFrame = frame + 6;
                        break;
                    case 22: // open codex popup
                        require(tapButton("图鉴"), "could not tap rail 图鉴");
                        step = 23;
                        nextStepFrame = frame + 10;
                        break;
                    case 23:
                        require(voyageOverlay() != null && voyageOverlay().name().equals("CODEX"),
                                "codex popup not open, got " + voyageOverlay());
                        require(findText("异兽") != null, "codex section 异兽 missing");
                        require(tapButton("关闭"), "could not tap codex 关闭");
                        step = 24;
                        nextStepFrame = frame + 10;
                        break;
                    case 24:
                        require(countText("关闭") == 0, "codex popup did not close");
                        step = 25;
                        nextStepFrame = frame + 6;
                        break;
                    case 25: // open 情报 popup (0.26.2 rail)
                        require(tapButton("情报"), "could not tap rail 情报");
                        step = 26;
                        nextStepFrame = frame + 10;
                        break;
                    case 26:
                        require(voyageOverlay() != null && voyageOverlay().name().equals("INTEL"),
                                "intel popup not open, got " + voyageOverlay());
                        System.out.println("SMOKE: cargo/codex/intel open+close OK");
                        require(tapButton("关闭"), "could not tap intel 关闭");
                        step = 27;
                        nextStepFrame = frame + 10;
                        break;
                    case 27:
                        require(countText("关闭") == 0, "intel popup did not close");
                        step = 28;
                        nextStepFrame = frame + 6;
                        break;
                    case 28: // open the full map via the round minimap
                        tapScreen(MM_X, MM_Y);
                        step = 29;
                        nextStepFrame = frame + 8;
                        break;
                    case 29:
                        require(voyageOverlay() != null && voyageOverlay().name().equals("MAP"),
                                "full map not open, got " + voyageOverlay());
                        require(!stageVisible(), "HUD stage must be hidden under the full-map modal");
                        require(noStageHitAtRail(), "rail/accel must not be clickable under the modal");
                        // 0.25.3 sea chart: 广州 (NE) and 佛逝 (SW) must project to
                        // clearly different points, and the chart pixels (sea, land,
                        // player marker) must actually be visible in the frame.
                        float[] gz = mapPortToScreen(0);
                        int fos = -1;
                        for (int i = 0; i < Catalog.PORTS.length; i++) {
                            if (Catalog.PORTS[i].equals("佛逝")) fos = i;
                        }
                        float[] fs = mapPortToScreen(fos);
                        float pdist = (float) Math.hypot(gz[0] - fs[0], gz[1] - fs[1]);
                        require(pdist > 100f, "广州/佛逝 project too close on the chart (dist=" + (int) pdist + ")");
                        System.out.println("SMOKE: chart ports distinct: 广州(" + (int) gz[0] + "," + (int) gz[1]
                                + ") vs 佛逝(" + (int) fs[0] + "," + (int) fs[1] + ") dist=" + (int) pdist);
                        System.out.println("SMOKE: full-map modal covers the HUD (stage hidden)");
                        saveShot("map-modal");
                        verifyMapShot();
                        step = 30;
                        nextStepFrame = frame + 6;
                        break;
                    case 30: // modal 关闭 button closes it
                        tapScreen(FM_CLOSE_X, FM_CLOSE_Y);
                        step = 31;
                        nextStepFrame = frame + 8;
                        break;
                    case 31:
                        require(voyageOverlay() == null || voyageOverlay().name().equals("NONE"),
                                "map modal 关闭 did not close it, got " + voyageOverlay());
                        require(stageVisible(), "stage should be visible again after closing the modal");
                        System.out.println("SMOKE: map modal 关闭 works, HUD back");
                        step = 32;
                        nextStepFrame = frame + 6;
                        break;
                    case 32: // reopen and tap a far port -> auto-sail
                        tapScreen(MM_X, MM_Y);
                        step = 33;
                        nextStepFrame = frame + 8;
                        break;
                    case 33:
                        require(voyageOverlay() != null && voyageOverlay().name().equals("MAP"),
                                "full map not reopened, got " + voyageOverlay());
                        float[] xy = mapPortToScreen(TARGET_PORT);
                        tapScreen((long) xy[0], (long) xy[1]);
                        step = 34;
                        nextStepFrame = frame + 8;
                        break;
                    case 34:
                        GameState st3 = voyageState();
                        require(st3.autoSail && st3.autoSailPort == TARGET_PORT,
                                "auto-sail not set for port " + TARGET_PORT + " (autoSail=" + st3.autoSail
                                        + " port=" + st3.autoSailPort + ")");
                        require(voyageOverlay() == null || voyageOverlay().name().equals("NONE"),
                                "full map should be closed after target tap, got " + voyageOverlay());
                        require(stageVisible(), "stage should be visible after the map closed");
                        System.out.println("SMOKE: auto-sail to port " + Catalog.PORTS[TARGET_PORT] + " set");
                        // Deterministic short pirate encounter: put one enemy in
                        // range, verify its lock UI, then tap the ship itself.
                        st3.autoSail = false;
                        st3.autoSailPort = -1;
                        st3.autoSailIsle = -1;
                        st3.pirateAlive = true;
                        st3.combatLock = false;
                        st3.pirateChase = false;
                        st3.pirateX = st3.x + 200f;
                        st3.pirateY = st3.y;
                        st3.pirateHpMax = Catalog.PIRATE_HP;
                        st3.pirateHp = st3.pirateHpMax;
                        st3.playerFireCd = 0f;
                        st3.pirateFireCd = 0f;
                        pirateHp0 = st3.pirateHp;
                        hull0 = st3.hull;
                        step = 35;
                        nextStepFrame = frame + 6;
                        break;
                    case 35:
                        require(findExactButton("锁定海盗") != null,
                                "pirate lock UI is not visible");
                        // World viewport is 960x540 in a 1280x720 window. Enemy is
                        // exactly +200 world units from the centred player.
                        tapScreen(640 + Math.round(200f * 1280f / 960f), 360);
                        step = 36;
                        nextStepFrame = frame + 6;
                        break;
                    case 36:
                        GameState fight = voyageState();
                        require(fight.combatLock, "tapping pirate ship did not lock it");
                        require(findExactButton("取消锁定") != null,
                                "cancel-lock UI is not visible after locking");
                        step = 37;
                        nextStepFrame = frame + 55;
                        break;
                    case 37:
                        GameState exchanged = voyageState();
                        require(exchanged.pirateHp < pirateHp0,
                                "locked pirate did not take automatic cannon damage");
                        require(exchanged.hull < hull0,
                                "pirate did not return fire against player");
                        require(exchanged.pirateChase,
                                "pirate did not pursue more closely after return fire");
                        require(tapButton("取消锁定"), "could not tap cancel lock");
                        saveShot("pirate-combat");
                        System.out.println("SMOKE: PASS - pirate tapped, auto-fire exchanged damage, cancel-lock visible");
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

            /** 0.26.2 top HUD: circular 船长 avatar + the four stat cells with
             * their live value labels, all present anywhere in the stage. */
            private boolean avatarAndStatsPresent() throws Exception {
                boolean ok = findExactButton("船长") != null;
                for (String nm : new String[] {"银两", "补给", "耐久", "船员"}) {
                    ok &= namedCell(nm) != null;
                }
                return ok;
            }

            private Actor namedCell(String name) throws Exception {
                for (Actor a : stage.getRoot().getChildren()) {
                    Actor hit = findNamed(name, a);
                    if (hit != null) return hit;
                }
                return null;
            }

            private Actor findNamed(String name, Actor a) {
                if (name.equals(a.getName())) return a;
                if (a instanceof Group) {
                    for (Actor c : ((Group) a).getChildren()) {
                        Actor hit = findNamed(name, c);
                        if (hit != null) return hit;
                    }
                }
                return null;
            }

            /** All five rail buttons present anywhere in the stage. */
            private boolean railPresent() throws Exception {
                return findExactButton("货物") != null
                        && findExactButton("图鉴") != null
                        && findExactButton("港口") != null
                        && findExactButton("情报") != null
                        && findExactButton("任务") != null;
            }

            /** Under the modal the stage root is invisible, so hits on the rail /
             * accel areas must return null (nothing below is clickable). */
            private boolean noStageHitAtRail() throws Exception {
                // 0.26.2 rail column centers (top-right), plus the avatar and the
                // bottom-right accel/decel stack.
                int[][] pts = {
                        {RAIL_X, RAIL_TOP_Y},
                        {RAIL_X, RAIL_TOP_Y + 56},
                        {RAIL_X, RAIL_TOP_Y + 112},
                        {RAIL_X, RAIL_TOP_Y + 168},
                        {RAIL_X, RAIL_TOP_Y + 224},
                        {AVATAR_X, AVATAR_YC},
                        {1200, 620}
                };
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

            /** 0.25.6 chart visibility: the saved full-map frame must contain deep
             * sea pixels, khaki land pixels and the RED radar pulse anchored at the
             * player's real projected position (not a corner legend). */
            private void verifyMapShot() throws Exception {
                java.io.File f = new java.io.File("/tmp/shots/map-modal.png");
                require(f.exists(), "map-modal.png missing");
                javax.imageio.ImageIO.setUseCache(false);
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(f);
                require(img != null, "map-modal.png unreadable");
                int sea = 0, land = 0;
                for (int y = 0; y < img.getHeight(); y++) {
                    for (int x = 0; x < img.getWidth(); x++) {
                        int rgb = img.getRGB(x, y);
                        int r = (rgb >> 16) & 0xff, g2 = (rgb >> 8) & 0xff, b = rgb & 0xff;
                        if (Math.abs(r - 13) <= 8 && Math.abs(g2 - 38) <= 8 && Math.abs(b - 61) <= 8) sea++;
                        else if (Math.abs(r - 148) <= 10 && Math.abs(g2 - 125) <= 10 && Math.abs(b - 82) <= 10) land++;
                    }
                }
                // Red radar center dot sits exactly on the player's projected world
                // position; count bright-red pixels within a 70px window of it.
                GameState st = voyageState();
                float[] me = worldToMapScreen(st.x, st.y);
                int radar = 0;
                int y0 = Math.max(0, (int) me[1] - 70), y1 = Math.min(img.getHeight() - 1, (int) me[1] + 70);
                int x0 = Math.max(0, (int) me[0] - 70), x1 = Math.min(img.getWidth() - 1, (int) me[0] + 70);
                for (int y = y0; y <= y1; y++) {
                    for (int x = x0; x <= x1; x++) {
                        int rgb = img.getRGB(x, y);
                        int r = (rgb >> 16) & 0xff, g2 = (rgb >> 8) & 0xff, b = rgb & 0xff;
                        if (r > 190 && g2 < 110 && b < 110) radar++;
                    }
                }
                require(sea > 2000, "full map has no sea (sea=" + sea + ")");
                require(land > 300, "full map has no land (land=" + land + ")");
                require(radar > 25, "red radar pulse missing at ship pos (" + (int) me[0]
                        + "," + (int) me[1] + ") radar=" + radar);
                System.out.println("SMOKE: sea chart + red radar at ship visible: sea=" + sea
                        + " land=" + land + " radar(red)=" + radar + " @(" + (int) me[0] + "," + (int) me[1] + ")");
            }

            /** World coords -> full-map screen pixel (modal rect from VoyageScreen). */
            private float[] worldToMapScreen(float wx, float wy) {
                float x = 90f, y = 90f, w = 1100f, h = 540f;
                float ix = x + 10f, iy = y + 10f, iw = w - 20f, ih = h - 20f;
                float px = ix + (wx / Catalog.WORLD_W) * iw;
                float py = iy + (wy / Catalog.WORLD_H) * ih;
                return new float[] {px, HUD_H - py};
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

            /** Tap an actor found by exact TextButton text OR by its stage name
             * (the 0.26.2 icon rail / avatar are named Tables with no text). */
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
                if (text.equals(a.getName())) return a;   // 0.26.2 named icon buttons
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

            /** Drag the stick to aim at an absolute compass heading (degrees,
             * 0=east). Screen y grows downward, so the vertical offset is negated
             * before the drag is injected. */
            private void joystickAimAt(float headingDeg) {
                float rad = (float) Math.toRadians(headingDeg);
                int tx = STICK_X + (int) Math.round(Math.cos(rad) * 60f);
                int ty = STICK_Y - (int) Math.round(Math.sin(rad) * 60f);
                com.badlogic.gdx.InputProcessor p = Gdx.input.getInputProcessor();
                p.touchDown(STICK_X, STICK_Y, 0, 0);
                p.touchDragged(tx, ty, 0);
            }

            private void joystickRelease() {
                com.badlogic.gdx.InputProcessor p = Gdx.input.getInputProcessor();
                p.touchUp(STICK_X + 60, STICK_Y, 0, 0);
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
