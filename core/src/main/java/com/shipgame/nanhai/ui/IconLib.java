package com.shipgame.nanhai.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
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
                Texture texture = new Texture(new IllustrationData(file));
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

        IllustrationData(FileHandle file) { super(file, null, Pixmap.Format.RGBA8888, true); }
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
                return thumbnail;
            } catch (RuntimeException ex) {
                if (thumbnail != null) thumbnail.dispose();
                throw ex;
            } finally { source.dispose(); }
        }
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
