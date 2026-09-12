package com.shipgame.nanhai.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.shipgame.nanhai.data.Catalog;
import com.shipgame.nanhai.data.GameState;
import com.shipgame.nanhai.data.VoyageGeometry;

/** One continuous GLES2 water draw. No depth textures, framebuffer copies or runtime mesh uploads. */
public final class VoyageWater implements Disposable {
    private Mesh highMesh, lowMesh, skyMesh;
    private ShaderProgram highShader, lowShader, skyShader;
    private Texture chop, swell, foam, skyMap;
    private final Vector3 right = new Vector3(), up = new Vector3();
    private static final Vector3 SUN = new Vector3(-.42f,.58f,-.69f).nor();
    // Shared by GPU deformation and the small visual buoyancy adjustment.
    private static final float[] WAVES = {
        .94f,.341f,.045f,1.50f, .80f,.60f,.080f,.82f,
        -.38f,.925f,.14f,.46f, .65f,-.76f,.25f,.28f,
        .22f,.976f,.42f,.14f, -.85f,.527f,.68f,.09f,
        .98f,-.199f,1.15f,.06f, -.60f,-.80f,1.90f,.035f
    };
    private final float[] shores = new float[24];
    private final float[] nearest = new float[8];
    private boolean disposed;

    public VoyageWater() {
        try {
            String vertex = Gdx.files.internal("shaders/voyage-water.vert").readString();
            String common = Gdx.files.internal("shaders/voyage-sky-common.glsl").readString();
            String fragment = Gdx.files.internal("shaders/voyage-water.frag").readString().replace("// SKY_FUNCTIONS", common);
            lowShader = compile("#define LOW_QUALITY\n", vertex, fragment);
            highShader = compile("", vertex, fragment);
            if (lowShader == null && highShader == null) return;
            highMesh = grid(224);
            lowMesh = grid(96);
            chop = texture("ocean_normal_choppy_01.png");
            swell = texture("ocean_normal_swell_01.png");
            foam = texture("ocean_foam_masks.png");
            skyMap = texture("ocean_sky_environment.png");
            skyMap.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);
            skyShader = compile("", Gdx.files.internal("shaders/voyage-sky.vert").readString(),
                    Gdx.files.internal("shaders/voyage-sky.frag").readString().replace("// SKY_FUNCTIONS",common));
            skyMesh = new Mesh(true,4,6,VertexAttribute.Position());
            skyMesh.setVertices(new float[]{-1,-1,0, 1,-1,0, 1,1,0, -1,1,0});
            skyMesh.setIndices(new short[]{0,1,2, 2,3,0});
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

    private static Texture texture(String name) {
        // File-backed managed textures automatically reload after Android GL context loss.
        Texture texture = new Texture(Gdx.files.internal("textures/water/"+name),true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        return texture;
    }

    public boolean available() { return !disposed && skyMap != null && (highShader != null || lowShader != null); }
    public boolean supportsHighQuality() { return available() && highShader != null; }
    public boolean supportsLowQuality() { return available() && lowShader != null; }

    /** 0.28.26 昼夜压暗系数：0=白天 1=深夜，作用于天空与海面颜色。 */
    private float nightDim;
    public void setNightDim(float amount) {
        nightDim = MathUtils.clamp(amount, 0f, 1f);
    }

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
        float ox = state.x, oz = -state.y;
        selectShores(state.x, -state.y);
        renderSky(camera);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        shader.bind();
        chop.bind(0); swell.bind(1); foam.bind(2); skyMap.bind(3);
        shader.setUniformi("u_chop", 0); shader.setUniformi("u_swell", 1);
        shader.setUniformi("u_foam", 2); shader.setUniformi("u_skyMap", 3);
        shader.setUniformf("u_sun",SUN);
        shader.setUniformMatrix("u_projView", camera.combined);
        shader.setUniformf("u_origin", ox, oz);
        shader.setUniformf("u_camera", camera.position.x - ox, camera.position.y, camera.position.z - oz);
        shader.setUniformf("u_time", time);
        shader.setUniformf("u_wind", MathUtils.clamp(state.windStr, 0, 1));
        shader.setUniform3fv("u_shores[0]", shores, 0, shores.length);
        shader.setUniform4fv("u_waves[0]",WAVES,0,shader==lowShader?16:32);
        shader.setUniformf("u_ship", state.x - ox, -state.y - oz);
        shader.setUniformf("u_night", nightDim); // 0.28.26 夜晚海面压暗
        shader.setUniformf("u_forward", MathUtils.cosDeg(state.headingDeg), -MathUtils.sinDeg(state.headingDeg));
        VoyageGeometry.Ship hull = VoyageGeometry.ship(state.ship);
        shader.setUniformf("u_hull", 25f * hull.length, 10f * hull.beam);
        shader.setUniformf("u_speed", Math.max(0, state.speed));
        (high && highShader != null ? highMesh : lowMesh).render(shader, GL20.GL_TRIANGLES);
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        return true;
    }

    private void renderSky(PerspectiveCamera camera) {
        if(skyShader==null) return;
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST); Gdx.gl.glDepthMask(false);
        Gdx.gl.glDisable(GL20.GL_BLEND); Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        skyShader.bind(); skyMap.bind(0);
        skyShader.setUniformi("u_skyMap",0); skyShader.setUniformf("u_sun",SUN);
        skyShader.setUniformf("u_night", nightDim); // 0.28.26 夜晚天空压暗
        float tangent=(float)Math.tan(Math.toRadians(camera.fieldOfView*.5));
        right.set(camera.direction).crs(camera.up).nor();
        up.set(right).crs(camera.direction).nor().scl(tangent);
        right.scl(tangent*camera.viewportWidth/camera.viewportHeight);
        skyShader.setUniformf("u_forward",camera.direction);
        skyShader.setUniformf("u_right",right); skyShader.setUniformf("u_up",up);
        skyMesh.render(skyShader,GL20.GL_TRIANGLES);
    }

