package com.shipgame.nanhai.data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.shipgame.nanhai.audio.VoyageAudio;

/** Runtime voyage: ship, cargo, weather, pirates, port/island actions. */
public class GameState {
    public String nickname = "船长";
    public int avatarIndex;
    public int redeemedCodes;
    public int dailyClaimDay;

    public boolean canClaimDaily() { return gameDay>dailyClaimDay; }
    public boolean claimDailyLogin() {
        if(!canClaimDaily()) { toast("今日奖励已领取，下一游戏日再来。"); return false; }
        if(silver>Integer.MAX_VALUE-Catalog.DAILY_LOGIN_SILVER) { toast("银两已达上限，请稍后领取。"); return false; }
        silver+=Catalog.DAILY_LOGIN_SILVER; dailyClaimDay=gameDay;
        playCoinSfx();
        questSilverPeak=Math.max(questSilverPeak,silver);
        toast("每日登录奖励：银两 +"+Catalog.DAILY_LOGIN_SILVER); return true;
    }

    /** Exact numeric codes, surrounding whitespace ignored; one use per save. */
    public boolean redeemCode(String input) {
        String code=input==null?"":input.trim();
        int bit=code.equals("666")?1:code.equals("888")?2:0;
        if(bit==0) { toast("兑换码无效，请检查后重试。"); return false; }
        if((redeemedCodes&bit)!=0) { toast("此兑换码已领取。"); return false; }
        int reward=bit==1?666:888;
        if(silver>Integer.MAX_VALUE-reward) { toast("银两已达上限，请稍后兑换。"); return false; }
        silver+=reward; redeemedCodes|=bit; questSilverPeak=Math.max(questSilverPeak,silver);
        playCoinSfx();
        toast("兑换成功：银两 +"+reward); return true;
    }

    public void setProfile(String name, int avatar) {
        String clean = name == null ? "" : name.trim().replaceAll("[\\p{Cntrl}]", "");
        nickname = clean.isEmpty() ? "船长" : clean.substring(0, Math.min(16, clean.length()));
        avatarIndex = MathUtils.clamp(avatar, 0, 3);
    }

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

    public MerchantData merchant;
    // 0.28.22 海面船队：原有单艘商船/海盗之外，新增 2 艘货船 + 2 艘战船软上限，
    // 刷新仍走远处带（NPC_SPAWN_MIN..MAX），每次刷怪判定 ~25% 概率，避免扎堆。
    public final TraderData[] traders = new TraderData[2];
    public final WarshipData[] warships = new WarshipData[2];
    public float traderSpawnTimer = 70f;
    public float warshipSpawnTimer = 90f;
    public float merchantSpawnTimer = 45f;
    public boolean merchantLock;
    public int pirateDamage = 1;
    private int pirateGeneration, merchantGeneration;
    private static final int PLAYER = 0, PIRATE = 1, MERCHANT = 2;
    public boolean pirateAlive;
    public float pirateX, pirateY, pirateHeading, pirateHp, pirateHpMax;
    public boolean pirateChase;
    private final CombatHelm pirateHelm = new CombatHelm();
    public boolean combatLock;
    public float playerFireCd, pirateFireCd;
    public float pirateSpawnTimer = 12f;
    // 0.26.2: discrete flying cannonballs instead of an instant laser line.
    // Runtime-only projectiles. Player balls are white; NPC balls are black.
    public static final int MAX_BALLS = 48;
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
    private final int[] ballTarget = new int[MAX_BALLS], ballGeneration = new int[MAX_BALLS];
    private final int[] ballDamage = new int[MAX_BALLS];

    // 0.28.22 沉船表现：击沉的船不瞬间消失，先倾侧下沉 ~2.5 秒再移除。
    public static final int WRECK_SLOTS = 8;
    public final Wreck[] wrecks = new Wreck[WRECK_SLOTS];
    // 0.28.22 掠夺结算：玩家亲手击沉时记录战利品，由界面弹窗展示（不再只用底部横幅）。
    public final LootGain[] lootPopup = new LootGain[2];
    public float lootPopupT;
    public float lootDelay;
    public Wreck sinkFocus;
    public float playerSinkRemaining;
    // Visual slots: player, pirate, merchant, trader 0/1, warship 0/1. Runtime only.
    public final float[] hurtTime = new float[7], rockTime = new float[7];
    public final Splash[] impactSplashes = new Splash[8];
    public static final class Splash { public float x,y,remaining; }
    public boolean playerSinking() { return playerSinkRemaining>0; }
    public boolean playerSunk() { return failed && "船沉".equals(failReason); }
    public float hitFlash(int slot) { return MathUtils.clamp(hurtTime[slot]/.3f,0,1); }
    public float hitRock(int slot) { return MathUtils.sin((.35f-rockTime[slot])*32f)*9f*MathUtils.clamp(rockTime[slot]/.35f,0,1); }
    private int hitSlot(Victim v) { return v.kind==Victim.PIRATE?1:v.kind==Victim.MERCHANT?2:v.kind==Victim.TRADER?3+v.index:5+v.index; }
    private void contactFeedback(int a,int b,float ax,float ay,float bx,float by) {
        hurtTime[a]=hurtTime[b]=.3f; rockTime[a]=rockTime[b]=.35f;
        int slot=0;
        for(int i=0;i<impactSplashes.length;i++) if(impactSplashes[i]==null || impactSplashes[i].remaining<=0) { slot=i;break; }
        Splash splash=impactSplashes[slot];
        if(splash==null) splash=impactSplashes[slot]=new Splash();
        splash.x=(ax+bx)*.5f; splash.y=(ay+by)*.5f; splash.remaining=.45f;
    }
    /** Presentation clock also runs during player death and the non-pausing loot card. */
    public void updateFeedback(float dt) {
        dt=Math.max(0,dt);
        for(int i=0;i<hurtTime.length;i++) { hurtTime[i]=Math.max(0,hurtTime[i]-dt);rockTime[i]=Math.max(0,rockTime[i]-dt); }
        for(Splash splash:impactSplashes) if(splash!=null) splash.remaining=Math.max(0,splash.remaining-dt);
        lootDelay=Math.max(0,lootDelay-dt);
        playerSinkRemaining=Math.max(0,playerSinkRemaining-dt);
        anchorBeat=Math.max(0f,anchorBeat-dt/0.4f);
        updateCannonFeedback(dt);
        updateWrecks(dt);
    }

    /** One sinking ship: 2.5s tilt+submerge animation, then despawn. */
    public static final class Wreck {
        public float x, y, headingDeg;
        public int ship;         // Catalog.SHIPS index (PIRATE_SHIP for pirates)
        public float timer;      // counts down to 0
        public float duration;   // total sink time (2.5s)
        public float tilt, submerge, sway; // renderer helper values (0..1)
        public boolean alive, player, pirate;
    }

    /** 0.28.22 玩家掠夺收益（击沉海盗/商船后弹窗展示）。 */
    public static final class LootGain {
        public String title;      // 弹窗标题（击沉海盗 / 击沉商船）
        public int silver;
        public String good;       // 货物名（可为空字符串）
        public int goodCount;
        public String extra;      // 附加说明（缴获部件 / 货舱不足等）
    }

