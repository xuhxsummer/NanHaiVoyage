package com.shipgame.nanhai.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.shipgame.nanhai.NanHaiVoyage;
import com.shipgame.nanhai.data.Catalog;
import com.shipgame.nanhai.data.GameState;
import com.shipgame.nanhai.data.SaveData;
import com.shipgame.nanhai.ui.IconLib;
import com.shipgame.nanhai.ui.FullscreenPage;
import com.shipgame.nanhai.data.VoyageGeometry;
import com.shipgame.nanhai.ui.QuestUi;
import com.shipgame.nanhai.ui.IntelPanel;
import com.shipgame.nanhai.ui.ShopShipsPanel;
import com.shipgame.nanhai.ui.MyShipPanel;
import com.shipgame.nanhai.ui.CaptainMenuPanel;
import com.shipgame.nanhai.ui.PortDockPanel;
import com.shipgame.nanhai.ui.CodexPanel;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.shipgame.nanhai.ui.WorldMapOverlay;
import com.shipgame.nanhai.ui.VoyageHud;
import com.shipgame.nanhai.render.VoyageWorldRenderer;

public class VoyageScreen extends ScreenAdapter {

    private static final float HUD_W = 1280f;
    private static final float HUD_H = 720f;
    private static final float MENU_W = 500f;             // popup content width
    private static final float PORT_MENU_W = 900f;
    private static final float SHOP_W = 820f;             // 0.26.4 商城 popup width
    // Round minimap: far top-right corner (0.26.5). The whole chart is drawn
    // inside the circle (no rectangular viewport window/frustum); 7 rail icons
    // sit in one horizontal row just LEFT of it, and the active-quest card sits
    // under that row.
    private static final float MM_CX = HUD_W - 102f;
    private static final float MM_CY = HUD_H - 100f;
    private static final float MM_R = 82f;
    // 0.27.2: tracking-minimap half-side in world units (== main camera half
    // width), so the round minimap follows the ship's view and never distorts.
    private static final float MM_WORLD_HALF = 480f;
    // Captain avatar (top-left): tap opens the 船长菜单 (account menu). Right of it
    // sits the stat panel with ICON+NUMBER for 银两/补给/耐久/船员; each cell
    // opens a short detail popup.
    private static final float AV_X = 44f;       // avatar circle center
    private static final float AV_Y = 682f;
    private static final float AV_R = 28f;
    private static final float STAT_Y = 654f;    // bottom edge of the stat panel
    private static final float STAT_CELL_W = 98f;
    private static final float STAT_CELL_H = 56f;
    // Top-right icon rail (0.26.5): SEVEN round icon buttons in one horizontal
    // row at the top, just LEFT of the round minimap (rows never wrap; the
    // HUD is 1280 wide so a straight 7-button row fits between the top-left
    // stat panel and the minimap). NR_ROW_Y is the buttons' center y.
    private static final float NR_D = 46f;                 // button diameter
    private static final float NR_GAP = 6f;                // pixel gap between buttons
    private static final float NR_RIGHT_EDGE = MM_CX - MM_R - 4f; // row right edge
    private static final float NR_ROW_Y = 664f;            // row center y (top band)
    // Full-map modal geometry (overlay == Overlay.MAP): the map is a fullscreen
    // dimmed rect with the map area centered; its 关闭 button sits top-right.

    private static final Color WATER = new Color(0.10f, 0.36f, 0.52f, 1f); // 0.26.2: 常年晴
    private static final Color PIRATE_C = new Color(0.72f, 0.16f, 0.14f, 1f);
    private static final Color PANEL = new Color(0f, 0f, 0f, 0.55f);

    private enum Overlay {
        NONE, PORT, ISLAND, MAP, CODEX, CARGO, PRICE, FAIL,
        // 0.26.0 sub-views: MARKET is the paged buy/sell screen reachable from
        // the docked actions menu; AVATAR is the paused 船长菜单 (account menu);
        // HOWTO is the first-run 玩法说明 popup shown once per install.
        // 0.26.2: STAT is the detail popup opened from a top stat cell.
        // 0.26.3: FISH is the 扬州-only 渔务 dock sub-view (hire fishers,
        // start/stop fishing, upgrades, catch list + sell).
        // 0.26.4: SHOP is the 商城 popup (buy / switch ships).
        // 0.26.5: MINE is the 我的 popup (current/owned ships + resources).
        MARKET, AVATAR, HOWTO, INTEL, QUESTS, STAT, FISH, SHOP, MINE, REDEEM
    }

    /** 0.26.0 first-run gameplay help. Body is verbatim from howto_spec.txt:
     * do not edit a single character or mark of punctuation here, or the
     * popup and the 需求文档 appendix drift apart. */
    private static final String HOWTO_BODY =
            "你将驾驶商船探索南海，在港口贸易、岛屿寻宝，并躲避或击败海盗。\n\n"
            + "1. 航行：左侧摇杆控制船头方向，右侧按钮控制加速和减速。补给耗尽或耐久降到 0，航程就会失败。\n"
            + "2. 港口贸易：不同港口的货价不同。低价买入、高价卖出可以赚取银两；点击「行情」可查看各港价格。\n"
            + "3. 岛屿探索：靠近岛屿后可以搜索草药和《山海经》异兽。发现后会收入货舱并加入图鉴，也可以带到港口出售。\n"
            + "4. 海盗战斗：点击海盗船即可锁定并自动开炮。再次点击取消锁定，也可以驶出战斗范围逃跑。\n"
            + "5. 船只成长：耐久代表船的生命；仓库升级可增加货舱容量；编制升级后可以雇佣更多船员；炮火和船员会提高开炮速度。\n"
            + "6. 补给与存档：回港可补给、修船、升级和保存进度。没钱补给时可以借债，但每次靠港会增加 2% 利息。\n"
            + "7. 地图：点击右上角小地图打开全图，再点港口或岛屿，船会自动驶向目标。";

    private final NanHaiVoyage game;
    private GameState g;

    private VoyageWorldRenderer world3d;
    private Viewport hudVp;
    private Stage stage;
    private ShapeRenderer shapes;
    private final Vector3 tmp = new Vector3();
    private final GlyphLayout layout = new GlyphLayout();

    private Overlay overlay = Overlay.NONE;
    private int cargoTab;
    private int selectedGood = -1;
    private int selectedBeast = -1;
    private int selectedHerb = -1;
    private int selectedFish = -1;   // 0.26.3 渔获（丢/卖）
    private Overlay priceReturnOverlay = Overlay.CARGO;
    // Popups the player explicitly closed stay closed until the context changes
    // (docking at a different port / reaching a different island / leaving).
    private int dismissedPort = -1;
    private int dismissedIsland = -1;
    private boolean dismissedFail;
    // 0.27.2: proximity hint state — each nearby port/island nudges the player
    // to TAP the icon once; the hint re-arms only after leaving its range.
    private int hintPort = -1;
    private int hintIsland = -1;
    private int selectedQuest = -1;
    private QuestUi questUi;
    private QuestUi intelUi;
    private ShopShipsPanel shopPanel;
    private MyShipPanel myShipPanel;
    private CaptainMenuPanel captainPanel;
    private PortDockPanel portPanel;
    private QuestUi cargoUi;
    private QuestUi pageUi;
    private ScrollPane cargoListPane;
    private int cargoListTab;
    private final float[] cargoScroll = new float[4];
    private CodexPanel codexPanel;
    private ScrollPane questListPane;
    private float questScrollY;
    private int selectedShip = -1;       // 0.26.4 商城：网格里点开的船（-1 = 网格）
    private int shopCat = 0;             // 船只 / 战船 / 货船 / 特种船
    private boolean logoutSwitching;     // 0.26.6 退出登录防重入
    private VoyageHud voyageHud;
    private final int[] trackedQuests = {-1, -1};
    private Label hudClock;              // 0.26.4 左下 第N日 HH:MM 白天/夜晚
    private Label sunTip;                // 今日：晴 label under the minimap

    // 0.26.0 市场 pagination: rows per page in each market column, plus the two
    // independent page cursors (buy column pages over goods, sell column pages
    // over the dynamic cargo list). Kept on the screen, not the model.
    private static final int MARKET_PAGE_SIZE = 7;
    private int marketBuyPage;
    private int marketSellPage;

    private Table menuRoot;
    private Label hudLine;
    private Label[] statVals = new Label[4];   // live numbers in the top stat cells
    private Table questCard;                   // 0.26.5 active-quest bordered card
    private Label questCardLabel;               // its 标题（进度） text
    private TextButton btnCancelAuto;
    private TextButton btnLockPirate;
    private TextButton btnCancelLock;
    private TextButton btnAccel;
    private TextButton btnDecel;
    // Stat-detail popup state (Overlay.STAT): 0=银两 1=补给 2=耐久 3=船员
    private int statDetail;
    private static final String[] STAT_SLUGS = {"silver", "supply", "hull", "crew"};
    private static final String[] STAT_NAMES = {"银两", "补给", "耐久", "船员"};

    private boolean stickActive;
    private float stickCX = 272f * 2f/3f, stickCY = 232f * 2f/3f, stickR = 152f * 2f/3f;
    private float stickKX, stickKY;
    private int stickPointer = -1;
    private int lookPointer = -1;
    private float lookX, lookY;
    private boolean accelDown, decelDown;
    private boolean loggedFirstFrame;
    private float radarT;   // seconds the full map has been open (radar pulse clock)
    private Pixmap miniChartPm; // 0.26.5 baked minimap chart (promoted to a texture)
    private Texture miniChartTex;
    private WorldMapOverlay worldMap; // Full-map resources, independent of the minimap
    private static final int miniHalf = 80;

    public VoyageScreen(NanHaiVoyage game) {
        this.game = game;
    }

    @Override
    public void show() {
        // Error-level milestones: every step is visible in logcat on Android so
        // a crash during the login transition can be pinned to the exact step
        // instead of being swallowed by a blanket catch.
        Gdx.app.error("VoyageEnter", "show() begin, game.state=" + (game.state == null ? "null" : "ok"));
        try {
            buildAll();
            Gdx.app.error("VoyageEnter", "buildAll() completed");
        } catch (Throwable t) {
            // show() runs inside setScreen during the login transition; it must
            // never kill the process. Surface the error on the HUD instead.
            Gdx.app.error("VoyageScreen", "show failed", t);
            disposeQuietly();
            g = (g != null) ? g : GameState.newGame();
            hudVp = new ExtendViewport(HUD_W, HUD_H);
            stage = new Stage(hudVp, game.batch);
            shapes = new ShapeRenderer();
            world3d = null; // HUD remains usable if the GPU cannot initialize the world.
            buildHud();
            Gdx.input.setInputProcessor(new InputMultiplexer(new WorldInput(), stage));
            overlay = Overlay.NONE;
            rebuildMenu();
            g.toast("画面组件加载失败，请退出后重试。");
        }
        if (game.state != null) {
            g = game.state;
        }
        Gdx.app.error("VoyageEnter", "show() end");
    }

    private void buildAll() {
        g = game.state;
        world3d = new VoyageWorldRenderer();
        Gdx.app.error("VoyageEnter", "perspective chase renderer created");
        hudVp = new ExtendViewport(HUD_W, HUD_H);
        stage = new Stage(hudVp, game.batch);
        Gdx.app.error("VoyageEnter", "hud viewport+stage created");
        shapes = new ShapeRenderer();
        Gdx.app.error("VoyageEnter", "3D world + HUD shape renderer created");

        buildHud();
        // World hit-testing goes first. It consumes only joystick/minimap/enemy
        // hits and otherwise falls through to Scene2D, so a full-parent HUD Table
        // cannot swallow pirate taps on Android.
        InputMultiplexer mux = new InputMultiplexer(new WorldInput(), stage);
        Gdx.input.setInputProcessor(mux);

        if (g.failed) {
            overlay = Overlay.FAIL;
        } else if (g.dockedPort >= 0) {
            overlay = Overlay.PORT;
        } else if (g.islandMenu >= 0) {
            overlay = Overlay.ISLAND;
        }
        rebuildMenu();
        maybeShowHowto();
        Gdx.app.error("VoyageEnter", "hud + menu built, overlay=" + overlay);
    }

    /** 0.26.0 first-run tutorial: the 玩法说明 popup appears once per install
     * after the first successful login (VoyageScreen only exists post-login).
     * The persistent flag lives in libGDX Preferences under key howto_shown;
     * both closing buttons record it, so the popup never nags again. */
    private void maybeShowHowto() {
        try {
            if (!Gdx.app.getPreferences("nanhai-voyage").getBoolean("howto_shown", false)) {
                overlay = Overlay.HOWTO;
                rebuildMenu();
            }
        } catch (Throwable ignored) {
            // Preferences failure must never block the voyage screen.
        }
    }

    private void disposeMiniChart() {
        try {
            if (miniChartTex != null) { miniChartTex.dispose(); }
        } catch (Throwable ignored) {}
        try {
            if (miniChartPm != null) { miniChartPm.dispose(); }
        } catch (Throwable ignored) {}
        try {
            if (worldMap != null) { worldMap.dispose(); }
        } catch (Throwable ignored) {}
        miniChartTex = null;
        miniChartPm = null;
        worldMap = null;
    }

    private void disposeQuietly() {
        if (voyageHud != null) { voyageHud.dispose(); voyageHud = null; }
        if (world3d != null) { world3d.dispose(); world3d = null; }
        if (questUi != null) { questUi.dispose(); questUi = null; }
        if (intelUi != null) { intelUi.dispose(); intelUi = null; }
        if (shopPanel != null) { shopPanel.dispose(); shopPanel = null; }
        if (myShipPanel != null) { myShipPanel.dispose(); myShipPanel = null; }
        if (captainPanel != null) { captainPanel.dispose(); captainPanel = null; }
        if (portPanel != null) { portPanel.dispose(); portPanel = null; }
        if (cargoUi != null) { cargoUi.dispose(); cargoUi = null; }
        if (pageUi != null) { pageUi.dispose(); pageUi = null; }
        cargoListPane = null;
        if (codexPanel != null) { codexPanel.dispose(); codexPanel = null; }
        questListPane = null;
        try {
            if (stage != null) { stage.dispose(); }
        } catch (Throwable ignored) {}
        try {
            if (shapes != null) { shapes.dispose(); }
        } catch (Throwable ignored) {}
        disposeMiniChart();
        stage = null;
        shapes = null;
    }

