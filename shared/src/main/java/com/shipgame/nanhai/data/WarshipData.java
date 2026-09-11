package com.shipgame.nanhai.data;

/** 0.28.22 海面船队：一艘战船（战级船体）。不存档（运行时海上交通）。 */
public class WarshipData {
    public int ship;
    public float x, y, heading, hp, hpMax;
    public float fireCd;
    public boolean alive = true;
    /** 战斗语境（海盗在场或玩家正在掠夺）时为真：可开炮，并可概率性主动撞船。 */
    public boolean hostile;
    /** 0.28.24: 被玩家撞过/击伤后记仇 —— 等效锁定，和平期也追击玩家。 */
    public boolean provoked;
    /** 和平期巡逻中心与当前角度（运行时 AI 状态）。 */
    public float anchorX, anchorY, patrolAngle;
}
