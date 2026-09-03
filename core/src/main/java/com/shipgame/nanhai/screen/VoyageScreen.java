package com.shipgame.nanhai.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.shipgame.nanhai.NanHaiVoyage;
import com.shipgame.nanhai.PixelMapRenderer;
import com.shipgame.nanhai.data.Catalog;
import com.shipgame.nanhai.data.GameState;
import com.shipgame.nanhai.ui.IconLib;

public class VoyageScreen extends ScreenAdapter {

    private static final float HUD_W = 1280f;
    private static final float HUD_H = 720f;
    private static final float MENU_W = 500f;             // popup content width
    // Round minimap: top-right corner
    private static final float MM_CX = HUD_W - 106f;
    private static final float MM_CY = HUD_H - 104f;
    private static final float MM_R = 80f;
    // Captain avatar: top-left, just under the live status line (no bottom-left
    // avatar anymore — the bottom-left belongs to the virtual joystick only).
    private static final float AV_X = 62f;
    private static final float AV_Y = 660f;
    private static final float AV_R = 20f;
    // Far-left circular action buttons 货物/图鉴/港口 stacked on the left edge.
    private static final float LB_X = 55f;       // button center x
    private static final float LB_SIZE = 70f;    // button diameter
    private static final float LB_GAP = 76f;     // center-to-center spacing
    private static final float LB_TOP = 580f;    // center y of the top (货物) button
    // Full-map modal geometry (overlay == Overlay.MAP): the map is a fullscreen
    // dimmed rect with the map area centered; its 关闭 button sits top-right.
    private static final float FM_X = 90f, FM_Y = 90f, FM_W = 1100f, FM_H = 540f;
    private static final float FM_CLOSE_X = FM_X + FM_W - 124f;
    private static final float FM_CLOSE_Y = FM_Y + FM_H - 36f;
    private static final float FM_CLOSE_W = 104f;
    private static final float FM_CLOSE_H = 28f;

    private static final Color WATER = new Color(0.10f, 0.36f, 0.52f, 1f);
    private static final Color WATER_RAIN = new Color(0.08f, 0.22f, 0.34f, 1f);
    private static final Color WATER_FOG = new Color(0.22f, 0.32f, 0.38f, 1f);
    private static final Color GRID = new Color(0.14f, 0.42f, 0.58f, 1f);
    private static final Color HULL = new Color(0.70f, 0.38f, 0.18f, 1f);
    private static final Color SAIL = new Color(0.93f, 0.90f, 0.80f, 1f);
    private static final Color PORT_C = new Color(0.92f, 0.78f, 0.28f, 1f);
    private static final Color ISLE_C = new Color(0.28f, 0.62f, 0.34f, 1f);
    private static final Color PIRATE_C = new Color(0.72f, 0.16f, 0.14f, 1f);
    private static final Color PANEL = new Color(0f, 0f, 0f, 0.55f);

    private enum Overlay { NONE, PORT, ISLAND, MAP, CODEX, CARGO, PRICE, FAIL }

    private final NanHaiVoyage game;
    private GameState g;

    private OrthographicCamera worldCam;
    private Viewport worldVp;
    private Viewport hudVp;
    private Stage stage;
    private ShapeRenderer shapes;
    private PixelMapRenderer pixelMap;
    private final Vector3 tmp = new Vector3();
    private final GlyphLayout layout = new GlyphLayout();

    private Overlay overlay = Overlay.NONE;
    private int cargoTab;
    private int selectedGood = -1;
    private int selectedBeast = -1;
    private int selectedHerb = -1;
    // Popups the player explicitly closed stay closed until the context changes
    // (docking at a different port / reaching a different island / leaving).
    private int dismissedPort = -1;
    private int dismissedIsland = -1;
    private boolean dismissedFail;

    private Table menuRoot;
    private Label hudLine;
    private TextButton btnCargo;
    private TextButton btnCodex;
    private TextButton btnCtx;      // 港口 / 岛屿 contextual reopen
    private TextButton btnCancelAuto;
    private TextButton btnCancelLock;
    private TextButton btnAccel;
    private TextButton btnDecel;

