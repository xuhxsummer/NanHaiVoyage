package com.shipgame.nanhai.data;

public class SaveData {
    public float x, y, headingDeg;
    public float hull, hullMax = 100f;
    public float supply, supplyMax = 100f;
    public int silver;
    public int debt;
    public int cargoCap;
    public int warehouseLevel = 1;
    public int cannonLevel = 1;
    public int cannonDamage;
    public int crew;
    public int crewCap;
    public int crewCapLevel = 1;
    public int[] trade;
    public int[] beasts;
    public int[] herbs;
    public boolean[] beastFound;
    public boolean[] herbFound;
    public int lastPort = 0;
    // 0.26.1 任务 system: per-quest progress + claim state.
    // Quest IDs match QuestDef IDs.
    public int questSellSilk = 0;       // 卖出丝绸数量
    public int questVisitPorts = 0;     // 访问过的不同港口数量
    public int questDefeatedPirates = 0; // 击败海盗数
    public int questBeastsFound = 0;    // 发现异兽种类数
    public boolean questDebtPaid = false; // 曾达成过零欠款（用于判读永久完成）
    public int questSilverPeak = 0;     // 达到的最高银两（用于银达X任务）
    public int questWarehouseUps = 0;   // 仓库升级次数
    public int questHiredCrew = 0;      // 雇佣总人数
    // 任务奖励领取标记：每个任务是否已领取
    public boolean questClaimSellSilk = false;
    public boolean questClaimVisitPorts = false;
    public boolean questClaimDefeatedPirates = false;
    public boolean questClaimBeastsFound = false;
    public boolean questClaimDebtPaid = false;
    public boolean questClaimSilverPeak = false;
    public boolean questClaimWarehouseUps = false;
    public boolean questClaimHiredCrew = false;
}
