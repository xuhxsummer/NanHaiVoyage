package com.shipgame.nanhai;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.shipgame.nanhai.data.Catalog;
import com.shipgame.nanhai.data.GameState;

/**
 * Top-down pixel-art world sprites. Water is tiled; ships/ports/islands are
 * icon-sized. Water and markers are drawn in separate passes so a vector
 * silhouette can be layered under the ship sprite (see VoyageScreen).
 */
public class PixelMapRenderer {

    private static final float WATER_TILE = 192f;
    private static final float SHIP_W = 28f;
    private static final float SHIP_H = 48f;
    private static final float PIRATE_W = 24f;
    private static final float PIRATE_H = 40f;
    private static final float PORT_SIZE = 48f;
    private static final float ISLAND_SIZE = 72f;

    private final Texture water;
    private final Texture ship;
    private final Texture pirate;
    private final Texture port;
    private final Texture island;

    /** False when ship.png could not load: VoyageScreen then draws the ship as an
     * enlarged vector hull outline so it is never invisible on screen. */
    public boolean shipSpriteOk = true;

    public PixelMapRenderer() {
        water = loadSafe("textures/water.png", true, 0.10f, 0.36f, 0.52f, 1f);
        ship = loadShip();
        pirate = loadSafe("textures/pirate.png", false, 0.70f, 0.20f, 0.15f, 1f);
        port = loadSafe("textures/port.png", false, 0.92f, 0.78f, 0.28f, 1f);
        island = loadSafe("textures/island.png", false, 0.28f, 0.62f, 0.34f, 1f);
    }

    private Texture loadShip() {
        try {
            Texture t = load("textures/ship.png", false);
            shipSpriteOk = true;
            return t;
        } catch (Exception ex) {
            Gdx.app.error("PixelMapRenderer", "ship texture failed, using vector hull", ex);
            shipSpriteOk = false;
            Pixmap pm = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
            pm.setColor(0.70f, 0.40f, 0.20f, 1f);
            pm.fill();
            Texture t = new Texture(pm);
            pm.dispose();
            return t;
        }
    }

    /** Loads a texture; on any failure falls back to a flat 4x4 color so the
     * game can still run instead of crashing during setScreen. */
    private static Texture loadSafe(String path, boolean repeat,
                                    float r, float g, float b, float a) {
        try {
            return load(path, repeat);
        } catch (Exception ex) {
            Gdx.app.error("PixelMapRenderer", "texture failed: " + path, ex);
            Pixmap pm = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
            pm.setColor(r, g, b, a);
            pm.fill();
            Texture t = new Texture(pm);
            pm.dispose();
            return t;
        }
    }

    private static Texture load(String path, boolean repeat) {
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        if (repeat) {
            t.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
        }
        return t;
    }

    /** Water tile pass (drawn first, under everything). */
    public void drawWater(SpriteBatch batch, GameState g) {
        Color old = batch.getColor();
        if (g.weather == GameState.WeatherKind.RAIN) {
            batch.setColor(0.62f, 0.72f, 0.82f, 1f);
        } else if (g.weather == GameState.WeatherKind.FOG) {
            batch.setColor(0.78f, 0.84f, 0.88f, 1f);
        } else {
            batch.setColor(Color.WHITE);
        }
        float left = g.x - 720f;
        float bottom = g.y - 420f;
        float width = 1440f;
        float height = 840f;
        float u0 = left / WATER_TILE;
        float v0 = bottom / WATER_TILE;
        float u1 = (left + width) / WATER_TILE;
        float v1 = (bottom + height) / WATER_TILE;
        batch.draw(water, left, bottom, width, height, u0, v0, u1, v1);
        batch.setColor(old);
    }

    /** Island / port / pirate pass over the water. The player ship is NOT drawn
     * here: VoyageScreen layers its vector hull outline first (so it can never be
     * hidden under an island/port tile), then the sprite on top. */
    public void drawMarkers(SpriteBatch batch, GameState g) {
        Color old = batch.getColor();
        batch.setColor(Color.WHITE);
        for (int i = 0; i < Catalog.ISLANDS.length; i++) {
            drawMarker(batch, island, Catalog.ISLAND_X[i], Catalog.ISLAND_Y[i], ISLAND_SIZE, ISLAND_SIZE, 0f);
        }
        for (int i = 0; i < Catalog.PORTS.length; i++) {
            drawMarker(batch, port, Catalog.PORT_X[i], Catalog.PORT_Y[i], PORT_SIZE, PORT_SIZE, 0f);
        }
        if (g.pirateAlive) {
            drawMarker(batch, pirate, g.pirateX, g.pirateY, PIRATE_W, PIRATE_H, g.pirateHeading - 90f);
        }
        batch.setColor(old);
    }

    /** Player ship sprite pass (drawn last, above islands/ports). Skipped when the
     * texture failed to load — the vector hull outline then stands alone. */
    public void drawShip(SpriteBatch batch, GameState g) {
        if (!shipSpriteOk) {
            return;
        }
        Color old = batch.getColor();
        batch.setColor(Color.WHITE);
        drawMarker(batch, ship, g.x, g.y, SHIP_W, SHIP_H, g.headingDeg - 90f);
        batch.setColor(old);
    }

    /** Full pass (water + markers) for callers that do not need the split. */
    public void draw(SpriteBatch batch, GameState g) {
        drawWater(batch, g);
        drawMarkers(batch, g);
        drawShip(batch, g);
    }

    private static void drawMarker(SpriteBatch batch, Texture tex, float x, float y, float w, float h, float rot) {
        batch.draw(tex, x - w * 0.5f, y - h * 0.5f, w * 0.5f, h * 0.5f, w, h, 1f, 1f, rot,
                0, 0, tex.getWidth(), tex.getHeight(), false, false);
    }

    public void dispose() {
        if (water != null) water.dispose();
        if (ship != null) ship.dispose();
        if (pirate != null) pirate.dispose();
        if (port != null) port.dispose();
        if (island != null) island.dispose();
    }
}
