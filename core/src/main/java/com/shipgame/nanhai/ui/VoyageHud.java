package com.shipgame.nanhai.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.*;
import com.shipgame.nanhai.audio.VoyageAudio;
import com.shipgame.nanhai.data.*;
import java.util.function.*;

/** Main voyage HUD in 1920x1080 design pixels, scaled into the existing 1280x720 stage. */
public final class VoyageHud extends Group implements Disposable {
    public final VoyageHudChrome ui;
    public final Label[] stats=new Label[4];
    public final Label status,clock;
    public final TextButton accel,decel,auto,cancelAuto,lock,cancelLock,anchor;
    private final Label coords,toast,place,placeSub;
    private final Table location,toastBar,questGroup;
    private final Table[] questCards=new Table[2];
    private final Array<RotatingGoldBorder> questBorders=new Array<>();
    private final Label[] questTitles=new Label[2],questBodies=new Label[2],questProgress=new Label[2];
    private final Image knob,dot,speedNeedle;
    private final VoyageMinimap minimap;
    private final Image captainPortrait;
    private int shownAvatar = -1;
    private final Array<float[]> anchors = new Array<>();
    public VoyageHud(Skin skin,Runnable captain,IntConsumer stat,Runnable[] shortcuts,Runnable world,
                     Runnable mine,Runnable intel,Runnable port,Runnable autoAction,Runnable cancel,Runnable lockAction,Runnable unlock,Runnable anchorAction,IntConsumer quest){
        setSize(1920,1080);setScale(2f/3f);setTouchable(Touchable.childrenOnly);
        ui=new VoyageHudChrome(skin);
        Table resources=panel(ui.panel,136,968,664,88);
        String[] icons={"silver","supply","ship","crew"},names={"银两","粮草","船体耐久","船员"};
        resources.pad(8,16,8,8);
        for(int i=0;i<4;i++){final int idx=i;Table cell=new Table();cell.setName(names[i]);cell.add(icon(icons[i])).size(40).padRight(4);
            Table copy=new Table();stats[i]=ui.label("0",28,VoyageHudChrome.PAPER);copy.add(stats[i]).left().row();copy.add(ui.label(names[i],20,VoyageHudChrome.GOLD)).left();cell.add(copy);cell.addListener(click(()->stat.accept(idx)));resources.add(cell).width(158).height(64);}
        Table badge=new Table();badge.setName("船长");
        captainPortrait=new Image(IconLib.avatar(0));captainPortrait.setName("船长头像");
        badge.add(captainPortrait).size(112);badge.addListener(click(captain));
        badge.setBounds(24,936,128,128);addActor(badge);
        status=ui.label("",21,VoyageHudChrome.PAPER);status.setEllipsis(true);status.setBounds(168,928,640,32);addActor(status);
        textLink("我的船只",mine,168,880,136);textLink("港口",port,312,880,80);textLink("情报",intel,400,880,80);
        String[] rail={"货舱","图鉴","商城","任务","活动","福利"};String[] glyph={"cargo","book","anchor","quest","cargo","gift"};
        Image questDot=null;
        for(int i=0;i<rail.length;i++){Table item=badge(rail[i],glyph[i],80,shortcuts[i]);item.setBounds(1008+i*104,952,80,108);addActor(item);
            if(i==3){questDot=new Image(ui.redCircle);questDot.setBounds(1068+i*104,1032,20,20);questDot.setTouchable(Touchable.disabled);addActor(questDot);}}
        dot=questDot;
        minimap=new VoyageMinimap(ui);minimap.setName("小地图");minimap.setBounds(1632,784,264,264);minimap.addListener(click(world));addActor(minimap);
        Label north=ui.label("北",24,VoyageHudChrome.PAPER);north.setBounds(1748,1008,32,32);north.setTouchable(Touchable.disabled);addActor(north);
        coords=ui.label("",17,VoyageHudChrome.PAPER);coords.setAlignment(Align.center);coords.setBounds(1664,816,192,24);coords.setTouchable(Touchable.disabled);addActor(coords);
        Table worldButton=badge("世界","helm",56,world);worldButton.setBounds(1824,772,72,84);addActor(worldButton);
        Table weather=panel(ui.panel,1664,712,232,48);weather.pad(4);weather.add(icon("sun")).size(36);weather.add(ui.label("今日：晴",24,VoyageHudChrome.PAPER));
        questGroup=new Table();questGroup.setTouchable(Touchable.childrenOnly);questGroup.setBounds(1576,388,320,288);addActor(questGroup);
        for(int i=0;i<2;i++){final int n=i;Table card=new Table();card.setName(i==0?"任务卡":"下一程任务卡");card.setBackground(i==0?ui.parchment:ui.panel);card.pad(8);
            Table head=new Table();head.setBackground(i==0?ui.red:ui.blue);head.pad(4,28,4,24);questTitles[i]=ui.label("",22,VoyageHudChrome.PAPER);questTitles[i].setName("任务标题"+i);questTitles[i].setEllipsis(true);head.add(questTitles[i]).growX();
            card.add(head).size(304,36).row();questBodies[i]=ui.label("",21,i==0?Color.valueOf("3F3326"):VoyageHudChrome.PAPER);questBodies[i].setWrap(true);card.add(questBodies[i]).width(288).height(56).left().row();
            questProgress[i]=ui.label("",20,i==0?Color.valueOf("4D3D28"):VoyageHudChrome.GOLD);card.add(questProgress[i]).width(288).height(28).left();card.addListener(click(()->quest.accept(n)));questCards[i]=card;questGroup.add(card).size(320,136).padBottom(i==0?16:0).row();
            // Preserve FreeBuff's clockwise gold sweep without consuming card taps.
            RotatingGoldBorder border=new RotatingGoldBorder();
            border.setBounds(0,0,320,136);
            card.addActor(border);questBorders.add(border);
        }
        accel=speed("加速","up",ui.greenCircle,1656,248);decel=speed("减速","down",ui.redCircle,1656,104);
        Image speedTrack=new Image(ui.goldLine);speedTrack.setBounds(1820,120,2,232);speedTrack.setTouchable(Touchable.disabled);addActor(speedTrack);
        speedNeedle=new Image(ui.goldLine);speedNeedle.setBounds(1804,120,32,8);speedNeedle.setTouchable(Touchable.disabled);addActor(speedNeedle);
        // Compass/rudder base and a ship medallion that follows the existing steering vector.
        Image stick=new Image(ui.compass);stick.setTouchable(Touchable.disabled);stick.setBounds(120,80,304,304);addActor(stick);
        knob=new Image(ui.circle);knob.setTouchable(Touchable.disabled);knob.setBounds(212,172,120,120);addActor(knob);
        Image shipKnob=icon("ship");shipKnob.setName("摇杆船徽");shipKnob.setTouchable(Touchable.disabled);shipKnob.setBounds(236,196,72,72);addActor(shipKnob);
        auto=ui.button("自动航行",ui.panel,22);auto.setName("自动航行");auto.setBounds(472,96,168,64);Label autoLabel=auto.getLabel();auto.clearChildren();auto.add(icon("helm")).size(40).padRight(8);auto.add(autoLabel);auto.addListener(click(autoAction));addActor(auto);
        Table time=panel(ui.panel,24,24,360,48);time.pad(4,12,4,12);time.add(icon("sun")).size(32).padRight(8);clock=ui.label("",22,VoyageHudChrome.PAPER);time.add(clock).growX();
        toastBar=panel(ui.panel,600,24,896,56);toastBar.setName("航行消息");toastBar.pad(8,24,8,24);toastBar.add(icon("anchor")).size(32).padRight(12);toast=ui.label("",22,VoyageHudChrome.PAPER);toast.setWrap(true);toastBar.add(toast).growX();toastBar.setTouchable(Touchable.disabled);
        location=panel(ui.panel,600,696,264,80);location.setTouchable(Touchable.enabled);location.pad(8);place=ui.label("",28,VoyageHudChrome.PAPER);placeSub=ui.label("",20,VoyageHudChrome.GOLD);location.add(place).row();location.add(placeSub);location.addListener(click(port));location.setName("所在地");
        cancelAuto=utility("取消自动",cancel,648);lock=utility("锁定海盗",lockAction,824);cancelLock=utility("取消锁定",unlock,1000);
        // 0.28.17 抛锚：仅港/岛范围内显示，标签在 抛锚/起锚 间切换。
        anchor=utility("抛锚",anchorAction,1176);
        for(Actor actor:getChildren()) anchors.add(new float[]{actor.getX(),actor.getY()});
    }
    /** Extend the sea-facing space while anchoring controls to the safe edges. */
    public void layoutViewport(float width,float height) {
        float scale=Math.min(width/1920f,height/1080f);
        setScale(scale);setSize(width/scale,height/scale);
        float extraX=getWidth()-1920,extraY=getHeight()-1080;
        for(int i=0;i<getChildren().size;i++) {
            Actor actor=getChildren().get(i);float[] origin=anchors.get(i);
            float dx=origin[0]>=904?extraX:origin[0]>=600?extraX/2:0;
            float dy=origin[1]>=712?extraY:origin[1]>=388?extraY/2:0;
            actor.setPosition(origin[0]+dx,origin[1]+dy);
        }
    }
    private Table panel(Drawable bg,float x,float y,float w,float h){Table t=new Table();t.setBackground(bg);t.setBounds(x,y,w,h);t.setTouchable(Touchable.childrenOnly);addActor(t);return t;}
    private Image icon(String name){Image i=new Image(ui.icon(name));i.setScaling(Scaling.fit);return i;}
    private Table badge(String title,String symbol,float size,Runnable action){Table t=new Table();t.setName(title);Table disc=new Table();disc.setBackground(ui.circle);disc.add(icon(symbol)).size(size*.68f);t.add(disc).size(size).row();if(!title.equals("船长"))t.add(ui.label(title,24,VoyageHudChrome.PAPER)).height(28);t.addListener(click(action));return t;}
    private void textLink(String text,Runnable action,float x,float y,float w){TextButton b=ui.button(text,ui.panel,20);b.setName(text);b.setBounds(x,y,w,40);b.addListener(click(action));addActor(b);}
    private TextButton speed(String text,String symbol,Drawable bg,float x,float y){TextButton b=ui.button(text,bg,28);TextButton.TextButtonStyle style=new TextButton.TextButtonStyle(b.getStyle());style.down=ui.circle;b.setStyle(style);Label l=b.getLabel();b.clearChildren();b.pad(12);b.add(icon(symbol)).size(56).row();b.add(l).height(32);b.setName(text);b.setBounds(x,y,120,120);addActor(b);return b;}
    private TextButton utility(String text,Runnable action,float x){TextButton b=ui.button(text,ui.panel,20);b.setName(text);b.setBounds(x,856,168,48);b.addListener(click(action));addActor(b);return b;}
    private static ClickListener click(Runnable r){return new ClickListener(){@Override public void clicked(InputEvent e,float x,float y){
        e.stop();
        // 0.28.18: UI tap SFX on HUD buttons (not on holds/hover).
        VoyageAudio audio=VoyageAudio.get();if(audio!=null)audio.playUi();
        r.run();}};}
    public void quest(int card,String title,String body,String progress){questTitles[card].setText(title);questBodies[card].setText(body);questProgress[card].setText(progress);}
    public void update(GameState g,boolean neutral,boolean ready,float kx,float ky){
        if(shownAvatar!=g.avatarIndex){shownAvatar=g.avatarIndex;captainPortrait.setDrawable(IconLib.avatar(shownAvatar));}
        speedNeedle.setY(120+com.badlogic.gdx.math.MathUtils.clamp(g.speed/Catalog.MAX_SPEED,0,1)*216);
        minimap.update(g);coords.setText("X:"+(int)g.x+"  Y:"+(int)g.y);questGroup.setVisible(neutral);dot.setVisible(ready);
        toastBar.setVisible(neutral && g.toastT>0 && !g.toast.isEmpty());toast.setText(g.toastT>0?g.toast:"");toastBar.setHeight(g.toast.length()>38&&g.toastT>0?80:56);
        knob.setPosition(212+kx*88,172+ky*88);Actor ship=findActor("摇杆船徽");ship.setPosition(236+kx*88,196+ky*88);
        int p=g.dockedPort>=0?g.dockedPort:g.nearestPortInRange();int isle=g.islandMenu>=0?g.islandMenu:g.nearestIslandInRange();
        location.setVisible(neutral&&(p>=0||isle>=0));
        if(p>=0){place.setText(Catalog.PORTS[p]+"港");placeSub.setText(g.dockedPort>=0?"已停泊 · 点击经营":"点击停靠 · 港口经营");}
        else if(isle>=0){place.setText(Catalog.ISLANDS[isle]);placeSub.setText("点击登岛 · 搜采奇珍");}
    }
    @Override public void dispose(){for(RotatingGoldBorder border:questBorders)border.dispose();questBorders.clear();minimap.dispose();ui.dispose();}
}
