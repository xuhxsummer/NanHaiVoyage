package com.shipgame.nanhai.data;

/**
 * Static world tables. Unspecified numbers are placeholders (需求文档 v0.23).
 * 0.26.0: world grew from 10 to 20 ports and from 3 to 13 islands, and the
 * goods list from 14 to 24 (each market column now paginates).
 */
public final class Catalog {

    public static final float WORLD_W = 4800f;
    public static final float WORLD_H = 3600f;

    public static final String[] PORTS = {
            "广州", "潮州", "雷州", "琼州", "崖州", "合浦", "交州", "占城", "真腊", "佛逝",
            "泉州", "福州", "明州", "钦州", "邕州", "暹罗", "渤泥", "吕宋", "苏禄", "爪哇",
            "扬州" // 0.26.3: 故乡，北缘海岸；新档从这里起航。附加在末尾以保持旧档港口序号
    };
    public static final float[] PORT_X = {
            3200f, 3900f, 2500f, 2400f, 2300f, 1800f, 1200f, 1400f, 900f, 1600f,
            4150f, 4400f, 4650f, 1300f, 2050f, 550f, 2900f, 4300f, 3300f, 2300f,
            3100f // 扬州：北缘沿海，四周留空不与其他港口重叠
    };
    public static final float[] PORT_Y = {
            2800f, 2700f, 2400f, 1900f, 1400f, 2200f, 1800f, 1100f, 800f, 400f,
            3000f, 3260f, 3520f, 2450f, 3150f, 1650f, 550f, 1250f, 700f, 200f,
            3400f // 扬州
    };
    /** 0.26.3: 故乡港口序号（PORTS 数组末尾，追加顺序固定）。 */
    public static final int YANGZHOU = PORTS.length - 1;

    public static final String[] ISLANDS = {
            "南澳屿", "琼东岛", "西沙礁",
            "东沙", "中沙", "永兴", "黄岩", "万山", "担杆", "川山", "涠洲", "海陵", "硇洲"
    };
    public static final float[] ISLAND_X = {
            3600f, 2800f, 2000f,
            4450f, 3350f, 2300f, 3700f, 3000f, 3400f, 2800f, 2050f, 2500f, 2700f
    };
    public static final float[] ISLAND_Y = {
            2500f, 1700f, 1200f,
            2400f, 1300f, 1080f, 1450f, 2350f, 2250f, 2500f, 2450f, 2650f, 2250f
    };

