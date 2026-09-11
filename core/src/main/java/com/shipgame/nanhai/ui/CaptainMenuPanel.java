package com.shipgame.nanhai.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Disposable;
import com.shipgame.nanhai.data.GameState;
import java.util.function.Consumer;

/** Fullscreen captain profile and account actions; persistence stays in VoyageScreen. */
public final class CaptainMenuPanel extends Table implements Disposable {
    public static final float WIDTH=1296, HEIGHT=800;
    private static final String[] NAV={"个人信息","存档","调试","账号","设置"};
    private final QuestUi ui;
    private final Skin skin;
    private final Runnable save,load,logout,fill,close,profileChanged;
    private final Consumer<Boolean> cameraChanged, autosaveChanged;
    private boolean godView, autosaveEnabled;
    private final Table body=new Table(), nav=new Table();
    private final Label message;
    private GameState g;
    private int selection;
    private TextField nickname;

    public CaptainMenuPanel(Skin skin,GameState state,Runnable profileChanged,Runnable save,Runnable load,
                            Runnable logout,Runnable fill,Runnable close,Consumer<Boolean> cameraChanged,Consumer<Boolean> autosaveChanged,boolean initialGodView,boolean initialAutosave) {
        this.skin=skin; this.g=state; this.profileChanged=profileChanged;
        this.save=save; this.load=load; this.logout=logout; this.fill=fill; this.close=close;
        this.cameraChanged=cameraChanged; this.autosaveChanged=autosaveChanged; this.godView=initialGodView; this.autosaveEnabled=initialAutosave;
        ui=new QuestUi(skin); setBackground(ui.frame); pad(24); top();
        Table heading=new Table();
        heading.add(ui.label("船长 · 世界暂停",36,QuestUi.PAPER)).expandX().left();
        heading.add(action("关闭",true,close)).size(112,64);
        add(heading).growX().height(72).padBottom(20).row();
        nav.setBackground(ui.inset); nav.pad(20).top();
        body.setBackground(ui.inset); body.pad(28).top().left();
        Table panes=new Table(); panes.add(nav).width(240).growY().padRight(24);
        panes.add(body).grow(); add(panes).grow().row();
        message=ui.label("昵称与头像会随当前账号保存。",24,QuestUi.JADE);
        add(message).growX().height(48).left().padTop(12);
        rebuild();
    }
    public void refresh(GameState state) { g=state; rebuild(); }
    public void openProfile() { selection=0; rebuild(); }
    private void rebuild() {
        nav.clearChildren(); body.clearChildren(); nickname=null;
        for(int i=0;i<NAV.length;i++) {
            final int index=i;
            nav.add(action(NAV[i],selection==i,()->{commitNickname();selection=index;rebuild();})).size(200,72).padBottom(16).row();
        }
        body.add(ui.label(NAV[selection],32,QuestUi.PAPER)).growX().left().height(48).padBottom(24).row();
        if(selection==0) profile();
        else if(selection==1) {
            copy("保存进度：把当前航程写入本机存档。\n读取存档：回到最近一次保存的进度。");
            body.add(action("保存进度",false,()->{save.run();message.setText(g.toast);})).size(480,72).left().padTop(32).row();
            body.add(action("读取存档",false,load)).size(480,72).left().padTop(20).row();
        } else if(selection==2) {
            copy("满级账号：解锁全部船只、异兽与草药，并补满资源。\n会覆盖当前账号的本机存档。");
            body.add(action("满级账号",true,fill)).size(480,72).left().padTop(32).row();
        } else if(selection==3) {
            copy("退出登录前会保存当前进度。\n再次登录这个账号即可继续航程。");
            body.add(action("退出登录",true,logout)).size(480,72).left().padTop(32).row();
        } else {
            // 0.28.21: 视角设置 —— 默认为航行追尾（战斗自动拉远），上帝视角恒用
            // 战斗级拉远；立即生效并写入本机偏好。
            copy("视角：默认为航行追尾视角，战斗自动拉远。\n上帝视角始终用战斗级拉远俯瞰，战斗结束后不回拉。");
            Table modes=new Table(); modes.left();
            modes.add(action("默认视角",!godView,()->{godView=false;cameraChanged.accept(false);rebuild();})).size(240,72).padRight(24);
            modes.add(action("上帝视角",godView,()->{godView=true;cameraChanged.accept(true);rebuild();})).size(240,72).row();
            Table saveModes=new Table(); saveModes.left();
            saveModes.add(action(autosaveEnabled?"自动存档：开":"自动存档：关",autosaveEnabled,()->{autosaveEnabled=!autosaveEnabled; autosaveChanged.accept(autosaveEnabled); rebuild();})).size(240,72);
            body.add(modes).left().padTop(32).row();
            body.add(saveModes).left().padTop(18).row();
        }
    }
    private void profile() {
        Table personal=new Table();
        Image portrait=new Image(IconLib.avatar(g.avatarIndex)); portrait.setName("当前头像");
        personal.add(portrait).size(128).padRight(24);
        Table fields=new Table(); fields.left();
        fields.add(ui.label("昵称（最多16字）",26,QuestUi.PAPER)).left().padBottom(12).row();
        TextField.TextFieldStyle style=new TextField.TextFieldStyle(skin.get(TextField.TextFieldStyle.class));
        style.background=ui.blue; style.fontColor=QuestUi.PAPER;
        nickname=new TextField(g.nickname,style); nickname.setName("昵称"); nickname.setMaxLength(16);
        nickname.setOnlyFontChars(false);
        fields.add(nickname).width(384).height(64).left();
        fields.add(action("保存昵称",true,()->{commitNickname();message.setText("昵称已保存。");})).size(144,64).padLeft(16);
        personal.add(fields).growX(); body.add(personal).growX().left().padBottom(32).row();
        body.add(ui.label("选择头像",28,QuestUi.PAPER)).left().height(48).row();
        Table portraits=new Table(); portraits.left();
        for(int i=0;i<4;i++) {
            final int avatar=i; Table card=new Table(); card.setName("头像"+(i+1));
            card.setBackground(g.avatarIndex==i?ui.selected:ui.blue); card.pad(12);
            card.add(new Image(IconLib.avatar(i))).size(136).row();
            card.add(ui.label(g.avatarIndex==i?"已选择":"头像 "+(i+1),24,QuestUi.PAPER)).height(40);
            card.addListener(new ClickListener(){@Override public void clicked(InputEvent event,float x,float y){
                commitNickname(); g.setProfile(g.nickname,avatar); profileChanged.run();
                rebuild(); message.setText("头像已保存。"); event.stop();
            }});
            portraits.add(card).size(172,204).padRight(i==3?0:16);
        }
        body.add(portraits).growX().left();
    }
    public void commitNickname() {
        if(nickname==null) return;
        g.setProfile(nickname.getText(),g.avatarIndex); profileChanged.run();
        nickname.setText(g.nickname);
        if(getStage()!=null) getStage().setKeyboardFocus(null);
        Gdx.input.setOnscreenKeyboardVisible(false);
    }
    public void showFillConfirmation(Runnable confirm) {
        body.clearChildren(); nickname=null;
        Table confirmation=new Table(); confirmation.setName("confirmFillAccount"); confirmation.top().left();
        Label explanation=ui.label("将全开当前账号的船、异兽、草药，补满船员、补给和船体。\n\n将覆盖本机存档，不能回到之前的进度。是否继续？",28,QuestUi.PAPER);
        explanation.setWrap(true);
        confirmation.add(ui.label("满级账号",36,QuestUi.PAPER)).left().padBottom(32).row();
        confirmation.add(explanation).width(720).left().padBottom(40).row();
        Table actions=new Table();
        actions.add(action("取消",false,this::rebuild)).size(240,72).padRight(24);
        actions.add(action("继续",true,confirm)).size(240,72);
        confirmation.add(actions).left(); body.add(confirmation).grow().top();
    }
    private void copy(String text) {
        Label label=ui.label(text,28,QuestUi.PAPER); label.setWrap(true);
        body.add(label).growX().left().padBottom(16).row();
    }
    private TextButton action(String text,boolean primary,Runnable action) {
        TextButton b=ui.button(text,primary); b.setName(text); b.getLabel().setFontScale(28f/22f);
        b.addListener(new ClickListener(){@Override public void clicked(InputEvent event,float x,float y){event.stop();action.run();}});
        return b;
    }
    @Override public void dispose(){ui.dispose();}
}
