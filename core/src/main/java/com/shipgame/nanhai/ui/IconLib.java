package com.shipgame.nanhai.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FileTextureData;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.shipgame.nanhai.data.Catalog;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared, lazy icons. Imported illustrations use Catalog names; legacy pixel
 * icons use positional slugs from tools/gen_icons.py. Missing art falls back to
 * the old icon or null, allowing the caller to retain its text/marker.
 */
public final class IconLib {

    private static final String[] GOOD_SLUGS = {
            "silk", "porcelain", "tea", "salt", "iron", "rice", "sugarcane",
            "agarwood", "sappanwood", "pepper", "ivory", "pearl",
            "tortoiseshell", "betel",
            "cotton", "lacquer", "bronze", "glass", "frankincense", "myrrh",
            "cardamom", "clove", "coral", "rhinohorn"
    };
    private static final String[] BEAST_SLUGS = {
            "jingwei", "nine-tail-fox", "gu-diao", "flying-fish",
            "chang-you", "xing-xing", "bai-ze", "three-leg-turtle"
    };
    private static final String[] HERB_SLUGS = {
            "ginseng", "lingzhi", "fuling", "danggui", "heshouwu",
            "guizhi", "gancao", "chrysanthemum"
    };
    private static final String[] FISH_SLUGS = {
            "yellow-croaker", "hairtail", "bass", "grouper", "tuna", "big-yellow-croaker"
    };
    /** 0.26.4 商城船只商品图（下标与 Catalog.SHIPS 对齐，pinyin slug）。 */
    private static final String[] SHIP_SLUGS = {
            "xiao-shangchuan", "lou-chuan", "meng-chong", "cao-fang", "dou-jian",
            "zou-ge", "huo-bo", "you-ting", "hai-gu"
    };

    private static final Map<String, TextureRegionDrawable> cache = new HashMap<>();

    private IconLib() {}

    /** Drawable for Catalog.GOODS[i], or null if missing. */
    public static TextureRegionDrawable good(int i) {
        return get("goods", GOOD_SLUGS, i);
    }

    /** Drawable for Catalog.BEASTS[i], or null if missing. */
    public static TextureRegionDrawable beast(int i) {
        TextureRegionDrawable art = named("beasts", "beast", Catalog.BEASTS, i);
        return art != null ? art : get("beasts", BEAST_SLUGS, i);
    }

    /** Drawable for Catalog.HERBS[i], or null if missing. */
    public static TextureRegionDrawable herb(int i) {
        TextureRegionDrawable art = named("herbs", "herb", Catalog.HERBS, i);
        return art != null ? art : get("herbs", HERB_SLUGS, i);
    }

    public static TextureRegionDrawable port(int i) {
        return named("ports", "port", Catalog.PORTS, i);
    }

    public static TextureRegionDrawable island(int i) {
        return named("islands", "island", Catalog.ISLANDS, i);
    }

