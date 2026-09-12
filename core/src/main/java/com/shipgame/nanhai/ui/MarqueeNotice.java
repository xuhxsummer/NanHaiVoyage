package com.shipgame.nanhai.ui;

import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;

/** Single-line, clipped announcement. Pause at either end so the whole message can be read. */
public final class MarqueeNotice extends ScrollPane {
    private final Label copy;
    private String message="";
    private float elapsed;
    public MarqueeNotice(Label copy) {
        super(copy,new ScrollPaneStyle()); this.copy=copy;
        copy.setWrap(false); copy.setName("航行消息文字");
        setName("航行消息轮播"); setScrollingDisabled(false,true);
        setOverscroll(false,false); setSmoothScrolling(false);
        setTouchable(Touchable.disabled);
    }
    public void setText(String text) {
        if(message.equals(text))return;
        message=text; copy.setText(text); elapsed=0; setScrollX(0); updateVisualScroll();
    }
    @Override public void act(float delta) {
        super.act(delta); validate();
        float overflow=getMaxX();
        if(overflow<=0) { elapsed=0;setScrollX(0); }
        else {
            elapsed+=Math.max(0,delta);
            float travel=overflow/70f,cycle=travel+2.4f;
            float phase=elapsed%cycle;
            setScrollX(Math.min(overflow,Math.max(0,phase-1.2f)*70f));
        }
        updateVisualScroll();
    }
}