    /** Rendering-only height; game coordinates, collision and movement never use it. */
    public float surfaceHeight(float x,float z,float time,float wind,boolean high) {
        if(!available()) return 0;
        float height=0, shore=1000;
        for(int i=0;i<Catalog.PORTS.length+Catalog.ISLANDS.length;i++) {
            boolean port=i<Catalog.PORTS.length; int id=port?i:i-Catalog.PORTS.length;
            float dx=x-(port?Catalog.PORT_X[id]:Catalog.ISLAND_X[id]);
            float dz=z+(port?Catalog.PORT_Y[id]:Catalog.ISLAND_Y[id]);
            shore=Math.min(shore,(float)Math.sqrt(dx*dx+dz*dz)-VoyageGeometry.landRadius(port,id)*.89f);
        }
        float t=MathUtils.clamp(shore/38,0,1); t=t*t*(3-2*t);
        for(int i=0;i<(high&&highShader!=null?32:16);i+=4) {
            float k=WAVES[i+2];
            height+=MathUtils.sin((x*WAVES[i]+z*WAVES[i+1])*k-time*(float)Math.sqrt(9.81*k))*WAVES[i+3];
        }
        return height*(.65f+MathUtils.clamp(wind,0,1)*.6f)*t-.45f;
    }

    @Override public void dispose() {
        if (disposed) return;
        disposed = true;
        if (highMesh != null) highMesh.dispose();
        if (lowMesh != null) lowMesh.dispose();
        if (chop != null) chop.dispose(); if(swell!=null) swell.dispose();
        if (foam != null) foam.dispose(); if(skyMap!=null) skyMap.dispose();
        if (skyMesh != null) skyMesh.dispose(); if(skyShader!=null) skyShader.dispose();
        if (highShader != null) highShader.dispose();
        if (lowShader != null) lowShader.dispose();
    }
}
