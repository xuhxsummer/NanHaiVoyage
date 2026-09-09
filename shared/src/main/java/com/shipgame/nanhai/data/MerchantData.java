package com.shipgame.nanhai.data;

/** One persistent independent trader. Missing from old saves means no trader yet. */
public class MerchantData {
    public int ship;
    public float x, y, heading, hp, hpMax;
    public int damage, silver, cargoGood, cargo;
    public int targetPort = -1, targetIsland = -1;
    public float rest, fireCd;
    public boolean hostile;

    public MerchantData copy() {
        MerchantData m = new MerchantData();
        m.ship=ship; m.x=x; m.y=y; m.heading=heading; m.hp=hp; m.hpMax=hpMax;
        m.damage=damage; m.silver=silver; m.cargoGood=cargoGood; m.cargo=cargo;
        m.targetPort=targetPort; m.targetIsland=targetIsland; m.rest=rest; m.fireCd=fireCd;
        m.hostile=hostile;
        return m;
    }
}
