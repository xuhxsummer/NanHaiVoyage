package com.shipgame.nanhai.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

/** A bottom speaker box; the full-screen dimmer catches taps before the sailing HUD. */
public final class QuestDialogue extends Table {
    private final String[][] lines;
    private final String objective;
    private final Runnable dismiss;
    private final Label speaker, body, hint;
    private final TextButton primary;
    private int line;

    public QuestDialogue(Skin skin, QuestUi ui, String title, String[][] lines, String objective,
                         String actionText, Runnable action, Runnable dismiss) {
        this.lines = lines;
        this.objective = objective;
        this.dismiss = dismiss;
        setName("questDialogue");
        setTouchable(Touchable.enabled);
        setBackground(skin.newDrawable("white", new Color(.01f, .02f, .03f, .5f)));
        bottom().pad(18);

        Table panel = new Table(); panel.setName("dialoguePanel");
        panel.setBackground(ui.frame); panel.pad(16, 24, 16, 24);
        Table header = new Table();
        Table plaque = new Table(); plaque.setBackground(ui.red);
        speaker = ui.label("", 28, QuestUi.PAPER); speaker.setName("dialogueSpeaker");
        plaque.add(speaker).center();
        header.add(plaque).size(180, 44).padRight(20);
        Label chapter = ui.label(title, 24, QuestUi.PAPER); chapter.setEllipsis(true);
        header.add(chapter).growX().left();
        TextButton close = ui.button("关闭", false); close.setName("dialogueClose");
        close.addListener(action(dismiss));
        header.add(close).size(112, 44).padLeft(16);
        panel.add(header).growX().height(44).padBottom(12).row();

        Table paper = new Table(); paper.setBackground(ui.parchment); paper.pad(10, 20, 10, 20);
        body = ui.dialogueLine(""); body.setName("dialogueBody");
        body.setWrap(true); body.setAlignment(Align.left);
        paper.add(body).grow();
        panel.add(paper).growX().height(112).padBottom(10).row();
        Table footer = new Table();
        hint = ui.label("", 21, QuestUi.PAPER); hint.setName("dialogueHint");
        footer.add(hint).growX().left();
        primary = ui.button(actionText, true); primary.setName("dialogueAction");
        primary.addListener(action(action));
        footer.add(primary).size(160, 44).padLeft(16);
        panel.add(footer).growX().height(44);
        add(panel).growX().height(264);
        addListener(new ClickListener() {
            @Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                // Buttons have their own touch focus. Their release must never also advance a line.
                for (Actor target = event.getTarget(); target != null; target = target.getParent())
                    if (target instanceof TextButton) return false;
                return super.touchDown(event, x, y, pointer, button);
            }
            @Override public void clicked(InputEvent event, float x, float y) {
                event.stop();
                if (line < lines.length) { line++; refresh(); }
                else dismiss.run();
            }
        });
        refresh();
    }

    private void refresh() {
        boolean ending = line >= lines.length;
        speaker.setText(ending ? "此程所托" : lines[line][0]);
        body.setText(ending ? objective : lines[line][1]);
        hint.setText(ending ? "点击空白收起 · 也可选择右侧按钮" : "点击继续  ·  " + (line + 1) + "/" + lines.length);
        primary.setVisible(ending);
    }

    private static ClickListener action(Runnable action) {
        return new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { event.stop(); action.run(); }
        };
    }
}
