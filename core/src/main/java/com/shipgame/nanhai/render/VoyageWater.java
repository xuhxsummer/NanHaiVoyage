package com.shipgame.nanhai.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.shipgame.nanhai.data.Catalog;
import com.shipgame.nanhai.data.GameState;
import com.shipgame.nanhai.data.VoyageGeometry;

/** One continuous GLES2 water draw. No depth textures, framebuffer copies or runtime mesh uploads. */
public final class VoyageWater implements Disposable {
    private Mesh highMesh, lowMesh;
    private ShaderProgram highShader, lowShader;
    private Texture detail;
    private Pixmap detailPixels;
    private final float[] shores = new float[24];
    private final float[] nearest = new float[8];
    private boolean disposed;

    public VoyageWater() {
        try {
            String vertex = Gdx.files.internal("shaders/voyage-water.vert").readString();
            String fragment = Gdx.files.internal("shaders/voyage-water.frag").readString();
            lowShader = compile("#define LOW_QUALITY\n", vertex, fragment);
            highShader = compile("", vertex, fragment);
            if (lowShader == null && highShader == null) return;
            highMesh = grid(128);
            lowMesh = grid(64);
            detail = detailTexture();
        } catch (RuntimeException e) {
            Gdx.app.error("VoyageWater", "Using legacy water fallback", e);
            dispose();
        }
    }

    private ShaderProgram compile(String prefix, String vertex, String fragment) {
        ShaderProgram shader = new ShaderProgram(prefix + vertex, prefix + fragment);
        if (shader.isCompiled()) return shader;
        Gdx.app.error("VoyageWater", shader.getLog());
        shader.dispose();
        return null;
    }

    private static float coordinate(int i, int segments) {
        float t = (i * 2f / segments) - 1;
        return Math.signum(t) * (180f * Math.abs(t) + 7000f * (float)Math.pow(Math.abs(t), 4));
    }

    private static Mesh grid(int segments) {
        int edge = segments + 1;
        float[] vertices = new float[edge * edge * 3];
        short[] indices = new short[segments * segments * 6];
        int v = 0, k = 0;
        for (int z = 0; z <= segments; z++) for (int x = 0; x <= segments; x++) {
            vertices[v++] = coordinate(x, segments); vertices[v++] = 0; vertices[v++] = coordinate(z, segments);
        }
        for (int z = 0; z < segments; z++) for (int x = 0; x < segments; x++) {
            int a = z * edge + x, b = a + edge;
            indices[k++] = (short)a; indices[k++] = (short)b; indices[k++] = (short)(a + 1);
            indices[k++] = (short)(a + 1); indices[k++] = (short)b; indices[k++] = (short)(b + 1);
        }
        Mesh mesh = new Mesh(true, edge * edge, indices.length, VertexAttribute.Position());
        mesh.setVertices(vertices); mesh.setIndices(indices);
        return mesh;
    }

    private static float hash(int x, int y, int cells) {
        int n = (x & (cells - 1)) * 374761393 + (y & (cells - 1)) * 668265263 + 173;
        n = (n ^ (n >>> 13)) * 1274126177;
        return ((n ^ (n >>> 16)) & 0xffff) / 65535f;
    }
    private static float noise(float x, float y, int cells) {
        x *= cells; y *= cells;
        int ix = MathUtils.floor(x), iy = MathUtils.floor(y);
        float u = x - ix, v = y - iy;
        u = u * u * (3 - 2 * u); v = v * v * (3 - 2 * v);
        return MathUtils.lerp(MathUtils.lerp(hash(ix,iy,cells),hash(ix+1,iy,cells),u),
                MathUtils.lerp(hash(ix,iy+1,cells),hash(ix+1,iy+1,cells),u),v);
    }
    private static float height(float x, float y) {
        return noise(x,y,8)*.6f + noise(x,y,16)*.28f + noise(x,y,32)*.12f;
    }

