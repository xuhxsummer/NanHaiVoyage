using UnityEngine;

/// <summary>
/// 数值占位。玩法规则以 /home/box/shipgame/需求文档.md v0.21「已确认」为准。
/// 文档没写死的数字一律放这里，标 PLACEHOLDER，不要当已拍板规则。
/// 已拍板：十港名、14 商货、借债每次回港利息 2%。
/// </summary>
public static class GameBalance
{
    public const float DebtInterestPerPortCall = 0.02f; // 已确认：每次回港 2%

    // --- PLACEHOLDER 数值（文档未点名）---
    public const int StartingSilver = 800;
    public const int StartingSupplies = 100;
    public const int MaxSupplies = 100;
    public const int StartingHull = 100;
    public const int MaxHull = 100;
    public const int StartingCargoCapacity = 40;
    public const int StartingCrew = 8;
    public const int StartingCrewCap = 8;
    public const int StartingCannonLevel = 1;
    public const int StartingCannonDamage = 8;

    public const float SupplyDrainPerSecondAtSea = 0.35f; // 配合「一趟几分钟」
    public const float SupplyDrainPerCrewPerSecond = 0.015f;
    public const float BaseSpeed = 8f;
    public const float AccelPerSecond = 6f;
    public const float DecelPerSecond = 8f;
    public const float CoastPerSecond = 2.5f;
    public const float TurnDegreesPerSecond = 90f;
    public const float MaxSpeed = 14f;

    public const float WindFairMul = 1.25f;
    public const float WindHeadMul = 0.7f;
    public const float WindChangeSeconds = 40f;
    public const float WeatherFogRainSecondsMin = 25f;
    public const float WeatherClearSecondsMin = 35f;

    public const float PortDockRadius = 4.5f;
    public const float IslandExploreRadius = 4f;
    public const float PirateCheckInterval = 18f;
    public const float PirateChance = 0.28f;
    public const float CombatLockRange = 22f;
    public const float EscapeRange = 28f;
    public const float PlayerFireInterval = 1.1f;
    public const float PirateFireInterval = 1.4f;
    public const float PirateDamage = 6f;
    public const float PirateChaseMulIfReturnFire = 1.35f;
    public const float RepairSilverPerHull = 4;
    public const int SupplySilverPerPoint = 3;
    public const int CargoUpgradeAmount = 10;
    public const int CargoUpgradeCost = 400;
    public const int CannonUpgradeDamage = 4;
    public const int CannonUpgradeCost = 350;
    public const int CrewCapUpgradeAmount = 4;
    public const int CrewCapUpgradeCost = 300;
    public const int HireCrewCost = 40;
    public const int LoanStep = 50;

    public const int DropGoodLootMin = 1;
    public const int DropGoodLootMax = 4;
    public const int DropSilverMin = 20;
    public const int DropSilverMax = 80;
    public const float RarePartAsCargoChance = 0.12f;
}