    /** Four circular captain portraits; missing PNGs get distinct Tang-style placeholder portraits. */
    public static TextureRegionDrawable avatar(int index) {
        final int id=Math.max(0,Math.min(3,index));
        String key="avatars/avatar_0"+(id+1);
        if(!cache.containsKey(key)) {
            Texture texture=new Texture(new AvatarData(key,id));
            texture.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);
            cache.put(key,new TextureRegionDrawable(new TextureRegion(texture)));
        }
        return cache.get(key);
    }

    private static final class AvatarData implements TextureData {
        private final String path; private final int id; private boolean prepared;
        AvatarData(String path,int id){this.path="textures/"+path+".png";this.id=id;}
        public TextureDataType getType(){return TextureDataType.Pixmap;}
        public boolean isPrepared(){return prepared;}
        public void prepare(){prepared=true;}
        public int getWidth(){return 256;}
        public int getHeight(){return 256;}
        public Pixmap.Format getFormat(){return Pixmap.Format.RGBA8888;}
        public boolean useMipMaps(){return false;}
        public boolean isManaged(){return true;}
        public boolean disposePixmap(){return true;}
        public void consumeCustomData(int target){throw new UnsupportedOperationException();}
        public Pixmap consumePixmap() {
            prepared=false;
            Pixmap p=new Pixmap(256,256,Pixmap.Format.RGBA8888);
            p.setBlending(Pixmap.Blending.None);
            if(Gdx.files.internal(path).exists()) {
                Pixmap source=new Pixmap(Gdx.files.internal(path));
                p.setFilter(Pixmap.Filter.BiLinear);
                int side=Math.min(source.getWidth(),source.getHeight());
                p.drawPixmap(source,(source.getWidth()-side)/2,(source.getHeight()-side)/2,side,side,0,0,256,256);
                source.dispose();
            } else {
                p.setColor(new int[]{0x294e58ff,0x5b3438ff,0x465d3fff,0x3d425aff}[id]); p.fill();
                p.setColor(new int[]{0x9b503bff,0x50778aff,0xc29952ff,0x6b6ca0ff}[id]);
                p.fillCircle(128,240,90);
                p.setColor(0xdec299ff);p.fillRectangle(113,157,30,40);p.fillCircle(128,109,48);
                p.setColor(0x282622ff);p.fillRectangle(81,58,94,32);
                if(id%2==0) {p.fillRectangle(78,48,100,30);p.fillRectangle(65,65,126,15);}
                else {p.fillCircle(128,47,24);p.fillRectangle(79,78,13,71);p.fillRectangle(164,78,13,71);}
                p.fillCircle(111,111,4);p.fillCircle(145,111,4);
                p.setColor(0x9c6046ff);p.fillRectangle(117,140,22,3);
                p.setColor(0xe0c591ff);p.fillTriangle(91,182,124,236,126,198);p.fillTriangle(165,182,132,236,130,198);
            }
            for(int y=0;y<256;y++) for(int x=0;x<256;x++) {
                float d=(x-127.5f)*(x-127.5f)+(y-127.5f)*(y-127.5f);
                if(d>126*126) p.drawPixel(x,y,0);
                else if(d>120*120) p.drawPixel(x,y,0xd8b673ff);
            }
            return p;
        }
    }

    /** 0.28.25 对话半身像：ui/dialogue/<slug>.png，透明纸片人；缺图返回 null。 */
    public static TextureRegionDrawable bust(String slug) {
        String key = "dialogue/" + slug;
        if (cache.containsKey(key)) return cache.get(key);
        TextureRegionDrawable art = null;
        try {
            String path = "ui/dialogue/" + slug + ".png";
            FileHandle file = Gdx.files.internal(path);
            if (file.exists()) {
                Texture texture = new Texture(file);
                texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                art = new TextureRegionDrawable(new TextureRegion(texture));
            }
        } catch (Throwable t) {
            Gdx.app.error("IconLib", "bust failed: " + slug, t);
        }
        cache.put(key, art);
        return art;
    }

    /** Drawable for Catalog.FISH[i] (0.26.3 渔获), or null if missing. */
    public static TextureRegionDrawable fish(int i) {
        return get("fish", FISH_SLUGS, i);
    }

    /** Drawable for Catalog.SHIPS[i] (0.26.4 商城商品图), or null if missing. */
    public static TextureRegionDrawable ship(int i) {
        return get("ships", SHIP_SLUGS, i);
    }

    /** Drawable for a 0.26.2 HUD icon by name (silver/supply/hull/crew/avatar/
     * cargo/codex/port/intel/quest/sun), or null if the PNG is missing. */
    public static TextureRegionDrawable hud(String slug) {
        String key = "hud/" + slug;
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        TextureRegionDrawable d = tryLoad("hud", slug);
        cache.put(key, d);
        return d;
    }

    private static TextureRegionDrawable get(String folder, String[] slugs, int i) {
        if (i < 0 || i >= slugs.length) {
            return null;
        }
        String slug = slugs[i];
        String key = folder + "/" + slug;
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        TextureRegionDrawable d = tryLoad(folder, slug);
        cache.put(key, d);
        return d;
    }

    private static TextureRegionDrawable tryLoad(String folder, String slug) {
        try {
            String path = "textures/" + folder + "/" + slug + ".png";
            if (!Gdx.files.internal(path).exists()) {
                Gdx.app.log("IconLib", "missing icon: " + path);
                return null;
            }
            Texture t = new Texture(Gdx.files.internal(path));
            t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            return new TextureRegionDrawable(new TextureRegion(t));
        } catch (Throwable t2) {
            Gdx.app.error("IconLib", "icon failed: " + folder + "/" + slug, t2);
            return null;
        }
    }

    private static TextureRegionDrawable named(String folder, String prefix, String[] names, int i) {
        if (i < 0 || i >= names.length) return null;
        String key = folder + "/" + prefix + "_" + names[i];
        if (cache.containsKey(key)) return cache.get(key);
        TextureRegionDrawable art = null;
        try {
            FileHandle file = Gdx.files.internal("textures/" + key + ".png");
            if (!file.exists()) file = Gdx.files.internal("textures/" + key + ".jpg");
            if (file.exists()) {
                Texture texture = new Texture(new IllustrationData(file, folder.equals("ports") || folder.equals("islands")));
                texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
                art = new TextureRegionDrawable(new TextureRegion(texture));
            }
        } catch (RuntimeException ex) {
            Gdx.app.error("IconLib", "illustration failed: " + key, ex);
        }
        cache.put(key, art);
        return art;
    }

    /** At most 256² + mipmaps per illustration (the whole chart is ~11 MiB).
     * FileTextureData reopens the original after Android GL context loss. The
     * square power-of-two canvas preserves aspect ratio and GLES2 mip support;
     * neither the decoded original nor the thumbnail is retained on the CPU. */
    private static final class IllustrationData extends FileTextureData {
        private static final int SIZE = 256;
        private final boolean location;

        IllustrationData(FileHandle file, boolean location) { super(file, null, Pixmap.Format.RGBA8888, true); this.location=location; }
        @Override public int getWidth() { return SIZE; }
        @Override public int getHeight() { return SIZE; }

        @Override public Pixmap consumePixmap() {
            Pixmap source = super.consumePixmap();
            Pixmap thumbnail = null;
            try {
                thumbnail = new Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888);
                thumbnail.setBlending(Pixmap.Blending.None);
                thumbnail.setFilter(Pixmap.Filter.BiLinear);
                float scale = Math.min((float)SIZE / source.getWidth(), (float)SIZE / source.getHeight());
                int width = Math.max(1, Math.round(source.getWidth() * scale));
                int height = Math.max(1, Math.round(source.getHeight() * scale));
                thumbnail.drawPixmap(source, 0, 0, source.getWidth(), source.getHeight(),
                        (SIZE - width) / 2, (SIZE - height) / 2, width, height);
                if(location) stripLocationMatte(thumbnail);
                return thumbnail;
            } catch (RuntimeException ex) {
                if (thumbnail != null) thumbnail.dispose();
                throw ex;
            } finally { source.dispose(); }
        }
    }

    /** Remove only edge-connected neutral checker/white matte or pale paper.
     * Interior white sails, roofs and sand remain opaque. Runs again on managed reload. */
    private static void stripLocationMatte(Pixmap image) {
        int w=image.getWidth(),h=image.getHeight();
        boolean[] seen=new boolean[w*h]; int[] queue=new int[w*h]; int tail=0;
        for(int y=0;y<h;y++) for(int x=0;x<w;x++) if(x==0 || y==0 || x==w-1 || y==h-1) {
            int index=y*w+x;
            if(matte(image.getPixel(x,y))) {seen[index]=true;queue[tail++]=index;}
        }
        for(int head=0;head<tail;head++) {
            int index=queue[head],x=index%w,y=index/w;
            image.drawPixel(x,y,image.getPixel(x,y)&0xffffff00);
            for(int direction=0;direction<4;direction++) {
                int nx=x+(direction==0?-1:direction==1?1:0),ny=y+(direction==2?-1:direction==3?1:0);
                if(nx<0 || ny<0 || nx>=w || ny>=h) continue;
                int next=ny*w+nx;
                if(seen[next]) continue;
                seen[next]=true;
                if(matte(image.getPixel(nx,ny)))queue[tail++]=next;
            }
        }
    }
    private static boolean matte(int rgba) {
        int r=rgba>>>24,g=(rgba>>>16)&255,b=(rgba>>>8)&255;
        int lo=Math.min(r,Math.min(g,b)),hi=Math.max(r,Math.max(g,b));
        return (rgba&255)<8 || (lo>=60 && hi-lo<=22)
                || (r>=215 && g>=195 && b>=155 && r>=g && g>=b && r-b<=70);
    }

    /** Owned by the application, since several screens share these drawables. */
    public static void dispose() {
        for (TextureRegionDrawable icon : cache.values()) {
            if (icon != null) icon.getRegion().getTexture().dispose();
        }
        cache.clear();
    }

    /** Smallest array length guard so slug tables can't drift from Catalog. */
    public static void checkAgainstCatalog() {
        if (GOOD_SLUGS.length != Catalog.GOODS.length
                || BEAST_SLUGS.length != Catalog.BEASTS.length
                || HERB_SLUGS.length != Catalog.HERBS.length
                || FISH_SLUGS.length != Catalog.FISH.length
                || SHIP_SLUGS.length != Catalog.SHIPS.length) {
            Gdx.app.error("IconLib", "slug table length mismatch with Catalog");
        }
    }
}
