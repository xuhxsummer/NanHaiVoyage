package com.shipgame.nanhai.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Disposable;

/** Paused captain menu. Save, load and logout remain owned by VoyageScreen. */
public final class CaptainMenuPanel extends Table implements Disposable {
    public static final float WIDTH = 752, HEIGHT = 560;
    private final QuestUi ui;

    public CaptainMenuPanel(Skin skin, Runnable save, Runnable load, Runnable logout, Runnable fill, Runnable close) {
        ui = new QuestUi(skin);
        setBackground(ui.frame); pad(24); top();
        Table heading = new Table();
        heading.add(ui.label("船长菜单 · 世界暂停", 32, QuestUi.PAPER)).expandX().left().padLeft(16);
        heading.add(action("关闭", true, close)).size(96, 56);
        add(heading).size(704, 64).padBottom(16).row();

        Table explanation = new Table(); explanation.left();
        explanation.add(ui.label("菜单开启后停船停事件。", 24, QuestUi.JADE)).left().height(32).row();
        Label copy = ui.label("保存进度 = 立刻把当前进度写入本机存档；\n读取存档 = 回最近一次靠港自动档。", 22, QuestUi.PAPER);
        copy.setWrap(true);
        explanation.add(copy).width(624).height(72).left();
        add(explanation).size(624, 112).padBottom(24).row();

        add(action("保存进度", false, save)).size(544, 60).padBottom(12).row();
        TextButton reload = action("读取存档", false, load);
        TextButton.TextButtonStyle reloadStyle = new TextButton.TextButtonStyle(reload.getStyle());
        reloadStyle.fontColor = Color.valueOf("C6DFC0");
        reload.setStyle(reloadStyle);
        add(reload).size(544, 60).padBottom(12).row();
        add(action("满级账号", true, fill)).size(544, 60).padBottom(12).row();
        add(action("退出登录", true, logout)).size(544, 60).row();
    }

    private TextButton action(String text, boolean danger, Runnable action) {
        TextButton button = ui.button(text, danger);
        button.setName(text);
        if (!text.equals("关闭")) {
            Label title = button.getLabel();
            button.clearChildren();
            String icon = text.equals("保存进度") ? "avatar" : text.equals("读取存档") ? "quest" : "port";
            button.add(new Image(IconLib.hud(icon))).size(48).padRight(24);
            button.add(title).width(240);
        }
        button.getLabel().setFontScale((text.equals("关闭") ? 24f : 32f) / 22f);
        button.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                event.stop(); action.run();
            }
        });
        return button;
    }

    @Override public void dispose() { ui.dispose(); }
}
