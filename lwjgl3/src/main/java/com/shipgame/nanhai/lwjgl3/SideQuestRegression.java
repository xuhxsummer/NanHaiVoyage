package com.shipgame.nanhai.lwjgl3;

import com.badlogic.gdx.utils.Json;
import com.shipgame.nanhai.data.*;
import com.shipgame.nanhai.screen.VoyageScreen;
import java.lang.reflect.*;

public final class SideQuestRegression {
    public static void main(String[] args)throws Exception {
        VoyageScreen screen=new VoyageScreen(null);GameState g=GameState.newGame();
        Object[] quests=(Object[])field(VoyageScreen.class,"QUESTS").get(null);Class<?> def=quests[0].getClass();
        Method claim=method("claimQuest",GameState.class,def),claimed=method("isQuestClaimed",GameState.class,def),active=method("getActiveQuestIndex");
        require(quests.length==31,"twelve appended side quests");
        g.questDialogueSeen=123;field(VoyageScreen.class,"g").set(screen,g);
        for(int i=19;i<quests.length;i++) {
            Object q=quests[i];int id=field(def,"id").getInt(q),type=field(def,"progressType").getInt(q),target=field(def,"targetAmount").getInt(q);
            require(id==i && field(def,"unlockAfter").getInt(q)==-1,"independent optional track");
            String[][] lines=(String[][])field(def,"dialogue").get(q);require(lines.length>=2 && lines.length<=4,"authored side dialogue");
            for(String[] line:lines)require(line.length==2 && !line[0].isEmpty() && line[1].length()>8 && line[1].length()<70,"readable named lines");
            int silver=g.silver;claim.invoke(screen,g,q);require(g.silver==silver && !(boolean)claimed.invoke(screen,g,q),"incomplete guard");
            if(type>=200)g.questGoodsSold[type-200]=target;
            else if(type>=100)g.questGoodsBought[type-100]=target;
            else switch(type){case 18:g.fishCaughtTotal=target;break;case 1:g.questVisitPorts=target;break;case 8:g.questIslandVisits=target;break;case 2:g.questDefeatedPirates=target;break;case 17:g.questBuyTea=target;break;case 5:g.questSilverPeak=target;break;case 15:g.questBeastsFound=target;break;default:throw new AssertionError("uncovered side counter");}
            int reward=field(def,"silverReward").getInt(q);claim.invoke(screen,g,q);claim.invoke(screen,g,q);
            require(g.silver==silver+reward && (boolean)claimed.invoke(screen,g,q),"single exact reward");
            SaveData save=g.toSave();g=GameState.fromSave(new Json().fromJson(SaveData.class,new Json().toJson(save)));field(VoyageScreen.class,"g").set(screen,g);
            require((boolean)claimed.invoke(screen,g,q) && g.questDialogueSeen==123 && (int)active.invoke(screen)==0,"saved side claim never changes main tracking/playback");
        }
        require(g.sideQuestClaims==4095,"all twelve independent bits");
        SaveData old=new Json().fromJson(SaveData.class,"{silver:1000}");g=GameState.fromSave(old);
        require(g.sideQuestClaims==0 && g.questGoodsBought.length==Catalog.GOODS.length && g.merchant==null,"old save defaults");
        g=GameState.newGame();g.silver=10000;
        g.buyGood(0,3,12);require(g.questGoodsBought[3]==12 && g.questGoodsBought[5]==0,"actual per-good buy counter");
        g.buyGood(0,3,9999);require(g.questGoodsBought[3]==12,"failed purchase adds no progress");
        g.sellGood(0,3,12);g.sellGood(0,3,1);require(g.questGoodsSold[3]==12,"successful sale only");
        g.trade[3]=2;g.dumpTrade(3,2);require(g.questGoodsSold[3]==12,"jettison is not a sale");
        System.out.println("SIDE QUEST PASS: 12 authored quests, independent claim bits, exact/duplicate rewards, JSON old/new saves, unchanged main HUD/seen flags, real trade counters");
    }
    private static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    private static Method method(String name,Class<?>...types)throws Exception{Method m=VoyageScreen.class.getDeclaredMethod(name,types);m.setAccessible(true);return m;}
    private static void require(boolean ok,String text){if(!ok)throw new AssertionError(text);}
}
