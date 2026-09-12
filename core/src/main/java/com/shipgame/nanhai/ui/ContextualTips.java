package com.shipgame.nanhai.ui;

/** One screen instance = one login cycle. No save/prefs state; the screen pauses the world while active. */
public final class ContextualTips {
    public enum Tip {
        STICK("推动左下摇杆，控制船只航行"),
        DOCK("靠近港岛后，点船旁「抛锚」或「停靠」"),
        PIRATE_LOCK("点海盗船或「锁定」开始交战"),
        PORT_TRADE("在港口可补给、买卖货物"),
        QUEST_CARD("点右侧任务卡，查看剧情与目标");
        public final String copy;
        Tip(String copy) { this.copy=copy; }
        public int bit() { return 1<<ordinal(); }
    }
    private static final Tip[] PRIORITY={Tip.PIRATE_LOCK,Tip.PORT_TRADE,Tip.DOCK,Tip.STICK,Tip.QUEST_CARD};
    private int shown;
    private Tip active;
    private float gap;
    public Tip active() { return active; }
    public boolean shown(Tip tip) { return (shown & tip.bit())!=0; }
    public void complete(Tip tip) {
        shown|=tip.bit();
        if(active==tip) dismiss();
    }
    public void dismiss() { active=null; gap=.5f; }
    public void update(float dt,int eligible) {
        if(active!=null) {
            if((eligible & active.bit())==0) dismiss();
            else return;
        }
        gap=Math.max(0,gap-Math.max(0,dt));
        if(gap>0) return;
        for(Tip tip:PRIORITY) if((eligible & tip.bit())!=0 && !shown(tip)) {
            active=tip;shown|=tip.bit();return;
        }
    }
}
