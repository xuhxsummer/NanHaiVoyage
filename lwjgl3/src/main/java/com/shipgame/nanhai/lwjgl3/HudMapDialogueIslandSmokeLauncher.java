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
import com.shipgame.nanhai.screen.VoyageScreen;
import com.shipgame.nanhai.ui.*;
import java.lang.reflect.*;
import java.nio.file.Path;

/** Real-input, isolated-account checks for each of the thirteen 0.28.27 requirements. */
public final class HudMapDialogueIslandSmokeLauncher {
    private static int result=1;
    public static void main(String[] args)throws Exception {
        Path dir=java.nio.file.Files.createTempDirectory("nanhai-2827-");
        Path output=Path.of("../Builds/ui2827").toAbsolutePath();
        Lwjgl3ApplicationConfiguration config=new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(1280,720);config.disableAudio(true);config.setForegroundFPS(0);
        config.setPreferencesConfig(dir.resolve("prefs").toString(),Files.FileType.Absolute);
        new Lwjgl3Application(new NanHaiVoyage(){
            VoyageScreen voyage;Stage stage;VoyageHud hud;int frame;boolean failed;
            @Override public void create(){
                Files original=Gdx.files;
                Gdx.files=(Files)Proxy.newProxyInstance(Files.class.getClassLoader(),new Class[]{Files.class},(p,m,a)->{
                    if(m.getName().equals("local"))return new FileHandle(dir.resolve((String)a[0]).toFile());
                    return m.invoke(original,a);
                });
                try {
                    super.create();accounts.register("hud-test","local-password");currentUser="hud-test";
                    state=GameState.newGame();state.questDialogueSeen=(1<<19)-1;state.undockInPlace();
                    state.x=2000;state.y=12000;state.silver=4321;
                    state.pirateSpawnTimer=state.merchantSpawnTimer=state.traderSpawnTimer=state.warshipSpawnTimer=10000;
                    voyage=new VoyageScreen(this);setScreen(voyage);stage=(Stage)get(voyage,"stage");hud=(VoyageHud)get(voyage,"voyageHud");
                    ContextualTips tips=(ContextualTips)get(voyage,"contextualTips");for(ContextualTips.Tip t:ContextualTips.Tip.values())tips.complete(t);
                }catch(Throwable t){fail(t);}
            }
            @Override public void render(){
                if(failed)return;
                try {
                    if(frame++==0){checks("1280");Gdx.graphics.setWindowedMode(1600,720);}
                    else {voyage.resize(1600,720);checks("1600");result=0;
                        System.out.println("HUD MAP DIALOGUE ISLAND PASS: all thirteen items at 1280/1600, real intel/map/autosail taps, measured title, silver retained in port, text-only auto, narrow centered tap dialogue, centered parchment notices, explore/anchor/resume and port actions; weather removal, ordinal day/night clock and rollover, status cleanup, live lower-right telemetry");Gdx.app.exit();}
                }catch(Throwable t){fail(t);}
            }
            private void checks(String width)throws Exception {
                invoke("closePopup");state.anchored=false;state.x=2000;state.y=12000;state.speed=0;
                for(int i=0;i<20;i++)voyage.render(.05f);
                // 1. Only one intel badge, now at the bottom-left corner of the minimap.
                Actor intel=actor("情报"),map=actor("小地图");
                require(intel.getX()==map.getX() && intel.getY()<map.getY()+30,"1 intel at minimap bottom-left");
                require(stage.getRoot().findActor("世界")==null,"1 old corner icon removed");
                tap("情报");voyage.render(0);require(overlay().equals("INTEL"),"1 intel opens actual panel");invoke("closePopup");voyage.render(0);
                // 2. Plate width follows actual title/subtitle font metrics, including hit exclusion.
                tap("小地图");voyage.render(0);require(overlay().equals("MAP"),"2 minimap still opens full map");
                WorldMapOverlay world=(WorldMapOverlay)get(voyage,"worldMap");Rectangle title=(Rectangle)get(world,"TITLE");
                require(title.width<450 && title.width>280,"2 compact measured plate: "+title.width);
                require(world.hit((title.x+title.width-2)*stage.getWidth()/1920,(title.y+5)*stage.getHeight()/1080,stage.getWidth(),stage.getHeight())==WorldMapOverlay.EMPTY,"2 title remains inert");
                capture(width+"-map");invoke("closePopup");voyage.render(0);
                // 3. Remove only the silver HUD cell; live values and port silver remain.
                require(hud.findActor("银两")==null && hud.stats[0]==null,"3 silver removed from main resource row");
                require(hud.stats[1].getText().toString().equals(""+(int)state.supply) && hud.stats[2].getText().toString().equals(""+(int)state.hull),"3 retained stats still refresh");
                // 4. Automatic sailing has a single text child and its map action still works.
                TextButton auto=(TextButton)actor("自动航行");require(auto.getChildren().size==1 && auto.getChildren().first() instanceof Label,"4 auto text only");
                tap("自动航行");voyage.render(0);require(overlay().equals("MAP"),"4 auto action opens map");invoke("closePopup");voyage.render(0);
                // 5. Centered readable column, no continue caption; tap still advances NPC -> player.
                tap("任务卡");voyage.render(0);Actor panel=actor("dialoguePanel");
                Vector2 origin=panel.localToStageCoordinates(new Vector2());
                require(panel.getWidth()<=940 && panel.getWidth()<stage.getWidth()*.8f && Math.abs(origin.x+panel.getWidth()/2-stage.getWidth()/2)<1,"5 centered narrower dialogue");
                require(((Label)actor("dialogueHint")).getText().length==0,"5 no continue caption");
                tap("dialogueBody");voyage.render(0);require(((Label)actor("dialogueSpeaker")).getText().toString().equals("老掌柜"),"5 tap advances once");
                require(((Label)actor("dialogueBody")).getPrefHeight()<actor("dialoguePaper").getHeight(),"5 wrapped text fits paper");capture(width+"-dialogue-npc");
                tap("dialogueBody");voyage.render(0);require(actor("dialogueBustPlayer").isVisible() && !actor("dialogueBustNpc").isVisible(),"5 active player side retained");capture(width+"-dialogue-player");
                tap("dialogueBody");voyage.render(0);require(actor("dialogueAction").isVisible(),"5 objective action retained");tap("dialogueClose");voyage.render(0);
                // 6. Discovery announcements share parchment + red quest-card header, centered even when wide.
                state.toast("远处发现战船，交战海域注意规避。");voyage.render(0);
                Table notice=hud.toastBar;require(notice.getBackground()==hud.ui.parchment && ((Table)actor("航行消息标题")).getBackground()==hud.ui.red,"6 quest card chrome");
                require(Math.abs(notice.getX()+notice.getWidth()/2-hud.getWidth()/2)<1 && notice.getY()>750,"6 centered in upper band");capture(width+"-warship-notice");
                // 7. Island exploration and anchor controls form one column; original explore flow remains.
                state.x=Catalog.ISLAND_X[0]+150;state.y=Catalog.ISLAND_Y[0];state.headingDeg=0;state.anchored=true;
                for(int i=0;i<30;i++)voyage.render(.05f);
                Actor explore=actor("探索"),anchor=actor("抛锚");
                require(explore.isVisible() && anchor.isVisible() && explore.getX()==anchor.getX() && Math.abs(explore.getY()-anchor.getY()-anchor.getHeight()-8)<1,"7 island controls stacked beside ship");
                require(allText(explore).trim().equals("探索"),"7 floating panel has no island name/subtitle");
                require(((TextButton)anchor).getText().toString().equals("起锚"),"7 anchored boat offers weigh anchor");capture(width+"-island-actions");
                tap("探索");voyage.render(0);require(overlay().equals("ISLAND") && state.islandMenu==0,"7 explore opens original island search menu");capture(width+"-island-menu");
                invoke("closePopup");voyage.render(0);tap("抛锚");voyage.render(0);require(!state.anchored,"7 weigh anchor resumes sailing");
                tap("抛锚");voyage.render(0);require(state.anchored,"7 drop anchor still works when sailing");
                // Port actions retain their existing labels and silver resource display.
                state.anchored=false;state.x=Catalog.PORT_X[0]+190;state.y=Catalog.PORT_Y[0];
                for(int i=0;i<30;i++)voyage.render(.05f);
                tap("所在地");voyage.render(0);require(overlay().equals("PORT"),"3/7 port flow retained");
                require(allText(stage.getRoot()).contains("银两 "+state.silver),"3 port still shows silver");capture(width+"-port-silver");invoke("closePopup");voyage.render(0);
                for(int card=0;card<2;card++) {
                    Table quest=(Table)actor(card==0?"任务卡":"下一程任务卡"),header=(Table)actor("任务标题栏"+card);
                    require(header.getBackground()==(card==0?hud.ui.questRedHeader:hud.ui.questBlueHeader),"12 undecorated header background");
                    Image left=(Image)actor("questMarkerLeft"+card),right=(Image)actor("questMarkerRight"+card);
                    require(header.getChildren().size==3 && left.getParent()==header && right.getParent()==header && left.getDrawable()==hud.ui.questMarker && right.getDrawable()==hud.ui.questMarker,"12 exactly two explicit header triangles");
                    require(left.getX()<actor("任务标题"+card).getX() && right.getX()>actor("任务标题"+card).getX()+actor("任务标题"+card).getWidth(),"12 markers flank title");
                    assertNoCornerMarkers(quest.getBackground());assertNoCornerMarkers(header.getBackground());
                    com.badlogic.gdx.graphics.g2d.NinePatch cardPatch=((com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable)quest.getBackground()).getPatch();
                    com.badlogic.gdx.graphics.g2d.NinePatch headPatch=((com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable)header.getBackground()).getPatch();
                    require(cardPatch.getLeftWidth()==headPatch.getLeftWidth() && cardPatch.getRightWidth()==headPatch.getRightWidth(),"13 matching background edge insets");
                    require(header.getX()==0 && header.getWidth()==quest.getWidth(),"13 quest header flush with body");
                    require(quest.getBackground()==(card==0?hud.ui.questPaper:hud.ui.questPanel),"12 undecorated quest body");
                }
                // 8–11. Weather belongs in the ordinal clock; navigation telemetry has its own safe corner.
                require(!allText(hud).contains("今日："),"8 weather plate and caption removed");
                state.gameDay=1;state.dayMin=981;state.speed=87;state.headingDeg=state.windDeg;
                voyage.render(0);
                require(hud.clock.getText().toString().equals("第一天 16:21 白天 晴"),"9 exact ordinal/time/day/weather format");
                Label telemetry=(Label)actor("风向航速文字");
                require(telemetry.getText().toString().equals("顺风 速87"),"11 live following wind and speed");
                require(!hud.status.getText().toString().matches(".*(欠|舱|顺风|逆风|侧风|速[0-9]).*"),"10/11 top status cleared of debt/cargo/wind/speed");
                Actor sailing=actor("航向航速");
                require(sailing.getX()>hud.getWidth()*.7f && sailing.getY()+sailing.getHeight()<hud.questGroup.getY() && sailing.getX()+sailing.getWidth()<=hud.getWidth(),"11 lower-right safe bounds below quests");
                require(telemetry.getPrefWidth()<=telemetry.getWidth(),"11 telemetry fits without clipping");
                capture(width+"-clock-wind-day");
                state.headingDeg=state.windDeg+180;state.speed=12;state.dayMin=1080;voyage.render(0);
                require(telemetry.getText().toString().equals("逆风 速12") && hud.clock.getText().toString().endsWith("18:00 夜晚 晴"),"9/11 night and opposing wind refresh");
                capture(width+"-clock-wind-night");
                int[] days={2,10,11,20,21,101,1001,1010,10000,10001,10010,100000001,Integer.MAX_VALUE};
                String[] ordinals={"二","十","十一","二十","二十一","一百零一","一千零一","一千零一十","一万","一万零一","一万零一十","一亿零一","二十一亿四千七百四十八万三千六百四十七"};
                for(int i=0;i<days.length;i++) {
                    state.gameDay=days[i];voyage.render(0);
                    require(hud.clock.getText().toString().startsWith("第"+ordinals[i]+"天 "),"9 Chinese ordinal "+days[i]);
                    require(hud.clock.getPrefWidth()<=hud.clock.getWidth(),"9 long day count stays readable "+days[i]);
                }
                state.gameDay=9;state.dayMin=1439.99f;
                Method advance=GameState.class.getDeclaredMethod("advanceClock",float.class);advance.setAccessible(true);advance.invoke(state,.1f);voyage.render(0);
                require(state.gameDay==10 && hud.clock.getText().toString().startsWith("第十天 00:00 夜晚 晴"),"9 midnight rollover");
                state.gameDay=1;state.dayMin=360;state.speed=0;
                System.out.println("Verified all thirteen items at "+width);
            }
            private void assertNoCornerMarkers(com.badlogic.gdx.scenes.scene2d.utils.Drawable background) {
                com.badlogic.gdx.graphics.glutils.FrameBuffer target=new com.badlogic.gdx.graphics.glutils.FrameBuffer(Pixmap.Format.RGBA8888,96,96,false);
                com.badlogic.gdx.math.Matrix4 previous=new com.badlogic.gdx.math.Matrix4(batch.getProjectionMatrix());
                target.begin();Gdx.gl.glClearColor(0,0,0,0);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                batch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0,0,96,96));batch.begin();background.draw(batch,0,0,96,96);batch.end();
                Pixmap pixels=Pixmap.createFromFrameBuffer(0,0,96,96);int white=0;
                for(int y=0;y<96;y++)for(int x=0;x<96;x++) { int rgba=pixels.getPixel(x,y);if((rgba>>>24)>230 && ((rgba>>>16)&255)>208 && ((rgba>>>8)&255)>158)white++; }
                pixels.dispose();target.end();target.dispose();batch.setProjectionMatrix(previous);
                require(white==0,"12 background renders without white corner triangles");
            }
            private String overlay()throws Exception{return get(voyage,"overlay").toString();}
            private Object get(Object owner,String name)throws Exception{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
            private void invoke(String name)throws Exception{Method m=VoyageScreen.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(voyage);}
            private Actor actor(String name){Actor a=stage.getRoot().findActor(name);require(a!=null,"actor "+name);return a;}
            private String allText(Actor a){String s=a instanceof Label?((Label)a).getText().toString():"";if(a instanceof Group)for(Actor c:((Group)a).getChildren())s+=allText(c);return s;}
            private void tap(String name){Actor a=actor(name);Vector2 p=a.localToStageCoordinates(new Vector2(a.getWidth()/2,a.getHeight()/2));stage.stageToScreenCoordinates(p);InputProcessor input=Gdx.input.getInputProcessor();input.touchDown(Math.round(p.x),Math.round(p.y),0,0);input.touchUp(Math.round(p.x),Math.round(p.y),0,0);}
            private void capture(String name){require(Gdx.gl.glGetError()==GL20.GL_NO_ERROR,"GL "+name);Pixmap p=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());PixmapIO.writePNG(new FileHandle(output.resolve(name+".png").toFile()),p,-1,true);p.dispose();}
            private void fail(Throwable t){t.printStackTrace();failed=true;Gdx.app.exit();}
        },config);System.exit(result);
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
