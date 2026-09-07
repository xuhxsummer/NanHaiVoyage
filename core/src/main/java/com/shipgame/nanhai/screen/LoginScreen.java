package com.shipgame.nanhai.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.shipgame.nanhai.NanHaiVoyage;
import com.shipgame.nanhai.data.GameState;
import com.shipgame.nanhai.data.SaveData;
import com.shipgame.nanhai.ui.UiFactory;

/** 1920 × 1080 login, eight-pixel layout grid; artwork and live controls are separate. */
public class LoginScreen extends ScreenAdapter {
    private final NanHaiVoyage game;
    private Stage stage;
    private Texture bgTex;
    private Image bg;              // 0.27.4: covers the full extended viewport
    private Skin loginSkin;
    private Label msg;
    private boolean switching;

    public LoginScreen(NanHaiVoyage game) { this.game = game; }

    @Override public void show() {
        switching = false;
        buildUi();
        if (game.updateChecker != null) {
            try { game.updateChecker.checkForUpdate(); } catch (Throwable ignored) { }
        }
    }

    private BitmapFont font(int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/nanhai-cjk.ttf"));
        try {
            FreeTypeFontGenerator.FreeTypeFontParameter params = new FreeTypeFontGenerator.FreeTypeFontParameter();
            params.size = size;
            params.characters = FreeTypeFontGenerator.DEFAULT_CHARS
                    + Gdx.files.internal("fonts/ui-chars.txt").readString("UTF-8")
                    + "本机登录 · 本机存档（关游戏不丢）公告客服设置暂未开放＊";
            return generator.generateFont(params);
        } finally { generator.dispose(); }
    }

    /** Small reusable nine-patches: stepped corners, untinted brass edges and inset shadow. */
    private Drawable frame(String name, String fill) {
        Pixmap p = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        p.setColor(Color.valueOf("A88A51")); p.fillRectangle(4, 0, 24, 32); p.fillRectangle(0, 4, 32, 24);
        p.setColor(Color.valueOf("263744")); p.fillRectangle(4, 2, 24, 28); p.fillRectangle(2, 4, 28, 24);
        p.setColor(Color.valueOf(fill)); p.fillRectangle(4, 4, 24, 24);
        p.setColor(Color.valueOf("D8B974")); p.drawLine(8, 4, 23, 4);
        p.setColor(0, 0, 0, .3f); p.fillRectangle(8, 26, 16, 2);
        Texture texture = new Texture(p); p.dispose();
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        loginSkin.add(name, texture);
        NinePatch patch = new NinePatch(texture, 8, 8, 8, 8);
        patch.setPadding(24, 24, 8, 8);
        return new NinePatchDrawable(patch);
    }

