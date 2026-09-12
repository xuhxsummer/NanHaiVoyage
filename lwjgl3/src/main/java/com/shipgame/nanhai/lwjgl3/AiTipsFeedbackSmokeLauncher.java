package com.shipgame.nanhai.lwjgl3;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.shipgame.nanhai.NanHaiVoyage;
import com.shipgame.nanhai.data.*;
import com.shipgame.nanhai.render.VoyageWorldRenderer;
import com.shipgame.nanhai.screen.*;
import com.shipgame.nanhai.ui.ContextualTips;
import java.lang.reflect.*;
import java.nio.file.Path;

/** Isolated real GL/input/login lifecycle coverage of the combined 0.28.25 build. */
public final class AiTipsFeedbackSmokeLauncher {
    private static int result=1;
    public static void main(String[] args)throws Exception {
        Path dir=java.nio.file.Files.createTempDirectory("nanhai-2825-");
        Path output=Path.of("../Builds/combined2825").toAbsolutePath();
        Lwjgl3ApplicationConfiguration config=new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(1280,720);config.disableAudio(true);config.setForegroundFPS(0);
        config.setPreferencesConfig(dir.resolve("prefs").toString(),Files.FileType.Absolute);
        new Lwjgl3Application(new NanHaiVoyage(){
            VoyageScreen voyage;Stage stage;VoyageWorldRenderer renderer;int frame;boolean failed;
            @Override public void create(){
                Files original=Gdx.files;
                Gdx.files=(Files)Proxy.newProxyInstance(Files.class.getClassLoader(),new Class[]{Files.class},(p,m,a)->{
                    if(m.getName().equals("local"))return new FileHandle(dir.resolve((String)a[0]).toFile());
                    return m.invoke(original,a);
                });
                try {
                    super.create();
                    if(args.length>0 && args[0].equals("portraits")) {
                        for(int i=0;i<4;i++) {
                            TextureData data=com.shipgame.nanhai.ui.IconLib.avatar(i).getRegion().getTexture().getTextureData();
                            data.prepare(); Pixmap portrait=data.consumePixmap();
                            PixmapIO.writePNG(new FileHandle(output.resolve("avatar-reference-"+i+".png").toFile()),portrait);
                            portrait.dispose();
                        }
                        result=0;failed=true;Gdx.app.exit();return;
                    }
                    accounts.register("combined-test","local-password");currentUser="combined-test";
                    state=GameState.newGame();state.questDialogueSeen=(1<<19)-1;state.supply=400;quiet();enter();
                }catch(Throwable t){fail(t);}
            }
            @Override public void render(){
                if(failed)return;
                try {
                    if(frame++==0){tips();feedback();Gdx.graphics.setWindowedMode(1600,720);}
                    else {
                        voyage.resize(1600,720);voyage.render(0);
                        require(tip("在港口"),"new login rearms port tip on wide screen");capture("wide-login-tip");
                        require(actor("contextualTip").getWidth()<=1600 && actor("contextualTip").getX()>=0,"tip stays on screen");
                        result=0;System.out.println("COMBINED GL PASS: five contextual tips, real actions/dismiss, no repeat, login rearm, no HOWTO auto-wall, NPC rendering/ram flash/splash, typed enemy sink and camera, delayed loot, player sink-before-fail, close-restart, 1280/1600");Gdx.app.exit();
                    }
                }catch(Throwable t){fail(t);}
            }
            private void quiet(){state.pirateSpawnTimer=state.merchantSpawnTimer=state.traderSpawnTimer=state.warshipSpawnTimer=10000;}
            private void enter()throws Exception{
                voyage=new VoyageScreen(this);setScreen(voyage);stage=(Stage)field("stage").get(voyage);renderer=(VoyageWorldRenderer)field("world3d").get(voyage);
            }
            private void tips()throws Exception {
                voyage.render(0);require(field("overlay").get(voyage).toString().equals("PORT"),"no first-install HOWTO wall");
                require(tip("在港口"),"port tip on actual port overlay");capture("port-tip");
                tap("补补给");voyage.render(0);require(!actor("contextualTip").isVisible() && state.questRefillCount==1,"successful supply dismisses port tip");
                tap("离港");state.x=2000;state.y=12000;state.headingDeg=0;voyage.render(.6f);
                require(tip("摇杆"),"first idle voyage shows stick tip");capture("stick-tip");
                float cx=field("stickCX").getFloat(voyage),cy=field("stickCY").getFloat(voyage);
                Vector2 point=stage.stageToScreenCoordinates(new Vector2(cx,cy+55));
                InputProcessor input=Gdx.input.getInputProcessor();input.touchDown((int)point.x,(int)point.y,0,0);
                voyage.render(.1f);require(!actor("contextualTip").isVisible() && state.thrustInput>0,"joystick remains usable and dismisses tip");
                input.touchUp((int)point.x,(int)point.y,0,0);voyage.render(.6f);
                require(tip("任务卡"),"quest card prompt when visible");tap("任务卡");voyage.render(0);
                require(field("overlay").get(voyage).toString().equals("DIALOGUE") && !actor("contextualTip").isVisible(),"card opens story and dismisses tip");
                tap("dialogueClose");voyage.render(.6f);
                state.x=Catalog.PORT_X[0]+190;state.y=Catalog.PORT_Y[0];state.speed=0;voyage.render(0);
                require(tip("抛锚"),"first reachable port enables dock tip");capture("dock-tip");tap("抛锚");voyage.render(0);
                require(state.anchored && !actor("contextualTip").isVisible(),"anchor completes contextual action");
                state.anchored=false;state.x=2000;state.y=12000;
                pirate(500,60);voyage.render(.6f);require(tip("点海盗"),"first pirate prompt");capture("pirate-tip");
                float minute=state.dayMin;tap("contextualTipDismiss");voyage.render(.1f);
                require(state.dayMin>minute && !actor("contextualTip").isVisible(),"dismiss does not pause or lock combat");
                state.clearPirate();pirate(500,60);voyage.render(1f);
                require(!actor("contextualTip").isVisible(),"second pirate cannot repeat same-cycle prompt");
                state.clearPirate();quiet();
            }
            private void feedback()throws Exception {
                state.x=2000;state.y=12000;state.headingDeg=0;state.speed=0;
                pirate(5,0);state.pirateHp=48;
                Method impact=GameState.class.getDeclaredMethod("updateShipImpacts",float.class);impact.setAccessible(true);impact.invoke(state,0f);
                voyage.render(.06f);require(state.hitFlash(0)>0,"actual renderer sees player flash");capture("ram-rock-splash");
                state.clearPirate();state.updateFeedback(1);pirate(450,140);state.pirateHp=state.pirateHpMax=1000;state.combatLock=true;
                WarshipData w=new WarshipData();w.ship=8;w.x=state.x+570;w.y=state.y-170;w.hp=w.hpMax=1000;w.provoked=true;state.warships[0]=w;
                for(int i=0;i<40;i++)voyage.render(.1f);capture("flank-ships");
                require(renderer.project(w.x,w.y,27,new Vector3()),"warship has a visible sea position");
                // Force an actual player projectile kill for presentation and delayed loot.
                state.warships[0]=null;state.combatLock=false;state.ballCount=0;state.pirateHp=1;
                Method fire=GameState.class.getDeclaredMethod("fireAt",boolean.class,int.class,int.class,float.class,float.class,float.class,float.class);fire.setAccessible(true);
                fire.invoke(state,true,1,1,state.x,state.y,state.pirateX,state.pirateY);
                Method balls=GameState.class.getDeclaredMethod("updateBalls",float.class);balls.setAccessible(true);balls.invoke(state,1f);
                voyage.render(0);require(!field("overlay").get(voyage).toString().equals("LOOT"),"no same-frame loot");
                for(int i=0;i<7;i++)voyage.render(.1f);capture("enemy-sink-bubbles");
                require(state.sinkFocus.alive && state.sinkFocus.tilt>30,"visible sink beat");
                Field offset=VoyageWorldRenderer.class.getDeclaredField("focusOffset");offset.setAccessible(true);
                require(((Vector3)offset.get(renderer)).len()>5,"camera lightly follows wreck");
                for(int i=0;i<6;i++)voyage.render(.1f);
                require(field("overlay").get(voyage).toString().equals("LOOT"),"loot appears after beat");capture("delayed-loot");
                for(int i=0;i<12;i++)voyage.render(.1f);require(!state.sinkFocus.alive,"sink continues beneath loot card");invoke("closePopup");
                state.hull=0;state.fail("船沉");voyage.render(.1f);
                require(!field("overlay").get(voyage).toString().equals("FAIL") && state.sinkFocus.player,"player sink precedes failure UI");
                for(int i=0;i<8;i++)voyage.render(.1f);capture("player-sink");
                for(int i=0;i<15;i++)voyage.render(.1f);
                require(field("overlay").get(voyage).toString().equals("FAIL"),"failure UI after player sink");capture("failure-after-sink");
                invoke("closePopup");voyage.render(0);require(state.hull>0 && !state.failed,"failure close restarts");
                ContextualTips old=(ContextualTips)field("contextualTips").get(voyage);
                require(old.shown(ContextualTips.Tip.PORT_TRADE),"restart does not reset login tips");
                // Real authentication plus the same new-screen entry used by LoginScreen.
                setScreen(new LoginScreen(this));require(accounts.login("combined-test","local-password"),"login succeeds");
                state=GameState.fromSave(accounts.load("combined-test"));currentUser="combined-test";quiet();enter();voyage.render(0);
                require(field("contextualTips").get(voyage)!=old && tip("在港口"),"new login rearms same tip");
            }
            private void pirate(float dx,float dy){state.pirateAlive=true;state.pirateX=state.x+dx;state.pirateY=state.y+dy;state.pirateHp=state.pirateHpMax=48;state.pirateDamage=1;state.pirateFireCd=1000;}
            private boolean tip(String part){return actor("contextualTip").isVisible() && ((Label)actor("contextualTipCopy")).getText().toString().contains(part);}
            private Field field(String name)throws Exception{Field f=VoyageScreen.class.getDeclaredField(name);f.setAccessible(true);return f;}
            private void invoke(String name)throws Exception{Method m=VoyageScreen.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(voyage);}
            private Actor actor(String name){Actor a=stage.getRoot().findActor(name);require(a!=null,"actor "+name);return a;}
            private void tap(String name){Actor a=actor(name);Vector2 p=a.localToStageCoordinates(new Vector2(a.getWidth()/2,a.getHeight()/2));stage.stageToScreenCoordinates(p);InputProcessor input=Gdx.input.getInputProcessor();input.touchDown(Math.round(p.x),Math.round(p.y),0,0);input.touchUp(Math.round(p.x),Math.round(p.y),0,0);}
            private void capture(String name){require(Gdx.gl.glGetError()==GL20.GL_NO_ERROR,"GL "+name);Pixmap p=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());PixmapIO.writePNG(new FileHandle(output.resolve(name+".png").toFile()),p,-1,true);p.dispose();}
            private void fail(Throwable t){t.printStackTrace();failed=true;Gdx.app.exit();}
        },config);System.exit(result);
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
