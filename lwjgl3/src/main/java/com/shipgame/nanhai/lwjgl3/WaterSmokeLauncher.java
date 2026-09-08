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
                    state.x=1900; state.y=2400;
                    state.headingDeg=125; state.speed=0; state.windStr=.65f;
                    float x=state.x,y=state.y;
                    renderer.render(state,0); still=capture("still");
                    for(int i=0;i<12;i++) renderer.render(state,.1f);
                    later=capture("idle-animated");
                    require(different(still,later,8)>3000,"stopped open-sea water animates without input");
                    require(state.speed==0 && state.x==x && state.y==y,"idle ripples do not propel the ship");
                    still.dispose(); still=later; later=null;
                    renderer.render(state,0); later=capture("idle-paused");
                    require(different(still,later,0)==0,"paused water clock produces an identical frame");
                    later.dispose(); later=null;
                    state.speed=85;
                    renderer.render(state,0); moving=capture("wake");
                    require(different(still,moving,0)>400,"speed creates visible wake");
                    for(int i=0;i<40;i++) renderer.render(state,.1f);
                    later=capture("animated");
                    require(different(moving,later,12)>6000,"surface animates with a stationary camera and ship");
                    renderer.setWaterQuality(false);
                    state.speed=0;
                    renderer.render(state,0); captureAndDispose("low");
                    require(Gdx.gl.glGetError()==GL20.GL_NO_ERROR,"low quality GL state");
                    Texture.invalidateAllTextures(Gdx.app);
                    Mesh.invalidateAllMeshes(Gdx.app);
                    com.badlogic.gdx.graphics.glutils.ShaderProgram.invalidateAllShaderPrograms(Gdx.app);
                    renderer.render(state,0);
                    require(Gdx.gl.glGetError()==GL20.GL_NO_ERROR,"managed water resources reload");
                    state.x=Catalog.PORT_X[0]+Catalog.DOCK_RANGE; state.y=Catalog.PORT_Y[0]; state.headingDeg=180;
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
                    state.pirateAlive=false; state.speed=0;
                    for(int i=0;i<40;i++) renderer.render(state,.1f);
                    still.dispose(); still=null;
                    renderer.render(state,0); still=capture("fallback-idle");
                    Field ringsField=VoyageWorldRenderer.class.getDeclaredField("idleRings"); ringsField.setAccessible(true);
                    com.badlogic.gdx.graphics.g3d.ModelInstance[] rings=(com.badlogic.gdx.graphics.g3d.ModelInstance[])ringsField.get(renderer);
                    for(var ring:rings) for(var node:ring.nodes) for(var part:node.parts) part.enabled=false;
                    renderer.render(state,0);
                    Pixmap withoutRings=capture("fallback-no-idle");
                    int idlePixels=different(still,withoutRings,0);
                    withoutRings.dispose();
                    require(idlePixels>20 && idlePixels<12000,"fallback hull ripples are visible and remain local: "+idlePixels);
                    for(var ring:rings) for(var node:ring.nodes) for(var part:node.parts) part.enabled=true;
                    for(int i=0;i<10;i++) renderer.render(state,.1f);
                    later.dispose(); later=capture("fallback-idle-animated");
                    require(different(still,later,8)>400,"fallback ripples animate at rest");
                    require(Gdx.gl.glGetError()==GL20.GL_NO_ERROR,"fallback GL state");
                    result=0;
                    System.out.println("WATER PASS: high/low GLSL, idle ripples without input, pause, speed wake, resource reload, shoreline, combat, fallback idle animation, GL, unchanged gameplay position");
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
                PixmapIO.writePNG(Gdx.files.local("../Builds/water289-"+name+".png"),p,-1,true);
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
