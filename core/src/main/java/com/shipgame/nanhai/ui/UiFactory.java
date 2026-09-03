package com.shipgame.nanhai.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/** Programmatic Scene2D skin (no uiskin atlas). */
public final class UiFactory {

    public static Skin create(BitmapFont font, BitmapFont small) {
        Skin skin = new Skin();
        skin.add("font", font);
        skin.add("small", small);

        Pixmap pm = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        Texture tex = new Texture(pm);
        pm.dispose();
        skin.add("white", tex);
        skin.add("panel", new TextureRegionDrawable(tex).tint(new Color(0.07f, 0.11f, 0.16f, 0.94f)));

        NinePatch np = new NinePatch(tex, 1, 1, 1, 1);
        skin.add("patch", np);

        Label.LabelStyle ls = new Label.LabelStyle(font, Color.WHITE);
        skin.add("default", ls);
        Label.LabelStyle lss = new Label.LabelStyle(small, Color.WHITE);
        skin.add("small", lss);

        TextButton.TextButtonStyle tbs = new TextButton.TextButtonStyle();
        tbs.font = font;
        tbs.fontColor = Color.WHITE;
        tbs.up = new NinePatchDrawable(np).tint(new Color(0.16f, 0.28f, 0.38f, 0.92f));
        tbs.down = new NinePatchDrawable(np).tint(new Color(0.28f, 0.48f, 0.58f, 0.95f));
        tbs.over = new NinePatchDrawable(np).tint(new Color(0.22f, 0.40f, 0.52f, 0.95f));
        tbs.disabledFontColor = new Color(0.6f, 0.6f, 0.6f, 1f);
        skin.add("default", tbs);

        TextButton.TextButtonStyle acc = new TextButton.TextButtonStyle(tbs);
        acc.up = new NinePatchDrawable(np).tint(new Color(0.22f, 0.42f, 0.28f, 0.92f));
        skin.add("go", acc);

        TextButton.TextButtonStyle danger = new TextButton.TextButtonStyle(tbs);
        danger.up = new NinePatchDrawable(np).tint(new Color(0.45f, 0.18f, 0.16f, 0.92f));
        skin.add("danger", danger);

        TextField.TextFieldStyle tfs = new TextField.TextFieldStyle();
        tfs.font = font;
        tfs.fontColor = Color.WHITE;
        tfs.background = new NinePatchDrawable(np).tint(new Color(0.08f, 0.10f, 0.14f, 0.9f));
        tfs.cursor = new TextureRegionDrawable(tex).tint(Color.WHITE);
        tfs.selection = new TextureRegionDrawable(tex).tint(new Color(0.3f, 0.5f, 0.7f, 0.5f));
        skin.add("default", tfs);

        ScrollPane.ScrollPaneStyle sps = new ScrollPane.ScrollPaneStyle();
        sps.background = new NinePatchDrawable(np).tint(new Color(0.05f, 0.08f, 0.10f, 0.55f));
        skin.add("default", sps);

        Window.WindowStyle ws = new Window.WindowStyle();
        ws.titleFont = font;
        ws.titleFontColor = Color.WHITE;
        ws.background = new NinePatchDrawable(np).tint(new Color(0.08f, 0.12f, 0.18f, 0.94f));
        skin.add("default", ws);

        return skin;
    }

    private UiFactory() {}
}
