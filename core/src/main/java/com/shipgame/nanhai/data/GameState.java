package com.shipgame.nanhai.data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;

/** Runtime voyage: ship, cargo, weather, pirates, port/island actions. */
public class GameState {

    // 0.26.2: weather is always sunny. Kept as a one-value enum so later
    // versions can reintroduce rain/fog without a save-data migration.
    public enum WeatherKind { CLEAR }

    public float x, y, headingDeg, speed;
    public float hull, hullMax = Catalog.HULL_MAX;
    public float supply, supplyMax = Catalog.SUPPLY_MAX;
    public int silver;
    public int debt;
    public int cargoCap;
    public int warehouseLevel = 1;
    public int cannonLevel = 1;
    public int cannonDamage;
    public int crew;
    public int crewCap;
    public int crewCapLevel = 1;
    public final int[] trade = new int[Catalog.GOODS.length];
    public final int[] beasts = new int[Catalog.BEASTS.length];
    public final int[] herbs = new int[Catalog.HERBS.length];
    public final boolean[] beastFound = new boolean[Catalog.BEASTS.length];
    public final boolean[] herbFound = new boolean[Catalog.HERBS.length];

    public int dockedPort = -1;
    public int islandMenu = -1;
    public boolean islandGathered;
    public float leaveCooldown;

    // 0.26.3 扬州渔业。渔夫只在扬州可雇；捕鱼只在停靠扬州且开始捕鱼时进行。
    public final int[] fish = new int[Catalog.FISH.length];
    public int fishers;              // 已雇渔夫人数
    public int fisherCapLevel = 1;   // 编制等级：上限 = FISHER_START_CAP + (level-1)
    public int fishToolLevel = 1;    // 钓具等级 1..FISH_TOOL_MAX
    public int fishSkillLevel = 1;   // 钓技等级 1..FISH_SKILL_MAX
    public boolean fishingOn;        // 是否在扬州开始捕鱼（离港自动停）
    public int fishCaughtTotal;      // 累计渔获条数（展示用）
    public float fishTimer;          // 距下次上钩剩余秒数（运行时）

    public boolean autoSail;
    public int autoSailPort = -1;   // auto-sail target port (>=0) when sailing to a port
    public int autoSailIsle = -1;   // auto-sail target island when sailing to an island

    // 0.26.2: weather is always 晴 (sunny). Wind still shifts so sailing keeps
    // its variety; rain/fog no longer occur.
    public WeatherKind weather = WeatherKind.CLEAR;
    public float windDeg = 90f;
    public float windStr = 0.35f;
    public float weatherTimer = 18f;

    public boolean pirateAlive;
    public float pirateX, pirateY, pirateHeading, pirateHp, pirateHpMax;
    public boolean pirateChase;
    public boolean combatLock;
    public float playerFireCd, pirateFireCd;
    public float pirateSpawnTimer = 12f;
    // 0.26.2: discrete flying cannonballs instead of an instant laser line.
    // Runtime-only (never persisted). Player balls are white, pirate balls black.
    public static final int MAX_BALLS = 16;
    public int ballCount;
    public final float[] ballX = new float[MAX_BALLS];
    public final float[] ballY = new float[MAX_BALLS];
    public final float[] ballSX = new float[MAX_BALLS];
    public final float[] ballSY = new float[MAX_BALLS];
    public final float[] ballDX = new float[MAX_BALLS];
    public final float[] ballDY = new float[MAX_BALLS];
    public final float[] ballDist = new float[MAX_BALLS];
    public final float[] ballT = new float[MAX_BALLS];     // seconds remaining
    public final float[] ballDur = new float[MAX_BALLS];
    public final boolean[] ballFromPlayer = new boolean[MAX_BALLS];

    public boolean holdAccel, holdDecel;
    public float steerInput; // -1..1 from stick / A-D
    public boolean manualHeadingActive;
    public float desiredHeadingDeg;

    public boolean failed;
    public String failReason = "";
    public String toast = "";
    public float toastT;

    // 0.26.4 商城：当前船 + 已拥有船（bit i 置位 = 已拥有 SHIPS[i]）。
    // 旧档无此字段：fromSave 补默认 = 0 号初始船且仅拥有它。
    public int ship = 0;
    public int shipOwned = 1; // bit0 = 初始小商船，永远置位

    // 0.26.4 游戏时钟：只在世界不暂停时推进（停靠/菜单/教学/失败等暂停）。
    // dayMin 是从当天 00:00 起的游戏分钟数；新档从第 1 天 06:00 起航。
    // 1 游戏日 = 15 现实分钟 = 900 秒；即 1 现实秒 = 1.6 游戏分钟。
    public static final float GAME_MIN_PER_SEC = 1440f / 900f;
    public int gameDay = 1;
    public float dayMin = 360f; // 06:00

    // 0.26.4 行情：每港每货的基准价偏移（千分位，±100 = ±10%）。
    // 只在每个游戏黎明 06:00 重摇一次；途中稳定。旧档缺省全 0（基准价）。
    public int[] marketOff; // [port * GOODS.length + good]，为 null 时视作全 0

    public int lastPort = 0;
    // 0.26.1 任务 tracking (runtime; persisted via SaveData).
    public int questSellSilk;
    public int questVisitPorts;
    public int questVisitPortSet = 0;   // bitmask of visited port indices
    public int questDefeatedPirates;
    public int questBeastsFound;
    public boolean questDebtPaid;
    public int questSilverPeak;
    public int questWarehouseUps;
    public int questHiredCrew;
    // 0.26.1 tutorial quest tracking
    public int questIslandVisits;      // 首次登岛：上岛搜采次数
    public int questRefillCount;       // 第一次补给：补给次数
    public int questRepairCount;       // 修一次船：修船次数
    public int questBuyCount;          // 买点货：买入货物次数
    public boolean questProfitableSell;// 赚个差价：是否曾有盈利卖出
    public boolean questIntelViewed;  // 看看行情：是否查看过情报/行情
    public int questUpgradeCount;     // 升级一项：升级次数
    public int questBuyTea;           // 买 10 茶叶：累计买入茶叶件数
    public int questSellPorcelain;    // 卖 30 瓷器：累计卖出的瓷器件数
    /** 每类商货累计支付银两（买入成本基准），用于判断「赚个差价」与卖价利润。 */
    public int[] costPaid = new int[Catalog.GOODS.length];
    // claim flags
    public boolean questClaimIslandVisit;
    public boolean questClaimRefill;
    public boolean questClaimRepair;
    public boolean questClaimBuy;
    public boolean questClaimProfitableSell;
    public boolean questClaimWinCombat;
    public boolean questClaimIntelViewed;
    public boolean questClaimUpgradeAny;
    public boolean questClaimSellSilk;
    public boolean questClaimVisitPorts;
    public boolean questClaimDefeatedPirates;
    public boolean questClaimBeastsFound;
    public boolean questClaimDebtPaid;
    public boolean questClaimSilverPeak;
    public boolean questClaimWarehouseUps;
    public boolean questClaimHiredCrew;
    public boolean questClaimBuyTea;
    public boolean questClaimSellPorcelain;
    public boolean questClaimIslandExplore;

