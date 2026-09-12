package com.shipgame.nanhai.data;

/** Runtime maneuver memory. Neither frame rate nor camera movement chooses a new escape side. */
public class CombatHelm {
    public float x, y, heading;
    public int side = 1;
    public float sideTime = 6f, retreatTime, retreatHeading, grace;
    public boolean withdrawing;
    public void reset() {
        side = 1; sideTime = 6f; retreatTime = grace = 0; withdrawing = false;
    }
}
