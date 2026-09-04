package com.shipgame.nanhai.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.shipgame.nanhai.NanHaiVoyage;
import com.shipgame.nanhai.data.GameState;
import com.shipgame.nanhai.data.SaveData;

/**
 * 0.27.0 Tang-maritime login: cinematic dusk-harbour background painted into a
 * single texture (no shipped art), 南海航程 gold logo with the 大唐海贸 seal,
 * 扬帆南海 · 通商万国 subtitle, gold-trimmed pill fields and the ornate
 * cinnabar 登录 / navy 注册 buttons. 客服/公告/活动/福利/占有度 are out of
 * scope — the login surface stays exactly two fields + two buttons, so the
 * desktop smoke test keeps driving it through the same input pipeline.
 */
public class LoginScreen extends ScreenAdapter {

    private static final int BG_W = 1280;
    private static final int BG_H = 720;

    private final NanHaiVoyage game;
    private Stage stage;
    private Texture bgTex;
    private Label msg;
    private boolean switching;

    public LoginScreen(NanHaiVoyage game) {
        this.game = game;
    }

    @Override
    public void show() {
        buildUi();
        // Fire-and-forget GitHub Releases check (Android only; null elsewhere).
        // Must never block or throw — implementation is failure-silent.
        if (game.updateChecker != null) {
            try {
                game.updateChecker.checkForUpdate();
            } catch (Throwable ignored) {
            }
        }
    }

