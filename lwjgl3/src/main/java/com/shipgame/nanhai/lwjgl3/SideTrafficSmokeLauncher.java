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
import com.shipgame.nanhai.screen.VoyageScreen;
import java.lang.reflect.*;
import java.nio.file.Path;

/** Real GL/input checks for optional dialogue and perspective NPC picking, with isolated saves. */
public final class SideTrafficSmokeLauncher {
    private static int result=1;
    public static void main(String[] args)throws Exception {
        Path dir=java.nio.file.Files.createTempDirectory("nanhai-side-traffic-");
        Path output=Path.of("../Builds/side-traffic2816").toAbsolutePath();
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
                    super.create();Gdx.app.getPreferences("nanhai-voyage").putBoolean("howto_shown",true).flush();
                    accounts.register("side-traffic","local-password");currentUser="side-traffic";
                    state=GameState.newGame();state.undockInPlace();state.x=2000;state.y=12000;state.headingDeg=0;
                    state.questDialogueSeen=(1<<19)-1;state.pirateSpawnTimer=state.merchantSpawnTimer=10000;
                    voyage=new VoyageScreen(this);setScreen(voyage);stage=(Stage)field("stage").get(voyage);renderer=(VoyageWorldRenderer)field("world3d").get(voyage);
                }catch(Throwable t){fail(t);}
            }
            @Override public void render(){
                if(failed)return;
                try {
                    if(frame++==0){side();traffic();Gdx.graphics.setWindowedMode(1600,720);}
                    else {
                        voyage.resize(1600,720);voyage.render(0);tap("任务");voyage.render(0);reveal("questRow30");tap("questRow30");voyage.render(0);
                        Actor panel=actor("dialoguePanel");require(panel.getWidth()>1400,"side dialogue fills wide screen");capture("wide-side");
                        result=0;System.out.println("SIDE TRAFFIC GL PASS: rail row to dialogue/detail/claim, skip and replay, saved reward, main-only HUD, 9 visible roster hulls, NPC perspective taps and cancel, camera orbit lifetime, off-horizon render, 1280/1600");Gdx.app.exit();
                    }
                }catch(Throwable t){fail(t);}
            }
            private void side()throws Exception {
                voyage.render(0);tap("任务");voyage.render(0);
                tapActor(button(stage.getRoot(),"支线"));voyage.render(0);capture("side-list");tap("questRow19");voyage.render(0);
                require(text("dialogueSpeaker").equals("雷州盐商"),"side row starts authored dialogue");capture("side-dialogue");
                float minute=state.dayMin;for(int n=0;n<4;n++)voyage.render(.1f);require(state.dayMin==minute,"side dialogue pauses world");
                for(int n=0;n<3;n++){tap("dialogueBody");voyage.render(0);}
                require(((TextButton)actor("dialogueAction")).getText().toString().equals("详情 / 领奖"),"dialogue final page leads to full detail");
                tap("dialogueAction");voyage.render(0);require(actor("questStory")!=null && field("selectedQuest").getInt(voyage)==19,"returns to selected side detail");
                TextButton claim=button(stage.getRoot(),"领取");require(claim.isDisabled(),"incomplete side reward disabled");
                state.questGoodsBought[3]=12;invoke("rebuildMenu");voyage.render(0);
                claim=button(stage.getRoot(),"领取");revealActor(claim);capture("side-reward");int silver=state.silver;
                tapActor(claim);voyage.render(0);require(state.sideQuestClaims==1 && state.silver==silver+60,"side reward once");
                require(accounts.load(currentUser).sideQuestClaims==1,"side claim saved on account");
                reveal("questRow30");tap("questRow30");voyage.render(0);tap("dialogueClose");voyage.render(0);
                require(field("selectedQuest").getInt(voyage)==30 && actor("questStory")!=null,"skip returns to last side detail");
                tapActor(button(stage.getRoot(),"关闭"));voyage.render(0);tap("任务卡");voyage.render(0);
                require(text("dialogueBody").contains("武则天"),"HUD remains first main quest");tap("dialogueClose");voyage.render(0);
            }
            private void traffic()throws Exception {
                state.pirateAlive=true;state.pirateX=state.x+880;state.pirateY=state.y+160;
                state.pirateHp=state.pirateHpMax=48;state.pirateDamage=7;state.pirateFireCd=1000;
                MerchantData m=new MerchantData();state.merchant=m;m.x=state.x+820;m.y=state.y-130;m.rest=100;
                for(int ship=0;ship<Catalog.SHIPS.length;ship++) {
                    m.ship=ship;m.hp=m.hpMax=GameState.merchantHull(ship);m.damage=GameState.merchantDamage(ship);
                    voyage.render(0);Vector3 v=new Vector3();require(renderer.project(m.x,m.y,27,v),"roster merchant visible in main sea");
                    require(renderer.project(state.pirateX,state.pirateY,27,new Vector3()),"pirate visible in spawn band");
                    if(ship==0 || ship==8)capture("sea-roster-"+ship);
                }
                Vector3 v=new Vector3();renderer.project(m.x,m.y,27,v);screenTap(v.x,Gdx.graphics.getHeight()-v.y);
                require(state.merchantLock && m.hostile,"world merchant tap uses lock UX");voyage.render(0);capture("plunder-lock");
                tap("取消锁定");require(!state.merchantLock,"visible cancel drops merchant focus");
                renderer.project(state.pirateX,state.pirateY,27,v);screenTap(v.x,Gdx.graphics.getHeight()-v.y);
                require(state.combatLock && !state.merchantLock,"world pirate tap switches focus");state.cancelLock();
                float px=state.pirateX,py=state.pirateY;
                renderer.beginLook();renderer.dragLook(.8f,0);voyage.render(0);capture("look-away");
                require(state.pirateAlive && state.pirateX==px && state.pirateY==py,"camera orbit does not despawn or move pirate");
                renderer.resetLook();voyage.render(0);require(state.pirateAlive,"look back keeps same pirate");
                m.x=state.x+3000;voyage.render(0);require(state.merchant==m && !state.merchantVisible(),"distant merchant retained, culled from render");
            }
            private Field field(String name)throws Exception {Field f=VoyageScreen.class.getDeclaredField(name);f.setAccessible(true);return f;}
            private void invoke(String name)throws Exception{Method m=VoyageScreen.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(voyage);}
            private Actor actor(String name){Actor a=stage.getRoot().findActor(name);require(a!=null,"actor "+name);return a;}
            private String text(String name){return ((Label)actor(name)).getText().toString();}
            private void reveal(String name){revealActor(actor(name));}
            private void revealActor(Actor actor){
                for(Actor a=actor.getParent();a!=null;a=a.getParent())if(a instanceof ScrollPane){
                    ScrollPane sp=(ScrollPane)a;Vector2 p=actor.localToAscendantCoordinates(sp.getActor(),new Vector2());
                    sp.scrollTo(p.x,p.y,actor.getWidth(),actor.getHeight(),false,true);sp.updateVisualScroll();
                }
                voyage.render(0);
            }
            private void tap(String name){tapActor(actor(name));}
            private void tapActor(Actor a){Vector2 p=a.localToStageCoordinates(new Vector2(a.getWidth()/2,a.getHeight()/2));stage.stageToScreenCoordinates(p);screenTap(p.x,p.y);}
            private void screenTap(float x,float y){InputProcessor input=Gdx.input.getInputProcessor();input.touchDown(Math.round(x),Math.round(y),0,0);input.touchUp(Math.round(x),Math.round(y),0,0);}
            private void capture(String name){require(Gdx.gl.glGetError()==GL20.GL_NO_ERROR,"GL "+name);Pixmap p=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());PixmapIO.writePNG(new FileHandle(output.resolve(name+".png").toFile()),p,-1,true);p.dispose();}
            private void fail(Throwable t){t.printStackTrace();failed=true;Gdx.app.exit();}
        },config);System.exit(result);
    }
    private static TextButton button(Group g,String text){for(Actor a:g.getChildren()){if(a instanceof TextButton && ((TextButton)a).getText().toString().equals(text))return (TextButton)a;if(a instanceof Group){TextButton b=button((Group)a,text);if(b!=null)return b;}}return null;}
    private static void require(boolean ok,String text){if(!ok)throw new AssertionError(text);}
}
