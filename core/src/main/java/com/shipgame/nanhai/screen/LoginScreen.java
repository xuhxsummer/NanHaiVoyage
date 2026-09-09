package com.shipgame.nanhai.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
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
import com.shipgame.nanhai.ui.LoginHarbor;
import com.shipgame.nanhai.ui.UpdateChecker;

/** 1920 × 1080 login, eight-pixel layout grid; artwork and live controls are separate. */
public class LoginScreen extends ScreenAdapter {
    private final NanHaiVoyage game;
    private Stage stage;
    private LoginHarbor bg;
    private Skin loginSkin;
    private Label msg;
    private boolean switching;
    private Table updatePanel;
    private ProgressBar updateProgress;
    private Label updateStatus;
    private Dialog modal;
    private Runnable modalDismiss;
    private Preferences settings;
    private Drawable modalFrame, modalRed, modalBlue, modalHover, progressTrack, progressFill;


    public LoginScreen(NanHaiVoyage game) { this.game = game; }

    @Override public void show() {
        switching = false;
        buildUi();
        if (game.updateChecker != null) {
            final Stage listeningStage = stage;
            game.updateChecker.setListener(new UpdateChecker.Listener() {
                @Override public void onUpdateAvailable(String version, final Runnable accept, final Runnable decline) {
                    if (stage != listeningStage) return;
                    showUpdatePrompt(version, accept, decline);
                }
                @Override public void onDownloadProgress(int percent) {
                    if (stage != listeningStage || updateProgress == null) return;
                    int value = Math.max(0, Math.min(100, percent));
                    updateProgress.setValue(value);
                    updateStatus.setText("正在下载… " + value + "%");
                }
                @Override public void onDownloadFinished(boolean success, String message) {
                    if (stage != listeningStage || updateProgress == null) return;
                    closeModal();
                    if (msg != null) msg.setText(success ? "下载完成，请按系统提示安装。"
                            : (message == null ? "下载失败，请稍后重试。" : message));
                }
            });
            try { game.updateChecker.checkForUpdate(); } catch (Throwable ignored) { }
        }
    }

    private Table openModal(String title, Runnable dismiss) {
        closeModal();
        Window.WindowStyle style = new Window.WindowStyle(loginSkin.get(Window.WindowStyle.class));
        style.background = modalFrame;
        style.stageBackground = loginSkin.newDrawable("white", new Color(0.01f, 0.02f, 0.04f, .72f));
        modal = new Dialog("", style);
        modal.setName("loginModal");
        modal.setMovable(false);
        modal.pad(36);
        modalDismiss = dismiss;
        modal.addListener(new InputListener() {
            @Override public boolean keyDown(InputEvent event, int keycode) {
                if (keycode != Input.Keys.ESCAPE && keycode != Input.Keys.BACK) return false;
                Runnable action = modalDismiss;
                if (action != null) action.run();
                return true;
            }
        });
        Table content = modal.getContentTable();
        content.defaults().spaceBottom(22);
        Label heading = label(title, 46);
        heading.setColor(Color.valueOf("FFF0BF"));
        content.add(heading).width(760).height(64).row();
        return content;
    }

    private void presentModal() {
        modal.show(stage, null);
        centerModal();
    }

    private void centerModal() {
        if (modal != null) modal.setPosition((stage.getWidth() - modal.getWidth()) / 2,
                (stage.getHeight() - modal.getHeight()) / 2);
    }

    private void closeModal() {
        if (modal != null) { modal.hide(null); modal = null; }
        modalDismiss = null;
        updatePanel = null; updateProgress = null; updateStatus = null;
    }

    private Label modalCopy(String text) {
        Label copy = label(text, 28);
        copy.setAlignment(Align.left);
        copy.setColor(Color.valueOf("F4E6CB"));
        copy.setWrap(true);
        return copy;
    }

