package com.shipgame.nanhai.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.Vector3;
import com.shipgame.nanhai.NanHaiVoyage;
import com.shipgame.nanhai.data.Catalog;
import com.shipgame.nanhai.data.GameState;
import com.shipgame.nanhai.data.VoyageGeometry;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.shipgame.nanhai.render.VoyageWorldRenderer;
import com.shipgame.nanhai.screen.VoyageScreen;
import java.lang.reflect.Field;

/** Real OpenGL / Scene2D integration check, using an unsaved in-memory voyage. */
public class Voyage3dSmokeLauncher {
    private static int result=1;
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration c=new Lwjgl3ApplicationConfiguration();
        c.setWindowedMode(1280,720); c.disableAudio(true); c.setForegroundFPS(0);
        new Lwjgl3Application(new NanHaiVoyage() {
            VoyageScreen screen; VoyageWorldRenderer renderer; int frame; float startX;
            Vector3 projected=new Vector3();
            @Override public void create() {
                super.create();
                state=GameState.newGame(); currentUser=null;
                state.undockInPlace(); state.x=Catalog.PORT_X[0]+160; state.y=Catalog.PORT_Y[0]-180;
                state.headingDeg=125; state.pirateSpawnTimer=10000;
                screen=new VoyageScreen(this); setScreen(screen);
                try {
                    Field f=VoyageScreen.class.getDeclaredField("world3d"); f.setAccessible(true);
                    renderer=(VoyageWorldRenderer) f.get(screen); require(renderer!=null,"3D initialization");
                    closeOverlay(); startX=state.x; checkModelGeometry();
                } catch(Exception e) { fail(e); }
            }
            private void closeOverlay() throws Exception {
                Field f=VoyageScreen.class.getDeclaredField("overlay"); f.setAccessible(true);
                for(Object v:f.getType().getEnumConstants()) if(v.toString().equals("NONE")) f.set(screen,v);
                java.lang.reflect.Method m=VoyageScreen.class.getDeclaredMethod("rebuildMenu"); m.setAccessible(true); m.invoke(screen);
            }
            @Override public void render() {
                try {
                    screen.render(1f/60); frame++;
                    require(Gdx.gl.glGetError()==GL20.GL_NO_ERROR,"GL error frame "+frame);
                    if(frame==2) {
                        require(renderer.project(state.x,state.y,9,projected),"ship visible");
                        require(projected.x>450 && projected.x<830 && projected.y>100 && projected.y<360,"lower-middle framing "+projected);
                        snap("sail"); state.speed=60;
                    }
                    if(frame==62) {
                        require(Math.abs(state.x-startX)>10,"sailing advances world position"); state.speed=0;
                        state.pirateAlive=true; state.pirateHp=state.pirateHpMax=200;
                        state.pirateX=state.x+cosDeg(state.headingDeg)*160;
                        state.pirateY=state.y+sinDeg(state.headingDeg)*160;
                        state.pirateHeading=state.headingDeg+180;
                    }
                    if(frame==180) {
                        require(renderer.chaseDistance()>460,"combat zoom out");
                        require(renderer.project(state.pirateX,state.pirateY,27,projected),"pirate visible");
                        require(renderer.hit(projected.x,720-projected.y,state.pirateX,state.pirateY,27,36),"perspective pick ray");
                        Gdx.input.getInputProcessor().touchDown((int)projected.x,720-(int)projected.y,0,0);
                        require(state.combatLock,"pirate tap locks through HUD"); snap("combat");
                    }
                    if(frame==205) {
                        require(state.pirateHp<200,"cannon damage");
                        Gdx.input.getInputProcessor().touchDown((int)projected.x,720-(int)projected.y,0,0);
                        require(!state.combatLock,"pirate tap toggles lock");
                        state.pirateAlive=false; state.combatLock=false;
                    }
                    if(frame==206) require(renderer.chaseDistance()>300,"return is smooth");
                    if(frame==330) {
                        require(renderer.chaseDistance()<VoyageWorldRenderer.SAIL_DISTANCE+2,"normal chase restored"); snap("restored");
                        state.x=Catalog.PORT_X[0]+Catalog.DOCK_RANGE-4; state.y=Catalog.PORT_Y[0]; state.headingDeg=180;
                        state.startAutoSail(0);
                    }
                    if(frame==390) {
                        require(!state.autoSail && state.nearestPortInRange()==0 && state.speed==0,"auto-sail stops in enlarged docking range");
                        closeOverlay(); state.undockInPlace(); state.speed=0;
                        // A full-map modal must still open and close via the real input path.
                        Gdx.input.getInputProcessor().touchDown(1174,104,0,0);
                        Gdx.input.getInputProcessor().touchUp(1174,104,0,0);
                    }
                    if(frame==400) {
                        Field stageField=VoyageScreen.class.getDeclaredField("stage"); stageField.setAccessible(true);
                        com.badlogic.gdx.scenes.scene2d.Stage stage=(com.badlogic.gdx.scenes.scene2d.Stage)stageField.get(screen);
                        require(!stage.getRoot().isVisible(),"minimap opens full-map modal"); snap("map");
                        Gdx.input.getInputProcessor().touchDown(1218,64,0,0);
                        Gdx.graphics.setWindowedMode(1600,720);
                    }
                    if(frame==450) {
                        require(renderer.camera.viewportWidth==1600,"resize camera"); snap("wide");
                        // Touch docking uses a perspective proxy and the shared range gate.
                        state.x=Catalog.PORT_X[0]+Catalog.DOCK_RANGE-6; state.y=Catalog.PORT_Y[0]; state.headingDeg=180;
                        closeOverlay();
                    }
                    if(frame==540) {
                        require(renderer.project(Catalog.PORT_X[0],Catalog.PORT_Y[0],15,projected),"port visible");
                        Gdx.input.getInputProcessor().touchDown((int)projected.x,720-(int)projected.y,0,0);
                        require(state.dockedPort==0,"port tap docks through perspective");
                        screen.pause(); // currentUser null: never writes an account save.
                        screen.hide(); screen.show(); screen.resize(1600,720);
                        Field rf=VoyageScreen.class.getDeclaredField("world3d"); rf.setAccessible(true);
                        renderer=(VoyageWorldRenderer)rf.get(screen);
                    }
                    if(frame>=541 && frame<=549) {
                        int id=frame-541;
                        state.silver=100000; state.buyShip(id);
                        require(state.ship==id,"shop selected ship "+id);
                        Field sf=VoyageWorldRenderer.class.getDeclaredField("ships"); sf.setAccessible(true);
                        ModelInstance[] models=(ModelInstance[])sf.get(renderer);
                        for(ModelInstance m:models) m.transform.setToTranslation(-9999,0,0);
                        renderer.render(state,0);
                        require(Math.abs(models[id].transform.getTranslation(new Vector3()).x-state.x)<.01,"next render switches model "+id);
                        snap("ship-"+id);
                        state.equipShip(0); renderer.render(state,0);
                        require(Math.abs(models[0].transform.getTranslation(new Vector3()).x-state.x)<.01,"equip restores starter model");
                    }
                    if(frame==550) { snap("reentered"); result=0; System.out.println("VOYAGE3D PASS: framing, movement, combat, ray picking, docking, resize, re-entry, all ship purchases/equips, model bounds, GL"); Gdx.app.exit(); }
                } catch(Throwable t) { fail(t); }
            }
            private void checkModelGeometry() throws Exception {
                Field sf=VoyageWorldRenderer.class.getDeclaredField("ships"); sf.setAccessible(true);
                ModelInstance[] ships=(ModelInstance[])sf.get(renderer);
                java.util.Set<String> sizes=new java.util.HashSet<>();
                for(int i=0;i<ships.length;i++) {
                    BoundingBox bounds=ships[i].calculateBoundingBox(new BoundingBox());
                    sizes.add(bounds.toString());
                    checkRadius(ships[i],VoyageGeometry.ship(i).radius());
                }
                require(sizes.size()==Catalog.SHIPS.length,"distinct ship dimensions");
                Field lf=VoyageWorldRenderer.class.getDeclaredField("scenery"); lf.setAccessible(true);
                com.badlogic.gdx.utils.Array<ModelInstance> land=(com.badlogic.gdx.utils.Array<ModelInstance>)lf.get(renderer);
                java.util.Set<Object> unique=new java.util.HashSet<>();
                for(int i=0;i<land.size;i++) {
                    boolean port=i<Catalog.PORTS.length; int id=port?i:i-Catalog.PORTS.length;
                    unique.add(land.get(i).model);
                    checkRadius(land.get(i),VoyageGeometry.landRadius(port,id));
                }
                require(unique.size()==Catalog.PORTS.length+Catalog.ISLANDS.length,"per-entry land models");
            }
            private void checkRadius(ModelInstance instance,float radius) {
                for(com.badlogic.gdx.graphics.Mesh mesh:instance.model.meshes) {
                    int stride=mesh.getVertexSize()/4;
                    int offset=mesh.getVertexAttribute(com.badlogic.gdx.graphics.VertexAttributes.Usage.Position).offset/4;
                    float[] vertices=new float[mesh.getNumVertices()*stride]; mesh.getVertices(vertices);
                    for(int v=offset;v<vertices.length;v+=stride)
                        require(Math.hypot(vertices[v],vertices[v+2])<=radius+.01,"mesh outside collision envelope");
                }
            }
            private float cosDeg(float angle) {return com.badlogic.gdx.math.MathUtils.cosDeg(angle);}
            private float sinDeg(float angle) {return com.badlogic.gdx.math.MathUtils.sinDeg(angle);}
            private void snap(String name) {
                Pixmap p=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
                PixmapIO.writePNG(Gdx.files.local("../Builds/voyage3d-"+name+".png"),p,-1,true); p.dispose();
            }
            private void fail(Throwable t) { t.printStackTrace(); result=1; Gdx.app.exit(); }
        },c);
        System.exit(result);
    }
    private static void require(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
}
