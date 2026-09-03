package com.shipgame.nanhai.data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;

/** Runtime voyage: ship, cargo, weather, pirates, port/island actions. */
public class GameState {

    public enum WeatherKind { CLEAR, RAIN, FOG }

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

    public boolean autoSail;
    public int autoSailPort = -1;   // auto-sail target port (>=0) when sailing to a port
    public int autoSailIsle = -1;   // auto-sail target island when sailing to an island

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
    public float muzzleFlash;

    public boolean holdAccel, holdDecel;
    public float steerInput; // -1..1 from stick / A-D
    public boolean manualHeadingActive;
    public float desiredHeadingDeg;

    public boolean failed;
    public String failReason = "";
    public String toast = "";
    public float toastT;

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
        g.x = Catalog.PORT_X[0] + 90f;
        g.y = Catalog.PORT_Y[0];
        g.headingDeg = 0f;
        g.hull = g.hullMax;
        g.supply = g.supplyMax;
        g.silver = Catalog.START_SILVER;
        g.cargoCap = Catalog.START_CARGO_CAP;
        g.crew = Catalog.START_CREW;
        g.crewCap = Catalog.START_CREW_CAP;
        g.cannonDamage = Catalog.START_CANNON_DMG;
        g.dockedPort = 0;
        g.lastPort = 0;
        g.toast("已在广州靠港。世界暂停。");
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
            int lp = s.lastPort;
            if (lp < 0 || lp >= Catalog.PORTS.length) {
                lp = 0; // corrupt port index must not throw AIOOBE
            }
            g.lastPort = lp;
            g.dockedPort = lp;
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

    public int cargoUsed() {
        int n = 0;
        for (int v : trade) n += v;
        for (int v : beasts) n += v;
        for (int v : herbs) n += v;
        return n;
    }

    public int cargoFree() {
        return Math.max(0, cargoCap - cargoUsed());
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
        muzzleFlash = Math.max(0f, muzzleFlash - dt);
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
            headingDeg = approachAngle(headingDeg, want, Catalog.TURN_RATE * dt);
            holdAccel = true;
            holdDecel = false;
        } else if (manualHeadingActive) {
            // The stick represents an absolute direction, not angular velocity.
            // approachAngle follows the shortest arc and becomes a no-op once
            // aligned, so holding the stick cannot make the ship spin forever.
            headingDeg = approachAngle(headingDeg, desiredHeadingDeg, Catalog.TURN_RATE * dt);
        } else {
            if (Math.abs(steerInput) > 0.08f) {
                headingDeg += steerInput * Catalog.TURN_RATE * dt;
            }
        }
        headingDeg = wrapDeg(headingDeg);
        float windAlign = MathUtils.cosDeg(headingDeg - windDeg);
        float windMul = 1f + Catalog.WIND_SPEED_FACTOR * windStr * windAlign;
        float max = Catalog.MAX_SPEED * windMul;
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
        float r = MathUtils.random();
        if (r < 0.55f) {
            weather = WeatherKind.CLEAR;
        } else if (r < 0.78f) {
            weather = WeatherKind.RAIN;
        } else {
            weather = WeatherKind.FOG;
        }
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
        int cost = Catalog.goodPrice(port, good) * qty;
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
        int gain = Catalog.goodPrice(port, good) * qty;
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
        return "仓库升级完成，共用货舱容量 " + cargoCap + "。";
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
        return "人数上限升到 " + crewCap + "。再花钱雇人上船。";
    }

    public String hireCrew() {
        if (crew >= crewCap) {
            return "已满员，先升编制。";
        }
        if (silver < Catalog.HIRE_COST) {
            return "雇人要 " + Catalog.HIRE_COST + " 两。";
        }
        silver -= Catalog.HIRE_COST;
        crew++;
        questHiredCrew++;
        return "立刻雇上 1 人。船员 " + crew + "/" + crewCap + "。火力与补给按实际人数。";
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
        return Math.max(0.13f, Catalog.FIRE_INTERVAL / rate);
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
        if (combatLock && d <= Catalog.PIRATE_RANGE) {
            playerFireCd -= dt;
            if (playerFireCd <= 0f) {
                playerFireCd = fireInterval();
                pirateChase = true;
                pirateHp -= 1f; // 0.25.9: every player hit deals a flat 1 point
                muzzleFlash = 0.12f;
                if (pirateHp <= 0f) {
                    winCombat();
                    return;
                }
            }
        }
        if (d <= Catalog.PIRATE_RANGE) {
            pirateFireCd -= dt;
            if (pirateFireCd <= 0f) {
                pirateFireCd = Catalog.PIRATE_FIRE_INTERVAL;
                hull -= Catalog.PIRATE_SHOT;
                if (hull <= 0f) {
                    hull = 0f;
                    fail("船沉");
                    return;
                }
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
        muzzleFlash = 0f;
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

    public String weatherLabel() {
        switch (weather) {
            case RAIN:
                return "雨";
            case FOG:
                return "雾";
            default:
                return "晴";
        }
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
