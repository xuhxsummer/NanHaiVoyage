package com.shipgame.nanhai.lwjgl3;

import com.badlogic.gdx.math.MathUtils;
import com.shipgame.nanhai.data.*;
import com.shipgame.nanhai.ui.ContextualTips;
import com.shipgame.nanhai.ui.ContextualTips.Tip;
import java.lang.reflect.Method;

public final class CombatAiRegression {
    private static Method pirate, warship;
    public static void main(String[] args)throws Exception {
        pirate=method("updateCombat",float.class);warship=method("updateWarship",WarshipData.class,float.class);
        float pirateMean=0,warshipMean=0;int cases=0;
        for(boolean war:new boolean[]{false,true}) for(float angle:new float[]{0,90,180,270}) for(float start:new float[]{130,460,950}) {
            GameState g=sea();float cx=MathUtils.cosDeg(angle),cy=MathUtils.sinDeg(angle);
            WarshipData w=enemy(g,war,g.x+cx*start,g.y+cy*start);
            float side=0,mean=0;int shots=0;
            for(int i=0;i<600;i++) {
                tick(g,w,.05f);float ex=ex(g,w),ey=ey(g,w),d=Catalog.dist(g.x,g.y,ex,ey);
                require(d>100,"no bow glue/ram seek");
                if(i<20)side=Math.max(side,Math.abs((ex-g.x)*cy-(ey-g.y)*cx));
                if(i>=400) { require(d>Catalog.PIRATE_RANGE*.35f && d<Catalog.PIRATE_RANGE*.70f,"held cannon band: "+war+" "+d); mean+=d/200; }
                shots+=g.ballCount;g.ballCount=0;
            }
            require(side>15,"flank component instead of straight seek");require(shots>5,"moving broadside fire");
            if(war)warshipMean+=mean/12;else pirateMean+=mean/12;cases++;
        }
        require(warshipMean>pirateMean+80,"pirates hold closer than warships");
        for(boolean war:new boolean[]{false,true}) {
            GameState g=sea();WarshipData w=enemy(g,war,g.x+430,g.y);
            if(war){w.hp=w.hpMax*.5f;w.heading=180;}else{g.pirateHp=g.pirateHpMax*.5f;g.pirateHeading=180;}
            float start=Catalog.dist(g.x,g.y,ex(g,w),ey(g,w));
            for(int i=0;i<20;i++)tick(g,w,.05f);
            require(Catalog.dist(g.x,g.y,ex(g,w),ey(g,w))>start+90,"exactly half HP withdraws");
            float heading=war?w.heading:g.pirateHeading;
            require(MathUtils.cosDeg(heading)>.9f,"fleeing hull faces away while firing");require(g.ballCount>0,"returns fire while withdrawing");
            if(war)w.hp=w.hpMax;else g.pirateHp=g.pirateHpMax;
            float before=Catalog.dist(g.x,g.y,ex(g,w),ey(g,w));
            for(int i=0;i<40;i++)tick(g,w,.05f);
            require(Catalog.dist(g.x,g.y,ex(g,w),ey(g,w))<(war?610:before),"healing restores flank band");
        }
        // Chase a moving player through repeated heading reversals, including at the horizon edge.
        for(boolean war:new boolean[]{false,true}) {
            GameState moving=sea(); WarshipData w=enemy(moving,war,moving.x+950,moving.y);
            moving.speed=150;
            for(int i=0;i<160;i++) {
                moving.headingDeg=(i/40)%2==0?180:0;
                moving.x+=MathUtils.cosDeg(moving.headingDeg)*moving.speed*.05f;
                tick(moving,w,.05f);
                require(war?w.hostile:moving.combatLock && moving.pirateAlive,"turning cannot clear lock or stop chase");
            }
            require(Catalog.dist(moving.x,moving.y,ex(moving,w),ey(moving,w))<700,"moving target closes into firing range");
        }
        GameState g=sea();enemy(g,false,g.x+550,g.y);tick(g,null,.1f);g.cancelLock();
        float px=g.pirateX,py=g.pirateY;tick(g,null,.5f);require(Catalog.dist(px,py,g.pirateX,g.pirateY)>30,"unlock grace continues maneuver");
        tick(g,null,3f);px=g.pirateX;py=g.pirateY;tick(g,null,.2f);require(px==g.pirateX && py==g.pirateY,"grace ends in idle");
        for(boolean war:new boolean[]{false,true}) {
            g=sea();int type=war?4:VoyageGeometry.PIRATE_SHIP;
            float margin=VoyageGeometry.ship(type).radius()+25;
            g.x=margin+220;g.y=margin+220;
            WarshipData w=enemy(g,war,margin,margin);
            if(war)w.hp=w.hpMax*.5f;else g.pirateHp=g.pirateHpMax*.5f;
            for(int i=0;i<100;i++) {
                tick(g,w,.05f);
                require(ex(g,w)>=VoyageGeometry.ship(type).radius()+12 && ey(g,w)>=VoyageGeometry.ship(type).radius()+12,"withdraw respects map boundary");
            }
            require(Catalog.dist(margin,margin,ex(g,w),ey(g,w))>100,"open-water retreat escapes corner without mirror jitter");
            g=sea();float land=VoyageGeometry.landRadius(true,0);
            px=Catalog.PORT_X[0]-land-VoyageGeometry.ship(type).radius()-50;py=Catalog.PORT_Y[0];g.x=px-250;g.y=py;
            w=enemy(g,war,px,py);if(war)w.hp=w.hpMax*.5f;else g.pirateHp=g.pirateHpMax*.5f;
            for(int i=0;i<100;i++) {
                tick(g,w,.05f);
                require(Catalog.dist(ex(g,w),ey(g,w),Catalog.PORT_X[0],Catalog.PORT_Y[0])>land+VoyageGeometry.ship(type).radius(),"withdraw avoids port hitbox");
            }
            require(Catalog.dist(px,py,ex(g,w),ey(g,w))>100,"withdraw makes progress around shore");
        }
        // Traders remain fleeing shooters. Ordinary merchant route logic is untouched.
        g=sea();TraderData t=new TraderData();t.ship=0;t.x=g.x+300;t.y=g.y;t.hp=t.hpMax=50;t.hostile=true;t.targetPort=0;
        method("updateTrader",TraderData.class,float.class).invoke(g,t,.5f);
        require(t.x>g.x+330 && g.ballCount>0,"trader flees and returns fire, never chases");
        // Mutual separation and opportunity ram remain live, but a withdrawing warship never charges.
        g=sea();WarshipData w=enemy(g,true,g.x+10,g.y);float oldX=g.x;
        method("separateLiveShips").invoke(g);require(g.x<oldX && w.x>oldX+10,"both hulls pushed apart");
        g=sea();w=enemy(g,true,g.x+Catalog.PIRATE_RANGE*.4f,g.y);w.hp=w.hpMax*.5f;float hull=g.hull;
        for(int i=0;i<100;i++)method("updateShipImpacts",float.class).invoke(g,2f);
        require(g.hull==hull,"half-HP retreat suppresses optional charge");
        tips();
        System.out.println("COMBAT AI PASS: "+cases+" flank/range cases, moving fire, closer pirates, half-HP withdrawal/heal, corners/shores, unlock grace, trader flee, mutual separation and tips cycle contracts");
    }
    private static void tips() {
        ContextualTips tips=new ContextualTips();
        for(Tip tip:Tip.values()) {
            tips.update(1,tip.bit());require(tips.active()==tip,"first relevant context shows "+tip);
            tips.dismiss();tips.update(1,tip.bit());require(tips.active()==null,"same context never repeats "+tip);
        }
        tips=new ContextualTips();tips.update(0,Tip.PIRATE_LOCK.bit());tips.update(30,Tip.PIRATE_LOCK.bit());require(tips.active()==Tip.PIRATE_LOCK,"new login rearms");
        tips.complete(Tip.PIRATE_LOCK);require(tips.active()==null,"learned action dismisses");
        tips.update(1,Tip.PIRATE_LOCK.bit());require(tips.active()==null,"second pirate does not rearm");
        tips.update(1,Tip.QUEST_CARD.bit());tips.update(.1f,0);require(tips.active()==null,"hidden control hides its prompt");
    }
    private static GameState sea(){GameState g=GameState.newGame();g.undockInPlace();g.x=2000;g.y=12000;return g;}
    private static WarshipData enemy(GameState g,boolean war,float x,float y){
        if(!war){g.pirateAlive=true;g.combatLock=true;g.pirateX=x;g.pirateY=y;g.pirateHp=g.pirateHpMax=48;g.pirateDamage=5;return null;}
        WarshipData w=new WarshipData();w.ship=4;w.x=x;w.y=y;w.hp=w.hpMax=100;w.provoked=true;g.warships[0]=w;return w;
    }
    private static void tick(GameState g,WarshipData w,float dt)throws Exception{if(w==null)pirate.invoke(g,dt);else warship.invoke(g,w,dt);}
    private static float ex(GameState g,WarshipData w){return w==null?g.pirateX:w.x;}
    private static float ey(GameState g,WarshipData w){return w==null?g.pirateY:w.y;}
    private static Method method(String name,Class<?>...types)throws Exception{Method m=GameState.class.getDeclaredMethod(name,types);m.setAccessible(true);return m;}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