    /** Seamless seeded noise derivatives with mipmaps: no regular sine-dot texture pattern. */
    private Texture detailTexture() {
        int size = 128;
        Pixmap pixels = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            float u = x / (float)size, v = y / (float)size, step=1f/size;
            float nx = MathUtils.clamp((height(u+step,v)-height(u-step,v))*5f,-1,1);
            float nz = MathUtils.clamp((height(u,v+step)-height(u,v-step))*5f,-1,1);
            pixels.setColor(nx * .5f + .5f, nz * .5f + .5f, height(u,v), 1);
            pixels.drawPixel(x, y);
        }
        // Retain 64 KiB CPU data so Android can restore this texture after context loss.
        detailPixels = pixels;
        Texture texture = new Texture(new PixmapTextureData(pixels, pixels.getFormat(), true, false, true));
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        return texture;
    }

    public boolean available() { return !disposed && detail != null && (highShader != null || lowShader != null); }
    public boolean supportsHighQuality() { return available() && highShader != null; }
    public boolean supportsLowQuality() { return available() && lowShader != null; }

    private void selectShores(float x, float z) {
        java.util.Arrays.fill(nearest, Float.MAX_VALUE);
        for (int i = 0; i < 8; i++) { shores[i * 3] = x + 30000; shores[i * 3 + 1] = z + 30000; shores[i * 3 + 2] = 0; }
        for (int i = 0; i < Catalog.PORTS.length + Catalog.ISLANDS.length; i++) {
            boolean port = i < Catalog.PORTS.length;
            int id = port ? i : i - Catalog.PORTS.length;
            float sx = port ? Catalog.PORT_X[id] : Catalog.ISLAND_X[id];
            float sz = -(port ? Catalog.PORT_Y[id] : Catalog.ISLAND_Y[id]);
            float d = (sx - x) * (sx - x) + (sz - z) * (sz - z);
            for (int j = 0; j < 8; j++) if (d < nearest[j]) {
                for (int k = 7; k > j; k--) {
                    nearest[k] = nearest[k - 1];
                    System.arraycopy(shores, (k - 1) * 3, shores, k * 3, 3);
                }
                nearest[j] = d;
                shores[j * 3] = sx; shores[j * 3 + 1] = sz;
                shores[j * 3 + 2] = VoyageGeometry.landRadius(port, id) * .89f;
                break;
            }
        }
    }

    public boolean render(PerspectiveCamera camera, GameState state, float time, boolean high, Color sky) {
        if (!available()) return false;
        ShaderProgram shader = high && highShader != null ? highShader : lowShader;
        if (shader == null) shader = highShader;
        float ox = MathUtils.floor(state.x / 8) * 8, oz = MathUtils.floor(-state.y / 8) * 8;
        selectShores(state.x, -state.y);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        shader.bind();
        detail.bind(0);
        shader.setUniformi("u_detail", 0);
        shader.setUniformMatrix("u_projView", camera.combined);
        shader.setUniformf("u_origin", ox, oz);
        shader.setUniformf("u_camera", camera.position.x - ox, camera.position.y, camera.position.z - oz);
        shader.setUniformf("u_time", time);
        shader.setUniformf("u_wind", MathUtils.clamp(state.windStr, 0, 1));
        shader.setUniformf("u_sky", sky.r, sky.g, sky.b);
        shader.setUniform3fv("u_shores[0]", shores, 0, shores.length);
        shader.setUniformf("u_ship", state.x - ox, -state.y - oz);
        shader.setUniformf("u_forward", MathUtils.cosDeg(state.headingDeg), -MathUtils.sinDeg(state.headingDeg));
        VoyageGeometry.Ship hull = VoyageGeometry.ship(state.ship);
        shader.setUniformf("u_hull", 25f * hull.length, 10f * hull.beam);
        shader.setUniformf("u_speed", Math.max(0, state.speed));
        (high && highShader != null ? highMesh : lowMesh).render(shader, GL20.GL_TRIANGLES);
        return true;
    }

    @Override public void dispose() {
        if (disposed) return;
        disposed = true;
        if (highMesh != null) highMesh.dispose();
        if (lowMesh != null) lowMesh.dispose();
        if (detail != null) detail.dispose();
        if (detailPixels != null) detailPixels.dispose();
        if (highShader != null) highShader.dispose();
        if (lowShader != null) lowShader.dispose();
    }
}
