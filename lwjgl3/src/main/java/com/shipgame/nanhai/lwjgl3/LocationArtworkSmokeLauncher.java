package com.shipgame.nanhai.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FileTextureData;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.shipgame.nanhai.NanHaiVoyage;
import com.shipgame.nanhai.data.Catalog;
import com.shipgame.nanhai.data.GameState;
import com.shipgame.nanhai.screen.VoyageScreen;
import com.shipgame.nanhai.ui.IconLib;
import com.shipgame.nanhai.ui.WorldMapOverlay;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Actual asset decode/reload, UI rendering and map input; no account or save writes. */
public final class LocationArtworkSmokeLauncher {
    private static int result = 1;

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(1280, 720);
        config.disableAudio(true);
        new Lwjgl3Application(new NanHaiVoyage() {
            VoyageScreen voyage;
            Stage stage;
            int frame;

            @Override public void create() {
                super.create();
                try {
                    state = GameState.newGame(); currentUser = null;
                    state.undockInPlace(); state.pirateSpawnTimer = 10000;
                    checkArt();
                    voyage = new VoyageScreen(this); setScreen(voyage);
                    stage = (Stage)field("stage").get(voyage);
                    overlay("MAP");
                } catch (Throwable t) { fail(t); }
            }

            @Override public void render() {
                try {
                    if (frame == 0) {
                        voyage.render(0);
                        snap("map");
                        WorldMapOverlay map = (WorldMapOverlay)field("worldMap").get(voyage);
                        for (int i = 0; i < Catalog.PORTS.length + Catalog.ISLANDS.length; i++) {
                            boolean port = i < Catalog.PORTS.length;
                            int index = port ? i : i - Catalog.PORTS.length;
                            float wx = port ? Catalog.PORT_X[index] : Catalog.ISLAND_X[index];
                            float wy = port ? Catalog.PORT_Y[index] : Catalog.ISLAND_Y[index];
                            float x = 144 + wx / Catalog.WORLD_W * 1568;
                            float y = 120 + wy / Catalog.WORLD_H * 824;
                            for (float width : new float[]{1280, 1600}) {
                                require(map.hit(x * width / 1920, y * 720 / 1080, width, 720) == i, "map center " + i);
                                require(map.hit((x + 25) * width / 1920, (y + 25) * 720 / 1080, width, 720) == i, "map corner " + i);
                            }
                            overlay("MAP");
                            Gdx.input.getInputProcessor().touchDown(Math.round(x * 1280 / 1920),
                                    720 - Math.round(y * 720 / 1080), 0, 0);
                            require(port ? state.autoSailPort == index : state.autoSailIsle == index, "map tap destination " + i);
                        }
                    } else if (frame <= Catalog.PORTS.length) {
                        int index = frame - 1;
                        state.dock(index); overlay("PORT"); voyage.render(0);
                        requireArt("portArtwork", IconLib.port(index));
                        if (index == 0 || index == Catalog.YANGZHOU) snap("port-" + index);
                    } else if (frame <= Catalog.PORTS.length + Catalog.ISLANDS.length) {
                        int index = frame - Catalog.PORTS.length - 1;
                        state.undockInPlace(); state.enterIsland(index);
                        overlay("ISLAND"); voyage.render(0);
                        requireArt("islandArtwork", IconLib.island(index));
                        if (index == 0 || index == 6) snap("island-" + index);
                    } else {
                        state.leaveIslandInPlace();
                        Arrays.fill(state.beastFound, true); Arrays.fill(state.herbFound, true);
                        overlay("CODEX"); voyage.render(0); snap("beasts");
                        Object panel = field("codexPanel").get(voyage);
                        Method category = panel.getClass().getDeclaredMethod("switchCategory", boolean.class);
                        category.setAccessible(true); category.invoke(panel, true);
                        voyage.render(0); snap("herbs");
                        result = 0;
                        System.out.println("LOCATION ART PASS: 50 named images, bounded managed reloads, 34 map targets/corners/taps, all dock/island panels, codex, GL");
                        Gdx.app.exit();
                    }
                    require(Gdx.gl.glGetError() == GL20.GL_NO_ERROR, "GL frame " + frame);
                    frame++;
                } catch (Throwable t) { fail(t); }
            }

            private void checkArt() {
                Set<Texture> textures = new HashSet<>();
                for (int i = 0; i < Catalog.PORTS.length; i++) check(IconLib.port(i), "ports/port_" + Catalog.PORTS[i], textures);
                for (int i = 0; i < Catalog.ISLANDS.length; i++) check(IconLib.island(i), "islands/island_" + Catalog.ISLANDS[i], textures);
                for (int i = 0; i < Catalog.BEASTS.length; i++) check(IconLib.beast(i), "beasts/beast_" + Catalog.BEASTS[i], textures);
                for (int i = 0; i < Catalog.HERBS.length; i++) check(IconLib.herb(i), "herbs/herb_" + Catalog.HERBS[i], textures);
                require(IconLib.port(-1) == null && IconLib.island(Catalog.ISLANDS.length) == null, "invalid index fallback");
                require(textures.size() == 50, "distinct Catalog artwork");
                require(IconLib.port(0) == IconLib.port(0), "shared cache");
            }

            private void check(TextureRegionDrawable art, String name, Set<Texture> textures) {
                require(art != null, "missing " + name);
                Texture texture = art.getRegion().getTexture();
                FileTextureData data = (FileTextureData)texture.getTextureData();
                require(data.getFileHandle().path().startsWith("textures/" + name + "."), "wrong file " + name);
                require(texture.isManaged(), "unmanaged " + name);
                texture.load(data); // Re-decode exactly as Android managed context restoration does.
                require(texture.getWidth() == 256 && texture.getHeight() == 256, "texture budget " + name);
                require(!data.isPrepared(), "retained source pixmap " + name);
                if(name.startsWith("ports/") || name.startsWith("islands/")) {
                    data.prepare(); Pixmap pixels=data.consumePixmap();
                    int clear=0,solid=0;
                    for(int y=0;y<256;y++) for(int x=0;x<256;x++) {
                        int alpha=pixels.getPixel(x,y)&255;
                        if(alpha==0)clear++; if(alpha>200)solid++;
                    }
                    require(clear>12000 && solid>1800,"transparent matte with retained silhouette "+name+" clear="+clear+" solid="+solid);
                    require((pixels.getPixel(3,3)&255)==0 && (pixels.getPixel(252,252)&255)==0,"no opaque corners "+name);
                    if(name.equals("ports/port_扬州") || name.equals("islands/island_东沙"))
                        PixmapIO.writePNG(Gdx.files.local("../Builds/matte2810-"+name.substring(name.indexOf('/')+1)+".png"),pixels);
                    pixels.dispose();
                }
                require(textures.add(texture), "duplicate art " + name);
            }

            private void requireArt(String actorName, TextureRegionDrawable expected) {
                Image actor = stage.getRoot().findActor(actorName);
                require(actor != null && actor.getDrawable() == expected, "panel artwork " + actorName);
            }

            private Field field(String name) throws Exception {
                Field f = VoyageScreen.class.getDeclaredField(name); f.setAccessible(true); return f;
            }

            private void overlay(String name) throws Exception {
                Field f = field("overlay");
                for (Object value : f.getType().getEnumConstants()) if (name.equals(value.toString())) f.set(voyage, value);
                Method rebuild = VoyageScreen.class.getDeclaredMethod("rebuildMenu");
                rebuild.setAccessible(true); rebuild.invoke(voyage);
            }

            private void snap(String name) {
                Pixmap p = Pixmap.createFromFrameBuffer(0, 0, 1280, 720);
                PixmapIO.writePNG(Gdx.files.local("../Builds/location-art288-" + name + ".png"), p, -1, true);
                p.dispose();
            }

            private void fail(Throwable t) { result = 1; t.printStackTrace(); Gdx.app.exit(); }
        }, config);
        System.exit(result);
    }

    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