    public static final String[] GOODS = {
            "丝绸", "瓷器", "茶叶", "盐", "铁器", "米粮", "蔗糖",
            "沉香", "苏木", "胡椒", "象牙", "珍珠", "玳瑁", "槟榔",
            "棉布", "漆器", "铜器", "琉璃", "乳香", "没药", "豆蔻", "丁香", "珊瑚", "犀角"
    };
    /** Base silver per unit. */
    public static final int[] GOOD_BASE = {
            80, 70, 50, 18, 60, 22, 40, 120, 55, 90, 150, 180, 160, 35,
            30, 60, 55, 85, 100, 95, 70, 80, 150, 140
    };
    /**
     * Per-port price tenths (10 = 1.0x). Fixed, does not change over time.
     * Rows = ports, cols = goods. New ports/goods use placeholder multipliers.
     */
    public static final int[][] GOOD_MULT_TENTHS = {
            {13, 12, 11, 9, 10, 8, 11, 12, 11, 14, 13, 12, 12, 10, 9, 11, 10, 12, 13, 13, 12, 13, 14, 13}, // 广州
            {12, 11, 10, 8, 11, 9, 10, 11, 10, 13, 14, 13, 13, 9, 8, 10, 10, 11, 12, 12, 11, 12, 13, 12},  // 潮州
            {10, 10, 9, 7, 9, 8, 9, 10, 9, 11, 12, 12, 11, 8, 7, 9, 9, 10, 11, 11, 10, 11, 12, 11},      // 雷州
            {9, 10, 10, 8, 10, 7, 8, 11, 10, 10, 11, 11, 10, 8, 8, 9, 10, 10, 11, 11, 10, 11, 12, 11},   // 琼州
            {9, 9, 10, 8, 11, 7, 7, 12, 11, 9, 10, 10, 9, 7, 8, 9, 10, 10, 10, 10, 10, 11, 11, 10},      // 崖州
            {10, 9, 9, 6, 8, 7, 8, 10, 9, 10, 11, 11, 10, 7, 6, 8, 8, 9, 10, 10, 10, 10, 11, 10},        // 合浦
            {8, 8, 9, 7, 8, 6, 8, 9, 8, 8, 9, 10, 9, 6, 7, 8, 8, 9, 10, 9, 9, 9, 10, 9},                 // 交州
            {7, 8, 8, 8, 9, 7, 9, 8, 7, 6, 8, 9, 8, 5, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8},                    // 占城
            {8, 9, 9, 8, 10, 8, 10, 7, 6, 7, 7, 8, 8, 6, 8, 8, 9, 8, 8, 8, 8, 8, 9, 8},                  // 真腊
            {7, 7, 8, 9, 9, 8, 9, 6, 5, 5, 6, 7, 7, 4, 9, 8, 8, 7, 7, 7, 6, 6, 7, 6},                    // 佛逝
            {9, 8, 9, 10, 10, 10, 8, 11, 10, 12, 12, 12, 11, 9, 7, 8, 9, 10, 12, 12, 12, 12, 13, 12},    // 泉州
            {8, 8, 9, 10, 10, 10, 8, 11, 10, 12, 12, 12, 11, 9, 7, 8, 9, 10, 12, 12, 12, 12, 13, 12},    // 福州
            {8, 9, 10, 11, 9, 10, 8, 11, 10, 13, 12, 12, 12, 9, 7, 8, 8, 10, 12, 12, 12, 12, 13, 12},    // 明州
            {9, 9, 9, 6, 8, 7, 8, 9, 8, 9, 10, 11, 10, 7, 6, 8, 8, 9, 10, 10, 10, 10, 11, 10},           // 钦州
            {8, 9, 10, 7, 9, 7, 9, 10, 9, 10, 11, 11, 10, 7, 6, 8, 8, 9, 10, 10, 10, 10, 11, 10},        // 邕州
            {11, 10, 9, 9, 10, 8, 9, 7, 6, 6, 7, 8, 8, 5, 10, 9, 9, 8, 8, 8, 7, 7, 9, 8},                // 暹罗
            {12, 11, 10, 10, 11, 9, 10, 5, 5, 6, 6, 6, 6, 5, 11, 10, 10, 8, 7, 7, 6, 6, 8, 7},           // 渤泥
            {12, 11, 11, 10, 11, 9, 10, 7, 6, 6, 5, 5, 6, 6, 12, 10, 10, 8, 7, 6, 6, 6, 7, 7},           // 吕宋
            {13, 12, 11, 11, 11, 10, 10, 6, 6, 6, 5, 4, 5, 5, 12, 11, 10, 8, 7, 6, 6, 5, 6, 6},          // 苏禄
            {11, 11, 10, 9, 11, 8, 9, 6, 6, 5, 7, 7, 7, 4, 11, 10, 9, 8, 7, 6, 5, 5, 8, 7},              // 爪哇
            {8, 9, 10, 11, 10, 10, 8, 12, 11, 13, 12, 12, 12, 9, 7, 8, 9, 10, 12, 12, 12, 12, 13, 12}   // 扬州（0.26.3，沿用江南口岸行情）
    };

    public static final String[] BEASTS = {
            "精卫", "九尾狐", "蛊雕", "文鳐鱼", "长右", "狌狌", "白泽", "三足龟"
    };
    public static final int[] BEAST_PRICE = {40, 90, 70, 55, 45, 60, 120, 80};

    public static final String[] HERBS = {
            "人参", "灵芝", "茯苓", "当归", "何首乌", "桂枝", "甘草", "菊花"
    };
    public static final int[] HERB_PRICE = {35, 50, 20, 25, 30, 15, 12, 18};

