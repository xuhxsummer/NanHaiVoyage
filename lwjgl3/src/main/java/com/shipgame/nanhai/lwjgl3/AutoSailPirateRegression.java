package com.shipgame.nanhai.lwjgl3;

import com.shipgame.nanhai.data.Catalog;
import com.shipgame.nanhai.data.GameState;
import java.lang.reflect.Method;

/** Pirates may threaten a route, but must not clear the captain's chosen destination. */
public final class AutoSailPirateRegression {
    public static void main(String[] args) throws Exception {
        Method spawn = GameState.class.getDeclaredMethod("spawnPirate"); spawn.setAccessible(true);
        Method combat = GameState.class.getDeclaredMethod("updateCombat", float.class); combat.setAccessible(true);
        Method win = GameState.class.getDeclaredMethod("winCombat"); win.setAccessible(true);
        for (boolean island : new boolean[]{false, true}) {
            GameState g = sea();
            start(g, island);
            spawn.invoke(g);
            require(g.pirateAlive && g.autoSail && !g.combatLock, "spawn preserves route without forced lock");
            destination(g, island);
            float x = g.x, y = g.y;
            g.update(.1f);
            require(g.autoSail && (g.x != x || g.y != y), "autopilot continues moving after encounter");
            g.cancelAutoSail();
            start(g, island);
            destination(g, island);
            require(g.autoSail, "autopilot starts with a live pirate");
            g.lockPirate(); require(g.combatLock, "manual combat lock still works");
            g.pirateX = g.x + 300; g.pirateY = g.y; g.playerFireCd = 0;
            g.update(.01f);
            require(g.ballCount > 0, "manual lock still fires");
            g.pirateX = g.x + Catalog.NPC_HORIZON + 200;
            combat.invoke(g, 0f);
            require(!g.pirateAlive && g.autoSail, "flee preserves route"); destination(g, island);
            spawn.invoke(g); win.invoke(g);
            require(!g.pirateAlive && g.autoSail && g.questDefeatedPirates == 1, "victory preserves route and rewards");
            g.aimHeading(90); require(!g.autoSail, "manual steering still cancels");
            g.releaseHeading(); start(g, island);
            g.fail("test"); require(!g.autoSail, "failure still cancels");

            g = sea(); start(g, island); g.pirateAlive = true;
            g.x = island ? Catalog.ISLAND_X[0] : Catalog.PORT_X[0];
            g.y = island ? Catalog.ISLAND_Y[0] : Catalog.PORT_Y[0];
            g.pirateX = g.x + 300; g.pirateY = g.y;
            g.update(0);
            require(!g.autoSail && g.speed == 0, "arrival still stops with live pirate");
        }
        GameState docked = GameState.newGame(); docked.pirateAlive = true;
        docked.startAutoSail(0); require(!docked.autoSail, "docked pause remains");
        System.out.println("AUTO SAIL PIRATE PASS: port/island start, spawn, continued movement, flee, victory, manual fire, manual cancel, arrival, failure and dock pause");
    }
    private static GameState sea() {
        GameState g = GameState.newGame(); g.undockInPlace();
        g.x = 2000; g.y = 12000; g.speed = 40; g.pirateSpawnTimer = 1000;
        return g;
    }
    private static void start(GameState g, boolean island) {
        if (island) g.startAutoSailIsle(0); else g.startAutoSail(0);
    }
    private static void destination(GameState g, boolean island) {
        require(g.autoSailPort == (island ? -1 : 0) && g.autoSailIsle == (island ? 0 : -1), "destination retained");
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
