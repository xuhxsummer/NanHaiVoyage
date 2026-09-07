package com.shipgame.nanhai.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Disposable;
import com.shipgame.nanhai.data.Catalog;
import com.shipgame.nanhai.data.GameState;
import java.util.function.Consumer;

/** Dock presentation only: each action is dispatched to the existing screen handlers. */
public final class PortDockPanel extends Table implements Disposable {
    public enum Action { MARKET, SUPPLY, REPAY, REPAIR, LEAVE, WAREHOUSE, CANNON, CREW_CAP, HIRE, FISH }
    public static final float WIDTH = 1152, HEIGHT = 816;
    private final QuestUi ui;

    public PortDockPanel(Skin skin) { ui = new QuestUi(skin); }

    public void refresh(GameState g, Consumer<Action> action, Runnable close) {
        clearChildren(); setBackground(ui.frame); pad(24); top();
        Table header = new Table();
        header.add().width(96);
        Table title = new Table(); title.setBackground(ui.blue); title.pad(0);
        title.add(ui.label(Catalog.PORTS[g.dockedPort] + " · 世界暂停", 32, QuestUi.PAPER));
        header.add(title).size(528, 64).expandX();
        header.add(button("关闭", "", null, true, close)).size(96, 56);
        add(header).size(1104, 64).row();

        Table resources = new Table(); resources.setBackground(ui.inset); resources.pad(8);
        resources.add(resource("silver", "银两 " + g.silver + " 两")).width(272);
        resources.add(resource("supply", "补给 " + (int)g.supply + "/" + (int)g.supplyMax)).width(272);
        resources.add(resource("hull", "耐久 " + (int)g.hull + "/" + (int)g.hullMax)).width(272);
        resources.add(resource("crew", "船员 " + g.crew + "/" + g.crewMax())).width(272);
        add(resources).size(1104, 64).row();
        add(ui.label("欠 " + g.debt + " 两    舱 " + g.cargoUsed() + "/" + g.holdCap(), 22, QuestUi.PAPER))
                .size(1072, 32).left().row();

        add(button("市场（买卖货物 · 行情）", "", "shop", true, () -> action.accept(Action.MARKET)))
                .size(1104, 80).padBottom(8).row();
        Table grid = new Table();
        pair(grid,
                button("补补给", "补充粮草和物资", "supply", false, () -> action.accept(Action.SUPPLY)),
                button("还债(全还)", "清偿船只欠款", "silver", false, () -> action.accept(Action.REPAY)));
        pair(grid,
                button("修理", "修复船体耐久", "hull", false, () -> action.accept(Action.REPAIR)),
                button("离港", "离开当前港口", "port", true, () -> action.accept(Action.LEAVE)));
        pair(grid,
                button("升仓库 " + g.warehouseCost() + " 两", "提升共用货舱容量", "cargo", false, () -> action.accept(Action.WAREHOUSE)),
                button("升炮火 " + g.cannonCost() + " 两", "提高射速", "mine", false, () -> action.accept(Action.CANNON)));
        grid.add(button("升编制 " + g.crewCapCost() + " 两", "扩充船员编制", "quest", false, () -> action.accept(Action.CREW_CAP))).size(544, 80).padRight(16);
        grid.add(button("雇人 " + Catalog.HIRE_COST + " 两", "招募船员", "crew", false, () -> action.accept(Action.HIRE))).size(544, 80);
        add(grid).size(1104, 344).row();

        if (g.dockedPort == Catalog.YANGZHOU) {
            add(button("渔务", "雇渔夫 · 捕鱼 · 渔获", "port", false, () -> action.accept(Action.FISH)))
                    .size(1104, 72).padTop(8).row();
        } else {
            add(ui.label("点「市场」查看各港行情，经营船只后继续航程。", 22, QuestUi.JADE))
                    .size(1104, 80).row();
        }
        Table footer = new Table(); footer.left();
        footer.add(ui.label("船员 " + g.crew + "/" + g.crewMax() + "    每发伤 1 · 射速 "
                + Math.round(g.firepower() * 10f) / 10f + " 发/秒    耐久 " + (int)g.hull, 22, QuestUi.PAPER)).left().height(32).row();
        footer.add(ui.label("点「市场」看各港价并买卖，点「离港」开船。", 22, QuestUi.PAPER)).left().height(32);
        add(footer).size(1072, 64);
    }

    private Table resource(String icon, String text) {
        Table row = new Table();
        row.add(new Image(IconLib.hud(icon))).size(32).padRight(8);
        row.add(ui.label(text, 22, QuestUi.PAPER));
        return row;
    }

    private void pair(Table grid, TextButton left, TextButton right) {
        grid.add(left).size(544, 80).padRight(16).padBottom(8);
        grid.add(right).size(544, 80).padBottom(8).row();
    }

    private TextButton button(String title, String detail, String icon, boolean primary, Runnable action) {
        TextButton button = ui.button(title, primary);
        button.setName(title);
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(button.getStyle());
        if (!primary) { style.up = ui.parchment; style.fontColor = QuestUi.INK; }
        style.downFontColor = QuestUi.PAPER;
        style.overFontColor = primary ? QuestUi.PAPER : QuestUi.INK;
        if (!primary) style.over = ui.parchment;
        button.setStyle(style);
        Label label = button.getLabel();
        label.setFontScale((title.startsWith("市场") ? 32f : 26f) / 22f);
        button.clearChildren(); button.pad(4, 16, 4, 16);
        if (icon != null) button.add(new Image(IconLib.hud(icon))).size(detail.isEmpty() ? 48 : 40).padRight(16);
        Table copy = new Table();
        copy.add(label).row();
        if (!detail.isEmpty()) copy.add(ui.label(detail, 18, primary ? QuestUi.PAPER : QuestUi.INK));
        button.add(copy);
        button.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { event.stop(); action.run(); }
        });
        return button;
    }

    @Override public void dispose() { ui.dispose(); }
}