    // 0.26.3 扬州渔业：鱼获入货舱（与商货/异兽/草药共用容量），任意港口固定价卖出。
    public static final String[] FISH = {
            "小黄鱼", "带鱼", "鲈鱼", "石斑", "金枪鱼", "大黄鱼"
    };
    public static final int[] FISH_PRICE = {12, 18, 30, 50, 80, 130};
    /** 钓具等级上限 / 钓技等级上限 / 渔夫编制等级上限（编制每级 +1 渔夫）。 */
    public static final int FISH_TOOL_MAX = 5;
    public static final int FISH_SKILL_MAX = 5;
    public static final int FISHER_CAP_MAX = 5;
    /** 雇一名渔夫的银两。 */
    public static final int FISHER_HIRE_COST = 40;
    /** 渔夫编制初始上限（编制等级 1 = 可雇 FISHER_START_CAP 人，每级 +1）。 */
    public static final int FISHER_START_CAP = 2;
    /**
     * 每种钓具等级的可获鱼权重（行 = 钓具等级 1..5，列按 Catalog.FISH）。
     * 高级钓具开放更大/更贵的鱼，并把权重向贵鱼倾斜（需求文档 v0.26.3 §4）。
     */
    public static final int[][] FISH_ODDS = {
            {75, 25, 0, 0, 0, 0},                     // L1：小黄鱼/带鱼
            {40, 35, 25, 0, 0, 0},                    // L2：+鲈鱼
            {30, 28, 24, 18, 0, 0},                   // L3：+石斑
            {18, 20, 24, 22, 16, 0},                  // L4：+金枪鱼
            {10, 12, 18, 22, 20, 18}                  // L5：+大黄鱼
    };
    /** 每次渔获所需基准秒数（一名渔夫、钓技 L1）。随渔夫数与钓技缩短。 */
    public static final float FISH_BASE_SECS = 12f;
    /** 每次渔获基础银两回报曲线仅供参考（UI 不直接使用）。 */
    public static int fishPrice(int i) {
        return FISH_PRICE[i];
    }

    public static final float DOCK_RANGE = 78f;
    public static final float ISLAND_RANGE = 70f;
    public static final float PIRATE_RANGE = 430f;
    public static final float PIRATE_FLEE_RANGE = 540f;
    public static final float MAX_SPEED = 150f;
    public static final float TURN_RATE = 110f;
    public static final float ACCEL = 70f;
    public static final float COAST = 28f;
    public static final float WIND_SPEED_FACTOR = 0.38f;
    public static final float SUPPLY_DRAIN_BASE = 0.55f;
    public static final float CREW_DRAIN = 0.08f;
    public static final int START_SILVER = 1000;
    public static final int START_CARGO_CAP = 40;
    public static final float SUPPLY_MAX = 500f;
    public static final float HULL_MAX = 500f;
    public static final int START_CREW = 2;
    public static final int START_CREW_CAP = 4;
    public static final int START_CANNON_DMG = 8;
    public static final int SUPPLY_UNIT_COST = 3;
    public static final int REPAIR_UNIT_COST = 2;
    public static final int HIRE_COST = 40;
    public static final float INTEREST = 0.02f;
    public static final float FIRE_INTERVAL = 0.42f;
    public static final float PIRATE_FIRE_INTERVAL = 0.85f;
    public static final int PIRATE_HP = 48;
    /** 0.25.9 combat balance: pirate cannonballs cost the player exactly 1 耐久
     * each (and the player's shots cost the pirate exactly 1 HP each), so a
     * fight wears the ship down little by little instead of sinking it in a
     * few broadsides. */
    public static final int PIRATE_SHOT = 1;

    public static int goodPrice(int port, int good) {
        int p = Math.round(GOOD_BASE[good] * GOOD_MULT_TENTHS[port][good] / 10f);
        return Math.max(1, p);
    }

    public static int nearestPort(float x, float y) {
        int best = 0;
        float bestD = Float.MAX_VALUE;
        for (int i = 0; i < PORTS.length; i++) {
            float d = dist2(x, y, PORT_X[i], PORT_Y[i]);
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    public static float dist2(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2, dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    public static float dist(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt(dist2(x1, y1, x2, y2));
    }

    private Catalog() {}
}
