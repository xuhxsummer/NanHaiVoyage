using System;
using System.Collections.Generic;

[Serializable]
public class CargoStacks
{
    public List<string> tradeIds = new List<string>();
    public List<int> tradeQty = new List<int>();
    public List<string> beasts = new List<string>();
    public List<string> herbs = new List<string>();

    public int UsedSlots()
    {
        int n = 0;
        for (int i = 0; i < tradeQty.Count; i++) n += tradeQty[i];
        n += beasts.Count + herbs.Count;
        return n;
    }

    public void AddTrade(string id, int qty)
    {
        int i = tradeIds.IndexOf(id);
        if (i >= 0) tradeQty[i] += qty;
        else { tradeIds.Add(id); tradeQty.Add(qty); }
    }

    public bool RemoveTrade(string id, int qty)
    {
        int i = tradeIds.IndexOf(id);
        if (i < 0 || tradeQty[i] < qty) return false;
        tradeQty[i] -= qty;
        if (tradeQty[i] <= 0) { tradeIds.RemoveAt(i); tradeQty.RemoveAt(i); }
        return true;
    }
}

[Serializable]
public class ShipSave
{
    public int silver;
    public int debt;
    public int supplies;
    public int hull;
    public int cargoCap;
    public int crew;
    public int crewCap;
    public int cannonLevel;
    public int cannonDamage;
    public CargoStacks cargo = new CargoStacks();
    public string dockedPort = "广州";
    public float x, z;
    public float headingY;
    public List<string> seenBeasts = new List<string>();
    public List<string> seenHerbs = new List<string>();
}

[Serializable]
public class AccountRecord
{
    public string user;
    public string passHash;
}

[Serializable]
public class AccountDb
{
    public List<AccountRecord> accounts = new List<AccountRecord>();
}
