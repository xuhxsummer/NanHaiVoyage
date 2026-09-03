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
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.shipgame.nanhai.NanHaiVoyage;
import com.shipgame.nanhai.PixelMapRenderer;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.shipgame.nanhai.data.Catalog;
import com.shipgame.nanhai.data.GameState;
import com.shipgame.nanhai.ui.IconLib;

public class VoyageScreen extends ScreenAdapter {

    private static final float HUD_W = 1280f;
    private static final float HUD_H = 720f;
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

    private Table menuRoot;
    private Label hudLine;
    private TextButton btnCancelAuto;
    private TextButton btnCancelLock;
    private TextButton btnAccel;
    private TextButton btnDecel;

    private boolean stickActive;
    private float stickCX = 140f, stickCY = 140f, stickR = 70f;
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
        hudLine = new Label("", game.skin, "small");
        hudLine.setAlignment(Align.left);
        Table top = new Table();
        top.setFillParent(true);
        top.top().left().pad(12);
        top.add(hudLine).left().expandX().fillX();
        stage.addActor(top);

        Table right = new Table();
        right.setFillParent(true);
        right.bottom().right().pad(16);
        btnAccel = new TextButton("加速", game.skin, "go");
        btnDecel = new TextButton("减速", game.skin, "danger");
        hold(btnAccel, true);
        hold(btnDecel, false);
        right.add(btnAccel).width(120).height(72).pad(6).row();
        right.add(btnDecel).width(120).height(72).pad(6);
        stage.addActor(right);

        Table leftBtns = new Table();
        leftBtns.setFillParent(true);
        leftBtns.bottom().left().padLeft(250).padBottom(16);
        TextButton cargo = new TextButton("货物", game.skin);
        TextButton codex = new TextButton("图鉴", game.skin);
        cargo.addListener(click(() -> {
            if (overlay == Overlay.CARGO) {
                overlay = g.dockedPort >= 0 ? Overlay.PORT : Overlay.NONE;
            } else {
                overlay = Overlay.CARGO;
            }
            rebuildMenu();
        }));
        codex.addListener(click(() -> {
            overlay = overlay == Overlay.CODEX ? (g.dockedPort >= 0 ? Overlay.PORT : Overlay.NONE) : Overlay.CODEX;
            rebuildMenu();
        }));
        leftBtns.add(cargo).width(100).height(44).pad(4);
        leftBtns.add(codex).width(100).height(44).pad(4);
        stage.addActor(leftBtns);

        btnCancelAuto = new TextButton("取消自动", game.skin, "danger");
        btnCancelLock = new TextButton("取消锁定", game.skin, "danger");
        btnCancelAuto.addListener(click(() -> {
            g.cancelAutoSail();
            rebuildMenu();
        }));
        btnCancelLock.addListener(click(() -> g.cancelLock()));
        Table mid = new Table();
        mid.setFillParent(true);
        mid.top().padTop(88);
        mid.add(btnCancelAuto).width(140).height(40).pad(4);
        mid.add(btnCancelLock).width(140).height(40).pad(4);
        stage.addActor(mid);

        menuRoot = new Table();
        menuRoot.setFillParent(true);
        menuRoot.right().top().padTop(80).padRight(160);
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

    private void rebuildMenu() {
        menuRoot.clear();
        if (overlay == Overlay.NONE || overlay == Overlay.MAP) {
            return;
        }
        Table box = new Table(game.skin);
        box.defaults().pad(3);
        box.background(game.skin.getDrawable("panel"));
        // tint via pad and width; Window-like
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
        menuRoot.add(sp).width(520).maxHeight(560);
    }