    /** Builds (or rebuilds) the whole login UI. Safe to call again after a
     * failed transition so the user is never stuck on a dead screen. */
    private void buildUi() {
        stage = new Stage(new FitViewport(1280, 720), game.batch);
        Gdx.input.setInputProcessor(stage);

        disposeBg();
        bgTex = buildBackground();

        // Cinematic backdrop first (lowest z); touches pass straight through it.
        Image bg = new Image(bgTex);
        bg.setTouchable(Touchable.disabled);
        stage.addActor(bg);

        Table root = new Table();
        root.setFillParent(true);
        root.top();
        stage.addActor(root);

        // --- Header: 南海航程 + 大唐海贸 vertical seal + 扬帆南海 · 通商万国 ---
        Label title = new Label("南海航程", game.skin, "gold");
        title.setFontScale(2.4f);

        Table seal = new Table();
        seal.setBackground(((NinePatchDrawable) game.skin.getDrawable("pillGold"))
                .tint(new Color(0.62f, 0.20f, 0.16f, 0.98f)));
        Label sealTxt = new Label("大唐海贸", game.skin, "goldSmall");
        sealTxt.setFontScale(1.4f);
        seal.add(sealTxt).pad(10, 14, 10, 14);
        seal.setRotation(-6f);

        Table titleRow = new Table();
        titleRow.add(title).padRight(18);
        titleRow.add(seal);

        Image lline = new Image(game.skin.getDrawable("white"));
        lline.setColor(0.90f, 0.74f, 0.38f, 0.85f);
        Image rline = new Image(game.skin.getDrawable("white"));
        rline.setColor(0.90f, 0.74f, 0.38f, 0.85f);
        Label sub = new Label("扬帆南海 · 通商万国", game.skin, "goldSmall");
        sub.setFontScale(1.3f);
        Table subRow = new Table();
        subRow.add(lline).size(84, 3).pad(10);
        subRow.add(sub).padLeft(14).padRight(14);
        subRow.add(rline).size(84, 3).pad(10);

        // --- Form: gold-trimmed navy panel with the two pill fields. ---
        Table box = new Table();
        box.setBackground(game.skin.getDrawable("panelGold"));
        box.pad(26, 44, 26, 44);

        final TextField user = new TextField("summer", game.skin, "goldField");
        user.setMessageText("请输入用户名");
        final TextField pass = new TextField("summer", game.skin, "goldField");
        pass.setMessageText("请输入密码");
        // 密码掩码必须用 nanhai-cjk.ttf 里真实存在的字形。默认掩码是 U+2022 '•'
        // （libGDX TextField 默认值），该字形在 CJK 字体里缺失，会画成乱码方块；
        // 且 libGDX 的 passwordBuffer 只在首次填充时写入掩码字符，之后再调
        // setPasswordCharacter 不会改写已填充的缓存（长度不变时）。
        // 所以顺序必须是：先设掩码字符（U+FF0A 全角＊，字体已含），再开密码模式，
        // 这样 displayText 首次生成时用的就是 ＊。
        pass.setPasswordCharacter('＊');
        pass.setPasswordMode(true);

        Label ul = new Label("用户名", game.skin, "goldSmall");
        Label pl = new Label("密码", game.skin, "goldSmall");
        Table row1 = new Table();
        row1.add(ul).width(104).left();
        row1.add(user).width(360).height(46);
        Table row2 = new Table();
        row2.add(pl).width(104).left();
        row2.add(pass).width(360).height(46);

        msg = new Label("注册一个本机账号，或登录已有账号。", game.skin, "goldSmall");
        msg.setWrap(true);

        TextButton login = new TextButton("登录", game.skin, "cinnabar");
        TextButton reg = new TextButton("注册", game.skin, "navy");

        login.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                doLogin(user.getText(), pass.getText());
            }
        });
        reg.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                doRegister(user.getText(), pass.getText());
            }
        });

        Image dot = new Image(game.skin.getDrawable("dotGold"));
        Table btnRow = new Table();
        btnRow.add(login).width(190).height(56).pad(6);
        btnRow.add(dot).size(10, 10).pad(10);
        btnRow.add(reg).width(190).height(56).pad(6);

        box.add(row1).padBottom(14).row();
        box.add(row2).padBottom(20).row();
        box.add(btnRow).row();
        box.add(msg).padTop(14);

        // --- Footer: agreement checkbox + highlighted doc links. ---
        CheckBox agree = new CheckBox("我已详细阅读并同意", game.skin, "tang");
        Label t2 = new Label("《用户协议》和《隐私政策》", game.skin, "goldSmall");
        Table terms = new Table();
        terms.add(agree).padRight(6);
        terms.add(t2);

        root.add(titleRow).padTop(56).row();
        root.add(subRow).padTop(18).row();
        root.add(box).padTop(44).row();
        root.add(terms).padTop(30);
    }

    // ------------------------------------------------------- cinematic scene

    /** Paints the 0.27.0 dusk-harbour backdrop: gradient dusk sky, sun glow,
     * clouds, mountain silhouette, sea with sun reflection, two sailing junks
     * and the dark wooden pier/table in the foreground. All procedural — the
     * APK ships zero art bytes for the login screen. */
    private Texture buildBackground() {
        Pixmap pm = new Pixmap(BG_W, BG_H, Pixmap.Format.RGBA8888);

        // Dusk sky: deep navy -> indigo -> warm apricot at the horizon.
        vgrad(pm, 0, 170, c(0.026f, 0.042f, 0.090f), c(0.085f, 0.125f, 0.235f));
        vgrad(pm, 170, 360, c(0.085f, 0.125f, 0.235f), c(0.27f, 0.24f, 0.38f));
        vgrad(pm, 360, 470, c(0.27f, 0.24f, 0.38f), c(0.60f, 0.42f, 0.28f));

        radial(pm, 1008, 462, 80, 0.40f, c(1f, 0.79f, 0.45f));
        radial(pm, 1008, 462, 26, 0.85f, c(1f, 0.91f, 0.66f));
        radial(pm, 1008, 462, 10, 0.95f, c(1f, 0.96f, 0.80f));

        cloud(pm, 250, 120, 48, 0.10f);
        cloud(pm, 700, 60, 34, 0.09f);
        cloud(pm, 660, 215, 62, 0.11f);
        cloud(pm, 920, 100, 40, 0.10f);
        cloud(pm, 1120, 250, 52, 0.09f);
        cloud(pm, 70, 300, 40, 0.08f);

        // Mountain ridge, then the sea.
        mountains(pm, 470, c(0.075f, 0.105f, 0.185f));
        vgrad(pm, 470, 618, c(0.055f, 0.15f, 0.27f), c(0.025f, 0.07f, 0.145f));
        sunReflection(pm, 1008, 474, 600);
        waves(pm, 480, 612);

        junk(pm, 330, 596, 1.05f);
        junk(pm, 962, 608, 1.5f);

        // Foreground wooden pier / table surface.
        vgrad(pm, 618, 720, c(0.19f, 0.105f, 0.05f), c(0.055f, 0.03f, 0.015f));
        planks(pm, 618, 720);
        compass(pm, 122, 652, 30);
        coins(pm);
        scroll(pm, 480, 634);

        Texture t = new Texture(pm);
        pm.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    private static Color c(float r, float g, float b) {
        return new Color(r, g, b, 1f);
    }

    /** Vertical gradient between two colors over rows y0..y1 (pixmap y-down). */
    private static void vgrad(Pixmap pm, int y0, int y1, Color c0, Color c1) {
        for (int y = y0; y < y1; y++) {
            float t = (y - y0) / (float) (y1 - y0);
            pm.setColor(c0.r + (c1.r - c0.r) * t,
                    c0.g + (c1.g - c0.g) * t,
                    c0.b + (c1.b - c0.b) * t, 1f);
            pm.drawLine(0, y, BG_W - 1, y);
        }
    }

    /** Soft radial glow: concentric circles, brightest in the core, fading out
     * (repeated SourceOver layers saturate the centre into a warm glow). */
    private static void radial(Pixmap pm, int cx, int cy, int maxR, float a, Color col) {
        pm.setColor(col.r, col.g, col.b, 0f);
        for (int r = maxR; r >= 1; r--) {
            float t = r / (float) maxR;
            pm.setColor(col.r, col.g, col.b, Math.min(1f, a * (1f - t)));
            pm.fillCircle(cx, cy, r);
        }
    }

    /** Soft cloud: a few overlapping blurred circles. */
    private static void cloud(Pixmap pm, int cx, int cy, int r, float a) {
        pm.setColor(0.95f, 0.90f, 0.78f, a);
        pm.fillCircle(cx, cy, r);
        pm.fillCircle(cx - (int) (r * 0.8f), cy + r / 3, (int) (r * 0.7f));
        pm.fillCircle(cx + (int) (r * 0.9f), cy + r / 4, (int) (r * 0.65f));
        pm.fillCircle(cx - (int) (r * 0.4f), cy - (int) (r * 0.5f), (int) (r * 0.55f));
        pm.fillCircle(cx + (int) (r * 0.5f), cy - (int) (r * 0.4f), (int) (r * 0.5f));
    }

    /** Dark jagged mountain band: fills each column from its ridge to baseY. */
    private static void mountains(Pixmap pm, int baseY, Color col) {
        pm.setColor(col);
        for (int x = 0; x < BG_W; x++) {
            float ridge = 26f + 15f * (float) Math.sin(x * 0.0042)
                    + 10f * (float) Math.sin(x * 0.012 + 1.3)
                    + 7f * (float) Math.sin(x * 0.021 + 4.2);
            int top = Math.max(0, baseY - (int) ridge);
            pm.drawLine(x, top, x, baseY);
        }
    }

    /** Vertical sun glints on the sea under the glow. */
    private static void sunReflection(Pixmap pm, int cx, int y0, int y1) {
        for (int y = y0; y < y1; y += 3) {
            float t = (y - y0) / (float) (y1 - y0);
            float a = 0.30f * (1f - t);
            float w = 6f + 26f * t;
            pm.setColor(0.98f, 0.82f, 0.45f, a);
            pm.drawLine(Math.round(cx - w), y, Math.round(cx + w), y);
        }
    }

    /** Sparse light wave dashes (deterministic, no random state). */
    private static void waves(Pixmap pm, int y0, int y1) {
        for (int y = y0; y < y1; y += 8) {
            int seed = y * 7;
            pm.setColor(0.82f, 0.87f, 0.93f, 0.10f);
            for (int x = (y * 13) % 61; x < BG_W; x += 71 + (y * 3) % 11) {
                int len = 2 + (y + x) % 4;
                pm.drawLine(x, y, Math.min(BG_W - 1, x + len), y);
            }
        }
    }

    /** Tang sailing junk: dark hull, slanted pale sails, gold pennant. */
    private static void junk(Pixmap pm, int cx, int baseY, float s) {
        Color hull = c(0.06f, 0.055f, 0.08f);
        Color sail = c(0.42f, 0.34f, 0.22f);
        int mastTop = baseY - Math.round(92f * s);

        // Mast.
        pm.setColor(hull);
        pm.drawLine(cx, baseY - Math.round(12f * s), cx, mastTop);

        // Three slanted sails (triangles) hung on the mast.
        tri(pm, cx, baseY - Math.round(58f * s), cx, baseY - Math.round(10f * s),
                cx + Math.round(46f * s), baseY - Math.round(58f * s), sail);
        tri(pm, cx, baseY - Math.round(46f * s), cx, baseY - Math.round(6f * s),
                cx - Math.round(34f * s), baseY - Math.round(40f * s), sail);
        tri(pm, cx, baseY - Math.round(88f * s), cx, baseY - Math.round(54f * s),
                cx + Math.round(30f * s), baseY - Math.round(84f * s), sail);

        // Gold pennant on the mast tip.
        pm.setColor(0.90f, 0.74f, 0.36f, 1f);
        tri(pm, cx, mastTop, cx, mastTop - Math.round(10f * s),
                cx + Math.round(20f * s), mastTop - Math.round(5f * s), new Color(0.90f, 0.74f, 0.36f, 1f));

        // Hull: rounded trapezoid.
        pm.setColor(hull);
        int h = Math.round(13f * s);
        int wTop = Math.round(16f * s), wBot = Math.round(34f * s);
        for (int y = 0; y <= h; y++) {
            float t = y / (float) h;
            int w = Math.round(wTop + (wBot - wTop) * t);
            pm.drawLine(cx - w, baseY - h + y, cx + w, baseY - h + y);
        }
        // Deck highlight.
        pm.setColor(0.55f, 0.42f, 0.26f, 1f);
        pm.drawLine(cx - wTop, baseY - h, cx + wTop, baseY - h);
    }

    /** Filled triangle via per-row x-range scan (y-down pixmap). */
    private static void tri(Pixmap pm, int x0, int y0, int x1, int y1, int x2, int y2, Color col) {
        pm.setColor(col);
        int minY = Math.min(y0, Math.min(y1, y2));
        int maxY = Math.max(y0, Math.max(y1, y2));
        for (int y = minY; y <= maxY; y++) {
            int xl = Integer.MAX_VALUE, xr = Integer.MIN_VALUE;
            int[] xs = {x0, x1, x2};
            int[] ys = {y0, y1, y2};
            for (int i = 0; i < 3; i++) {
                int j = (i + 1) % 3;
                int ya = ys[i], yb = ys[j], xa = xs[i], xb = xs[j];
                if ((y >= ya && y < yb) || (y >= yb && y < ya)) {
                    int xi = xa + (xb - xa) * (y - ya) / (yb - ya);
                    xl = Math.min(xl, xi);
                    xr = Math.max(xr, xi);
                }
            }
            if (xr >= xl) {
                pm.drawLine(Math.max(0, xl), y, Math.min(BG_W - 1, xr), y);
            }
        }
    }

    /** Horizontal plank seams + staggered vertical board notches. */
    private static void planks(Pixmap pm, int y0, int y1) {
        pm.setColor(0.02f, 0.01f, 0.005f, 0.85f);
        for (int y = y0 + 8; y < y1; y += 16) {
            pm.drawLine(0, y, BG_W - 1, y);
            for (int x = (y * 5) % 96; x < BG_W; x += 96) {
                pm.drawLine(x, y, x, Math.min(y1 - 1, y + 5));
            }
        }
    }

    /** Brass compass on the pier (echoes the mockup's position). */
    private static void compass(Pixmap pm, int cx, int cy, int r) {
        pm.setColor(0.66f, 0.50f, 0.24f, 1f);
        pm.fillCircle(cx, cy, r);
        pm.setColor(0.13f, 0.08f, 0.04f, 1f);
        pm.fillCircle(cx, cy, r - 4);
        pm.setColor(0.85f, 0.70f, 0.38f, 1f);
        pm.drawLine(cx - r + 6, cy, cx + r - 6, cy);
        pm.drawLine(cx, cy - r + 6, cx, cy + r - 6);
        pm.drawLine(cx - 8, cy - 8, cx + 8, cy + 8);
        pm.drawLine(cx + 8, cy - 8, cx - 8, cy + 8);
    }

    /** A few scattered gold coins. */
    private static void coins(Pixmap pm) {
        pm.setColor(0.92f, 0.78f, 0.36f, 0.95f);
        pm.fillCircle(300, 700, 5);
        pm.fillCircle(330, 709, 4);
        pm.fillCircle(262, 704, 4);
        pm.fillCircle(704, 716, 5);
        pm.fillCircle(736, 706, 4);
        pm.fillCircle(960, 698, 4);
    }

    /** Rolled map scroll on the pier. */
    private static void scroll(Pixmap pm, int x, int y) {
        pm.setColor(0.72f, 0.64f, 0.48f, 0.92f);
        pm.fillRectangle(x, y, 210, 22);
        pm.setColor(0.85f, 0.78f, 0.62f, 1f);
        pm.fillRectangle(x + 4, y + 3, 202, 16);
        pm.setColor(0.50f, 0.42f, 0.30f, 1f);
        pm.fillRectangle(x, y, 7, 22);
        pm.fillRectangle(x + 203, y, 7, 22);
    }

    private void disposeBg() {
        if (bgTex != null) {
            bgTex.dispose();
            bgTex = null;
        }
    }

    private void doRegister(String u, String p) {
        if (u == null || p == null || u.trim().isEmpty() || p.isEmpty()) {
            msg.setText("请输入用户名和密码。");
            return;
        }
        try {
            if (game.accounts.userExists(u)) {
                msg.setText("账号已存在，请登录。");
                return;
            }
            if (!game.accounts.register(u, p)) {
                msg.setText("注册失败。");
                return;
            }
            game.currentUser = u.trim();
            game.state = GameState.newGame();
            game.accounts.save(game.currentUser, game.state.toSave());
            enterVoyage();
        } catch (Throwable t) { // Errors too: nothing on this path may kill the process
            Gdx.app.error("LoginScreen", "register failed", t);
            msg.setText("注册错误。");
        }
    }

    private void doLogin(String u, String p) {
        Gdx.app.error("LoginScreen", "login click: user='" + (u == null ? "<null>" : u) + "'");
        if (u == null || p == null) {
            msg.setText("用户名或密码不对，或账号不存在。");
            return;
        }
        try {
            if (!game.accounts.login(u, p)) {
                msg.setText("用户名或密码不对，或账号不存在。");
                return;
            }
            game.currentUser = u.trim();
            SaveData s = game.accounts.load(game.currentUser);
            game.state = s == null ? GameState.newGame() : GameState.fromSave(s);
            if (s == null) {
                game.accounts.save(game.currentUser, game.state.toSave());
            }
            Gdx.app.error("LoginScreen", "login ok for '" + game.currentUser
                    + "', state dockedPort=" + game.state.dockedPort
                    + ", lastPort=" + game.state.lastPort);
            enterVoyage();
        } catch (Throwable t) { // Errors too: corrupt data must not kill the app
            Gdx.app.error("LoginScreen", "login failed", t);
            msg.setText("登录错误。");
        }
    }

    /**
     * Defers the screen switch to the next frame via postRunnable. Running
     * setScreen synchronously inside the click handler disposes this stage
     * mid-dispatch and races the Android surface lifecycle (resize NPE).
     */
    private void enterVoyage() {
        if (switching) {
            return; // ignore double-taps: only one transition may run
        }
        switching = true;
        Gdx.input.setInputProcessor(null); // stop new input before the swap
        Gdx.app.error("LoginScreen", "posting setScreen(VoyageScreen) for next frame");
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                Gdx.app.error("LoginScreen", "postRunnable fired, calling setScreen");
                try {
                    game.setScreen(new VoyageScreen(game));
                    Gdx.app.error("LoginScreen", "setScreen returned OK, current=" + game.getScreen().getClass().getSimpleName());
                } catch (Throwable t) {
                    Gdx.app.error("LoginScreen", "enter voyage failed", t);
                    // Rebuild the login UI so the app stays usable.
                    try {
                        buildUi();
                        msg.setText("进入航海失败，请重试（" + t.getClass().getSimpleName() + "）。");
                    } catch (Throwable ignored) {
                    }
                    switching = false;
                }
            }
        });
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.05f, 0.09f, 0.16f, 1f);
        if (stage == null) {
            return;
        }
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        // hide() sets stage = null; Android can fire resize at any point
        // during the transition (IME hide, immersive-mode focus change).
        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
        if (stage != null) {
            stage.dispose();
            stage = null;
        }
        disposeBg();
    }
}