    public static GameState newGame() {
        GameState g = new GameState();
        // 0.26.3: 扬州是故乡，新档从这里起航（北缘海岸，初始渔夫编制 2）。
        int home = Catalog.YANGZHOU;
        g.x = Catalog.PORT_X[home] + 90f;
        g.y = Catalog.PORT_Y[home];
        g.headingDeg = 0f;
        g.hull = g.hullMax;
        g.supply = g.supplyMax;
        g.silver = Catalog.START_SILVER;
        g.cargoCap = Catalog.START_CARGO_CAP;
        g.crew = Catalog.START_CREW;
        g.crewCap = Catalog.START_CREW_CAP;
        g.cannonDamage = Catalog.START_CANNON_DMG;
        g.dockedPort = home;
        g.lastPort = home;
        g.ship = 0;
        g.shipOwned = 1;
        g.gameDay = 1;
        g.dayMin = 360f; // 第 1 天 06:00 天亮启航
        g.refreshMarket(); // 新档行情也按基准价±5~8% 摇一次，情报所见即当前价
        g.toast("已在故乡扬州靠港。世界暂停。");
        return g;
    }

    public SaveData toSave() {
        SaveData s = new SaveData();
        s.x = x;
        s.y = y;
        s.headingDeg = headingDeg;
        s.hull = hull;
        s.hullMax = hullMax;
        s.supply = supply;
        s.supplyMax = supplyMax;
        s.silver = silver;
        s.debt = debt;
        s.cargoCap = cargoCap;
        s.warehouseLevel = warehouseLevel;
        s.cannonLevel = cannonLevel;
        s.cannonDamage = cannonDamage;
        s.crew = crew;
        s.crewCap = crewCap;
        s.crewCapLevel = crewCapLevel;
        s.trade = trade.clone();
        s.beasts = beasts.clone();
        s.herbs = herbs.clone();
        s.beastFound = beastFound.clone();
        s.herbFound = herbFound.clone();
        s.lastPort = lastPort;
        // 0.26.4 商城 / 时钟 / 行情
        s.ship = ship;
        s.shipOwned = shipOwned;
        s.gameDay = gameDay;
        s.dayMin = dayMin;
        if (marketOff != null) {
            s.marketOff = marketOff.clone();
        }
        // 0.26.3 fishing state
        s.fish = fish.clone();
        s.fishers = fishers;
        s.fisherCapLevel = fisherCapLevel;
        s.fishToolLevel = fishToolLevel;
        s.fishSkillLevel = fishSkillLevel;
        s.fishingOn = fishingOn;
        s.fishCaughtTotal = fishCaughtTotal;
        s.questSellSilk = questSellSilk;
        s.questVisitPorts = questVisitPorts;
        s.questDefeatedPirates = questDefeatedPirates;
        s.questBeastsFound = questBeastsFound;
        s.questDebtPaid = questDebtPaid;
        s.questSilverPeak = questSilverPeak;
        s.questWarehouseUps = questWarehouseUps;
        s.questHiredCrew = questHiredCrew;
        s.questClaimSellSilk = questClaimSellSilk;
        s.questClaimVisitPorts = questClaimVisitPorts;
        s.questClaimDefeatedPirates = questClaimDefeatedPirates;
        s.questClaimBeastsFound = questClaimBeastsFound;
        s.questClaimDebtPaid = questClaimDebtPaid;
        s.questClaimSilverPeak = questClaimSilverPeak;
        s.questClaimWarehouseUps = questClaimWarehouseUps;
        s.questClaimHiredCrew = questClaimHiredCrew;
        // 0.26.1 quest v2: tutorial + per-good counters + claim flags.
        s.questIslandVisits = questIslandVisits;
        s.questRefillCount = questRefillCount;
        s.questRepairCount = questRepairCount;
        s.questBuyCount = questBuyCount;
        s.questProfitableSell = questProfitableSell;
        s.questIntelViewed = questIntelViewed;
        s.questUpgradeCount = questUpgradeCount;
        s.questBuyTea = questBuyTea;
        s.questSellPorcelain = questSellPorcelain;
        s.questClaimIslandVisit = questClaimIslandVisit;
        s.questClaimRefill = questClaimRefill;
        s.questClaimRepair = questClaimRepair;
        s.questClaimBuy = questClaimBuy;
        s.questClaimProfitableSell = questClaimProfitableSell;
        s.questClaimWinCombat = questClaimWinCombat;
        s.questClaimIntelViewed = questClaimIntelViewed;
        s.questClaimUpgradeAny = questClaimUpgradeAny;
        s.questClaimBuyTea = questClaimBuyTea;
        s.questClaimSellPorcelain = questClaimSellPorcelain;
        s.questClaimIslandExplore = questClaimIslandExplore;
        s.costPaid = costPaid.clone();
        return s;
    }

