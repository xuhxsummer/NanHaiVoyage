package com.shipgame.nanhai.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.shipgame.nanhai.NanHaiVoyage;
import com.shipgame.nanhai.data.Catalog;
import com.shipgame.nanhai.data.GameState;

/** Full-map-only presentation. All geometry and input use the same 1920 × 1080 chart. */
public final class WorldMapOverlay implements Disposable {
    public static final int CLOSE = -2, EMPTY = -1;
    private static final float WIDTH = 1920, HEIGHT = 1080;
    private static final float ICON_SIZE = 56;
    private static final Rectangle FRAME = new Rectangle(24, 24, 1872, 1032);
    private static final Rectangle CLOSE_BOX = new Rectangle(1776, 928, 104, 112);
    private static final Rectangle TITLE = new Rectangle(48, 856, 592, 176);
    private static final Rectangle LEGEND = new Rectangle(96, 64, 312, 144);
    private static final Color GOLD = Color.valueOf("D7B777");
    private static final Color PAPER = Color.valueOf("F4DFAD");
    private static final Color JADE = Color.valueOf("97AC77");
    private static final Color NAVY = Color.valueOf("102733E8");
    private static final Color RED = Color.valueOf("D45E46");
    private final NanHaiVoyage game;
    private final Texture background;
    private final GlyphLayout glyphs = new GlyphLayout();
    private final Matrix4 projection = new Matrix4().setToOrtho2D(0, 0, WIDTH, HEIGHT);
    private final Matrix4 oldBatch = new Matrix4(), oldShapes = new Matrix4();
    private final Node[] nodes = new Node[Catalog.PORTS.length + Catalog.ISLANDS.length];

    private static final class Node {
        String name;
        float x, y;
        boolean port;
        Rectangle label, icon;
        TextureRegionDrawable art;
    }

    public WorldMapOverlay(NanHaiVoyage game) {
        this.game = game;
        background = new Texture(Gdx.files.internal("textures/world-map/sea-chart-hd.png"));
        background.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        for (int i = 0; i < nodes.length; i++) {
            Node n = nodes[i] = new Node();
            n.port = i < Catalog.PORTS.length;
            int index = n.port ? i : i - Catalog.PORTS.length;
            n.name = n.port ? Catalog.PORTS[index] : Catalog.ISLANDS[index];
            n.x = projectX(n.port ? Catalog.PORT_X[index] : Catalog.ISLAND_X[index]);
            n.y = projectY(n.port ? Catalog.PORT_Y[index] : Catalog.ISLAND_Y[index]);
            n.art = n.port ? IconLib.port(index) : IconLib.island(index);
            n.icon = new Rectangle(n.x - ICON_SIZE / 2, n.y - ICON_SIZE / 2, ICON_SIZE, ICON_SIZE);
        }
        layoutLabels();
    }

    private static float projectX(float x) { return 144 + x / Catalog.WORLD_W * 1568; }
    private static float projectY(float y) { return 120 + y / Catalog.WORLD_H * 824; }

    /** Search eight-pixel offsets, reserving every node and previously placed label. */
    private void layoutLabels() {
        BitmapFont font = game.fontSmall;
        float sx = font.getData().scaleX, sy = font.getData().scaleY;
        font.getData().setScale(1.5f);
        try {
            for (Node n : nodes) {
                glyphs.setText(font, n.name);
                float w = (float) Math.ceil((glyphs.width + 16) / 8) * 8;
                Rectangle best = null;
                float bestScore = Float.MAX_VALUE;
                for (int ring = 0; ring < 6; ring++) {
                    float gap = 36 + ring * 16;
                    float[][] offsets = {{gap,-16},{-w-gap,-16},{-w/2,gap},
                            {-w/2,-gap-32},{gap,gap},{-w-gap,gap},{gap,-gap-32},{-w-gap,-gap-32}};
                    for (float[] offset : offsets) {
                        Rectangle r = new Rectangle(Math.round((n.x + offset[0])/8)*8,
                                Math.round((n.y + offset[1])/8)*8, w, 32);
                        float score = ring * 8;
                        if (r.x < 64 || r.x+r.width > 1752 || r.y < 80 || r.y+32 > 1008) score += 10000;
                        if (r.overlaps(TITLE) || r.overlaps(LEGEND) || r.overlaps(CLOSE_BOX)) score += 10000;
                        for (Node other : nodes) {
                            if (other.label != null && r.overlaps(other.label)) score += 1000;
                            if (r.overlaps(other.icon)) score += 1000;
                        }
                        if (score < bestScore) { best = r; bestScore = score; }
                    }
                }
                n.label = best;
            }
        } finally { font.getData().setScale(sx, sy); }
    }

