package com.shipgame.nanhai.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.shipgame.nanhai.NanHaiVoyage;
import com.shipgame.nanhai.data.GameState;
import com.shipgame.nanhai.data.SaveData;

public class LoginScreen extends ScreenAdapter {

    private final NanHaiVoyage game;
    private Stage stage;
    private Label msg;

    public LoginScreen(NanHaiVoyage game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(1280, 720), game.batch);
        Gdx.input.setInputProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Label title = new Label("南海航程", game.skin);
        title.setFontScale(1.4f);
        Label sub = new Label("本机登录 · 本机存档（关游戏不丢）", game.skin, "small");

        final TextField user = new TextField("", game.skin);
        user.setMessageText("用户名");
        final TextField pass = new TextField("", game.skin);
        pass.setMessageText("密码");
        pass.setPasswordMode(true);
        pass.setPasswordCharacter('*');

        msg = new Label("注册一个本机账号，或登录已有账号。", game.skin, "small");
        msg.setWrap(true);

        TextButton login = new TextButton("登录", game.skin, "go");
        TextButton reg = new TextButton("注册", game.skin);

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

        Table box = new Table();
        box.defaults().pad(8);
        box.add(title).colspan(2).padBottom(6).row();
        box.add(sub).colspan(2).padBottom(16).row();
        box.add(new Label("用户名", game.skin, "small")).left();
        box.add(user).width(360).height(42).row();
        box.add(new Label("密码", game.skin, "small")).left();
        box.add(pass).width(360).height(42).row();
        box.add(login).width(160).height(48);
        box.add(reg).width(160).height(48).row();
        box.add(msg).colspan(2).width(520).padTop(12);

        root.add(box);
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
            Gdx.input.setInputProcessor(null); // stage is disposed in hide(); release it first
            game.setScreen(new VoyageScreen(game));
        } catch (Exception ex) {
            Gdx.app.error("LoginScreen", "register failed", ex);
            msg.setText("注册错误。");
        }
    }

    private void doLogin(String u, String p) {
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
            Gdx.input.setInputProcessor(null); // stage is disposed in hide(); release it first
            game.setScreen(new VoyageScreen(game));
        } catch (Exception ex) {
            Gdx.app.error("LoginScreen", "login failed", ex);
            msg.setText("登录错误。");
        }
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.07f, 0.16f, 0.24f, 1f);
        if (stage == null) {
            return;
        }
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
        if (stage != null) {
            stage.dispose();
            stage = null;
        }
    }
}
