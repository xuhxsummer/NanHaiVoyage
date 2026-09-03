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
}
