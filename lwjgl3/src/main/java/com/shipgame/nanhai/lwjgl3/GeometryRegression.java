package com.shipgame.nanhai.lwjgl3;

import com.shipgame.nanhai.data.*;
import java.lang.reflect.Method;

/** Deterministic gameplay regression checks; no account store or graphics needed. */
public final class GeometryRegression {
    public static void main(String[] args) throws Exception {
        Method move=GameState.class.getDeclaredMethod("move",float.class); move.setAccessible(true);
        Method steering=GameState.class.getDeclaredMethod("applySteerAndSpeed",float.class); steering.setAccessible(true);
        for(int angle=0;angle<360;angle+=30) for(float speed:new float[]{0,.01f,2,60}) for(int direction:new int[]{-1,1}) {
            GameState g=GameState.newGame(); g.undockInPlace(); g.headingDeg=angle; g.speed=speed; g.steerInput=direction;
            g.holdDecel=true; steering.invoke(g,.1f);
            float delta=((g.headingDeg-angle+540)%360)-180;
            require(delta*direction<0,"screen-relative helm at heading "+angle+" speed "+speed);
        }
        GameState profile=GameState.newGame();profile.setProfile("  海风船长  ",3);
        GameState loaded=GameState.fromSave(profile.toSave());
        require(loaded.nickname.equals("海风船长") && loaded.avatarIndex==3,"profile round trip");
        SaveData legacy=new SaveData();legacy.nickname=null;legacy.avatarIndex=99;
        loaded=GameState.fromSave(legacy);
        require(loaded.nickname.equals("船长") && loaded.avatarIndex==3,"legacy profile defaults");
        for(int version=1;version<=3;version++) {
            legacy.worldVersion=version;legacy.x=700;legacy.y=900;legacy.dockedPort=-1;
            float scale=version==1?4:version==2?2:1;
            loaded=GameState.fromSave(legacy);
            require(loaded.x==700*scale && loaded.y==900*scale,"sea migration version "+version);
            loaded=GameState.fromSave(loaded.toSave());
            require(loaded.x==700*scale && loaded.y==900*scale,"world migration runs once");
            for(int port=0;port<Catalog.PORTS.length;port++) {
                legacy.dockedPort=port;
                legacy.x=Catalog.PORT_X[port]/scale+210;legacy.y=Catalog.PORT_Y[port]/scale;
                loaded=GameState.fromSave(legacy);
                require(loaded.x==Catalog.PORT_X[port]+210 && loaded.y==Catalog.PORT_Y[port],"dock migration version "+version+" port "+port);
            }
        }
        GameState daily=GameState.newGame(); int startSilver=daily.silver;
        require(daily.canClaimDaily() && daily.claimDailyLogin() && daily.silver==startSilver+200,"daily first claim");
        daily=GameState.fromSave(daily.toSave());
        require(!daily.claimDailyLogin() && daily.silver==startSilver+200,"daily saved repeat guard");
        Method clock=GameState.class.getDeclaredMethod("advanceClock",float.class);clock.setAccessible(true);
        daily.dayMin=1439.9f;clock.invoke(daily,1f);
        require(daily.gameDay==2 && daily.claimDailyLogin() && daily.silver==startSilver+400,"midnight refreshes daily reward");
        daily.silver=Integer.MAX_VALUE; daily.gameDay++;
        require(!daily.claimDailyLogin() && daily.canClaimDaily(),"overflow does not consume daily reward");
        int silver=loaded.silver;
        require(!loaded.redeemCode(null) && !loaded.redeemCode("invalid") && loaded.silver==silver,"invalid codes leave balance unchanged");
        require(loaded.redeemCode(" 666 ") && loaded.silver==silver+666,"666 reward");
        loaded=GameState.fromSave(loaded.toSave());
        require(!loaded.redeemCode("666") && loaded.silver==silver+666,"redeem persisted once");
        require(loaded.redeemCode("888") && loaded.silver==silver+1554 && !loaded.redeemCode("888"),"888 reward once");
        int impacts=0;
        for(int ship=0;ship<Catalog.SHIPS.length;ship++) for(boolean port:new boolean[]{true,false}) {
            int count=port?Catalog.PORTS.length:Catalog.ISLANDS.length;
            for(int i=0;i<count;i++) {
                float ox=port?Catalog.PORT_X[i]:Catalog.ISLAND_X[i], oy=port?Catalog.PORT_Y[i]:Catalog.ISLAND_Y[i];
                float limit=VoyageGeometry.landRadius(port,i)+VoyageGeometry.ship(ship).radius();
                require(limit<(port?Catalog.DOCK_RANGE:Catalog.ISLAND_RANGE)-2,"dock clearance");
                for(int angle=0;angle<360;angle+=30) {
                    GameState g=GameState.newGame(); g.undockInPlace(); g.ship=ship;
                    double a=Math.toRadians(angle);
                    g.x=ox+(float)Math.cos(a)*(limit+5); g.y=oy+(float)Math.sin(a)*(limit+5);
                    if(g.x<40 || g.x>Catalog.WORLD_W-40 || g.y<40 || g.y>Catalog.WORLD_H-40) continue;
                    g.headingDeg=angle+180; g.speed=1000;
                    move.invoke(g,1f); // End-point-only collision would tunnel all the way through.
                    require(Catalog.dist(g.x,g.y,ox,oy)>=limit-.01,"penetration");
                    require((g.x-ox)*Math.cos(a)+(g.y-oy)*Math.sin(a)>0,"tunneled through land ship="+ship+" port="+port+" id="+i+" angle="+angle+" x="+g.x+" y="+g.y);
                    impacts++;
                }
                GameState g=GameState.newGame(); g.undockInPlace(); g.ship=ship;
                g.x=ox; g.y=oy; g.ensureLandClearance();
                require(Catalog.dist(g.x,g.y,ox,oy)>=limit-.01,"legacy save overlap");
                require(port?g.nearestPortInRange()==i:g.nearestIslandInRange()==i,"interaction range");
                g.x=ox+(port?Catalog.DOCK_RANGE:Catalog.ISLAND_RANGE)+60; g.y=oy; g.headingDeg=180; g.pirateSpawnTimer=10000;
                if(port) g.startAutoSail(i); else g.startAutoSailIsle(i);
                for(int step=0;step<1200 && g.autoSail;step++) g.update(1f/60);
                require(!g.autoSail && g.speed==0,"auto arrival");
                require(Catalog.dist(g.x,g.y,ox,oy)>=limit-.01,"auto penetration");
            }
        }
        for(int target=0;target<Catalog.PORTS.length+Catalog.ISLANDS.length;target++) {
            GameState g=GameState.newGame();g.undockInPlace();g.pirateSpawnTimer=100000;g.supply=100000;
            boolean port=target<Catalog.PORTS.length;int id=port?target:target-Catalog.PORTS.length;
            if(port)g.startAutoSail(id);else g.startAutoSailIsle(id);
            for(int step=0;step<36000&&g.autoSail;step++)g.update(1f/30);
            require(!g.autoSail && g.speed==0,"long route from Yangzhou to "+(port?Catalog.PORTS[id]:Catalog.ISLANDS[id]));
        }
        // 0.28.21 coastal drift: unanchored ship inside port range slides OUT of
        // interact range within seconds (drift stops once clear of land range);
        // anchored ship stays exactly put; open sea has no drift.
        GameState drift=GameState.newGame(); drift.undockInPlace(); drift.pirateSpawnTimer=100000; drift.supply=100000;
        drift.x=Catalog.PORT_X[0]+Catalog.DOCK_RANGE-40; drift.y=Catalog.PORT_Y[0];
        for(int i=0;i<300;i++) drift.update(1f/60); // 5s unanchored
        float moved=Catalog.dist(drift.x,drift.y,Catalog.PORT_X[0]+Catalog.DOCK_RANGE-40,Catalog.PORT_Y[0]);
        require(moved>=35f && drift.nearestPortInRange()<0,"unanchored ship drifts out of interact range (moved="+moved+")");
        drift.x=Catalog.PORT_X[0]+Catalog.DOCK_RANGE-20; drift.y=Catalog.PORT_Y[0]; drift.dropAnchor();
        float bx=drift.x,by=drift.y;
        for(int i=0;i<300;i++) drift.update(1f/60);
        require(drift.anchored && Catalog.dist(bx,by,drift.x,drift.y)<0.01f,"anchored ship stays put");
        // Open sea (out of every port/island range) must not drift.
        drift.weighAnchor(); drift.x=Catalog.WORLD_W/2; drift.y=Catalog.WORLD_H/2;
        float sx=drift.x,sy=drift.y;
        for(int i=0;i<300;i++) drift.update(1f/60);
        require(drift.x==sx && drift.y==sy,"open sea has no drift");
        System.out.println("GEOMETRY PASS: "+impacts+" high-speed impacts; every ship/land docking, auto arrival, legacy recovery; helm/profile; 34 long routes from Yangzhou; coastal drift/anchor hold");
    }
    private static void require(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
}
