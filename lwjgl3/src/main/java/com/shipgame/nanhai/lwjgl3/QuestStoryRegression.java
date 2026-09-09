package com.shipgame.nanhai.lwjgl3;

import com.shipgame.nanhai.data.GameState;
import com.shipgame.nanhai.screen.VoyageScreen;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Story additions must retain all nineteen saved quest identities and reward steps. */
public final class QuestStoryRegression {
    public static void main(String[] args) throws Exception {
        VoyageScreen voyage = new VoyageScreen(null);
        GameState g = GameState.newGame();
        Field state = field(VoyageScreen.class, "g"); state.set(voyage, g);
        Object[] quests = (Object[]) field(VoyageScreen.class, "QUESTS").get(null);
        require(quests.length == 19, "nineteen existing quests");
        Class<?> definition = quests[0].getClass();
        Method claim = method("claimQuest", GameState.class, definition);
        Method claimed = method("isQuestClaimed", GameState.class, definition);
        Method active = method("getActiveQuestIndex");
        String[] counters = {"questIslandVisits", "questRefillCount", "questRepairCount", "questBuyCount",
                "questProfitableSell", "questDefeatedPirates", "questIntelViewed", "questUpgradeCount",
                "questSellSilk", "questVisitPorts", "questBuyTea", "questSellPorcelain", "questIslandVisits",
                "questDefeatedPirates", "questBeastsFound", "questDebtPaid", "questSilverPeak", "questWarehouseUps", "questHiredCrew"};
        int[] targets = {1,1,1,1,1,1,1,1,50,5,10,30,3,3,5,1,5000,3,5};
        int[] silver = {30,20,20,40,80,100,30,80,200,300,50,250,120,150,250,400,0,150,100};
        for (int i = 0; i < quests.length; i++) {
            Object q = quests[i];
            require(field(definition, "id").getInt(q) == i, "saved quest id");
            require(field(definition, "unlockAfter").getInt(q) == i - 1, "original unlock chain");
            String story = (String) field(definition, "story").get(q);
            require(story.length() > 25 && story.length() < 120, "phone-length story for " + i);
            String[][] dialogue = (String[][]) field(definition, "dialogue").get(q);
            require(dialogue.length >= 2 && dialogue.length <= 5, "two to five spoken lines");
            for (String[] line : dialogue)
                require(line.length == 2 && !line[0].isEmpty() && !line[1].isEmpty() && line[1].length() <= 70,
                        "named speaker and phone-length line");
            require((int) active.invoke(voyage) == i, "next quest before completion");
            int money = g.silver;
            claim.invoke(voyage, g, q);
            require(g.silver == money && !(boolean) claimed.invoke(voyage, g, q), "incomplete quest cannot pay out");
            Field counter = GameState.class.getField(counters[i]);
            if (counter.getType() == boolean.class) counter.setBoolean(g, true); else counter.setInt(g, targets[i]);
            claim.invoke(voyage, g, q);
            require(g.silver == money + silver[i] && (boolean) claimed.invoke(voyage, g, q), "original claim and reward");
            claim.invoke(voyage, g, q);
            require(g.silver == money + silver[i], "no duplicate payout");
            // Cross the same save boundary used by an existing local account.
            g.questDialogueSeen |= 1 << i;
            g = GameState.fromSave(g.toSave()); state.set(voyage, g);
            require((boolean) claimed.invoke(voyage, g, q), "claim survives save round-trip");
            require((g.questDialogueSeen & (1 << i)) != 0, "playback marker survives save round-trip");
        }
        require((int) active.invoke(voyage) == -1, "chain completes");
        System.out.println("QUEST STORY PASS: all 19 IDs/unlocks, short authored stories, incomplete guards, original rewards, no duplicate claims and saved progress");
    }
    private static Field field(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name); f.setAccessible(true); return f;
    }
    private static Method method(String name, Class<?>... types) throws Exception {
        Method m = VoyageScreen.class.getDeclaredMethod(name, types); m.setAccessible(true); return m;
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