    public static GameState fromSave(SaveData s) {
        GameState g = new GameState();
        if (s == null) {
            return newGame();
        }
        try {
            float oldHullMax = s.hullMax <= 0 ? Catalog.HULL_MAX : s.hullMax;
            float oldSupplyMax = s.supplyMax <= 0 ? Catalog.SUPPLY_MAX : s.supplyMax;
            g.hullMax = Math.max(Catalog.HULL_MAX, oldHullMax);
            g.supplyMax = Math.max(Catalog.SUPPLY_MAX, oldSupplyMax);
            float loadedHull = (s.hull <= 0f || Float.isNaN(s.hull)) ? g.hullMax : s.hull;
            float loadedSupply = (s.supply <= 0f || Float.isNaN(s.supply)) ? g.supplyMax : s.supply;
            g.hull = Math.min(loadedHull, g.hullMax);
            g.supply = Math.min(loadedSupply, g.supplyMax);
            g.silver = s.silver;
            g.debt = Math.max(0, s.debt);
            g.cargoCap = s.cargoCap <= 0 ? Catalog.START_CARGO_CAP : s.cargoCap;
            g.warehouseLevel = Math.max(1, s.warehouseLevel);
            g.cannonLevel = Math.max(1, s.cannonLevel);
            g.cannonDamage = s.cannonDamage <= 0 ? Catalog.START_CANNON_DMG : s.cannonDamage;
            g.crew = Math.max(0, s.crew);
            g.crewCap = s.crewCap <= 0 ? Catalog.START_CREW_CAP : Math.max(s.crew, s.crewCap);
            g.crewCapLevel = Math.max(1, s.crewCapLevel);
            copy(s.trade, g.trade);
            copy(s.beasts, g.beasts);
            copy(s.herbs, g.herbs);
            copy(s.beastFound, g.beastFound);
            copy(s.herbFound, g.herbFound);
            // 0.26.3 fishing state (absent fields on old saves keep defaults).
            copy(s.fish, g.fish);
            g.fishers = Math.max(0, s.fishers);
            g.fisherCapLevel = Math.max(1, Math.min(Catalog.FISHER_CAP_MAX, s.fisherCapLevel));
            g.fishToolLevel = Math.max(1, Math.min(Catalog.FISH_TOOL_MAX, s.fishToolLevel));
            g.fishSkillLevel = Math.max(1, Math.min(Catalog.FISH_SKILL_MAX, s.fishSkillLevel));
            g.fishCaughtTotal = Math.max(0, s.fishCaughtTotal);
            int lp = s.lastPort;
            if (lp < 0 || lp >= Catalog.PORTS.length) {
                lp = 0; // corrupt port index must not throw AIOOBE
            }
            g.lastPort = lp;
            g.dockedPort = lp;
            // 0.26.4 船 / 时钟 / 行情（旧档缺省：初始船、第 1 天 06:00、基准价）
            g.ship = s.ship >= 0 && s.ship < Catalog.SHIPS.length ? s.ship : 0;
            g.shipOwned = (s.shipOwned | 1); // 初始船永远拥有
            if (!g.ownsShip(g.ship)) {
                g.ship = 0;
            }
            g.gameDay = Math.max(1, s.gameDay);
            g.dayMin = (s.dayMin >= 0f && s.dayMin < 1440f) ? s.dayMin : 360f;
            if (s.marketOff == null || s.marketOff.length != Catalog.PORTS.length * Catalog.GOODS.length) {
                g.refreshMarket();
            } else {
                g.marketOff = s.marketOff.clone();
            }
            // 捕鱼只在扬州停靠时恢复（其它港口的存档视为已收网）。
            g.fishingOn = s.fishingOn && lp == Catalog.YANGZHOU;
            g.fishTimer = 0f;
            g.questSellSilk = s.questSellSilk;
            g.questVisitPorts = s.questVisitPorts;
            g.questDefeatedPirates = s.questDefeatedPirates;
            g.questBeastsFound = s.questBeastsFound;
            g.questDebtPaid = s.questDebtPaid;
            g.questSilverPeak = s.questSilverPeak;
            g.questWarehouseUps = s.questWarehouseUps;
            g.questHiredCrew = s.questHiredCrew;
            g.questClaimSellSilk = s.questClaimSellSilk;
            g.questClaimVisitPorts = s.questClaimVisitPorts;
            g.questClaimDefeatedPirates = s.questClaimDefeatedPirates;
            g.questClaimBeastsFound = s.questClaimBeastsFound;
            g.questClaimDebtPaid = s.questClaimDebtPaid;
            g.questClaimSilverPeak = s.questClaimSilverPeak;
            g.questClaimWarehouseUps = s.questClaimWarehouseUps;
            g.questClaimHiredCrew = s.questClaimHiredCrew;
            // 0.26.1 quest v2 (missing fields on old saves default to 0/false).
            g.questIslandVisits = s.questIslandVisits;
            g.questRefillCount = s.questRefillCount;
            g.questRepairCount = s.questRepairCount;
            g.questBuyCount = s.questBuyCount;
            g.questProfitableSell = s.questProfitableSell;
            g.questIntelViewed = s.questIntelViewed;
            g.questUpgradeCount = s.questUpgradeCount;
            g.questBuyTea = s.questBuyTea;
            g.questSellPorcelain = s.questSellPorcelain;
            g.questClaimIslandVisit = s.questClaimIslandVisit;
            g.questClaimRefill = s.questClaimRefill;
            g.questClaimRepair = s.questClaimRepair;
            g.questClaimBuy = s.questClaimBuy;
            g.questClaimProfitableSell = s.questClaimProfitableSell;
            g.questClaimWinCombat = s.questClaimWinCombat;
            g.questClaimIntelViewed = s.questClaimIntelViewed;
            g.questClaimUpgradeAny = s.questClaimUpgradeAny;
            g.questClaimBuyTea = s.questClaimBuyTea;
            g.questClaimSellPorcelain = s.questClaimSellPorcelain;
            g.questClaimIslandExplore = s.questClaimIslandExplore;
            copy(s.costPaid, g.costPaid);
            // Always respawn at the dock: any stale/NaN position is discarded.
            g.x = Catalog.PORT_X[lp] + 90f;
            g.y = Catalog.PORT_Y[lp];
            g.headingDeg = 0f;
            g.speed = 0;
            g.clearPirate();
            g.autoSail = false;
            g.autoSailPort = -1;
            g.autoSailIsle = -1;
            g.failed = false;
            g.toast("已读取靠港存档。");
            return g;
        } catch (Throwable t) {
            // Corrupt save: start a fresh game instead of dying.
            Gdx.app.error("GameState", "save corrupt, starting new game", t);
            return newGame();
        }
    }

    private static void copy(int[] src, int[] dst) {
        if (src == null) {
            return;
        }
        System.arraycopy(src, 0, dst, 0, Math.min(src.length, dst.length));
    }

    private static void copy(boolean[] src, boolean[] dst) {
        if (src == null) {
            return;
        }
        System.arraycopy(src, 0, dst, 0, Math.min(src.length, dst.length));
    }

    public boolean worldPaused() {
        return dockedPort >= 0 || islandMenu >= 0 || failed;
    }

    /** 当前船在 SHIPS 里是否已拥有。 */
    public boolean ownsShip(int i) {
        return i >= 0 && i < 32 && (shipOwned & (1 << i)) != 0;
    }

    /** 货舱容量（仓库基础 + 当前船加成）。 */
    public int holdCap() {
        return cargoCap + Catalog.SHIP_HOLD[ship];
    }

    /** 船员上限（编制基础 + 当前船加成）。 */
    public int crewMax() {
        return crewCap + Catalog.SHIP_CREW[ship];
    }

    /** 航速倍率（当前船）。 */
    public float speedMult() {
        return 1f + Catalog.SHIP_SPEED[ship] / 100f;
    }

    /** 转向倍率（当前船，走舸更灵活）。 */
    public float turnMult() {
        return 1f + Catalog.SHIP_TURN[ship] / 100f;
    }

    /** 行情：当前（含黎明偏移后的）某港某货价。 */
    public int goodPrice(int port, int good) {
        int base = Catalog.goodPrice(port, good);
        if (marketOff == null || marketOff.length == 0) {
            return base;
        }
        int off = marketOff[port * Catalog.GOODS.length + good];
        if (off == 0) {
            return base;
        }
        return Math.max(1, Math.round(base * (1000 + off) / 1000f));
    }

    /** 游戏时间文案：第N日 HH:MM 白天/夜晚（06:00-18:00 白天）。 */
    public String timeLabel() {
        int total = (int) dayMin;
        int hh = (total / 60) % 24;
        int mm = total % 60;
        boolean day = total >= 360 && total < 1080;
        return String.format("第%d日 %02d:%02d %s", gameDay, hh, mm, day ? "白天" : "夜晚");
    }

    /** 每个游戏黎明 06:00 重摇各港各货价格偏移（±5%~±8%，硬上限 ±10%）。 */
    public void refreshMarket() {
        int n = Catalog.PORTS.length * Catalog.GOODS.length;
        if (marketOff == null || marketOff.length != n) {
            marketOff = new int[n];
        }
        for (int i = 0; i < n; i++) {
            int sign = MathUtils.randomBoolean() ? 1 : -1;
            // 幅度 50..80 千分位（5%~8%），符号随机
            int mag = 50 + MathUtils.random(30);
            marketOff[i] = sign * mag;
        }
    }