    /** Input is converted from the existing HUD viewport; minimap geometry is untouched. */
    public int hit(float hudX, float hudY, float hudWidth, float hudHeight) {
        float x = hudX * WIDTH / hudWidth, y = hudY * HEIGHT / hudHeight;
        if (!FRAME.contains(x,y) || CLOSE_BOX.contains(x,y)) return CLOSE;
        if (TITLE.contains(x,y) || LEGEND.contains(x,y)) return EMPTY;
        int nearest = EMPTY;
        float distance = Float.MAX_VALUE;
        for (int i = 0; i < nodes.length; i++) {
            float dx = x-nodes[i].x, dy = y-nodes[i].y, d = dx*dx+dy*dy;
            if ((d < 32 * 32 || (nodes[i].art != null && nodes[i].icon.contains(x,y)))
                    && d < distance) { nearest = i; distance = d; }
        }
        if (nearest != EMPTY) return nearest;
        for (int i = 0; i < nodes.length; i++) if (nodes[i].label.contains(x,y)) return i;
        return EMPTY;
    }

    public void draw(ShapeRenderer s, GameState state, float time) {
        SpriteBatch batch = game.batch;
        oldBatch.set(batch.getProjectionMatrix()); oldShapes.set(s.getProjectionMatrix());
        float batchColor = batch.getPackedColor();
        batch.setProjectionMatrix(projection); s.setProjectionMatrix(projection);
        try {
            batch.setColor(Color.WHITE);
            batch.begin(); batch.draw(background, 0, 0, WIDTH, HEIGHT); batch.end();
            // 0.27.4: 航线 polylines removed — the chart keeps ports, islands,
            // markers, labels and the live auto-sail target line only.
            s.begin(ShapeRenderer.ShapeType.Filled);
            float mx = projectX(state.x), my = projectY(state.y);
            int target = state.autoSailPort >= 0 ? state.autoSailPort
                    : state.autoSailIsle >= 0 ? Catalog.PORTS.length + state.autoSailIsle : EMPTY;
            if (state.autoSail && target >= 0 && target < nodes.length) {
                dashed(s,mx,my,nodes[target].x,nodes[target].y,JADE,3);
            }
            for (Node n : nodes) {
                // Leader ends at the nearest label edge, including displaced coastal names.
                s.setColor(.75f,.65f,.43f,.45f);
                s.rectLine(n.x,n.y,MathUtils.clamp(n.x,n.label.x,n.label.x+n.label.width),
                        MathUtils.clamp(n.y,n.label.y,n.label.y+n.label.height),1);
                if (n.art == null) marker(s,n.x,n.y,n.port);

            }
            s.end();
            batch.setColor(Color.WHITE);
            batch.begin();
            for (Node n : nodes) if (n.art != null)
                n.art.draw(batch,n.icon.x+2,n.icon.y+2,ICON_SIZE-4,ICON_SIZE-4);
            batch.end();
            // Chrome, the live ship marker and names stay above the illustrations.
            s.begin(ShapeRenderer.ShapeType.Filled);
            panel(s,TITLE); panel(s,LEGEND); panel(s,CLOSE_BOX);
            marker(s,136,160,true); marker(s,136,104,false);
            // Large visual cross; the whole 104 × 112 area is a close hit target.
            s.setColor(GOLD);
            s.rectLine(1808,976,1848,1016,4); s.rectLine(1808,1016,1848,976,4);

            s.end();
            s.begin(ShapeRenderer.ShapeType.Line);
            s.setColor(GOLD); s.rect(32,32,1856,1016);
            s.circle(120,976,48,64); s.circle(120,976,40,64);
            for (int i=0;i<16;i++) {
                float angle=i*MathUtils.PI2/16;
                s.line(120+MathUtils.cos(angle)*40,976+MathUtils.sin(angle)*40,
                        120+MathUtils.cos(angle)*48,976+MathUtils.sin(angle)*48);
            }
            s.setColor(.48f,.36f,.19f,1); s.rect(40,40,1840,1000);
            if (state.pirateAlive) {
                float px=projectX(state.pirateX), py=projectY(state.pirateY);
                s.setColor(RED); s.circle(px,py,14,24);
                s.setColor(.95f,.30f,.22f,.55f); s.circle(px,py,22,24);
            }
            float pulse = (time % 2.4f)/2.4f;
            s.setColor(.85f,.30f,.20f,(1-pulse)*.55f); s.circle(mx,my,64+pulse*24,64);
            s.end();
            s.begin(ShapeRenderer.ShapeType.Filled);
            s.setColor(PAPER); s.triangle(120,1020,112,976,120,962);
            s.triangle(76,976,120,984,134,976);
            s.setColor(GOLD); s.triangle(120,932,128,976,120,990);
            s.triangle(164,976,120,968,106,976);
            ship(s,mx,my); s.end();
            batch.begin();
            text(game.font,"南海海图",192,988,2,PAPER);
            text(game.fontSmall,"点港口/岛屿自动驶向",112,892,1.5f,GOLD);
            text(game.fontSmall,"关闭",1800,960,1.5f,PAPER);
            text(game.fontSmall,"港口城市",176,172,1.5f,PAPER);
            text(game.fontSmall,"岛屿/礁盘",176,116,1.5f,PAPER);
            for (Node n : nodes) text(game.fontSmall,n.name,n.label.x+8,n.label.y+28,1.5f,n.port?PAPER:JADE);
            float bx = MathUtils.clamp(mx-32,72,1760);
            float by = my-72;
            if (by < 88) by = my+88;
            text(game.fontSmall,"本船",bx,by,1.5f,PAPER);
            batch.end();
        } finally {
            if (batch.isDrawing()) batch.end();
            batch.setPackedColor(batchColor);
            batch.setProjectionMatrix(oldBatch); s.setProjectionMatrix(oldShapes);
        }
    }

