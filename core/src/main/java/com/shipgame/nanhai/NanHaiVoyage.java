package com.shipgame.nanhai;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.shipgame.nanhai.data.AccountStore;
import com.shipgame.nanhai.data.GameState;
import com.shipgame.nanhai.screen.LoginScreen;
import com.shipgame.nanhai.ui.UiFactory;

public class NanHaiVoyage extends Game {

    public static final String FONT_CHARS = FreeTypeFontGenerator.DEFAULT_CHARS
            + "登录注册用户名密码确认取消账号存档错误成功请输入已存在不存在进入航海游戏"
            + "南海航程银两补给耐久船员债风向雨雾晴速度货舱容量伤害上限实际缺口利息欠款"
            + "小地图全图取消自动锁定加速减速图鉴货物丢弃靠港离港买卖行情本港各港修理升级仓库炮火编制雇人"
            + "立刻花钱搜采岛屿满舱不能持有库存数量单价总价购买出售获得新记录没有发现"
            + "丝绸瓷器茶叶盐铁器米粮蔗糖沉香苏木胡椒象牙珍珠玳瑁槟榔"
            + "广州潮州雷州琼州崖州合浦交州占城真腊佛逝"
            + "精卫九尾狐蛊雕文鳐鱼长右狌白泽羽民国三足龟"
            + "人参灵芝茯苓当归何首乌桂枝甘草菊花"
            + "海盗开火逃跑缴获失败读档船沉耗尽还击追击就地范围暂停世界驶向摇杆"
            + "顺风逆风侧风滑行三栏商货异兽草药共用可离港无有级按键点长按"
            + "天气战斗港口菜单列表还债借债补足修好雇满未满员火力消耗"
            + "点选敌船连续离开战斗返回手动继续开船点港口"
            + "点击空白关闭详情当前目标距离东沙西沙南澳琼东礁屿探岛发现"
            + "空空如也银不足舱已满修理完毕升级完成已雇人补给已满无需"
            + "利息已计入本次回港离开港口出海失败了将读取上次靠港存档"
            + "点击海盗锁定取消锁定按钮加速中减速中滑行中自动航行中战斗中停泊中探岛中"
            + "风向北南东西吨石匹两枚第级已在靠近此处搜过带不走先卖掉或丢掉"
            + "搜了一圈没有新发现采得一期只可卖钱数量不对没有这么多货没有这只"
            + "海上丢弃不在岛上离岛后再来世界暂停欠款计息现欠花两银两不够借债"
            + "船体无需修船要仓库升级炮火升级人数上限升到再花钱雇人上船立刻雇上"
            + "船员火力与补给按实际人数自动驶向改回手动已锁定海盗自动开火取消锁定"
            + "遭遇海盗点船锁定开火默认就地打还击会追得紧已开出范围海盗停火"
            + "打赢海盗抢得缴获当货物卖掉一件船部件额外银两补给耗尽船沉"
            + "按缺口补补给没有欠款没有银两可还不还也能离港还债剩余欠款"
            + "已满员先升编制雇人要关游戏不丢本机登录本机存档或登录已有账号"
            + "账号已存在请登录注册失败用户名或密码不对或账号不存在"
            + "货舱三栏共用容量海上可丢货丢了就没了点货物再点丢掉丢掉选中"
            + "各港行情固定返回列表只卖随时可开靠岸菜单异兽草药进图鉴草药只卖钱"
            + "补给空或船沉都直接失败读取上次靠港存档读档重来"
            + "只显示港和岛无海盗点港口自动驶向点空白关闭"
            + "，。、：；！？「」（）【】—…·《》％／－＋＝＊[]x/";

    public SpriteBatch batch;
    public BitmapFont font;
    public BitmapFont fontSmall;
    public Skin skin;
    public AccountStore accounts;
    public String currentUser;
    public GameState state;

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = loadFont(22);
        fontSmall = loadFont(16);
        skin = UiFactory.create(font, fontSmall);
        accounts = new AccountStore();
        setScreen(new LoginScreen(this));
    }

    private BitmapFont loadFont(int size) {
        try {
            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/nanhai-cjk.ttf"));
            FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
            p.size = size;
            p.color = Color.WHITE;
            p.borderWidth = 1.1f;
            p.borderColor = new Color(0f, 0f, 0f, 0.75f);
            p.characters = FONT_CHARS;
            BitmapFont f = gen.generateFont(p);
            gen.dispose();
            return f;
        } catch (Exception ex) {
            Gdx.app.error("NanHaiVoyage", "font load failed", ex);
            BitmapFont fallback = new BitmapFont();
            fallback.getData().setScale(size / 16f);
            return fallback;
        }
    }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().hide();
        }
        if (skin != null) skin.dispose();
        if (font != null) font.dispose();
        if (fontSmall != null) fontSmall.dispose();
        if (batch != null) batch.dispose();
    }
}
