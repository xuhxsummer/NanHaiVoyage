package com.shipgame.nanhai.lwjgl3;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Json;
import com.shipgame.nanhai.data.*;
import java.lang.reflect.Method;

/** Deterministic contract checks: distance, lifetime damage, independent AI and kill attribution. */
public final class SeaTrafficRegression {
    public static void main(String[] args) throws Exception {
        MathUtils.random.setSeed(2816);
        Method spawn=method("spawnPirate"), combat=method("updateCombat",float.class), balls=method("updateBalls",float.class);
        Method spawnMerchant=method("spawnMerchant"), ai=method("updateMerchant",float.class);
        Method fire=method("fireAt",boolean.class,int.class,int.class,float.class,float.class,float.class,float.class);
        boolean[] damageSeen=new boolean[11];
        for(int i=0;i<400;i++) {
            GameState g=sea();
            if(i%4==0) {g.x=80;g.y=80;}
            g.startAutoSail(0); spawn.invoke(g);
            require(g.pirateAlive && g.autoSail && !g.combatLock,"spawn keeps route, no forced focus");
            float d=Catalog.dist(g.x,g.y,g.pirateX,g.pirateY);
            require(d>=799.99f && d<=1000.01f && d>Catalog.PIRATE_RANGE,"spawn band even at edges");
            require(g.pirateDamage>=1 && g.pirateDamage<=10,"integer cannon roll");damageSeen[g.pirateDamage]=true;
            combat.invoke(g,.1f);require(g.ballCount==0,"spawn outside fire range");
            int damage=g.pirateDamage;
            float px=g.pirateX,py=g.pirateY;
            g.x=px-650;g.y=py;g.pirateFireCd=0;g.hull=10000;
            combat.invoke(g,.01f); require(g.ballCount==1,"Aggro B fires without lock");
            balls.invoke(g,1f);require(g.hull==10000-damage,"rolled ship damage reaches player");
            for(int n=0;n<4;n++){combat.invoke(g,1f);balls.invoke(g,1f);}
            require(g.pirateDamage==damage && g.pirateX==px && g.pirateY==py,"no chase and no per-shot reroll");
            g.x=px-Catalog.NPC_HORIZON; combat.invoke(g,0f);require(g.pirateAlive,"on horizon remains");
            g.x-=1;combat.invoke(g,0f);require(!g.pirateAlive && g.autoSail,"only sailing beyond horizon clears");
        }
        for(int i=1;i<=10;i++)require(damageSeen[i],"full 1–10 roll domain");
        GameState g=sea();spawnMerchant.invoke(g); MerchantData original=g.merchant;
        require(original!=null && g.merchantVisible(),"visible roster merchant spawn");
        for(int ship=0;ship<Catalog.SHIPS.length;ship++) {
            require(GameState.merchantHull(ship)>=28 && GameState.merchantDamage(ship)==1+Catalog.SHIP_FIRE[ship]/10,"roster scaled stats");
        }
        g.x=10000;g.y=10000;float mx=original.x,my=original.y;
        for(int i=0;i<200;i++)ai.invoke(g,.1f);
        require(g.merchant==original && (mx!=original.x || my!=original.y) && !g.merchantVisible(),"off-horizon simulates same ship");
        spawnMerchant.invoke(g); require(g.merchant==original,"global cap includes distant ship");
        int initialSilver=original.silver;
        for(int i=0;i<2000;i++)ai.invoke(g,.5f);
        require(original.silver>initialSilver,"AI really reaches ports and trades without teleporting");
        g.x=original.x-200;g.y=original.y;require(g.merchantVisible(),"can re-encounter trader");
        g.ballCount=0;original.rest=100;ai.invoke(g,1f);require(g.ballCount==0,"peaceful merchant ignores player");
        g.lockMerchant();require(g.merchantLock && !g.combatLock && original.hostile,"lock begins plunder");
        ai.invoke(g,1f);require(g.ballCount>0,"merchant returns fire on lock");
        g.cancelLock();require(!g.merchantLock && original.hostile,"dropping focus does not pacify victim");
        SaveData save=g.toSave();g=GameState.fromSave(new Json().fromJson(SaveData.class,new Json().toJson(save)));
        require(g.merchant!=null && g.merchant.x==original.x && g.merchant.hostile,"merchant route and hostility persisted");
        g.merchant.silver=80;require(save.merchant.silver!=80,"save is a detached snapshot");
        // A completed port visit earns only small bounded trade gains, including long-running loops.
        g=sea();spawnMerchant.invoke(g);
        for(int i=0;i<100;i++) {
            g.merchant.targetPort=0;g.merchant.targetIsland=-1;g.merchant.rest=0;
            g.merchant.x=Catalog.PORT_X[0]+180;g.merchant.y=Catalog.PORT_Y[0];
            ai.invoke(g,0f);
        }
        require(g.merchant.silver==90 && g.merchant.cargo==3,"trade wealth/cargo caps");
        for(boolean player:new boolean[]{false,true}) {
            g=encounter();int silver=g.silver,goods=g.cargoUsed();
            g.merchant.hp=1;int reward=g.merchant.silver,cargo=g.merchant.cargo;
            fire.invoke(g,player,2,1,g.x,g.y,g.merchant.x,g.merchant.y);balls.invoke(g,1f);
            require(g.merchant==null && g.silver==silver+(player?reward:0) && g.cargoUsed()==goods+(player?cargo:0),"merchant last-hit loot attribution");
            require(g.merchantSpawnTimer>=120 && g.questDefeatedPirates==0,"low respawn and merchant is not pirate quest credit");
            g=encounter();silver=g.silver;g.pirateHp=1;
            fire.invoke(g,player,1,1,g.x,g.y,g.pirateX,g.pirateY);balls.invoke(g,1f);
            require(!g.pirateAlive && g.questDefeatedPirates==(player?1:0),"pirate last-hit quest attribution");
            require(player?g.silver>silver:g.silver==silver,"pirate NPC sink pays nothing");
        }
        g=encounter();g.merchant.hp=1;g.merchant.cargo=3;g.trade[0]=g.holdCap();int used=g.cargoUsed();
        fire.invoke(g,true,2,1,g.x,g.y,g.merchant.x,g.merchant.y);balls.invoke(g,1f);
        require(g.cargoUsed()==used,"auto loot honors full cargo hold");
        // Pirate and trader target each other when the player sits beyond cannon range.
        g=encounter();g.pirateX=g.x+900;g.pirateY=g.y;g.merchant.x=g.x+850;g.merchant.y=g.y+150;
        g.merchant.rest=10;g.pirateFireCd=g.merchant.fireCd=0;
        float php=g.pirateHp,mhp=g.merchant.hp,hp=g.hull;
        combat.invoke(g,.1f);ai.invoke(g,.1f);balls.invoke(g,1f);
        require(g.pirateHp<php && g.merchant.hp<mhp && g.hull==hp,"NPCs fight while player watches");
        g=encounter();pxCheck(g);
        // A prior generation's shot must never hit a replacement entity.
        g=encounter();g.pirateHp=1;
        fire.invoke(g,true,1,1,g.x,g.y,g.pirateX,g.pirateY);
        float px=g.pirateX,py=g.pirateY;g.clearPirate();spawn.invoke(g);g.pirateX=px;g.pirateY=py;
        balls.invoke(g,1f);require(g.pirateHp==g.pirateHpMax,"stale projectile cannot hit respawn");
        System.out.println("SEA TRAFFIC PASS: 400 band/edge rolls, stationary Aggro B, life damage, horizon, roster stats, persistent capped merchant AI, plunder retaliation, NPC combat, last-hit loot, full hold, stale shots");
    }
    private static void pxCheck(GameState g) {
        float px=g.pirateX,py=g.pirateY;g.pirateDamage=9;g.pirateHp=17;
        g.dock(0);require(g.pirateAlive && g.pirateX==px && g.pirateY==py,"docking pauses rather than despawns pirate");
        g=GameState.fromSave(new Json().fromJson(SaveData.class,new Json().toJson(g.toSave())));
        require(g.pirateAlive && g.pirateHp==17 && g.pirateDamage==9 && g.pirateX==px,"pirate life roll/position/health survive reload");
    }
    private static GameState sea(){GameState g=GameState.newGame();g.undockInPlace();g.x=2000;g.y=12000;g.pirateSpawnTimer=10000;g.merchantSpawnTimer=10000;return g;}
    private static GameState encounter() throws Exception {
        GameState g=sea();method("spawnPirate").invoke(g);method("spawnMerchant").invoke(g);
        g.pirateX=g.x+400;g.pirateY=g.y;g.merchant.x=g.x+300;g.merchant.y=g.y+200;
        return g;
    }
    private static Method method(String name,Class<?>...types)throws Exception{Method m=GameState.class.getDeclaredMethod(name,types);m.setAccessible(true);return m;}
    private static void require(boolean ok,String text){if(!ok)throw new AssertionError(text);}
}