    /** 只在世界不暂停时被 update() 调用：推进时钟并在跨过 06:00 时刷新行情。 */
    private void advanceClock(float dt) {
        float prev = dayMin;
        dayMin += dt * GAME_MIN_PER_SEC;
        while (dayMin >= 1440f) {
            dayMin -= 1440f;
            gameDay++;
        }
        if (dayMin < 0f) {
            dayMin = 0f;
        }
        // 同一游戏日内跨过 06:00（360 分钟）或过午夜后再次到达 06:00
        boolean crossedDawn = prev < 360f && dayMin >= 360f;
        if (crossedDawn) {
            refreshMarket();
        }
    }

    public int cargoUsed() {
        int n = 0;
        for (int v : trade) n += v;
        for (int v : beasts) n += v;
        for (int v : herbs) n += v;
        for (int v : fish) n += v;
        return n;
    }

    public int cargoFree() {
        return Math.max(0, holdCap() - cargoUsed());
    }

    /** 渔获总条数。 */
    public int fishTotal() {
        int n = 0;
        for (int v : fish) n += v;
        return n;
    }

    public void toast(String m) {
        toast = m;
        toastT = 3.2f;
    }

    public void update(float dt) {
        if (toastT > 0) {
            toastT -= dt;
        }
        if (silver > questSilverPeak) {
            questSilverPeak = silver;
        }
        if (failed) {
            return;
        }
        if (worldPaused()) {
            return;
        }
        leaveCooldown = Math.max(0f, leaveCooldown - dt);
        updateWeather(dt);
        applySteerAndSpeed(dt);
        move(dt);
        drain(dt);
        if (supply <= 0f) {
            fail("补给耗尽");
            return;
        }
        if (hull <= 0f) {
            fail("船沉");
            return;
        }
        if (pirateAlive) {
            updateCombat(dt);
        } else {
            pirateSpawnTimer -= dt;
            if (pirateSpawnTimer <= 0f) {
                spawnPirate();
            }
            if (leaveCooldown <= 0f) {
                tryApproach();
            }
        }
        // 0.26.4 游戏时钟只在世界真实运行时推进；停靠/菜单/教学/失败等暂停。
        advanceClock(dt);
    }

    private void applySteerAndSpeed(float dt) {
        if (autoSail && (autoSailPort >= 0 || autoSailIsle >= 0)) {
            float tx, ty;
            if (autoSailPort >= 0) {
                tx = Catalog.PORT_X[autoSailPort];
                ty = Catalog.PORT_Y[autoSailPort];
            } else {
                tx = Catalog.ISLAND_X[autoSailIsle];
                ty = Catalog.ISLAND_Y[autoSailIsle];
            }
            float want = MathUtils.atan2(ty - y, tx - x) * MathUtils.radiansToDegrees;
            headingDeg = approachAngle(headingDeg, want, Catalog.TURN_RATE * turnMult() * dt);
            holdAccel = true;
            holdDecel = false;
        } else if (manualHeadingActive) {
            // The stick represents an absolute direction, not angular velocity.
            // approachAngle follows the shortest arc and becomes a no-op once
            // aligned, so holding the stick cannot make the ship spin forever.
            headingDeg = approachAngle(headingDeg, desiredHeadingDeg, Catalog.TURN_RATE * turnMult() * dt);
        } else {
            if (Math.abs(steerInput) > 0.08f) {
                headingDeg += steerInput * Catalog.TURN_RATE * turnMult() * dt;
            }
        }
        headingDeg = wrapDeg(headingDeg);
        float windAlign = MathUtils.cosDeg(headingDeg - windDeg);
        float windMul = 1f + Catalog.WIND_SPEED_FACTOR * windStr * windAlign;
        float max = Catalog.MAX_SPEED * windMul * speedMult();
        if (holdAccel) {
            speed += Catalog.ACCEL * dt;
        } else if (holdDecel) {
            speed -= Catalog.ACCEL * 1.2f * dt;
        } else {
            speed -= Catalog.COAST * dt;
        }
        speed = MathUtils.clamp(speed, 0f, max);
    }

    private void move(float dt) {
        float rad = headingDeg * MathUtils.degreesToRadians;
        x += MathUtils.cos(rad) * speed * dt;
        y += MathUtils.sin(rad) * speed * dt;
        x = MathUtils.clamp(x, 40f, Catalog.WORLD_W - 40f);
        y = MathUtils.clamp(y, 40f, Catalog.WORLD_H - 40f);
    }

    private void drain(float dt) {
        if (speed <= 4f) {
            return;
        }
        float rate = Catalog.SUPPLY_DRAIN_BASE * (1f + Catalog.CREW_DRAIN * crew);
        supply -= rate * dt;
        if (supply < 0f) {
            supply = 0f;
        }
    }

    private void updateWeather(float dt) {
        weatherTimer -= dt;
        if (weatherTimer > 0f) {
            return;
        }
        weatherTimer = 16f + MathUtils.random(22f);
        windDeg = wrapDeg(windDeg + MathUtils.random(-80f, 80f));
        windStr = 0.15f + MathUtils.random(0.7f);
        weather = WeatherKind.CLEAR; // 0.26.2: 常年晴，无雨雾
    }

    private void tryApproach() {
        for (int i = 0; i < Catalog.PORTS.length; i++) {
            if (Catalog.dist(x, y, Catalog.PORT_X[i], Catalog.PORT_Y[i]) < Catalog.DOCK_RANGE) {
                dock(i);
                return;
            }
        }
        for (int i = 0; i < Catalog.ISLANDS.length; i++) {
            if (Catalog.dist(x, y, Catalog.ISLAND_X[i], Catalog.ISLAND_Y[i]) < Catalog.ISLAND_RANGE) {
                islandMenu = i;
                islandGathered = false;
                speed = 0f;
                stopAutoSail();
                toast("靠近" + Catalog.ISLANDS[i] + "，可搜采。世界暂停。");
                return;
            }
        }
    }

    public void dock(int port) {
        dockedPort = port;
        lastPort = port;
        islandMenu = -1;
        speed = 0f;
        stopAutoSail();
        clearPirate();
        // 任务追踪：访问新港口
        if ((questVisitPortSet & (1 << port)) == 0) {
            questVisitPortSet |= (1 << port);
            questVisitPorts++;
        }
        if (debt > 0) {
            int extra = Math.max(1, Math.round(debt * Catalog.INTEREST));
            debt += extra;
            toast(Catalog.PORTS[port] + "靠港。欠款计息 2%，现欠 " + debt + "。");
        } else {
            toast(Catalog.PORTS[port] + "靠港。世界暂停。");
        }
    }

    public void leavePort() {
        if (dockedPort < 0) {
            return;
        }
        int p = dockedPort;
        dockedPort = -1;
        // 0.26.3: 离港即停捕鱼（渔夫只能在家门口作业）。
        fishingOn = false;
        fishTimer = 0f;
        x = Catalog.PORT_X[p] + 95f;
        y = Catalog.PORT_Y[p] + 10f;
        headingDeg = 0f;
        speed = 0f;
        leaveCooldown = 2.2f;
        toast("离港。欠债不挡出航。");
    }