    private void portTable(Table box) {
        IconLib.checkAgainstCatalog();
        int p = g.dockedPort;
        box.add(new Label(Catalog.PORTS[p] + " · 世界暂停", game.skin)).colspan(2).left().row();
        box.add(lbl("银 " + g.silver + "  欠 " + g.debt + "  舱 " + g.cargoUsed() + "/" + g.cargoCap)).colspan(2).left().row();
        box.add(btn("补补给", () -> { g.toast(g.refillSupply()); persist(); rebuildMenu(); })).width(150);
        box.add(btn("还债(全还)", () -> { g.toast(g.repay(g.debt)); persist(); rebuildMenu(); })).width(150).row();
        box.add(btn("修理", () -> { g.toast(g.repair()); persist(); rebuildMenu(); })).width(150);
        box.add(btn("离港", () -> {
            g.leavePort();
            persist();
            overlay = Overlay.NONE;
            rebuildMenu();
        })).width(150).row();
        box.add(btn("升仓库 " + g.warehouseCost(), () -> { g.toast(g.upgradeWarehouse()); persist(); rebuildMenu(); })).width(150);
        box.add(btn("升炮火 " + g.cannonCost(), () -> { g.toast(g.upgradeCannon()); persist(); rebuildMenu(); })).width(150).row();
        box.add(btn("升编制 " + g.crewCapCost(), () -> { g.toast(g.upgradeCrewCap()); persist(); rebuildMenu(); })).width(150);
        box.add(btn("雇人 " + Catalog.HIRE_COST, () -> { g.toast(g.hireCrew()); persist(); rebuildMenu(); })).width(150).row();
        box.add(lbl("船员 " + g.crew + "/" + g.crewCap + "  炮伤 " + g.firepower() + "  耐久 " + (int) g.hull)).colspan(2).left().row();
        tabs(box);
        listItems(box, true);
    }

    private void islandTable(Table box) {
        box.add(new Label(Catalog.ISLANDS[g.islandMenu] + " · 搜采", game.skin)).colspan(2).left().row();
        box.add(btn("搜采", () -> {
            g.toast(g.gatherIsland());
            persist();
            rebuildMenu();
        })).width(160);
        box.add(btn("离开岛屿", () -> {
            g.leaveIsland();
            overlay = Overlay.NONE;
            rebuildMenu();
        })).width(160).row();
        box.add(lbl(g.toastT > 0 ? g.toast : "靠岸菜单搜采。异兽/草药进图鉴，草药只卖钱。")).colspan(2).width(460).left().row();
    }

