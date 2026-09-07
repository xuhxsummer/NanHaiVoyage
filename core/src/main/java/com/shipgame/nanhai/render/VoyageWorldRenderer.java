package com.shipgame.nanhai.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.shipgame.nanhai.data.Catalog;
import com.shipgame.nanhai.data.GameState;

/** Presentation only: map (x,y) becomes (x,height,-y). No gameplay coordinates are changed. */
public final class VoyageWorldRenderer implements Disposable {
    public static final float SAIL_DISTANCE = 155f;
    public static final float COMBAT_DISTANCE = 600f;
    public final PerspectiveCamera camera = new PerspectiveCamera(58f, 1280, 720);
    private final ModelBatch batch = new ModelBatch();
    private final Environment light = new Environment();
    private final Array<Model> models = new Array<>();
    private final Array<ModelInstance> scenery = new Array<>();
    private final ModelCache sceneryCache = new ModelCache();
    private final ModelInstance ship, pirate, ocean, ripples, wake, whiteBall, blackBall;
    private final Vector3 target = new Vector3(), desired = new Vector3(), point = new Vector3();
    private float distance = SAIL_DISTANCE, heading, time;
    private boolean initialized;
    private static final long ATTR = Usage.Position | Usage.Normal;
    private static final Color SKY = new Color(.48f, .70f, .80f, 1);