    public void leaveIsland() {
        if (islandMenu < 0) {
            return;
        }
        int i = islandMenu;
        islandMenu = -1;
        x = Catalog.ISLAND_X[i] + 90f;
        y = Catalog.ISLAND_Y[i];
        leaveCooldown = 2.2f;
        toast("离开岛屿。");
    }

    public String gatherIsland() {
        if (islandMenu < 0 || islandGathered) {
            return islandGathered ? "此处已搜过，离岛后再来。" : "不在岛上。";
        }
        islandGathered = true;
        if (cargoFree() <= 0) {
            return "货舱已满，搜到的也带不走。先卖掉或丢掉。";
        }
        // 任务追踪：首次登岛 / 探N岛（搜采一次算 1）
        questIslandVisits++;
        float r = MathUtils.random();
        if (r < 0.18f) {
            return "搜了一圈，没有新发现。";
        }
        if (r < 0.58f) {
            int idx = MathUtils.random(Catalog.BEASTS.length - 1);
            beasts[idx]++;
            if (!beastFound[idx]) {
                beastFound[idx] = true;
                questBeastsFound++;
            }
            return "发现异兽「" + Catalog.BEASTS[idx] + "」，已入货舱与图鉴。";
        }
        int idx = MathUtils.random(Catalog.HERBS.length - 1);
        herbs[idx]++;
        herbFound[idx] = true;
        return "采得草药「" + Catalog.HERBS[idx] + "」（一期只可卖钱）。";
    }

    public String buyGood(int port, int good, int qty) {
        if (qty <= 0) {
            return "数量不对。";
        }
        if (cargoFree() < qty) {
            return "货舱已满，不能再买。";
        }
        int cost = goodPrice(port, good) * qty;
        if (silver < cost) {
            return "银两不足。";
        }
        silver -= cost;
        trade[good] += qty;
        costPaid[good] += cost;
        // 任务追踪：买点货 / 买 10 茶叶
        questBuyCount++;
        if (good == 2) { // 茶叶 = GOODS[2]
            questBuyTea += qty;
        }
        return "买入 " + Catalog.GOODS[good] + " x" + qty + "。";
    }

    public String sellGood(int port, int good, int qty) {
        if (qty <= 0 || trade[good] < qty) {
            return "没有这么多货。";
        }
        int gain = goodPrice(port, good) * qty;
        // 赚个差价：卖出价高于买入均价即为盈利（成本基准 costPaid 随买卖维护）。
        if (trade[good] > 0) {
            int avgCost = costPaid[good] / trade[good];
            if (gain > avgCost * qty) {
                questProfitableSell = true;
            }
            costPaid[good] -= avgCost * qty;
            if (costPaid[good] < 0) {
                costPaid[good] = 0;
            }
        }
        trade[good] -= qty;
        silver += gain;
        // 任务追踪：卖出丝绸 / 卖 30 瓷器
        if (good == 0) {
            questSellSilk += qty;
        }
        if (good == 1) { // 瓷器 = GOODS[1]
            questSellPorcelain += qty;
        }
        return "卖出 " + Catalog.GOODS[good] + " x" + qty + "，得 " + gain + " 两。";
    }

    public String sellBeast(int idx, int qty) {
        if (qty <= 0 || beasts[idx] < qty) {
            return "没有这只异兽。";
        }
        int gain = Catalog.BEAST_PRICE[idx] * qty;
        beasts[idx] -= qty;
        silver += gain;
        return "卖掉 " + Catalog.BEASTS[idx] + " x" + qty + "，得 " + gain + " 两。";
    }

    public String sellHerb(int idx, int qty) {
        if (qty <= 0 || herbs[idx] < qty) {
            return "没有这种草药。";
        }
        int gain = Catalog.HERB_PRICE[idx] * qty;
        herbs[idx] -= qty;
        silver += gain;
        return "卖掉 " + Catalog.HERBS[idx] + " x" + qty + "，得 " + gain + " 两。";
    }

    public String dumpTrade(int good, int qty) {
        if (qty <= 0 || trade[good] < qty) {
            return "没有这么多。";
        }
        trade[good] -= qty;
        return "海上丢弃 " + Catalog.GOODS[good] + " x" + qty + "，没了。";
    }

    public String dumpBeast(int idx, int qty) {
        if (qty <= 0 || beasts[idx] < qty) {
            return "没有这么多。";
        }
        beasts[idx] -= qty;
        return "海上丢弃 " + Catalog.BEASTS[idx] + " x" + qty + "。";
    }

    public String dumpHerb(int idx, int qty) {
        if (qty <= 0 || herbs[idx] < qty) {
            return "没有这么多。";
        }
        herbs[idx] -= qty;
        return "海上丢弃 " + Catalog.HERBS[idx] + " x" + qty + "。";
    }

    // ------------------------------------------------------------------
    // 0.26.3 扬州渔业
    // ------------------------------------------------------------------

    /** 渔夫编制上限：初始 2，每级 +1（扬州专属升级）。 */
    public int fisherCap() {
        return Catalog.FISHER_START_CAP + (fisherCapLevel - 1);
    }

    public int fisherCapCost() {
        return 120 * fisherCapLevel;
    }

    public int fishToolCost() {
        return 150 * fishToolLevel;
    }

    public int fishSkillCost() {
        return 100 * fishSkillLevel;
    }

    /** 只在扬州可雇渔夫（其他港口只雇水手）。 */
    public String hireFisher() {
        if (dockedPort != Catalog.YANGZHOU) {
            return "渔夫只在故乡扬州招募。";
        }
        if (fishers >= fisherCap()) {
            return "渔夫已满（上限 " + fisherCap() + "），先升级「渔夫编制」。";
        }
        if (silver < Catalog.FISHER_HIRE_COST) {
            return "雇渔夫要 " + Catalog.FISHER_HIRE_COST + " 两。";
        }
        silver -= Catalog.FISHER_HIRE_COST;
        fishers++;
        return "渔夫上船，现共 " + fishers + " 人（上限 " + fisherCap() + "）。";
    }

    public String upgradeFisherCap() {
        if (dockedPort != Catalog.YANGZHOU) {
            return "渔夫编制只在扬州升级。";
        }
        if (fisherCapLevel >= Catalog.FISHER_CAP_MAX) {
            return "渔夫编制已到顶（" + fisherCap() + " 人）。";
        }
        int c = fisherCapCost();
        if (silver < c) {
            return "银两不足（渔夫编制升级 " + c + " 两）。";
        }
        silver -= c;
        fisherCapLevel++;
        questUpgradeCount++;
        return "渔夫编制升级，上限 " + fisherCap() + " 人（再花钱雇人）。";
    }

    public String upgradeFishTool() {
        if (dockedPort != Catalog.YANGZHOU) {
            return "钓具只在扬州升级。";
        }
        if (fishToolLevel >= Catalog.FISH_TOOL_MAX) {
            return "钓具已是最精良（Lv" + fishToolLevel + "）。";
        }
        int c = fishToolCost();
        if (silver < c) {
            return "银两不足（钓具升级 " + c + " 两）。";
        }
        silver -= c;
        fishToolLevel++;
        questUpgradeCount++;
        return "钓具升到 Lv" + fishToolLevel + "，可钓更大更贵的鱼。";
    }