    private boolean stickActive;
    private float stickCX = 170f, stickCY = 180f, stickR = 60f;
    private float stickKX, stickKY;
    private int stickPointer = -1;
    private boolean accelDown, decelDown;
    private boolean loggedFirstFrame;

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
            worldCam = new OrthographicCamera();
            worldVp = new FitViewport(960, 540, worldCam);
            hudVp = new FitViewport(HUD_W, HUD_H);
            stage = new Stage(hudVp, game.batch);
            shapes = new ShapeRenderer();
            pixelMap = null; // vector fallbacks still render the ship + map
            buildHud();
            Gdx.input.setInputProcessor(new InputMultiplexer(stage, new WorldInput()));
            overlay = Overlay.NONE;
            rebuildMenu();
            g.toast("画面组件加载失败，已启用简化渲染。");
        }
        if (game.state != null) {
            g = game.state;
        }
        Gdx.app.error("VoyageEnter", "show() end");
    }

    private void buildAll() {
        g = game.state;
        worldCam = new OrthographicCamera();
        worldVp = new FitViewport(960, 540, worldCam);
        Gdx.app.error("VoyageEnter", "world camera+viewport created");
        hudVp = new FitViewport(HUD_W, HUD_H);
        stage = new Stage(hudVp, game.batch);
        Gdx.app.error("VoyageEnter", "hud viewport+stage created");
        shapes = new ShapeRenderer();
        pixelMap = new PixelMapRenderer();
        Gdx.app.error("VoyageEnter", "shape renderer + pixel map textures created");

        buildHud();
        InputMultiplexer mux = new InputMultiplexer(stage, new WorldInput());
        Gdx.input.setInputProcessor(mux);

        if (g.failed) {
            overlay = Overlay.FAIL;
        } else if (g.dockedPort >= 0) {
            overlay = Overlay.PORT;
        } else if (g.islandMenu >= 0) {
            overlay = Overlay.ISLAND;
        }
        rebuildMenu();
        Gdx.app.error("VoyageEnter", "hud + menu built, overlay=" + overlay);
    }

    private void disposeQuietly() {
        try {
            if (stage != null) { stage.dispose(); }
        } catch (Throwable ignored) {}
        try {
            if (shapes != null) { shapes.dispose(); }
        } catch (Throwable ignored) {}
        try {
            if (pixelMap != null) { pixelMap.dispose(); }
        } catch (Throwable ignored) {}
        stage = null;
        shapes = null;
        pixelMap = null;
    }

    private void buildHud() {
        stage.clear();

        // Top-left live status line.
        hudLine = new Label("", game.skin, "small");
        hudLine.setAlignment(Align.left);
        Table top = new Table();
        top.setFillParent(true);
        top.top().left().pad(12);
        top.add(hudLine).left().expandX().fillX();
        stage.addActor(top);

        // Bottom-right 加速 / 减速 (hold to keep sailing).
        Table right = new Table();
        right.setFillParent(true);
        right.bottom().right().pad(16);
        btnAccel = new TextButton("加速", game.skin, "go");
        btnDecel = new TextButton("减速", game.skin, "danger");
        hold(btnAccel, true);
        hold(btnDecel, false);
        right.add(btnAccel).width(124).height(72).pad(6).row();
        right.add(btnDecel).width(124).height(72).pad(6);
        stage.addActor(right);

        // Far-left circular rail: 货物 / 图鉴 / 港口 on the very left edge. They are
        // real stage actors sized 70x70, stacked vertically, clear of both the
        // avatar (top-left) and the joystick (bottom-left).
        btnCargo = roundBtn("货物");
        btnCodex = roundBtn("图鉴");
        btnCtx = roundBtn("港口");
        btnCargo.addListener(click(() -> {
            if (overlay == Overlay.CARGO) {
                closePopup();
            } else if (overlay != Overlay.MAP) {
                overlay = Overlay.CARGO;
                rebuildMenu();
            }
        }));
        btnCodex.addListener(click(() -> {
            if (overlay == Overlay.CODEX) {
                closePopup();
            } else if (overlay != Overlay.MAP) {
                overlay = Overlay.CODEX;
                rebuildMenu();
            }
        }));
        btnCtx.addListener(click(() -> {
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
                g.toast("不在港口或岛屿附近：靠近港口/岛屿会自动弹出菜单。");
            }
        }));
        placeRailButton(btnCargo, 0);
        placeRailButton(btnCodex, 1);
        placeRailButton(btnCtx, 2);
        stage.addActor(btnCargo);
        stage.addActor(btnCodex);
        stage.addActor(btnCtx);

        // Contextual 取消自动 / 取消锁定 — slim strip at the very top-center so no
        // popup or HUD element can swallow their touches.
        btnCancelAuto = new TextButton("取消自动", game.skin, "danger");
        btnCancelLock = new TextButton("取消锁定", game.skin, "danger");
        btnCancelAuto.addListener(click(() -> {
            g.cancelAutoSail();
            rebuildMenu();
        }));
        btnCancelLock.addListener(click(() -> g.cancelLock()));
        Table mid = new Table();
        mid.setFillParent(true);
        mid.top().padTop(8);
        mid.add(btnCancelAuto).width(132).height(38).pad(4);
        mid.add(btnCancelLock).width(132).height(38).pad(4);
        stage.addActor(mid);

        // Popup root: right side, clear of the minimap circle on top-right.
        menuRoot = new Table();
        menuRoot.setFillParent(true);
        menuRoot.right().top().padTop(96).padRight(172);
        stage.addActor(menuRoot);
    }

    private TextButton roundBtn(String label) {
        TextButton b = new TextButton(label, game.skin, "circ");
        b.getLabel().setFontScale(1f);
        return b;
    }

    /** Positions the i-th left-rail circular button (0 = 货物 top, 2 = 港口 bottom). */
    private void placeRailButton(TextButton b, int i) {
        float cy = LB_TOP - i * LB_GAP;
        b.setBounds(LB_X - LB_SIZE / 2f, cy - LB_SIZE / 2f, LB_SIZE, LB_SIZE);
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
                boolean modal = overlay == Overlay.PORT || overlay == Overlay.ISLAND || overlay == Overlay.FAIL;
                if (modal) {
                    // A popup is open: the world pauses by design. The button still
                    // reacts (pressed feedback) and explains instead of silently
                    // doing nothing — never a dead control.
                    g.toast("港口/岛屿菜单开着世界暂停：先「离港/离开岛屿」或「关闭」再开船。");
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

    /** Closes whatever popup is open. A closed popup stays closed for the current
     * context (port / island / failure) so the auto-open logic cannot fight the
     * player's 关闭 tap. */
    private void closePopup() {
        if (overlay == Overlay.PORT && g.dockedPort >= 0) {
            dismissedPort = g.dockedPort;
        } else if (overlay == Overlay.ISLAND && g.islandMenu >= 0) {
            dismissedIsland = g.islandMenu;
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
        menuRoot.clear();
        refreshBaseCtx();
        if (overlay == Overlay.NONE || overlay == Overlay.MAP) {
            return;
        }
        Table box = new Table(game.skin);
        box.pad(8f);
        box.background(game.skin.getDrawable("panel"));
        if (overlay == Overlay.PORT) {
            portTable(box);
        } else if (overlay == Overlay.ISLAND) {
            islandTable(box);
        } else if (overlay == Overlay.CODEX) {
            codexTable(box);
        } else if (overlay == Overlay.CARGO) {
            cargoTable(box);
        } else if (overlay == Overlay.PRICE) {
            priceTable(box);
        } else if (overlay == Overlay.FAIL) {
            failTable(box);
        }
        ScrollPane sp = new ScrollPane(box, game.skin);
        sp.setFadeScrollBars(false);
        menuRoot.add(sp).width(520).maxHeight(500);
    }

    /** Left-rail context label: 港口 always; becomes 岛屿 while pausing on an island. */
    private void refreshBaseCtx() {
        if (g == null) {
            return;
        }
        if (g.islandMenu >= 0) {
            btnCtx.setText("岛屿");
        } else {
            btnCtx.setText("港口");
        }
    }

    /** Popup title row: horizontal title on the left, 关闭 button top-right. */
    private void menuHeader(Table box, String title) {
        Table h = new Table();
        Label t = new Label(title, game.skin);
        t.setWrap(false);
        TextButton close = new TextButton("关闭", game.skin, "danger");
        close.getLabel().setFontScale(0.9f);
        close.addListener(click(this::closePopup));
        h.add(t).left().expandX().padLeft(2);
        h.add(close).width(88).height(38);
        box.add(h).width(MENU_W).padBottom(6).row();
    }

    private void portTable(Table box) {
        IconLib.checkAgainstCatalog();
        int p = g.dockedPort;
        menuHeader(box, Catalog.PORTS[p] + " · 世界暂停");
        box.add(infoRow("银 " + g.silver + "    欠 " + g.debt + "    舱 " + g.cargoUsed() + "/" + g.cargoCap))
                .width(MENU_W).padBottom(5).row();
        pairRow(box,
                "补补给", () -> { g.toast(g.refillSupply()); persist(); rebuildMenu(); },
                "还债(全还)", () -> { g.toast(g.repay(g.debt)); persist(); rebuildMenu(); });
        pairRow(box,
                "修理", () -> { g.toast(g.repair()); persist(); rebuildMenu(); },
                "离港", () -> {
                    g.leavePort();
                    persist();
                    dismissedPort = -1;
                    overlay = Overlay.NONE;
                    rebuildMenu();
                });
        pairRow(box,
                "升仓库 " + g.warehouseCost(), () -> { g.toast(g.upgradeWarehouse()); persist(); rebuildMenu(); },
                "升炮火 " + g.cannonCost(), () -> { g.toast(g.upgradeCannon()); persist(); rebuildMenu(); });
        pairRow(box,
                "升编制 " + g.crewCapCost(), () -> { g.toast(g.upgradeCrewCap()); persist(); rebuildMenu(); },
                "雇人 " + Catalog.HIRE_COST, () -> { g.toast(g.hireCrew()); persist(); rebuildMenu(); });
        box.add(infoRow("船员 " + g.crew + "/" + g.crewCap + "    炮伤 " + g.firepower()
                + "    耐久 " + (int) g.hull)).width(MENU_W).padTop(5).padBottom(4).row();
        tabs(box);
        listItems(box, true);
    }

    private void islandTable(Table box) {
        menuHeader(box, Catalog.ISLANDS[g.islandMenu] + " · 搜采");
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
        menuHeader(box, "货舱（三栏共用容量 " + g.cargoUsed() + "/" + g.cargoCap + "）");
        tabs(box);
        listItems(box, g.dockedPort >= 0);
        if (g.dockedPort < 0) {
            box.add(wrapLbl("海上可丢货，丢了就没了。点货物再点丢掉。")).width(MENU_W - 10).left().padTop(4).row();
            Table act = new Table();
            act.add(btn("丢掉选中 x1", this::dumpSelected)).width(220).height(44);
            box.add(act).width(MENU_W).padTop(4).row();
        }
    }

    private void dumpSelected() {
        String m;
        if (cargoTab == 0 && selectedGood >= 0) m = g.dumpTrade(selectedGood, 1);
        else if (cargoTab == 1 && selectedBeast >= 0) m = g.dumpBeast(selectedBeast, 1);
        else if (cargoTab == 2 && selectedHerb >= 0) m = g.dumpHerb(selectedHerb, 1);
        else m = "先点一种货。";
        g.toast(m);
        rebuildMenu();
    }

    private void priceTable(Table box) {
        if (selectedGood < 0) {
            overlay = Overlay.CARGO;
            rebuildMenu();
            return;
        }
        int gidx = selectedGood;
        TextureRegionDrawable ico = IconLib.good(gidx);
        String head = (ico != null ? "" : "") + Catalog.GOODS[gidx] + " · 各港行情（固定）";
        menuHeader(box, head);
        if (ico != null) {
            Table headRow = new Table();
            headRow.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(ico)).size(28, 28).padRight(6);
            box.add(headRow).width(MENU_W).left().padBottom(2).row();
        }
        int here = g.dockedPort;
        for (int i = 0; i < Catalog.PORTS.length; i++) {
            String mark = i == here ? "  <-本港" : "";
            box.add(infoRow(Catalog.PORTS[i] + "    " + Catalog.goodPrice(i, gidx) + " 两" + mark))
                    .width(MENU_W).left().padBottom(1).row();
        }
        Table act = new Table();
        act.add(btn("买 1", () -> { g.toast(g.buyGood(here, gidx, 1)); persist(); rebuildMenu(); })).width(150).height(44);
        act.add(btn("卖 1", () -> { g.toast(g.sellGood(here, gidx, 1)); persist(); rebuildMenu(); })).width(150).height(44).padLeft(8);
        act.add(btn("返回列表", () -> { overlay = Overlay.CARGO; rebuildMenu(); })).width(180).height(44).padLeft(8);
        box.add(act).width(MENU_W).padTop(6).row();
    }

    private void codexTable(Table box) {
        menuHeader(box, "图鉴");
        box.add(new Label("异兽", game.skin, "small")).width(MENU_W).left().padTop(2).padBottom(2).row();
        for (int i = 0; i < Catalog.BEASTS.length; i++) {
            codexRow(box, IconLib.beast(i), g.beastFound[i] ? Catalog.BEASTS[i] : "？？？");
        }
        box.add(new Label("草药（一期只卖钱）", game.skin, "small")).width(MENU_W).left().padTop(6).padBottom(2).row();
        for (int i = 0; i < Catalog.HERBS.length; i++) {
            codexRow(box, IconLib.herb(i), g.herbFound[i] ? Catalog.HERBS[i] : "？？？");
        }
    }

    private void failTable(Table box) {
        menuHeader(box, "失败：" + g.failReason);
        box.add(wrapLbl("补给空或船沉都直接失败，读取上次靠港存档。")).width(MENU_W - 10).left().padBottom(6).row();
        Table act = new Table();
        act.add(btn("读档重来", () -> {
            game.state = GameState.fromSave(game.accounts.load(game.currentUser));
            g = game.state;
            dismissedPort = -1;
            dismissedIsland = -1;
            dismissedFail = false;
            overlay = g.dockedPort >= 0 ? Overlay.PORT : Overlay.NONE;
            rebuildMenu();
        })).width(240).height(46);
        box.add(act).width(MENU_W).row();
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
        Table t = new Table();
        t.add(btn(cargoTab == 0 ? "[商货]" : "商货", () -> { cargoTab = 0; rebuildMenu(); })).width(160).height(40);
        t.add(btn(cargoTab == 1 ? "[异兽]" : "异兽", () -> { cargoTab = 1; rebuildMenu(); })).width(160).height(40).padLeft(8);
        t.add(btn(cargoTab == 2 ? "[草药]" : "草药", () -> { cargoTab = 2; rebuildMenu(); })).width(160).height(40).padLeft(8);
        box.add(t).width(MENU_W).padBottom(4).row();
    }

    private void listItems(Table box, boolean trading) {
        if (cargoTab == 0) {
            int port = Math.max(0, g.dockedPort);
            for (int i = 0; i < Catalog.GOODS.length; i++) {
                final int idx = i;
                String s = Catalog.GOODS[i] + "   持有 " + g.trade[i];
                if (g.dockedPort >= 0) {
                    s += "    本港 " + Catalog.goodPrice(port, i) + " 两";
                }
                final String txt = s;
                iconRow(box, IconLib.good(i), txt, () -> {
                    selectedGood = idx;
                    if (trading && g.dockedPort >= 0) {
                        overlay = Overlay.PRICE;
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
                iconRow(box, IconLib.beast(i), txt, () -> {
                    selectedBeast = idx;
                    if (trading && g.dockedPort >= 0 && g.beasts[idx] > 0) {
                        g.toast(g.sellBeast(idx, 1));
                        persist();
                    }
                    rebuildMenu();
                });
            }
        } else {
            for (int i = 0; i < Catalog.HERBS.length; i++) {
                final int idx = i;
                if (g.herbs[i] <= 0 && !(trading && g.herbFound[i])) {
                    continue;
                }
                String s = Catalog.HERBS[i] + "    x" + g.herbs[i] + "    卖价 " + Catalog.HERB_PRICE[i] + "（只卖）";
                final String txt = s;
                iconRow(box, IconLib.herb(i), txt, () -> {
                    selectedHerb = idx;
                    if (trading && g.dockedPort >= 0 && g.herbs[idx] > 0) {
                        g.toast(g.sellHerb(idx, 1));
                        persist();
                    }
                    rebuildMenu();
                });
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

    private void persist() {
        if (game.currentUser != null) {
            game.accounts.save(game.currentUser, g.toSave());
        }
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
        g.steerInput = stickActive ? stickKX : g.steerInput;
        g.onManualSteer();
        // World update is gated by the model (paused while docked/island/failed).
        // A docked ship whose popup was closed is un-paused by undockIfNeeded() in
        // the input handlers the moment the player touches a control, so the ship
        // can always sail unless a popup is actually open (0.25.2 lockup fix).
        if (!g.worldPaused()) {
            g.update(delta);
        }

        // Context transitions. Explicitly dismissed popups stay dismissed until the
        // context changes; otherwise dock/island/fail auto-open their popup. While
        // the full-map modal is up, nothing auto-opens under it: the modal keeps
        // covering the whole UI until the player closes it, then the pending
        // context popup opens normally.
        if (overlay != Overlay.MAP && g.failed && !dismissedFail && overlay != Overlay.FAIL) {
            overlay = Overlay.FAIL;
            rebuildMenu();
        } else if (overlay != Overlay.MAP && overlay != Overlay.FAIL && g.dockedPort >= 0
                && dismissedPort != g.dockedPort) {
            overlay = Overlay.PORT;
            persist();
            rebuildMenu();
        } else if (overlay != Overlay.MAP && overlay != Overlay.FAIL && overlay != Overlay.PORT
                && g.islandMenu >= 0 && dismissedIsland != g.islandMenu) {
            overlay = Overlay.ISLAND;
            rebuildMenu();
        }

        btnCancelAuto.setVisible(g.autoSail && overlay != Overlay.MAP);
        btnCancelLock.setVisible(g.combatLock && overlay != Overlay.MAP);
        hudLine.setText(statusText());

        Color bg = WATER;
        if (g.weather == GameState.WeatherKind.RAIN) bg = WATER_RAIN;
        else if (g.weather == GameState.WeatherKind.FOG) bg = WATER_FOG;
        ScreenUtils.clear(bg);
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);

        worldCam.position.set(g.x, g.y, 0);
        worldCam.update();
        worldVp.apply();
        shapes.setProjectionMatrix(worldCam.combined);
        game.batch.setProjectionMatrix(worldCam.combined);
        drawWorld();

        hudVp.apply();
        shapes.setProjectionMatrix(hudVp.getCamera().combined);
        game.batch.setProjectionMatrix(hudVp.getCamera().combined);

        // Full-map modal: covers the ENTIRE screen. The HUD stage is hidden (not
        // drawn and not hit-testable) so every button below — cargo/codex/port
        // rail, joystick, accel/decel, minimap — is invisible AND unclickable;
        // WorldInput alone handles all touches while it is open.
        boolean mapOpen = overlay == Overlay.MAP;
        if (mapOpen) {
            drawFullMap();
        } else {
            drawHudDecor();
        }
        stage.getRoot().setVisible(!mapOpen);
        stage.act(delta);
        if (!mapOpen) {
            stage.draw();
        }

        if (!mapOpen && g.toastT > 0) {
            game.batch.begin();
            layout.setText(game.font, g.toast);
            game.font.draw(game.batch, g.toast, (HUD_W - layout.width) / 2f, 46f);
            game.batch.end();
        }
    }

    private String statusText() {
        String s = "银" + g.silver + " 欠" + g.debt
                + " 补给" + (int) g.supply + "/" + (int) g.supplyMax
                + " 耐久" + (int) g.hull
                + " 船员" + g.crew + "/" + g.crewCap
                + " 舱" + g.cargoUsed() + "/" + g.cargoCap
                + "  " + g.windLabel() + g.weatherLabel()
                + " 速" + (int) g.speed;
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
            s += " 海盗";
        }
        return s;
    }

    private void readKeyboard() {
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
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && overlay == Overlay.MAP) {
            overlay = Overlay.NONE;
        }
    }

    private void drawWorld() {
        if (pixelMap != null) {
            game.batch.begin();
            pixelMap.drawWater(game.batch, g);
            game.batch.end();
            // Islands / ports / pirate only — the player ship is layered last.
            game.batch.begin();
            pixelMap.drawMarkers(game.batch, g);
            game.batch.end();
        }
        // The camera centers on the ship (see render) and the ship is always drawn
        // ABOVE the island/port tiles: vector hull outline first (never hidden),
        // then the sprite on top when the texture really loaded. If the texture
        // failed, the outline is enlarged 1.5x and stands alone. Without the
        // begin()/end() pass below every frame throws "begin must be called first"
        // and the app dies on the first voyage frame (the 0.24.x login crash).
        boolean shipSpriteOk = pixelMap != null && pixelMap.shipSpriteOk;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawShipSilhouette(g.x, g.y, g.headingDeg, shipSpriteOk ? 0.94f : 1.5f);
        shapes.end();
        if (shipSpriteOk) {
            game.batch.begin();
            pixelMap.drawShip(game.batch, g);
            game.batch.end();
        }

        if (g.muzzleFlash > 0 && g.pirateAlive && g.combatLock) {
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(1f, 0.85f, 0.3f, 1f);
            shapes.rectLine(g.x, g.y, g.pirateX, g.pirateY, 2.5f);
            shapes.end();
        }

        game.batch.begin();
        BitmapFont f = game.fontSmall;
        boolean hideFar = g.weather != GameState.WeatherKind.CLEAR;
        for (int i = 0; i < Catalog.PORTS.length; i++) {
            float d = Catalog.dist(g.x, g.y, Catalog.PORT_X[i], Catalog.PORT_Y[i]);
            if (hideFar && d > 520) continue;
            f.draw(game.batch, Catalog.PORTS[i], Catalog.PORT_X[i] + 18, Catalog.PORT_Y[i] + 10);
        }
        for (int i = 0; i < Catalog.ISLANDS.length; i++) {
            float d = Catalog.dist(g.x, g.y, Catalog.ISLAND_X[i], Catalog.ISLAND_Y[i]);
            if (hideFar && d > 520) continue;
            f.draw(game.batch, Catalog.ISLANDS[i], Catalog.ISLAND_X[i] + 20, Catalog.ISLAND_Y[i] + 8);
        }
        game.batch.end();
    }

    /** Vector junk silhouette of the player ship. It is drawn above islands/water
     * but under the ship sprite (or alone, enlarged, when the sprite texture is
     * missing) so the ship can never disappear from the sea. scale 1.0 ≈ sprite
     * footprint; bigger values are the texture-failure fallback. */
    private void drawShipSilhouette(float x, float y, float headingDeg, float scale) {
        float rad = headingDeg * MathUtils.degreesToRadians;
        float ux = MathUtils.cos(rad), uy = MathUtils.sin(rad);
        float px = -uy, py = ux;
        float bowX = x + ux * 24f * scale, bowY = y + uy * 24f * scale;
        float sternX = x - ux * 20f * scale, sternY = y - uy * 20f * scale;
        shapes.setColor(0.76f, 0.62f, 0.28f, 1f); // sail tan body
        shapes.rectLine(sternX, sternY, bowX, bowY, 13f * scale);
        shapes.setColor(0.45f, 0.26f, 0.12f, 1f); // dark hull deck
        shapes.rectLine(x - px * 5f * scale - ux * 22f * scale, y - py * 5f * scale - uy * 22f * scale,
                x + px * 5f * scale + ux * 22f * scale, y + py * 5f * scale + uy * 22f * scale, 8f * scale);
        shapes.setColor(0.97f, 0.94f, 0.84f, 1f); // bright sail canvas
        shapes.rectLine(x + ux * 2f * scale - px * 5f * scale, y + uy * 2f * scale - py * 5f * scale,
                x + ux * 2f * scale + px * 5f * scale, y + uy * 2f * scale + py * 5f * scale, 11f * scale);
        shapes.setColor(1f, 0.92f, 0.45f, 1f); // mast cap
        shapes.circle(bowX, bowY, 2.6f * scale);
    }

    private static boolean isFinite(float v) {
        return !Float.isNaN(v) && !Float.isInfinite(v);
    }

    // ------------------------------------------------------- HUD drawing

    /** Round minimap (top-right), virtual stick (left) and captain avatar. */
    private void drawHudDecor() {
        boolean mapOpen = overlay == Overlay.MAP;
        if (!mapOpen) {
            drawRoundMinimap();
        }
        drawAvatarAndStick();
    }

    /** Captain avatar (top-left, under the status line) + bottom-left joystick. */
    private void drawAvatarAndStick() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        // avatar ring + round color block
        shapes.setColor(0.03f, 0.05f, 0.09f, 0.95f);
        shapes.circle(AV_X, AV_Y, AV_R + 3f);
        shapes.setColor(0.55f, 0.20f, 0.18f, 1f);
        shapes.circle(AV_X, AV_Y, AV_R);
        shapes.setColor(0.86f, 0.66f, 0.50f, 1f);
        shapes.circle(AV_X, AV_Y, AV_R * 0.55f);

        // virtual joystick base (bottom-left)
        shapes.setColor(0f, 0f, 0f, 0.35f);
        shapes.circle(stickCX, stickCY, stickR);
        shapes.setColor(0.10f, 0.18f, 0.26f, 0.6f);
        shapes.circle(stickCX, stickCY, stickR - 7f);
        shapes.setColor(0.85f, 0.85f, 0.85f, 0.55f);
        float knx = stickCX, kny = stickCY;
        if (stickActive) {
            knx = stickCX + stickKX * (stickR - 20f);
            kny = stickCY + stickKY * (stickR - 20f);
        }
        shapes.circle(knx, kny, 20f);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.8f, 0.8f, 0.8f, 0.8f);
        shapes.circle(stickCX, stickCY, stickR);
        shapes.setColor(0.95f, 0.9f, 0.8f, 0.9f);
        shapes.circle(AV_X, AV_Y, AV_R + 3f);
        shapes.end();

        game.batch.begin();
        layout.setText(game.fontSmall, "船长");
        game.fontSmall.draw(game.batch, "船长", AV_X + AV_R + 10f, AV_Y + 4f);
        game.batch.end();
    }

    /** Round minimap with markers clipped to the circle. */
    private void drawRoundMinimap() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.02f, 0.05f, 0.09f, 0.92f);
        shapes.circle(MM_CX, MM_CY, MM_R + 5f);
        shapes.setColor(0.06f, 0.19f, 0.27f, 0.97f);
        shapes.circle(MM_CX, MM_CY, MM_R);
        shapes.end();

        float rr = MM_R - 6f;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        float scaleX = 2f * rr / Catalog.WORLD_W;
        float scaleY = 2f * rr / Catalog.WORLD_H;
        for (int i = 0; i < Catalog.ISLANDS.length; i++) {
            float px = MM_CX + (Catalog.ISLAND_X[i] - Catalog.WORLD_W / 2f) * scaleX;
            float py = MM_CY + (Catalog.ISLAND_Y[i] - Catalog.WORLD_H / 2f) * scaleY;
            float ddx = px - MM_CX, ddy = py - MM_CY;
            if (ddx * ddx + ddy * ddy > (rr - 3f) * (rr - 3f)) continue;
            shapes.setColor(0f, 0f, 0f, 0.35f);
            shapes.circle(px + 1f, py - 1f, 4.2f);
            shapes.setColor(ISLE_C);
            shapes.circle(px, py, 4.2f);
        }
        for (int i = 0; i < Catalog.PORTS.length; i++) {
            float px = MM_CX + (Catalog.PORT_X[i] - Catalog.WORLD_W / 2f) * scaleX;
            float py = MM_CY + (Catalog.PORT_Y[i] - Catalog.WORLD_H / 2f) * scaleY;
            float ddx = px - MM_CX, ddy = py - MM_CY;
            if (ddx * ddx + ddy * ddy > (rr - 3f) * (rr - 3f)) continue;
            shapes.setColor(0f, 0f, 0f, 0.35f);
            shapes.circle(px + 1f, py - 1f, 3.8f);
            shapes.setColor(PORT_C);
            shapes.circle(px, py, 3.8f);
        }
        // Player ship (white ring + hull dot + short heading tick, clipped).
        float px = MM_CX + (g.x - Catalog.WORLD_W / 2f) * scaleX;
        float py = MM_CY + (g.y - Catalog.WORLD_H / 2f) * scaleY;
        float ddx = px - MM_CX, ddy = py - MM_CY;
        if (ddx * ddx + ddy * ddy < (rr - 8f) * (rr - 8f)) {
            float rad = g.headingDeg * MathUtils.degreesToRadians;
            shapes.setColor(1f, 1f, 1f, 0.9f);
            shapes.circle(px, py, 5.4f);
            shapes.setColor(HULL);
            shapes.circle(px, py, 3.4f);
            shapes.setColor(1f, 0.95f, 0.55f, 1f);
            float tx = px + MathUtils.cos(rad) * 9f;
            float ty = py + MathUtils.sin(rad) * 9f;
            if (Math.abs(tx - MM_CX) > rr - 2f || Math.abs(ty - MM_CY) > rr - 2f) {
                tx = px + MathUtils.cos(rad) * 6f;
                ty = py + MathUtils.sin(rad) * 6f;
            }
            shapes.rectLine(px, py, tx, ty, 2.2f);
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.80f, 0.87f, 0.95f, 0.9f);
        shapes.circle(MM_CX, MM_CY, MM_R + 5f);
        shapes.end();

        game.batch.begin();
        layout.setText(game.fontSmall, "小地图(点开全图)");
        game.fontSmall.draw(game.batch, "小地图(点开全图)", MM_CX - layout.width / 2f, MM_CY + MM_R + 20f);
        game.batch.end();
    }



    /** Fullscreen modal full map: a dim veil covers the whole HUD (the stage is
     * hidden while overlay == Overlay.MAP, so no other control is visible or
     * clickable). 关闭 sits top-right inside the map; blank taps keep it open.
     * 0.25.3: rendered as a top-down pixel sea chart — deep sea + wave flecks,
     * simplified mainland/Indochina/Hainan/Xisha land blocks aligned to Catalog
     * world coords (广州/潮州 NE, 佛逝/真腊/占城 SW, 琼州/崖州 on Hainan), golden
     * town icons with names, green island blobs, bright yellow player marker. */
    private void drawFullMap() {
        float ix = FM_X + 10f, iy = FM_Y + 10f, iw = FM_W - 20f, ih = FM_H - 20f;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        // fullscreen dim veil over the entire screen
        shapes.setColor(0.02f, 0.04f, 0.07f, 0.96f);
        shapes.rect(0f, 0f, HUD_W, HUD_H);
        // map frame + deep sea base
        shapes.setColor(0.02f, 0.08f, 0.13f, 1f);
        shapes.rect(FM_X, FM_Y, FM_W, FM_H);
        shapes.setColor(0.05f, 0.15f, 0.24f, 1f);
        shapes.rect(ix, iy, iw, ih);
        // wave flecks: deterministic pseudo-random lighter dots across the sea
        shapes.setColor(0.11f, 0.29f, 0.40f, 0.85f);
        long s = 0x9E3779B97F4A7C15L;
        for (int k = 0; k < 130; k++) {
            s = s * 6364136223846793005L + 1442695040888963407L;
            double rx = ((s >>> 33) & 0x7fffffffL) / (double) 0x7fffffffL;
            s = s * 6364136223846793005L + 1442695040888963407L;
            double ry = ((s >>> 33) & 0x7fffffffL) / (double) 0x7fffffffL;
            float wx = (float) (ix + rx * iw);
            float wy = (float) (iy + ry * ih);
            float sz = k % 3 == 0 ? 4.2f : 2.8f;
            shapes.rect(wx, wy, sz, sz);
        }
        // simplified land blocks (world coords -> chart, so ports stay put)
        drawChartLand(ix, iy, iw, ih);
        // island blobs + golden town icons (drawn over land/sea, under labels)
        for (int i = 0; i < Catalog.ISLANDS.length; i++) {
            float[] xy = mapToUi(Catalog.ISLAND_X[i], Catalog.ISLAND_Y[i], ix, iy, iw, ih);
            shapes.setColor(0f, 0f, 0f, 0.4f);
            shapes.circle(xy[0] + 1.5f, xy[1] - 1.5f, 9f);
            shapes.setColor(ISLE_C);
            shapes.circle(xy[0], xy[1], 9f);
            shapes.setColor(0.16f, 0.38f, 0.20f, 1f);
            shapes.circle(xy[0], xy[1], 5f);
        }
        for (int i = 0; i < Catalog.PORTS.length; i++) {
            float[] xy = mapToUi(Catalog.PORT_X[i], Catalog.PORT_Y[i], ix, iy, iw, ih);
            shapes.setColor(0.20f, 0.12f, 0.05f, 1f);
            shapes.rect(xy[0] - 5f, xy[1] - 5f, 10f, 10f);
            shapes.setColor(0.93f, 0.80f, 0.30f, 1f);
            shapes.rect(xy[0] - 3f, xy[1] - 3f, 6f, 6f);
        }
        // Player ship: white ring + bright yellow dot + heading tick, drawn last
        // so it can never hide under a port/island marker or land block.
        float[] me = mapToUi(g.x, g.y, ix, iy, iw, ih);
        float rad = g.headingDeg * MathUtils.degreesToRadians;
        shapes.setColor(1f, 1f, 1f, 0.95f);
        shapes.circle(me[0], me[1], 8f);
        shapes.setColor(1f, 0.84f, 0.15f, 1f);
        shapes.circle(me[0], me[1], 6f);
        shapes.setColor(0.25f, 0.18f, 0.03f, 1f);
        shapes.rectLine(me[0], me[1],
                me[0] + MathUtils.cos(rad) * 14f, me[1] + MathUtils.sin(rad) * 14f, 3.2f);
        if (g.weather != GameState.WeatherKind.CLEAR) {
            shapes.setColor(0.7f, 0.75f, 0.8f, g.weather == GameState.WeatherKind.FOG ? 0.45f : 0.28f);
            shapes.rect(ix, iy, iw, ih);
        }
        // 关闭 button (top-right corner inside the map)
        shapes.setColor(0.45f, 0.16f, 0.14f, 0.98f);
        shapes.rect(FM_CLOSE_X, FM_CLOSE_Y, FM_CLOSE_W, FM_CLOSE_H);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.82f, 0.88f, 0.95f, 0.9f);
        shapes.rect(FM_X, FM_Y, FM_W, FM_H);
        shapes.rect(FM_CLOSE_X, FM_CLOSE_Y, FM_CLOSE_W, FM_CLOSE_H);
        shapes.end();
        // labels: title, 关闭, port names beside their icons, island names, 本船
        game.batch.begin();
        layout.setText(game.font, "南海海图：点港口/岛屿自动驶向");
        game.font.draw(game.batch, "南海海图：点港口/岛屿自动驶向", FM_X + 16f, FM_Y + FM_H - 14f);
        layout.setText(game.fontSmall, "关闭");
        game.fontSmall.draw(game.batch, "关闭",
                FM_CLOSE_X + (FM_CLOSE_W - layout.width) / 2f,
                FM_CLOSE_Y + (FM_CLOSE_H + layout.height) / 2f);
        for (int i = 0; i < Catalog.PORTS.length; i++) {
            float[] xy = mapToUi(Catalog.PORT_X[i], Catalog.PORT_Y[i], ix, iy, iw, ih);
            game.fontSmall.draw(game.batch, Catalog.PORTS[i], xy[0] + 8, xy[1] + 4);
        }
        for (int i = 0; i < Catalog.ISLANDS.length; i++) {
            float[] xy = mapToUi(Catalog.ISLAND_X[i], Catalog.ISLAND_Y[i], ix, iy, iw, ih);
            game.fontSmall.draw(game.batch, Catalog.ISLANDS[i], xy[0] + 11, xy[1] - 12);
        }
        game.fontSmall.draw(game.batch, "本船", me[0] + 10, me[1] - 12);
        game.batch.end();
    }

    /** Simplified land blocks: mainland China band + Indochina coast + Leizhou
     * peninsula + Hainan island + Xisha reef dots, projected from Catalog world
     * coords so ports/islands keep their relative positions (广州/潮州 NE on the
     * mainland, 琼州/崖州 on Hainan, 佛逝/真腊/占城 SW on Indochina). */
    private void drawChartLand(float ix, float iy, float iw, float ih) {
        shapes.setColor(0.58f, 0.49f, 0.32f, 1f); // khaki land
        // mainland China band across the top
        rectFromWorld(0f, 2350f, 4800f, 3600f, ix, iy, iw, ih);
        // Indochina block (west) + 占城 coast finger + southern Mekong bulge
        rectFromWorld(0f, 700f, 1500f, 2350f, ix, iy, iw, ih);
        rectFromWorld(1150f, 900f, 1500f, 1300f, ix, iy, iw, ih);
        rectFromWorld(0f, 500f, 1150f, 800f, ix, iy, iw, ih);
        // Hainan island (between the Leizhou peninsula and the open sea)
        rectFromWorld(2150f, 1300f, 2550f, 2050f, ix, iy, iw, ih);
        // rounded coast: Guangxi shore (合浦), Leizhou peninsula (雷州), Chaozhou
        // shore (潮州), Indochina south tip, Hainan blobs
        float[] a;
        a = mapToUi(1800f, 2250f, ix, iy, iw, ih); shapes.circle(a[0], a[1], 46f);
        a = mapToUi(2550f, 2440f, ix, iy, iw, ih); shapes.circle(a[0], a[1], 38f);
        a = mapToUi(4300f, 2520f, ix, iy, iw, ih); shapes.circle(a[0], a[1], 38f);
        a = mapToUi(900f, 650f, ix, iy, iw, ih); shapes.circle(a[0], a[1], 44f);
        a = mapToUi(2350f, 1700f, ix, iy, iw, ih); shapes.circle(a[0], a[1], 62f);
        a = mapToUi(2350f, 1420f, ix, iy, iw, ih); shapes.circle(a[0], a[1], 56f);
        // Xisha reef dots (southeast of Hainan)
        shapes.setColor(0.54f, 0.45f, 0.30f, 1f);
        float[][] xisha = {{2050f, 1100f}, {2150f, 1000f}, {2250f, 1080f},
                {2100f, 1180f}, {2190f, 1140f}};
        for (float[] d : xisha) {
            float[] xy = mapToUi(d[0], d[1], ix, iy, iw, ih);
            shapes.circle(xy[0], xy[1], 9f);
        }
    }

    /** Axis-aligned world rect -> screen rect (the projection is linear and
     * axis-aligned, so mapping the two corners is exact). */
    private void rectFromWorld(float wx1, float wy1, float wx2, float wy2,
                               float ix, float iy, float iw, float ih) {
        float[] p1 = mapToUi(wx1, wy1, ix, iy, iw, ih);
        float[] p2 = mapToUi(wx2, wy2, ix, iy, iw, ih);
        shapes.rect(Math.min(p1[0], p2[0]), Math.min(p1[1], p2[1]),
                Math.abs(p2[0] - p1[0]), Math.abs(p2[1] - p1[1]));
    }

    private float[] mapToUi(float wx, float wy, float x, float y, float w, float h) {
        float px = x + (wx / Catalog.WORLD_W) * w;
        float py = y + (wy / Catalog.WORLD_H) * h;
        return new float[] {px, py};
    }

    // ------------------------------------------------------------- input

    private class WorldInput extends InputAdapter {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            hudVp.unproject(tmp.set(screenX, screenY, 0));
            float hx = tmp.x, hy = tmp.y;

            // Virtual joystick: usable whenever no modal popup is open. While
            // docked with the port menu closed the world is NOT paused, so the
            // first touch undocks the ship and sailing starts immediately.
            float dx = hx - stickCX, dy = hy - stickCY;
            boolean modal = overlay == Overlay.PORT || overlay == Overlay.ISLAND || overlay == Overlay.FAIL;
            if (!modal && overlay != Overlay.MAP
                    && dx * dx + dy * dy <= (stickR + 26f) * (stickR + 26f)) {
                undockIfNeeded();
                stickActive = true;
                stickPointer = pointer;
                setStick(hx, hy);
                g.onManualSteer();
                return true;
            }
            // Round minimap (top-right): toggle the full map. Only from a neutral
            // overlay so an open popup must be closed via its own 关闭 button.
            if (overlay == Overlay.NONE) {
                float mdx = hx - MM_CX, mdy = hy - MM_CY;
                if (mdx * mdx + mdy * mdy <= (MM_R + 14f) * (MM_R + 14f)) {
                    overlay = Overlay.MAP;
                    rebuildMenu();
                    return true;
                }
            }
            if (overlay == Overlay.MAP) {
                return handleFullMapTap(hx, hy);
            }
            // Lock a pirate ship: tap near it while sailing.
            if (g.pirateAlive && overlay == Overlay.NONE) {
                worldVp.unproject(tmp.set(screenX, screenY, 0));
                g.tryLockPirate(tmp.x, tmp.y);
                return false;
            }
            return false;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
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
            if (pointer == stickPointer) {
                stickActive = false;
                stickPointer = -1;
                stickKX = stickKY = 0;
                g.steerInput = 0;
                return true;
            }
            return false;
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
        // stick only steers: horizontal (and angle) turns the ship
        g.steerInput = stickKX;
        if (g.autoSail && (stickKX * stickKX + stickKY * stickKY) > 0.05f) {
            g.cancelAutoSail();
        }
    }

    private boolean handleFullMapTap(float hx, float hy) {
        // 关闭 button and taps on the dimmed margin outside the map close it.
        boolean inMap = hx >= FM_X && hx <= FM_X + FM_W && hy >= FM_Y && hy <= FM_Y + FM_H;
        boolean inClose = hx >= FM_CLOSE_X && hx <= FM_CLOSE_X + FM_CLOSE_W
                && hy >= FM_CLOSE_Y && hy <= FM_CLOSE_Y + FM_CLOSE_H;
        if (!inMap || inClose) {
            overlay = Overlay.NONE;
            rebuildMenu();
            return true;
        }
        float ix = FM_X + 10f, iy = FM_Y + 10f, iw = FM_W - 20f, ih = FM_H - 20f;
        // Tap a port -> auto-sail to it (radius covers the icon + name label).
        for (int i = 0; i < Catalog.PORTS.length; i++) {
            float[] xy = mapToUi(Catalog.PORT_X[i], Catalog.PORT_Y[i], ix, iy, iw, ih);
            float ddx = hx - xy[0], ddy = hy - xy[1];
            if (ddx * ddx + ddy * ddy < 34 * 34) {
                overlay = Overlay.NONE;
                undockIfNeeded(); // full-map tap also sails from a closed-menu dock
                g.startAutoSail(i);
                rebuildMenu();
                return true;
            }
        }
        // Tap an island -> auto-sail to it (arriving opens the island menu).
        for (int i = 0; i < Catalog.ISLANDS.length; i++) {
            float[] xy = mapToUi(Catalog.ISLAND_X[i], Catalog.ISLAND_Y[i], ix, iy, iw, ih);
            float ddx = hx - xy[0], ddy = hy - xy[1];
            if (ddx * ddx + ddy * ddy < 28 * 28) {
                overlay = Overlay.NONE;
                undockIfNeeded();
                g.startAutoSailIsle(i);
                rebuildMenu();
                return true;
            }
        }
        // Blank tap inside the map keeps it open (close via the 关闭 button).
        return true;
    }

    @Override
    public void resize(int width, int height) {
        if (worldVp != null) {
            worldVp.update(width, height);
        }
        if (hudVp != null) {
            hudVp.update(width, height, true);
        }
    }

    @Override
    public void hide() {
        if (stage != null) {
            stage.dispose();
            stage = null;
        }
        if (shapes != null) {
            shapes.dispose();
            shapes = null;
        }
        if (pixelMap != null) {
            pixelMap.dispose();
            pixelMap = null;
        }
    }
}