    // Active-quest card: a bordered frame showing 标题 + 进度（如 访问一个岛屿（0/1））.
    // 0.26.5 hung it under the rail row next to the minimap; 0.26.6 moves it to
    // the FAR RIGHT edge, mid/lower down the screen — clear of the round minimap
    // (top-right), the top icon rail, the bottom-right 加速/减速 buttons and the
    // bottom-left joystick. QP_X/QP_Y are the frame's bottom-left stage corner.
    private static final float QP_W = 248f;
    private static final float QP_H = 54f;
    private static final float QP_X = HUD_W - 18f - QP_W;   // far-right edge
    private static final float QP_Y = 288f;                 // lower-middle band

    private void buildHud() {
        stage.clear();
        if (voyageHud != null) voyageHud.dispose();
        Runnable world = () -> {
            if (overlay == Overlay.NONE) { overlay = Overlay.MAP; rebuildMenu(); }
        };
        Runnable[] shortcuts = {this::toggleCargo, this::toggleCodex, this::toggleShop,
                () -> {
                    if (g.dockedPort >= 0) {
                        overlay = Overlay.MARKET; marketBuyPage = marketSellPage = 0; rebuildMenu();
                    } else { contextReopen(); }
                }, this::toggleQuestOverlay,
                () -> g.toast("活动尚未开放，敬请期待。"),
                () -> { overlay=Overlay.REDEEM; rebuildMenu(); }};
        voyageHud = new VoyageHud(game.skin, this::toggleAvatar, this::statClicked, shortcuts,
                world, this::toggleMine, this::openIntel, this::contextReopen,
                () -> { if (g.autoSail) { g.cancelAutoSail(); rebuildMenu(); } else world.run(); },
                () -> { g.cancelAutoSail(); rebuildMenu(); }, () -> g.lockPirate(), () -> g.cancelLock(),
                card -> {
                    if (trackedQuests[card] >= 0) selectedQuest = trackedQuests[card];
                    toggleQuestOverlay();
                });
        stage.addActor(voyageHud);
        applyHudScale(); // 0.27.4: scale the 1920×1080 HUD grid over the full viewport
        statVals = voyageHud.stats; hudLine = voyageHud.status; hudClock = voyageHud.clock;
        btnAccel = voyageHud.accel; btnDecel = voyageHud.decel;
        btnCancelAuto = voyageHud.cancelAuto; btnLockPirate = voyageHud.lock; btnCancelLock = voyageHud.cancelLock;
        hold(btnAccel, true); hold(btnDecel, false);
        menuRoot = new Table(); menuRoot.setFillParent(true);
        menuRoot.setTouchable(Touchable.childrenOnly);
        menuRoot.center().top().padTop(78); stage.addActor(menuRoot);
    }

    /** Keep the full HUD visible and steering hit areas aligned on wide devices. */
    private void applyHudScale() {
        if (voyageHud == null || hudVp == null) return;
        voyageHud.layoutViewport(hudVp.getWorldWidth(), hudVp.getWorldHeight());
        float scale = voyageHud.getScaleX();
        stickCX = 272f * scale;
        stickCY = 232f * scale;
        stickR = 152f * scale;
    }

    /** Tap a top stat cell: opens (or switches) the detail popup for that stat. */
    private void statClicked(int idx) {
        if (overlay == Overlay.STAT && statDetail == idx) {
            closePopup();
        } else if (overlay != Overlay.MAP) {
            statDetail = idx;
            overlay = Overlay.STAT;
            rebuildMenu();
        }
    }

    /** Avatar opens the paused 船长菜单 (account menu) only from a neutral HUD. */
    private void toggleAvatar() {
        if (overlay == Overlay.AVATAR) {
            closePopup();
        } else if (overlay == Overlay.NONE) {
            if (captainPanel != null) captainPanel.openProfile();
            overlay = Overlay.AVATAR;
            rebuildMenu();
        }
    }

    private void toggleCargo() {
        if (overlay == Overlay.CARGO) {
            closePopup();
        } else if (overlay != Overlay.MAP) {
            overlay = Overlay.CARGO;
            rebuildMenu();
        }
    }

    private void toggleCodex() {
        if (overlay == Overlay.CODEX) {
            closePopup();
        } else if (overlay != Overlay.MAP) {
            overlay = Overlay.CODEX;
            rebuildMenu();
        }
    }

    /** 港口/岛屿 icon: reopen the context menu of wherever the ship is. */
    private void contextReopen() {
        if (overlay == Overlay.PORT && g.dockedPort >= 0) {
            closePopup();
        } else if (overlay == Overlay.ISLAND && g.islandMenu >= 0) {
            closePopup();
        } else if (g.dockedPort >= 0) {
            dismissedPort = -1; // manual reopen re-enables auto-open rules
            overlay = Overlay.PORT;
            rebuildMenu();
        } else if (g.islandMenu >= 0) {
            dismissedIsland = -1;
            overlay = Overlay.ISLAND;
            rebuildMenu();
        } else {
            // 0.27.2: 关闭菜单后船仍留在原地；站在港口/岛屿范围内再点图标可重开。
            int np = g.nearestPortInRange();
            if (np >= 0) {
                g.dock(np);
                overlay = Overlay.PORT;
                persist();
                rebuildMenu();
                return;
            }
            int ni = g.nearestIslandInRange();
            if (ni >= 0) {
                g.enterIsland(ni);
                overlay = Overlay.ISLAND;
                rebuildMenu();
                return;
            }
            g.toast("不在港口或岛屿附近：需先驶近，再点击图标打开菜单。");
        }
    }

    private void openIntel() {
        if (overlay == Overlay.INTEL) {
            closePopup();
        } else if (overlay != Overlay.MAP) {
            overlay = Overlay.INTEL;
            markIntelViewed();
            rebuildMenu();
        }
    }

    /** Highlight for the currently selected quest row (0.26.6): the same
     * gold-rimmed 9-patch used by the active-quest card, so the selected quest
     * reads clearly against the TextButton rows. */
    private Drawable selectedRowBg() {
        Drawable base = game.skin.getDrawable("patch");
        if (base instanceof NinePatchDrawable) {
            NinePatchDrawable nd = (NinePatchDrawable) base;
            return nd.tint(new Color(0.66f, 0.55f, 0.24f, 1f));
        }
        return base;
    }

    /** The round rail 任务 icon and the active-quest card both open/close the
     * quest popup. */
    private void toggleQuestOverlay() {
        if (overlay == Overlay.QUESTS) {
            closePopup();
        } else if (overlay != Overlay.MAP) {
            overlay = Overlay.QUESTS;
            // Show the current task's detail by default, not the empty pane.
            if (selectedQuest < 0 || selectedQuest >= QUESTS.length) {
                selectedQuest = getActiveQuestIndex();
            }
            rebuildMenu();
        }
    }

    /** 0.26.5 我的：当前船 + 已拥有船只（随时免费换乘）+ 资源/货物汇总。 */
    private void toggleMine() {
        if (overlay == Overlay.MINE) {
            closePopup();
        } else if (overlay != Overlay.MAP) {
            overlay = Overlay.MINE;
            rebuildMenu();
        }
    }

    /** 0.26.4 商城：打开时若在网格则默认停在网格，点过船则回到那条船的详情。 */
    private void toggleShop() {
        if (overlay == Overlay.SHOP) {
            closePopup();
        } else if (overlay != Overlay.MAP) {
            overlay = Overlay.SHOP;
            shopCat = 0;
            rebuildMenu();
        }
    }

    private void openShipDetail(int i) {
        selectedShip = i;
        rebuildMenu();
    }

