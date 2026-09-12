package com.shipgame.nanhai.lwjgl3;

import com.shipgame.nanhai.data.*;
import java.lang.reflect.*;

public final class CombatFeedbackRegression {
    public static void main(String[] args)throws Exception {
        GameState g=sea();g.pirateAlive=true;g.pirateX=g.x+5;g.pirateY=g.y;g.pirateHp=g.pirateHpMax=48;
        method("updateShipImpacts",float.class).invoke(g,.01f);
        require(g.hurtTime[0]>0 && g.hurtTime[1]>0 && g.rockTime[0]>0 && g.rockTime[1]>0,"both ram hulls rock and flash");
        require(g.impactSplashes[0]!=null && g.impactSplashes[0].remaining>0,"local contact splash");
        g.updateFeedback(.08f);require(Math.abs(g.hitRock(0))>1 && g.hitFlash(0)>0,"short rock pulse");
        g.updateFeedback(.4f);require(g.hitRock(0)==0 && g.hitFlash(0)==0 && g.impactSplashes[0].remaining==0,"feedback expires");
        g=sea();g.pirateAlive=true;g.pirateX=g.x+300;g.pirateY=g.y;g.pirateHp=1;g.pirateHpMax=48;
        method("fireAt",boolean.class,int.class,int.class,float.class,float.class,float.class,float.class).invoke(g,true,1,1,g.x,g.y,g.pirateX,g.pirateY);
        method("updateBalls",float.class).invoke(g,1f);
        require(!g.pirateAlive && g.sinkFocus!=null && g.sinkFocus.pirate,"pirate death makes actual pirate wreck");
        require(g.pollLootPopup()==null,"loot never shares first sink frame");
        g.updateFeedback(.8f);require(g.sinkFocus.tilt>30 && g.sinkFocus.submerge>0 && g.pollLootPopup()==null,"sink starts before reward card");
        g.updateFeedback(.4f);require(g.pollLootPopup()!=null && g.sinkFocus.alive,"reward appears after sink beat settles");
        g.updateFeedback(1.1f);require(!g.sinkFocus.alive,"sink removes at 2.2 seconds");
        g=sea();WarshipData w=new WarshipData();w.ship=8;w.hp=1;w.hpMax=100;w.x=g.x+30;w.y=g.y;g.warships[0]=w;
        method("updateShipImpacts",float.class).invoke(g,.01f);
        require(g.warships[0]==null && g.sinkFocus!=null && g.sinkFocus.ship==8,"ram-sunk warship gets correct wreck instead of disappearing");
        require(g.pollLootPopup()==null,"ram sink grants no player loot");
        g=sea();g.ship=4;g.hull=0;g.fail("船沉");
        require(g.failed && g.playerSinking() && g.sinkFocus.player && g.sinkFocus.ship==4,"player follows same sink pipeline");
        g.update(.5f);require(g.playerSinkRemaining>2,"failed simulation cannot consume animation accidentally");
        g.updateFeedback(1);require(g.playerSinking() && g.sinkFocus.alive,"presentation clock works while failed");
        g.updateFeedback(1.3f);require(!g.playerSinking() && !g.sinkFocus.alive,"player sink finishes before fail UI eligibility");
        System.out.println("COMBAT FEEDBACK PASS: two-sided rock/flash, local splash, expiry, typed wreck, 2.2s sink, delayed loot, warship ram sink, player failed presentation clock");
    }
    private static GameState sea(){GameState g=GameState.newGame();g.undockInPlace();g.x=2000;g.y=12000;return g;}
    private static Method method(String name,Class<?>...types)throws Exception{Method m=GameState.class.getDeclaredMethod(name,types);m.setAccessible(true);return m;}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