    public VoyageWorldRenderer() {
        camera.near = 2f;
        camera.far = 6500f;
        light.set(new ColorAttribute(ColorAttribute.AmbientLight, .66f, .70f, .74f, 1));
        light.set(new ColorAttribute(ColorAttribute.Fog, SKY));
        light.add(new DirectionalLight().set(1f, .88f, .66f, -.5f, -.85f, -.3f));
        ship = new ModelInstance(shipModel(false));
        pirate = new ModelInstance(shipModel(true));
        ocean = new ModelInstance(oceanModel());
        ripples = new ModelInstance(waterLines());
        wake = new ModelInstance(wakeModel());
        whiteBall = new ModelInstance(keep(new ModelBuilder().createSphere(5, 5, 5, 8, 6,
                material(.98f, .92f, .70f), ATTR)));
        blackBall = new ModelInstance(keep(new ModelBuilder().createSphere(5, 5, 5, 8, 6,
                material(.10f, .08f, .06f), ATTR)));
        Model port = landModel(true), island = landModel(false);
        for (int i = 0; i < Catalog.PORTS.length; i++) {
            ModelInstance instance = new ModelInstance(port);
            instance.transform.setToTranslation(Catalog.PORT_X[i], 0, -Catalog.PORT_Y[i]);
            scenery.add(instance);
        }
        for (int i = 0; i < Catalog.ISLANDS.length; i++) {
            ModelInstance instance = new ModelInstance(island);
            instance.transform.setToTranslation(Catalog.ISLAND_X[i], 0, -Catalog.ISLAND_Y[i]);
            scenery.add(instance);
        }
        sceneryCache.begin();
        sceneryCache.add(scenery);
        sceneryCache.end();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private Model keep(Model model) { models.add(model); return model; }
    private static Material material(float r, float g, float b) {
        return new Material(ColorAttribute.createDiffuse(r, g, b, 1));
    }
    private static MeshPartBuilder part(ModelBuilder b, String name, Material mat) {
        return b.part(name, GL20.GL_TRIANGLES, ATTR, mat);
    }
    private static void box(ModelBuilder b, String name, Material mat,
                            float x, float y, float z, float w, float h, float d) {
        part(b, name, mat).box(x, y, z, w, h, d);
    }

    /** Local bow points +X, stern -X. Tapered hull is actual closed geometry. */
    private Model shipModel(boolean enemy) {
        ModelBuilder b = new ModelBuilder(); b.begin();
        Material wood = material(.27f, .13f, .065f), trim = material(.66f, .43f, .18f);
        Material canvas = enemy ? material(.27f, .10f, .085f) : material(.90f, .79f, .53f);
        MeshPartBuilder hull = part(b, "hull", wood);
        float[][] outline = {{-25,-9},{-19,-12},{13,-11},{29,0},{13,11},{-19,12}};
        for (int i = 0; i < outline.length; i++) {
            float[] a = outline[i], c = outline[(i+1)%outline.length];
            Vector3 v1 = new Vector3(a[0], 9, a[1]), v2 = new Vector3(c[0], 9, c[1]);
            Vector3 v3 = new Vector3(c[0]*.82f, 1, c[1]*.62f), v4 = new Vector3(a[0]*.82f, 1, a[1]*.62f);
            Vector3 n = new Vector3(v2).sub(v1).crs(new Vector3(v3).sub(v1)).nor();
            hull.rect(v1, v2, v3, v4, n);
            // Deck fan faces upward.
            part(b, "deck"+i, trim).triangle(new Vector3(0,9,0), v2, v1);
        }
        box(b,"stern",wood,-17,14,0,17,10,19);
        box(b,"roof",trim,-17,20,0,20,2,22);
        box(b,"upperCabin",wood,-18,23,0,12,5,15);
        box(b,"upperRoof",trim,-18,26,0,16,2,19);
        for (int i = -1; i <= 1; i++) box(b,"window"+i,material(.95f,.70f,.24f),-25.6f,15,i*5, .5f,3,3);
        for (int side : new int[]{-1,1}) {
            box(b,"rail"+side,trim,-2,12,side*11,37,1.3f,1);
            for (int i=0;i<6;i++) box(b,"post"+side+i,trim,-21+i*7,10.5f,side*11,1,5,1);
            for (int i=0;i<3;i++) box(b,"gun"+side+i,material(.12f,.13f,.13f),-6+i*8,10,side*13,3,3,5);
        }
        for (int mast=0;mast<2;mast++) {
            float x = mast == 0 ? 2 : 17, top = mast == 0 ?  60f : 43;
            box(b,"mast"+mast,wood,x,(top+9)/2,0,1.6f,top-9,1.6f);
            float bottom = mast == 0 ? 21 : 18, half = mast == 0 ? 16 : 10;
            // Thick, lightly bowed sail panels, double visible from bow and stern.
            for (int j=0;j<5;j++) {
                float y = bottom+(top-bottom)*j/5, h=(top-bottom)/5;
                float width=half*(1f-.25f*j/5);
                box(b,"sail"+mast+j,canvas,x-1.5f-(float)Math.sin(j*.65f)*2,y+h/2,0,1.2f,h-.4f,width*2);
                box(b,"batten"+mast+j,trim,x-2,y,0,1, .55f,width*2+1);
            }
            box(b,"pennant"+mast,enemy ? material(.65f,.08f,.05f) : material(.73f,.23f,.12f),x-5,top+1,0,9,3,.7f);
        }
        return keep(b.end());
    }

    private Model landModel(boolean port) {
        ModelBuilder b = new ModelBuilder(); b.begin();
        // Footprints stay inside the existing collision circles (38 port / 30 island).
        MeshPartBuilder sand = part(b,"shore",material(.63f,.59f,.37f));
        sand.setVertexTransform(new Matrix4().setToTranslation(0,1,0));
        sand.cylinder(port ? 74:58,4,port ? 74:58,12);
        MeshPartBuilder hill = part(b,"hill",material(.22f,.40f,.29f));
        hill.setVertexTransform(new Matrix4().setToTranslation(-5,port ? 12:21,0));
        hill.cone(port ? 58:50,port ? 25:44,port ? 58:50,9);
        if (port) {
            Material wall=material(.70f,.58f,.38f), roof=material(.18f,.23f,.23f);
            box(b,"pier",material(.35f,.23f,.13f),23,3,0,25,3,12);
            for (int i=0;i<3;i++) {
                box(b,"house"+i,wall,8,7,i*13-13,12,10,10);
                box(b,"roof"+i,roof,8,13,i*13-13,15,3,13);
            }
            for (int i=0;i<3;i++) {
                box(b,"tower"+i,wall,-13,10+i*9,0,10-i*2,8,10-i*2);
                box(b,"eave"+i,roof,-13,15+i*9,0,16-i*2,2,16-i*2);
            }
        }
        return keep(b.end());
    }

    private Model oceanModel() {
        ModelBuilder b=new ModelBuilder(); b.begin();
        MeshPartBuilder p=part(b,"ocean",material(.075f,.32f,.43f));
        // Subdivision keeps vertex fog local instead of fogging the whole plane from its far corners.
        for (int x=-40;x<40;x++) for (int z=-40;z<40;z++) {
            float px=x*225f,pz=z*225f;
            p.rect(px,0,pz, px,0,pz+225, px+225,0,pz+225, px+225,0,pz, 0,1,0);
        }
        return keep(b.end());
    }

    private Model waterLines() {
        ModelBuilder b = new ModelBuilder(); b.begin();
        MeshPartBuilder p = part(b,"wavelets",material(.23f,.51f,.59f));
        // Deterministic mesh, reused each frame; no textures or per-frame mesh uploads.
        for (int x=-40;x<=40;x++) for (int z=-40;z<=40;z++) {
            float px=x*38f+MathUtils.sin(z*7.1f+x)*12, pz=z*38f+MathUtils.cos(x*3.7f+z)*12;
            float w=9+Math.abs(MathUtils.sin(x+z))*15;
            p.rect(px,0,pz, px,0,pz+.55f, px+w,0,pz+.55f, px+w,0,pz, 0,1,0);
        }
        return keep(b.end());
    }
    private Model wakeModel() {
        ModelBuilder b=new ModelBuilder(); b.begin();
        MeshPartBuilder p=part(b,"foam",material(.65f,.81f,.79f));
        for (int i=0;i<20;i++) {
            float x=-27-i*5;
            p.box(x,0,-11-i*.6f,4,.15f,1.7f);
            p.box(x,0,11+i*.6f,4,.15f,1.7f);
        }
        return keep(b.end());
    }

    public void resize(int width, int height) {
        if (width<=0 || height<=0) return;
        camera.viewportWidth=width; camera.viewportHeight=height;
        camera.update();
    }

    public void render(GameState g, float dt) {
        dt=MathUtils.clamp(dt,0,.1f); time+=dt;
        float alpha=1f-(float)Math.exp(-3f*dt);
        float wanted=g.pirateAlive && !g.failed ? COMBAT_DISTANCE:SAIL_DISTANCE;
        if (!initialized) { heading=g.headingDeg; distance=wanted; }
        distance=MathUtils.lerp(distance,wanted,alpha);
        heading=MathUtils.lerpAngleDeg(heading,g.headingDeg,1f-(float)Math.exp(-5f*dt));
        float fx=MathUtils.cosDeg(heading), fz=-MathUtils.sinDeg(heading);
        float combat=(distance-SAIL_DISTANCE)/(COMBAT_DISTANCE-SAIL_DISTANCE);
        // Aim above the hull to put the visible ship in the lower middle, with a horizon.
        target.set(g.x+fx*22,36,-g.y+fz*22);
        desired.set(g.x-fx*distance,85+combat*255,-g.y-fz*distance);
        if (!initialized || camera.position.dst2(desired)>1600f*1600f) camera.position.set(desired);
        else camera.position.lerp(desired,1f-(float)Math.exp(-8f*dt));
        initialized=true;
        camera.up.set(Vector3.Y); camera.lookAt(target); camera.update();
        Gdx.gl.glViewport(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(SKY.r,SKY.g,SKY.b,1);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);
        ocean.transform.setToTranslation(g.x,-.2f,-g.y);
        ripples.transform.setToTranslation(MathUtils.floor(g.x/38)*38, .05f+MathUtils.sin(time)*.03f,
                MathUtils.floor(-g.y/38)*38+time%38);
        ship.transform.setToTranslation(g.x,MathUtils.sin(time*1.6f)*.45f,-g.y).rotate(Vector3.Y,g.headingDeg);
        pirate.transform.setToTranslation(g.pirateX,.2f,-g.pirateY).rotate(Vector3.Y,g.pirateHeading);
        batch.begin(camera);
        batch.render(ocean,light); batch.render(ripples,light);
        batch.render(sceneryCache,light);
        if (g.speed>1) {
            wake.transform.setToTranslation(g.x,.25f,-g.y).rotate(Vector3.Y,g.headingDeg).scale(MathUtils.clamp(g.speed/90,.2f,1.3f),1,1);
            batch.render(wake,light);
        }
        batch.render(ship,light);
        if (g.pirateAlive) batch.render(pirate,light);
        for (int i=0;i<g.ballCount;i++) {
            ModelInstance ball=g.ballFromPlayer[i]?whiteBall:blackBall;
            ball.transform.setToTranslation(g.ballX[i],12,-g.ballY[i]);
            // Flush because the same instance is reused for subsequent shots.
            batch.render(ball,light); batch.flush();
        }
        batch.end();
        // Scene2D and ShapeRenderer must never inherit depth/culling state.
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glDepthMask(true);
    }

    /** Project a world anchor to screen pixels (bottom-left origin), rejecting points behind the eye. */
    public boolean project(float x,float y,float height,Vector3 out) {
        point.set(x,height,-y);
        if (newDirectionDot(point)<=camera.near) return false;
        camera.project(out.set(point));
        return out.z>=0 && out.z<=1 && out.x>=0 && out.x<=camera.viewportWidth && out.y>=0 && out.y<=camera.viewportHeight;
    }
    private float newDirectionDot(Vector3 p) {
        return (p.x-camera.position.x)*camera.direction.x+(p.y-camera.position.y)*camera.direction.y+(p.z-camera.position.z)*camera.direction.z;
    }

    /** Ray-vs-proxy picking uses exactly the rendered transform, never orthographic unprojection. */
    public boolean hit(float screenX,float screenY,float x,float y,float height,float radius) {
        Ray ray=camera.getPickRay(screenX,screenY);
        return Intersector.intersectRaySphere(ray,point.set(x,height,-y),radius,null);
    }
    public float chaseDistance() { return distance; }
    @Override public void dispose() { batch.dispose(); sceneryCache.dispose(); for (Model m:models) m.dispose(); models.clear(); }
}
