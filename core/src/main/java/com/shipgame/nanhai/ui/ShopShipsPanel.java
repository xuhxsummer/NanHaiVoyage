package com.shipgame.nanhai.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Scaling;
import com.shipgame.nanhai.data.Catalog;
import com.shipgame.nanhai.data.GameState;
import java.util.function.IntConsumer;

/** Shop presentation in 1920x1080 design pixels. Economy remains in GameState. */
public final class ShopShipsPanel extends Table implements Disposable {
    public static final float WIDTH = 1248, HEIGHT = 800;
    private static final String[] CATEGORIES = {"船只", "战船", "货船", "特种船"};
    private final QuestUi ui;
    private final Texture atlas;

    public ShopShipsPanel(Skin skin) {
        ui = new QuestUi(skin);
        atlas = new Texture(Gdx.files.internal("textures/shop/ships-atlas.png"));
        atlas.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    // Catalog has no category field; match the reference labels without changing stats.
    private int category(int i) {
        if (Catalog.SHIP_HOLD[i] > 0) return 2;
        if (i == 7 || i == 8) return 3;
        return i == 0 ? 0 : 1;
    }

    private Image artwork(int i) {
        int x = (i % 3) * atlas.getWidth() / 3;
        int y = (i / 3) * atlas.getHeight() / 3;
        int right = (i % 3 + 1) * atlas.getWidth() / 3;
        int bottom = (i / 3 + 1) * atlas.getHeight() / 3;
        Image image = new Image(new TextureRegionDrawable(new TextureRegion(atlas,
                x + 2, y + 2, right - x - 4, bottom - y - 4)));
        image.setScaling(Scaling.fit);
        return image;
    }

    private TextButton button(String title, boolean primary, Runnable action) {
        TextButton button = ui.button(title, primary);
        button.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                event.stop(); action.run();
            }
        });
        return button;
    }

    private Label copy(String text, int size) {
        Label label = ui.label(text, size, QuestUi.PAPER);
        label.setWrap(true);
        return label;
    }

    public void refresh(GameState g, int cat, int selected, String attributes, String effects,
                        IntConsumer filter, IntConsumer detail, Runnable back,
                        Runnable purchase, Runnable equip, Runnable close) {
        clearChildren();
        setBackground(ui.frame); pad(24);
        Table heading = new Table();
        heading.add().width(104);
        Table title = new Table(); title.setBackground(ui.blue); title.pad(0);
        title.add(ui.label("商城 · 买船换船", 32, QuestUi.PAPER));
        heading.add(title).size(464, 64).expandX();
        heading.add(button("关闭", true, close)).size(104, 56);
        add(heading).size(1200, 64).row();
        add(copy(g.silver + " 两    当前：" + Catalog.SHIPS[g.ship]
                + "（货舱 " + g.holdCap() + " · 船员 " + g.crew + "/" + g.crewMax()
                + " · 射速 " + Math.round(g.firepower() * 10f) / 10f + " 发/秒）", 24))
                .size(1200, 40).row();
        add(copy("战船提高射速与船员上限；货船加大货舱（与仓库升级叠加）。换小货舱船前请先卖掉多余货物。", 20))
                .size(1200, 48).row();

        Table body = new Table();
        Table categories = new Table(); categories.top().pad(8); categories.setBackground(ui.inset);
        categories.add(ui.label("分类", 28, QuestUi.PAPER)).height(40).padBottom(8).row();
        for (int c = 0; c < CATEGORIES.length; c++) {
            final int index = c;
            TextButton tab = button(CATEGORIES[c], false, () -> filter.accept(index));
            tab.setName("商城分类" + CATEGORIES[c]);
            if (c == cat) {
                TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(tab.getStyle());
                style.up = ui.blue; style.fontColor = QuestUi.JADE;
                tab.setStyle(style);
            } else {
                TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(tab.getStyle());
                style.up = ui.inset; tab.setStyle(style);
            }
            categories.add(tab).size(176, 64).padBottom(16).row();
        }
        body.add(categories).size(192, 552).padRight(16);
        if (selected < 0 || selected >= Catalog.SHIPS.length) {
            Table grid = new Table(); grid.top().left();
            int count = 0;
            for (int i = 0; i < Catalog.SHIPS.length; i++) {
                if (cat != 0 && category(i) != cat) continue;
                final int index = i;
                Table card = new Table(); card.setBackground(i == g.ship ? ui.blue : ui.inset);
                card.pad(4); card.setName("商城" + Catalog.SHIPS[i]);
                Table name = new Table();
                name.add(ui.label(Catalog.SHIPS[i], 24, QuestUi.PAPER)).expandX().left();
                String tag = i == g.ship ? "当前" : g.ownsShip(i) ? "已拥有" : CATEGORIES[category(i)];
                name.add(ui.label(tag, 18, g.ownsShip(i) ? QuestUi.JADE : QuestUi.PAPER)).right();
                Table caption = new Table(); caption.top().pad(4);
                caption.add(name).growX().height(32);
                Image art = artwork(i); art.setScaling(Scaling.stretch);
                Stack picture = new Stack(); picture.add(art); picture.add(caption);
                card.add(picture).size(232, 136).row();
                card.add(ui.label(Catalog.SHIP_PRICE[i] + " 两", 22, QuestUi.PAPER)).height(32);
                card.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent event, float x, float y) { detail.accept(index); }
                });
                grid.add(card).size(240, 176).pad(4);
                if (++count % 4 == 0) grid.row();
            }
            body.add(grid).size(992, 552);
        } else {
            Table det = new Table(); det.setBackground(ui.inset); det.pad(16).top();
            det.add(button("返回列表", false, back)).size(160, 56).left().row();
            Table hero = new Table();
            hero.add(artwork(selected)).size(432, 288).padRight(24);
            Table description = new Table(); description.top();
            description.add(copy(Catalog.SHIPS[selected] + " · " + CATEGORIES[category(selected)], 32)).width(496).padBottom(16).row();
            description.add(copy(Catalog.SHIP_DESC[selected], 24)).width(496).padBottom(16).row();
            description.add(copy(attributes, 22)).width(496).row();
            hero.add(description).width(496);
            det.add(hero).size(960, 288).row();
            det.add(copy(effects, 22)).size(960, 80).row();
            Table actions = new Table();
            actions.add(ui.label("船价 " + Catalog.SHIP_PRICE[selected] + " 两", 26, QuestUi.PAPER)).expandX().left();
            if (selected == g.ship) {
                TextButton current = button("当前船只", false, () -> {});
                current.setDisabled(true); actions.add(current).size(256, 64);
            } else {
                boolean owned = g.ownsShip(selected);
                actions.add(button(owned ? "免费换乘" : "购买 · " + Catalog.SHIP_PRICE[selected] + " 两",
                        !owned, owned ? equip : purchase)).size(256, 64);
            }
            det.add(actions).size(960, 64);
            body.add(det).size(992, 552);
        }
        add(body).size(1200, 552).row();
        add(copy("点船看详情；已拥有的船可以随时免费换乘。", 22)).size(1200, 48);
    }

    @Override public void dispose() { atlas.dispose(); ui.dispose(); }
}
