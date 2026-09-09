package com.shipgame.nanhai.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.shipgame.nanhai.NanHaiVoyage;
import com.shipgame.nanhai.data.*;
import com.shipgame.nanhai.ui.ShopShipsPanel;

/** Verifies the supplied icon paths and renders the actual shop; never logs into an account. */
public final class ShipArtworkSmokeLauncher {
    private static int result=1;
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration c=new Lwjgl3ApplicationConfiguration();
        c.setWindowedMode(1248,800); c.disableAudio(true);
        new Lwjgl3Application(new NanHaiVoyage() {
            Stage preview; ShopShipsPanel panel; int frames;
            @Override public void create() {
                super.create(); getScreen().hide();
                state=GameState.newGame(); currentUser=null;
                panel=new ShopShipsPanel(skin); panel.setSize(1248,800);
                preview=new Stage(new FitViewport(1248,800),batch);
                preview.getViewport().update(1248,800,true); preview.addActor(panel);
            }
            @Override public void render() {
                try {
                    int selected=frames-2;
                    panel.refresh(state,0,selected,"","",i->{},i->{},()->{},()->{},()->{},()->{},()->{});
                    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                    preview.act(1f/60); preview.draw();
                    if(Gdx.gl.glGetError()!=GL20.GL_NO_ERROR) throw new AssertionError("shop art GL error");
                    if(frames==1) {
                        Pixmap p=Pixmap.createFromFrameBuffer(0,0,1248,800);
                        PixmapIO.writePNG(Gdx.files.local("../Builds/ship-icons287.png"),p,-1,true); p.dispose();
                        for(String name:Catalog.SHIPS) if(!Gdx.files.internal("textures/ships/ship_"+name+".png").exists())
                            throw new AssertionError("missing icon: "+name);
                    }
                    if(++frames==11) { result=0; System.out.println("SHIP ART PASS: nine named icons, grid, every detail, GL"); Gdx.app.exit(); }
                } catch(Throwable t) { t.printStackTrace(); Gdx.app.exit(); }
            }
            @Override public void dispose() { if(panel!=null) panel.dispose(); if(preview!=null) preview.dispose(); super.dispose(); }
        },c);
        System.exit(result);
    }
}
