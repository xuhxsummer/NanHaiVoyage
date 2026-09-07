package com.shipgame.nanhai.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.shipgame.nanhai.data.Catalog;

/** Presentation only: callers supply live prices and the existing navigation callback. */
public final class IntelPanel extends Table {
    private final QuestUi chrome;
    private final Table cheap = new Table(), dear = new Table(), arbitrage = new Table();

    public IntelPanel(QuestUi chrome, Runnable close) {
        this.chrome = chrome;
        setBackground(chrome.frame);
        pad(24,32,24,32);
        Table heading = new Table();
        Table title = new Table(); title.setBackground(chrome.blue);
        title.add(label("情报 · 各港价格",36,QuestUi.PAPER)).pad(8,48,8,48);
        heading.add().width(104);
        heading.add(title).expandX();
        heading.add(button("关闭",close)).size(104,56);
        add(heading).size(1136,64).row();
        add(label("市场瞬息万变，把握商机，通商四海",22,QuestUi.PAPER)).height(40).padBottom(16).row();
        Table top = new Table();
        top.add(section(cheap,"最低的3种货（点行自动驶往该港）",false)).size(552,256);
        top.add(section(dear,"最高的3种货（点行自动驶往该港）",false)).size(552,256).padLeft(32);
        add(top).size(1136,256).padBottom(16).row();
        add(section(arbitrage,"套利提示（每个便宜货：哪儿最高）",true)).size(1136,256);
    }

    private Table section(Table rows,String title,boolean spread) {
        Table panel = new Table(); panel.setBackground(chrome.parchment); panel.pad(8);
        panel.add(label(title,22,QuestUi.INK)).height(40).row();
        Table columns = new Table();
        if (spread) {
            column(columns,"货物",144); column(columns,"低价港口及价格",232);
            column(columns,"→",48); column(columns,"高价港口及价格",416);
            column(columns,"每份差价",136); column(columns,"前往",144);
        } else {
            column(columns,"货物",144); column(columns,"港口",112);
            column(columns,"价格（银两）",160); column(columns,"前往",120);
        }
        panel.add(columns).height(32).row();
        panel.add(rows).height(168).growX();
        return panel;
    }

    private void column(Table row,String text,int width) {
        row.add(label(text,20,QuestUi.INK)).width(width).height(32);
    }

    private Label label(String text,int size,Color color) {
        Label label=chrome.label(text,size,color); label.setAlignment(Align.center); return label;
    }

    private Table good(int index) {
        Table cell=new Table(); TextureRegionDrawable icon=IconLib.good(index);
        if(icon!=null) cell.add(new Image(icon)).size(32).padRight(8);
        cell.add(label(Catalog.GOODS[index],22,QuestUi.PAPER));
        return cell;
    }

    private TextButton button(String text,Runnable action) {
        TextButton button=chrome.button(text,false);
        button.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event,float x,float y) {
                event.stop(); action.run();
            }
        });
        return button;
    }

    private Table row(String name,Runnable action) {
        Table row=new Table(); row.setBackground(chrome.inset); row.pad(0); row.setName(name);
        if(action!=null) row.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event,float x,float y) {
                // A button has its own callback; a row tap must navigate exactly once.
                for(Actor target=event.getTarget();target!=null && target!=row;target=target.getParent()) {
                    if(target instanceof TextButton) return;
                }
                action.run();
            }
        });
        return row;
    }

    public void addPrice(boolean lowest,int rank,int good,int port,int price,Runnable navigate) {
        Table row=row((lowest?"情报低":"情报高")+rank,navigate);
        row.add(good(good)).width(144);
        row.add(label(Catalog.PORTS[port],22,QuestUi.PAPER)).width(112);
        row.add(label(Integer.toString(price),24,lowest?QuestUi.JADE:QuestUi.PAPER)).width(160);
        row.add(button("前往",navigate)).size(104,40).padLeft(8).padRight(8);
        (lowest?cheap:dear).add(row).size(536,56).row();
    }

    public void addArbitrage(int rank,int good,int buyPort,int buyPrice,int sellPort,int sellPrice,Runnable navigate) {
        int profit=sellPrice-buyPrice;
        Table row=row("情报套"+rank,profit>0?navigate:null);
        row.add(good(good)).width(144);
        row.add(label(Catalog.PORTS[buyPort]+"  买"+buyPrice,22,QuestUi.PAPER)).width(232);
        row.add(label("→",24,QuestUi.PAPER)).width(48);
        row.add(label(Catalog.PORTS[sellPort]+"  卖"+sellPrice,22,QuestUi.PAPER)).width(416);
        row.add(label(profit>0?"+"+profit:"0",24,profit>0?QuestUi.JADE:QuestUi.PAPER)).width(136);
        TextButton go=button(profit>0?"前往":"没空子",navigate);
        go.setDisabled(profit<=0);
        row.add(go).size(128,40).padLeft(8).padRight(8);
        arbitrage.add(row).size(1120,56).row();
    }
}
