package com.shipgame.nanhai.data;

/** 0.28.22 海面船队：一艘和平货船（商级船体）。不存档（运行时海上交通）。 */
public class TraderData {
    public int ship;
    public float x, y, heading, hp, hpMax;
    public float fireCd;
    public int targetPort = -1;
    public boolean alive = true;
    /** 被撞/被攻击后警觉：还击并加速逃离（商船从不主动撞击）。 */
    public boolean hostile;
}
