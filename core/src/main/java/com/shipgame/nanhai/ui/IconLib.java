package com.shipgame.nanhai.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.shipgame.nanhai.data.Catalog;

import java.util.HashMap;
import java.util.Map;

/**
 * Lazy, missing-safe pixel icons for goods / beasts / herbs.
 * Slugs are positional (index i -> Catalog array entry i), matching
 * tools/gen_icons.py. A missing file never crashes: null is returned and the
 * caller falls back to the plain text row.
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

    private static final Map<String, TextureRegionDrawable> cache = new HashMap<>();

    private IconLib() {}

    /** Drawable for Catalog.GOODS[i], or null if missing. */
    public static TextureRegionDrawable good(int i) {
        return get("goods", GOOD_SLUGS, i);
    }

    /** Drawable for Catalog.BEASTS[i], or null if missing. */
    public static TextureRegionDrawable beast(int i) {
        return get("beasts", BEAST_SLUGS, i);
    }

    /** Drawable for Catalog.HERBS[i], or null if missing. */
    public static TextureRegionDrawable herb(int i) {
        return get("herbs", HERB_SLUGS, i);
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

    /** Smallest array length guard so slug tables can't drift from Catalog. */
    public static void checkAgainstCatalog() {
        if (GOOD_SLUGS.length != Catalog.GOODS.length
                || BEAST_SLUGS.length != Catalog.BEASTS.length
                || HERB_SLUGS.length != Catalog.HERBS.length) {
            Gdx.app.error("IconLib", "slug table length mismatch with Catalog");
        }
    }
}
