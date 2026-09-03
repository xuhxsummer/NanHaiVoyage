using System.Collections.Generic;
using UnityEngine;

/// <summary>
/// 已确认港名与 14 商货；各港固定差价表为占位（文档只确认「固定、不随时间变」，未给具体价）。
/// 岛、异兽、草药条目文档未点名，用占位内容，可卖、进图鉴。
/// </summary>
public static class WorldCatalog
{
    public static readonly string[] Ports =
    {
        "广州", "潮州", "雷州", "琼州", "崖州", "合浦", "交州", "占城", "真腊", "佛逝"
    };

    public static readonly string[] TradeGoods =
    {
        "丝绸", "瓷器", "茶叶", "盐", "铁器", "米粮", "蔗糖",
        "沉香", "苏木", "胡椒", "象牙", "珍珠", "玳瑁", "槟榔"
    };

    // 相对布局，不考据。xz 平面。
    public static readonly Dictionary<string, Vector3> PortPositions = new Dictionary<string, Vector3>
    {
        { "广州", new Vector3(0, 0, 48) },
        { "潮州", new Vector3(22, 0, 40) },
        { "雷州", new Vector3(-14, 0, 16) },
        { "琼州", new Vector3(-10, 0, -2) },
        { "崖州", new Vector3(-12, 0, -22) },
        { "合浦", new Vector3(-32, 0, 12) },
        { "交州", new Vector3(-38, 0, -16) },
        { "占城", new Vector3(-24, 0, -40) },
        { "真腊", new Vector3(-4, 0, -52) },
        { "佛逝", new Vector3(16, 0, -60) }
    };

    public static readonly string[] IslandNames = { "占位岛甲", "占位岛乙", "占位岛丙" };
    public static readonly Vector3[] IslandPositions =
    {
        new Vector3(8, 0, 18),
        new Vector3(-22, 0, -8),
        new Vector3(6, 0, -32)
    };

    public static readonly string[] Beasts = { "占位异兽·南禺", "占位异兽·海鲵", "占位异兽·丹鸟" };
    public static readonly string[] Herbs = { "占位草药·海菖", "占位草药·琼枝", "占位草药·沉叶" };

    static Dictionary<string, int[]> _prices;

    public static void EnsurePrices()
    {
        if (_prices != null) return;
        _prices = new Dictionary<string, int[]>();
        // 固定差价占位：每种货一个基价，各港乘一个固定倍率再取整。
        int[] bases = { 90, 80, 70, 25, 55, 30, 40, 120, 60, 85, 140, 160, 150, 35 };
        float[] portMul = { 1.00f, 1.08f, 0.95f, 0.92f, 0.88f, 0.97f, 0.90f, 1.12f, 1.18f, 1.22f };
        for (int g = 0; g < TradeGoods.Length; g++)
        {
            var row = new int[Ports.Length];
            for (int p = 0; p < Ports.Length; p++)
                row[p] = Mathf.Max(5, Mathf.RoundToInt(bases[g] * portMul[p]));
            _prices[TradeGoods[g]] = row;
        }
    }

    public static int Price(string good, string port)
    {
        EnsurePrices();
        int gi = System.Array.IndexOf(TradeGoods, good);
        int pi = System.Array.IndexOf(Ports, port);
        if (gi < 0 || pi < 0) return 0;
        return _prices[good][pi];
    }

    public static int BeastSellPrice => 200; // PLACEHOLDER
    public static int HerbSellPrice => 60;   // PLACEHOLDER
}