    public String upgradeFishSkill() {
        if (dockedPort != Catalog.YANGZHOU) {
            return "钓技只在扬州进修。";
        }
        if (fishSkillLevel >= Catalog.FISH_SKILL_MAX) {
            return "钓技已炉火纯青（Lv" + fishSkillLevel + "）。";
        }
        int c = fishSkillCost();
        if (silver < c) {
            return "银两不足（钓技升级 " + c + " 两）。";
        }
        silver -= c;
        fishSkillLevel++;
        questUpgradeCount++;
        return "钓技升到 Lv" + fishSkillLevel + "，下竿更快。";
    }

    /** 捕鱼只在停靠扬州且渔夫≥1 时进行；此处只负责开关。 */
    public String startFishing() {
        if (dockedPort != Catalog.YANGZHOU) {
            return "只在扬州家门口捕鱼。";
        }
        if (fishers <= 0) {
            return "先雇一名渔夫。";
        }
        if (cargoFree() <= 0) {
            return "货舱满了，先卖掉些鱼或货腾出空位。";
        }
        fishingOn = true;
        if (fishTimer <= 0f) {
            fishTimer = catchInterval();
        }
        return "渔夫开始下网，约每 " + (int) Math.ceil(catchInterval()) + " 秒一条。";
    }

    public String stopFishing() {
        fishingOn = false;
        fishTimer = 0f;
        return "收网上岸，暂停捕鱼。";
    }

    /** 每次渔获秒数：1 渔夫/钓技 Lv1 为基准 12 秒；每多 1 名渔夫 +50% 速度，
     * 钓技每级 -6% 耗时，最短 3 秒（需求文档 v0.26.3 §3）。 */
    public float catchInterval() {
        if (fishers <= 0) {
            return Catalog.FISH_BASE_SECS;
        }
        float multi = 1f + 0.5f * (fishers - 1);
        float skillCut = 1f - 0.06f * (fishSkillLevel - 1);
        return Math.max(3f, Catalog.FISH_BASE_SECS / multi * skillCut);
    }

    /** 按钓具等级掷一条鱼（权重表 Catalog.FISH_ODDS）。 */
    private int rollFishKind() {
        int lv = Math.max(1, Math.min(Catalog.FISH_TOOL_MAX, fishToolLevel));
        int[] w = Catalog.FISH_ODDS[lv - 1];
        int total = 0;
        for (int v : w) {
            total += v;
        }
        int r = MathUtils.random(total - 1);
        for (int i = 0; i < w.length; i++) {
            r -= w[i];
            if (r < 0) {
                return i;
            }
        }
        return 0;
    }

    /** 每帧调用：停靠扬州 + 捕鱼中 + 有渔夫且舱未满时累积倒计时，到点入舱。
     * 返回本帧是否钓上一条（供 UI 决定是否刷新渔获面板）。 */
    public boolean tickFishing(float dt) {
        boolean caught = false;
        if (dockedPort != Catalog.YANGZHOU || !fishingOn || fishers <= 0 || failed) {
            return false;
        }
        if (cargoFree() <= 0) {
            // 舱满自动收网：不再下竿，卖鱼后才能继续。
            if (fishingOn) {
                fishingOn = false;
                toast("货舱满了，渔夫收网。卖些鱼腾出空位再开始。");
            }
            fishTimer = 0f;
            return false;
        }
        fishTimer -= dt;
        if (fishTimer <= 0f) {
            int kind = rollFishKind();
            fish[kind]++;
            fishCaughtTotal++;
            fishTimer = catchInterval();
            caught = true;
            toast("渔夫钓上「" + Catalog.FISH[kind] + "」，已入货舱。");
        }
        return caught;
    }

    /** 任意港口可把渔获按固定价卖给市场（与异兽/草药同规则）。 */
    public String sellFish(int idx, int qty) {
        if (qty <= 0 || fish[idx] < qty) {
            return "没有这种鱼。";
        }
        int gain = Catalog.FISH_PRICE[idx] * qty;
        fish[idx] -= qty;
        silver += gain;
        return "卖出 " + Catalog.FISH[idx] + " x" + qty + "，得 " + gain + " 两。";
    }

    public String dumpFish(int idx, int qty) {
        if (qty <= 0 || fish[idx] < qty) {
            return "没有这么多。";
        }
        fish[idx] -= qty;
        return "海上丢弃 " + Catalog.FISH[idx] + " x" + qty + "。";
    }

    public String refillSupply() {
        float gap = supplyMax - supply;
        if (gap < 0.5f) {
            return "补给已满。";
        }
        int cost = Math.max(1, Math.round(gap * Catalog.SUPPLY_UNIT_COST));
        int paid = Math.min(cost, silver);
        if (silver >= cost) {
            silver -= cost;
        } else {
            debt += cost - silver;
            silver = 0;
        }
        supply = supplyMax;
        // 任务追踪：第一次补给
        questRefillCount++;
        if (paid >= cost) {
            return "按缺口补补给，花 " + cost + " 两。";
        }
        return "银两不够，借债 " + (cost - paid) + " 两补满补给。现欠 " + debt + "。";
    }

    public String repay(int amount) {
        if (debt <= 0) {
            return "没有欠款。";
        }
        int pay = Math.min(amount, Math.min(debt, silver));
        if (pay <= 0) {
            return "没有银两可还。不还也能离港。";
        }
        silver -= pay;
        debt -= pay;
        if (debt <= 0) {
            debt = 0;
            questDebtPaid = true;
        }
        return "还债 " + pay + " 两，剩余欠款 " + debt + "。";
    }

    public String repair() {
        float gap = hullMax - hull;
        if (gap < 0.5f) {
            return "船体无需修理。";
        }
        int cost = Math.max(1, Math.round(gap * Catalog.REPAIR_UNIT_COST));
        if (silver < cost) {
            return "银两不足，修船要 " + cost + " 两。";
        }
        silver -= cost;
        hull = hullMax;
        // 任务追踪：修一次船
        questRepairCount++;
        return "立刻修好，花 " + cost + " 两。";
    }

    public int warehouseCost() {
        return 250 * warehouseLevel;
    }

    public int cannonCost() {
        return 200 * cannonLevel;
    }

    public int crewCapCost() {
        return 120 * crewCapLevel;
    }

    public String upgradeWarehouse() {
        int c = warehouseCost();
        if (silver < c) {
            return "银两不足（仓库升级 " + c + " 两）。";
        }
        silver -= c;
        warehouseLevel++;
        cargoCap += 20;
        questWarehouseUps++;
        questUpgradeCount++;
        return "仓库升级完成，共用货舱容量 " + holdCap()
                + (Catalog.SHIP_HOLD[ship] != 0 ? "（含本船 +" + Catalog.SHIP_HOLD[ship] + "）" : "") + "。";
    }