    private TextButton button(String text, Drawable up, Drawable down) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(loginSkin.get(TextButton.TextButtonStyle.class));
        style.up = up; style.down = down; style.over = down;
        style.fontColor = Color.valueOf("F4E3B8");
        return new TextButton(text, style);
    }

    private void place(Actor actor, int x, int y, int w, int h) {
        actor.setBounds(x, y, w, h); stage.addActor(actor);
    }

    private Label label(String text, int size) {
        Label label = new Label(text, loginSkin, "gold");
        label.setFontScale(size / 32f); label.setAlignment(Align.center);
        return label;
    }

    private void buildUi() {
        releaseUi();
        // 0.27.4: ExtendViewport fills any aspect ratio edge-to-edge (no black
        // or solid side bars); the artwork below is sized to cover the whole world.
        stage = new Stage(new ExtendViewport(1920, 1080), game.batch);
        stage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        Gdx.input.setInputProcessor(stage);
        loginSkin = UiFactory.create(font(32), font(24));
        bgTex = new Texture(Gdx.files.internal("textures/login/harbor-hd.png"));
        bgTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        bg = new Image(bgTex); bg.setTouchable(Touchable.disabled);
        place(bg, 0, 0, (int) stage.getViewport().getWorldWidth(),
                (int) stage.getViewport().getWorldHeight());

        Drawable navy = frame("loginNavy", "102735F5");
        Drawable hover = frame("loginHover", "244655");
        Drawable red = frame("loginRed", "842D24");
        Drawable redDown = frame("loginRedDown", "A6402D");
        place(label("扬帆南海 · 通商万国", 32), 576, 672, 768, 56);
        Label local = label("本机登录 · 本机存档（关游戏不丢）", 24);
        local.setColor(Color.valueOf("D3C5A7"));
        place(local, 544, 600, 832, 40);

        TextField.TextFieldStyle fieldStyle = new TextField.TextFieldStyle(loginSkin.get("goldField", TextField.TextFieldStyle.class));
        fieldStyle.background = navy;
        fieldStyle.focusedBackground = hover;
        fieldStyle.messageFont = loginSkin.getFont("font");
        fieldStyle.messageFontColor = Color.valueOf("A5B0AF");
        final TextField user = new TextField("summer", fieldStyle);
        user.setName("loginUsername"); user.setMessageText("请输入用户名");
        final TextField pass = new TextField("summer", fieldStyle);
        pass.setName("loginPassword"); pass.setMessageText("请输入密码");
        pass.setPasswordCharacter('＊'); pass.setPasswordMode(true);
        for (int i = 0; i < 2; i++) {
            int y = 496 - i * 112;
            Table caption = new Table(); caption.setBackground(navy);
            caption.add(label(i == 0 ? "用户名" : "密码", 32)).expand().fill();
            place(caption, 544, y, 192, 80);
        }
        place(user, 752, 496, 624, 80);
        place(pass, 752, 384, 624, 80);
        TextButton login = button("登录", red, redDown);
        TextButton register = button("注册", navy, hover);
        login.getLabel().setFontScale(1.5f); register.getLabel().setFontScale(1.5f);
        place(login, 552, 240, 352, 96);
        place(register, 1016, 240, 352, 96);
        login.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { doLogin(user.getText(), pass.getText()); }
        });
        register.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { doRegister(user.getText(), pass.getText()); }
        });
        msg = label("注册一个本机账号，或登录已有账号。", 24);
        msg.setWrap(true);
        place(msg, 520, 144, 880, 72);
        String[] helpers = {"公告", "客服", "设置"};
        for (int i = 0; i < helpers.length; i++) {
            final String text = helpers[i];
            TextButton helper = button(text, navy, hover);
            helper.getLabel().setFontScale(.75f);
            place(helper, 1512 + i * 120, 960, 104, 64);
            helper.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) { msg.setText(text + "暂未开放。"); }
            });
        }
    }

    private void releaseUi() {
        if (stage != null && Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
        if (stage != null) { stage.dispose(); stage = null; }
        bg = null;
        if (bgTex != null) { bgTex.dispose(); bgTex = null; }
        if (loginSkin != null) { loginSkin.dispose(); loginSkin = null; }
    }

    private void doRegister(String u, String p) {
        if (switching || stage == null) { return; }
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
        if (switching || stage == null) { return; }
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
        final Stage pendingStage = stage;
        Gdx.input.setInputProcessor(null); // stop new input before the swap
        Gdx.app.error("LoginScreen", "posting setScreen(VoyageScreen) for next frame");
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                // A hidden or rebuilt login must not perform an obsolete transition.
                if (stage != pendingStage || game.getScreen() != LoginScreen.this) {
                    return;
                }
                Gdx.app.error("LoginScreen", "postRunnable fired, calling setScreen");
                try {
                    game.setScreen(new VoyageScreen(game));
                    Gdx.app.error("LoginScreen", "setScreen returned OK, current=" + game.getScreen().getClass().getSimpleName());
                } catch (Throwable t) {
                    Gdx.app.error("LoginScreen", "enter voyage failed", t);
                    // setScreen assigns the new screen before calling show(). Restore
                    // the actual active screen as well as its input and UI on failure.
                    try {
                        game.setScreen(LoginScreen.this);
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
            if (bg != null) {
                bg.setSize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
            }
        }
    }

    @Override public void hide() {
        releaseUi();
    }

    @Override public void dispose() { releaseUi(); }
}
