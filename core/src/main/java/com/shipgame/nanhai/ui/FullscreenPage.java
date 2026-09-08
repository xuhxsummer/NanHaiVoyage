package com.shipgame.nanhai.ui;

import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;

/** Fits a design grid to the stage; the page itself covers every pixel and touch. */
public final class FullscreenPage extends WidgetGroup {
    private final Table content;
    private final float designWidth, designHeight;
    public FullscreenPage(Table content,float width,float height) {
        this.content=content; designWidth=width; designHeight=height;
        setName("fullscreenPage"); setTouchable(Touchable.enabled);
        content.setTransform(true); content.setOrigin(0,0); addActor(content);
    }
    @Override public void layout() {
        float scale=Math.min(getWidth()/designWidth,getHeight()/designHeight);
        if(scale<=0) return;
        content.setScale(scale);
        content.setBounds(0,0,getWidth()/scale,getHeight()/scale);
        content.validate();
    }
}
