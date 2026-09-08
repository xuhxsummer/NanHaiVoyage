package com.shipgame.nanhai.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.shipgame.nanhai.data.Catalog;
import com.shipgame.nanhai.data.GameState;
import com.shipgame.nanhai.data.VoyageGeometry;

/** Map (x,y) becomes (x,height,-y); gameplay owns land-clearance correction. */
public final class VoyageWorldRenderer implements Disposable {
    public static final float SAIL_DISTANCE = 155f;
    public static final float COMBAT_DISTANCE = 600f;
    public final PerspectiveCamera camera = new PerspectiveCamera(58f, 1280, 720);
    private final ModelBatch batch = new ModelBatch();
    private final Environment light = new Environment();
    private final Array<Model> models = new Array<>();
    private final Array<ModelInstance> scenery = new Array<>();
    private final ModelCache sceneryCache = new ModelCache();
    private final ModelInstance[] ships = new ModelInstance[Catalog.SHIPS.length];
    private final ModelInstance pirate, ocean, nearOcean, ripples, foam, wake, whiteBall, blackBall;
    private final Vector3 target = new Vector3(), desired = new Vector3(), point = new Vector3();
    private float distance = SAIL_DISTANCE, heading, time;
    private boolean initialized;
    private boolean highWaterQuality = true;
    private static final long ATTR = Usage.Position | Usage.Normal;
    private static final Color SKY = new Color(.48f, .70f, .80f, 1);