    private TextButton modalButton(String text, boolean primary, Runnable action) {
        TextButton button = button(text, primary ? modalRed : modalBlue, modalHover);
        button.setName(text);
        button.getLabel().setFontScale(1.15f);
        button.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { action.run(); }
        });
        return button;
    }

    private void showUpdatePrompt(String version, final Runnable accept, final Runnable decline) {
        Runnable later = () -> { closeModal(); decline.run(); };
        updatePanel = openModal("发现新版本", later);
        updatePanel.add(modalCopy("南海航程 · 版本 " + version + "\n新航程已备妥，可下载完整安装包更新。"))
                .width(760).row();
        updatePanel.add(modalCopy("本机账号与存档将继续保留。也可稍后更新，先进入游戏。"))
                .width(760).row();
        Table actions = new Table();
        actions.add(modalButton("下载更新", true, () -> {
            // Build first: an implementation may synchronously deliver progress or failure.
            showDownloadPanel();
            accept.run();
        })).size(340, 88).padRight(32);
        actions.add(modalButton("稍后再说", false, later)).size(340, 88);
        updatePanel.add(actions).padTop(8).row();
        presentModal();
    }

    private void cancelUpdate() {
        closeModal();
        if (game.updateChecker != null) game.updateChecker.cancelDownload();
        if (msg != null) msg.setText("下载已取消，可继续登录。");
    }

    private void showDownloadPanel() {
        if (stage == null) return;
        updatePanel = openModal("新航程 · 下载更新", this::cancelUpdate);
        updateStatus = label("正在下载… 0%", 38);
        updateStatus.setColor(Color.valueOf("FFF0BF"));
        updateStatus.setName("updateStatus");
        updatePanel.add(updateStatus).width(760).height(60).row();
        ProgressBar.ProgressBarStyle ps = new ProgressBar.ProgressBarStyle();
        ps.background = progressTrack;
        ps.knobBefore = progressFill;
        updateProgress = new ProgressBar(0, 100, 1, false, ps);
        updateProgress.setName("updateProgress");
        updateProgress.setAnimateDuration(.1f);
        updatePanel.add(updateProgress).width(720).height(48).padBottom(28).row();
        updatePanel.add(modalCopy("完整安装包下载完成后，将打开系统安装界面。\n请保持网络连接，取消下载不影响本机存档。"))
                .width(760).row();
        updatePanel.add(modalButton("取消下载", false, this::cancelUpdate)).size(340, 88).row();
        presentModal();
    }

    private void showHelper(String name) {
        Table content = openModal(name + " · 南海航程", this::closeModal);
        if ("公告".equals(name)) {
            content.add(modalCopy("版本 0.28.14 · 武周海贸新篇\n\n"
                    + "一、更新与下载弹窗换上金边新装。\n"
                    + "二、遭遇或甩开海盗时，自动航行继续。\n"
                    + "三、公告、客服与本机设置现已开放。\n"
                    + "四、十九段武周主线，续写《南海见闻录》。"))
                    .width(760).row();
        } else if ("客服".equals(name)) {
            content.add(modalCopy("本机游戏，反馈请发给开发者。\n"
                    + "请附上版本号 0.28.14、问题截图和发生前的操作。\n\n"
                    + "账号与存档保存在这台设备上；请记好用户名和密码。"
                    + "卸载或清除应用数据会删除本机存档，更新请直接覆盖安装。\n\n"
                    + "更新下载失败时可稍后重试，或从版本发布页下载完整安装包。"))
                    .width(760).row();
        } else {
            content.add(modalCopy("设置仅影响登录画面，修改后立即生效并保存在本机。"))
                    .width(760).row();
            content.add(settingButton("登录动效", "loginMotion", true)).size(700, 80).row();
            content.add(settingButton("海水波纹", "loginSea", true)).size(700, 80).row();
            content.add(modalCopy("当前版本暂无音乐与音效。\n本机存档：账号与航程保存在当前设备，设置不会清除存档。"))
                    .width(760).row();
        }
        content.add(modalButton("关闭", false, this::closeModal)).size(340, 80).row();
        presentModal();
    }

    private TextButton settingButton(String title, String key, boolean initial) {
        TextButton toggle = button(title + "：" + (settings.getBoolean(key, initial) ? "开启" : "关闭"), modalBlue, modalHover);
        toggle.setName(key);
        toggle.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                boolean enabled = !settings.getBoolean(key, initial);
                settings.putBoolean(key, enabled).flush();
                toggle.setText(title + "：" + (enabled ? "开启" : "关闭"));
                applySettings();
            }
        });
        return toggle;
    }

    private void applySettings() {
        bg.setMotionEnabled(settings.getBoolean("loginMotion", true));
        bg.setSeaEnabled(settings.getBoolean("loginSea", true));
    }

    private BitmapFont font(int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/nanhai-cjk.ttf"));
        try {
            FreeTypeFontGenerator.FreeTypeFontParameter params = new FreeTypeFontGenerator.FreeTypeFontParameter();
            params.size = size;
            params.characters = FreeTypeFontGenerator.DEFAULT_CHARS
                    + Gdx.files.internal("fonts/ui-chars.txt").readString("UTF-8")
                    + "本机登录 · 本机存档（关掉不丢）公告客服设置暂未开放＊";
            return generator.generateFont(params);
        } finally { generator.dispose(); }
    }

    /** Thick gold rims remain readable after the 1920-wide UI is scaled down on phones. */
    private Drawable frame(String name, String fill) {
        Pixmap p = new Pixmap(48, 48, Pixmap.Format.RGBA8888);
        p.setColor(Color.valueOf("D8AE60")); p.fillRectangle(8, 0, 32, 48); p.fillRectangle(0, 8, 48, 32);
        p.setColor(Color.valueOf("FFF0B5")); p.fillRectangle(10, 0, 28, 2);
        p.setColor(Color.valueOf("70502C")); p.fillRectangle(8, 44, 32, 4);
        // Replace pixels so the panel's translucent center isn't composited over opaque gold.
        p.setBlending(Pixmap.Blending.None);
        p.setColor(Color.valueOf(fill)); p.fillRectangle(6, 8, 36, 32); p.fillRectangle(8, 6, 32, 36);
        Texture texture = new Texture(p); p.dispose();
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        loginSkin.add(name, texture);
        NinePatch patch = new NinePatch(texture, 12, 12, 12, 12);
        patch.setPadding(24, 24, 8, 8);
        return new NinePatchDrawable(patch);
    }

    private TextButton button(String text, Drawable up, Drawable down) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(loginSkin.get(TextButton.TextButtonStyle.class));
        style.up = up; style.down = down; style.over = down;
        style.fontColor = Color.valueOf("FFF4D3");
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
        bg = new LoginHarbor();
        bg.fit(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
        stage.addActor(bg);
        settings = Gdx.app.getPreferences("nanhai-settings");
        applySettings();
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
        modalFrame = frame("modalFrame", "081522");
        modalRed = frame("modalRed", "B34428");
        modalBlue = frame("modalBlue", "235873");
        modalHover = frame("modalHover", "497891");
        progressTrack = frame("progressTrack", "030B13");
        progressTrack.setMinHeight(44);
        progressFill = frame("progressFill", "C7542C");
        progressFill.setMinHeight(32);

        Drawable navy = frame("loginNavy", "091824");
        Drawable hover = frame("loginHover", "204C61");
        Drawable red = frame("loginRed", "B34428");
        Drawable redDown = frame("loginRedDown", "DB6238");
        Drawable registerBlue = frame("loginRegister", "235873");
        Table formPanel = new Table();
        formPanel.setName("loginFormPanel");
        formPanel.setBackground(frame("loginPanel", "06111CEB"));
        formPanel.setTouchable(Touchable.disabled);
        place(formPanel, 504, 352, 912, 304);
        place(label("扬帆南海 · 通商万国", 32), 576, 672, 768, 56);
        Label local = label("本机登录 · 本机存档（关掉不丢）", 28);
        local.setColor(Color.valueOf("FFF0CC"));
        place(local, 544, 600, 832, 40);

        TextField.TextFieldStyle fieldStyle = new TextField.TextFieldStyle(loginSkin.get("goldField", TextField.TextFieldStyle.class));
        fieldStyle.background = navy;
        fieldStyle.focusedBackground = hover;
        BitmapFont inputFont = font(38);
        loginSkin.add("loginInput", inputFont);
        fieldStyle.font = inputFont;
        fieldStyle.fontColor = Color.valueOf("FFF7E4");
        fieldStyle.messageFont = inputFont;
        fieldStyle.messageFontColor = Color.valueOf("D4DCE1");
        final TextField user = new TextField("summer", fieldStyle);
        user.setName("loginUsername"); user.setMessageText("请输入用户名");
        final TextField pass = new TextField("summer", fieldStyle);
        pass.setName("loginPassword"); pass.setMessageText("请输入密码");
        pass.setPasswordCharacter('＊'); pass.setPasswordMode(true);
        for (int i = 0; i < 2; i++) {
            int y = 496 - i * 112;
            Table caption = new Table(); caption.setBackground(navy);
            Label captionLabel = label(i == 0 ? "用户名" : "密码", 40);
            captionLabel.setColor(Color.valueOf("FFF4D3"));
            caption.add(captionLabel).expand().fill();
            place(caption, 544, y, 192, 80);
        }
        place(user, 752, 496, 624, 80);
        place(pass, 752, 384, 624, 80);
        TextButton login = button("登录", red, redDown);
        TextButton register = button("注册", registerBlue, hover);
        login.getLabel().setFontScale(1.75f); register.getLabel().setFontScale(1.75f);
        place(login, 552, 240, 352, 96);
        place(register, 1016, 240, 352, 96);
        login.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { doLogin(user.getText(), pass.getText()); }
        });
        register.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { doRegister(user.getText(), pass.getText()); }
        });
        msg = label("注册一个本机账号，或登录已有账号。", 28);
        msg.setColor(Color.valueOf("FFF0CC"));
        msg.setWrap(true);
        place(msg, 520, 144, 880, 72);
        String[] helpers = {"公告", "客服", "设置"};
        for (int i = 0; i < helpers.length; i++) {
            final String text = helpers[i];
            TextButton helper = button(text, modalBlue, modalHover);
            helper.setName("login" + text);
            helper.getLabel().setFontScale(.9f);
            place(helper, 1464 + i * 144, 960, 128, 72);
            helper.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) { showHelper(text); }
            });
        }
    }

    private void releaseUi() {
        closeModal();
        if (stage != null) {
            if (game.updateChecker != null) game.updateChecker.setListener(null);
            // enterVoyage already detaches input before hide() runs.
            Gdx.input.setCatchKey(Input.Keys.BACK, false);
        }
        if (stage != null && Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
        if (stage != null) { stage.dispose(); stage = null; }
        if (bg != null) { bg.dispose(); bg = null; }
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
            centerModal();
            if (bg != null) {
                bg.fit(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
            }
        }
    }

    @Override public void hide() {
        releaseUi();
    }

    @Override public void dispose() { releaseUi(); }
}
