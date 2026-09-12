package com.shipgame.nanhai.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

/** A bottom speaker box; the full-screen dimmer catches taps before the sailing HUD.
 * The active speaker alone appears above the frame: NPC left, player 我 right. */
public final class QuestDialogue extends Table {
    private final String[][] lines;
    private final String objective;
    private final Runnable dismiss;
    private final java.util.function.UnaryOperator<String> actionText;
    private final Label speaker, body, hint;
    private final TextButton primary;
    private final Image leftBust, rightBust;
    private final Table leftSlot, rightSlot;
    private int line;

    /** Paper-cutout busts by dialogue speaker; generic 老者/水手 fallback for unknown names. */
    public static String bustSlug(String speaker) {
        if (speaker == null) return "npc_laoren";
        switch (speaker) {
            case "老掌柜": case "掌柜": case "商客": return "npc_zhanggui";
            case "船医": return "npc_yi";
            case "画师": return "npc_huashi";
            case "老者": case "水手": return "npc_laoren";
            default: return "npc_laoren";
        }
    }

    public QuestDialogue(Skin skin, QuestUi ui, String title, String[][] lines, String objective,
                         String actionText, Runnable action, Runnable dismiss) {
        this(skin, ui, title, lines, objective, s -> actionText, action, dismiss);
    }

    public QuestDialogue(Skin skin, QuestUi ui, String title, String[][] lines, String objective,
                         java.util.function.UnaryOperator<String> actionText, Runnable action, Runnable dismiss) {
        this.lines = lines;
        this.objective = objective;
        this.dismiss = dismiss;
        this.actionText = actionText;
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

        Table paper = new Table(); paper.setName("dialoguePaper"); paper.setBackground(ui.parchment); paper.pad(10, 20, 10, 20);
        leftBust = bustImage(ui, null); leftSlot = bustSlot(leftBust, "dialogueBustNpc");
        rightBust = bustImage(ui, null); rightSlot = bustSlot(rightBust, "dialogueBustPlayer");
        body = ui.dialogueLine(""); body.setName("dialogueBody");
        body.setFontScale(1f); body.setWrap(true); body.setAlignment(Align.left);
        paper.add(body).grow();
        panel.add(paper).growX().height(210).padBottom(10).row();
        Table footer = new Table();
        hint = ui.label("", 21, QuestUi.PAPER); hint.setName("dialogueHint");
        footer.add(hint).growX().left();
        primary = ui.button(actionText.apply(null), true); primary.setName("dialogueAction");
        primary.addListener(action(action));
        footer.add(primary).size(160, 44).padLeft(16);
        panel.add(footer).growX().height(44);
        Table portraits=new Table(); portraits.setTouchable(Touchable.disabled);
        portraits.add(leftSlot).size(210,240).left().padLeft(24);
        portraits.add().expandX();
        portraits.add(rightSlot).size(210,240).right().padRight(24);
        com.badlogic.gdx.scenes.scene2d.ui.Value columnWidth=new com.badlogic.gdx.scenes.scene2d.ui.Value() {
            @Override public float get(Actor context) { return Math.min(940,QuestDialogue.this.getWidth()-80); }
        };
        add(portraits).width(columnWidth).height(240).row();
        add(panel).width(columnWidth).height(352);
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

    private static Image bustImage(QuestUi ui, TextureRegion region) {
        Image image = new Image();
        image.setScaling(com.badlogic.gdx.utils.Scaling.fit);
        if (region != null) image.setDrawable(new TextureRegionDrawable(region));
        return image;
    }

    private static Table bustSlot(Image image, String name) {
        Table slot = new Table(); slot.setName(name);
        slot.setTransform(true);
        slot.add(image).grow();
        return slot;
    }

    private void setBust(Image image, String slug) {
        TextureRegionDrawable art = slug == null ? null : IconLib.bust(slug);
        if (art != null) {
            image.setDrawable(art);
            image.setColor(1, 1, 1, 1);
        } else {
            // Missing art: keep the slot but hide the image; text-only fallback stays readable.
            image.setDrawable((Drawable) null);
        }
        image.setVisible(art != null);
    }

    private void refresh() {
        boolean ending = line >= lines.length;
        String speakerName = ending ? "此程所托" : lines[line][0];
        speaker.setText("你".equals(speakerName) ? "我" : speakerName);
        body.setText(ending ? objective : lines[line][1]);
        hint.setText(""); // Advance on tap without a visible instruction caption.
        primary.setText(actionText.apply(speakerName));
        boolean playerSpeaking = !ending && ("你".equals(speakerName) || "我".equals(speakerName));
        setBust(leftBust, !ending && !playerSpeaking ? bustSlug(speakerName) : null);
        setBust(rightBust, playerSpeaking ? "player_" + playerAvatarIndex() : null);
        leftSlot.setVisible(!ending && !playerSpeaking);
        rightSlot.setVisible(playerSpeaking);
        primary.setVisible(ending);
    }


    /** Same identity source as the HUD portrait and captain menu (set via reflection-free hook). */
    private static volatile java.util.function.IntSupplier avatarSupplier;

    /** VoyageScreen injects g.avatarIndex so the right bust matches the captain. */
    public static void setAvatarSupplier(java.util.function.IntSupplier supplier) {
        avatarSupplier = supplier;
    }

    private static int playerAvatarIndex() {
        java.util.function.IntSupplier supplier = avatarSupplier;
        if (supplier == null) return 0;
        try {
            return supplier.getAsInt();
        } catch (Throwable t) {
            return 0;
        }
    }

    private static ClickListener action(Runnable action) {
        return new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { event.stop(); action.run(); }
        };
    }
}