    public boolean holdAccel, holdDecel;
    public float steerInput; // Screen-left is -1; subtract it: +heading yaws left in (x, height, -y).
    // 0.28.17 摇杆油门：stick 前推 = +stickKY（0..1 前进油门，越大越快）。
    // 轻推缓行、推满全速；松杆不硬刹；拉杆向后只轻带减速（无倒船）。
    public float thrustInput;
    // 0.28.17 抛锚：近港/岛时停稳船只便于经营。离范围自动起锚；世界暂停时锚不变。
    public boolean anchored;
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
    // IDs 19+ have an independent claim bitset; main dialogue bits stay 0–18.
    public long sideQuestClaims;
    public int[] questGoodsBought = new int[Catalog.GOODS.length];
    public int[] questGoodsSold = new int[Catalog.GOODS.length];
    public int questSellSilk;
    public int questDialogueSeen;
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
        g.x = Catalog.PORT_X[home] + Catalog.DOCK_RANGE - 8f;
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
        s.worldVersion = Catalog.WORLD_VERSION;
        s.dailyClaimDay = dailyClaimDay;
        s.redeemedCodes = redeemedCodes;
        s.nickname = nickname;
        s.avatarIndex = avatarIndex;
        s.dockedPort = dockedPort;
        s.failed = failed;
        s.questVisitPortSet = questVisitPortSet;
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
        s.questDialogueSeen = questDialogueSeen;
        s.sideQuestClaims = sideQuestClaims;
        s.questGoodsBought = questGoodsBought.clone();
        s.questGoodsSold = questGoodsSold.clone();
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
        s.pirateAlive = pirateAlive;
        s.pirateX = pirateX; s.pirateY = pirateY; s.pirateHeading = pirateHeading;
        s.pirateHp = pirateHp; s.pirateDamage = pirateDamage; s.pirateSpawnTimer = pirateSpawnTimer;
        s.merchant = merchant == null ? null : merchant.copy();
        s.merchantSpawnTimer = merchantSpawnTimer;
        return s;
    }

    private static boolean finite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value); // Also supported on Android API 21.
    }

    public static GameState fromSave(SaveData s) {
        GameState g = new GameState();
        if (s == null) {
            return newGame();
        }
        try {
            g.setProfile(s.nickname, s.avatarIndex);
            g.redeemedCodes = s.redeemedCodes & 3;
            g.dailyClaimDay = Math.max(0,s.dailyClaimDay);
            float oldHullMax = s.hullMax <= 0 ? Catalog.HULL_MAX : s.hullMax;
            float oldSupplyMax = s.supplyMax <= 0 ? Catalog.SUPPLY_MAX : s.supplyMax;
            g.hullMax = Math.max(Catalog.HULL_MAX, oldHullMax);
            g.supplyMax = Math.max(Catalog.SUPPLY_MAX, oldSupplyMax);
            float loadedHull = (s.hull < 0f || Float.isNaN(s.hull)) ? g.hullMax : s.hull;
            float loadedSupply = (s.supply < 0f || Float.isNaN(s.supply)) ? g.supplyMax : s.supply;
            g.hull = Math.min(loadedHull, g.hullMax);
            g.supply = Math.min(loadedSupply, g.supplyMax);
            g.silver = s.silver;
            g.debt = Math.max(0, s.debt);
            g.cargoCap = s.cargoCap <= 0 ? Catalog.START_CARGO_CAP : s.cargoCap;
            g.warehouseLevel = Math.max(1, s.warehouseLevel);
            g.cannonLevel = Math.max(1, s.cannonLevel);
            g.cannonDamage = s.cannonDamage <= 0 ? Catalog.START_CANNON_DMG : s.cannonDamage;
            g.crew = Math.max(0, s.crew);
            g.crewCap = s.crewCap <= 0 ? Catalog.START_CREW_CAP : s.crewCap;
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
            g.dockedPort = s.dockedPort >= 0 && s.dockedPort < Catalog.PORTS.length ? s.dockedPort : -1;
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
            g.fishingOn = s.fishingOn && g.dockedPort == Catalog.YANGZHOU;
            g.fishTimer = 0f;
            g.questSellSilk = s.questSellSilk;
            g.questDialogueSeen = s.questDialogueSeen;
            g.sideQuestClaims = s.sideQuestClaims;
            if (s.questGoodsBought != null) System.arraycopy(s.questGoodsBought, 0, g.questGoodsBought, 0, Math.min(s.questGoodsBought.length, g.questGoodsBought.length));
            if (s.questGoodsSold != null) System.arraycopy(s.questGoodsSold, 0, g.questGoodsSold, 0, Math.min(s.questGoodsSold.length, g.questGoodsSold.length));
            g.questVisitPorts = s.questVisitPorts;
            g.questVisitPortSet = s.questVisitPortSet;
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
            // Local save checkpoints preserve position; transient combat and
            // navigation reset. A loaded save is always a playable dock snapshot
            // (失败状态不写回读档)。
            float coordinateScale=s.worldVersion<2?4f:s.worldVersion<3?2f:1f;
            g.x = (!Float.isNaN(s.x) && !Float.isInfinite(s.x)) ? s.x*coordinateScale : Catalog.PORT_X[lp]+Catalog.DOCK_RANGE-8;
            g.y = (!Float.isNaN(s.y) && !Float.isInfinite(s.y)) ? s.y*coordinateScale : Catalog.PORT_Y[lp];
            if(coordinateScale>1 && g.dockedPort>=0 && (!Float.isNaN(s.x) && !Float.isInfinite(s.x)) && (!Float.isNaN(s.y) && !Float.isInfinite(s.y))) {
                int port=g.dockedPort;
                // Move the harbor anchor, retaining the ship's local docking offset.
                g.x=Catalog.PORT_X[port]+s.x-Catalog.PORT_X[port]/coordinateScale;
                g.y=Catalog.PORT_Y[port]+s.y-Catalog.PORT_Y[port]/coordinateScale;
            }
            g.x=MathUtils.clamp(g.x,40,Catalog.WORLD_W-40);
            g.y=MathUtils.clamp(g.y,40,Catalog.WORLD_H-40);
            g.headingDeg = (!Float.isNaN(s.headingDeg) && !Float.isInfinite(s.headingDeg)) ? s.headingDeg : 0f;
            g.speed = 0;
            if (s.pirateAlive && finite(s.pirateX) && finite(s.pirateY)
                    && finite(s.pirateHp) && s.pirateHp > 0) {
                g.pirateAlive = true;
                g.pirateX = s.pirateX; g.pirateY = s.pirateY;
                g.pirateHeading = finite(s.pirateHeading) ? s.pirateHeading : 0;
                g.pirateHpMax = Catalog.PIRATE_HP; g.pirateHp = Math.min(s.pirateHp, g.pirateHpMax);
                g.pirateDamage = MathUtils.clamp(s.pirateDamage, 1, 10);
                g.pirateFireCd = .4f;
            }
            g.pirateSpawnTimer = finite(s.pirateSpawnTimer) ? Math.max(1, s.pirateSpawnTimer) : 12;
            if (s.merchant != null && s.merchant.ship >= 0 && s.merchant.ship < Catalog.SHIPS.length
                    && finite(s.merchant.x) && finite(s.merchant.y)
                    && finite(s.merchant.hp) && s.merchant.hp > 0) {
                g.merchant = s.merchant.copy();
                g.merchant.x = MathUtils.clamp(g.merchant.x, 80, Catalog.WORLD_W-80);
                g.merchant.y = MathUtils.clamp(g.merchant.y, 80, Catalog.WORLD_H-80);
                g.merchant.silver = MathUtils.clamp(g.merchant.silver, 0, 90);
                g.merchant.cargo = MathUtils.clamp(g.merchant.cargo, 0, 3);
                g.merchant.cargoGood = MathUtils.clamp(g.merchant.cargoGood, 0, Catalog.GOODS.length-1);
                g.merchant.damage = merchantDamage(g.merchant.ship);
                g.merchant.hpMax = merchantHull(g.merchant.ship);
                g.merchant.hp = Math.min(g.merchant.hp, g.merchant.hpMax);
            }
            g.merchantSpawnTimer = finite(s.merchantSpawnTimer) ? Math.max(1, s.merchantSpawnTimer) : 45;
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

    /** 抛锚条件：船在任一港口停靠范围或岛屿搜采范围内。纯查询。 */
    public boolean canAnchor() {
        return nearestPortInRange() >= 0 || nearestIslandInRange() >= 0;
    }

    /** 抛锚：仅港/岛范围内有效，停稳船便于稳定经营。 */
    public void dropAnchor() {
        if (!canAnchor() || worldPaused()) {
            toast(canAnchor() ? "菜单开着，世界已暂停。" : "离港口/岛屿太远，无法抛锚。");
            return;
        }
        anchored = true;
        stopAutoSail();
        speed = 0f;
        anchorBeat = 1f; // 0.28.26 抛锚顿挫
        toast("已抛锚，船停稳了，可以安心经营。再点一次起锚。");
    }

    /** 起锚：恢复自由航行（漂移/滑行）。 */
    public void weighAnchor() {
        if (!anchored) {
            return;
        }
        anchored = false;
        anchorBeat = 1f; // 0.28.26 起锚顿挫
        toast("起锚，恢复航行。");
    }

    /** 每帧锚检查：锚只在港/岛范围内维持；范围外或主动驱动则自动脱锚。 */
    private void updateAnchor() {
        if (anchored && !canAnchor()) {
            anchored = false;
            return;
        }
        if (!anchored) {
            return;
        }
        // 主动开船（明显推杆/按加速）= 起锚，避免摇杆“失灵”的死锁感。
        if (holdAccel || Math.abs(steerInput) > 0.3f || thrustInput > 0.3f) {
            anchored = false;
            toast("起锚，恢复航行。");
            return;
        }
        steerInput = 0f;
        thrustInput = 0f;
        holdAccel = holdDecel = false;
        manualHeadingActive = false;
        stopAutoSail();
        speed = 0f;
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
        // 0.28.17: 抛锚时每帧停稳；被推出锚地则自动起锚。
        updateAnchor();
        applySteerAndSpeed(dt);
        applyDockAssist(dt); // 0.28.26 近港对位磁吸
        move(dt);
        if(failed) return;
        // 0.28.21: 未抛锚且停在港/岛范围内时，海流缓慢把船推离岸边。
        applyCoastalDrift(dt);
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
        }
        updateMerchant(dt);
        // 0.28.22: 远处带 soft-cap 刷新 —— 2 商船 + 2 战船，每次判定约 25% 概率。
        updateTraderRoster(dt);
        updateWarshipRoster(dt);
        updatePlayerFire(dt);
        updateBalls(dt);
        updateShipImpacts(dt);
        if (failed) return;
        // 0.27.2: 靠近港口/岛屿不再自动靠泊/登岛（tryApproach 已移除）——
        // 玩家必须在范围内点击港口/岛屿图标才会打开对应菜单。自动航行
        // 抵达目标时停船等待，由界面提示点击。
        if (autoSail && autoSailPort >= 0
                && Catalog.dist(x, y, Catalog.PORT_X[autoSailPort], Catalog.PORT_Y[autoSailPort]) < Catalog.DOCK_RANGE) {
            stopAutoSail();
            speed = 0f;
        } else if (autoSail && autoSailIsle >= 0
                && Catalog.dist(x, y, Catalog.ISLAND_X[autoSailIsle], Catalog.ISLAND_Y[autoSailIsle]) < Catalog.ISLAND_RANGE) {
            stopAutoSail();
            speed = 0f;
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
            float want = autoSailCourse(tx, ty); // 0.27.4: steer around land
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
                headingDeg -= steerInput * Catalog.TURN_RATE * turnMult() * dt;
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
            // 0.28.17 摇杆油门：默认好开 —— 杆量即目标航速，柔和趋近；
            // 0.28.26 松杆：滑行减速曲线 —— 高速自然带阻尼滑行，低速缓慢停下，
            // 不到 2 单位/秒直接停稳，既不瞬间急停也不拖泥带水。
            float target = thrustTarget(max);
            if (speed < target) {
                speed = Math.min(target, speed + Catalog.ACCEL * dt);
            } else {
                float pull = thrustInput < -0.3f ? Catalog.COAST * 2.2f : Catalog.COAST;
                float natural = pull + speed * 0.12f; // speed-proportional damping
                speed = Math.max(target, speed - natural * dt);
                if (target <= 0.01f && speed < 2f) speed = 0f;
            }
        }
        speed = MathUtils.clamp(speed, 0f, max);
    }

    /** 摇杆油门的目标航速：向前推 = 油门（带小死区），向后拉 = 轻减速，不产生倒退目标。 */
    private float thrustTarget(float max) {
        if (thrustInput > 0.12f) {
            return max * Math.min(1f, thrustInput);
        }
        return 0f;
    }

    /** 0.28.21 近岸漂流：不抛锚的船在港/岛范围内会被海流持续推离（约几秒内
     * 明显滑出交互范围），抛锚则完全停稳。方向固定为离最近陆地中心向外，
 * 与陆地碰撞解算同向，不会把船推进岸里；公海不受影响。 */
    private void applyCoastalDrift(float dt) {
        if (anchored || autoSail || dockedPort >= 0 || islandMenu >= 0) return;
        if (nearestPortInRange() < 0 && nearestIslandInRange() < 0) return;
        float dx = 1f, dy = 0f, best = Float.MAX_VALUE;
        for (int i = 0; i < Catalog.PORTS.length; i++) {
            float d = Catalog.dist(x, y, Catalog.PORT_X[i], Catalog.PORT_Y[i]);
            if (d < best) { best = d; dx = x - Catalog.PORT_X[i]; dy = y - Catalog.PORT_Y[i]; }
        }
        for (int i = 0; i < Catalog.ISLANDS.length; i++) {
            float d = Catalog.dist(x, y, Catalog.ISLAND_X[i], Catalog.ISLAND_Y[i]);
            if (d < best) { best = d; dx = x - Catalog.ISLAND_X[i]; dy = y - Catalog.ISLAND_Y[i]; }
        }
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) { dx = 1f; dy = 0f; len = 1f; }
        float push = COASTAL_DRIFT_SPEED * dt / len;
        x = MathUtils.clamp(x + dx * push, 40f, Catalog.WORLD_W - 40f);
        y = MathUtils.clamp(y + dy * push, 40f, Catalog.WORLD_H - 40f);
    }

    /** 0.28.26 近港对位磁吸：低速时轻微拉向停靠圈中心，靠泊不再来回打转。
     * 只在松杆、距离已近且朝向大致对准陆地时生效；力度很小，永不瞬移。 */
    private static final float DOCK_ASSIST_RANGE = 300f;   // 仅在停靠圈外圈内生效
    private static final float DOCK_ASSIST_SPEED = 14f;    // 单位/秒，轻微
    private void applyDockAssist(float dt) {
        if (anchored || autoSail || dockedPort >= 0 || islandMenu >= 0 || failed) return;
        if (speed > 45f) return;
        if (holdAccel || Math.abs(steerInput) > 0.25f || thrustInput > 0.25f) return;
        int p = nearestPortInRange(), isle = nearestIslandInRange();
        float tx, ty;
        if (p >= 0) { tx = Catalog.PORT_X[p]; ty = Catalog.PORT_Y[p]; }
        else if (isle >= 0) { tx = Catalog.ISLAND_X[isle]; ty = Catalog.ISLAND_Y[isle]; }
        else return;
        float d = Catalog.dist(x, y, tx, ty);
        if (d > DOCK_ASSIST_RANGE || d < 24f) return;
        float ux = (tx - x) / d, uy = (ty - y) / d;
        x += ux * DOCK_ASSIST_SPEED * dt;
        y += uy * DOCK_ASSIST_SPEED * dt;
    }

    /** 近岸漂流速度（单位/秒）：DOCK_RANGE 218，几秒内能明显滑出范围。 */
    private static final float COASTAL_DRIFT_SPEED = 20f;

    // ================= 0.28.26 炮击轻反馈（开炮小后坐 + 炮口闪） =================
    /** 玩家开炮时的瞬时反馈时钟（>0 表示反馈进行中）。 */
    public float cannonKick;    // 后坐/震感强度 0..1（约 0.18s 衰减）
    public float muzzleFlash;   // 炮口闪强度 0..1（约 0.12s 衰减）
    /** 0.28.26 抛锚/起锚顿挫：>0 时给镜头一个短促下沉再回弹。 */
    public float anchorBeat;
    /** 玩家每开一炮调用一次（真实开炮成功时）。 */
    private void pokeCannonFeedback() {
        cannonKick = 1f;
        muzzleFlash = 1f;
    }
    private void updateCannonFeedback(float dt) {
        if (cannonKick > 0f) cannonKick = Math.max(0f, cannonKick - dt / 0.18f);
        if (muzzleFlash > 0f) muzzleFlash = Math.max(0f, muzzleFlash - dt / 0.12f);
    }

    // ================= 0.28.22 沉船表现 =================
    /** 沉没动画时长：倾侧 + 下沉，之后移除（从不瞬间消失）。 */
    private static final float SINK_SECONDS = 2.2f;

    /** 记录一艘沉船并开始下沉动画（位置保持不变，倾侧渐增，整体没入水中）。 */
    private void addWreck(float wx, float wy, float heading, int shipIndex) {
        addWreck(wx,wy,heading,shipIndex,false,false);
    }
    private void addWreck(float wx,float wy,float heading,int shipIndex,boolean player,boolean pirate) {
        for (int i = 0; i < wrecks.length; i++) {
            if (wrecks[i] != null && wrecks[i].alive) continue;
            Wreck w = wrecks[i] == null ? new Wreck() : wrecks[i];
            w.x = wx; w.y = wy; w.headingDeg = heading; w.ship = shipIndex;
            w.duration = SINK_SECONDS; w.timer = SINK_SECONDS;
            w.tilt = 0f; w.submerge = 0f; w.sway = MathUtils.random(-14f, 14f);
            w.alive = true; w.player=player; w.pirate=pirate;
            wrecks[i] = w; sinkFocus=w;
            playSinkSfx();
            return;
        }
    }

    private void updateWrecks(float dt) {
        for (int i = 0; i < wrecks.length; i++) {
            Wreck w = wrecks[i];
            if (w == null || !w.alive) continue;
            w.timer = Math.max(0f, w.timer - dt);
            float p = 1f - w.timer / w.duration; // 0..1
            w.tilt = Math.min(72f, p * 115f);                  // 越沉越倾侧
            w.submerge = MathUtils.clamp((p-.16f)/.84f,0,1); w.submerge *= w.submerge;                                // 后半段沉得更快
            if (w.timer <= 0f) w.alive = false;
        }
    }

    private void playSinkSfx() {
        VoyageAudio a = VoyageAudio.get();
        if (a != null) a.playSink();
    }

    // ================= 0.28.22 掠夺结算（弹窗数据） =================
    private void pushLootPopup(String title, int silver, String good, int goodCount, String extra) {
        int slot=lootPopup[0]==null?0:1;
        LootGain gain = new LootGain(); lootPopup[slot] = gain;
        if(slot==0) lootDelay=1.1f;
        gain.title = title; gain.silver = silver; gain.good = good;
        gain.goodCount = goodCount; gain.extra = extra == null ? "" : extra;
        lootPopupT = 6f; // 弹窗未处理时保留一段时间
    }

    /** 界面每帧调用：取走并清空当前掠夺结算。 */
    public LootGain pollLootPopup() {
        if(lootDelay>0) return null;
        LootGain gain = lootPopup[0];
        lootPopup[0] = lootPopup[1];
        lootPopup[1] = null;
        lootPopupT = 0f;
        return gain;
    }

    // ================= 0.28.22 船体碰撞（撞船） =================
    /** 撞击冷却：一对船碰一次后 3 秒内不再结算，一次擦碰不等于连击。 */
    private static final float RAM_COOLDOWN_SECONDS = 3f;
    /** 战斗中战船主动撞船的概率（每次冷却转好后的判定）—— 不是 100%，修船很贵。 */
    private static final float WARSHIP_RAM_CHANCE = 0.25f;

    /** 原有单艘商船的撞击伤害结算也走这条路径（复用冷却字段）。 */
    private float ramRamCd;
    private float ramTraderCd;
    private final float[] ramWarshipCd = new float[2];

    /** 受击者标识：撞击与伤害按实体寻址，避免用坐标反查撞到谁。 */
    private static final class Victim {
        static final int PIRATE = 1, MERCHANT = 2, TRADER = 3, WARSHIP = 4;
        final int kind, index;
        Victim(int kind, int index) { this.kind = kind; this.index = index; }
    }

    private static final Victim victimPirate() { return new Victim(Victim.PIRATE, 0); }
    private static final Victim victimMerchant() { return new Victim(Victim.MERCHANT, 0); }
    private static final Victim victimTrader(int i) { return new Victim(Victim.TRADER, i); }
    private static final Victim victimWarship(int i) { return new Victim(Victim.WARSHIP, i); }

    private float npcHp(Victim v) {
        switch (v.kind) {
            case Victim.PIRATE: return pirateAlive ? pirateHp : 0f;
            case Victim.MERCHANT: return merchant != null ? merchant.hp : 0f;
            case Victim.TRADER:
                TraderData t = v.index < traders.length ? traders[v.index] : null;
                return t != null && t.alive ? t.hp : 0f;
            default:
                WarshipData w = v.index < warships.length ? warships[v.index] : null;
                return w != null && w.alive ? w.hp : 0f;
        }
    }

    private float npcHpMax(Victim v) {
        switch (v.kind) {
            case Victim.PIRATE: return Catalog.PIRATE_HP;
            case Victim.MERCHANT: return merchant != null ? merchant.hpMax : 0f;
            case Victim.TRADER:
                TraderData t = v.index < traders.length ? traders[v.index] : null;
                return t != null ? traderHull(t.ship) : 0f;
            default:
                WarshipData w = v.index < warships.length ? warships[v.index] : null;
                return w != null ? warshipHull(w.ship) : 0f;
        }
    }

    private int npcShipIndex(Victim v) {
        switch (v.kind) {
            case Victim.PIRATE: return VoyageGeometry.PIRATE_SHIP;
            case Victim.MERCHANT: return merchant != null ? merchant.ship : 0;
            case Victim.TRADER:
                TraderData t = v.index < traders.length ? traders[v.index] : null;
                return t != null ? t.ship : 0;
            default:
                WarshipData w = v.index < warships.length ? warships[v.index] : null;
                return w != null ? w.ship : 0;
        }
    }

    private float npcX(Victim v) {
        switch (v.kind) {
            case Victim.PIRATE: return pirateX;
            case Victim.MERCHANT: return merchant.x;
            case Victim.TRADER: return traders[v.index].x;
            default: return warships[v.index].x;
        }
    }

    private float npcY(Victim v) {
        switch (v.kind) {
            case Victim.PIRATE: return pirateY;
            case Victim.MERCHANT: return merchant.y;
            case Victim.TRADER: return traders[v.index].y;
            default: return warships[v.index].y;
        }
    }

    /** 伤害入口：扣血，沉船时给残骸 +（玩家火炮击沉时）掠夺结算。 */
    private void damageNpc(Victim v, float dmg) {
        if (dmg <= 0f) return;
        hurtTime[hitSlot(v)]=.3f;
        switch (v.kind) {
            case Victim.PIRATE:
                if (!pirateAlive) return;
                pirateHp -= dmg;
                if (pirateHp <= 0f) { addWreck(pirateX, pirateY, pirateHeading, VoyageGeometry.PIRATE_SHIP,false,true); clearPirate(); }
                return;
            case Victim.MERCHANT:
                if (merchant == null) return;
                merchant.hp -= dmg;
                if (merchant.hp <= 0f) sinkMerchant(false);
                return;
            case Victim.TRADER:
                TraderData t = v.index < traders.length ? traders[v.index] : null;
                if (t == null || !t.alive) return;
                t.hostile = true; // 被打/被撞后警觉：还击并逃离
                t.hp -= dmg;
                if (t.hp <= 0f) despawnTrader(v.index, true);
                return;
            default:
                WarshipData w = v.index < warships.length ? warships[v.index] : null;
                if (w == null || !w.alive) return;
                w.provoked = true; // 0.28.24: 被打/被撞后记仇：追击并还击
                w.hp -= dmg;
                if (w.hp <= 0f) despawnWarship(v.index, true);
        }
    }

    /** 撞击伤害规则（文档化选择）：小船掉自身上限 20%，大船掉自身上限 10%，
     * 同大小各 15% —— 力的作用是相互的，即使大撞小也承受损伤。撞击击沉
     * 不算「亲手击沉」，不产生掠夺收益。 */
    private int ramDamageSelf(float selfMax, float selfSize, float otherSize) {
        if (selfSize > otherSize * 1.06f) return pct(selfMax, .10f);
        if (otherSize > selfSize * 1.06f) return pct(selfMax, .20f);
        return pct(selfMax, .15f);
    }

    private static int pct(float value, float fraction) {
        return Math.max(1, Math.round(value * fraction));
    }

    /** 玩家与一名 NPC 的对撞：双方按大小结算，冷却 3 秒。 */
    private void ramPlayerVs(Victim other) {
        float otherMax = npcHpMax(other);
        if (otherMax <= 0f) return;
        float playerSize = VoyageGeometry.ship(ship).radius();
        float otherSize = VoyageGeometry.ship(npcShipIndex(other)).radius();
        contactFeedback(0,hitSlot(other),x,y,npcX(other),npcY(other));
        hull = Math.max(0f, hull - ramDamageSelf(hullMax, playerSize, otherSize));
        damageNpc(other, ramDamageSelf(otherMax, otherSize, playerSize));
        playCannonSfx(); // 撞击重响
        toast("撞击！船体受损。");
        if (hull <= 0f) fail("船沉");
    }

    /** NPC 互撞：双方按大小结算（海盗/战船与商船交战擦碰）。 */
    private void ramNpcVs(Victim a, Victim b) {
        float aMax = npcHpMax(a), bMax = npcHpMax(b);
        if (aMax <= 0f || bMax <= 0f) return;
        float aSize = VoyageGeometry.ship(npcShipIndex(a)).radius();
        float bSize = VoyageGeometry.ship(npcShipIndex(b)).radius();
        contactFeedback(hitSlot(a),hitSlot(b),npcX(a),npcY(a),npcX(b),npcY(b));
        damageNpc(a, ramDamageSelf(aMax, aSize, bSize));
        damageNpc(b, ramDamageSelf(bMax, bSize, aSize));
    }

    private boolean warshipHostile(WarshipData w) {
        // 0.28.24: 被玩家撞过/击伤的战船记仇（锁定等效），转入战斗语境。
        return w.provoked || pirateAlive || (merchant != null && merchant.hostile);
    }

    /** 每帧检测玩家与所有 NPC、NPC 互相之间的船体接触；接触即结算一次撞击伤害。 */
    private void updateShipImpacts(float dt) {
        ramRamCd = Math.max(0f, ramRamCd - dt);
        ramTraderCd = Math.max(0f, ramTraderCd - dt);
        for (int i = 0; i < ramWarshipCd.length; i++) ramWarshipCd[i] = Math.max(0f, ramWarshipCd[i] - dt);
        float playerR = VoyageGeometry.ship(ship).radius();

        // 玩家 vs 原有单艘商船 / 海盗（共用一对冷却，同时接触只结算一次）
        if (ramRamCd <= 0f) {
            if (merchant != null && Catalog.dist(x, y, merchant.x, merchant.y)
                    < playerR + VoyageGeometry.ship(merchant.ship).radius()) {
                ramPlayerVs(victimMerchant());
                ramRamCd = RAM_COOLDOWN_SECONDS;
            } else if (pirateAlive && Catalog.dist(x, y, pirateX, pirateY)
                    < playerR + VoyageGeometry.ship(VoyageGeometry.PIRATE_SHIP).radius()) {
                ramPlayerVs(victimPirate());
                ramRamCd = RAM_COOLDOWN_SECONDS;
            }
        }
        // 玩家 vs 货船编队
        for (int i = 0; i < traders.length && ramTraderCd <= 0f; i++) {
            TraderData t = traders[i];
            if (t == null || !t.alive) continue;
            if (Catalog.dist(x, y, t.x, t.y) < playerR + VoyageGeometry.ship(t.ship).radius()) {
                ramPlayerVs(victimTrader(i));
                ramTraderCd = RAM_COOLDOWN_SECONDS;
            }
        }
        // 玩家 vs 战船编队（交战中的战船还可能概率性主动撞船）
        for (int i = 0; i < warships.length; i++) {
            WarshipData w = warships[i];
            if (w == null || !w.alive) continue;
            float d = Catalog.dist(x, y, w.x, w.y);
            if (d < playerR + VoyageGeometry.ship(w.ship).radius()) {
                if (ramWarshipCd[i] <= 0f) {
                    ramPlayerVs(victimWarship(i));
                    ramWarshipCd[i] = RAM_COOLDOWN_SECONDS;
                }
            } else if (warshipHostile(w) && w.hp > w.hpMax * .5f && d < Catalog.PIRATE_RANGE * 0.45f
                    && ramWarshipCd[i] <= 0f) {
                // 概率性主动撞船：不是每次都撞（修船很贵）。
                ramWarshipCd[i] = RAM_COOLDOWN_SECONDS;
                if (MathUtils.random() < WARSHIP_RAM_CHANCE) {
                    // 冲刺贴帮后结算（力的作用是相互的）。
                    if (d > 1f) {
                        float push = Math.min(90f, d - playerR - VoyageGeometry.ship(w.ship).radius() * 0.5f);
                        if (push > 0f) { w.x += (x - w.x) / d * push; w.y += (y - w.y) / d * push; }
                    }
                    ramPlayerVs(victimWarship(i));
                }
            }
        }
        // NPC 互撞：海盗/战船 与 商船/货船 接触擦碰（只在同屏交战语境发生）。
        if (pirateAlive && merchant != null && ramRamCd <= 0f
                && Catalog.dist(pirateX, pirateY, merchant.x, merchant.y)
                        < VoyageGeometry.ship(VoyageGeometry.PIRATE_SHIP).radius()
                                + VoyageGeometry.ship(merchant.ship).radius()) {
            ramNpcVs(victimPirate(), victimMerchant());
            ramRamCd = RAM_COOLDOWN_SECONDS;
        }
        for (int i = 0; i < warships.length; i++) {
            WarshipData w = warships[i];
            if (w == null || !w.alive || ramWarshipCd[i] > 0f) continue;
            float wr = VoyageGeometry.ship(w.ship).radius();
            boolean hit = false;
            if (merchant != null && Catalog.dist(w.x, w.y, merchant.x, merchant.y)
                    < wr + VoyageGeometry.ship(merchant.ship).radius()) {
                ramNpcVs(victimWarship(i), victimMerchant());
                hit = true;
            } else {
                for (int j = 0; j < traders.length; j++) {
                    TraderData t = traders[j];
                    if (t == null || !t.alive) continue;
                    if (Catalog.dist(w.x, w.y, t.x, t.y) < wr + VoyageGeometry.ship(t.ship).radius()) {
                        ramNpcVs(victimWarship(i), victimTrader(j));
                        hit = true;
                        break;
                    }
                }
            }
            if (hit) ramWarshipCd[i] = RAM_COOLDOWN_SECONDS;
        }
    }

    /** Apply a bounded mutual separation impulse to every live NPC hull. */
    private void separateLiveShips() {
        if (pirateAlive) ensurePirateSeparation();
        if (merchant != null) separateFrom(merchant);
        for (TraderData t: traders) if (t != null && t.alive) separateFrom(t);
        for (WarshipData w: warships) if (w != null && w.alive) separateFrom(w);
    }

    /** 0.28.24 玩家与 NPC 的互推位移结果（NPC 侧），separateMutual 写入。 */
    private final float[] sepNpc = new float[2];

    /** 0.28.24: 船体重叠时双方都沿接触法线分开（玩家 45% / NPC 55%，单步上限
     * 12 单位）——此前只推玩家导致撞船后粘船。NPC 位移写入 sepNpc。 */
    private void separateMutual(float ox, float oy, float otherR) {
        sepNpc[0] = 0f; sepNpc[1] = 0f;
        float dx=x-ox, dy=y-oy, d=(float)Math.sqrt(dx*dx+dy*dy);
        float limit=VoyageGeometry.ship(ship).radius()+otherR;
        if (d>=limit || d<.001f) return;
        float nx=dx/d, ny=dy/d, step=Math.min(12f, limit-d), ps=step*.45f, ns=step-ps;
        x=MathUtils.clamp(x+nx*ps,40f,Catalog.WORLD_W-40f);
        y=MathUtils.clamp(y+ny*ps,40f,Catalog.WORLD_H-40f);
        sepNpc[0]=-nx*ns; sepNpc[1]=-ny*ns;
    }
    private void separateFrom(MerchantData m) {
        separateMutual(m.x, m.y, VoyageGeometry.ship(m.ship).radius());
        m.x=MathUtils.clamp(m.x+sepNpc[0],40f,Catalog.WORLD_W-40f);
        m.y=MathUtils.clamp(m.y+sepNpc[1],40f,Catalog.WORLD_H-40f);
    }
    private void separateFrom(TraderData t) {
        separateMutual(t.x, t.y, VoyageGeometry.ship(t.ship).radius());
        t.x=MathUtils.clamp(t.x+sepNpc[0],40f,Catalog.WORLD_W-40f);
        t.y=MathUtils.clamp(t.y+sepNpc[1],40f,Catalog.WORLD_H-40f);
    }
    private void separateFrom(WarshipData w) {
        separateMutual(w.x, w.y, VoyageGeometry.ship(w.ship).radius());
        w.x=MathUtils.clamp(w.x+sepNpc[0],40f,Catalog.WORLD_W-40f);
        w.y=MathUtils.clamp(w.y+sepNpc[1],40f,Catalog.WORLD_H-40f);
    }

    private void move(float dt) {
        ensureLandClearance();
        updateShipImpacts(0);
        if(failed) return;
        separateLiveShips();
        // Bounded travel steps prevent crossing an entire obstacle during a slow frame.
        int steps = Math.max(1, (int)Math.ceil(speed * Math.max(0, dt) / 2f));
        float step = Math.max(0, dt) / steps;
        for (int n=0; n<steps; n++) {
            x += MathUtils.cosDeg(headingDeg) * speed * step;
            y += MathUtils.sinDeg(headingDeg) * speed * step;
            x = MathUtils.clamp(x, 40f, Catalog.WORLD_W - 40f);
            y = MathUtils.clamp(y, 40f, Catalog.WORLD_H - 40f);
            ensureLandClearance();
            x = MathUtils.clamp(x, 40f, Catalog.WORLD_W - 40f);
            y = MathUtils.clamp(y, 40f, Catalog.WORLD_H - 40f);
            updateShipImpacts(0);
            if(failed) return;
            separateLiveShips();
        }
    }

    /** Also called before rendering paused/loaded voyages and newly equipped ships. */
    public void ensureLandClearance() {
        for (int i=0; i<Catalog.PORTS.length; i++)
            resolveCollision(Catalog.PORT_X[i], Catalog.PORT_Y[i], VoyageGeometry.landRadius(true,i));
        for (int i=0; i<Catalog.ISLANDS.length; i++)
            resolveCollision(Catalog.ISLAND_X[i], Catalog.ISLAND_Y[i], VoyageGeometry.landRadius(false,i));
    }

    /** Recover initial overlap or a larger equipped hull, also while the world is paused. */
    public void ensurePirateSeparation() {
        if(!pirateAlive) return;
        float limit=VoyageGeometry.pirateSeparation(ship);
        if(Catalog.dist(x,y,pirateX,pirateY)>=limit-.001f) return;
        float angle=Catalog.dist(x,y,pirateX,pirateY)<.001f ? headingDeg+180
                : MathUtils.atan2(pirateY-y,pirateX-x)*MathUtils.radiansToDegrees;
        // Continuous mutual hull force: both bodies move apart in small bounded steps.
        // Never teleport the pirate to a ring; the player is pushed back too.
        float need=limit-Catalog.dist(x,y,pirateX,pirateY);
        float step=Math.min(6f,need);
        float playerShare=step*.45f, pirateShare=step-playerShare;
        x=MathUtils.clamp(x-MathUtils.cosDeg(angle)*playerShare,40f,Catalog.WORLD_W-40f);
        y=MathUtils.clamp(y-MathUtils.sinDeg(angle)*playerShare,40f,Catalog.WORLD_H-40f);
        pirateX=MathUtils.clamp(pirateX+MathUtils.cosDeg(angle)*pirateShare,40f,Catalog.WORLD_W-40f);
        pirateY=MathUtils.clamp(pirateY+MathUtils.sinDeg(angle)*pirateShare,40f,Catalog.WORLD_H-40f);
    }
    private boolean pirateWaterClear(float px,float py) {
        float r=VoyageGeometry.ship(VoyageGeometry.PIRATE_SHIP).radius();
        if(px<r || py<r || px>Catalog.WORLD_W-r || py>Catalog.WORLD_H-r) return false;
        for(int i=0;i<Catalog.PORTS.length;i++)
            if(Catalog.dist(px,py,Catalog.PORT_X[i],Catalog.PORT_Y[i])<r+VoyageGeometry.landRadius(true,i)) return false;
        for(int i=0;i<Catalog.ISLANDS.length;i++)
            if(Catalog.dist(px,py,Catalog.ISLAND_X[i],Catalog.ISLAND_Y[i])<r+VoyageGeometry.landRadius(false,i)) return false;
        return true;
    }

    /** Push the ship out of an obstacle circle and kill the inward velocity so
     * it stops and slides tangentially. Boundary = obstacle radius + ship radius. */
    private void resolveCollision(float ox, float oy, float hitR) {
        float dx = x - ox, dy = y - oy;
        float limit = hitR + VoyageGeometry.ship(ship).radius();
        float d2 = dx * dx + dy * dy;
        if (d2 >= limit * limit) {
            return;
        }
        float d = (float) Math.sqrt(d2);
        float nx, ny;
        if (d < 0.001f) { // dead center (e.g. a legacy save parked on a port): push east
            nx = 1f;
            ny = 0f;
        } else {
            nx = dx / d;
            ny = dy / d;
        }
        x = ox + nx * limit;
        y = oy + ny * limit;
        float vx = MathUtils.cosDeg(headingDeg) * speed;
        float vy = MathUtils.sinDeg(headingDeg) * speed;
        float vn = vx * nx + vy * ny; // radial component (negative = into the land)
        if (vn < 0f) {
            vx -= vn * nx;
            vy -= vn * ny;
            speed = (float) Math.sqrt(vx * vx + vy * vy);
            if (speed > 0.5f) {
                headingDeg = MathUtils.atan2(vy, vx) * MathUtils.radiansToDegrees;
            } else {
                speed = 0f; // head-on impact: stop dead against the land
            }
        }
    }

    /** 0.27.4: auto-sail course to (tx,ty) deflected around any port/island
     * circle currently blocking the straight lane (same circular hitboxes as
     * the collision model). The destination itself is never avoided. */
    private float autoSailCourse(float tx, float ty) {
        float dx = tx - x, dy = ty - y;
        float targetDist = (float) Math.sqrt(dx * dx + dy * dy);
        if (targetDist < 1f) {
            return MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        }
        float ux = dx / targetDist, uy = dy / targetDist;
        float nearestT = Float.MAX_VALUE;
        float avoidDeg = Float.NaN;
        for (int i = 0; i < Catalog.PORTS.length; i++) {
            if (autoSailPort >= 0 && i == autoSailPort) continue;
            float[] hit = avoidCheck(Catalog.PORT_X[i], Catalog.PORT_Y[i],
                    VoyageGeometry.landRadius(true,i), ux, uy, targetDist);
            if (hit != null && hit[0] < nearestT) {
                nearestT = hit[0];
                avoidDeg = hit[1];
            }
        }
        for (int i = 0; i < Catalog.ISLANDS.length; i++) {
            if (autoSailIsle >= 0 && i == autoSailIsle) continue;
            float[] hit = avoidCheck(Catalog.ISLAND_X[i], Catalog.ISLAND_Y[i],
                    VoyageGeometry.landRadius(false,i), ux, uy, targetDist);
            if (hit != null && hit[0] < nearestT) {
                nearestT = hit[0];
                avoidDeg = hit[1];
            }
        }
        if (!Float.isNaN(avoidDeg)) {
            return avoidDeg;
        }
        return MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
    }

    /** Returns {distanceAlongLane, courseDeg} when this circle blocks the lane,
     * else null. The course heads at the tangent point on the side away from
     * the obstacle's offset, so the ship rounds it and resumes the direct line. */
    private float[] avoidCheck(float ox, float oy, float hitR, float ux, float uy, float targetDist) {
        float r = hitR + VoyageGeometry.ship(ship).radius() + 24f; // hull clearance margin
        float oxr = ox - x, oyr = oy - y;
        float t = oxr * ux + oyr * uy;               // distance along the lane
        if (t < 40f || t > targetDist - 20f) return null; // behind / past the target
        float lateral = oxr * uy - oyr * ux;         // signed offset from the lane
        if (Math.abs(lateral) >= r) return null;     // lane is clear here
        float side = lateral >= 0f ? -1f : 1f;
        float wpx = ox + uy * side * r, wpy = oy - ux * side * r;
        float deg = MathUtils.atan2(wpy - y, wpx - x) * MathUtils.radiansToDegrees;
        return new float[]{t, deg};
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

    /** 附近在停靠范围内的港口（纯查询，不改变任何状态），否则 -1。 */
    public int nearestPortInRange() {
        for (int i = 0; i < Catalog.PORTS.length; i++) {
            if (Catalog.dist(x, y, Catalog.PORT_X[i], Catalog.PORT_Y[i]) < Catalog.DOCK_RANGE) {
                return i;
            }
        }
        return -1;
    }

    /** 附近在搜采范围内的岛屿（纯查询，不改变任何状态），否则 -1。 */
    public int nearestIslandInRange() {
        for (int i = 0; i < Catalog.ISLANDS.length; i++) {
            if (Catalog.dist(x, y, Catalog.ISLAND_X[i], Catalog.ISLAND_Y[i]) < Catalog.ISLAND_RANGE) {
                return i;
            }
        }
        return -1;
    }

    /** 玩家点击岛屿图标主动登岛（仅当船在搜采范围内时由界面调用）。 */
    public void enterIsland(int idx) {
        if (idx < 0 || idx >= Catalog.ISLANDS.length) {
            return;
        }
        islandMenu = idx;
        islandGathered = false;
        speed = 0f;
        stopAutoSail();
        toast("登岛「" + Catalog.ISLANDS[idx] + "」，可搜采。世界暂停。");
    }

    /** 关闭港口菜单：不传送、不吸附，船留在原地，世界继续模拟。 */
    public void undockInPlace() {
        if (dockedPort < 0) {
            return;
        }
        dockedPort = -1;
        // 0.26.3: 离港即停捕鱼（渔夫只能在家门口作业）。
        fishingOn = false;
        fishTimer = 0f;
        leaveCooldown = 2.2f;
    }

    /** 关闭岛屿菜单：不传送、不吸附，船留在原地，世界继续模拟。 */
    public void leaveIslandInPlace() {
        if (islandMenu < 0) {
            return;
        }
        islandMenu = -1;
        leaveCooldown = 2.2f;
    }

    public void dock(int port) {
        dockedPort = port;
        lastPort = port;
        islandMenu = -1;
        speed = 0f;
        anchored = false;
        stopAutoSail();
        cancelLock(); // Docking pauses traffic; it does not erase nearby ships.
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
        anchored = false;
        leaveCooldown = 2.2f;
        toast("离港。欠债不挡出航。");
    }

    public void leaveIsland() {
        if (islandMenu < 0) {
            return;
        }
        islandMenu = -1;
        anchored = false;
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
        questGoodsBought[good] += qty;
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
        questGoodsSold[good] += qty;
        silver += gain;
        playCoinSfx();
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
        playCoinSfx();
        return "卖掉 " + Catalog.BEASTS[idx] + " x" + qty + "，得 " + gain + " 两。";
    }

    public String sellHerb(int idx, int qty) {
        if (qty <= 0 || herbs[idx] < qty) {
            return "没有这种草药。";
        }
        int gain = Catalog.HERB_PRICE[idx] * qty;
        herbs[idx] -= qty;
        silver += gain;
        playCoinSfx();
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
        playCoinSfx();
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

    /** Fill the current voyage for local testing/showcase without discarding cargo. */
    public void fillAccount() {
        for (int i=0; i<Catalog.SHIPS.length; i++) shipOwned |= 1 << i;
        ship = Catalog.SHIPS.length - 1;
        for (int i=0; i<beasts.length; i++) {
            beastFound[i] = true;
            beasts[i] = Math.max(1, beasts[i]);
        }
        for (int i=0; i<herbs.length; i++) {
            herbFound[i] = true;
            herbs[i] = Math.max(1, herbs[i]);
        }
        // Base capacity suffices even when switching back to the starter ship.
        cargoCap = Math.max(cargoCap, cargoUsed());
        silver = Math.max(silver, 99999);
        hull = hullMax;
        supply = supplyMax;
        crew = crewMax();
        questBeastsFound = Math.max(questBeastsFound, beasts.length);
        questSilverPeak = Math.max(questSilverPeak, silver);
        failed = false;
        failReason = "";
        ensureLandClearance();
        // No fish discovery flags exist; existing fish and trade cargo are preserved.
    }

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
        if (worldPaused()) {
            return;
        }
        autoSail = true;
        autoSailPort = port;
        autoSailIsle = -1;
        toast("自动驶向 " + Catalog.PORTS[port]);
    }

    /** Full-map tap on an island: sail there; arriving stops the ship and the
     * player taps the island icon to open the search menu (0.27.2). */
    public void startAutoSailIsle(int idx) {
        if (worldPaused()) {
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

    /** Clears auto-sail without a toast (arrival, docking, failure or manual control). */
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
    }    /** Player hits retain one damage; cannon upgrades and roster fire bonuses improve reload.
     * 0.28.18: slowed ~4x so the short cannon SFX can finish between shots.
     * Per-shot damage is unchanged; upgrades still raise damage, not rate. */
    public float fireInterval() {
        float rate = 1f + 0.15f * Math.max(0, cannonLevel - 1) + 0.04f * Math.max(0, crew - 1);
        float shipFire = 1f + Catalog.SHIP_FIRE[ship] / 100f;
        return Math.max(0.6f, Catalog.FIRE_INTERVAL * 3.6f / (rate * shipFire));
    }

    /** 0.28.18 cannon SFX hook; safe no-op when audio is unavailable. */
    public void playCannonSfx() {
        VoyageAudio a = VoyageAudio.get();
        if (a != null) a.playCannon();
    }

    /** 0.28.18 coin SFX hook; safe no-op when audio is unavailable. */
    public void playCoinSfx() {
        VoyageAudio a = VoyageAudio.get();
        if (a != null) a.playCoin();
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

    /** Focus the pirate; merchants can independently remain hostile.
     * 0.28.24: 锁定即追击 —— combatLock 让海盗主动逼近玩家（半血后改为边打边逃）。 */
    public void lockPirate() {
        if (!pirateAlive) {
            return;
        }
        combatLock = true;
        merchantLock = false;
        playerFireCd = Math.min(playerFireCd, 0.08f);
        toast("已锁定海盗，对方将追击，自动连续开火。");
    }

    public void cancelLock() {
        combatLock = false;
        merchantLock = false;
        toast("取消锁定。");
    }

    private void spawnPirate() {
        pirateSpawnTimer = 55f + MathUtils.random(40f);
        if (pirateAlive) return;
        for (int i=0;i<Catalog.PORTS.length;i++) {
            if (Catalog.dist(x,y,Catalog.PORT_X[i],Catalog.PORT_Y[i]) < 240f) {
                pirateSpawnTimer = 8f; return;
            }
        }
        float[] point = trafficSpawn(VoyageGeometry.PIRATE_SHIP, 420f, 620f);
        if (point == null) { pirateSpawnTimer = 8f; return; }
        pirateX = point[0]; pirateY = point[1];
        pirateHeading = MathUtils.atan2(y-pirateY,x-pirateX)*MathUtils.radiansToDegrees;
        pirateHp = pirateHpMax = Catalog.PIRATE_HP;
        pirateDamage = MathUtils.random(1,10); // Roll ONCE for this ship's entire life.
        pirateGeneration++;
        pirateAlive = true; pirateChase = false; pirateHelm.reset(); hurtTime[1]=rockTime[1]=0;
        pirateFireCd = .4f;
        toast("外海发现海盗，可绕航避开。进入680范围会遭炮击。" + (autoSail ? "自动航行继续。" : ""));
    }

    private float[] trafficSpawn(int hullType) {
        return trafficSpawn(hullType, Catalog.NPC_SPAWN_MIN, Catalog.NPC_SPAWN_MAX);
    }

    /** Pirate discovery ships spawn in the combat-visible horizon band; ordinary traffic remains distant. */
    private float[] trafficSpawn(int hullType, float minRadius, float maxRadius) {
        for (int n=0;n<64;n++) {
            float angle=MathUtils.random(360f), radius=MathUtils.random(minRadius,maxRadius);
            float px=x+MathUtils.cosDeg(angle)*radius, py=y+MathUtils.sinDeg(angle)*radius;
            if (trafficWaterClear(px,py,hullType)
                    && (!pirateAlive || Catalog.dist(px,py,pirateX,pirateY)>180)
                    && (merchant==null || Catalog.dist(px,py,merchant.x,merchant.y)>180)
                    && rosterClear(px,py)) return new float[]{px,py};
        }
        return null; // Never clamp a spawn into fire range or onto land.
    }

    private boolean trafficWaterClear(float px,float py,int type) {
        float r=VoyageGeometry.ship(type).radius()+12;
        if(px<r || py<r || px>Catalog.WORLD_W-r || py>Catalog.WORLD_H-r) return false;
        for(int i=0;i<Catalog.PORTS.length;i++)
            if(Catalog.dist(px,py,Catalog.PORT_X[i],Catalog.PORT_Y[i])<r+VoyageGeometry.landRadius(true,i)) return false;
        for(int i=0;i<Catalog.ISLANDS.length;i++)
            if(Catalog.dist(px,py,Catalog.ISLAND_X[i],Catalog.ISLAND_Y[i])<r+VoyageGeometry.landRadius(false,i)) return false;
        return true;
    }

    // Pirate pressure is faster/closer; warships hold a wider broadside orbit.
    private static final float PIRATE_SAIL_SPEED = 100f;

    /** Seek a moving flank point, never the player's center. Escape headings are held
     * for 1.2s and scored against open-water probes, avoiding mirrored-point jitter. */
    private void maneuver(CombatHelm h, int type, float ex, float ey, float heading,
                          float preferred, float speed, float dt, boolean retreat) {
        h.x=ex; h.y=ey; h.heading=heading;
        if(dt<=0) return;
        h.sideTime-=dt;
        if(h.sideTime<=0) { h.side=-h.side; h.sideTime=6f; }
        if(retreat != h.withdrawing) h.retreatTime=0;
        h.withdrawing=retreat;
        h.retreatTime-=dt;
        int steps=Math.max(1,(int)Math.ceil(speed*dt/8f));
        float step=speed*dt/steps;
        for(int n=0;n<steps;n++) {
            float dx=h.x-x, dy=h.y-y, distance=Math.max(1f,Catalog.dist(x,y,h.x,h.y));
            float ux=dx/distance, uy=dy/distance;
            if(distance<=1) { ux=MathUtils.cosDeg(heading); uy=MathUtils.sinDeg(heading); }
            float wanted;
            if(retreat) {
                if(h.retreatTime<=0) {
                    float away=MathUtils.atan2(uy,ux)*MathUtils.radiansToDegrees;
                    h.retreatHeading=clearHeading(h,type,away,220f,true);
                    h.retreatTime=1.2f;
                }
                wanted=h.retreatHeading;
            } else {
                float tangent=preferred*.38f*h.side;
                float tx=x+ux*preferred-uy*tangent, ty=y+uy*preferred+ux*tangent;
                wanted=MathUtils.atan2(ty-h.y,tx-h.x)*MathUtils.radiansToDegrees;
            }
            float course=clearHeading(h,type,wanted,retreat?100f:60f,retreat);
            if(Float.isNaN(course)) { h.retreatTime=0; h.sideTime=Math.min(h.sideTime,.5f); break; }
            float nx=h.x+MathUtils.cosDeg(course)*step, ny=h.y+MathUtils.sinDeg(course)*step;
            if(!combatWaterClear(h,type,nx,ny)) { h.retreatTime=0; break; }
            h.x=nx; h.y=ny;
            // Hull follows the route; firing never snaps a withdrawing bow back toward its pursuer.
            h.heading=MathUtils.lerpAngleDeg(h.heading,course,Math.min(1f,7f*dt/steps));
        }
    }

    /** Sample forward clearance along the whole probe, with continuity as a tie-breaker. */
    private float clearHeading(CombatHelm h,int type,float wanted,float reach,boolean retreat) {
        float best=Float.NaN, score=-Float.MAX_VALUE;
        if(Float.isNaN(wanted)) wanted=h.heading;
        float away=MathUtils.atan2(h.y-y,h.x-x)*MathUtils.radiansToDegrees;
        for(int i=0;i<24;i++) {
            float offset=((i+1)/2)*15*(i%2==0?1:-1), angle=wanted+offset;
            boolean clear=true;
            for(float d=12;d<=reach;d+=12) {
                if(!combatWaterClear(h,type,h.x+MathUtils.cosDeg(angle)*d,h.y+MathUtils.sinDeg(angle)*d)) { clear=false; break; }
            }
            if(!clear) continue;
            float value=MathUtils.cosDeg(offset)*2f+MathUtils.cosDeg(angle-h.heading)*.18f;
            if(retreat) value+=MathUtils.cosDeg(angle-away)*1.5f;
            if(value>score) { score=value; best=angle; }
        }
        return best;
    }

    private boolean combatWaterClear(CombatHelm h,int type,float px,float py) {
        if(!trafficWaterClear(px,py,type)) return false;
        float r=VoyageGeometry.ship(type).radius();
        float playerLimit=r+VoyageGeometry.ship(ship).radius()+12;
        float d=Catalog.dist(px,py,x,y);
        // Recover an existing overlap by moving outward; mutual separation remains active too.
        if(d<playerLimit && d<=Catalog.dist(h.x,h.y,x,y)) return false;
        if(pirateAlive && h!=pirateHelm && Catalog.dist(px,py,pirateX,pirateY)<r+VoyageGeometry.ship(VoyageGeometry.PIRATE_SHIP).radius()+8) return false;
        if(merchant!=null && Catalog.dist(px,py,merchant.x,merchant.y)<r+VoyageGeometry.ship(merchant.ship).radius()+8) return false;
        for(TraderData t:traders) if(t!=null && t.alive && Catalog.dist(px,py,t.x,t.y)<r+VoyageGeometry.ship(t.ship).radius()+8) return false;
        for(WarshipData w:warships) if(w!=null && w.alive && w.helm!=h && Catalog.dist(px,py,w.x,w.y)<r+VoyageGeometry.ship(w.ship).radius()+8) return false;
        return true;
    }

    /** Broadside guns can bear off either side; retreat also allows the stern return volley. */
    private boolean gunsBear(float heading,float ex,float ey,float tx,float ty,boolean retreat) {
        if(retreat) return true;
        float bearing=MathUtils.atan2(ty-ey,tx-ex)*MathUtils.radiansToDegrees;
        return Math.abs(MathUtils.cosDeg(bearing-heading))<.96f;
    }

    private void updateCombat(float dt) {
        float d=Catalog.dist(x,y,pirateX,pirateY);
        if (d>Catalog.NPC_HORIZON) {
            clearPirate();
            toast(autoSail ? "已驶出海盗海域，自动航行继续。" : "已驶出海盗海域。");
            return;
        }
        boolean lowHp = pirateHp <= pirateHpMax * .5f;
        if(combatLock) pirateHelm.grace=2.5f;
        else pirateHelm.grace=Math.max(0,pirateHelm.grace-dt);
        boolean engaged=combatLock || pirateHelm.grace>0;
        pirateChase=engaged && !lowHp;
        if(engaged || lowHp) {
            maneuver(pirateHelm,VoyageGeometry.PIRATE_SHIP,pirateX,pirateY,pirateHeading,
                    Catalog.PIRATE_RANGE*.65f,PIRATE_SAIL_SPEED*(lowHp?1.2f:1f),dt,lowHp);
            pirateX=pirateHelm.x; pirateY=pirateHelm.y; pirateHeading=pirateHelm.heading;
        }
        d=Catalog.dist(x,y,pirateX,pirateY);
        float md=merchant==null ? Float.MAX_VALUE : Catalog.dist(pirateX,pirateY,merchant.x,merchant.y);
        if (Math.min(d,md)<=Catalog.PIRATE_RANGE) {
            boolean player=d<=md;
            float tx=player?x:merchant.x, ty=player?y:merchant.y;
            if(!engaged && !lowHp) pirateHeading=MathUtils.atan2(ty-pirateY,tx-pirateX)*MathUtils.radiansToDegrees;
            if((engaged || lowHp) && !gunsBear(pirateHeading,pirateX,pirateY,tx,ty,lowHp)) return;
            pirateFireCd-=dt;
            if(pirateFireCd<=0) {
                // 0.28.18: slowed 2x so the cannon SFX can finish between volleys.
                pirateFireCd=Catalog.PIRATE_FIRE_INTERVAL*2f;
                fireAt(false,player?PLAYER:MERCHANT,pirateDamage,pirateX,pirateY,tx,ty);
                playCannonSfx();
            }
        }
    }

    public boolean merchantVisible() {
        return merchant!=null && Catalog.dist(x,y,merchant.x,merchant.y)<=Catalog.NPC_HORIZON;
    }

    public void lockMerchant() {
        if(!merchantVisible() || worldPaused() || failed) return;
        merchantLock=true; combatLock=false; merchant.hostile=true;
        playerFireCd=Math.min(playerFireCd,.08f);
        toast("已锁定商船掠夺，对方会还击。亲手击沉才可获得财货。");
    }

    public static int merchantDamage(int ship) { return 1+Catalog.SHIP_FIRE[ship]/10; }
    public static float merchantHull(int ship) { return 28+Catalog.SHIP_FIRE[ship]+Catalog.SHIP_HOLD[ship]/2f; }

    // ================= 0.28.22 远处船队刷新（软上限 + 低概率判定） =================
    /** 每次刷怪判定成功生成的概率：支持「船队感」但不扎堆（约 25%）。 */
    private static final float SPAWN_ROLL_CHANCE = 0.25f;

    private boolean rosterClear(float px, float py) {
        for (TraderData t : traders) if (t != null && t.alive && Catalog.dist(px, py, t.x, t.y) < 170f) return false;
        for (WarshipData w : warships) if (w != null && w.alive && Catalog.dist(px, py, w.x, w.y) < 170f) return false;
        return true;
    }

    static int traderHull(int ship) { return Math.round(30 + Catalog.SHIP_FIRE[ship] * .6f + Catalog.SHIP_HOLD[ship] * .5f); }
    static int traderDamage(int ship) { return 1 + Catalog.SHIP_FIRE[ship] / 12; }
    public static int warshipHull(int ship) { return 46 + Catalog.SHIP_FIRE[ship]; }
    public static int warshipDamage(int ship) { return 2 + Catalog.SHIP_FIRE[ship] / 9; }

    private void updateTraderRoster(float dt) {
        traderSpawnTimer -= dt;
        if (traderSpawnTimer <= 0f) {
            traderSpawnTimer = 90f + MathUtils.random(60f);
            if (MathUtils.random() < SPAWN_ROLL_CHANCE) {
                for (int i = 0; i < traders.length; i++) {
                    if (traders[i] != null && traders[i].alive) continue;
                    spawnTrader(i);
                    break;
                }
            }
        }
        for (int i = 0; i < traders.length; i++) {
            TraderData t = traders[i];
            if (t == null || !t.alive) continue;
            if (Catalog.dist(x, y, t.x, t.y) > Catalog.NPC_HORIZON * 1.4f) { traders[i] = null; continue; }
            updateTrader(t, dt);
        }
    }

    private void spawnTrader(int slot) {
        int type = new int[]{0, 3, 6}[MathUtils.random(2)]; // 商级船体：小商船/漕舫/货舶
        float[] point = trafficSpawn(type);
        if (point == null) { traderSpawnTimer = 15f; return; }
        TraderData t = new TraderData();
        t.ship = type; t.x = point[0]; t.y = point[1];
        t.hp = t.hpMax = traderHull(type);
        t.targetPort = nearestPortTo(t.x, t.y);
        traders[slot] = t; hurtTime[3+slot]=rockTime[3+slot]=0;
    }

    private int nearestPortTo(float px, float py) {
        int best = 0; float bd = Float.MAX_VALUE;
        for (int i = 0; i < Catalog.PORTS.length; i++) {
            float d = Catalog.dist(px, py, Catalog.PORT_X[i], Catalog.PORT_Y[i]);
            if (d < bd) { bd = d; best = i; }
        }
        return best;
    }

    /** 货船 AI：循商路航行；从不主动撞船；被攻击/被撞后还击并加速逃离。 */
    private void updateTrader(TraderData t, float dt) {
        float tx, ty;
        if (t.hostile) { // 逃离：沿「玩家-货船」连线的镜像点跑
            tx = MathUtils.clamp(t.x * 2f - x, 300f, Catalog.WORLD_W - 300f);
            ty = MathUtils.clamp(t.y * 2f - y, 300f, Catalog.WORLD_H - 300f);
        } else {
            if (t.targetPort < 0) t.targetPort = nearestPortTo(t.x, t.y);
            tx = Catalog.PORT_X[t.targetPort]; ty = Catalog.PORT_Y[t.targetPort];
            if (Catalog.dist(t.x, t.y, tx, ty) < VoyageGeometry.landRadius(true, t.targetPort) + 130f) {
                t.targetPort = MathUtils.random(Catalog.PORTS.length - 1);
                return;
            }
        }
        moveNpc(t, t.ship, tx, ty, 58f * (1f + Catalog.SHIP_SPEED[t.ship] / 100f) * (t.hostile ? 1.6f : 1f), dt);
        // 0.28.24: 警觉货船边逃边还击（此前 fireCd 从未使用，货船只会逃不会打）。
        if (t.hostile && Catalog.dist(x, y, t.x, t.y) <= Catalog.PIRATE_RANGE) {
            t.fireCd -= dt;
            if (t.fireCd <= 0f) {
                t.fireCd = Catalog.PIRATE_FIRE_INTERVAL * 1.8f;
                fireAt(false, PLAYER, traderDamage(t.ship), t.x, t.y, x, y);
            }
        }
    }

    private void updateWarshipRoster(float dt) {
        warshipSpawnTimer -= dt;
        if (warshipSpawnTimer <= 0f) {
            warshipSpawnTimer = 110f + MathUtils.random(70f);
            if (MathUtils.random() < SPAWN_ROLL_CHANCE) {
                for (int i = 0; i < warships.length; i++) {
                    if (warships[i] != null && warships[i].alive) continue;
                    spawnWarship(i);
                    break;
                }
            }
        }
        for (int i = 0; i < warships.length; i++) {
            WarshipData w = warships[i];
            if (w == null || !w.alive) continue;
            if (Catalog.dist(x, y, w.x, w.y) > Catalog.NPC_HORIZON * 1.4f) { warships[i] = null; continue; }
            updateWarship(w, dt);
        }
    }

    private void spawnWarship(int slot) {
        int type = new int[]{4, 8}[MathUtils.random(1)]; // 战级船体：斗舰/海鹘
        float[] point = trafficSpawn(type);
        if (point == null) { warshipSpawnTimer = 20f; return; }
        WarshipData w = new WarshipData();
        w.ship = type; w.x = point[0]; w.y = point[1];
        w.hp = w.hpMax = warshipHull(type);
        w.anchorX = point[0]; w.anchorY = point[1];
        w.patrolAngle = MathUtils.random(360f);
        warships[slot] = w; hurtTime[5+slot]=rockTime[5+slot]=0;
        if (Catalog.dist(x, y, w.x, w.y) <= Catalog.NPC_HORIZON) toast("远处发现战船，交战海域注意规避。");
    }

    /** Standoff flank combat, open-water withdrawal, otherwise the existing patrol route. */
    private void updateWarship(WarshipData w, float dt) {
        w.hostile=warshipHostile(w);
        if(w.hostile) w.helm.grace=2.5f;
        else w.helm.grace=Math.max(0,w.helm.grace-dt);
        boolean lowHp=w.hp<=w.hpMax*.5f;
        boolean engaged=w.hostile || w.helm.grace>0;
        if(engaged || lowHp) {
            maneuver(w.helm,w.ship,w.x,w.y,w.heading,Catalog.PIRATE_RANGE*.83f,
                    78f*(lowHp?1.35f:1f),dt,lowHp);
            w.x=w.helm.x; w.y=w.helm.y; w.heading=w.helm.heading;
        } else {
            w.patrolAngle+=14f*dt;
            moveNpc(w,w.ship,w.anchorX+MathUtils.cosDeg(w.patrolAngle)*240,
                    w.anchorY+MathUtils.sinDeg(w.patrolAngle)*240,50f,dt);
        }
        if((engaged || lowHp) && Catalog.dist(x,y,w.x,w.y)<=Catalog.PIRATE_RANGE
                && gunsBear(w.heading,w.x,w.y,x,y,lowHp)) {
            w.fireCd-=dt;
            if(w.fireCd<=0) {
                w.fireCd=Catalog.PIRATE_FIRE_INTERVAL*1.6f;
                fireAt(false,PLAYER,warshipDamage(w.ship),w.x,w.y,x,y);
            }
        }
    }

    /** 共用 NPC 步进移动：避岸避船，到不了就走偏移方向（12 向弧采样）。 */
    private void moveNpc(Object entity, int shipType, float tx, float ty, float speed, float dt) {
        TraderData t = entity instanceof TraderData ? (TraderData) entity : null;
        WarshipData w = entity instanceof WarshipData ? (WarshipData) entity : null;
        if (t == null && w == null) return;
        int steps = Math.max(1, (int) Math.ceil(dt * speed / 10f));
        for (int s = 0; s < steps; s++) {
            float ex = t != null ? t.x : w.x, ey = t != null ? t.y : w.y;
            float angle = MathUtils.atan2(ty - ey, tx - ex) * MathUtils.radiansToDegrees;
            for (int n = 0; n < 12; n++) {
                float offset = ((n + 1) / 2) * 30 * (n % 2 == 0 ? 1 : -1);
                float heading = angle + offset;
                float nx = ex + MathUtils.cosDeg(heading) * speed * dt / steps;
                float ny = ey + MathUtils.sinDeg(heading) * speed * dt / steps;
                if (!trafficWaterClear(nx, ny, shipType)) continue;
                if (Catalog.dist(nx, ny, x, y) < VoyageGeometry.ship(shipType).radius() + VoyageGeometry.ship(ship).radius() + 8) continue;
                if (pirateAlive && Catalog.dist(nx, ny, pirateX, pirateY) < VoyageGeometry.ship(shipType).radius() + VoyageGeometry.ship(VoyageGeometry.PIRATE_SHIP).radius() + 8) continue;
                if (t != null) { t.x = nx; t.y = ny; t.heading = heading; }
                else { w.x = nx; w.y = ny; w.heading = heading; }
                break;
            }
        }
    }

    private void despawnTrader(int slot, boolean withWreck) {
        TraderData t = slot < traders.length ? traders[slot] : null;
        if (t == null) return;
        if (withWreck) addWreck(t.x, t.y, t.heading, t.ship);
        traders[slot] = null;
    }

    private void despawnWarship(int slot, boolean withWreck) {
        WarshipData w = slot < warships.length ? warships[slot] : null;
        if (w == null) return;
        if (withWreck) addWreck(w.x, w.y, w.heading, w.ship);
        warships[slot] = null;
    }

    private void spawnMerchant() {
        if(merchant!=null) return; // Global cap one, even when outside the horizon.
        int type=MathUtils.random(Catalog.SHIPS.length-1);
        float[] point=trafficSpawn(type);
        if(point==null) { merchantSpawnTimer=15; return; }
        MerchantData m=new MerchantData(); m.ship=type; m.x=point[0]; m.y=point[1];
        m.hp=m.hpMax=merchantHull(type); m.damage=merchantDamage(type);
        m.silver=25+MathUtils.random(20); m.cargoGood=MathUtils.random(Catalog.GOODS.length-1); m.cargo=1;
        merchant=m; merchantGeneration++; hurtTime[2]=rockTime[2]=0;
        m.targetPort=0;
        for(int i=1;i<Catalog.PORTS.length;i++)
            if(Catalog.dist(m.x,m.y,Catalog.PORT_X[i],Catalog.PORT_Y[i])<Catalog.dist(m.x,m.y,Catalog.PORT_X[m.targetPort],Catalog.PORT_Y[m.targetPort])) m.targetPort=i;
        toast("远处驶来一艘"+Catalog.SHIPS[type]+"，正循商路航行。点船可锁定掠夺。");
    }

    private void updateMerchant(float dt) {
        if(merchant==null) {
            merchantSpawnTimer-=dt;
            if(merchantSpawnTimer<=0) spawnMerchant();
            return;
        }
        MerchantData m=merchant;
        if(!merchantVisible()) merchantLock=false; // Entity/hostility persist, only focus leaves range.
        float pd=Catalog.dist(x,y,m.x,m.y);
        float enemy=pirateAlive?Catalog.dist(m.x,m.y,pirateX,pirateY):Float.MAX_VALUE;
        boolean firePlayer=m.hostile && pd<=Catalog.PIRATE_RANGE && pd<=enemy;
        if(firePlayer || enemy<=Catalog.PIRATE_RANGE) {
            m.fireCd-=dt;
            if(m.fireCd<=0) {
                m.fireCd=1.1f/(1+Catalog.SHIP_FIRE[m.ship]/100f);
                fireAt(false,firePlayer?PLAYER:PIRATE,m.damage,m.x,m.y,firePlayer?x:pirateX,firePlayer?y:pirateY);
            }
        }
        // AI and trade continue off-screen; no spawning a replacement for a distant trader.
        if(m.rest>0) { m.rest=Math.max(0,m.rest-dt); return; }
        if(m.targetPort<0 && m.targetIsland<0 || m.targetPort>=Catalog.PORTS.length || m.targetIsland>=Catalog.ISLANDS.length) chooseMerchantRoute(m);
        boolean port=m.targetPort>=0;
        int index=port?m.targetPort:m.targetIsland;
        float tx=port?Catalog.PORT_X[index]:Catalog.ISLAND_X[index];
        float ty=port?Catalog.PORT_Y[index]:Catalog.ISLAND_Y[index];
        // 0.28.22 商船 AI：从不主动撞船；被掠夺时还击（上方已有）并加速逃离。
        if(m.hostile) {
            tx=MathUtils.clamp(m.x*2f-x,300f,Catalog.WORLD_W-300f);
            ty=MathUtils.clamp(m.y*2f-y,300f,Catalog.WORLD_H-300f);
        }
        if(!m.hostile && Catalog.dist(m.x,m.y,tx,ty)<VoyageGeometry.landRadius(port,index)+110) {
            m.silver=Math.min(90,m.silver+(port?4:2));
            m.cargo=Math.min(3,m.cargo+1);
            if(port) m.cargoGood=(m.cargoGood+1)%Catalog.GOODS.length;
            m.rest=port?9:15;
            chooseMerchantRoute(m); return;
        }
        // 0.28.22 被掠夺时逃逸提速（平时原速）。
        float speed=58*(1+Catalog.SHIP_SPEED[m.ship]/100f)*(m.hostile?1.55f:1f);
        int steps=Math.max(1,(int)Math.ceil(dt*speed/10));
        for(int step=0;step<steps;step++) {
            float angle=MathUtils.atan2(ty-m.y,tx-m.x)*MathUtils.radiansToDegrees;
            for(int n=0;n<12;n++) {
                float offset=((n+1)/2)*30*(n%2==0?1:-1);
                float heading=angle+offset;
                float nx=m.x+MathUtils.cosDeg(heading)*speed*dt/steps, ny=m.y+MathUtils.sinDeg(heading)*speed*dt/steps;
                if(!trafficWaterClear(nx,ny,m.ship)) continue;
                if(pirateAlive && Catalog.dist(nx,ny,pirateX,pirateY)<VoyageGeometry.ship(m.ship).radius()+VoyageGeometry.ship(VoyageGeometry.PIRATE_SHIP).radius()+8) continue;
                if(Catalog.dist(nx,ny,x,y)<VoyageGeometry.ship(m.ship).radius()+VoyageGeometry.ship(ship).radius()+8) continue;
                m.x=nx;m.y=ny;m.heading=heading;break;
            }
        }
    }

    private void chooseMerchantRoute(MerchantData m) {
        int previous=m.targetPort;
        m.targetPort=-1; m.targetIsland=-1;
        if(MathUtils.random(7)==0) m.targetIsland=MathUtils.random(Catalog.ISLANDS.length-1);
        else {
            int next=MathUtils.random(Catalog.PORTS.length-2);
            m.targetPort=next>=previous && previous>=0 ? next+1 : next;
        }
    }

    private void updatePlayerFire(float dt) {
        int target=combatLock && pirateAlive?PIRATE:merchantLock && merchant!=null?MERCHANT:-1;
        if(target<0) return;
        float tx=target==PIRATE?pirateX:merchant.x, ty=target==PIRATE?pirateY:merchant.y;
        if(Catalog.dist(x,y,tx,ty)>Catalog.PIRATE_RANGE) return;
        playerFireCd-=dt;
        if(playerFireCd<=0) {
            playerFireCd=fireInterval(); fireAt(true,target,1,x,y,tx,ty);
            playCannonSfx();
            pokeCannonFeedback(); // 0.28.26 开炮小后坐 + 炮口闪
        }
    }

    private void fireAt(boolean player,int target,int damage,float sx,float sy,float tx,float ty) {
        int i=ballCount;
        spawnBall(player,sx,sy,tx,ty);
        if(ballCount==i) return;
        ballTarget[i]=target; ballDamage[i]=damage;
        ballGeneration[i]=target==PIRATE?pirateGeneration:target==MERCHANT?merchantGeneration:0;
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
        ballTarget[i] = fromPlayer ? PIRATE : PLAYER;
        ballGeneration[i] = fromPlayer ? pirateGeneration : 0;
        ballDamage[i] = fromPlayer ? 1 : pirateDamage;
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
            int target=ballTarget[i], damage=ballDamage[i], generation=ballGeneration[i];
            boolean player=ballFromPlayer[i];
            float bx=ballX[i], by=ballY[i];
            removeBall(i); // Removing a target never changes attribution of other in-flight shots.
            if(target==PIRATE && pirateAlive && generation==pirateGeneration && Catalog.dist(bx,by,pirateX,pirateY)<=100) {
                hurtTime[1]=.3f;
                pirateHp-=damage;
                if(pirateHp<=0) { addWreck(pirateX,pirateY,pirateHeading,VoyageGeometry.PIRATE_SHIP,false,true); if(player) winCombat(); else clearPirate(); }
            } else if(target==MERCHANT && merchant!=null && generation==merchantGeneration && Catalog.dist(bx,by,merchant.x,merchant.y)<=100) {
                hurtTime[2]=.3f;
                merchant.hp-=damage;
                if(merchant.hp<=0) sinkMerchant(player);
            } else if(target==PLAYER && Catalog.dist(bx,by,x,y)<=90) {
                hurtTime[0]=.3f;
                hull=Math.max(0,hull-damage);
                if(hull<=0) { fail("船沉"); return; }
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
            ballTarget[i] = ballTarget[last]; ballGeneration[i] = ballGeneration[last]; ballDamage[i] = ballDamage[last];
        }
        ballCount--;
    }

    private void winCombat() {
        if (!pirateAlive) return;
        questDefeatedPirates++;
        int loot = 25 + MathUtils.random(55);
        silver += loot;
        playCoinSfx();
        String extra = "";
        String good = ""; int goodCount = 0;
        if (cargoFree() > 0 && MathUtils.randomBoolean()) {
            int g = MathUtils.random(Catalog.GOODS.length - 1);
            trade[g]++;
            good = Catalog.GOODS[g]; goodCount = 1;
            extra = "缴获货舱：" + Catalog.GOODS[g] + "×1（当货物卖掉）。";
        } else if (MathUtils.random() < 0.2f) {
            extra = "缴获一件船部件（折算额外 40 两）。";
            silver += 40;
        }
        // 0.28.22: 掠夺收益改用弹窗展示（不再只有底部横幅）。
        pushLootPopup("击沉海盗", loot, good, goodCount, extra);
        toast("击沉海盗。" + (autoSail ? "自动航行继续。" : ""));
        clearPirate();
    }

    public void clearPirate() {
        pirateAlive = false;
        combatLock = false;
        pirateChase = false;
        pirateHelm.reset();
        pirateSpawnTimer = Math.max(pirateSpawnTimer, 55f);
        // Shots already fired retain their target and rolled damage until impact.
    }

    private void sinkMerchant(boolean playerKill) {
        if(merchant==null) return;
        addWreck(merchant.x, merchant.y, merchant.heading, merchant.ship); // 沉船动画，不瞬间消失
        if(playerKill) {
            int cargo=Math.min(cargoFree(),merchant.cargo);
            int silverGain=merchant.silver;
            silver+=silverGain; trade[merchant.cargoGood]+=cargo;
            playCoinSfx();
            // 0.28.22: 掠夺收益改用弹窗展示（不再只有底部横幅）。
            pushLootPopup("击沉商船", silverGain, Catalog.GOODS[merchant.cargoGood], cargo,
                    cargo<merchant.cargo?"货舱不足，余货随船沉没。":"");
            toast("击沉商船。");
        }
        merchant=null; merchantLock=false;
        merchantSpawnTimer=120f+MathUtils.random(80f);
    }

    public void fail(String reason) {
        if(failed) return;
        if("船沉".equals(reason)) {
            playerSinkRemaining=SINK_SECONDS;
            addWreck(x,y,headingDeg,ship,true,false);
        }
        failed = true;
        failReason = reason;
        merchantLock = false; ballCount = 0;
        stopAutoSail();
        clearPirate();
        speed = 0f;
        toast(reason + "。航程失败，请重新开始。");
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
