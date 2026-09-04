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
    // 0.26.4 商城：当前船下标 + 已拥有位掩码；时钟：第 N 天 + 当日分钟；行情偏移表。
    // 旧档无这些字段：GameState.fromSave 补默认（初始船、第 1 天 06:00、基准价）。
    public int ship = 0;
    public int shipOwned = 1;
    public int gameDay = 1;
    public float dayMin = 360f;
    public int[] marketOff;
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
    // 0.26.1 quest v2 tutorial / per-good counters.
    public int questIslandVisits = 0;   // 上岛搜采次数
    public int questRefillCount = 0;    // 补给次数
    public int questRepairCount = 0;    // 修船次数
    public int questBuyCount = 0;       // 买入操作次数
    public boolean questProfitableSell = false; // 是否曾有盈利卖出
    public boolean questIntelViewed = false;    // 是否看过情报/行情
    public int questUpgradeCount = 0;   // 任意升级次数
    public int questBuyTea = 0;         // 买入茶叶件数
    public int questSellPorcelain = 0;  // 卖出瓷器件数
    // 0.26.1 quest v2 claim flags (id -> GameState field mapping in VoyageScreen).
    public boolean questClaimIslandVisit = false;
    public boolean questClaimRefill = false;
    public boolean questClaimRepair = false;
    public boolean questClaimBuy = false;
    public boolean questClaimProfitableSell = false;
    public boolean questClaimWinCombat = false;
    public boolean questClaimIntelViewed = false;
    public boolean questClaimUpgradeAny = false;
    public boolean questClaimBuyTea = false;
    public boolean questClaimSellPorcelain = false;
    public boolean questClaimIslandExplore = false;
    // 任务奖励领取标记：每个任务是否已领取
    public boolean questClaimSellSilk = false;
    public boolean questClaimVisitPorts = false;
    public boolean questClaimDefeatedPirates = false;
    public boolean questClaimBeastsFound = false;
    public boolean questClaimDebtPaid = false;
    public boolean questClaimSilverPeak = false;
    public boolean questClaimWarehouseUps = false;
    public boolean questClaimHiredCrew = false;
    /** 每类商货累计买入支付银两（判断盈利卖出的成本基准）。 */
    public int[] costPaid;
    // 0.26.3 扬州渔业。旧档无此字段：GameState.fromSave 用缺省值补零。
    public int[] fish;
    public int fishers;
    public int fisherCapLevel = 1;
    public int fishToolLevel = 1;
    public int fishSkillLevel = 1;
    public boolean fishingOn;
    public int fishCaughtTotal;
}