    /** Hold-to-keep listeners for 加速/减速. Press state is tracked directly (not
     * via the button's isPressed in the render loop) so a long press survives
     * drag jitter, and the pressed visual always shows — even when the world is
     * paused at port the button itself still reacts and explains why the ship
     * does not move yet. */
    private void hold(TextButton b, boolean accel) {
        b.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                boolean modal = overlay != Overlay.NONE;
                if (modal) {
                    // A popup is open: the world pauses by design. The button still
                    // reacts (pressed feedback) and explains instead of silently
                    // doing nothing — never a dead control.
                    if (overlay == Overlay.AVATAR) {
                        g.toast("船长菜单开着，世界暂停：先点「关闭」再开船。");
                    } else if (overlay == Overlay.MARKET) {
                        g.toast("市场开着：点「返回」回港口菜单，再点「离港」开船。");
                    } else if (overlay == Overlay.HOWTO) {
                        g.toast("玩法说明开着：点「开始航行」或「以后不再提示」继续。");
                    } else {
                        g.toast("港口/岛屿菜单开着世界暂停：先「离港/离开岛屿」或「关闭」再开船。");
                    }
                    return true;
                }
                // No popup open: not paused. If the ship is still flagged docked
                // (docked save whose port menu was closed), undock so the input
                // actually sails instead of being swallowed by the model pause.
                undockIfNeeded();
                if (accel) {
                    accelDown = true;
                    g.holdAccel = true;
                } else {
                    decelDown = true;
                    g.holdDecel = true;
                }
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (accel) {
                    accelDown = false;
                    g.holdAccel = false;
                } else {
                    decelDown = false;
                    g.holdDecel = false;
                }
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                // keep the hold while the finger stays on the button
            }
        });
    }

    /** 0.25.2 docked-save lockup fix: the world pauses only while the port/island
     * popup is OPEN. If the ship is docked (or at an island) with the popup closed
     * — e.g. right after loading a docked save and closing the menu — touching the
     * joystick or 加速/减速 undocks first, so the controls are never dead and the
     * ship really sails. Without this, worldPaused() (dockedPort >= 0) silently
     * swallowed every control and the speed stayed 0. */
    private void undockIfNeeded() {
        if (g.dockedPort >= 0) {
            g.leavePort();
            persist();
            dismissedPort = -1;
            Gdx.app.error("VoyageScreen", "undocked via controls (port menu closed), leaving "
                    + Catalog.PORTS[g.lastPort]);
            g.toast("港口菜单未开：直接开船离港。");
        } else if (g.islandMenu >= 0) {
            g.leaveIsland();
            dismissedIsland = -1;
            g.toast("岛屿菜单未开：直接开船离岛。");
        }
    }

    private ClickListener click(Runnable r) {
        return new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                r.run();
            }
        };
    }

    // ------------------------------------------------------------- popups

    /** Closes whatever popup is open. 0.27.2: closing a port/island menu undocks
     * / leaves the island IN PLACE — the ship is never teleported or snapped, and
     * the world resumes simulating from exactly where it was. */
    private void closePopup() {
        if (overlay == Overlay.REDEEM) Gdx.input.setOnscreenKeyboardVisible(false);
        if (overlay == Overlay.AVATAR && captainPanel != null) captainPanel.commitNickname();
        if (overlay == Overlay.PORT && g.dockedPort >= 0) {
            g.undockInPlace();
            persist();
            dismissedPort = -1;
        } else if (overlay == Overlay.ISLAND && g.islandMenu >= 0) {
            g.leaveIslandInPlace();
            dismissedIsland = -1;
        } else if (overlay == Overlay.FAIL) {
            dismissedFail = true;
        } else if (overlay == Overlay.PORT || overlay == Overlay.ISLAND) {
            // port/island disappeared while the popup was up; reset dismissals
            dismissedPort = -1;
            dismissedIsland = -1;
        }
        overlay = Overlay.NONE;
        rebuildMenu();
    }

    private void rebuildMenu() {
        if (overlay != Overlay.NONE) releaseWorldControls();
        updateQuestButtonLabel();
        if (questListPane != null) questScrollY = questListPane.getScrollY();
        questListPane = null;
        if (cargoListPane != null) cargoScroll[cargoListTab] = cargoListPane.getScrollY();
        cargoListPane = null;
        menuRoot.clear();
        stage.setKeyboardFocus(null); stage.setScrollFocus(null);
        boolean fullscreen = isFullscreenOverlay();
        menuRoot.pad(fullscreen ? 0 : 78,0,0,0);
        menuRoot.setTouchable(overlay == Overlay.NONE || overlay == Overlay.MAP ? Touchable.childrenOnly : Touchable.enabled);
        voyageHud.setVisible(!fullscreen);
        if (overlay == Overlay.NONE || overlay == Overlay.MAP) {
            return;
        }
        if (overlay == Overlay.PORT) {
            if (g.dockedPort < 0) { overlay = Overlay.NONE; return; }
            if (portPanel == null) portPanel = new PortDockPanel(game.skin);
            portPanel.refresh(g, this::portAction, this::closePopup);
            portPanel.setSize(PortDockPanel.WIDTH, PortDockPanel.HEIGHT);
            portPanel.setTransform(true);
            portPanel.setScale(2f / 3f);
            Group holder = new Group();
            holder.addActor(portPanel);
            menuRoot.add(holder).size(PortDockPanel.WIDTH * 2f / 3f,
                    PortDockPanel.HEIGHT * 2f / 3f).padTop(16);
            return;
        }
        if (overlay == Overlay.AVATAR) {
            if (captainPanel == null) captainPanel = new CaptainMenuPanel(game.skin,g,this::persist,
                    this::saveNow,this::tryReloadLatestSave,this::logoutToLogin,this::confirmFillAccount,this::closePopup);
            else captainPanel.refresh(g);
            showFullscreen(captainPanel,CaptainMenuPanel.WIDTH,CaptainMenuPanel.HEIGHT);
            return;
        }
        if (overlay == Overlay.MINE) {
            if (myShipPanel == null) myShipPanel = new MyShipPanel(game.skin);
            myShipPanel.refresh(g, shipAttrLine(g.ship), idx -> {
                g.toast(g.equipShip(idx)); persist(); rebuildMenu();
            }, this::closePopup);
            showFullscreen(myShipPanel,MyShipPanel.WIDTH,MyShipPanel.HEIGHT);
            return;
        }
        if (overlay == Overlay.SHOP) {
            if (shopPanel == null) shopPanel = new ShopShipsPanel(game.skin);
            int idx = selectedShip;
            boolean detail = idx >= 0 && idx < Catalog.SHIPS.length;
            shopPanel.refresh(g, shopCat, idx, detail ? shipAttrLine(idx) : "",
                    detail ? shipEffectLine(idx) : "",
                    category -> { shopCat = category; selectedShip = -1; rebuildMenu(); },
                    this::openShipDetail,
                    () -> { selectedShip = -1; rebuildMenu(); },
                    () -> { g.toast(g.buyShip(idx)); persist(); rebuildMenu(); },
                    () -> { g.toast(g.equipShip(idx)); persist(); rebuildMenu(); },
                    this::closePopup);
            showFullscreen(shopPanel,ShopShipsPanel.WIDTH,ShopShipsPanel.HEIGHT);
            return;
        }
        if (overlay == Overlay.CARGO) {
            if (cargoUi == null) cargoUi = new QuestUi(game.skin);
            Table cargo = new Table();
            cargoTable(cargo);
            showFullscreen(cargo,1296,784);
            cargo.validate();
            cargoListPane.setScrollY(cargoScroll[cargoTab]);
            cargoListPane.updateVisualScroll();
            return;
        }
        if (overlay == Overlay.CODEX) {
            if (codexPanel == null) codexPanel = new CodexPanel(game.skin, g, this::closePopup);
            else codexPanel.refresh(g);
            showFullscreen(codexPanel,1328,784);
            return;
        }
        if (overlay == Overlay.INTEL) {
            if (intelUi == null) intelUi = new QuestUi(game.skin);
            Table intel = intelTable();
            showFullscreen(intel,1200,736);
            return;
        }
        if (overlay == Overlay.QUESTS) {
            if (questUi == null) questUi = new QuestUi(game.skin);
            Table quests = new Table();
            questsTable(quests);
            // The overlay uses design pixels, leaving the existing HUD viewport intact.
            showFullscreen(quests,1152,752);
            quests.validate();
            if (questListPane != null) {
                questListPane.setScrollY(questScrollY);
                questListPane.updateVisualScroll();
            }
            return;
        }
        if (isFullscreenOverlay()) {
            if (pageUi == null) pageUi = new QuestUi(game.skin);
            Table page;
            if (overlay == Overlay.FISH) page = fishingPage();
            else if (overlay == Overlay.PRICE) page = pricePage();
            else if (overlay == Overlay.STAT) page = statPage();
            else if (overlay == Overlay.FAIL) page = failurePage();
            else if (overlay == Overlay.REDEEM) page = redeemPage();
            else page = howtoPage();
            showFullscreen(page,1296,800);
            return;
        }
        Table box = new Table(game.skin);
        box.pad(8f); box.background(game.skin.getDrawable("panel"));
        if (overlay == Overlay.MARKET) marketTable(box);
        else if (overlay == Overlay.ISLAND) islandTable(box);
        ScrollPane sp = new ScrollPane(box,game.skin); sp.setFadeScrollBars(false);
        menuRoot.add(sp).width(overlay == Overlay.MARKET ? PORT_MENU_W+24f : 520f).maxHeight(560);
    }

    private boolean isFullscreenOverlay() {
        return overlay != Overlay.NONE && overlay != Overlay.MAP && overlay != Overlay.PORT
                && overlay != Overlay.ISLAND && overlay != Overlay.MARKET;
    }

    private void showFullscreen(Table content,float width,float height) {
        readableLabels(content);
        FullscreenPage page = new FullscreenPage(content,width,height);
        menuRoot.add(page).grow();
        menuRoot.validate(); page.validate();
    }

    private void readableLabels(Actor actor) {
        if (actor instanceof Label) {
            Label label = (Label)actor;
            float base = label.getStyle().font == game.fontSmall ? 16f : 22f;
            label.setFontScale(Math.max(label.getFontScaleX(),24f/base));
        } else if (actor instanceof Group) {
            for (Actor child : ((Group)actor).getChildren()) readableLabels(child);
        }
    }

    private Table pageFrame(String title,Table content,Runnable close) {
        Table page=new Table(); page.setBackground(pageUi.frame); page.pad(28).top();
        Table header=new Table();
        header.add(pageUi.label(title,36,QuestUi.PAPER)).expandX().left();
        header.add(pageAction("关闭",true,close)).size(112,64);
        page.add(header).growX().height(72).padBottom(24).row();
        ScrollPane scroll=new ScrollPane(content,game.skin);
        scroll.setName("pageScroll"); scroll.setScrollingDisabled(true,false);
        scroll.setFadeScrollBars(false); scroll.setOverscroll(false,false);
        page.add(scroll).grow().row();
        Label notice=pageUi.label("世界暂停 · 关闭后继续航行",24,QuestUi.JADE);
        notice.setName("pageNotice");
        page.add(notice).growX().height(48).padTop(16).left();
        return page;
    }

    private TextButton pageAction(String title,boolean primary,Runnable action) {
        TextButton button=pageUi.button(title,primary); button.setName(title);
        button.getLabel().setFontScale(28f/22f); button.addListener(click(action));
        return button;
    }
    private Label pageCopy(String text) {
        Label label=pageUi.label(text,28,QuestUi.PAPER); label.setWrap(true); return label;
    }
    private void dismissHowto() {
        Gdx.app.getPreferences("nanhai-voyage").putBoolean("howto_shown",true).flush();
        closePopup();
    }
    private Table redeemPage() {
        Table content=new Table(); content.top().left();
        content.add(pageCopy("输入兑换码领取银两。每个兑换码在当前存档中仅可领取一次。")).width(1080).left().padBottom(32).row();
        TextField.TextFieldStyle style=new TextField.TextFieldStyle(game.skin.get(TextField.TextFieldStyle.class));
        style.background=pageUi.blue; style.fontColor=QuestUi.PAPER;
        TextField code=new TextField("",style); code.setName("兑换码"); code.setMessageText("请输入兑换码"); code.setMaxLength(32);
        content.add(code).size(720,80).left().padBottom(28).row();
        Label result=pageCopy("兑换奖励将自动保存。"); result.setName("兑换结果");
        content.add(pageAction("确认兑换",true,()->{
            if(g.redeemCode(code.getText())) persist();
            result.setText(g.toast);
            stage.setKeyboardFocus(null); Gdx.input.setOnscreenKeyboardVisible(false);
        })).size(320,72).left().padBottom(28).row();
        content.add(result).width(1080).left();
        return pageFrame("福利 · 兑换码",content,this::closePopup);
    }

    private Table howtoPage() {
        Table content=new Table(); content.top().left();
        for(String paragraph:HOWTO_BODY.split("\\n")) {
            if(paragraph.isEmpty())continue;
            Table row=new Table(); row.top().left();
            if(paragraph.length()>3 && Character.isDigit(paragraph.charAt(0))) {
                row.add(pageUi.label(paragraph.substring(0,2),28,QuestUi.JADE)).width(48).top().left();
                row.add(pageCopy(paragraph.substring(3))).width(1120).top().left();
            } else row.add(pageCopy(paragraph)).width(1168).left();
            content.add(row).width(1168).left().padBottom(24).row();
        }
        content.add(pageCopy("拖动空白海面可临时环顾四周，松手后视角自动回到船后。摇杆左右控制转向。"))
                .width(1168).left().padBottom(28).row();
        Table actions=new Table();
        actions.add(pageAction("开始航行",true,this::dismissHowto)).size(360,64).padRight(24);
        actions.add(pageAction("以后不再提示",false,this::dismissHowto)).size(360,64);
        Table page=pageFrame("南海航程玩法",content,this::dismissHowto);
        page.row();page.add(actions).height(72).left();
        return page;
    }
    private Table failurePage() {
        Table content=new Table(); content.top().left();
        content.add(new Image(IconLib.ship(g.ship))).size(136).left().padBottom(24).row();
        content.add(pageUi.label(failCauseLabel(g.failReason),40,QuestUi.PAPER)).left().padBottom(20).row();
        content.add(pageCopy("航程失败。读取最近一次保存的进度，或重新开始航程。")).width(1100).left().padBottom(32).row();
        Table actions=new Table();
        actions.add(pageAction("读取存档",true,this::tryReloadLatestSave)).size(360,72).padRight(24);
        actions.add(pageAction("重新开始",false,this::restartNewGame)).size(360,72);
        content.add(actions).left().padBottom(32).row();
        content.add(pageCopy("重新开始：银两1000、补给500、耐久500、空货舱。新进度会覆盖本账号的旧存档。"))
                .width(1100).left();
        return pageFrame("航程失败",content,this::closePopup);
    }
    private Table statPage() {
        statDetail=MathUtils.clamp(statDetail,0,3);
        Table content=new Table(); content.top().left();
        Table nav=new Table(); nav.top().setBackground(pageUi.inset); nav.pad(16);
        for(int i=0;i<4;i++) {
            final int index=i;
            nav.add(pageAction(STAT_NAMES[i],statDetail==i,()->{statDetail=index;rebuildMenu();})).size(208,72).padBottom(16).row();
        }
        Table detail=new Table(); detail.setBackground(pageUi.inset); detail.pad(32).top().left();
        String[] values={g.silver+" 两",(int)g.supply+" / "+(int)g.supplyMax,(int)g.hull+" / "+(int)g.hullMax,g.crew+" / "+g.crewMax()};
        String[] descriptions={
            "贸易的本钱。低买高卖、卖出异兽和草药、击败海盗都能赚取银两。升级、雇人、补给、修船需要花费银两。欠债后每次回港计息2%。",
            "航行时按船员人数消耗，耗尽则航程失败。靠港可补满补给，按缺口花费银两；银两不足可以借债。",
            "船体的生命。遭到炮击会损失耐久，归零则船沉失败。靠港可花费银两修满；与海盗交手时可拉开距离或击沉对方。",
            "船员越多，开炮越快，补给也消耗得更快。先在港口升级编制，再雇人上船。"};
        detail.add(new Image(IconLib.hud(STAT_SLUGS[statDetail]))).size(128).left().padBottom(24).row();
        detail.add(pageUi.label("当前："+values[statDetail],36,QuestUi.PAPER)).left().padBottom(32).row();
        detail.add(pageCopy(descriptions[statDetail])).width(800).left().top().row();
        content.add(nav).width(240).height(512).top().padRight(24);
        content.add(detail).width(912).height(512).top();
        return pageFrame(STAT_NAMES[statDetail]+" · 说明",content,this::closePopup);
    }
    private Table pricePage() {
        Table content=new Table(); content.top().left();
        if(selectedGood<0) {
            content.add(pageCopy("先在货舱选择一种商货。")).width(1120);
            return pageFrame("各港行情",content,()->{overlay=priceReturnOverlay;rebuildMenu();});
        }
        int good=selectedGood;
        for(int i=0;i<Catalog.PORTS.length;i++) {
            Table row=new Table(); row.setBackground(pageUi.inset); row.pad(12);
            row.add(pageUi.label(Catalog.PORTS[i]+(i==g.dockedPort?" · 本港":""),26,QuestUi.PAPER)).width(184).left();
            row.add(pageUi.label(g.goodPrice(i,good)+" 两",28,QuestUi.JADE)).width(150).right();
            content.add(row).size(376,64).pad(6);
            if(i%3==2) content.row();
        }
        content.row(); Table actions=new Table();
        TextButton buy=pageAction("买 1",true,()->{g.toast(g.buyGood(g.dockedPort,good,1));persist();rebuildMenu();});
        TextButton sell=pageAction("卖 1",false,()->{g.toast(g.sellGood(g.dockedPort,good,1));persist();rebuildMenu();});
        buy.setDisabled(g.dockedPort<0);sell.setDisabled(g.dockedPort<0||g.trade[good]<=0);
        actions.add(buy).size(256,64).padRight(16); actions.add(sell).size(256,64).padRight(16);
        actions.add(pageAction("返回列表",false,()->{overlay=priceReturnOverlay;rebuildMenu();})).size(256,64);
        content.add(actions).colspan(3).left().padTop(20);
        return pageFrame(Catalog.GOODS[good]+" · 各港行情",content,()->{overlay=priceReturnOverlay;rebuildMenu();});
    }
    private Table fishingPage() {
        Table content=new Table();content.top().left();
        Runnable back=()->{overlay=g.dockedPort>=0?Overlay.PORT:Overlay.NONE;rebuildMenu();};
        if(g.dockedPort!=Catalog.YANGZHOU) {
            content.add(pageCopy("请先停靠扬州，再雇渔夫捕鱼。")).width(1120);
            return pageFrame("扬州 · 渔务",content,back);
        }
        content.add(pageUi.label("银两 "+g.silver+"    舱 "+g.cargoUsed()+"/"+g.holdCap()
                +"    渔夫 "+g.fishers+"/"+g.fisherCap(),28,QuestUi.PAPER)).colspan(2).left().height(56).row();
        Table actions=new Table();actions.top().left().pad(20);actions.setBackground(pageUi.inset);
        actions.add(pageAction("雇渔夫 "+Catalog.FISHER_HIRE_COST+"两",true,()->{
            g.toast(g.hireFisher());persist();rebuildMenu();})).size(512,64).padBottom(12).row();
        actions.add(pageAction("升钓具 Lv"+g.fishToolLevel+" · "+g.fishToolCost()+"两",false,()->{
            g.toast(g.upgradeFishTool());persist();rebuildMenu();})).size(512,64).padBottom(12).row();
        actions.add(pageAction("升钓技 Lv"+g.fishSkillLevel+" · "+g.fishSkillCost()+"两",false,()->{
            g.toast(g.upgradeFishSkill());persist();rebuildMenu();})).size(512,64).padBottom(12).row();
        actions.add(pageAction("升渔夫编制 · "+g.fisherCapCost()+"两",false,()->{
            g.toast(g.upgradeFisherCap());persist();rebuildMenu();})).size(512,64).padBottom(12).row();
        actions.add(pageAction(g.fishingOn?"停止捕鱼":"开始捕鱼",true,()->{
            g.toast(g.fishingOn?g.stopFishing():g.startFishing());persist();rebuildMenu();})).size(512,64).padBottom(20).row();
        actions.add(pageCopy(g.fishingOn?"捕鱼中：约每"+(int)Math.ceil(g.catchInterval())+"秒一条。":"渔夫待命。点「开始捕鱼」自动下竿。")).width(512).left().row();
        Table catches=new Table();catches.pad(20).top().left();catches.setBackground(pageUi.inset);
        catches.add(pageUi.label("渔获 · 累计 "+g.fishCaughtTotal+" 条",28,QuestUi.PAPER)).left().padBottom(20).row();
        if(g.fishTotal()==0)catches.add(pageCopy("船上还没有鱼。")).width(528).left().row();
        for(int i=0;i<Catalog.FISH.length;i++) {
            if(g.fish[i]<=0)continue;final int fish=i;
            Table row=new Table();row.add(new Image(IconLib.fish(i))).size(40).padRight(12);
            row.add(pageUi.label(Catalog.FISH[i]+" x"+g.fish[i]+" · "+Catalog.FISH_PRICE[i]+"两",24,QuestUi.PAPER)).width(352).left();
            row.add(pageAction("卖1",false,()->{g.toast(g.sellFish(fish,1));persist();rebuildMenu();})).size(112,56);
            catches.add(row).left().padBottom(12).row();
        }
        catches.add(pageCopy("鱼入货舱，占用共用容量。\n可在此卖出，也可到任意港口市场卖出。\n离港后渔夫收网。")).width(528).left().padTop(32);
        content.add(actions).width(552).top().padRight(24);content.add(catches).width(568).top();
        Table page=pageFrame("扬州 · 渔务",content,back);
        page.row();page.add(pageAction("返回",false,back)).size(240,64).left();
        return page;
    }

    /** Popup title row: horizontal title on the left, 关闭 button top-right. */
    private void menuHeader(Table box, String title) {
        float width = overlay == Overlay.PORT ? PORT_MENU_W : (overlay == Overlay.SHOP ? SHOP_W : MENU_W);
        Table h = new Table();
        Label t = new Label(title, game.skin);
        t.setWrap(false);
        TextButton close = new TextButton("关闭", game.skin, "danger");
        close.getLabel().setFontScale(0.9f);
        close.addListener(click(this::closePopup));
        h.add(t).left().expandX().padLeft(2);
        h.add(close).width(88).height(38);
        box.add(h).width(width).padBottom(6).row();
    }

    /** Keep the dock's original economy and two-level navigation behind the new cards. */
    private void portAction(PortDockPanel.Action action) {
        if (g.dockedPort < 0) return;
        switch (action) {
            case MARKET:
                overlay = Overlay.MARKET;
                marketBuyPage = 0;
                marketSellPage = 0;
                rebuildMenu();
                return;
            case FISH:
                if (g.dockedPort == Catalog.YANGZHOU) overlay = Overlay.FISH;
                rebuildMenu();
                return;
            case SUPPLY: g.toast(g.refillSupply()); break;
            case REPAY: g.toast(g.repay(g.debt)); break;
            case REPAIR: g.toast(g.repair()); break;
            case WAREHOUSE: g.toast(g.upgradeWarehouse()); break;
            case CANNON: g.toast(g.upgradeCannon()); break;
            case CREW_CAP: g.toast(g.upgradeCrewCap()); break;
            case HIRE: g.toast(g.hireCrew()); break;
            case LEAVE:
                g.leavePort();
                dismissedPort = -1;
                overlay = Overlay.NONE;
                break;
        }
        persist();
        rebuildMenu();
    }

    /** Port dock popup — layer 2 (市场): two independent paged columns. 本港可买 on
     * the left pages over all goods; 船上可卖 on the right pages over the cargo the
     * player actually carries (goods + beasts + herbs). Each column shows
     * MARKET_PAGE_SIZE rows per page with 上一页/下一页 under it. The 返回 button goes
     * back to the dock actions menu (Overlay.PORT), never straight to 离港. */
    private void marketTable(Table box) {
        if (g.dockedPort < 0) {
            overlay = Overlay.PORT;
            rebuildMenu();
            return;
        }
        int p = g.dockedPort;
        Table h = new Table();
        Label t = new Label(Catalog.PORTS[p] + " · 市场（买卖）", game.skin);
        t.setWrap(false);
        TextButton back = new TextButton("返回", game.skin, "danger");
        back.getLabel().setFontScale(0.9f);
        back.addListener(click(() -> {
            overlay = Overlay.PORT;
            rebuildMenu();
        }));
        h.add(t).left().expandX().padLeft(2);
        h.add(back).width(88).height(38);
        box.add(h).width(PORT_MENU_W).padBottom(4).row();
        box.add(infoRow("银 " + g.silver + "    欠 " + g.debt + "    舱 " + g.cargoUsed() + "/" + g.holdCap()
                + "    「行情」看该货各港价")).width(PORT_MENU_W).padBottom(6).row();

        Table market = new Table();
        // --- left column: goods buyable at this port (24 goods, paged) ---
        Table buy = new Table();
        buy.top().left();
        buy.add(infoRow("本港可买 · 点买入，行情看各港价")).width(430).left().padBottom(4).row();
        int buyPages = Math.max(1, (Catalog.GOODS.length + MARKET_PAGE_SIZE - 1) / MARKET_PAGE_SIZE);
        marketBuyPage = Math.max(0, Math.min(marketBuyPage, buyPages - 1));
        int bStart = marketBuyPage * MARKET_PAGE_SIZE;
        int bEnd = Math.min(Catalog.GOODS.length, bStart + MARKET_PAGE_SIZE);
        for (int i = bStart; i < bEnd; i++) {
            final int good = i;
            Table row = new Table();
            TextureRegionDrawable icon = IconLib.good(good);
            if (icon != null) row.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(icon)).size(26, 26).padRight(4);
            row.add(infoRow(Catalog.GOODS[good] + "  " + g.goodPrice(p, good) + "两")).width(190).left();
            row.add(btn("买1", () -> {
                g.toast(g.buyGood(p, good, 1)); persist(); rebuildMenu();
            })).width(78).height(36).padRight(4);
            row.add(btn("行情", () -> {
                selectedGood = good;
                priceReturnOverlay = Overlay.MARKET;
                overlay = Overlay.PRICE;
                markIntelViewed();
                rebuildMenu();
            })).width(78).height(36);
            buy.add(row).width(430).left().padBottom(2).row();
        }
        pagerRow(buy, marketBuyPage, buyPages, true);

        // --- right column: cargo the player can sell (goods/beasts/herbs/fish, paged) ---
        Table sell = new Table();
        sell.top().left();
        sell.add(infoRow("船上可卖 · 点击卖出 1 件")).width(430).left().padBottom(4).row();
        int n = 0;
        for (int i = 0; i < Catalog.GOODS.length; i++) if (g.trade[i] > 0) n++;
        for (int i = 0; i < Catalog.BEASTS.length; i++) if (g.beasts[i] > 0) n++;
        for (int i = 0; i < Catalog.HERBS.length; i++) if (g.herbs[i] > 0) n++;
        for (int i = 0; i < Catalog.FISH.length; i++) if (g.fish[i] > 0) n++;
        if (n == 0) {
            sell.add(infoRow("船上暂无可卖货物")).width(430).left().padBottom(6).row();
        } else {
            int[] kind = new int[n];  // 0 = goods, 1 = beasts, 2 = herbs, 3 = fish
            int[] idx = new int[n];
            int k = 0;
            for (int i = 0; i < Catalog.GOODS.length; i++) if (g.trade[i] > 0) { kind[k] = 0; idx[k] = i; k++; }
            for (int i = 0; i < Catalog.BEASTS.length; i++) if (g.beasts[i] > 0) { kind[k] = 1; idx[k] = i; k++; }
            for (int i = 0; i < Catalog.HERBS.length; i++) if (g.herbs[i] > 0) { kind[k] = 2; idx[k] = i; k++; }
            for (int i = 0; i < Catalog.FISH.length; i++) if (g.fish[i] > 0) { kind[k] = 3; idx[k] = i; k++; }
            int sellPages = Math.max(1, (n + MARKET_PAGE_SIZE - 1) / MARKET_PAGE_SIZE);
            marketSellPage = Math.max(0, Math.min(marketSellPage, sellPages - 1));
            int sStart = marketSellPage * MARKET_PAGE_SIZE;
            int sEnd = Math.min(n, sStart + MARKET_PAGE_SIZE);
            for (int j = sStart; j < sEnd; j++) {
                final int kj = kind[j];
                final int ij = idx[j];
                String label;
                Runnable sellOne;
                if (kj == 0) {
                    label = "商货 · " + Catalog.GOODS[ij] + " x" + g.trade[ij]
                            + "  卖 " + g.goodPrice(p, ij) + "两";
                    sellOne = () -> { g.toast(g.sellGood(p, ij, 1)); persist(); rebuildMenu(); };
                } else if (kj == 1) {
                    label = "异兽 · " + Catalog.BEASTS[ij] + " x" + g.beasts[ij]
                            + "  卖 " + Catalog.BEAST_PRICE[ij] + "两";
                    sellOne = () -> { g.toast(g.sellBeast(ij, 1)); persist(); rebuildMenu(); };
                } else if (kj == 2) {
                    label = "草药 · " + Catalog.HERBS[ij] + " x" + g.herbs[ij]
                            + "  卖 " + Catalog.HERB_PRICE[ij] + "两";
                    sellOne = () -> { g.toast(g.sellHerb(ij, 1)); persist(); rebuildMenu(); };
                } else {
                    label = "渔获 · " + Catalog.FISH[ij] + " x" + g.fish[ij]
                            + "  卖 " + Catalog.FISH_PRICE[ij] + "两";
                    sellOne = () -> { g.toast(g.sellFish(ij, 1)); persist(); rebuildMenu(); };
                }
                sell.add(btn(label, sellOne)).width(430).height(36).left().padBottom(2).row();
            }
            pagerRow(sell, marketSellPage, sellPages, false);
        }
        market.add(buy).width(438).top();
        market.add(sell).width(438).top().padLeft(12);
        box.add(market).width(PORT_MENU_W).padBottom(4).row();
        box.add(infoRow("返回后仍在港口菜单，点「离港」才开船。")).width(PORT_MENU_W).left().row();
    }

    /** 扬州 dock sub-view：雇渔夫、升钓具/钓技/渔夫编制、开始/停止捕鱼、渔获清单。
     * 只在扬州停靠时出现（渔夫/捕鱼/升级均扬州专属）。渔获与商货/异兽/草药共用
     * 货舱容量，捕鱼规则：停靠扬州且渔夫≥1、点「开始捕鱼」后自动下竿，货舱满自动收网。 */
    /** 属性行：火力(+射速%)/船员上限/货舱。只显示非零加成。 */
    private String shipAttrLine(int idx) {
        StringBuilder sb = new StringBuilder();
        sb.append("火力(射速) +" + Catalog.SHIP_FIRE[idx] + "%");
        sb.append("    船员上限 +" + Catalog.SHIP_CREW[idx]);
        sb.append("    货舱 +" + Catalog.SHIP_HOLD[idx]);
        if (Catalog.SHIP_SPEED[idx] > 0) {
            sb.append("    航速 +" + Catalog.SHIP_SPEED[idx] + "%");
        }
        if (Catalog.SHIP_TURN[idx] > 0) {
            sb.append("    转向 +" + Catalog.SHIP_TURN[idx] + "%");
        }
        return sb.toString();
    }

    /** 实际效果：当前船 → 此船 射速 / 船员上限 / 货舱的变化（按现有配置换算）。 */
    private String shipEffectLine(int idx) {
        float curFire = g.firepower();
        // 估算换船后射速：保留现有炮火/船员，只换 SHIP_FIRE 系数
        float effFire = curFire;
        if (idx >= 0 && idx < Catalog.SHIPS.length) {
            float oldFactor = 1f + Catalog.SHIP_FIRE[g.ship] / 100f;
            float newFactor = 1f + Catalog.SHIP_FIRE[idx] / 100f;
            if (oldFactor > 0f) {
                effFire = curFire * newFactor / oldFactor;
            }
        }
        int effCrewMax = g.crewCap + Catalog.SHIP_CREW[idx];
        int effHold = g.cargoCap + Catalog.SHIP_HOLD[idx];
        return "换乘后效果：射速约 " + Math.round(effFire * 10f) / 10f + " 发/秒"
                + "（当前 " + Math.round(curFire * 10f) / 10f + "）   船员上限 " + effCrewMax
                + "   货舱 " + effHold;
    }

    /** 上一页 / 页号 / 下一页 strip under a paged market column. Buttons no-op at
     * the ends instead of flipping onto an empty page. */
    private void pagerRow(Table col, int page, int pages, boolean isBuy) {
        Table bar = new Table();
        bar.add(btn("上一页", () -> {
            if (isBuy) { if (marketBuyPage > 0) { marketBuyPage--; rebuildMenu(); } }
            else { if (marketSellPage > 0) { marketSellPage--; rebuildMenu(); } }
        })).width(120).height(32);
        Label mid = new Label((page + 1) + " / " + pages, game.skin, "small");
        mid.setAlignment(Align.center);
        bar.add(mid).width(150);
        bar.add(btn("下一页", () -> {
            if (isBuy) { if (marketBuyPage < pages - 1) { marketBuyPage++; rebuildMenu(); } }
            else { if (marketSellPage < pages - 1) { marketSellPage++; rebuildMenu(); } }
        })).width(120).height(32);
        col.add(bar).width(430).padTop(4).row();
    }

    private void islandTable(Table box) {
        menuHeader(box, Catalog.ISLANDS[g.islandMenu] + " · 搜采");
        TextureRegionDrawable art = IconLib.island(g.islandMenu);
        if (art != null) {
            Image portrait = new Image(art);
            portrait.setScaling(Scaling.fit);
            portrait.setName("islandArtwork");
            box.add(portrait).size(MENU_W, 176).padBottom(8).row();
        }
        Table actions = new Table();
        actions.add(btn("搜采", () -> {
            g.toast(g.gatherIsland());
            persist();
            rebuildMenu();
        })).width(240).height(46);
        actions.add(btn("离开岛屿", () -> {
            g.leaveIsland();
            dismissedIsland = -1;
            overlay = Overlay.NONE;
            rebuildMenu();
        })).width(240).height(46).padLeft(10);
        box.add(actions).width(MENU_W).padBottom(5).row();
        box.add(infoRow("靠岸搜采。异兽/草药进图鉴，草药只卖钱。")).width(MENU_W).padBottom(4).row();
        if (g.toastT > 0 && !g.toast.isEmpty()) {
            box.add(wrapLbl(g.toast)).width(MENU_W - 10).left().row();
        }
    }

    private void cargoTable(Table box) {
        box.setBackground(cargoUi.frame);
        box.pad(24, 32, 24, 32);
        Table header = new Table();
        header.add().width(104);
        header.add(cargoLabel("货舱（共用容量 " + g.cargoUsed() + "/" + g.holdCap() + "）", 32, QuestUi.PAPER)).expandX();
        header.add(cargoButton("关闭", false, this::closePopup)).size(104, 48);
        box.add(header).size(1232, 64).row();
        tabs(box);
        Table columns = new Table();
        columns.setBackground(cargoUi.parchment);
        columns.pad(0);
        columns.add(cargoLabel("物品名称", 22, QuestUi.INK)).width(544);
        columns.add(cargoLabel("持有数量", 22, QuestUi.INK)).width(240);
        columns.add(cargoLabel("本港单价", 22, QuestUi.INK)).width(448);
        box.add(columns).size(1232, 40).row();
        Table items = new Table();
        items.top();
        listItems(items, g.dockedPort >= 0);
        if (items.getChildren().size == 0) {
            items.add(cargoLabel("暂无物品", 24, QuestUi.PAPER)).width(1232).height(96);
        }
        cargoListPane = new ScrollPane(items, game.skin);
        cargoListTab = cargoTab;
        cargoListPane.setScrollingDisabled(true, false);
        cargoListPane.setFadeScrollBars(false);
        cargoListPane.setOverscroll(false, false);
        boolean atSea = g.dockedPort < 0;
        box.add(cargoListPane).width(1232).height(atSea ? 480 : 528).row();
        if (atSea) {
            int selected = cargoTab == 0 ? selectedGood : cargoTab == 1 ? selectedBeast
                    : cargoTab == 2 ? selectedHerb : selectedFish;
            String[] names = cargoTab == 0 ? Catalog.GOODS : cargoTab == 1 ? Catalog.BEASTS
                    : cargoTab == 2 ? Catalog.HERBS : Catalog.FISH;
            Table actions = new Table();
            actions.add(cargoLabel(selected >= 0 && selected < names.length ? "选中：" + names[selected] : "先点一种货。", 22, QuestUi.PAPER)).expandX().left();
            TextButton dump = cargoButton("丢掉选中 x1", true, this::dumpSelected);
            dump.setDisabled(selected < 0);
            actions.add(dump).size(232, 40);
            box.add(actions).size(1232, 56).row();
        }
        String note = atSea ? "海上可丢货，丢了就没了。点货物再点丢掉。"
                : cargoTab == 0 ? "点击物品可查看详情与交易信息" : "点击物品卖出1件";
        box.add(cargoLabel(note, 20, QuestUi.PAPER)).height(40).row();
    }

    private Label cargoLabel(String text, int size, Color color) {
        Label label = cargoUi.label(text, size, color);
        label.setAlignment(Align.center);
        return label;
    }

    private TextButton cargoButton(String text, boolean primary, Runnable action) {
        TextButton button = cargoUi.button(text, primary);
        button.addListener(click(action));
        return button;
    }

    private void cargoItemRow(Table box, TextureRegionDrawable icon, String name, int quantity,
                              String price, boolean selected, Runnable action) {
        Table row = new Table();
        row.setBackground(selected ? cargoUi.selected : cargoUi.inset);
        row.pad(0);
        Table item = new Table();
        if (icon != null) item.add(new Image(icon)).size(32).padRight(24);
        item.add(cargoLabel(name, 24, QuestUi.PAPER)).expandX().left();
        row.add(item).width(520).padLeft(24);
        row.add(cargoLabel("持有 " + quantity, 22, quantity > 0 ? QuestUi.JADE : QuestUi.PAPER)).width(240);
        row.add(cargoLabel(price, 22, QuestUi.PAPER)).width(448);
        row.addListener(click(action));
        box.add(row).size(1232, 56).row();
    }

    private void dumpSelected() {
        String m;
        if (cargoTab == 0 && selectedGood >= 0) m = g.dumpTrade(selectedGood, 1);
        else if (cargoTab == 1 && selectedBeast >= 0) m = g.dumpBeast(selectedBeast, 1);
        else if (cargoTab == 2 && selectedHerb >= 0) m = g.dumpHerb(selectedHerb, 1);
        else if (cargoTab == 3 && selectedFish >= 0) m = g.dumpFish(selectedFish, 1);
        else m = "先点一种货。";
        g.toast(m);
        rebuildMenu();
    }

    /** 重新开始: discard everything and start a brand-new game (银 1000 / 补给 500 /
     * 耐久 500 / empty holds), write it back into the current account's local save
     * (overwriting the old slot) and enter the voyage at the start port. */
    private void restartNewGame() {
        GameState fresh = GameState.newGame();
        game.state = fresh;
        g = fresh;
        persist();
        dismissedFail = false;
        dismissedPort = -1;
        dismissedIsland = -1;
        overlay = fresh.dockedPort >= 0 ? Overlay.PORT : Overlay.NONE;
        rebuildMenu();
        g.toast("重新开始：银 1000 / 补给 500 / 耐久 500，已写回本机存档。");
    }

    /** User-facing phrasing for the two failure reasons the model can set. */
    private String failCauseLabel(String reason) {
        if (reason == null || reason.isEmpty()) return "未知原因";
        if (reason.equals("补给耗尽")) return "补给耗尽";
        if (reason.equals("船沉")) return "船只沉没";
        return reason;
    }

    /** 情报 overlay: market intel across ALL ports. Shows:
     *  - top 3 cheapest goods (good + port + price)
     *  - top 3 most expensive goods (good + port + price)
     *  - for each cheapest good: where it sells highest (port + price + profit per unit)
     * Recomputes whenever the overlay opens (prices are fixed per port, so the
     * snapshot is fresh each time the player taps 情报). Scrollable via the
     * ScrollPane from rebuildMenu(). */
    private Table intelTable() {
        IntelPanel panel = new IntelPanel(intelUi, this::closePopup);
        int nPorts = Catalog.PORTS.length;
        int nGoods = Catalog.GOODS.length;
        // Build sorted lists of all (port, good, price) triples.
        int total = nPorts * nGoods;
        int[] allPrices = new int[total];
        int[] allPorts = new int[total];
        int[] allGoods = new int[total];
        int k = 0;
        for (int p = 0; p < nPorts; p++) {
            for (int gg = 0; gg < nGoods; gg++) {
                allPrices[k] = this.g.goodPrice(p, gg);
                allPorts[k] = p;
                allGoods[k] = gg;
                k++;
            }
        }
        // Cheapest 3: ascending.
        Integer[] cheapIdx = new Integer[3];
        for (int i = 0; i < 3; i++) cheapIdx[i] = -1;
        for (int i = 0; i < total; i++) {
            int pr = allPrices[i];
            if (cheapIdx[0] == -1 || pr < allPrices[cheapIdx[0]]) {
                cheapIdx[2] = cheapIdx[1]; cheapIdx[1] = cheapIdx[0]; cheapIdx[0] = i;
            } else if (cheapIdx[1] == -1 || pr < allPrices[cheapIdx[1]]) {
                cheapIdx[2] = cheapIdx[1]; cheapIdx[1] = i;
            } else if (cheapIdx[2] == -1 || pr < allPrices[cheapIdx[2]]) {
                cheapIdx[2] = i;
            }
        }
        // Most expensive 3: descending.
        Integer[] dearIdx = new Integer[3];
        for (int i = 0; i < 3; i++) dearIdx[i] = -1;
        for (int i = 0; i < total; i++) {
            int pr = allPrices[i];
            if (dearIdx[0] == -1 || pr > allPrices[dearIdx[0]]) {
                dearIdx[2] = dearIdx[1]; dearIdx[1] = dearIdx[0]; dearIdx[0] = i;
            } else if (dearIdx[1] == -1 || pr > allPrices[dearIdx[1]]) {
                dearIdx[2] = dearIdx[1]; dearIdx[1] = i;
            } else if (dearIdx[2] == -1 || pr > allPrices[dearIdx[2]]) {
                dearIdx[2] = i;
            }
        }
        for (int i = 0; i < 3; i++) {
            if (cheapIdx[i] < 0) break;
            int idx = cheapIdx[i];
            final int port = allPorts[idx];
            panel.addPrice(true, i, allGoods[idx], port, allPrices[idx], () -> autoSailFromIntel(port));
        }
        for (int i = 0; i < 3; i++) {
            if (dearIdx[i] < 0) break;
            int idx = dearIdx[i];
            final int port = allPorts[idx];
            panel.addPrice(false, i, allGoods[idx], port, allPrices[idx], () -> autoSailFromIntel(port));
        }
        for (int i = 0; i < 3; i++) {
            if (cheapIdx[i] < 0) break;
            int idx = cheapIdx[i];
            int gidx = allGoods[idx];
            int buyPort = allPorts[idx];
            int buyPrice = allPrices[idx];
            int bestPort = 0;
            int bestPrice = this.g.goodPrice(0, gidx);
            for (int q = 1; q < nPorts; q++) {
                int qp = this.g.goodPrice(q, gidx);
                if (qp > bestPrice) { bestPrice = qp; bestPort = q; }
            }
            final int destination = bestPort;
            panel.addArbitrage(i, gidx, buyPort, buyPrice, bestPort, bestPrice,
                    () -> autoSailFromIntel(destination));
        }
        return panel;
    }

    /** Shared handler for the 0.26.6 tappable intel rows. Docked ships get a
     * hint (they cannot sail); at sea it reuses GameState.startAutoSail — the
     * same mechanism as the quest 前往目标 button and full-map port taps — and
     * closes the intel popup so the voyage visibly starts. */
    private void autoSailFromIntel(int port) {
        if (g == null) return;
        if (port < 0 || port >= Catalog.PORTS.length) return;
        if (g.dockedPort >= 0 || g.worldPaused()) {
            g.toast("先离港再点这里：会自动驶向「" + Catalog.PORTS[port] + "」。");
            return;
        }
        g.startAutoSail(port);
        g.toast("自动驶向「" + Catalog.PORTS[port] + "」。（取消自动可停）");
        closePopup();
    }

    // ---- 任务 system (0.26.1) ----
    private static class QuestDef {
        public final int id;
        public final String title;
        public final String description;
        public final int progressType;
        public final int targetAmount;
        public final int targetGood;
        public final int targetPort;
        public final int targetIsland;
        public final int silverReward;
        public final int supplyReward;
        public final int hullReward;
        public final int unlockAfter;
        public final String claimField;
        public QuestDef(int id, String title, String desc, int progType, int target,
                        int good, int port, int island, int silver, int supply, int hull,
                        int unlockAfter, String claimField) {
            this.id = id; this.title = title; this.description = desc;
            this.progressType = progType; this.targetAmount = target;
            this.targetGood = good; this.targetPort = port; this.targetIsland = island;
            this.silverReward = silver; this.supplyReward = supply; this.hullReward = hull;
            this.unlockAfter = unlockAfter; this.claimField = claimField;
        }
    }
    private static final QuestDef[] QUESTS = new QuestDef[] {
        new QuestDef(0, "首次登岛", "靠近一个岛屿，点菜单搜采一次，找出异兽或草药。",
                8, 1, -1, -1, 0, 30, 0, 0, -1, "claimIslandVisit"),
        new QuestDef(1, "第一次补给", "回港口点「补补给」，把补给补满。",
                9, 1, -1, 0, -1, 20, 0, 0, 0, "claimRefill"),
        new QuestDef(2, "修一次船", "回港口点「修理」，把耐久修好。",
                10, 1, -1, 0, -1, 20, 0, 0, 1, "claimRepair"),
        new QuestDef(3, "买点货", "回港口点「市场」买进任意一种货物至少 1 件。",
                11, 1, -1, 0, -1, 40, 0, 0, 2, "claimBuy"),
        new QuestDef(4, "赚个差价", "把买进的货卖出去，赚到比买时更多的银两（任意货、任意港）。",
                12, 1, -1, 0, -1, 80, 0, 0, 3, "claimProfitableSell"),
        new QuestDef(5, "打赢海盗", "在海上遇到海盗，点船锁定自动开火，打沉它。",
                2, 1, -1, -1, -1, 100, 50, 0, 4, "claimWinCombat"),
        new QuestDef(6, "看看行情", "打开「情报」或「行情」看看各港价格，了解差价。",
                13, 1, -1, -1, -1, 30, 0, 0, 5, "claimIntelViewed"),
        new QuestDef(7, "升级一项", "回港口升级仓库、炮火或编制中的任意一项。",
                14, 1, -1, 0, -1, 80, 0, 0, 6, "claimUpgradeAny"),
        // Volume/trade quests (8+)
        new QuestDef(8, "卖 50 丝绸", "把丝绸卖到港口，卖出 50 件即可（货物仍留在货舱，可继续卖）。",
                0, 50, 0, -1, -1, 200, 0, 0, 7, "claimSellSilk"),
        new QuestDef(9, "去 5 个港口", "靠岸去过 5 个不同的港口。",
                1, 5, -1, -1, -1, 300, 0, 0, 8, "claimVisitPorts"),
        new QuestDef(10, "买 10 茶叶", "在港口买进茶叶 10 件（可分次买，货物留在仓内）。",
                17, 10, 2, -1, -1, 50, 0, 0, 9, "claimBuyTea"),
        new QuestDef(11, "卖 30 瓷器", "把瓷器卖出 30 件（可在不同港口分次卖）。",
                16, 30, 1, -1, -1, 250, 0, 0, 10, "claimSellPorcelain"),
        new QuestDef(12, "探 3 个岛", "上岛搜采 3 次（可找不同岛，每次算 1）。",
                8, 3, -1, -1, -1, 120, 0, 0, 11, "claimIslandExplore"),
        new QuestDef(13, "打 3 条海盗", "在海上把 3 艘海盗船打沉。",
                2, 3, -1, -1, -1, 150, 100, 0, 12, "claimDefeatedPirates"),
        new QuestDef(14, "找 5 种异兽", "上岛找出 5 种不同的《山海经》异兽（每种第一次算）。",
                15, 5, -1, -1, -1, 250, 0, 0, 13, "claimBeastsFound"),
        new QuestDef(15, "还完欠钱", "把欠的钱全部还清（欠款归零）。",
                4, 0, -1, -1, -1, 400, 0, 0, 14, "claimDebtPaid"),
        new QuestDef(16, "银到 5000", "帐上银两超过 5000 两（峰值算）。",
                5, 5000, -1, -1, -1, 0, 200, 100, 15, "claimSilverPeak"),
        new QuestDef(17, "升仓 3 次", "把共用货舱仓库升级 3 次。",
                6, 3, -1, -1, -1, 150, 0, 0, 16, "claimWarehouseUps"),
        new QuestDef(18, "雇 5 个人", "花钱雇了 5 个船员上船。",
                7, 5, -1, -1, -1, 100, 0, 0, 17, "claimHiredCrew"),
    };

    private int getQuestProgress(GameState g, int type) {
        switch (type) {
            case 0: return g.questSellSilk;
            case 1: return g.questVisitPorts;
            case 2: return g.questDefeatedPirates;
            case 3: return g.questBeastsFound;
            case 4: return g.debt;
            case 5: return g.questSilverPeak;
            case 6: return g.questWarehouseUps;
            case 7: return g.questHiredCrew;
            case 8: return g.questIslandVisits;
            case 9: return g.questRefillCount;
            case 10: return g.questRepairCount;
            case 11: return g.questBuyCount;
            case 12: return g.questProfitableSell ? 1 : 0;
            case 13: return g.questIntelViewed ? 1 : 0;
            case 14: return g.questUpgradeCount;
            case 15: return g.questBeastsFound;
            case 16: return g.questSellPorcelain;
            case 17: return g.questBuyTea;
            default: return 0;
        }
    }

    private boolean isQuestComplete(GameState g, QuestDef q) {
        int prog = getQuestProgress(g, q.progressType);
        if (q.progressType == 4) return g.questDebtPaid; // debt paid is boolean
        if (q.progressType == 13) return g.questIntelViewed;
        if (q.targetAmount == 0) return prog >= 0; // just needs to happen once
        return prog >= q.targetAmount;
    }

    private boolean isQuestClaimed(GameState g, QuestDef q) {
        switch (q.id) {
            case 0: return g.questClaimIslandVisit;
            case 1: return g.questClaimRefill;
            case 2: return g.questClaimRepair;
            case 3: return g.questClaimBuy;
            case 4: return g.questClaimProfitableSell;
            case 5: return g.questClaimWinCombat;
            case 6: return g.questClaimIntelViewed;
            case 7: return g.questClaimUpgradeAny;
            case 8: return g.questClaimSellSilk;
            case 9: return g.questClaimVisitPorts;
            case 10: return g.questClaimBuyTea;
            case 11: return g.questClaimSellPorcelain;
            case 12: return g.questClaimIslandExplore;
            case 13: return g.questClaimDefeatedPirates;
            case 14: return g.questClaimBeastsFound;
            case 15: return g.questClaimDebtPaid;
            case 16: return g.questClaimSilverPeak;
            case 17: return g.questClaimWarehouseUps;
            case 18: return g.questClaimHiredCrew;
            default: return false;
        }
    }

    private String claimQuest(GameState g, QuestDef q) {
        if (isQuestClaimed(g, q)) return "这奖励拿过了。";
        if (!isQuestComplete(g, q)) return "还没做完。";
        g.silver += q.silverReward;
        g.supply += q.supplyReward;
        if (g.supply > g.supplyMax) g.supply = g.supplyMax;
        g.hull += q.hullReward;
        if (g.hull > g.hullMax) g.hull = g.hullMax;
        setQuestClaimed(g, q);
        java.util.ArrayList<String> parts = new java.util.ArrayList<String>();
        if (q.silverReward > 0) parts.add("银 +" + q.silverReward);
        if (q.supplyReward > 0) parts.add("补给 +" + q.supplyReward);
        if (q.hullReward > 0) parts.add("耐久 +" + q.hullReward);
        String msg = "拿到" + (parts.isEmpty() ? "奖励" : String.join("，", parts)) + "。";
        return msg;
    }

    private void setQuestClaimed(GameState g, QuestDef q) {
        switch (q.id) {
            case 0: g.questClaimIslandVisit = true; break;
            case 1: g.questClaimRefill = true; break;
            case 2: g.questClaimRepair = true; break;
            case 3: g.questClaimBuy = true; break;
            case 4: g.questClaimProfitableSell = true; break;
            case 5: g.questClaimWinCombat = true; break;
            case 6: g.questClaimIntelViewed = true; break;
            case 7: g.questClaimUpgradeAny = true; break;
            case 8: g.questClaimSellSilk = true; break;
            case 9: g.questClaimVisitPorts = true; break;
            case 10: g.questClaimBuyTea = true; break;
            case 11: g.questClaimSellPorcelain = true; break;
            case 12: g.questClaimIslandExplore = true; break;
            case 13: g.questClaimDefeatedPirates = true; break;
            case 14: g.questClaimBeastsFound = true; break;
            case 15: g.questClaimDebtPaid = true; break;
            case 16: g.questClaimSilverPeak = true; break;
            case 17: g.questClaimWarehouseUps = true; break;
            case 18: g.questClaimHiredCrew = true; break;
        }
    }

    private void questsTable(Table box) {
        box.setBackground(questUi.frame);
        box.pad(24, 32, 24, 32);
        Table heading = new Table();
        Table title = new Table();
        title.setBackground(questUi.red);
        title.add(questUi.label("任务", 38, QuestUi.PAPER)).pad(8, 96, 8, 96);
        TextButton close = questUi.button("关闭", true);
        close.addListener(click(this::closePopup));
        heading.add().width(112);
        heading.add(title).expandX();
        heading.add(close).size(112, 64);
        box.add(heading).width(1088).height(72).row();
        box.add(questUi.label("（" + QUESTS.length + "条，顺序解锁，先做手头的）", 22, QuestUi.PAPER))
                .height(48).padBottom(16).row();
        Table panes = new Table();

        // LEFT pane: scrollable quest list (all quests, progress + claim state).
        Table leftPane = new Table();
        leftPane.background(questUi.inset);
        leftPane.pad(8);
        leftPane.add(questUi.label("任务列表", 24, QuestUi.PAPER)).width(400).height(32).left().padBottom(8).row();

        Table listTbl = new Table();
        for (int i = 0; i < QUESTS.length; i++) {
            final int qi = i;
            QuestDef q = QUESTS[i];
            int prog = getQuestProgress(g, q.progressType);
            boolean done = isQuestComplete(g, q);
            boolean claimed = isQuestClaimed(g, q);
            String sub;
            if (claimed) {
                sub = "已完成 · 已领奖";
            } else if (done) {
                sub = "完成！点开领取奖励";
            } else if (q.progressType == 4) { // 还债任务显示当前欠款
                sub = "进行中 欠款 " + prog + " 两";
            } else {
                int t = q.targetAmount > 0 ? q.targetAmount : 1;
                sub = "进行中 " + prog + "/" + t;
            }
            Table row = new Table();
            row.pad(8, 16, 8, 16);
            boolean selected = qi == selectedQuest;
            row.setBackground(selected ? questUi.selected : questUi.parchment);
            Color ink = selected ? QuestUi.PAPER : QuestUi.INK;
            Table copy = new Table();
            copy.add(questUi.label(q.title, 24, ink)).left().growX().row();
            Label status = questUi.label(sub, 20, selected && done ? QuestUi.JADE : ink);
            copy.add(status).left().growX();
            row.add(copy).expandX().fillX().padLeft(24);
            TextureRegionDrawable icon = IconLib.hud("quest");
            if (icon != null) row.add(new Image(icon)).size(32).padRight(16);
            row.addListener(click(() -> { selectedQuest = qi; rebuildMenu(); }));
            listTbl.add(row).width(400).height(80).row();
        }
        ScrollPane listSp = new ScrollPane(listTbl, game.skin);
        listSp.setScrollingDisabled(true, false);
        listSp.setFadeScrollBars(false);
        listSp.setOverscroll(false, false);
        questListPane = listSp;
        leftPane.add(listSp).width(400).height(512).left();

        // RIGHT pane: detail of the selected quest.
        Table rightPane = new Table();
        rightPane.background(questUi.inset);
        rightPane.pad(24f);
        Table detTbl = new Table();
        if (selectedQuest >= 0 && selectedQuest < QUESTS.length) {
            QuestDef q = QUESTS[selectedQuest];
            detTbl.add(questUi.label("任务详情", 26, QuestUi.PAPER)).width(568).left().padBottom(16).row();
            detTbl.add(questWrap(q.description)).width(568).left().padBottom(16).row();

            String targetInfo;
            if (q.targetGood >= 0) {
                targetInfo = "目标货物：" + Catalog.GOODS[q.targetGood];
                if (q.targetAmount > 0) targetInfo += "   需要 " + q.targetAmount + " 件";
            } else if (q.targetPort >= 0) {
                targetInfo = "目标港口：" + Catalog.PORTS[q.targetPort];
            } else if (q.targetIsland >= 0) {
                targetInfo = "目标岛屿：" + Catalog.ISLANDS[q.targetIsland];
            } else {
                targetInfo = "完成条件：见上方描述";
            }
            detTbl.add(questInfo(targetInfo)).width(568).left().padBottom(8).row();

            int prog = getQuestProgress(g, q.progressType);
            String progText;
            if (q.progressType == 4) {
                progText = "当前欠款：" + g.debt + " 两（归零即完成）";
            } else if (q.progressType == 13) {
                progText = "是否看过行情：" + (g.questIntelViewed ? "是" : "否");
            } else if (q.targetAmount > 0) {
                progText = "进度：" + prog + "/" + q.targetAmount;
            } else {
                progText = "进度：" + (prog > 0 ? "已完成" : "尚未");
            }
            detTbl.add(questInfo(progText)).width(568).left().padBottom(8).row();

            detTbl.add(questInfo("—— 奖励明细 ——")).width(568).center().padTop(24).padBottom(16).row();
            Table rewards = new Table();
            questReward(rewards, "silver", "银两", q.silverReward + "两");
            if (q.supplyReward > 0) questReward(rewards, "supply", "补给", "+" + q.supplyReward);
            if (q.hullReward > 0) questReward(rewards, "hull", "耐久", "+" + q.hullReward);
            detTbl.add(rewards).width(568).height(112).row();
            detTbl.add(questInfo("（纯奖励，净赚为正）")).width(568).center().padBottom(16).row();

            if (q.targetGood >= 0) {
                String note = "怎么做：低买高卖赚差价（可在「情报」里找便宜买点与贵卖点），"
                        + "奖励银 " + q.silverReward + " 两是差价之外的纯收益，不与成本相抵。";
                detTbl.add(questWrap(note)).width(568).left().padTop(2).padBottom(8).row();
            }

            boolean done = isQuestComplete(g, q);
            boolean claimed = isQuestClaimed(g, q);
            boolean hasNav = q.targetPort >= 0 || q.targetIsland >= 0;
            boolean canGo = hasNav && g.dockedPort < 0 && !g.worldPaused();
            // 0.27.4: 领取 / 前往 side by side; 领取 is disabled until the quest is
            // complete and unclaimed, 前往 until the ship is at sea.
            Table actions = new Table();
            TextButton claim = questUi.button("领取", true);
            claim.setDisabled(!(done && !claimed));
            claim.addListener(click(() -> {
                // The world keeps sailing while this popup is open at sea, so
                // re-check on tap: the quest may have changed underneath us.
                if (isQuestClaimed(g, q) || !isQuestComplete(g, q)) {
                    g.toast("这个任务现在不能领奖。");
                    rebuildMenu();
                    return;
                }
                String msg = claimQuest(g, q);
                g.toast(msg);
                try {
                    persist(); // a save failure must never crash the claim
                } catch (Throwable t) {
                    Gdx.app.error("VoyageScreen", "persist after quest claim failed", t);
                }
                selectedQuest = getActiveQuestIndex();
                rebuildMenu();
            }));
            actions.add(claim).width(270).height(64);
            if (hasNav) {
                final String dst = q.targetPort >= 0 ? Catalog.PORTS[q.targetPort]
                        : Catalog.ISLANDS[q.targetIsland];
                TextButton go = questUi.button("前往", false);
                go.setDisabled(!canGo);
                go.addListener(click(() -> {
                    if (q.targetPort >= 0) {
                        g.startAutoSail(q.targetPort);
                    } else {
                        g.startAutoSailIsle(q.targetIsland);
                    }
                    g.toast("自动驶向 " + dst + "。");
                    closePopup();
                    rebuildMenu();
                }));
                actions.add(go).width(270).height(64).padLeft(28);
            }
            detTbl.add(actions).width(568).left().padBottom(8).row();
            if (claimed) {
                detTbl.add(questInfo("奖励已领取 ✓")).width(568).left().padBottom(16).row();
            } else if (!done) {
                detTbl.add(questInfo("（完成目标后即可领取）")).width(568).left().padBottom(16).row();
            }
            if (hasNav && !canGo) {
                detTbl.add(questInfo("（先离港再前往）")).width(568).left().padBottom(8).row();
            }
        } else if (getActiveQuestIndex() < 0) {
            detTbl.add(questWrap("全部任务已完成、奖励已领。祝你在南海航程一路顺风！"))
                    .width(568).left().padBottom(16).row();
        } else {
            detTbl.add(questInfo("点左边列表选一个任务，右侧看详情。")).width(568).left().padBottom(16).row();
        }
        ScrollPane detSp = new ScrollPane(detTbl, game.skin);
        detSp.setFadeScrollBars(false);
        detSp.setScrollingDisabled(true, false);
        detSp.setOverscroll(false, false);
        rightPane.add(detSp).width(568).height(512).left();

        panes.add(leftPane).width(424).height(568);
        panes.add(rightPane).width(640).height(568).padLeft(24);
        box.add(panes).width(1088).height(568).row();
    }

    private Label questInfo(String text) {
        return questUi.label(text, 24, QuestUi.PAPER);
    }

    private Label questWrap(String text) {
        Label label = questInfo(text);
        label.setWrap(true);
        return label;
    }

    private TextButton questButton(String text, boolean primary, Runnable action) {
        TextButton button = questUi.button(text, primary);
        button.getLabel().setFontScale(1.25f);
        button.addListener(click(action));
        return button;
    }

    private void questReward(Table row, String icon, String name, String amount) {
        Table card = new Table();
        TextureRegionDrawable art = IconLib.hud(icon);
        if (art != null) card.add(new Image(art)).size(48).padBottom(8).row();
        card.add(questUi.label(name + " " + amount, 24, QuestUi.PAPER)).row();
        row.add(card).expandX().fillX().pad(8);
    }

    /** The HUD shows at most one quest: the first unclaimed quest in the chain
     * that is unlocked (its predecessor claimed) — completed-but-unclaimed ones
     * count too, so the HUD keeps prompting the player to claim rewards, which
     * is what unlocks the next tutorial step. */
    private int getActiveQuestIndex() {
        for (int i = 0; i < QUESTS.length; i++) {
            QuestDef q = QUESTS[i];
            if (isQuestClaimed(g, q)) continue;
            if (q.unlockAfter >= 0 && !isQuestClaimed(g, QUESTS[q.unlockAfter])) continue;
            return i;
        }
        return -1;
    }

    /** Save before clearing the session and returning to login.
     * Uses the same deferred screen-switch pattern as LoginScreen.enterVoyage
     * (never setScreen() from inside a click dispatch — Android surface race). */
    private void logoutToLogin() {
        if (logoutSwitching) {
            return;
        }
        logoutSwitching = true;
        try {
            if (game.currentUser != null) {
                game.accounts.save(game.currentUser, g.toSave());
            }
        } catch (Throwable ignored) {
            // 存档失败不能卡住退出登录。
        }
        Gdx.input.setInputProcessor(null);
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    game.currentUser = null;
                    game.state = null;
                    game.setScreen(new LoginScreen(game));
                } catch (Throwable t) {
                    Gdx.app.error("VoyageScreen", "logout transition failed", t);
                    logoutSwitching = false;
                }
            }
        });
    }

    /** One horizontal codex row: optional icon + single-line name. */
    private void codexRow(Table box, TextureRegionDrawable icon, String text) {
        if (icon != null) {
            Table row = new Table();
            row.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(icon)).size(32, 32).padRight(8);
            Label l = new Label(text, game.skin, "small");
            l.setWrap(false);
            row.add(l).left();
            box.add(row).width(MENU_W).left().padBottom(1).row();
        } else {
            Label l = new Label(text, game.skin, "small");
            l.setWrap(false);
            box.add(l).width(MENU_W).left().padBottom(1).row();
        }
    }

    private void tabs(Table box) {
        Table tabs = new Table();
        String[] names = {"商货", "异兽", "草药", "渔获"};
        for (int i = 0; i < names.length; i++) {
            final int tab = i;
            TextButton button = cargoButton(names[i], cargoTab == i, () -> { cargoTab = tab; rebuildMenu(); });
            tabs.add(button).size(296, 56).padRight(i < 3 ? 16 : 0);
        }
        box.add(tabs).size(1232, 56).padBottom(8).row();
    }

    private void listItems(Table box, boolean trading) {
        if (cargoTab == 0) {
            int port = Math.max(0, g.dockedPort);
            for (int i = 0; i < Catalog.GOODS.length; i++) {
                final int idx = i;
                String s = Catalog.GOODS[i] + "   持有 " + g.trade[i];
                if (g.dockedPort >= 0) {
                    s += "    本港 " + g.goodPrice(port, i) + " 两";
                }
                final String txt = s;
                cargoItemRow(box, IconLib.good(i), Catalog.GOODS[i], g.trade[i],
                        g.dockedPort >= 0 ? "本港 " + g.goodPrice(port, i) + " 两" : "靠港查看", selectedGood == i, () -> {
                    selectedGood = idx;
                    if (trading && g.dockedPort >= 0) {
                        priceReturnOverlay = Overlay.CARGO;
                        overlay = Overlay.PRICE;
                        markIntelViewed();
                    }
                    rebuildMenu();
                });
            }
        } else if (cargoTab == 1) {
            for (int i = 0; i < Catalog.BEASTS.length; i++) {
                final int idx = i;
                if (g.beasts[i] <= 0 && !(trading && g.beastFound[i])) {
                    continue;
                }
                String s = Catalog.BEASTS[i] + "    x" + g.beasts[i] + "    卖价 " + Catalog.BEAST_PRICE[i];
                final String txt = s;
                cargoItemRow(box, IconLib.beast(i), Catalog.BEASTS[i], g.beasts[i],
                        "卖价 " + Catalog.BEAST_PRICE[i] + " 两", selectedBeast == i, () -> {
                    selectedBeast = idx;
                    if (trading && g.dockedPort >= 0 && g.beasts[idx] > 0) {
                        g.toast(g.sellBeast(idx, 1));
                        persist();
                    }
                    rebuildMenu();
                });
            }
        } else if (cargoTab == 2) {
            for (int i = 0; i < Catalog.HERBS.length; i++) {
                final int idx = i;
                if (g.herbs[i] <= 0 && !(trading && g.herbFound[i])) {
                    continue;
                }
                String s = Catalog.HERBS[i] + "    x" + g.herbs[i] + "    卖价 " + Catalog.HERB_PRICE[i] + "（只卖）";
                final String txt = s;
                cargoItemRow(box, IconLib.herb(i), Catalog.HERBS[i], g.herbs[i],
                        "卖价 " + Catalog.HERB_PRICE[i] + " 两", selectedHerb == i, () -> {
                    selectedHerb = idx;
                    if (trading && g.dockedPort >= 0 && g.herbs[idx] > 0) {
                        g.toast(g.sellHerb(idx, 1));
                        persist();
                    }
                    rebuildMenu();
                });
            }
        } else { // cargoTab == 3: 渔获
            for (int i = 0; i < Catalog.FISH.length; i++) {
                final int idx = i;
                if (g.fish[i] <= 0) {
                    continue;
                }
                String s = Catalog.FISH[i] + "    x" + g.fish[i] + "    卖价 " + Catalog.FISH_PRICE[i] + "（只卖）";
                final String txt = s;
                cargoItemRow(box, IconLib.fish(i), Catalog.FISH[i], g.fish[i],
                        "卖价 " + Catalog.FISH_PRICE[i] + " 两", selectedFish == i, () -> {
                    selectedFish = idx;
                    if (trading && g.dockedPort >= 0 && g.fish[idx] > 0) {
                        g.toast(g.sellFish(idx, 1));
                        persist();
                    }
                    rebuildMenu();
                });
            }
            if (g.fishTotal() <= 0) {
                box.add(cargoLabel("还没有渔获：到故乡扬州雇渔夫捕鱼。", 24, QuestUi.PAPER)).width(1232).height(96).row();
            }
        }
    }

    /** Adds an icon+text row; icon cell omitted when the drawable is null. */
    private void iconRow(Table box, TextureRegionDrawable icon, String text, Runnable onClick) {
        TextButton b = btn(text, onClick);
        if (icon != null) {
            Table row = new Table();
            row.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(icon)).size(28, 28).padRight(6);
            row.add(b).width(452).height(42);
            box.add(row).width(MENU_W).left().padBottom(2).row();
        } else {
            box.add(b).width(452).height(42).left().padBottom(2).row();
        }
    }

    /** Pair of action buttons on one horizontal row (single-line text). */
    private void pairRow(Table box, String a, Runnable ra, String b, Runnable rb) {
        Table row = new Table();
        row.add(btn(a, ra)).width(242).height(46);
        row.add(btn(b, rb)).width(242).height(46).padLeft(10);
        box.add(row).width(MENU_W).padBottom(5).row();
    }

    /** Small single-line info text (never wraps into a vertical column). */
    private Label infoRow(String s) {
        Label l = new Label(s, game.skin, "small");
        l.setWrap(false);
        return l;
    }

    /** Wrapped hint text with an explicit width. */
    private Label wrapLbl(String s) {
        Label l = new Label(s, game.skin, "small");
        l.setWrap(true);
        return l;
    }

    private TextButton btn(String t, Runnable r) {
        TextButton b = new TextButton(t, game.skin);
        b.getLabel().setFontScale(0.92f);
        b.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                r.run();
            }
        });
        return b;
    }

    /** Existing sequential quests are shown as current + next, never invented side quests. */
    private void updateQuestButtonLabel() {
        if (g == null || voyageHud == null) return;
        int active = getActiveQuestIndex();
        trackedQuests[0] = active;
        trackedQuests[1] = active >= 0 && active + 1 < QUESTS.length ? active + 1 : -1;
        for (int card = 0; card < 2; card++) {
            int index = trackedQuests[card];
            if (index < 0) {
                voyageHud.quest(card, card == 0 ? "航海日志 · 功成" : "下一程 · 自由航行",
                        card == 0 ? "当前任务均已完成，继续探索南海。" : "寻访各港，发现更多奇珍异兽。", "点击查看任务日志");
                continue;
            }
            QuestDef q = QUESTS[index];
            int progress = getQuestProgress(g, q.progressType);
            String count = q.targetAmount <= 0 ? "欠款 " + progress + " 两" : "进度 " + Math.min(progress, q.targetAmount) + "/" + q.targetAmount;
            voyageHud.quest(card, (card == 0 ? "主线 · " : "下一程 · ") + q.title,
                    q.description, card == 1 ? "待前序领奖解锁" : isQuestComplete(g, q) ? "已完成 · 点击领奖" : count);
        }
    }

    /** 任务追踪：看过一次「情报」或某货的「行情」即完成 tutorial quest 6. */
    private void markIntelViewed() {
        if (g != null && !g.questIntelViewed) {
            g.questIntelViewed = true;
            persist();
        }
    }

    /** Refreshes the four ICON+NUMBER values in the top stat panel. */
    private void updateStatValues() {
        if (g == null || statVals == null || statVals[0] == null) return;
        statVals[0].setText(String.valueOf(g.silver));
        statVals[1].setText(String.valueOf((int) g.supply));
        statVals[2].setText(String.valueOf((int) g.hull));
        statVals[3].setText(g.crew + "/" + g.crewMax());
        if (hudClock != null) {
            hudClock.setText(g.timeLabel());
        }
    }

    private void persist() {
        if (game.currentUser != null && g != null) {
            game.accounts.save(game.currentUser, g.toSave());
        }
    }

    private void confirmFillAccount() {
        if (game.currentUser == null) {
            g.toast("没有登录账号，无法保存进度。");
            return;
        }
        if (stage.getRoot().findActor("confirmFillAccount") != null) return;
        captainPanel.showFillConfirmation(() -> {
            g.fillAccount(); persist(); updateStatValues(); rebuildMenu();
            g.toast("船、异兽、草药已全开，进度已保存到本机存档。");
        });
    }

    /** 存档按钮：立刻把当前进度写入本机存档。 */
    private void saveNow() {
        if (game.currentUser == null) {
            g.toast("没有登录账号，无法保存进度。");
            return;
        }
        game.accounts.save(game.currentUser, g.toSave());
        g.toast("进度已保存到本机存档。");
    }

    /** 读档按钮 / 失败弹窗「读取存档」：回到最近一次写入的本机存档。
     * 没有存档时提示并留在当前界面，不跳回登录。 */
    private void tryReloadLatestSave() {
        if (game.currentUser == null) {
            g.toast("没有登录账号，无法读取存档。");
            return;
        }
        SaveData s = game.accounts.load(game.currentUser);
        if (s == null) {
            g.toast("没有存档");
            return;
        }
        game.state = GameState.fromSave(s);
        g = game.state;
        dismissedPort = -1;
        dismissedIsland = -1;
        dismissedFail = false;
        overlay = g.dockedPort >= 0 ? Overlay.PORT : Overlay.NONE;
        rebuildMenu();
    }

    // ------------------------------------------------------------ render

    @Override
    public void render(float delta) {
        if (!loggedFirstFrame) {
            loggedFirstFrame = true;
            Gdx.app.error("VoyageEnter", "first render frame OK (world x=" + (g == null ? -1 : g.x)
                    + " y=" + (g == null ? -1 : g.y) + ")");
        }
        if (g == null || stage == null) {
            ScreenUtils.clear(WATER);
            return;
        }
        if (!isFinite(g.x) || !isFinite(g.y) || !isFinite(g.headingDeg) || !isFinite(g.speed)) {
            int lp = Math.max(0, Math.min(g.lastPort, Catalog.PORTS.length - 1));
            g.x = Catalog.PORT_X[lp] + 90f;
            g.y = Catalog.PORT_Y[lp];
            g.headingDeg = 0f;
            g.speed = 0f;
            if (g.dockedPort < 0) {
                g.dockedPort = lp;
            }
            Gdx.app.error("VoyageScreen", "non-finite ship state, reset to port");
        }
        readKeyboard();
        g.onManualSteer();
        // Every open page pauses movement, clock, weather and combat.
        if (!g.worldPaused() && overlay == Overlay.NONE) g.update(delta);
        else if (g.toastT > 0) g.toastT = Math.max(0,g.toastT-delta);

        // 0.26.3: 捕鱼只在停靠扬州时进行（即使世界暂停/菜单开着）。钓上一条就
        // 刷新渔务面板并保存，避免退游戏丢鱼。
        if (g.dockedPort == Catalog.YANGZHOU && (overlay == Overlay.PORT || overlay == Overlay.FISH)) {
            boolean caught = g.tickFishing(delta);
            if (caught) {
                if (overlay == Overlay.FISH) {
                    rebuildMenu();
                }
                persist();
            }
        }

        // Context transitions. 0.27.2: ONLY the fail popup auto-opens. Port/island
        // menus NEVER auto-open on proximity — the player must tap the port/island
        // icon while in range (see WorldInput). While the full-map modal is up,
        // nothing opens under it.
        // Sub-views of the docked menu must never be stomped by the fail auto-
        // open: 行情 (PRICE) used to flash back to PORT the frame after opening, and
        // the same applies to 市场 (MARKET), the 船长菜单 (AVATAR) and the first-run
        // 玩法说明 (HOWTO).
        boolean dockSub = overlay == Overlay.PRICE || overlay == Overlay.MARKET
                || overlay == Overlay.AVATAR || overlay == Overlay.HOWTO
                || overlay == Overlay.INTEL || overlay == Overlay.QUESTS
                || overlay == Overlay.FISH || overlay == Overlay.SHOP || overlay == Overlay.MINE;
        if (overlay != Overlay.MAP && !dockSub && g.failed && !dismissedFail && overlay != Overlay.FAIL) {
            overlay = Overlay.FAIL;
            persist();
            rebuildMenu();
        }
        // 0.27.2: proximity only nudges (toast once per port/island), never opens.
        if (overlay == Overlay.NONE && !g.worldPaused() && !g.autoSail) {
            int np = g.nearestPortInRange();
            if (np >= 0 && np != hintPort) {
                hintPort = np;
                g.toast("已靠近「" + Catalog.PORTS[np] + "」：点击港口图标可停靠。");
            } else if (np < 0) {
                hintPort = -1;
            }
            int ni = g.nearestIslandInRange();
            if (ni >= 0 && ni != hintIsland) {
                hintIsland = ni;
                g.toast("已靠近「" + Catalog.ISLANDS[ni] + "」：点击岛屿图标可搜采。");
            } else if (ni < 0) {
                hintIsland = -1;
            }
        }

        btnCancelAuto.setVisible(g.autoSail && overlay != Overlay.MAP);
        btnLockPirate.setVisible(g.pirateAlive && !g.combatLock && overlay == Overlay.NONE);
        btnCancelLock.setVisible(g.combatLock && overlay != Overlay.MAP);
        Label pageNotice = menuRoot.findActor("pageNotice");
        if (pageNotice != null) pageNotice.setText(g.toastT>0 ? g.toast : "世界暂停 · 关闭后继续航行");
        hudLine.setText(statusText());
        updateStatValues();   // live numbers in the top stat cells
        updateQuestButtonLabel();
        int activeQuest = getActiveQuestIndex();
        voyageHud.update(g, overlay == Overlay.NONE,
                activeQuest >= 0 && isQuestComplete(g, QUESTS[activeQuest]), stickKX, stickKY);

        if (world3d != null) world3d.render(g, overlay == Overlay.NONE ? delta : 0f);
        else ScreenUtils.clear(WATER);

        hudVp.apply();
        shapes.setProjectionMatrix(hudVp.getCamera().combined);
        game.batch.setProjectionMatrix(hudVp.getCamera().combined);

        if (world3d != null && overlay != Overlay.MAP) drawWorldLabels();

        // Full-map modal: covers the ENTIRE screen. The HUD stage is hidden (not
        // drawn and not hit-testable) so every button below — cargo/codex/port
        // rail, joystick, accel/decel, minimap — is invisible AND unclickable;
        // WorldInput alone handles all touches while it is open.
        boolean mapOpen = overlay == Overlay.MAP;
        if (mapOpen) {
            radarT += delta;   // freezes while the map is closed, resumes on reopen
            drawFullMap();
        }
        stage.getRoot().setVisible(!mapOpen);
        stage.act(delta);
        if (!mapOpen) {
            stage.draw();
        }

    }

    private String statusText() {
        String s = "欠" + g.debt + " 舱 " + g.cargoUsed() + "/" + g.holdCap()
                + "   " + g.windLabel() + " 速" + (int) g.speed;
        if (g.autoSailPort >= 0) {
            s += " 自动->" + Catalog.PORTS[g.autoSailPort];
        } else if (g.autoSailIsle >= 0) {
            s += " 自动->" + Catalog.ISLANDS[g.autoSailIsle];
        }
        if (g.dockedPort >= 0) {
            s += " ·停泊" + Catalog.PORTS[g.dockedPort];
        } else if (g.islandMenu >= 0) {
            s += " ·探岛" + Catalog.ISLANDS[g.islandMenu];
        }
        if (g.pirateAlive) {
            int distance = (int) Catalog.dist(g.x, g.y, g.pirateX, g.pirateY);
            s += " 【海盗】敌" + Math.max(0, (int) g.pirateHp) + "/" + (int) g.pirateHpMax
                    + " 距" + distance + (g.combatLock ? " ·已锁定自动开火" : " ·点船锁定");
        }
        return s;
    }

    private void readKeyboard() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && overlay != Overlay.NONE) {
            if (overlay == Overlay.HOWTO) dismissHowto();
            else closePopup();
            return;
        }
        if (overlay != Overlay.NONE) return;
        float steer = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) steer -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) steer += 1f;
        if (!stickActive) {
            g.steerInput = steer;
        }
        boolean w = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean s = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        // Tracked in hold(): reliable long-press even if isPressed() flickers.
        g.holdAccel = w || accelDown;
        g.holdDecel = s || decelDown;
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            overlay = overlay == Overlay.MAP ? Overlay.NONE : Overlay.MAP;
            rebuildMenu();
        }
    }

    /** World labels are projected onto the HUD plane, below the existing Scene2D overlay. */
    private boolean worldAnchor(float x, float y, float height) {
        if (!world3d.project(x, y, height, tmp)) return false;
        hudVp.unproject(tmp.set(tmp.x, Gdx.graphics.getHeight() - tmp.y, 0));
        return true;
    }

    private void drawWorldLabels() {
        game.batch.begin();
        for (int i = 0; i < Catalog.PORTS.length; i++) {
            if (Catalog.dist(g.x,g.y,Catalog.PORT_X[i],Catalog.PORT_Y[i]) < 1800
                    && worldAnchor(Catalog.PORT_X[i], Catalog.PORT_Y[i], 42)) {
                game.fontSmall.draw(game.batch, Catalog.PORTS[i], tmp.x - 22, tmp.y);
            }
        }
        for (int i = 0; i < Catalog.ISLANDS.length; i++) {
            if (Catalog.dist(g.x,g.y,Catalog.ISLAND_X[i],Catalog.ISLAND_Y[i]) < 1200
                    && worldAnchor(Catalog.ISLAND_X[i], Catalog.ISLAND_Y[i], 50)) {
                game.fontSmall.draw(game.batch, Catalog.ISLANDS[i], tmp.x - 22, tmp.y);
            }
        }
        if (g.pirateAlive && worldAnchor(g.pirateX,g.pirateY,74)) {
            game.fontSmall.draw(game.batch, g.combatLock ? "海盗 已锁定" : "海盗 点船锁定",tmp.x-45,tmp.y+20);
        }
        game.batch.end();
        if (g.pirateAlive) {
            drawHealth(g.x,g.y,68,g.hull,g.hullMax,false);
            drawHealth(g.pirateX,g.pirateY,74,g.pirateHp,g.pirateHpMax,g.combatLock);
        }
    }

    private void drawHealth(float x,float y,float height,float hp,float max,boolean selected) {
        if (!worldAnchor(x,y,height)) return;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(selected ? Color.GOLD : Color.DARK_GRAY);
        shapes.rect(tmp.x-35,tmp.y-9,70,9);
        shapes.setColor(PIRATE_C);
        shapes.rect(tmp.x-33,tmp.y-7,66*MathUtils.clamp(hp/Math.max(1,max),0,1),5);
        shapes.end();
    }

    private static boolean isFinite(float v) {
        return !Float.isNaN(v) && !Float.isInfinite(v);
    }

    private WorldMapOverlay worldMap() {
        if (worldMap == null) worldMap = new WorldMapOverlay(game);
        return worldMap;
    }

    private void drawFullMap() {
        worldMap().draw(shapes, g, radarT);
    }

    // ------------------------------------------------------------- input

    private class WorldInput extends InputAdapter {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            hudVp.unproject(tmp.set(screenX, screenY, 0));
            float hx = tmp.x, hy = tmp.y;

            if (overlay == Overlay.MAP) return handleFullMapTap(hx, hy);
            // Visible Scene2D controls win over port/enemy hit areas beneath them.
            Actor hudHit = stage.hit(hx, hy, true);
            if (hudHit != null && hudHit != stage.getRoot()) return false;

            // Virtual joystick: usable whenever no modal popup is open. While
            // docked with the port menu closed the world is NOT paused, so the
            // first touch undocks the ship and sailing starts immediately.
            float dx = hx - stickCX, dy = hy - stickCY;
            if (overlay == Overlay.NONE && stickPointer < 0
                    && dx * dx + dy * dy <= (stickR + 26f) * (stickR + 26f)) {
                undockIfNeeded();
                stickActive = true;
                stickPointer = pointer;
                setStick(hx, hy);
                g.onManualSteer();
                return true;
            }
            // Lock a pirate ship: tap near it while sailing.
            if (g.pirateAlive && overlay == Overlay.NONE) {
                if (world3d != null && world3d.hit(screenX, screenY, g.pirateX, g.pirateY, 27, 36)) {
                    if (g.combatLock) g.cancelLock(); else g.lockPirate();
                    return true;
                }
            }
            // 0.27.2: world-space tap on a port/island icon opens its menu ONLY
            // when the ship is within the existing dock/search range. Proximity
            // alone never opens anything; closing never moves the ship.
            if (overlay == Overlay.NONE) {
                for (int i = 0; i < Catalog.PORTS.length; i++) {
                    if (world3d != null && world3d.hit(screenX, screenY, Catalog.PORT_X[i], Catalog.PORT_Y[i], 35, VoyageGeometry.landRadius(true,i))
                            && Catalog.dist(g.x, g.y, Catalog.PORT_X[i], Catalog.PORT_Y[i]) < Catalog.DOCK_RANGE) {
                        g.dock(i);
                        overlay = Overlay.PORT;
                        persist();
                        rebuildMenu();
                        return true;
                    }
                }
                for (int i = 0; i < Catalog.ISLANDS.length; i++) {
                    if (world3d != null && world3d.hit(screenX, screenY, Catalog.ISLAND_X[i], Catalog.ISLAND_Y[i], 25, VoyageGeometry.landRadius(false,i))
                            && Catalog.dist(g.x, g.y, Catalog.ISLAND_X[i], Catalog.ISLAND_Y[i]) < Catalog.ISLAND_RANGE) {
                        g.enterIsland(i);
                        overlay = Overlay.ISLAND;
                        rebuildMenu();
                        return true;
                    }
                }
            }
            if (overlay == Overlay.NONE && world3d != null && lookPointer < 0 && button == Input.Buttons.LEFT) {
                lookPointer = pointer; lookX = screenX; lookY = screenY;
                world3d.beginLook();
                return true;
            }
            return false;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            if (pointer == lookPointer && world3d != null) {
                world3d.dragLook((screenX-lookX)/Gdx.graphics.getWidth(), (screenY-lookY)/Gdx.graphics.getHeight());
                lookX = screenX; lookY = screenY;
                return true;
            }
            if (stickActive && pointer == stickPointer) {
                hudVp.unproject(tmp.set(screenX, screenY, 0));
                setStick(tmp.x, tmp.y);
                g.onManualSteer();
                return true;
            }
            return false;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            if (pointer == lookPointer) {
                lookPointer = -1;
                if (world3d != null) world3d.endLook();
                return true;
            }
            if (pointer == stickPointer) {
                stickActive = false;
                stickPointer = -1;
                stickKX = stickKY = 0;
                g.steerInput = 0;
                g.releaseHeading();
                return true;
            }
            return false;
        }

        @Override public boolean touchCancelled(int x, int y, int pointer, int button) {
            return touchUp(x,y,pointer,button);
        }
    }

    private void setStick(float hx, float hy) {
        float dx = hx - stickCX, dy = hy - stickCY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > stickR) {
            dx = dx / len * stickR;
            dy = dy / len * stickR;
            len = stickR;
        }
        stickKX = dx / stickR;
        stickKY = dy / stickR;
        // The chase-view helm is ship-relative: left/right controls yaw at every speed.
        g.releaseHeading();
        g.steerInput = Math.abs(dx) >= 10f ? stickKX : 0f;
    }

    private void releaseWorldControls() {
        stickActive = false; stickPointer = lookPointer = -1; stickKX = stickKY = 0;
        accelDown = decelDown = false;
        g.steerInput = 0; g.releaseHeading(); g.holdAccel = g.holdDecel = false;
        if (world3d != null) world3d.resetLook();
    }

    private boolean handleFullMapTap(float hx, float hy) {
        // The extended HUD viewport may exceed HUD_W×HUD_H on non-16:9 screens;
        // map the tap through the ACTUAL world size so it lines up with the chart.
        int target = worldMap().hit(hx, hy, hudVp.getWorldWidth(), hudVp.getWorldHeight());
        if (target == WorldMapOverlay.EMPTY) return true;
        overlay = Overlay.NONE;
        if (target >= 0) {
            undockIfNeeded();
            if (target < Catalog.PORTS.length) {
                g.startAutoSail(target);
            } else {
                g.startAutoSailIsle(target - Catalog.PORTS.length);
            }
        }
        rebuildMenu();
        return true;
    }

    @Override
    public void resize(int width, int height) {
        if (world3d != null) world3d.resize(width, height);
        if (hudVp != null) {
            hudVp.update(width, height, true);
        }
        applyHudScale();
    }

    @Override
    public void dispose() { disposeQuietly(); }

    @Override
    public void pause() { if (g != null) releaseWorldControls(); if (overlay == Overlay.AVATAR && captainPanel != null) captainPanel.commitNickname(); persist(); }

    @Override
    public void hide() {
        if (g != null) releaseWorldControls();
        persist();
        if (voyageHud != null) { voyageHud.dispose(); voyageHud = null; }
        if (world3d != null) { world3d.dispose(); world3d = null; }
        if (questUi != null) { questUi.dispose(); questUi = null; }
        if (intelUi != null) { intelUi.dispose(); intelUi = null; }
        if (shopPanel != null) { shopPanel.dispose(); shopPanel = null; }
        if (myShipPanel != null) { myShipPanel.dispose(); myShipPanel = null; }
        if (captainPanel != null) { captainPanel.dispose(); captainPanel = null; }
        if (portPanel != null) { portPanel.dispose(); portPanel = null; }
        if (cargoUi != null) { cargoUi.dispose(); cargoUi = null; }
        if (pageUi != null) { pageUi.dispose(); pageUi = null; }
        cargoListPane = null;
        if (codexPanel != null) { codexPanel.dispose(); codexPanel = null; }
        questListPane = null;
        if (worldMap != null) {
            worldMap.dispose();
            worldMap = null;
        }
        if (stage != null) {
            stage.dispose();
            stage = null;
        }
        if (shapes != null) {
            shapes.dispose();
            shapes = null;
        }
    }
}
