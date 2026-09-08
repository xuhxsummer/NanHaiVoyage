package com.shipgame.nanhai.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.shipgame.nanhai.data.Catalog;
import com.shipgame.nanhai.data.GameState;

/** Open-book codex. Selection/filtering only read the persistent discovery flags. */
public final class CodexPanel extends Table implements Disposable {
    private final QuestUi chrome;
    private final Skin skin;
    private GameState state;
    private final Runnable close;
    private boolean herbs;
    private int filter, page;
    private final int[] selection = {-1,-1};
    private static final Color MUTED = Color.valueOf("786748");

    public CodexPanel(Skin skin,GameState state,Runnable close) {
        this.skin=skin; this.state=state; this.close=close;
        chrome=new QuestUi(skin);
        refresh();
    }

    private String[] names() { return herbs?Catalog.HERBS:Catalog.BEASTS; }
    private boolean[] found() { return herbs?state.herbFound:state.beastFound; }
    private TextureRegionDrawable icon(int i) { return herbs?IconLib.herb(i):IconLib.beast(i); }

    public void refresh(GameState current) {
        state = current; // Reloading a save replaces GameState on the same voyage screen.
        refresh();
    }

    public void refresh() {
        clear(); setBackground(chrome.frame); pad(24);
        Array<Integer> entries=new Array<>(); int count=0;
        for(int i=0;i<names().length;i++) {
            if(found()[i]) count++;
            if(filter==0 || (filter==1 && found()[i]) || (filter==2 && !found()[i])) entries.add(i);
        }
        int category=herbs?1:0;
        if(!entries.contains(selection[category],false)) {
            selection[category]=entries.size==0?-1:entries.first();
            // Prefer an actually discovered entry; never unlock the reference's turtle.
            if(filter==0) for(int i:entries) if(found()[i]) { selection[category]=i; break; }
        }
        page=Math.min(page,Math.max(0,(entries.size-1)/9));
        Table ribbon=new Table(); ribbon.setBackground(chrome.blue);
        Label book=label("图\n鉴",40,QuestUi.PAPER); book.setAlignment(Align.center);
        ribbon.add(book).expandY().top().padTop(56).row();
        ribbon.add(label("南\n海\n航\n程",22,QuestUi.PAPER)).padBottom(64);
        add(ribbon).size(96,736);

        Table left=new Table(); left.setBackground(chrome.parchment); left.pad(16);
        Table categories=new Table();
        categories.add(button("异兽",!herbs,()->switchCategory(false))).size(248,48).padRight(8);
        categories.add(button("草药",herbs,()->switchCategory(true))).size(248,48);
        left.add(categories).height(48).padBottom(8).row();
        Table summary=new Table();
        summary.add(label((herbs?"草药":"异兽")+"收集进度："+count+"/"+names().length,22,QuestUi.INK)).expandX().left();
        summary.add(label("默认排序",20,MUTED)).right();
        left.add(summary).width(504).height(40).padBottom(8).row();
        Table grid=new Table();
        for(int slot=0;slot<9;slot++) {
            int at=page*9+slot;
            Table card=new Table(); card.pad(8);
            if(at<entries.size) {
                final int index=entries.get(at);
                boolean known=found()[index], selected=index==selection[category];
                card.setBackground(selected?chrome.selected:chrome.parchment);
                TextureRegionDrawable art=icon(index);
                if(art!=null) {
                    Image image=new Image(art);
                    if(!known) image.setColor(.25f,.24f,.18f,.35f);
                    card.add(image).size(112).row();
                } else card.add(label("？",48,MUTED)).height(112).row();
                card.add(label(known?names()[index]:"？？？",22,selected?QuestUi.PAPER:QuestUi.INK)).height(24);
                card.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent event,float x,float y) {
                        selection[herbs?1:0]=index; refresh();
                    }
                });
            } else {
                card.setBackground(chrome.parchment);
                card.add(label("—",24,MUTED)); // empty slot, not a fictitious collectible
            }
            grid.add(card).size(160,160).pad(4);
            if(slot%3==2) grid.row();
        }
        left.add(grid).size(504,504).row();
        Table filters=new Table(); String[] labels={"全部","已发现","未发现"};
        for(int i=0;i<labels.length;i++) {
            final int value=i;
            filters.add(button(labels[i],filter==i,()->{filter=value;page=0;refresh();})).size(160,48).pad(4);
        }
        left.add(filters).height(56).row();
        if(entries.size>9) {
            Table pages=new Table();
            TextButton prev=button("上一页",false,()->{page--;refresh();}); prev.setDisabled(page==0);
            TextButton next=button("下一页",false,()->{page++;refresh();}); next.setDisabled((page+1)*9>=entries.size);
            pages.add(prev).size(144,40); pages.add(label((page+1)+"/"+((entries.size+8)/9),20,MUTED)).width(144);
            pages.add(next).size(144,40); left.add(pages).height(40);
        } else left.add(label(entries.size==0?"暂无条目":"点击图鉴条目可查看详细信息",20,MUTED)).height(40);
        add(left).size(552,736);
        Table spine=new Table(); spine.setBackground(chrome.inset); add(spine).size(24,736);
        add(detail(selection[category])).size(608,736);
    }

    private Table detail(int index) {
        Table right=new Table();right.setBackground(chrome.parchment);right.pad(24);
        Table heading=new Table();
        boolean known=index>=0 && found()[index];
        heading.add(label(index<0?"图鉴":known?names()[index]:"？？？",38,QuestUi.INK)).expandX().left();
        heading.add(button("关闭",false,close)).size(96,48);
        right.add(heading).width(560).height(56).padBottom(16).row();
        Table content=new Table();
        Table tags=new Table();
        tags.add(label(herbs?"草药":"异兽",22,QuestUi.INK)).padRight(24);
        tags.add(label(known?"已发现":"未发现",22,known?Color.valueOf("426548"):MUTED));
        content.add(tags).left().height(32).row();
        Table portrait=new Table();
        String desc=index<0?"当前筛选没有条目。请切换分类或筛选。":known
                ? (herbs?"在岛屿搜采时发现的草药，可在港口出售。":"在岛屿搜采时发现的异兽，收集记录保留在图鉴中。")
                : "尚未发现。靠近岛屿后搜采，发现后解锁图鉴。";
        Label description=label(desc,24,QuestUi.INK);description.setWrap(true);
        portrait.add(description).width(272).top().padTop(24).padRight(16);
        TextureRegionDrawable art=index>=0?icon(index):null;
        if(known && art!=null) portrait.add(new Image(art)).size(256);
        else portrait.add(label("？？？",40,MUTED)).size(256);
        content.add(portrait).width(544).height(272).row();
        section(content,"收集记录");
        if(known) {
            int quantity=herbs?state.herbs[index]:state.beasts[index];
            int price=herbs?Catalog.HERB_PRICE[index]:Catalog.BEAST_PRICE[index];
            paragraph(content,"持有："+quantity+"    卖价："+price+"两");
        } else paragraph(content,"？？？");
        section(content,"可能掉落");paragraph(content,"暂无记载");
        section(content,"栖息地");paragraph(content,known?"岛屿搜采；具体地点暂无记载。":"？？？");
        section(content,"传说故事");paragraph(content,"暂无记载");
        ScrollPane.ScrollPaneStyle scrollStyle=new ScrollPane.ScrollPaneStyle(skin.get(ScrollPane.ScrollPaneStyle.class));
        scrollStyle.background=null;
        ScrollPane scroll=new ScrollPane(content,scrollStyle);scroll.setScrollingDisabled(true,false);
        scroll.setFadeScrollBars(false);scroll.setOverscroll(false,false);
        right.add(scroll).size(560,592);
        return right;
    }

    private void section(Table content,String text) {
        Table band=new Table();band.setBackground(chrome.parchment);
        band.add(label(text,24,QuestUi.INK)).left().expandX().pad(8);
        content.add(band).width(544).height(40).padTop(8).row();
    }
    private void paragraph(Table content,String text) {
        Label label=label(text,22,QuestUi.INK);label.setWrap(true);
        content.add(label).width(528).left().pad(8).row();
    }
    private Label label(String text,int size,Color color) {return chrome.label(text,Math.max(24,size),color);}
    private TextButton button(String text,boolean selected,Runnable action) {
        TextButton button=chrome.button(text,selected); button.getLabel().setFontScale(24f/22f);
        button.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event,float x,float y) {action.run();}
        });return button;
    }
    private void switchCategory(boolean value) {herbs=value;page=0;refresh();}
    @Override public void dispose() {chrome.dispose();}
}
