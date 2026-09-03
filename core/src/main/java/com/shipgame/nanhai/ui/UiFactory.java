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
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
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
        // Skin keys resources by runtime class and getDrawable(name) only looks in
        // the Drawable / TextureRegion / NinePatch / Sprite maps. tint() returns a
        // SpriteDrawable, so without the explicit key this lookup always throws
        // "No Drawable registered with name: panel" (the login->voyage crash).
        skin.add("panel", new TextureRegionDrawable(tex).tint(new Color(0.07f, 0.11f, 0.16f, 0.94f)),
                Drawable.class);

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
        tbs.down = new NinePatchDrawable(np).tint(new Color(0.30f, 0.52f, 0.64f, 0.97f));
        tbs.over = new NinePatchDrawable(np).tint(new Color(0.22f, 0.40f, 0.52f, 0.95f));
        tbs.disabledFontColor = new Color(0.6f, 0.6f, 0.6f, 1f);
        skin.add("default", tbs);

        TextButton.TextButtonStyle acc = new TextButton.TextButtonStyle(tbs);
        acc.up = new NinePatchDrawable(np).tint(new Color(0.22f, 0.42f, 0.28f, 0.92f));
        acc.down = new NinePatchDrawable(np).tint(new Color(0.34f, 0.64f, 0.42f, 0.97f));
        acc.over = new NinePatchDrawable(np).tint(new Color(0.28f, 0.54f, 0.35f, 0.95f));
        skin.add("go", acc);

        TextButton.TextButtonStyle danger = new TextButton.TextButtonStyle(tbs);
        danger.up = new NinePatchDrawable(np).tint(new Color(0.45f, 0.18f, 0.16f, 0.92f));
        danger.down = new NinePatchDrawable(np).tint(new Color(0.68f, 0.28f, 0.24f, 0.97f));
        danger.over = new NinePatchDrawable(np).tint(new Color(0.58f, 0.24f, 0.20f, 0.95f));
        skin.add("danger", danger);

        // Circular action buttons (left-rail 货物/图鉴/港口). Painted at 128px and
        // minified to the button size so the circle edge stays smooth.
        Pixmap cp = new Pixmap(128, 128, Pixmap.Format.RGBA8888);
        cp.setBlending(Pixmap.Blending.None);
        cp.setColor(0f, 0f, 0f, 0f);
        cp.fill();
        float cc = 64f, cr = 60f;
        for (int yy = 0; yy < 128; yy++) {
            for (int xx = 0; xx < 128; xx++) {
                float ddx = xx - cc, ddy = yy - cc;
                float dist = (float) Math.sqrt(ddx * ddx + ddy * ddy);
                if (dist <= cr - 1f) {
                    cp.setColor(1f, 1f, 1f, 1f);
                } else if (dist <= cr + 1f) {
                    cp.setColor(1f, 1f, 1f, Math.max(0f, (cr + 1f - dist) / 2f));
                } else {
                    cp.setColor(0f, 0f, 0f, 0f);
                }
                cp.drawPixel(xx, yy);
            }
        }
        Texture ctex = new Texture(cp);
        cp.dispose();
        skin.add("circle", ctex);
        TextureRegionDrawable circWhite = new TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(ctex));
        TextButton.TextButtonStyle circ = new TextButton.TextButtonStyle();
        circ.font = font;
        circ.fontColor = Color.WHITE;
        circ.up = circWhite.tint(new Color(0.14f, 0.26f, 0.36f, 0.92f));
        circ.down = circWhite.tint(new Color(0.34f, 0.56f, 0.68f, 0.98f));
        circ.over = circWhite.tint(new Color(0.22f, 0.42f, 0.55f, 0.96f));
        skin.add("circ", circ);

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
