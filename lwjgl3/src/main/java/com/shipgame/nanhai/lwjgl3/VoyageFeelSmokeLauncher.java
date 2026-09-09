package com.shipgame.nanhai.lwjgl3;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.shipgame.nanhai.NanHaiVoyage;
import com.shipgame.nanhai.data.*;
import com.shipgame.nanhai.screen.*;
import com.shipgame.nanhai.render.VoyageWorldRenderer;
import com.shipgame.nanhai.ui.IconLib;
import java.lang.reflect.*;
import java.nio.file.Path;

/** 0.28.9 real GL/touch/save checks. Accounts and preferences are isolated from player data. */
public final class VoyageFeelSmokeLauncher {
    private static int result=1;
    public static void main(String[] args) throws Exception {
        Path dir=java.nio.file.Files.createTempDirectory("nanhai-feel2811-");
        Lwjgl3ApplicationConfiguration config=new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(1280,720);config.disableAudio(true);config.setForegroundFPS(0);
        config.setPreferencesConfig(dir.resolve("prefs").toString(),Files.FileType.Absolute);
        new Lwjgl3Application(new NanHaiVoyage() {
            VoyageScreen voyage; Stage stage; VoyageWorldRenderer renderer; int frame;
            final String[] pages={"AVATAR","QUESTS","CARGO","INTEL","CODEX","SHOP","MINE","STAT","HOWTO","FAIL","FISH","PRICE","REDEEM","DAILY"};
            @Override public void create() {
                Files original=Gdx.files;
                Gdx.files=(Files)Proxy.newProxyInstance(Files.class.getClassLoader(),new Class[]{Files.class},(p,m,a)->{
                    if(m.getName().equals("local")) {
                        if(((String)a[0]).startsWith("../Builds/")) return m.invoke(original,a);
                        return new FileHandle(dir.resolve((String)a[0]).toFile());
                    }
                    return m.invoke(original,a);
                });
                super.create();
                try {
                    Gdx.app.getPreferences("nanhai-voyage").putBoolean("howto_shown",true).flush();
                    accounts.register("feel-test","local-password");currentUser="feel-test";state=GameState.newGame();
                    state.questDialogueSeen=(1<<19)-1; // Dialogue has its own input/persistence smoke; this run exercises sailing.
                    sea(); accounts.save(currentUser,state.toSave());
                    voyage=new VoyageScreen(this);setScreen(voyage);
                    stage=(Stage)field("stage").get(voyage);renderer=(VoyageWorldRenderer)field("world3d").get(voyage);
                    overlay("NONE");
                } catch(Throwable t){fail(t);}
            }
            private void sea() {
                state.failed=false;state.dockedPort=state.islandMenu=-1;
                state.x=700;state.y=2900;state.speed=0;state.headingDeg=0;state.pirateAlive=false;
                state.pirateSpawnTimer=10000;state.supply=state.supplyMax;state.hull=state.hullMax;state.cancelAutoSail();
            }
            @Override public void render() {
                try {
                    if(frame==0) {
                        voyage.render(1f/60);
                        steerAndLook();
                        profile();
                        redeemAndHud();
                        dailyAndShop();
                        sceneSnapshots();
                    } else if(frame<=pages.length) {
                        page(pages[frame-1]);
                    } else if(frame==pages.length+1) {
                        interactions(); Gdx.graphics.setWindowedMode(1600,720);
                    } else if(frame<=pages.length*2+1) {
                        page(pages[frame-pages.length-2]);
                    } else if(frame==pages.length*2+2) {
                        sea(); overlay("NONE");voyage.render(0);tap("船长");voyage.render(0);
                        tap("账号");voyage.render(0);tap("退出登录");
                    } else {
                        super.render();
                        require(getScreen() instanceof LoginScreen && currentUser==null && state==null,"logout session cleanup");
                        result=0;
                        System.out.println("VOYAGE FEEL PASS: stopped/slow/all-heading helm, undock, multitouch look+return+clamps, HUD ownership, all 14 fullscreen pages at 1280/1600, pause, profile/save/load/avatars, dock/market/island, logout, GL");
                        Gdx.app.exit();return;
                    }
                    require(Gdx.gl.glGetError()==GL20.GL_NO_ERROR,"GL frame "+frame);
                    frame++;
                }catch(Throwable t){fail(t);}
            }
            private void steerAndLook() throws Exception {
                float cx=field("stickCX").getFloat(voyage),cy=field("stickCY").getFloat(voyage),r=field("stickR").getFloat(voyage);
                for(int heading:new int[]{0,90,180,270}) for(float speed:new float[]{0,.1f,30}) for(int dir:new int[]{-1,1}) {
                    sea();state.headingDeg=heading;state.speed=speed;
                    downStage(cx+dir*r*.7f,cy,0);voyage.render(.04f);
                    float turn=((state.headingDeg-heading+540)%360)-180;
                    require(turn*dir<0,"helm sign heading="+heading+" speed="+speed+" dir="+dir);
                    up(0,0,0);
                }
                sea();state.dockedPort=Catalog.YANGZHOU;overlay("NONE");
                downStage(cx-r*.7f,cy,0);voyage.render(.04f);
                require(state.dockedPort<0 && state.headingDeg>0,"left helm undocks and yaws left");up(0,0,0);
                sea();overlay("NONE");state.startAutoSail(0);voyage.render(0);
                down(620,400,1);drag(880,240,1);voyage.render(.04f);
                require(renderer.lookYaw()<-20 && Math.abs(renderer.lookPitch())>10,"drag orbits");
                require(state.autoSail && state.steerInput==0,"look preserves autopilot");
                snap("look-sky");
                downStage(cx-r*.7f,cy,2);voyage.render(.04f);
                require(state.steerInput<0 && !state.autoSail && field("lookPointer").getInt(voyage)==1,"separate look and helm pointers");
                up(0,0,3);require(field("lookPointer").getInt(voyage)==1,"unrelated release does not cancel look");
                up(0,0,2);float before=Math.abs(renderer.lookYaw());up(880,240,1);voyage.render(.10f);
                require(Math.abs(renderer.lookYaw())>1 && Math.abs(renderer.lookYaw())<before,"smooth return");
                for(int i=0;i<15;i++)voyage.render(1f/60);
                require(renderer.lookYaw()==0 && renderer.lookPitch()==0,"return within .35 seconds");
                down(620,400,1);drag(3000,4000,1);voyage.render(.02f);
                require(renderer.camera.position.y>=9 && Float.isFinite(renderer.camera.direction.y),"pitch remains above sea");
                snap("look-water");up(3000,4000,1);
                // A drag starting on acceleration stays with that HUD control.
                overlay("NONE");voyage.render(0);Vector2 point=point(actor("加速"));
                down((int)point.x,(int)point.y,4);drag(620,400,4);voyage.render(.02f);
                require(field("lookPointer").getInt(voyage)<0 && state.holdAccel,"HUD drag ownership");
                up(620,400,4);voyage.pause();
                require(!state.holdAccel && field("lookPointer").getInt(voyage)<0,"pause releases input");
            }
            private void sceneSnapshots() throws Exception {
                for(int location:new int[]{20,7,14,15,19,23,27,31}) {
                    sea();boolean port=location<Catalog.PORTS.length;int id=port?location:location-Catalog.PORTS.length;
                    float x=port?Catalog.PORT_X[id]:Catalog.ISLAND_X[id],y=port?Catalog.PORT_Y[id]:Catalog.ISLAND_Y[id];
                    state.x=x+VoyageGeometry.landRadius(port,id)+VoyageGeometry.ship(state.ship).radius()+20;
                    state.y=y;state.headingDeg=180;overlay("NONE");
                    for(int i=0;i<75;i++)renderer.render(state,1f/60);
                    voyage.render(0);snap("scene-"+location);
                    renderer.beginLook();renderer.dragLook(-.28f,0);renderer.render(state,0);
                    snap("scene-side-"+location);renderer.resetLook();
                }
            }
            private void profile() throws Exception {
                sea();overlay("NONE");voyage.render(0);tap("船长");voyage.render(0);
                require(overlayName().equals("AVATAR"),"HUD opens captain");
                TextField name=stage.getRoot().findActor("昵称");require(name!=null,"editable nickname");
                name.setText("晴岚船长");
                tap("头像4");voyage.render(0);
                require(state.nickname.equals("晴岚船长") && state.avatarIndex==3,"profile edits applied");
                SaveData saved=accounts.load(currentUser);
                require(saved.nickname.equals(state.nickname) && saved.avatarIndex==3,"profile saved immediately");
                Image hud=stage.getRoot().findActor("船长头像");
                require(hud.getDrawable()==IconLib.avatar(3),"HUD portrait updates live");
                for(int i=0;i<4;i++) {
                    Texture t=IconLib.avatar(i).getRegion().getTexture();
                    require(t.isManaged(),"avatar context restoration");
                    t.load(t.getTextureData());
                }
                snap("captain-profile");tap("存档");voyage.render(0);
                state.silver=4321;tap("保存进度");state.silver=0;tap("读取存档");voyage.render(0);
                require(state.silver==4321 && state.avatarIndex==3 && state.nickname.equals("晴岚船长"),"local reload preserves profile");
            }
            private void redeemAndHud() throws Exception {
                sea();overlay("NONE");voyage.render(0);
                tap("福利");voyage.render(0);require(overlayName().equals("REDEEM"),"welfare opens redeem page");
                TextField code=(TextField)actor("兑换码");
                int silver=state.silver;
                code.setText("bad");tap("确认兑换");require(state.silver==silver,"invalid redeem UI");
                code.setText(" 666 ");tap("确认兑换");require(state.silver==silver+666,"redeem 666 UI");
                tap("确认兑换");require(state.silver==silver+666,"repeat 666 UI");
                SaveData saved=accounts.load(currentUser);
                require(saved.silver==silver+666 && saved.redeemedCodes==1 && saved.worldVersion==3,"redeem persisted to actual account file");
                code.setText("888");tap("确认兑换");require(state.silver==silver+1554,"redeem 888 UI");
                saved=accounts.load(currentUser);
                GameState restored=GameState.fromSave(saved);
                require(!restored.redeemCode("888") && !restored.redeemCode("666"),"codes cannot repeat after disk reload");
                voyage.render(0);snap("redeem-success");tap("关闭");
                state.toastT=0;voyage.render(0);
                require(!actor("航行消息").isVisible(),"no idle blessing or empty footer frame");
                for(int i=0;i<2;i++) {
                    Actor title=actor("任务标题"+i);
                    require(title.getX()>=28,"quest title clears left corner decoration");
                }
                snap("hud-no-blessing");
                state.toast("兑换检查完成");voyage.render(0);
                require(actor("航行消息").isVisible(),"action feedback still visible");
            }
            private void dailyAndShop() throws Exception {
                sea();overlay("NONE");voyage.render(0);
                require(find(stage.getRoot(),"船坞")==null && find(stage.getRoot(),"商行")==null,"duplicate shop shortcuts removed");
                int silver=state.silver;
                tap("活动");voyage.render(0);require(overlayName().equals("DAILY"),"activity opens daily page");
                snap("daily-available");tap("领取每日奖励");voyage.render(0);
                require(state.silver==silver+200 && ((TextButton)actor("领取每日奖励")).isDisabled(),"daily claimed UI");
                tap("领取每日奖励");require(state.silver==silver+200,"disabled daily claim cannot repeat");
                SaveData saved=accounts.load(currentUser);
                require(saved.dailyClaimDay==state.gameDay && saved.silver==state.silver,"daily reward persisted to disk");
                snap("daily-claimed");tap("关闭");state.gameDay++;
                tap("活动");voyage.render(0);require(!((TextButton)actor("领取每日奖励")).isDisabled(),"next day claim becomes available");
                tap("领取每日奖励");require(state.silver==silver+400,"next day UI reward");tap("关闭");
                state.silver=100000;tap("商城");voyage.render(0);require(overlayName().equals("SHOP"),"single shop shortcut");
                snap("merged-shop");
                for(String category:new String[]{"战船","货船","特种船","船只"}) {tap("商城分类"+category);voyage.render(0);}
                for(int id:new int[]{1,3,7}) {
                    tap("商城"+Catalog.SHIPS[id]);voyage.render(0);
                    int before=state.silver;tap("购买 · "+Catalog.SHIP_PRICE[id]+" 两");voyage.render(0);
                    require(state.ship==id && state.ownsShip(id) && state.silver==before-Catalog.SHIP_PRICE[id],"shop purchase "+id);
                    tap("返回列表");voyage.render(0);
                }
                tap("商城分类我的船只");voyage.render(0);require(overlayName().equals("MINE"),"shop links to owned ships");
                tap("关闭");
            }
            private void page(String name) throws Exception {
                sea();state.speed=30;state.autoSail=true;state.autoSailPort=0;
                if(name.equals("FISH"))state.dockedPort=Catalog.YANGZHOU;
                if(name.equals("FAIL")){state.failed=true;state.failReason="船沉";}
                if(name.equals("PRICE"))field("selectedGood").setInt(voyage,0);
                if(name.equals("QUESTS"))field("selectedQuest").setInt(voyage,0);
                overlay(name);
                float x=state.x,y=state.y,clock=state.dayMin,weather=state.weatherTimer,pirates=state.pirateSpawnTimer,supply=state.supply;
                voyage.render(.1f);voyage.render(.1f);
                Actor full=stage.getRoot().findActor("fullscreenPage");
                require(full!=null && Math.abs(full.getWidth()-stage.getWidth())<.1 && Math.abs(full.getHeight()-stage.getHeight())<.1,"stage-filling "+name);
                require(!((Actor)field("voyageHud").get(voyage)).isVisible(),"HUD hidden "+name);
                require(state.x==x && state.y==y && state.dayMin==clock && state.weatherTimer==weather && state.pirateSpawnTimer==pirates && state.supply==supply,"world pause "+name);
                Actor close=find(full,"关闭");require(close!=null,"page close "+name);
                Vector2 closePoint=point(close);
                require(closePoint.x>=0 && closePoint.x<Gdx.graphics.getWidth() && closePoint.y>=0 && closePoint.y<Gdx.graphics.getHeight(),"close on screen "+name);
                snap(name.toLowerCase()+"-"+Gdx.graphics.getWidth());
                tapActor(close);require(!overlayName().equals(name),"close works "+name);
                voyage.render(0);
            }
            private void interactions() throws Exception {
                sea();state.x=Catalog.PORT_X[Catalog.YANGZHOU]+Catalog.DOCK_RANGE-8;state.y=Catalog.PORT_Y[Catalog.YANGZHOU];
                state.dock(Catalog.YANGZHOU);overlay("PORT");voyage.render(0);
                require(stage.getRoot().findActor("fullscreenPage")==null,"port retains panel");
                snap("port");float x=state.x,y=state.y;
                overlay("MARKET");voyage.render(0);require(stage.getRoot().findActor("fullscreenPage")==null,"market retains panel");snap("market");
                overlay("PORT");tap("关闭");require(state.x==x&&state.y==y&&state.dockedPort<0,"close port in place");
                state.x=Catalog.ISLAND_X[0]+Catalog.ISLAND_RANGE-5;state.y=Catalog.ISLAND_Y[0];state.enterIsland(0);
                overlay("ISLAND");voyage.render(0);require(stage.getRoot().findActor("fullscreenPage")==null,"island retains panel");
                x=state.x;y=state.y;snap("island");tap("关闭");require(state.x==x&&state.y==y&&state.islandMenu<0,"close island in place");
            }
            private Actor find(Actor actor,String text) {
                if(text.equals(actor.getName()) || actor instanceof TextButton && text.equals(((TextButton)actor).getText().toString()))return actor;
                if(actor instanceof Group)for(Actor child:((Group)actor).getChildren()){Actor found=find(child,text);if(found!=null)return found;}
                return null;
            }
            private Actor actor(String name) {
                Actor a=find(stage.getRoot(),name);require(a!=null,"actor "+name);return a;
            }
            private Vector2 point(Actor a) {return stage.stageToScreenCoordinates(a.localToStageCoordinates(new Vector2(a.getWidth()/2,a.getHeight()/2)));}
            private void tap(String name){tapActor(actor(name));}
            private void tapActor(Actor a) {
                stage.act(0); Vector2 p=point(a);down((int)p.x,(int)p.y,0);up((int)p.x,(int)p.y,0);
            }
            private void downStage(float x,float y,int pointer){Vector2 p=stage.stageToScreenCoordinates(new Vector2(x,y));down((int)p.x,(int)p.y,pointer);}
            private void down(int x,int y,int p){Gdx.input.getInputProcessor().touchDown(x,y,p,Input.Buttons.LEFT);}
            private void drag(int x,int y,int p){Gdx.input.getInputProcessor().touchDragged(x,y,p);}
            private void up(int x,int y,int p){Gdx.input.getInputProcessor().touchUp(x,y,p,Input.Buttons.LEFT);}
            private Field field(String name)throws Exception{Field f=VoyageScreen.class.getDeclaredField(name);f.setAccessible(true);return f;}
            private String overlayName()throws Exception{return field("overlay").get(voyage).toString();}
            private void overlay(String name)throws Exception{
                Field f=field("overlay");for(Object v:f.getType().getEnumConstants())if(v.toString().equals(name))f.set(voyage,v);
                Method m=VoyageScreen.class.getDeclaredMethod("rebuildMenu");m.setAccessible(true);m.invoke(voyage);
            }
            private void snap(String name) {
                Pixmap p=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
                PixmapIO.writePNG(Gdx.files.local("../Builds/feel2811-"+name+".png"),p,-1,true);p.dispose();
            }
            private void fail(Throwable t){result=1;t.printStackTrace();Gdx.app.exit();}
        },config);
        System.exit(result);
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
