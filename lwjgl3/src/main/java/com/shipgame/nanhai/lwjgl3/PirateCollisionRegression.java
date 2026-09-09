package com.shipgame.nanhai.lwjgl3;

import com.shipgame.nanhai.data.*;
import java.lang.reflect.Method;

/** Swept ship contact, independent of graphics/accounts and real-time frame rate. */
public final class PirateCollisionRegression {
    public static void main(String[] args) throws Exception {
        Method move=GameState.class.getDeclaredMethod("move",float.class);move.setAccessible(true);
        Method combat=GameState.class.getDeclaredMethod("updateCombat",float.class);combat.setAccessible(true);
        int cases=0;
        for(int ship=0;ship<Catalog.SHIPS.length;ship++) for(int angle=0;angle<360;angle+=15) {
            double a=Math.toRadians(angle);float cx=(float)Math.cos(a),cy=(float)Math.sin(a);
            for(float dt:new float[]{1f/60,.1f,1f,10f}) {
                GameState g=sea(ship);g.pirateX=g.x+cx*300;g.pirateY=g.y+cy*300;g.headingDeg=angle;g.speed=1000;
                move.invoke(g,dt);separated(g,"player high-speed approach");
                require((g.pirateX-g.x)*cx+(g.pirateY-g.y)*cy>0,"player tunneled across pirate");
                g=sea(ship);g.pirateX=g.x+cx*300;g.pirateY=g.y+cy*300;
                combat.invoke(g,dt);separated(g,"pirate long-frame pursuit");
                require((g.pirateX-g.x)*cx+(g.pirateY-g.y)*cy>0,"pirate crossed the player");
                cases+=2;
            }
            GameState g=sea(ship);g.pirateX=g.x;g.pirateY=g.y;g.ensurePirateSeparation();separated(g,"initial overlap");
            g=sea(ship);g.pirateX=g.x+180;g.pirateY=g.y+18;g.headingDeg=0;g.speed=150;
            for(int frame=0;frame<180;frame++) {move.invoke(g,1f/30);combat.invoke(g,1f/30);separated(g,"sliding/moving pursuit");}
        }
        for(int port=0;port<Catalog.PORTS.length;port++) {
            GameState g=sea(8);g.x=Catalog.PORT_X[port]+VoyageGeometry.landRadius(true,port)+VoyageGeometry.ship(8).radius()+1;g.y=Catalog.PORT_Y[port];
            g.pirateX=g.x;g.pirateY=g.y;g.ensurePirateSeparation();separated(g,"shore recovery");
            require(Catalog.dist(g.pirateX,g.pirateY,Catalog.PORT_X[port],Catalog.PORT_Y[port])>=VoyageGeometry.landRadius(true,port)+VoyageGeometry.ship(VoyageGeometry.PIRATE_SHIP).radius(),"recovery placed pirate in land");
        }
        for(float x:new float[]{40,Catalog.WORLD_W-40}) for(float y:new float[]{40,Catalog.WORLD_H-40}) {
            GameState g=sea(8);g.x=g.pirateX=x;g.y=g.pirateY=y;g.ensurePirateSeparation();separated(g,"world corner recovery");
            require(g.pirateX>=0 && g.pirateX<=Catalog.WORLD_W && g.pirateY>=0 && g.pirateY<=Catalog.WORLD_H,"pirate outside world");
            g.speed=1000;g.headingDeg=225;move.invoke(g,2f);separated(g,"boundary movement");
            require(g.x>=40 && g.x<=Catalog.WORLD_W-40 && g.y>=40 && g.y<=Catalog.WORLD_H-40,"player collision escaped world boundary");
        }
        System.out.println("PIRATE COLLISION PASS: "+cases+" swept approaches, every ship/heading, long-frame chase, sliding, initial overlap, shores and world corners");
    }
    private static GameState sea(int ship) {
        GameState g=GameState.newGame();g.undockInPlace();g.ship=ship;g.x=2000;g.y=12000;
        g.pirateAlive=g.pirateChase=true;g.pirateHp=g.pirateHpMax=100000;
        g.playerFireCd=g.pirateFireCd=100000;return g;
    }
    private static void separated(GameState g,String context) {require(Catalog.dist(g.x,g.y,g.pirateX,g.pirateY)>=VoyageGeometry.pirateSeparation(g.ship)-.01f,context);}
    private static void require(boolean ok,String text) {if(!ok)throw new AssertionError(text);}
}
