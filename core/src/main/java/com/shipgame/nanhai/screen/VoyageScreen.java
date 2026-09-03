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
    // Captain avatar: bottom-left, above it sits the virtual joystick
    private static final float AV_X = 82f;
    private static final float AV_Y = 66f;
    private static final float AV_R = 30f;

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
    private float stickCX = 165f, stickCY = 205f, stickR = 62f;
    private float stickKX, stickKY;
    private int stickPointer = -1;
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

        // Right-center base buttons: 货物 / 图鉴 (+ 港口/岛屿 while paused there).
        Table ctl = new Table();
        ctl.setFillParent(true);
        ctl.right().center().padRight(22);
        btnCargo = new TextButton("货物", game.skin);
        btnCodex = new TextButton("图鉴", game.skin);
        btnCtx = new TextButton("港口", game.skin);
        btnCargo.addListener(click(() -> {
            if (overlay == Overlay.CARGO) {
                closePopup();
            } else {
                overlay = Overlay.CARGO;
                rebuildMenu();
            }
        }));
        btnCodex.addListener(click(() -> {
            if (overlay == Overlay.CODEX) {
                closePopup();
            } else {
                overlay = Overlay.CODEX;
                rebuildMenu();
            }
        }));
        btnCtx.addListener(click(() -> {
            if (g.dockedPort >= 0 && overlay != Overlay.PORT) {
                dismissedPort = -1; // manual reopen re-enables auto-open rules
                overlay = Overlay.PORT;
                rebuildMenu();
            } else if (g.islandMenu >= 0 && overlay != Overlay.ISLAND) {
                dismissedIsland = -1;
                overlay = Overlay.ISLAND;
                rebuildMenu();
            }
        }));
        ctl.add(btnCargo).width(122).height(50).pad(6).row();
        ctl.add(btnCodex).width(122).height(50).pad(6).row();
        ctl.add(btnCtx).width(122).height(50).pad(6);
        stage.addActor(ctl);

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

        // Popup root: right side, above the right-center cluster column, clear of
        // the minimap circle on top-right.
        menuRoot = new Table();
        menuRoot.setFillParent(true);
        menuRoot.right().top().padTop(96).padRight(172);
        stage.addActor(menuRoot);
    }

    private void hold(TextButton b, boolean accel) {
        b.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (accel) g.holdAccel = true;
                else g.holdDecel = true;
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (accel) g.holdAccel = false;
                else g.holdDecel = false;
            }
        });
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

    /** Right-center cluster: 货物/图鉴 always; 港口 or 岛屿 while paused there. */
    private void refreshBaseCtx() {
        if (g == null) {
            return;
        }
        if (g.dockedPort >= 0) {
            btnCtx.setText("港口");
            btnCtx.setVisible(true);
        } else if (g.islandMenu >= 0) {
            btnCtx.setText("岛屿");
            btnCtx.setVisible(true);
        } else {
            btnCtx.setVisible(false);
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
        if (!g.worldPaused() && overlay != Overlay.PORT && overlay != Overlay.ISLAND && overlay != Overlay.FAIL) {
            g.update(delta);
        } else if (!g.worldPaused()) {
            g.update(delta);
        }

        // Context transitions. Explicitly dismissed popups stay dismissed until the
        // context changes; otherwise dock/island/fail auto-open their popup.
        if (g.failed && !dismissedFail && overlay != Overlay.FAIL) {
            overlay = Overlay.FAIL;
            rebuildMenu();
        } else if (overlay != Overlay.FAIL && g.dockedPort >= 0 && dismissedPort != g.dockedPort) {
            overlay = Overlay.PORT;
            persist();
            rebuildMenu();
        } else if (overlay != Overlay.FAIL && overlay != Overlay.PORT && g.islandMenu >= 0
                && dismissedIsland != g.islandMenu) {
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
        drawHudDecor();
        if (overlay == Overlay.MAP) {
            drawFullMap();
        }

        stage.act(delta);
        stage.draw();

        game.batch.begin();
        if (g.toastT > 0) {
            layout.setText(game.font, g.toast);
            game.font.draw(game.batch, g.toast, (HUD_W - layout.width) / 2f, 46f);
        }
        game.batch.end();
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
        boolean accelBtn = btnAccel != null && btnAccel.isPressed();
        boolean decelBtn = btnDecel != null && btnDecel.isPressed();
        g.holdAccel = w || accelBtn;
        g.holdDecel = s || decelBtn;
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
        }
        // Vector silhouette under the sprite: even if the ship texture fails
        // to render on a device, the player ship is always visible at sea.
        // Must run inside an active ShapeRenderer pass: without begin()/end()
        // every render frame throws "begin must be called first" and the app
        // dies on the first voyage frame (the 0.24.2/0.24.3 login crash).
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawShipSilhouette(g.x, g.y, g.headingDeg);
        shapes.end();
        if (pixelMap != null) {
            game.batch.begin();
            pixelMap.drawMarkers(game.batch, g);
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

    /** Vector junk silhouette drawn under the player sprite so the ship can
     * never disappear from the sea (texture failure, alpha issue, etc.). */
    private void drawShipSilhouette(float x, float y, float headingDeg) {
        float rad = headingDeg * MathUtils.degreesToRadians;
        float ux = MathUtils.cos(rad), uy = MathUtils.sin(rad);
        float px = -uy, py = ux;
        float bowX = x + ux * 24f, bowY = y + uy * 24f;
        float sternX = x - ux * 20f, sternY = y - uy * 20f;
        shapes.setColor(0.76f, 0.62f, 0.28f, 1f); // sail tan body
        shapes.rectLine(sternX, sternY, bowX, bowY, 13f);
        shapes.setColor(0.45f, 0.26f, 0.12f, 1f); // dark hull deck
        shapes.rectLine(x - px * 5f - ux * 22f, y - py * 5f - uy * 22f,
                x + px * 5f + ux * 22f, y + py * 5f + uy * 22f, 8f);
        shapes.setColor(0.97f, 0.94f, 0.84f, 1f); // bright sail canvas
        shapes.rectLine(x + ux * 2f - px * 5f, y + uy * 2f - py * 5f,
                x + ux * 2f + px * 5f, y + uy * 2f + py * 5f, 11f);
        shapes.setColor(1f, 0.92f, 0.45f, 1f); // mast cap
        shapes.circle(bowX, bowY, 2.6f);
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

    /** Captain avatar (bottom-left) + virtual joystick above it. */
    private void drawAvatarAndStick() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        // avatar ring + round color block
        shapes.setColor(0.03f, 0.05f, 0.09f, 0.95f);
        shapes.circle(AV_X, AV_Y, AV_R + 4f);
        shapes.setColor(0.55f, 0.20f, 0.18f, 1f);
        shapes.circle(AV_X, AV_Y, AV_R);
        shapes.setColor(0.86f, 0.66f, 0.50f, 1f);
        shapes.circle(AV_X, AV_Y, AV_R * 0.55f);

        // stick base
        shapes.setColor(0f, 0f, 0f, 0.35f);
        shapes.circle(stickCX, stickCY, stickR);
        shapes.setColor(0.10f, 0.18f, 0.26f, 0.6f);
        shapes.circle(stickCX, stickCY, stickR - 7f);
        shapes.setColor(0.85f, 0.85f, 0.85f, 0.55f);
        float knx = stickCX + stickKX * (stickR - 20f);
        float kny = stickCY + stickKY * (stickR - 20f);
        if (!stickActive) {
            knx = stickCX;
            kny = stickCY;
        }
        shapes.circle(knx, kny, 20f);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.8f, 0.8f, 0.8f, 0.8f);
        shapes.circle(stickCX, stickCY, stickR);
        shapes.setColor(0.95f, 0.9f, 0.8f, 0.9f);
        shapes.circle(AV_X, AV_Y, AV_R + 4f);
        shapes.end();

        game.batch.begin();
        layout.setText(game.fontSmall, "船长");
        game.fontSmall.draw(game.batch, "船长", AV_X - layout.width / 2f, AV_Y - layout.height / 2f + 2f);
        game.batch.end();
    }

    private void drawFullMap() {
        float x = 80, y = 70, w = 1120, h = 560;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.72f);
        shapes.rect(x, y, w, h);
        shapes.setColor(0.08f, 0.22f, 0.30f, 1f);
        shapes.rect(x + 10, y + 10, w - 20, h - 20);
        drawMapContents(x + 10, y + 10, w - 20, h - 20, true);
        if (g.weather != GameState.WeatherKind.CLEAR) {
            shapes.setColor(0.7f, 0.75f, 0.8f, g.weather == GameState.WeatherKind.FOG ? 0.45f : 0.28f);
            shapes.rect(x + 10, y + 10, w - 20, h - 20);
        }
        // 关闭 button (top-right corner of the full map)
        float bx = x + w - 112f, by = y + h - 34f, bw = 96f, bh = 26f;
        shapes.setColor(0.45f, 0.16f, 0.14f, 0.95f);
        shapes.rect(bx, by, bw, bh);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.8f, 0.8f, 0.8f, 0.8f);
        shapes.rect(bx, by, bw, bh);
        shapes.end();
        game.batch.begin();
        layout.setText(game.fontSmall, "关闭");
        game.fontSmall.draw(game.batch, "关闭", bx + (bw - layout.width) / 2f, by + (bh + layout.height) / 2f);
        game.font.draw(game.batch, "全图：点港口/岛屿自动驶向", x + 20, y + h - 8);
        // Ports/islands/ship are always labelled: the full map must never be blank.
        for (int i = 0; i < Catalog.PORTS.length; i++) {
            float[] xy = mapToUi(Catalog.PORT_X[i], Catalog.PORT_Y[i], x + 10, y + 10, w - 20, h - 20);
            game.fontSmall.draw(game.batch, Catalog.PORTS[i], xy[0] + 12, xy[1] + 12);
        }
        for (int i = 0; i < Catalog.ISLANDS.length; i++) {
            float[] xy = mapToUi(Catalog.ISLAND_X[i], Catalog.ISLAND_Y[i], x + 10, y + 10, w - 20, h - 20);
            game.fontSmall.draw(game.batch, Catalog.ISLANDS[i], xy[0] + 14, xy[1] - 10);
        }
        float[] me = mapToUi(g.x, g.y, x + 10, y + 10, w - 20, h - 20);
        game.fontSmall.draw(game.batch, "本船", me[0] + 12, me[1] - 10);
        game.batch.end();
    }

    private void drawMapContents(float x, float y, float w, float h, boolean full) {
        float islandR = full ? 12f : 5.5f;
        float portR = full ? 10f : 5f;
        for (int i = 0; i < Catalog.ISLANDS.length; i++) {
            float[] xy = mapToUi(Catalog.ISLAND_X[i], Catalog.ISLAND_Y[i], x, y, w, h);
            shapes.setColor(0f, 0f, 0f, 0.35f);
            shapes.circle(xy[0] + 1.5f, xy[1] - 1.5f, islandR);
            shapes.setColor(ISLE_C);
            shapes.circle(xy[0], xy[1], islandR);
        }
        for (int i = 0; i < Catalog.PORTS.length; i++) {
            float[] xy = mapToUi(Catalog.PORT_X[i], Catalog.PORT_Y[i], x, y, w, h);
            shapes.setColor(0f, 0f, 0f, 0.35f);
            shapes.circle(xy[0] + 1.5f, xy[1] - 1.5f, portR);
            shapes.setColor(PORT_C);
            shapes.circle(xy[0], xy[1], portR);
            if (full) {
                shapes.setColor(0.25f, 0.16f, 0.05f, 1f);
                shapes.circle(xy[0], xy[1], portR * 0.42f);
            }
        }
        // Player: white-outlined hull dot + heading tick, drawn last so it can
        // never be hidden under a port/island marker.
        float[] me = mapToUi(g.x, g.y, x, y, w, h);
        float r = full ? 7f : 4f;
        float rad = g.headingDeg * MathUtils.degreesToRadians;
        shapes.setColor(1f, 1f, 1f, 0.9f);
        shapes.circle(me[0], me[1], r + 2f);
        shapes.setColor(HULL);
        shapes.circle(me[0], me[1], r);
        shapes.setColor(1f, 0.95f, 0.55f, 1f);
        shapes.rectLine(me[0], me[1],
                me[0] + MathUtils.cos(rad) * (r + (full ? 9f : 7f)),
                me[1] + MathUtils.sin(rad) * (r + (full ? 9f : 7f)), full ? 3f : 2f);
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

            // Virtual joystick: only usable while the world is actually moving.
            float dx = hx - stickCX, dy = hy - stickCY;
            if (!g.worldPaused() && overlay != Overlay.MAP
                    && dx * dx + dy * dy <= (stickR + 26f) * (stickR + 26f)) {
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
        float x = 80, y = 70, w = 1120, h = 560;
        // 关闭 button (top-right inside the map) and taps outside the map close it.
        float bx = x + w - 112f, by = y + h - 34f, bw = 96f, bh = 26f;
        boolean inClose = hx >= bx && hx <= bx + bw && hy >= by && hy <= by + bh;
        if (hx < x || hx > x + w || hy < y || hy > y + h || inClose) {
            overlay = Overlay.NONE;
            rebuildMenu();
            return true;
        }
        float ix = x + 10, iy = y + 10, iw = w - 20, ih = h - 20;
        // Tap a port -> auto-sail to it.
        for (int i = 0; i < Catalog.PORTS.length; i++) {
            float[] xy = mapToUi(Catalog.PORT_X[i], Catalog.PORT_Y[i], ix, iy, iw, ih);
            float ddx = hx - xy[0], ddy = hy - xy[1];
            if (ddx * ddx + ddy * ddy < 22 * 22) {
                overlay = Overlay.NONE;
                g.startAutoSail(i);
                rebuildMenu();
                return true;
            }
        }
        // Tap an island -> auto-sail to it (arriving opens the island menu).
        for (int i = 0; i < Catalog.ISLANDS.length; i++) {
            float[] xy = mapToUi(Catalog.ISLAND_X[i], Catalog.ISLAND_Y[i], ix, iy, iw, ih);
            float ddx = hx - xy[0], ddy = hy - xy[1];
            if (ddx * ddx + ddy * ddy < 24 * 24) {
                overlay = Overlay.NONE;
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