    public String upgradeCannon() {
        int c = cannonCost();
        if (silver < c) {
            return "银两不足（炮火升级 " + c + " 两）。";
        }
        silver -= c;
        cannonLevel++;
        questUpgradeCount++;
        float secs = Math.round(fireInterval() * 100f) / 100f;
        return "炮火升级完成：每发固定伤 1 点，连发加快到约 " + secs + " 秒一发（船员越多也越快）。";
    }

    public String upgradeCrewCap() {
        int c = crewCapCost();
        if (silver < c) {
            return "银两不足（编制 " + c + " 两）。";
        }
        silver -= c;
        crewCapLevel++;
        crewCap += 1;
        questUpgradeCount++;
        return "人数上限升到 " + crewMax()
                + (Catalog.SHIP_CREW[ship] != 0 ? "（含本船 +" + Catalog.SHIP_CREW[ship] + "）" : "") + "。再花钱雇人上船。";
    }

    public String hireCrew() {
        if (crew >= crewMax()) {
            return "已满员，先升编制或换更大船。";
        }
        if (silver < Catalog.HIRE_COST) {
            return "雇人要 " + Catalog.HIRE_COST + " 两。";
        }
        silver -= Catalog.HIRE_COST;
        crew++;
        questHiredCrew++;
        return "立刻雇上 1 人。船员 " + crew + "/" + crewMax() + "。火力与补给按实际人数。";
    }

    // ------------------------------------------------------------ 商城 / 船

    /** 买一艘船：扣款、标记拥有并立刻换乘。银两不足不扣款、返回原因。 */
    public String buyShip(int i) {
        if (i < 0 || i >= Catalog.SHIPS.length) {
            return "没有这条船。";
        }
        if (ownsShip(i)) {
            return equipShip(i);
        }
        int price = Catalog.SHIP_PRICE[i];
        if (silver < price) {
            return "银两不足：" + Catalog.SHIPS[i] + " 要 " + price + " 两，还差 " + (price - silver) + " 两。";
        }
        if (cargoUsed() > cargoCap + Catalog.SHIP_HOLD[i]) {
            return "货舱放不下现有货物：" + Catalog.SHIPS[i] + " 的容量只有 "
                    + (cargoCap + Catalog.SHIP_HOLD[i]) + "，船上现有 " + cargoUsed() + " 件。先卖掉一些再买。";
        }
        silver -= price;
        shipOwned |= (1 << i);
        ship = i;
        return "购得「" + Catalog.SHIPS[i] + "」并立刻换乘（花 " + price + " 两）。";
    }

    /** 换乘到已拥有的船（不花钱）。换到货舱更小的船时先检查装得下。 */
    public String equipShip(int i) {
        if (i < 0 || i >= Catalog.SHIPS.length) {
            return "没有这条船。";
        }
        if (!ownsShip(i)) {
            return "还没有「" + Catalog.SHIPS[i] + "」，先购买再换乘。";
        }
        if (i == ship) {
            return "现在开的就是「" + Catalog.SHIPS[i] + "」。";
        }
        if (cargoUsed() > cargoCap + Catalog.SHIP_HOLD[i]) {
            return "货舱放不下现有货物：" + Catalog.SHIPS[i] + " 的容量只有 "
                    + (cargoCap + Catalog.SHIP_HOLD[i]) + "，船上现有 " + cargoUsed() + " 件。先卖掉一些再换。";
        }
        ship = i;
        return "换乘「" + Catalog.SHIPS[i] + "」完成。";
    }

    /** 是否禁止某条船（当前不在航行/货舱容量检查提示用）。 */
    public boolean shipFitsCargo(int i) {
        return cargoUsed() <= cargoCap + Catalog.SHIP_HOLD[i];
    }

    public void startAutoSail(int port) {
        if (worldPaused() || pirateAlive) {
            return;
        }
        autoSail = true;
        autoSailPort = port;
        autoSailIsle = -1;
        toast("自动驶向 " + Catalog.PORTS[port]);
    }

    /** Full-map tap on an island: sail there; arriving opens the island menu. */
    public void startAutoSailIsle(int idx) {
        if (worldPaused() || pirateAlive) {
            return;
        }
        autoSail = true;
        autoSailPort = -1;
        autoSailIsle = idx;
        toast("自动驶向 " + Catalog.ISLANDS[idx]);
    }

    public void cancelAutoSail() {
        if (!autoSail) {
            return;
        }
        stopAutoSail();
        toast("取消自动驶向，改回手动。");
    }

    /** Clears auto-sail without a toast (docking, failure, pirate spawn, ...). */
    private void stopAutoSail() {
        autoSail = false;
        autoSailPort = -1;
        autoSailIsle = -1;
    }

    public void onManualSteer() {
        if (autoSail && (manualHeadingActive || Math.abs(steerInput) > 0.25f)) {
            cancelAutoSail();
        }
    }

    public void aimHeading(float heading) {
        desiredHeadingDeg = wrapDeg(heading);
        manualHeadingActive = true;
        onManualSteer();
    }

    public void releaseHeading() {
        manualHeadingActive = false;
    }

    /** Seconds between two player cannon shots. 0.25.9 balance: every hit —
     * from either side — costs exactly 1 point (耐久 / pirate HP), so the
     * 升炮火 upgrade and a bigger crew no longer hit harder; they reload
     * faster instead (each cannon level and crew member shortens the interval
     * a bit), which keeps upgrades meaningful under the flat-1 combat rule. */
    public float fireInterval() {
        float rate = 1f + 0.15f * Math.max(0, cannonLevel - 1) + 0.04f * Math.max(0, crew - 1);
        float shipFire = 1f + Catalog.SHIP_FIRE[ship] / 100f;
        return Math.max(0.13f, Catalog.FIRE_INTERVAL / (rate * shipFire));
    }

    /** Shots per second, shown on the port menu as 射速. */
    public float firepower() {
        return 1f / fireInterval();
    }

    public boolean tryLockPirate(float wx, float wy) {
        if (!pirateAlive) {
            return false;
        }
        if (Catalog.dist(wx, wy, pirateX, pirateY) < 70f) {
            lockPirate();
            return true;
        }
        return false;
    }

    /** Locks the only enemy in the current encounter (used by the visible HUD
     * button as well as direct taps on the pirate sprite). */
    public void lockPirate() {
        if (!pirateAlive) {
            return;
        }
        combatLock = true;
        playerFireCd = Math.min(playerFireCd, 0.08f);
        toast("已锁定海盗，自动连续开火。");
    }

    public void cancelLock() {
        combatLock = false;
        toast("取消锁定。");
    }

    private void spawnPirate() {
        pirateSpawnTimer = 22f + MathUtils.random(28f);
        // Ports are safe waters. Postpone the roll instead of spawning an enemy
        // on top of a docking popup where it cannot be selected or fought.
        for (int i = 0; i < Catalog.PORTS.length; i++) {
            if (Catalog.dist(x, y, Catalog.PORT_X[i], Catalog.PORT_Y[i]) < 240f) {
                pirateSpawnTimer = 8f;
                return;
            }
        }
        float ang = MathUtils.random(360f) * MathUtils.degreesToRadians;
        pirateX = MathUtils.clamp(x + MathUtils.cos(ang) * 300f, 50f, Catalog.WORLD_W - 50f);
        pirateY = MathUtils.clamp(y + MathUtils.sin(ang) * 300f, 50f, Catalog.WORLD_H - 50f);
        pirateHeading = MathUtils.atan2(y - pirateY, x - pirateX) * MathUtils.radiansToDegrees;
        pirateHpMax = Catalog.PIRATE_HP;
        pirateHp = pirateHpMax;
        pirateAlive = true;
        pirateChase = false;
        combatLock = false;
        playerFireCd = 0f;
        pirateFireCd = 0.4f;
        stopAutoSail(); // pirate encounter: 自动航行遇海盗仍停战
        toast("遭遇海盗！点船锁定开火。默认就地打；还击会追得紧。");
    }

