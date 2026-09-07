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

/** Asset overview in design pixels. All mutations stay with the screen/GameState. */
public final class MyShipPanel extends Table implements Disposable {
    public static final float WIDTH = 1296, HEIGHT = 832;
    private final QuestUi ui;
    private final Skin skin;
    private final Texture atlas;
    private ScrollPane fleet;

    public MyShipPanel(Skin skin) {
        this.skin = skin;
        ui = new QuestUi(skin);
        atlas = new Texture(Gdx.files.internal("textures/shop/ships-atlas.png"));
        atlas.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    private Label label(String text, int size) { return ui.label(text, size, QuestUi.PAPER); }
    private Label wrap(String text, int size) {
        Label label = label(text, size); label.setWrap(true); return label;
    }
    private TextButton button(String text, Runnable action) {
        TextButton button = ui.button(text, true);
        button.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                event.stop(); action.run();
            }
        });
        return button;
    }
    private Image artwork(int ship) {
        int x = ship % 3 * atlas.getWidth() / 3, y = ship / 3 * atlas.getHeight() / 3;
        Image image = new Image(new TextureRegion(atlas, x + 2, y + 2,
                (ship % 3 + 1) * atlas.getWidth() / 3 - x - 4,
                (ship / 3 + 1) * atlas.getHeight() / 3 - y - 4));
        image.setScaling(Scaling.fit); return image;
    }
    private Table resource(String icon, String text) {
        Table cell = new Table();
        TextureRegionDrawable drawable = IconLib.hud(icon);
        if (drawable != null) cell.add(new Image(drawable)).size(40).padRight(8);
        cell.add(label(text, 24));
        return cell;
    }

    public void refresh(GameState g, String attributes, IntConsumer equip, Runnable close) {
        float scroll = fleet == null ? 0 : fleet.getScrollX();
        clearChildren(); setBackground(ui.frame); pad(24); top();
        Table heading = new Table();
        heading.add().width(104);
        Table title = new Table(); title.setBackground(ui.red); title.pad(0);
        title.add(label("我的 · 船与家当", 36));
        heading.add(title).size(480, 64).expandX();
        heading.add(button("关闭", close)).size(104, 56);
        add(heading).size(1248, 64).row();

        Table resources = new Table(); resources.setBackground(ui.inset); resources.pad(8);
        resources.add(resource("silver", "银两 " + g.silver + " 两")).width(304);
        resources.add(resource("supply", "补给 " + (int)g.supply + "/" + (int)g.supplyMax)).width(304);
        resources.add(resource("hull", "耐久 " + (int)g.hull + "/" + (int)g.hullMax)).width(304);
        resources.add(resource("crew", "船员 " + g.crew + "/" + g.crewMax())).width(304);
        add(resources).size(1248, 72).row();

        Table current = new Table(); current.pad(8, 16, 8, 16);
        current.add(label("当前：" + Catalog.SHIPS[g.ship], 28)).colspan(2).left().height(40).row();
        Table portrait = new Table(); portrait.setBackground(ui.inset); portrait.pad(4);
        portrait.add(artwork(g.ship)).size(264, 112);
        current.add(portrait).size(272, 120).padRight(24);
        Table stats = new Table();
        stats.add(wrap(attributes, 22)).width(920).height(64).left().row();
        stats.add(label("射速 " + Math.round(g.firepower()*10f)/10f + " 发/秒        货舱 "
                + g.holdCap() + "        船员上限 " + g.crewMax(), 26)).left().height(40);
        current.add(stats).width(920);
        add(current).size(1248, 176).row();
        add(label("已拥有船只（点「更换」随时免费换乘）", 24)).width(1232).height(40).left().row();

        Table ships = new Table(); ships.left();
        // Current vessel leads the strip, with all other owned ships reachable by swiping.
        addShip(ships, g, g.ship, equip);
        int count = 1;
        for (int i = 0; i < Catalog.SHIPS.length; i++) {
            if (i != g.ship && g.ownsShip(i)) { addShip(ships, g, i, equip); count++; }
        }
        for (int i = count; i < 4; i++) {
            Table empty = new Table(); empty.setBackground(ui.inset); empty.pad(8);
            Image silhouette = new Image(IconLib.ship(0));
            silhouette.setScaling(Scaling.fit); silhouette.setColor(.35f, .38f, .37f, .25f);
            empty.add(silhouette).size(192, 80);
            empty.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
            ships.add(empty).size(304, 104).padRight(8);
        }
        fleet = new ScrollPane(ships, skin); fleet.setScrollingDisabled(false, true);
        fleet.setOverscroll(false, false); fleet.setFadeScrollBars(false);
        add(fleet).size(1248, 112).row();

        Table cargo = new Table(); cargo.setBackground(ui.inset); cargo.pad(8, 24, 8, 24);
        cargo.add(label("货舱明细", 28)).left().height(40);
        cargo.add(label("货舱占用 " + g.cargoUsed() + "/" + g.holdCap(), 22)).right().height(40).row();
        Table summaries = new Table(); summaries.top().left();
        summaries.add(wrap(summary("商货", Catalog.GOODS, g.trade, 6), 23)).width(576).top().padRight(24);
        summaries.add(wrap(summary("异兽", Catalog.BEASTS, g.beasts, 4), 23)).width(576).top().row();
        summaries.add(wrap(summary("渔获", Catalog.FISH, g.fish, 4), 23)).width(576).top().pad(16, 0, 8, 24);
        summaries.add(wrap(summary("草药", Catalog.HERBS, g.herbs, 4), 23)).width(576).top().padTop(16).padBottom(8);
        ScrollPane contents = new ScrollPane(summaries, skin);
        contents.setScrollingDisabled(true, false); contents.setOverscroll(false, false);
        cargo.add(contents).colspan(2).size(1200, 112);
        add(cargo).size(1248, 176).row();
        add(label("换船不换货，不花银两；货舱不够时会提示先卖货。", 23))
                .width(1200).height(40).left();
        validate(); fleet.setScrollX(scroll); fleet.updateVisualScroll();
    }

    private void addShip(Table ships, GameState g, int ship, IntConsumer equip) {
        boolean current = ship == g.ship;
        Table card = new Table(); card.setBackground(current ? ui.blue : ui.inset); card.pad(8);
        card.setName("我的" + Catalog.SHIPS[ship]);
        card.add(artwork(ship)).size(112, 80).padRight(8);
        Table copy = new Table();
        copy.add(label(Catalog.SHIPS[ship], 24)).height(40).row();
        if (current) copy.add(label("【正在开】", 23)).height(40);
        else {
            TextButton swap = button("更换", () -> equip.accept(ship));
            swap.setName("更换" + Catalog.SHIPS[ship]);
            copy.add(swap).size(144, 48);
        }
        card.add(copy).width(152);
        ships.add(card).size(304, 104).padRight(8);
    }

    private static String summary(String kind, String[] names, int[] amounts, int limit) {
        StringBuilder text = new StringBuilder(kind).append("：");
        int shown = 0, types = 0;
        for (int i = 0; i < names.length; i++) {
            if (amounts[i] <= 0) continue;
            types++;
            if (shown >= limit) continue;
            if (shown++ > 0) text.append("、");
            text.append(names[i]).append("x").append(amounts[i]);
        }
        if (types == 0) text.append("空");
        else if (types > limit) text.append("等").append(types).append("种");
        return text.toString();
    }
    @Override public void dispose() { atlas.dispose(); ui.dispose(); }
}
