package com.shipgame.nanhai.data;

/**
 * Static world tables. Unspecified numbers are placeholders (需求文档 v0.23).
 */
public final class Catalog {

    public static final float WORLD_W = 4800f;
    public static final float WORLD_H = 3600f;

    public static final String[] PORTS = {
            "广州", "潮州", "雷州", "琼州", "崖州", "合浦", "交州", "占城", "真腊", "佛逝"
    };
    public static final float[] PORT_X = {
            3200f, 3900f, 2500f, 2400f, 2300f, 1800f, 1200f, 1400f, 900f, 1600f
    };
    public static final float[] PORT_Y = {
            2800f, 2700f, 2400f, 1900f, 1400f, 2200f, 1800f, 1100f, 800f, 400f
    };

    public static final String[] ISLANDS = {"南澳屿", "琼东岛", "西沙礁"};
    public static final float[] ISLAND_X = {3600f, 2800f, 2000f};
    public static final float[] ISLAND_Y = {2500f, 1700f, 1200f};

    public static final String[] GOODS = {
            "丝绸", "瓷器", "茶叶", "盐", "铁器", "米粮", "蔗糖",
            "沉香", "苏木", "胡椒", "象牙", "珍珠", "玳瑁", "槟榔"
    };
    /** Base silver per unit. */
    public static final int[] GOOD_BASE = {
            80, 70, 50, 18, 60, 22, 40, 120, 55, 90, 150, 180, 160, 35
    };
    /**
     * Per-port price tenths (10 = 1.0x). Fixed, does not change over time.
     * Rows = ports, cols = goods.
     */
    public static final int[][] GOOD_MULT_TENTHS = {
            {13, 12, 11, 9, 10, 8, 11, 12, 11, 14, 13, 12, 12, 10}, // 广州
            {12, 11, 10, 8, 11, 9, 10, 11, 10, 13, 14, 13, 13, 9},  // 潮州
            {10, 10, 9, 7, 9, 8, 9, 10, 9, 11, 12, 12, 11, 8},      // 雷州
            {9, 10, 10, 8, 10, 7, 8, 11, 10, 10, 11, 11, 10, 8},    // 琼州
            {9, 9, 10, 8, 11, 7, 7, 12, 11, 9, 10, 10, 9, 7},       // 崖州
            {10, 9, 9, 6, 8, 7, 8, 10, 9, 10, 11, 11, 10, 7},       // 合浦
            {8, 8, 9, 7, 8, 6, 8, 9, 8, 8, 9, 10, 9, 6},            // 交州
            {7, 8, 8, 8, 9, 7, 9, 8, 7, 6, 8, 9, 8, 5},             // 占城
            {8, 9, 9, 8, 10, 8, 10, 7, 6, 7, 7, 8, 8, 6},           // 真腊
            {7, 7, 8, 9, 9, 8, 9, 6, 5, 5, 6, 7, 7, 4}              // 佛逝
    };

    public static final String[] BEASTS = {
            "精卫", "九尾狐", "蛊雕", "文鳐鱼", "长右", "狌狌", "白泽", "三足龟"
    };
    public static final int[] BEAST_PRICE = {40, 90, 70, 55, 45, 60, 120, 80};

    public static final String[] HERBS = {
            "人参", "灵芝", "茯苓", "当归", "何首乌", "桂枝", "甘草", "菊花"
    };
    public static final int[] HERB_PRICE = {35, 50, 20, 25, 30, 15, 12, 18};

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
    public static final int START_SILVER = 520;
    public static final int START_CARGO_CAP = 40;
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
    public static final int PIRATE_SHOT = 5;

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