    private void cargoTable(Table box) {
        box.add(new Label("货舱（三栏共用容量 " + g.cargoUsed() + "/" + g.cargoCap + "）", game.skin)).colspan(2).left().row();
        tabs(box);
        listItems(box, g.dockedPort >= 0);
        if (g.dockedPort < 0) {
            box.add(lbl("海上可丢货，丢了就没了。点货物再点丢掉。")).colspan(2).left().row();
            box.add(btn("丢掉选中 x1", this::dumpSelected)).width(180).row();
        }
        box.add(btn("关闭", () -> {
            overlay = g.dockedPort >= 0 ? Overlay.PORT : Overlay.NONE;
            rebuildMenu();
        })).width(120).row();
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
            overlay = Overlay.PORT;
            rebuildMenu();
            return;
        }
        int gidx = selectedGood;
        TextureRegionDrawable ico = IconLib.good(gidx);
        if (ico != null) {
            Table head = new Table();
            head.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(ico)).size(32, 32).padRight(8);
            head.add(new Label(Catalog.GOODS[gidx] + " · 各港行情（固定）", game.skin));
            box.add(head).colspan(2).left().row();
        } else {
            box.add(new Label(Catalog.GOODS[gidx] + " · 各港行情（固定）", game.skin)).colspan(2).left().row();
        }
        int here = g.dockedPort;
        for (int i = 0; i < Catalog.PORTS.length; i++) {
            String mark = i == here ? " <-本港" : "";
            box.add(lbl(Catalog.PORTS[i] + "  " + Catalog.goodPrice(i, gidx) + " 两" + mark)).colspan(2).left().row();
        }
        box.add(btn("买 1", () -> {
            g.toast(g.buyGood(here, gidx, 1));
            persist();
            rebuildMenu();
        })).width(120);
        box.add(btn("卖 1", () -> {
            g.toast(g.sellGood(here, gidx, 1));
            persist();
            rebuildMenu();
        })).width(120).row();
        box.add(btn("返回列表", () -> {
            overlay = Overlay.PORT;
            rebuildMenu();
        })).colspan(2).width(160).row();
    }

    private void codexTable(Table box) {
        box.add(new Label("图鉴（随时可开）", game.skin)).colspan(2).left().row();
        box.add(new Label("异兽", game.skin, "small")).colspan(2).left().row();
        for (int i = 0; i < Catalog.BEASTS.length; i++) {
            codexRow(box, IconLib.beast(i), g.beastFound[i] ? Catalog.BEASTS[i] : "？？？");
        }
        box.add(new Label("草药（一期只卖钱）", game.skin, "small")).colspan(2).left().row();
        for (int i = 0; i < Catalog.HERBS.length; i++) {
            codexRow(box, IconLib.herb(i), g.herbFound[i] ? Catalog.HERBS[i] : "？？？");
        }
        box.add(btn("关闭", () -> {
            overlay = g.dockedPort >= 0 ? Overlay.PORT : (g.islandMenu >= 0 ? Overlay.ISLAND : Overlay.NONE);
            rebuildMenu();
        })).width(120).row();
    }

    private void failTable(Table box) {
        box.add(new Label("失败：" + g.failReason, game.skin)).colspan(2).left().row();
        box.add(lbl("补给空或船沉都直接失败，读取上次靠港存档。")).colspan(2).width(460).left().row();
        box.add(btn("读档重来", () -> {
            game.state = GameState.fromSave(game.accounts.load(game.currentUser));
            g = game.state;
            overlay = g.dockedPort >= 0 ? Overlay.PORT : Overlay.NONE;
            rebuildMenu();
        })).width(180).row();
    }

    private void codexRow(Table box, TextureRegionDrawable icon, String text) {
        if (icon != null) {
            Table row = new Table();
            row.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(icon)).size(32, 32).padRight(8);
            row.add(lbl(text));
            box.add(row).colspan(2).left().row();
        } else {
            box.add(lbl(text)).colspan(2).left().row();
        }
    }

    private void tabs(Table box) {
        Table t = new Table();
        t.add(btn(cargoTab == 0 ? "[商货]" : "商货", () -> { cargoTab = 0; rebuildMenu(); })).width(90);
        t.add(btn(cargoTab == 1 ? "[异兽]" : "异兽", () -> { cargoTab = 1; rebuildMenu(); })).width(90);
        t.add(btn(cargoTab == 2 ? "[草药]" : "草药", () -> { cargoTab = 2; rebuildMenu(); })).width(90);
        box.add(t).colspan(2).left().row();
    }

    private void listItems(Table box, boolean trading) {
        if (cargoTab == 0) {
            int port = Math.max(0, g.dockedPort);
            for (int i = 0; i < Catalog.GOODS.length; i++) {
                final int idx = i;
                String s = Catalog.GOODS[i] + "  持有" + g.trade[i];
                if (g.dockedPort >= 0) {
                    s += "  本港" + Catalog.goodPrice(port, i);
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
                String s = Catalog.BEASTS[i] + "  x" + g.beasts[i] + "  卖价" + Catalog.BEAST_PRICE[i];
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
                String s = Catalog.HERBS[i] + "  x" + g.herbs[i] + "  卖价" + Catalog.HERB_PRICE[i] + "（只卖）";
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

    private Drawable iconOrNull(TextureRegionDrawable d) {
        return d;
    }

    /** Adds an icon+text row; icon cell omitted when the drawable is null. */
    private void iconRow(Table box, TextureRegionDrawable icon, String text, Runnable onClick) {
        TextButton b = btn(text, onClick);
        if (icon != null) {
            Table row = new Table();
            row.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(icon)).size(28, 28).padRight(6);
            row.add(b).width(400).height(40);
            box.add(row).colspan(2).left().row();
        } else {
            box.add(b).colspan(2).width(460).left().row();
        }
    }

    private Label lbl(String s) {
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
        } else if (overlay == Overlay.PORT || overlay == Overlay.ISLAND) {
            // paused
        } else if (!g.worldPaused()) {
            g.update(delta);
        }

        if (g.failed && overlay != Overlay.FAIL) {
            overlay = Overlay.FAIL;
            rebuildMenu();
        }
        if (g.dockedPort >= 0 && overlay == Overlay.NONE) {
            overlay = Overlay.PORT;
            persist();
            rebuildMenu();
        }
        if (g.islandMenu >= 0 && overlay == Overlay.NONE) {
            overlay = Overlay.ISLAND;
            rebuildMenu();
        }

        btnCancelAuto.setVisible(g.autoSail);
        btnCancelLock.setVisible(g.combatLock);
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
        drawMinimapAndStick();
        if (overlay == Overlay.MAP) {
            drawFullMap();
        }

        stage.act(delta);
        stage.draw();

        game.batch.begin();
        if (g.toastT > 0) {
            game.font.draw(game.batch, g.toast, 24, 92);
        }
        game.batch.end();
    }

    private String statusText() {
        return "银" + g.silver + " 欠" + g.debt
                + " 补给" + (int) g.supply + "/" + (int) g.supplyMax
                + " 耐久" + (int) g.hull
                + " 船员" + g.crew + "/" + g.crewCap
                + " 舱" + g.cargoUsed() + "/" + g.cargoCap
                + "  " + g.windLabel() + g.weatherLabel()
                + " 速" + (int) g.speed
                + (g.autoSail ? " 自动航行" : "")
                + (g.pirateAlive ? " 海盗" : "");
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
        if (w) g.holdAccel = true;
        if (s) g.holdDecel = true;
        if (!w && btnAccel != null && !btnAccel.isPressed()) {
            if (!Gdx.input.isKeyPressed(Input.Keys.W) && !Gdx.input.isKeyPressed(Input.Keys.UP)) {
                // keep holdAccel if button pressed — handled in touch; if neither key nor we need to clear
            }
        }
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

    private void drawMinimapAndStick() {
        float mx = 24, my = HUD_H - 24 - 170, mw = 230, mh = 170;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(PANEL);
        shapes.rect(mx, my, mw, mh);
        shapes.setColor(0.07f, 0.2f, 0.28f, 0.95f);
        shapes.rect(mx + 6, my + 6, mw - 12, mh - 12);
        drawMapContents(mx + 6, my + 6, mw - 12, mh - 12, false);

        // stick base
        shapes.setColor(0f, 0f, 0f, 0.35f);
        shapes.circle(stickCX, stickCY, stickR);
        shapes.setColor(0.85f, 0.85f, 0.85f, 0.5f);
        float knx = stickCX + stickKX * (stickR - 22);
        float kny = stickCY + stickKY * (stickR - 22);
        if (!stickActive) {
            knx = stickCX;
            kny = stickCY;
        }
        shapes.circle(knx, kny, 22f);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.8f, 0.8f, 0.8f, 0.8f);
        shapes.rect(mx, my, mw, mh);
        shapes.circle(stickCX, stickCY, stickR);
        shapes.end();

        game.batch.begin();
        game.fontSmall.draw(game.batch, "小地图(点开全图)", mx + 8, my + mh - 4);
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
        shapes.end();
        game.batch.begin();
        game.font.draw(game.batch, "全图（点港口自动驶向，点空白关闭）", x + 20, y + h - 8);
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

    private class WorldInput extends InputAdapter {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            hudVp.unproject(tmp.set(screenX, screenY, 0));
            float hx = tmp.x, hy = tmp.y;
            float dx = hx - stickCX, dy = hy - stickCY;
            if (dx * dx + dy * dy <= (stickR + 24) * (stickR + 24) && overlay != Overlay.PORT && overlay != Overlay.FAIL) {
                stickActive = true;
                stickPointer = pointer;
                setStick(hx, hy);
                g.onManualSteer();
                return true;
            }
            float mx = 24, my = HUD_H - 24 - 170, mw = 230, mh = 170;
            if (hx >= mx && hx <= mx + mw && hy >= my && hy <= my + mh) {
                overlay = overlay == Overlay.MAP ? Overlay.NONE : Overlay.MAP;
                rebuildMenu();
                return true;
            }
            if (overlay == Overlay.MAP) {
                return handleFullMapTap(hx, hy);
            }
            if (g.pirateAlive && overlay != Overlay.PORT) {
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
        if (hx < x || hx > x + w || hy < y || hy > y + h) {
            overlay = Overlay.NONE;
            rebuildMenu();
            return true;
        }
        float ix = x + 10, iy = y + 10, iw = w - 20, ih = h - 20;
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
        overlay = Overlay.NONE;
        rebuildMenu();
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