    public VoyageWorldRenderer() {
        camera.near = 2f;
        camera.far = 6500f;
        light.set(new ColorAttribute(ColorAttribute.AmbientLight, .66f, .70f, .74f, 1));
        light.set(new ColorAttribute(ColorAttribute.Fog, SKY));
        light.add(new DirectionalLight().set(1f, .88f, .66f, -.5f, -.85f, -.3f));
        for (int i=0;i<ships.length;i++) ships[i] = new ModelInstance(shipModel(i, false));
        pirate = new ModelInstance(shipModel(4, true));
        ocean = new ModelInstance(oceanModel());
        nearOcean = new ModelInstance(nearOceanModel());
        ripples = new ModelInstance(waterLines());
        foam = new ModelInstance(foamModel());
        wake = new ModelInstance(wakeModel());
        whiteBall = new ModelInstance(keep(new ModelBuilder().createSphere(5, 5, 5, 8, 6,
                material(.98f, .92f, .70f), ATTR)));
        blackBall = new ModelInstance(keep(new ModelBuilder().createSphere(5, 5, 5, 8, 6,
                material(.10f, .08f, .06f), ATTR)));
        for (int i = 0; i < Catalog.PORTS.length; i++) {
            ModelInstance instance = new ModelInstance(landModel(true, i));
            instance.transform.setToTranslation(Catalog.PORT_X[i], 0, -Catalog.PORT_Y[i]);
            scenery.add(instance);
        }
        for (int i = 0; i < Catalog.ISLANDS.length; i++) {
            ModelInstance instance = new ModelInstance(landModel(false, i));
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
    private static Material waterMaterial(float r, float g, float b) {
        return new Material(ColorAttribute.createDiffuse(r, g, b, 1),
                ColorAttribute.createSpecular(.55f, .78f, .86f, 1),
                FloatAttribute.createShininess(72f));
    }
    private static MeshPartBuilder part(ModelBuilder b, String name, Material mat) {
        return b.part(name, GL20.GL_TRIANGLES, ATTR, mat);
    }
    private static void box(ModelBuilder b, String name, Material mat,
                            float x, float y, float z, float w, float h, float d) {
        part(b, name, mat).box(x, y, z, w, h, d);
    }

    /** Local bow points +X, stern -X. Tapered hull is actual closed geometry. */
    private Model shipModel(int id, boolean enemy) {
        ModelBuilder b = new ModelBuilder(); b.begin();
        Material wood = material(.27f, .13f, .065f), trim = material(.66f, .43f, .18f);
        VoyageGeometry.Ship style = VoyageGeometry.ship(id);
        Material canvas = enemy ? material(.27f, .10f, .085f) : color(style.color);
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
        for (int mast=0;mast<style.sails;mast++) {
            float x = style.sails==1 ? 7 : -5+mast*13, top = mast == 0 ? 60f : 49-mast*5;
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
        Model model = b.end();
        // Scale baked vertices so bounds and collisions use the same dimensions.
        for (com.badlogic.gdx.graphics.Mesh mesh : model.meshes)
            mesh.transform(new Matrix4().setToScaling(style.length,style.height,style.beam));
        return keep(model);
    }

    private static Material color(int rgb) {
        return material(((rgb>>16)&255)/255f, ((rgb>>8)&255)/255f, (rgb&255)/255f);
    }

    private Model landModel(boolean port, int id) {
        ModelBuilder b = new ModelBuilder(); b.begin();
        java.util.Random random = new java.util.Random((port ? 7109L:1907L)+id*104729L);
        float radius=VoyageGeometry.landRadius(port,id);
        Material sand=material(.20f+random.nextFloat()*.10f,.36f+random.nextFloat()*.16f,.22f+random.nextFloat()*.10f);
        Material rock=material(.16f+random.nextFloat()*.14f,.24f+random.nextFloat()*.16f,.18f+random.nextFloat()*.12f);
        MeshPartBuilder shore=part(b,"shore",sand);
        int count=14+id%5;
        for(int v=0;v<count;v++) {
            float a=v*MathUtils.PI2/count, c=(v+1)*MathUtils.PI2/count;
            float r=VoyageGeometry.shoreRadius(port,id,v), t=VoyageGeometry.shoreRadius(port,id,(v+1)%count);
            Vector3 p=new Vector3(MathUtils.cos(a)*r,2,MathUtils.sin(a)*r);
            Vector3 q=new Vector3(MathUtils.cos(c)*t,2,MathUtils.sin(c)*t);
            shore.triangle(new Vector3(0,2,0),q,p);
            shore.rect(p,q,new Vector3(q.x,-2,q.z),new Vector3(p.x,-2,p.z),new Vector3(p.x+q.x,0,p.z+q.z).nor());
        }
        if(port) {
            Material wall=material(.48f+random.nextFloat()*.18f,.20f+random.nextFloat()*.10f,.11f+random.nextFloat()*.08f);
            Material roof=color(new int[]{0x172c35,0x263b38,0x3b3028,0x24413d,0x332c3b,0x1d3445}[id%6]);
            Material trim=color(0xc08b43);
            int houses=(id==0 ? 7 : 3+id%4);
            for(int h=0;h<houses;h++) {
                float angle=h*MathUtils.PI2/houses, x=MathUtils.cos(angle)*15, z=MathUtils.sin(angle)*15;
                float height=6+random.nextFloat()*7;
                box(b,"house"+h,wall,x,2+height/2,z,8,height,7);
                MeshPartBuilder roofMesh=part(b,"roof"+h,roof);
                roofMesh.setVertexTransform(new Matrix4().setToTranslation(x,3+height,z));
                roofMesh.cone(11,2.8f,10,4);
                box(b,"beam"+h,trim,x,2.8f+height,z,8.8f,.45f,7.8f);
                if (id==0) {
                    Material lantern=material(1.0f,.56f,.12f);
                    box(b,"lantern"+h,lantern,x,4+height*.55f,z,1.2f,2.0f,1.2f);
                }
            }
            float pierLength=10+id%5*2;
            box(b,"pier",color(0x624126),radius-pierLength/2-2,3,0,pierLength,3,7);
            // Small moored junk silhouette beside the pier.
            float boatX=radius-7, boatZ=8;
            box(b,"mooredHull",color(0x3b2114),boatX,4,boatZ,11,2.5f,4.5f);
            box(b,"mooredMast",color(0x6b4523),boatX,11,boatZ,0.7f,14,0.7f);
            box(b,"mooredSail",color(0xd8b779),boatX+1,10,boatZ,0.7f,8,7);
            int levels=(id==0 ? 3 : 1+id%4);
            for(int level=0;level<levels;level++) {
                float width=9-level*1.1f;
                box(b,"tower"+level,wall,-7,7+level*8,-3,width,8,width);
                MeshPartBuilder eave=part(b,"eave"+level,roof);
                eave.setVertexTransform(new Matrix4().setToTranslation(-7,12+level*8,-3));
                eave.cone(13-level,2.4f,13-level,4);
                box(b,"towerTrim"+level,trim,-7,12.8f+level*8,-3,width+.8f,.35f,width+.8f);
            }
            MeshPartBuilder hill=part(b,"hillside",rock);
            hill.setVertexTransform(new Matrix4().setToTranslation(-17,7+id%3*2,0));
            hill.cone(15,14+id%3*4,16,7+id%4);
            // A compact rear ridge gives the harbor a mountain backdrop without
            // changing the gameplay collision footprint.
            MeshPartBuilder ridge=part(b,"mountainRidge",rock);
            ridge.setVertexTransform(new Matrix4().setToTranslation(7,10,-radius*.42f));
            ridge.cone(Math.min(radius*.52f,22),22+id%3*7,18,7);
        } else {
            // Reef arcs, cliff stacks and wooded peaks use different silhouettes.
            int kind=id%3, peaks=2+id%4;
            for(int k=0;k<peaks;k++) {
                float angle=(k/(float)peaks)*MathUtils.PI2+.17f*id;
                float x=MathUtils.cos(angle)*radius*.48f, z=MathUtils.sin(angle)*radius*.48f;
                float height=kind==0 ? 15+random.nextFloat()*23 : kind==1 ? 7+random.nextFloat()*12 : 3+random.nextFloat()*5;
                MeshPartBuilder peak=part(b,"rock"+k,rock);
                peak.setVertexTransform(new Matrix4().setToTranslation(x,2+height/2,z));
                if(kind==1) peak.cylinder(12,height,11,5+id%4);
                else peak.cone(15,height,14,6+id%3);
                if(kind!=2) {
                    box(b,"trunk"+k,color(0x684529),x,5,z,1.4f,7,1.4f);
                    MeshPartBuilder tree=part(b,"tree"+k,material(.13f,.31f+id*.008f,.18f));
                    tree.setVertexTransform(new Matrix4().setToTranslation(x,11,z)); tree.cone(8,10,8,5);
                }
            }
            if(kind==2) {
                // Shallow enclosed lagoon: water inset, surrounded by the solid reef platform.
                MeshPartBuilder lagoon=part(b,"lagoon",material(.10f,.56f,.58f));
                lagoon.setVertexTransform(new Matrix4().setToTranslation(0,2.1f,0));
                lagoon.cylinder(17+id*.25f,.15f,12,11);
            }
        }
        return keep(b.end());
    }

    private Model oceanModel() {
        ModelBuilder b=new ModelBuilder(); b.begin();
        MeshPartBuilder p=part(b,"ocean",waterMaterial(.025f,.16f,.29f));
        // Subdivision keeps vertex fog local instead of fogging the whole plane from its far corners.
        for (int x=-40;x<40;x++) for (int z=-40;z<40;z++) {
            float px=x*225f,pz=z*225f;
            float y0=wave(px,pz), y1=wave(px,pz+225), y2=wave(px+225,pz+225), y3=wave(px+225,pz);
            p.rect(px,y0,pz, px,y1,pz+225, px+225,y2,pz+225, px+225,y3,pz, 0,1,0);
        }
        return keep(b.end());
    }

    private Model nearOceanModel() {
        ModelBuilder b=new ModelBuilder(); b.begin();
        MeshPartBuilder p=part(b,"nearWater",waterMaterial(.04f,.34f,.43f));
        float s=1100f;
        for(int x=-5;x<5;x++) for(int z=-5;z<5;z++) {
            float px=x*s,pz=z*s;
            p.rect(px,wave(px,pz),pz, px,wave(px,pz+s),pz+s, px+s,wave(px+s,pz+s),pz+s, px+s,wave(px+s,pz),pz,0,1,0);
        }
        return keep(b.end());
    }
    private static float wave(float x,float z) {
        return .65f*(float)Math.sin(x*.006+z*.003)+.28f*(float)Math.cos(z*.009-x*.002);
    }

    private Model waterLines() {
        ModelBuilder b = new ModelBuilder(); b.begin();
        MeshPartBuilder p = part(b,"wavelets",material(.28f,.60f,.68f));
        // Deterministic mesh, reused each frame; no textures or per-frame mesh uploads.
        for (int x=-40;x<=40;x++) for (int z=-40;z<=40;z++) {
            float px=x*38f+MathUtils.sin(z*7.1f+x)*12, pz=z*38f+MathUtils.cos(x*3.7f+z)*12;
            float w=9+Math.abs(MathUtils.sin(x+z))*15;
            p.rect(px,0,pz, px,0,pz+.55f, px+w,0,pz+.55f, px+w,0,pz, 0,1,0);
            if ((x+z)%3==0) p.rect(px+13, .015f, pz+4, px+13, .015f, pz+4.35f, px+w+10, .015f, pz+4.35f, px+w+10, .015f, pz+4, 0,1,0);
        }
        return keep(b.end());
    }
    private Model foamModel() {
        ModelBuilder b=new ModelBuilder(); b.begin();
        MeshPartBuilder p=part(b,"whitecaps",waterMaterial(.65f,.88f,.88f));
        java.util.Random r=new java.util.Random(81273L);
        for(int i=0;i<180;i++) {
            float x=-1450+r.nextFloat()*2900, z=-1450+r.nextFloat()*2900;
            float w=3+r.nextFloat()*16;
            p.rect(x,.9f,z,x,.9f,z+.7f,x+w,.9f,z+.7f,x+w,.9f,z,0,1,0);
        }
        return keep(b.end());
    }
    private Model wakeModel() {
        ModelBuilder b=new ModelBuilder(); b.begin();
        MeshPartBuilder p=part(b,"foam",material(.65f,.81f,.79f));
        for (int i=0;i<28;i++) {
            float x=-25-i*4.2f, spread=8.5f+i*.42f;
            float bend=(float)Math.sin(i*.38f)*2.4f;
            p.box(x,0,-spread+bend,4.5f,.24f,1.8f);
            p.box(x,0,spread+bend,4.5f,.24f,1.8f);
            if(i%3==0) p.box(x-1.4f,0,-spread*.5f+bend,3.2f,.18f,1.2f);
        }
        return keep(b.end());
    }

    public void resize(int width, int height) {
        if (width<=0 || height<=0) return;
        camera.viewportWidth=width; camera.viewportHeight=height;
        camera.update();
    }

    public void render(GameState g, float dt) {
        g.ensureLandClearance();
        ModelInstance ship=ships[VoyageGeometry.shipIndex(g.ship)];
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
        nearOcean.transform.setToTranslation(g.x,-.28f,-g.y);
        ripples.transform.setToTranslation(MathUtils.floor(g.x/38)*38, .05f+MathUtils.sin(time)*.03f,
                MathUtils.floor(-g.y/38)*38+time%38);
        foam.transform.setToTranslation(MathUtils.floor(g.x/90)*90, .72f,
                MathUtils.floor(-g.y/90)*90+time%90);
        ship.transform.setToTranslation(g.x,MathUtils.sin(time*1.6f)*.45f,-g.y).rotate(Vector3.Y,g.headingDeg);
        pirate.transform.setToTranslation(g.pirateX,.2f,-g.pirateY).rotate(Vector3.Y,g.pirateHeading);
        batch.begin(camera);
        batch.render(ocean,light); batch.render(nearOcean,light); batch.render(ripples,light);
        if (highWaterQuality) batch.render(foam,light);
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
    /** Disable scattered whitecaps on weaker devices while retaining waves, specular water and wake. */
    public void setWaterQuality(boolean high) { highWaterQuality = high; }
    @Override public void dispose() { batch.dispose(); sceneryCache.dispose(); for (Model m:models) m.dispose(); models.clear(); }
}
