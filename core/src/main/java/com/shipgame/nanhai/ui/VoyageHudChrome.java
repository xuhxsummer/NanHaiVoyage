package com.shipgame.nanhai.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.Disposable;
import java.util.*;

/** HUD-owned, high resolution pixel chrome. Shared popup skins are never changed. */
public final class VoyageHudChrome implements Disposable {
    public static final Color GOLD=Color.valueOf("D8B579"), PAPER=Color.valueOf("F4DEB0"), MUTED=Color.valueOf("B8C5C3");
    public final Drawable goldLine, panel, parchment, red, blue, jade, circle, greenCircle, redCircle, ring, compass;
    /** 0.28.22 半透明深色底板：放在亮色天空上的文字标签后面，保证可读。 */
    public final Drawable plate, questPaper, questPanel;
    private final java.util.List<Texture> owned=new ArrayList<>();
    private final Map<String,Drawable> icons=new HashMap<>();
    private final Skin skin;
    private final BitmapFont font;
    public VoyageHudChrome(Skin skin) {
        this.skin=skin;
        FreeTypeFontGenerator generator=new FreeTypeFontGenerator(Gdx.files.internal("fonts/nanhai-cjk.ttf"));
        try {
            FreeTypeFontGenerator.FreeTypeFontParameter parameter=new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size=28;
            parameter.characters=FreeTypeFontGenerator.DEFAULT_CHARS+Gdx.files.internal("fonts/ui-chars.txt").readString("UTF-8");
            parameter.minFilter=Texture.TextureFilter.Linear;parameter.magFilter=Texture.TextureFilter.Linear;
            font=generator.generateFont(parameter);font.setUseIntegerPositions(false);
        } finally {generator.dispose();}
        Pixmap line=new Pixmap(1,1,Pixmap.Format.RGBA8888);line.setColor(GOLD);line.fill();goldLine=new TextureRegionDrawable(texture(line));
        panel=patch("102631"); parchment=patch("CAB184"); red=patch("622C25"); blue=patch("163A47"); jade=patch("264C42");
        questPaper=patch("CAB184",false);questPanel=patch("102631",false);
        circle=disc("112A36",true,false); greenCircle=disc("315C44",true,false); redCircle=disc("733329",true,false);
        ring=disc("112A36",false,false); compass=disc("142F39",true,true);
        Pixmap platePixmap=new Pixmap(8,8,Pixmap.Format.RGBA8888);
        platePixmap.setColor(0.02f,0.055f,0.09f,0.8f);platePixmap.fill();
        plate=new TextureRegionDrawable(texture(platePixmap));
    }
    private Texture texture(Pixmap p) {Texture t=new Texture(p);p.dispose();t.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);owned.add(t);return t;}
    private Drawable patch(String base) { return patch(base,true); }
    private Drawable patch(String base,boolean corners) {
        Pixmap p=new Pixmap(96,96,Pixmap.Format.RGBA8888); Color c=Color.valueOf(base);
        for(int y=4;y<92;y++) for(int x=4;x<92;x++) {float n=(((x*13+y*7)%5)-2)*.001f+(92-y)*.00018f;p.setColor(c.r+n,c.g+n,c.b+n,.97f);p.drawPixel(x,y);}
        p.setColor(Color.valueOf("463A2B"));p.drawRectangle(3,3,90,90);
        p.setColor(GOLD);p.drawRectangle(5,5,86,86);p.setColor(Color.valueOf("806841"));p.drawRectangle(8,8,80,80);
        p.setColor(PAPER);
        if(corners) for(int x:new int[]{6,89}) for(int y:new int[]{6,89}) {int sx=x<48?1:-1,sy=y<48?1:-1;
            p.drawLine(x,y,x+sx*18,y);p.drawLine(x,y,x,y+sy*18);
            p.drawLine(x+sx*5,y+sy*4,x+sx*12,y+sy*11);p.drawLine(x+sx*12,y+sy*11,x+sx*5,y+sy*18);
            p.drawLine(x+sx*5,y+sy*18,x+sx*5,y+sy*4);
        }
        NinePatch patch=new NinePatch(texture(p),28,28,28,28);patch.setPadding(16,16,12,12);return new NinePatchDrawable(patch);
    }
    private Drawable disc(String base,boolean fill,boolean ticks) {
        Pixmap p=new Pixmap(256,256,Pixmap.Format.RGBA8888);
        if(fill) {Color c=Color.valueOf(base);for(int y=0;y<256;y++)for(int x=0;x<256;x++){float d=(float)Math.hypot(x-128,y-128);if(d<119){float light=(119-d)*.00028f;p.setColor(c.r+light,c.g+light,c.b+light,.94f);p.drawPixel(x,y);}}}
        p.setColor(Color.valueOf("453C2F"));for(int r=119;r<=124;r++)p.drawCircle(128,128,r);
        p.setColor(GOLD);p.drawCircle(128,128,124);p.drawCircle(128,128,118);p.setColor(PAPER);p.drawCircle(128,128,121);
        p.setColor(Color.valueOf("7D6947"));p.drawCircle(128,128,ticks?93:112);
        for(int i=0;i<64;i++){double a=i*Math.PI/32;int r=ticks?(i%4==0?99:105):115;
            p.setColor(i%4==0?GOLD:Color.valueOf("77674B"));p.drawLine(128+(int)(Math.cos(a)*r),128+(int)(Math.sin(a)*r),128+(int)(Math.cos(a)*113),128+(int)(Math.sin(a)*113));}
        if(ticks) {p.setColor(GOLD);for(int i=0;i<4;i++){double a=i*Math.PI/2;int x=128+(int)(Math.cos(a)*97),y=128+(int)(Math.sin(a)*97);p.fillTriangle(x,y,x+(int)(Math.cos(a+2.5)*16),y+(int)(Math.sin(a+2.5)*16),x+(int)(Math.cos(a-2.5)*16),y+(int)(Math.sin(a-2.5)*16));}}
        return new TextureRegionDrawable(texture(p));
    }
    public Label label(String text,float size,Color color){Label l=new Label(text,new Label.LabelStyle(font,color));l.setFontScale(size/28f);return l;}
    public TextButton button(String title,Drawable background,float size){TextButton.TextButtonStyle s=new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));s.font=font;s.up=background;s.down=red;s.over=background;s.checked=background;s.fontColor=PAPER;TextButton b=new TextButton(title,s);b.getLabel().setFontScale(size/28f);return b;}
    public Drawable icon(String name) {
        if(icons.containsKey(name))return icons.get(name);
        Pixmap p=new Pixmap(80,80,Pixmap.Format.RGBA8888);p.setColor(GOLD);
        // Crisp two-pixel strokes, designed at 80px; no inherited white icon discs.
        switch(name) {
            case "ship":
                line(p,10,55,68,55);line(p,10,55,23,67);line(p,23,67,58,67);line(p,58,67,68,55);
                line(p,39,12,39,57);line(p,55,21,55,55);p.fillTriangle(35,16,17,46,35,46);p.fillTriangle(43,18,43,46,54,46);
                for(int y=25;y<48;y+=7){line(p,21,y,34,y);line(p,43,y,53,y);}line(p,13,72,62,72);break;
            case "cargo": case "gift":
                p.drawRectangle(16,30,48,35);p.drawRectangle(14,25,52,12);line(p,39,25,39,65);
                if(name.equals("gift")){p.drawCircle(30,19,9);p.drawCircle(49,19,9);}else {line(p,16,30,39,18);line(p,39,18,64,30);line(p,26,40,26,59);line(p,53,40,53,59);}break;
            case "book": case "quest": case "intel":
                p.drawRectangle(18,15,44,53);line(p,27,15,27,68);for(int y=27;y<58;y+=9)line(p,34,y,55,y);
                if(name.equals("quest")){line(p,31,10,49,10);line(p,31,10,31,20);line(p,49,10,49,20);}break;
            case "anchor":
                p.drawCircle(40,17,7);line(p,40,24,40,65);line(p,23,34,57,34);line(p,40,65,16,48);line(p,40,65,64,48);line(p,16,48,16,39);line(p,64,48,64,39);break;
            case "up":case "down":
                for(int y:new int[]{25,40}){if(name.equals("up")){line(p,18,y+18,40,y);line(p,40,y,62,y+18);}else{line(p,18,y,40,y+18);line(p,40,y+18,62,y);}}break;
            case "silver":p.drawCircle(31,43,18);p.drawCircle(48,30,18);p.drawRectangle(44,25,9,9);p.drawRectangle(26,38,9,9);break;
            case "supply":line(p,15,45,25,62);line(p,25,62,56,62);line(p,56,62,65,45);line(p,15,45,65,45);for(int x=28;x<=52;x+=12)line(p,x,35,x-5,18);break;
            case "crew":p.drawCircle(40,24,10);line(p,31,37,22,63);line(p,49,37,58,63);line(p,22,63,58,63);line(p,31,37,49,37);break;
            case "sun":p.drawCircle(40,40,15);for(int i=0;i<8;i++){double a=i*Math.PI/4;line(p,40+(int)(Math.cos(a)*22),40+(int)(Math.sin(a)*22),40+(int)(Math.cos(a)*31),40+(int)(Math.sin(a)*31));}break;
            default:
                p.drawCircle(40,40,21);p.drawCircle(40,40,6);for(int i=0;i<8;i++){double a=i*Math.PI/4;line(p,40+(int)(Math.cos(a)*8),40+(int)(Math.sin(a)*8),40+(int)(Math.cos(a)*32),40+(int)(Math.sin(a)*32));}break;
        }
        Drawable d=new TextureRegionDrawable(texture(p));icons.put(name,d);return d;
    }
    private void line(Pixmap p,int x,int y,int x2,int y2){p.drawLine(x,y,x2,y2);p.drawLine(x+1,y,x2+1,y2);p.drawLine(x,y+1,x2,y2+1);}
    @Override public void dispose(){for(Texture t:owned)t.dispose();owned.clear();font.dispose();}
}
