package com.shipgame.nanhai.lwjgl3;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.*;
import com.shipgame.nanhai.data.Catalog;
import com.shipgame.nanhai.data.GameState;
import com.shipgame.nanhai.render.VoyageWater;
import com.shipgame.nanhai.render.VoyageWorldRenderer;
import java.lang.reflect.Field;

/** Actual OpenGL water checks. No accounts, login or persistent game state. */
public final class WaterSmokeLauncher {
    private static int result=1;
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config=new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(1280,720); config.disableAudio(true);
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override public void create() {
                VoyageWorldRenderer renderer=null;
                Pixmap still=null, moving=null, later=null;
                try {
                    renderer=new VoyageWorldRenderer();
                    require(renderer.hasWaterShader(),"custom shader must compile; fallback is not a shader pass");
                    Field f=VoyageWorldRenderer.class.getDeclaredField("water"); f.setAccessible(true);
                    VoyageWater water=(VoyageWater)f.get(renderer);
                    require(water.supportsHighQuality(),"high shader compiled");
                    require(water.supportsLowQuality(),"low shader compiled");
                    GameState state=GameState.newGame(); state.undockInPlace();
                    state.x=Catalog.PORT_X[0]+160; state.y=Catalog.PORT_Y[0]-180;
                    state.headingDeg=125; state.speed=0; state.windStr=.65f;
                    float x=state.x,y=state.y;
                    renderer.render(state,0); still=capture("still");
                    state.speed=85;
                    renderer.render(state,0); moving=capture("wake");
                    require(different(still,moving,0)>400,"speed creates visible wake");
                    for(int i=0;i<40;i++) renderer.render(state,.1f);
                    later=capture("animated");
                    require(different(moving,later,12)>6000,"surface animates with a stationary camera and ship");
                    renderer.setWaterQuality(false);
                    renderer.render(state,0); captureAndDispose("low");
                    require(Gdx.gl.glGetError()==GL20.GL_NO_ERROR,"low quality GL state");
                    Texture.invalidateAllTextures(Gdx.app);
                    Mesh.invalidateAllMeshes(Gdx.app);
                    com.badlogic.gdx.graphics.glutils.ShaderProgram.invalidateAllShaderPrograms(Gdx.app);
                    renderer.render(state,0);
                    require(Gdx.gl.glGetError()==GL20.GL_NO_ERROR,"managed water resources reload");
                    state.x=Catalog.PORT_X[0]+76; state.y=Catalog.PORT_Y[0]; state.headingDeg=180;
                    renderer.setWaterQuality(true);
                    for(int i=0;i<40;i++) renderer.render(state,.1f);
                    captureAndDispose("shore");
                    state.x=x; state.y=y; state.headingDeg=125;
                    state.pirateAlive=true; state.pirateX=x+120; state.pirateY=y-150;
                    for(int i=0;i<40;i++) renderer.render(state,.1f);
                    captureAndDispose("combat");
                    require(renderer.chaseDistance()>590,"combat camera unaffected");
                    require(state.x==x && state.y==y,"water does not move gameplay state");
                    water.dispose(); // Exercise the exact path used when shaders are unavailable.
                    require(!renderer.hasWaterShader(),"unavailable shader reports fallback");
                    renderer.render(state,0); captureAndDispose("fallback");
                    require(Gdx.gl.glGetError()==GL20.GL_NO_ERROR,"fallback GL state");
                    result=0;
                    System.out.println("WATER PASS: high/low GLSL, time animation, speed wake, resource reload, shoreline, combat, fallback, GL, unchanged gameplay position");
                } catch(Throwable t) { t.printStackTrace(); }
                finally {
                    if(still!=null) still.dispose(); if(moving!=null) moving.dispose(); if(later!=null) later.dispose();
                    if(renderer!=null) renderer.dispose();
                    Gdx.app.exit();
                }
            }
            private Pixmap capture(String name) {
                require(Gdx.gl.glGetError()==GL20.GL_NO_ERROR,"GL error at "+name);
                Pixmap p=Pixmap.createFromFrameBuffer(0,0,1280,720);
                PixmapIO.writePNG(Gdx.files.local("../Builds/water287-"+name+".png"),p,-1,true);
                return p;
            }
            private void captureAndDispose(String name) { capture(name).dispose(); }
        },config);
        System.exit(result);
    }
    private static int different(Pixmap a,Pixmap b,int threshold) {
        int changed=0;
        // Lower part of the framebuffer: water and wake, excluding sky.
        for(int y=0;y<440;y++) for(int x=0;x<1280;x++) {
            int p=a.getPixel(x,y),q=b.getPixel(x,y);
            int delta=Math.abs((p>>>24)-(q>>>24))+Math.abs(((p>>>16)&255)-((q>>>16)&255))+Math.abs(((p>>>8)&255)-((q>>>8)&255));
            if(delta>threshold) changed++;
        }
        return changed;
    }
    private static void require(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
}