    private void text(BitmapFont font,String value,float x,float y,float scale,Color color) {
        float sx=font.getData().scaleX,sy=font.getData().scaleY;
        float packed=font.getColor().toFloatBits();
        font.getData().setScale(scale);
        font.setColor(.025f,.05f,.06f,.95f); font.draw(game.batch,value,x+2,y-2);
        font.setColor(color); font.draw(game.batch,value,x,y);
        font.getData().setScale(sx,sy); Color.abgr8888ToColor(font.getColor(),packed);
    }

    private static void panel(ShapeRenderer s,Rectangle r) {
        s.setColor(.04f,.07f,.09f,.65f); s.rect(r.x+4,r.y-4,r.width,r.height);
        s.setColor(GOLD); s.rect(r.x,r.y,r.width,r.height);
        s.setColor(NAVY); s.rect(r.x+2,r.y+2,r.width-4,r.height-4);
        s.setColor(.64f,.48f,.25f,1); s.rect(r.x+8,r.y+8,24,2); s.rect(r.x+8,r.y+8,2,24);
        s.rect(r.x+r.width-32,r.y+r.height-10,24,2); s.rect(r.x+r.width-10,r.y+r.height-32,2,24);
    }

    private static void marker(ShapeRenderer s,float x,float y,boolean port) {
        s.setColor(.02f,.06f,.08f,.8f); s.circle(x+2,y-2,16,24);
        s.setColor(port?GOLD:JADE); s.circle(x,y,14,24);
        s.setColor(port?Color.valueOf("293332"):Color.valueOf("405343")); s.circle(x,y,11,24);
        s.setColor(port?GOLD:JADE);
        if (port) {
            // Anchor silhouette: circular eye, shank, stock and swept flukes.
            s.circle(x,y+6,3,12); s.rect(x-1,y-7,2,11); s.rect(x-6,y+1,12,2);
            s.rectLine(x,y-8,x-7,y-3,2); s.rectLine(x,y-8,x+7,y-3,2);
            s.triangle(x-8,y-1,x-8,y-6,x-4,y-3); s.triangle(x+8,y-1,x+8,y-6,x+4,y-3);
        } else { s.setColor(.72f,.78f,.47f,.7f); s.arc(x,y,8,40,120,12); }
    }

    private static void dashed(ShapeRenderer s,float x,float y,float tx,float ty,Color color,float width) {
        float dx=tx-x,dy=ty-y,len=(float)Math.sqrt(dx*dx+dy*dy);
        if (len < 1) return;
        s.setColor(color);
        for (float d=20;d<len-16;d+=24) {
            float end=Math.min(d+10,len-16);
            s.rectLine(x+dx*d/len,y+dy*d/len,x+dx*end/len,y+dy*end/len,width);
        }
    }

    /** Upright Chinese junk symbol centered on the real world position, with battened sails. */
    private static void ship(ShapeRenderer s,float x,float y) {
        s.setColor(Color.valueOf("4B2922"));
        s.triangle(x-32,y-16,x+32,y-16,x+20,y-30); s.triangle(x-32,y-16,x+20,y-30,x-20,y-30);
        s.setColor(GOLD); s.rect(x-28,y-16,56,3);
        s.setColor(Color.valueOf("CB9860")); s.rect(x-7,y-16,3,58); s.rect(x+14,y-16,3,44);
        s.setColor(Color.valueOf("E7BF83"));
        s.triangle(x-9,y+38,x-9,y-7,x-31,y-3); s.triangle(x-9,y+38,x-31,y-3,x-27,y+30);
        s.setColor(Color.valueOf("B77F50"));
        s.triangle(x-2,y+32,x-2,y-6,x+14,y); s.triangle(x+19,y+24,x+19,y-8,x+33,y-3);
        s.setColor(Color.valueOf("735039"));
        for(int i=0;i<5;i++) s.rectLine(x-27,y+i*7,x-10,y+i*8,1.5f);
        s.setColor(RED); s.triangle(x-4,y+43,x-4,y+51,x+12,y+47);
    }

    @Override public void dispose() { background.dispose(); }
}