    private void updateCombat(float dt) {
        float d = Catalog.dist(x, y, pirateX, pirateY);
        if (d > Catalog.PIRATE_FLEE_RANGE) {
            toast("已开出范围，海盗停火。改回手动。");
            clearPirate();
            stopAutoSail();
            return;
        }
        // Player fires a WHITE cannonball (damage lands when it reaches the
        // pirate, not instantly). Pirate fires BLACK balls back, 1 point each.
        if (combatLock && d <= Catalog.PIRATE_RANGE) {
            playerFireCd -= dt;
            if (playerFireCd <= 0f) {
                playerFireCd = fireInterval();
                spawnBall(true, x, y, pirateX, pirateY);
            }
        }
        if (d <= Catalog.PIRATE_RANGE) {
            pirateFireCd -= dt;
            if (pirateFireCd <= 0f) {
                pirateFireCd = Catalog.PIRATE_FIRE_INTERVAL;
                spawnBall(false, pirateX, pirateY, x, y);
            }
        }
        if (pirateChase) {
            pirateHeading = MathUtils.atan2(y - pirateY, x - pirateX) * MathUtils.radiansToDegrees;
            float rad = pirateHeading * MathUtils.degreesToRadians;
            // It mostly fights in place. Retaliating provokes pursuit, but the
            // player can still escape by sailing well and opening the gap.
            float chase = 105f;
            pirateX += MathUtils.cos(rad) * chase * dt;
            pirateY += MathUtils.sin(rad) * chase * dt;
        }
        updateBalls(dt);
    }

    /** Fires one cannonball along a straight line toward the target. Travel time
     * scales with distance (~0.2-0.5s), so balls are visible in flight. */
    private void spawnBall(boolean fromPlayer, float sx, float sy, float tx, float ty) {
        if (ballCount >= MAX_BALLS) {
            return;
        }
        float dx = tx - sx, dy = ty - sy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < 1f) {
            dist = 1f;
        }
        float dur = 0.10f + dist / 1500f;
        if (dur > 0.5f) dur = 0.5f;
        int i = ballCount;
        ballSX[i] = sx;
        ballSY[i] = sy;
        ballX[i] = sx;
        ballY[i] = sy;
        ballDX[i] = dx / dist;
        ballDY[i] = dy / dist;
        ballDist[i] = dist;
        ballT[i] = dur;
        ballDur[i] = dur;
        ballFromPlayer[i] = fromPlayer;
        ballCount++;
    }

    /** Advances every ball; damage is applied only when a ball reaches its
     * target (misses if the enemy sailed away beyond the hit radius). */
    private void updateBalls(float dt) {
        int i = 0;
        while (i < ballCount) {
            ballT[i] -= dt;
            float p = 1f - MathUtils.clamp(ballT[i] / Math.max(ballDur[i], 1e-4f), 0f, 1f);
            ballX[i] = ballSX[i] + ballDX[i] * ballDist[i] * p;
            ballY[i] = ballSY[i] + ballDY[i] * ballDist[i] * p;
            if (ballT[i] > 0f) {
                i++;
                continue;
            }
            boolean hit = false;
            if (ballFromPlayer[i] && pirateAlive
                    && Catalog.dist(ballX[i], ballY[i], pirateX, pirateY) <= 100f) {
                // 0.25.9: every player hit deals a flat 1 point.
                pirateHp -= 1f;
                pirateChase = true;
                hit = true;
                if (pirateHp <= 0f) {
                    removeBall(i);
                    winCombat();
                    return;
                }
            } else if (!ballFromPlayer[i]
                    && Catalog.dist(ballX[i], ballY[i], x, y) <= 90f) {
                hull -= Catalog.PIRATE_SHOT; // 1 点耐久 / 海盗弹
                hit = true;
                if (hull <= 0f) {
                    removeBall(i);
                    hull = 0f;
                    fail("船沉");
                    return;
                }
            }
            removeBall(i);
            if (!hit) {
                // miss: ball splashes into the sea, nothing else happens
            }
        }
    }

    private void removeBall(int i) {
        int last = ballCount - 1;
        if (i != last) {
            ballSX[i] = ballSX[last];
            ballSY[i] = ballSY[last];
            ballX[i] = ballX[last];
            ballY[i] = ballY[last];
            ballDX[i] = ballDX[last];
            ballDY[i] = ballDY[last];
            ballDist[i] = ballDist[last];
            ballT[i] = ballT[last];
            ballDur[i] = ballDur[last];
            ballFromPlayer[i] = ballFromPlayer[last];
        }
        ballCount--;
    }

    private void winCombat() {
        questDefeatedPirates++;
        int loot = 25 + MathUtils.random(55);
        silver += loot;
        String extra = "";
        if (cargoFree() > 0 && MathUtils.randomBoolean()) {
            int g = MathUtils.random(Catalog.GOODS.length - 1);
            trade[g]++;
            extra = " 缴获 " + Catalog.GOODS[g] + "（当货物卖掉）。";
        } else if (MathUtils.random() < 0.2f) {
            extra = " 缴获一件船部件（一期当货：额外银两）。";
            silver += 40;
        }
        toast("打赢海盗，抢得 " + loot + " 两。" + extra + " 改回手动。");
        clearPirate();
        stopAutoSail();
    }

    public void clearPirate() {
        pirateAlive = false;
        combatLock = false;
        pirateChase = false;
        ballCount = 0; // in-flight balls vanish with the encounter
    }

    public void fail(String reason) {
        failed = true;
        failReason = reason;
        stopAutoSail();
        clearPirate();
        speed = 0f;
        toast(reason + "。失败，将读取上次靠港存档。");
    }

    public float windSpeedMul() {
        float windAlign = MathUtils.cosDeg(headingDeg - windDeg);
        return 1f + Catalog.WIND_SPEED_FACTOR * windStr * windAlign;
    }

    public String windLabel() {
        float a = MathUtils.cosDeg(headingDeg - windDeg);
        if (a > 0.35f) {
            return "顺风";
        }
        if (a < -0.35f) {
            return "逆风";
        }
        return "侧风";
    }

    private static float wrapDeg(float d) {
        while (d > 180f) d -= 360f;
        while (d < -180f) d += 360f;
        return d;
    }

    private static float approachAngle(float from, float to, float maxDelta) {
        float diff = wrapDeg(to - from);
        diff = MathUtils.clamp(diff, -maxDelta, maxDelta);
        return wrapDeg(from + diff);
    }
}
