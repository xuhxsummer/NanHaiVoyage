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

/** Isolated real GL/input/login lifecycle coverage of the 0.28.26 UI changes and inherited combat feedback. */
public final class AiTipsFeedbackSmokeLauncher {
    private static int result=1;
    public static void main(String[] args)throws Exception {
        Path dir=java.nio.file.Files.createTempDirectory("nanhai-2825-");
        Path output=Path.of("../Builds/ui2826").toAbsolutePath();
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
                    if(frame++==0){tips();uiChanges();feedback();Gdx.graphics.setWindowedMode(1600,720);}
                    else {
                        voyage.resize(1600,720);voyage.render(0);
                        require(tip("在港口"),"new login rearms port tip on wide screen");capture("wide-login-tip");
                        require(actor("contextualTip").getWidth()<=1600 && actor("contextualTip").getX()>=0,"tip stays on screen");
                        tap("contextualTipDismiss");invoke("closePopup");voyage.render(0);
                        if(stage.getRoot().findActor("questDialogue")==null) { tap("任务卡");voyage.render(0); }
                        require(actor("dialoguePanel").getWidth()>1400 && actor("dialogueBustNpc").isVisible() && !actor("dialogueBustPlayer").isVisible(),"wide dialogue keeps single bust above frame");capture("wide-dialogue");
                        result=0;System.out.println("UI 0.28.26 GL PASS: modal pause and input isolation, single-side busts above text with all avatars, activity/welfare merge, intel badge, stacked cancel lock, top marquee; five contextual tips, real actions/dismiss, no repeat, login rearm, no HOWTO auto-wall, NPC rendering/ram flash/splash, typed enemy sink and camera, delayed loot, player sink-before-fail, close-restart, 1280/1600");Gdx.app.exit();
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
                tap("contextualTipDismiss");tap("补补给");voyage.render(0);require(!actor("contextualTip").isVisible() && state.questRefillCount==1,"successful supply dismisses port tip");
                tap("离港");state.x=2000;state.y=12000;state.headingDeg=0;voyage.render(.6f);
                require(tip("摇杆"),"first idle voyage shows stick tip");capture("stick-tip");
                float cx=field("stickCX").getFloat(voyage),cy=field("stickCY").getFloat(voyage);
                Vector2 point=stage.stageToScreenCoordinates(new Vector2(cx,cy+55));
                InputProcessor input=Gdx.input.getInputProcessor();input.touchDown((int)point.x,(int)point.y,0,0);
                float pausedX=state.x, pausedTime=state.dayMin;
                voyage.render(12f);
                require(tip("摇杆") && state.x==pausedX && state.dayMin==pausedTime && state.thrustInput==0,"modal pauses world, blocks helm, and never times out");
                input.touchUp((int)point.x,(int)point.y,0,0); tap("contextualTipDismiss");
                input.touchDown((int)point.x,(int)point.y,0,0);
                voyage.render(.1f);require(!actor("contextualTip").isVisible() && state.thrustInput>0,"helm resumes after acknowledgement");
                input.touchUp((int)point.x,(int)point.y,0,0);voyage.render(.6f);
                require(tip("任务卡"),"quest card prompt when visible");tap("contextualTipDismiss");tap("任务卡");voyage.render(0);
                require(field("overlay").get(voyage).toString().equals("DIALOGUE") && !actor("contextualTip").isVisible(),"card opens story and dismisses tip");
                tap("dialogueClose");voyage.render(.6f);
                state.x=Catalog.PORT_X[0]+190;state.y=Catalog.PORT_Y[0];state.speed=0;voyage.render(0);
                require(tip("抛锚"),"first reachable port enables dock tip");capture("dock-tip");tap("contextualTipDismiss");tap("抛锚");voyage.render(0);
                require(state.anchored && !actor("contextualTip").isVisible(),"anchor completes contextual action");
                state.anchored=false;state.x=2000;state.y=12000;
                pirate(500,60);voyage.render(.6f);require(tip("点海盗"),"first pirate prompt");capture("pirate-tip");
                float minute=state.dayMin, enemyX=state.pirateX, enemyY=state.pirateY;
                voyage.render(2f); require(state.dayMin==minute && state.pirateX==enemyX && state.pirateY==enemyY,"pirate tip pauses combat and time");
                tap("contextualTipDismiss");voyage.render(.1f);
                require(state.dayMin>minute && !actor("contextualTip").isVisible(),"dismiss does not pause or lock combat");
                state.clearPirate();pirate(500,60);voyage.render(1f);
                require(!actor("contextualTip").isVisible(),"second pirate cannot repeat same-cycle prompt");
                state.clearPirate();quiet();
            }
            private void uiChanges()throws Exception {
                require(stage.getRoot().findActor("福利")==null,"welfare rail removed");
                require(stage.getRoot().findActor("我的船只")==null && stage.getRoot().findActor("港口")==null,"obsolete text links removed");
                require(actor("情报") instanceof Table && actor("rail:情报")!=null,"intel circular badge with label");
                Actor auto=actor("自动航行"),cancel=actor("取消锁定");
                require(auto.getX()==cancel.getX() && Math.abs(cancel.getY()-auto.getY()-auto.getHeight()-8)<1,"cancel lock stacked above autopilot");
                Actor notice=actor("航行消息"); require(notice.getY()>800,"status announcements near top resources");
                tap("活动");voyage.render(0);
                require(actor("兑换码") instanceof TextField && actor("领取每日奖励")!=null,"activities contains daily and redeem together");capture("activities-welfare");
                int silver=state.silver; tap("领取每日奖励");voyage.render(0);
                require(state.silver==silver+Catalog.DAILY_LOGIN_SILVER,"daily reward still pays in merged page");
                ((TextField)actor("兑换码")).setText("666");
                ScrollPane activityScroll=(ScrollPane)actor("pageScroll");
                activityScroll.setScrollPercentY(1);activityScroll.updateVisualScroll();voyage.render(0);
                tap("确认兑换");voyage.render(0);
                require(state.silver==silver+Catalog.DAILY_LOGIN_SILVER+666,"redeem action reachable and pays in merged page");
                tap("确认兑换");voyage.render(0);
                require(state.silver==silver+Catalog.DAILY_LOGIN_SILVER+666,"duplicate redeem cannot pay twice"); capture("activities-redeem");
                invoke("closePopup");voyage.render(0);
                tap("情报");voyage.render(0);require(field("overlay").get(voyage).toString().equals("INTEL"),"intel badge opens market intelligence");invoke("closePopup");
                pirate(500,0);voyage.render(0);tap("锁定海盗");voyage.render(.1f);
                require(state.combatLock,"lock button still reachable");capture("stacked-cancel-lock");
                tap("取消锁定");voyage.render(0);require(!state.combatLock,"stacked cancel lock works");state.clearPirate();
                for(int avatar=0;avatar<4;avatar++) {
                    state.avatarIndex=avatar;voyage.render(0);tap("任务卡");voyage.render(0);
                    Actor paper=actor("dialoguePaper"),left=actor("dialogueBustNpc"),right=actor("dialogueBustPlayer");
                    require(left.isVisible() && !right.isVisible(),"NPC only left bust");
                    Vector2 a=left.localToStageCoordinates(new Vector2()),b=paper.localToStageCoordinates(new Vector2(0,paper.getHeight()));
                    require(a.y>=b.y,"bust sits above text, never in wrapping");
                    tap("dialogueBody");voyage.render(0);capture("npc-bust-"+avatar);
                    tap("dialogueBody");voyage.render(0);
                    require(!left.isVisible() && right.isVisible() && ((Label)actor("dialogueSpeaker")).getText().toString().equals("我"),"player only right bust");
                    Image portrait=(Image)((Table)right).getChildren().first();
                    require(portrait.getDrawable()==com.shipgame.nanhai.ui.IconLib.bust("player_"+avatar),"captain identity matches bust "+avatar);
                    capture("player-bust-"+avatar);tap("dialogueClose");voyage.render(0);
                }
                state.toast("已锁定海盗，船只正在追击目标。撞击！船体受损，请注意敌舰动向。长消息在顶部自动滚动，完整显示航行情况与战斗反馈。");
                voyage.render(0);
                ScrollPane marquee=(ScrollPane)actor("航行消息轮播");
                voyage.render(2.5f);require(marquee.getScrollX()>0,"overlong announcement scrolls horizontally");capture("top-marquee");
                voyage.render(marquee.getMaxX()/70f+.1f); // loop remains bounded, never leaks over the rail.
                require(marquee.getScrollX()<=marquee.getMaxX(),"marquee clipped to bar");
                state.toast("继续航行");voyage.render(0);require(marquee.getScrollX()==0,"new short message resets scroll");
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
