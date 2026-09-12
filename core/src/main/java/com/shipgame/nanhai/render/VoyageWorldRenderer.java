package com.shipgame.nanhai.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
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
    public static final float SAIL_DISTANCE = 185f;
    public static final float COMBAT_DISTANCE = 600f;
    /** 0.28.21 上帝视角：true 时恒用 COMBAT_DISTANCE（设置页可切换，存 prefs）。 */
    public boolean godView = false;
    public final PerspectiveCamera camera = new PerspectiveCamera(58f, 1280, 720);
    private final ModelBatch batch = new ModelBatch();
    private final Environment light = new Environment();
    private final Array<Model> models = new Array<>();
    private final Array<ModelInstance> scenery = new Array<>();
    private final ModelCache sceneryCache = new ModelCache();
    private final ModelInstance[] ships = new ModelInstance[Catalog.SHIPS.length];
    private final ModelInstance[] merchantShips = new ModelInstance[Catalog.SHIPS.length];
    // 0.28.22 沉船表现：每艘沉船一个独立模型实例（复用船模几何），
    // 倾侧 + 下沉，动画结束后移除 —— 沉船从不瞬间消失。
    private final ModelInstance[] wreckInstances = new ModelInstance[GameState.WRECK_SLOTS];
    private final ModelInstance pirate, ocean, ripples, foam, wake, whiteBall, blackBall, spray;
    private ModelInstance star, moon; // 0.28.26 夜空
    private final ModelInstance[][] traderInstances=new ModelInstance[2][Catalog.SHIPS.length];
    private final ModelInstance[][] warshipInstances=new ModelInstance[2][Catalog.SHIPS.length];
    private final int[] wreckTypes=new int[GameState.WRECK_SLOTS];
    private final boolean[] wreckPirates=new boolean[GameState.WRECK_SLOTS];
    private final Vector3 focusOffset=new Vector3();
    private final ModelInstance[] idleRings = new ModelInstance[3];
    private final VoyageWater water = new VoyageWater();
    private final Vector3 target = new Vector3(), desired = new Vector3(), point = new Vector3();
    private float distance = SAIL_DISTANCE, heading, time;
    private final Vector3 chasePosition = new Vector3(), orbit = new Vector3();
    private float lookYaw, lookPitch, returnYaw, returnPitch, returnTime;
    private boolean looking;
    public static final float LOOK_RETURN_SECONDS = .32f;
    private boolean initialized;
    private boolean highWaterQuality = true;
    private static final long ATTR = Usage.Position | Usage.Normal;
    private static final Color SKY = new Color(.48f, .70f, .80f, 1);
    // 0.28.26 昼夜天空：按游戏时钟在白昼/黄昏/夜晚配色间平滑过渡（无硬切换）。
    private static final Color DAY_SKY = new Color(.48f, .70f, .80f, 1);
    private static final Color DUSK_SKY = new Color(.80f, .55f, .38f, 1);
    private static final Color NIGHT_SKY = new Color(.07f, .10f, .18f, 1);
    private static final Color DAY_SEA = new Color(.29f, .55f, .62f, 1);
    private static final Color NIGHT_SEA = new Color(.13f, .21f, .29f, 1);
    private final Color skyNow = new Color(DAY_SKY);
    private final Color seaTintNow = new Color(DAY_SEA);
    private final Color lightColorNow = new Color(1f, .88f, .66f, 1);
    private float nightAmount; // 0=白天 1=深夜
    private final Color ambientDay = new Color(.66f, .70f, .74f, 1);
    private final Color ambientNight = new Color(.30f, .34f, .44f, 1);
    private static final int STAR_COUNT = 42;
    private final float[] starAz = new float[STAR_COUNT];
    private final float[] starEl = new float[STAR_COUNT];
    private final float[] starSize = new float[STAR_COUNT];

    public VoyageWorldRenderer() {
        java.util.Random starRandom = new java.util.Random(20260912L);
        for (int i = 0; i < STAR_COUNT; i++) {
            starAz[i] = starRandom.nextFloat() * 360f;
            starEl[i] = 14f + starRandom.nextFloat() * 54f;
            starSize[i] = 1.2f + starRandom.nextFloat() * 2.1f;
        }
        camera.near = 2f;
        camera.far = 6500f;
        light.set(new ColorAttribute(ColorAttribute.AmbientLight, .66f, .70f, .74f, 1));
        light.set(new ColorAttribute(ColorAttribute.Fog, SKY));
        light.add(new DirectionalLight().set(1f, .88f, .66f, -.5f, -.85f, -.3f));
        for (int i=0;i<ships.length;i++) ships[i] = new ModelInstance(shipModel(i, false));
        for(int i=0;i<ships.length;i++) merchantShips[i]=new ModelInstance(ships[i].model);
        java.util.Arrays.fill(wreckTypes,-1);
        for(int slot=0;slot<2;slot++) for(int type=0;type<ships.length;type++) {
            traderInstances[slot][type]=new ModelInstance(ships[type].model);
            warshipInstances[slot][type]=new ModelInstance(ships[type].model);
        }
        spray=new ModelInstance(keep(new ModelBuilder().createSphere(3,3,3,6,4,
                new Material(ColorAttribute.createDiffuse(.8f,.95f,1f,1f),new BlendingAttribute(true,.8f)),ATTR)));
        pirate = new ModelInstance(shipModel(VoyageGeometry.PIRATE_SHIP, true));
        ocean = new ModelInstance(oceanModel());
        ripples = new ModelInstance(waterLines());
        foam = new ModelInstance(foamModel());
        wake = new ModelInstance(wakeModel());
        Model idleRing = idleRingModel();
        for (int i=0;i<idleRings.length;i++) idleRings[i]=new ModelInstance(idleRing);
        whiteBall = new ModelInstance(keep(new ModelBuilder().createSphere(5, 5, 5, 8, 6,
                material(.98f, .92f, .70f), ATTR)));
        blackBall = new ModelInstance(keep(new ModelBuilder().createSphere(5, 5, 5, 8, 6,
                material(.10f, .08f, .06f), ATTR)));
        // 0.28.26 夜空星星与月亮（无深度写入的 blending 白球，只在夜幕下渲染）。
        star = new ModelInstance(keep(new ModelBuilder().createSphere(4, 4, 4, 6, 5,
                new Material(ColorAttribute.createDiffuse(1f, 1f, .92f, 1f),
                        new BlendingAttribute(true, 0f)), ATTR)));
        moon = new ModelInstance(keep(new ModelBuilder().createSphere(46, 46, 46, 12, 10,
                new Material(ColorAttribute.createDiffuse(.93f, .93f, .85f, 1f),
                        new BlendingAttribute(true, 0f)), ATTR)));
        for (int i = 0; i < Catalog.PORTS.length; i++) {
            ModelInstance instance = new ModelInstance(keep(VoyageLandModels.create(true, i)));
            instance.transform.setToTranslation(Catalog.PORT_X[i], 0, -Catalog.PORT_Y[i]);
            scenery.add(instance);
        }
        for (int i = 0; i < Catalog.ISLANDS.length; i++) {
            ModelInstance instance = new ModelInstance(keep(VoyageLandModels.create(false, i)));
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

    /** Thin rectangular spars/ropes; geometry is baked once, never rebuilt at runtime. */
    private static void spar(ModelBuilder b, Material mat, Vector3 a, Vector3 c, float width) {
        Vector3 direction = new Vector3(c).sub(a);
        MeshPartBuilder p = part(b, "spar", mat);
        p.setVertexTransform(new Matrix4().set(new Vector3(a).add(c).scl(.5f),
                new Quaternion().setFromCross(Vector3.Y, direction.cpy().nor()), new Vector3(1,1,1)));
        p.box(width, direction.len(), width);
    }

    /** Swept hip roof with rising eave tips and individually visible tile ribs. */
    private static void tiledRoof(ModelBuilder b, Material tile, Material edge,
                                  float x, float y, float z, float w, float d) {
        for (int side : new int[]{-1,1}) {
            for (int strip=0; strip<4; strip++) {
                float t=strip/4f, u=(strip+1)/4f;
                float ya=y+2.8f*(1-t)*(1-t)+.7f*t*t*t;
                float yb=y+2.8f*(1-u)*(1-u)+.7f*u*u*u;
                Vector3 a=new Vector3(x-w*.38f-w*.12f*t,ya,z+side*d*.5f*t);
                Vector3 c=new Vector3(x+w*.38f+w*.12f*t,ya,z+side*d*.5f*t);
                Vector3 e=new Vector3(x+w*.38f+w*.12f*u,yb,z+side*d*.5f*u);
                Vector3 f=new Vector3(x-w*.38f-w*.12f*u,yb,z+side*d*.5f*u);
                MeshPartBuilder p=part(b,"roofSurface",tile);
                Vector3 normal=new Vector3(c).sub(a).crs(new Vector3(e).sub(a)).nor();
                if(normal.y<0) p.rect(f,e,c,a,normal.scl(-1)); else p.rect(a,c,e,f,normal);
                for(int rib=0;rib<=8;rib++) spar(b,tile,
                        new Vector3(a).lerp(c,rib/8f),new Vector3(f).lerp(e,rib/8f),.10f);
            }
        }
        spar(b,edge,new Vector3(x-w*.42f,y+2.9f,z),new Vector3(x+w*.42f,y+2.9f,z),.3f);
    }

    /** Local bow points +X, stern -X. Tapered hull is actual closed geometry. */
    private Model shipModel(int id, boolean enemy) {
        ModelBuilder b = new ModelBuilder(); b.begin();
        Material wood = material(.23f, .115f, .065f), trim = material(.43f, .30f, .16f);
        VoyageGeometry.Ship style = VoyageGeometry.ship(id);
        Material canvas = enemy ? material(.27f, .10f, .085f) : color(style.color);
        Material rope=material(.42f,.34f,.23f), darkWood=material(.17f,.085f,.045f);
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
        tiledRoof(b,color(0x273b3c),trim,-18,27,0,16,19);
        for (int i = -1; i <= 1; i++) box(b,"window"+i,material(.95f,.70f,.24f),-25.6f,15,i*5, .5f,3,3);
        for (int side : new int[]{-1,1}) {
            for(int plank=0;plank<5;plank++) {
                float height=2.8f+plank*1.3f;
                float breadth=7.7f+plank*.85f;
                spar(b,plank%2==0?trim:darkWood,new Vector3(-19,height,side*breadth),
                        new Vector3(13,height,side*(breadth-.5f)),.23f);
            }
            for(int window=0;window<5;window++) {
                box(b,"sternLattice",darkWood,-23+window*2.7f,16,side*9.6f,1.7f,2.7f,.18f);
                box(b,"windowMullion",trim,-23+window*2.7f,16,side*9.8f,.18f,2.7f,.16f);
            }
            box(b,"rail"+side,trim,-2,12,side*11,37,1.3f,1);
            for (int i=0;i<6;i++) box(b,"post"+side+i,trim,-21+i*7,10.5f,side*11,1,5,1);
            for (int i=0;i<3;i++) box(b,"gun"+side+i,material(.12f,.13f,.13f),-6+i*8,10,side*13,3,3,5);
        }
        for (int mast=0;mast<style.sails;mast++) {
            float x = style.sails==1 ? 7 : -5+mast*13, top = mast == 0 ? 60f : 49-mast*5;
            box(b,"mast"+mast,wood,x,(top+9)/2,0,1.6f,top-9,1.6f);
            float bottom = mast == 0 ? 21 : 18, half = mast == 0 ? 16 : 10;
            for(int side:new int[]{-1,1}) {
                spar(b,rope,new Vector3(x,top,0),new Vector3(-18,12,side*10),.16f);
                spar(b,rope,new Vector3(x,top-4,0),new Vector3(19,11,side*8),.14f);
            }
            // Thick, lightly bowed sail panels, double visible from bow and stern.
            for (int j=0;j<9;j++) {
                float y = bottom+(top-bottom)*j/9, h=(top-bottom)/9;
                float width=half*(1f-.35f*j/9);
                float belly=x-1.5f-(float)Math.sin(j*.34f)*2;
                box(b,"sail"+mast+j,canvas,belly,y+h/2,0,.22f,h-.10f,width*2);
                box(b,"batten"+mast+j,rope,belly-.18f,y,0,.25f,.20f,width*2);
                if(mast==0 && j>2 && j<7) {
                    float markWidth=(j==3 || j==6)?2.3f:4.5f;
                    box(b,"sailSeal",material(.48f,.12f,.09f),belly-.14f,y+h/2,0,.04f,h-.15f,markWidth);
                }
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

    private Model oceanModel() {
        ModelBuilder b=new ModelBuilder(); b.begin();
        MeshPartBuilder p=part(b,"ocean",waterMaterial(.035f,.25f,.32f));
        // Subdivision keeps vertex fog local instead of fogging the whole plane from its far corners.
        for (int x=-40;x<40;x++) for (int z=-40;z<40;z++) {
            float px=x*225f,pz=z*225f;
            float y0=wave(px,pz), y1=wave(px,pz+225), y2=wave(px+225,pz+225), y3=wave(px+225,pz);
            p.rect(px,y0,pz, px,y1,pz+225, px+225,y2,pz+225, px+225,y3,pz, 0,1,0);
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

    private Model idleRingModel() {
        ModelBuilder b=new ModelBuilder(); b.begin();
        Material mat=material(.38f,.66f,.69f);
        mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA,.14f));
        MeshPartBuilder p=part(b,"idleHullRipples",mat);
        for (int i=0;i<64;i++) {
            // Small gaps soften the fallback ring instead of drawing a solid white outline.
            if (i%9==0) continue;
            float a=i*MathUtils.PI2/64, c=(i+1)*MathUtils.PI2/64;
            float ax=MathUtils.cos(a),az=MathUtils.sin(a),cx=MathUtils.cos(c),cz=MathUtils.sin(c);
            p.rect(ax,0,az, ax*.98f,0,az*.98f, cx*.98f,0,cz*.98f, cx,0,cz, 0,1,0);
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
        g.ensurePirateSeparation();
        ModelInstance ship=ships[VoyageGeometry.shipIndex(g.ship)];
        dt=MathUtils.clamp(dt,0,.1f); time+=dt;
        float alpha=1f-(float)Math.exp(-3f*dt);
        // 0.28.21: 上帝视角始终用战斗级拉远，不受战斗结束回拉影响。
        boolean sinkBeat=g.sinkFocus!=null && g.sinkFocus.alive;
        float wanted=godView || sinkBeat || ((g.pirateAlive || g.merchantLock) && !g.failed)
                ? COMBAT_DISTANCE:SAIL_DISTANCE;
        if (!initialized) { heading=g.headingDeg; distance=wanted; }
        distance=MathUtils.lerp(distance,wanted,alpha);
        heading=MathUtils.lerpAngleDeg(heading,g.headingDeg,1f-(float)Math.exp(-5f*dt));
        float fx=MathUtils.cosDeg(heading), fz=-MathUtils.sinDeg(heading);
        float combat=(distance-SAIL_DISTANCE)/(COMBAT_DISTANCE-SAIL_DISTANCE);
        // Aim above the hull to put the visible ship in the lower middle, with a horizon.
        target.set(g.x+fx*22,36,-g.y+fz*22);
        desired.set(g.x-fx*distance,85+combat*255,-g.y-fz*distance);
        float focus=0;
        if(sinkBeat && !looking) {
            float p=1-g.sinkFocus.timer/g.sinkFocus.duration;
            focus=MathUtils.sin(MathUtils.PI*p)*(g.sinkFocus.player?1f:.28f);
        }
        point.set(sinkBeat?(g.sinkFocus.x-g.x)*focus:0,sinkBeat?-24*g.sinkFocus.submerge*focus:0,
                sinkBeat?-(g.sinkFocus.y-g.y)*focus:0);
        focusOffset.lerp(point,1f-(float)Math.exp(-5f*dt));
        target.add(focusOffset); desired.add(focusOffset.x*.4f,focusOffset.y*.2f,focusOffset.z*.4f);
        if (!initialized || chasePosition.dst2(desired)>1600f*1600f) chasePosition.set(desired);
        else chasePosition.lerp(desired,1f-(float)Math.exp(-8f*dt));
        if (!looking && returnTime < LOOK_RETURN_SECONDS) {
            returnTime = Math.min(LOOK_RETURN_SECONDS, returnTime + dt);
            float t = returnTime / LOOK_RETURN_SECONDS;
            float remaining = 1f - t*t*(3f-2f*t);
            lookYaw = returnYaw * remaining;
            lookPitch = returnPitch * remaining;
        }
        orbit.set(chasePosition).sub(target);
        float elevation = MathUtils.atan2(orbit.y, (float)Math.hypot(orbit.x,orbit.z)) * MathUtils.radiansToDegrees;
        float radius = orbit.len();
        float yaw = MathUtils.atan2(orbit.z,orbit.x) * MathUtils.radiansToDegrees + lookYaw;
        float pitch = MathUtils.clamp(elevation + lookPitch, -6f, 72f);
        camera.position.set(target).add(MathUtils.cosDeg(yaw)*MathUtils.cosDeg(pitch)*radius,
                MathUtils.sinDeg(pitch)*radius, MathUtils.sinDeg(yaw)*MathUtils.cosDeg(pitch)*radius);
        camera.position.y = Math.max(9f,camera.position.y);
        initialized=true;
        camera.up.set(Vector3.Y); camera.lookAt(target); camera.update();
        // 0.28.26 镜头节奏：开炮小后坐、抛锚/起锚短促下沉、战斗回拉更顺滑。
        if (g.cannonKick > 0f) {
            float kick = g.cannonKick * g.cannonKick;
            camera.position.add(fx * kick * 2.2f, -kick * 1.4f, fz * kick * 2.2f);
        }
        if (g.anchorBeat > 0f) {
            float beat = MathUtils.sin(MathUtils.PI * MathUtils.clamp(g.anchorBeat, 0f, 1f));
            camera.position.y -= beat * 10f;
        }
        camera.up.set(Vector3.Y); camera.lookAt(target); camera.update();
        Gdx.gl.glViewport(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
        // 0.28.26 昼夜天空 + 海面亮度（游戏时钟 06:00-18:00 白昼，平滑过渡，无硬切）。
        float clock = g.dayMin; // 0..1439
        float dayness;
        if (clock >= 300f && clock <= 1020f) dayness = 1f;            // 05:00-17:00 白昼
        else if (clock > 1020f && clock < 1140f) dayness = 1f - (clock - 1020f) / 120f; // 17:00-19:00 入夜（黄昏中段）
        else if (clock >= 1260f || clock < 240f) dayness = 0f;        // 21:00-04:00 深夜
        else if (clock >= 240f && clock < 300f) dayness = (clock - 240f) / 60f;         // 04:00-05:00 黎明
        else dayness = 1f - (1140f - clock) / 120f;                    // 19:00-21:00 余晖
        dayness = MathUtils.clamp(dayness, 0f, 1f);
        // 黄昏暖色权重：日出日落前后各约 1 小时，呈三角峰。
        float duskMix = 0f;
        if (clock >= 240f && clock <= 420f) duskMix = 1f - Math.abs(clock - 330f) / 90f;
        else if (clock >= 990f && clock <= 1170f) duskMix = 1f - Math.abs(clock - 1080f) / 90f;
        duskMix = MathUtils.clamp(duskMix, 0f, 1f) * (1f - Math.abs(dayness - 0.5f) * 0.6f);
        skyNow.set(DAY_SKY).lerp(NIGHT_SKY, 1f - dayness).lerp(DUSK_SKY, duskMix * .85f);
        seaTintNow.set(DAY_SEA).lerp(NIGHT_SEA, 1f - dayness);
        lightColorNow.set(1f, .88f, .66f, 1).lerp(new Color(.55f, .62f, .82f, 1), 1f - dayness);
        nightAmount = 1f - dayness;
        // 0.28.26 夜里调暗环境光与月光色方向光，让模型船也跟着天色变暗。
        light.set(new ColorAttribute(ColorAttribute.AmbientLight,
                ambientDay.r + (ambientNight.r - ambientDay.r) * nightAmount,
                ambientDay.g + (ambientNight.g - ambientDay.g) * nightAmount,
                ambientDay.b + (ambientNight.b - ambientDay.b) * nightAmount, 1));
        Gdx.gl.glClearColor(skyNow.r,skyNow.g,skyNow.b,1);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);
        ocean.transform.setToTranslation(g.x,-.2f,-g.y);
        ripples.transform.setToTranslation(MathUtils.floor(g.x/38)*38, .05f+MathUtils.sin(time)*.03f,
                MathUtils.floor(-g.y/38)*38+time%38);
        foam.transform.setToTranslation(MathUtils.floor(g.x/90)*90, .72f,
                MathUtils.floor(-g.y/90)*90+time%90);
        float shipFloat=water.available()?water.surfaceHeight(g.x,-g.y,time,g.windStr,highWaterQuality):MathUtils.sin(time*1.6f)*.45f;
        ship.transform.setToTranslation(g.x,shipFloat,-g.y).rotate(Vector3.Y,g.headingDeg);
        pirate.transform.setToTranslation(g.pirateX,water.surfaceHeight(g.pirateX,-g.pirateY,time,g.windStr,highWaterQuality),-g.pirateY).rotate(Vector3.Y,g.pirateHeading);
        boolean customWater = water.render(camera,g,time,highWaterQuality,skyNow);
        water.setNightDim(nightAmount); // applied to the next frame's water/sky uniforms
        light.set(new ColorAttribute(ColorAttribute.Fog, skyNow));
        batch.begin(camera);
        if (!customWater) {
            batch.render(ocean,light); batch.render(ripples,light);
            if (highWaterQuality) batch.render(foam,light);
        }
        batch.render(sceneryCache,light);
        if (!customWater && !g.playerSunk() && g.speed<22) {
            VoyageGeometry.Ship hull=VoyageGeometry.ship(g.ship);
            for (int i=0;i<idleRings.length;i++) {
                float phase=(time*.24f+i/(float)idleRings.length)%1f;
                ModelInstance ring=idleRings[i];
                ring.transform.setToTranslation(g.x,.85f,-g.y).rotate(Vector3.Y,g.headingDeg)
                        .scale(25*hull.length+phase*13,1,10*hull.beam+phase*10);
                ((BlendingAttribute)ring.materials.first().get(BlendingAttribute.Type)).opacity=
                        .18f*MathUtils.sin(phase*MathUtils.PI)*(1-MathUtils.clamp(g.speed/22,0,1));
                batch.render(ring,light);
            }
        }
        if (!customWater && !g.playerSunk() && g.speed>1) {
            wake.transform.setToTranslation(g.x,.25f,-g.y).rotate(Vector3.Y,g.headingDeg).scale(MathUtils.clamp(g.speed/90,.2f,1.3f),1,1);
            batch.render(wake,light);
        }
        if(!g.playerSunk()) { damageFeedback(ship,g,0); batch.render(ship,light); }
        if (g.pirateAlive) { damageFeedback(pirate,g,1); batch.render(pirate,light); }
        if (g.merchantVisible()) {
            ModelInstance trader=merchantShips[g.merchant.ship];
            trader.transform.setToTranslation(g.merchant.x,water.surfaceHeight(g.merchant.x,-g.merchant.y,time,g.windStr,highWaterQuality),-g.merchant.y).rotate(Vector3.Y,g.merchant.heading);
            damageFeedback(trader,g,2); batch.render(trader,light);
        }
        for(int i=0;i<g.traders.length;i++) {
            com.shipgame.nanhai.data.TraderData t=g.traders[i];
            if(t!=null && t.alive && Catalog.dist(g.x,g.y,t.x,t.y)<=Catalog.NPC_HORIZON)
                renderNpc(traderInstances[i][t.ship],g,t.x,t.y,t.heading,3+i);
        }
        for(int i=0;i<g.warships.length;i++) {
            com.shipgame.nanhai.data.WarshipData w=g.warships[i];
            if(w!=null && w.alive && Catalog.dist(g.x,g.y,w.x,w.y)<=Catalog.NPC_HORIZON)
                renderNpc(warshipInstances[i][w.ship],g,w.x,w.y,w.heading,5+i);
        }
        for (int i=0;i<g.wrecks.length;i++) {
            GameState.Wreck w=g.wrecks[i];
            if(w==null || !w.alive) continue;
            if(wreckTypes[i]!=w.ship || wreckPirates[i]!=w.pirate) {
                wreckInstances[i]=new ModelInstance(w.pirate?pirate.model:ships[w.ship].model);
                wreckTypes[i]=w.ship; wreckPirates[i]=w.pirate;
            }
            ModelInstance wreck=wreckInstances[i];
            float surface=water.available()?water.surfaceHeight(w.x,-w.y,time,g.windStr,highWaterQuality):0;
            wreck.transform.setToTranslation(w.x,surface-w.submerge*90f,-w.y)
                    .rotate(Vector3.Y,w.headingDeg).rotate(Vector3.X,w.sway>=0?w.tilt:-w.tilt)
                    .rotate(Vector3.Y,w.sway*w.submerge);
            batch.render(wreck,light);
            float progress=1-w.timer/w.duration;
            for(int n=0;n<10;n++) {
                float phase=(progress*2+n*.1f)%1,angle=n*137.5f;
                float bubbleRadius=12+phase*30;
                particle(w.x+MathUtils.cosDeg(angle)*bubbleRadius,surface+1+MathUtils.sin(phase*MathUtils.PI)*8,
                        -w.y+MathUtils.sinDeg(angle)*bubbleRadius,1+phase,.7f*(1-phase));
            }
        }
        for(GameState.Splash splash:g.impactSplashes) if(splash!=null && splash.remaining>0) {
            float p=1-splash.remaining/.45f;
            for(int n=0;n<10;n++) {
                float angle=n*36f, splashRadius=5+p*30;
                particle(splash.x+MathUtils.cosDeg(angle)*splashRadius,2+MathUtils.sin(p*MathUtils.PI)*(10+n%3*3),
                        -splash.y+MathUtils.sinDeg(angle)*splashRadius,1.2f-p*.5f,.85f*(1-p));
            }
        }
        for (int i=0;i<g.ballCount;i++) {
            ModelInstance ball=g.ballFromPlayer[i]?whiteBall:blackBall;
            ball.transform.setToTranslation(g.ballX[i],12,-g.ballY[i]);
            // Flush because the same instance is reused for subsequent shots.
            batch.render(ball,light); batch.flush();
        }
        // 0.28.26 开炮炮口闪：船首前方一朵短命的暖色光球。
        if (g.muzzleFlash > 0f && !g.playerSunk()) {
            float flash = g.muzzleFlash;
            spray.transform.setToTranslation(
                    g.x + MathUtils.cosDeg(g.headingDeg) * 34f,
                    16f + 5f * flash,
                    -g.y - MathUtils.sinDeg(g.headingDeg) * 34f)
                    .scl(1f + 1.6f * flash);
            ((BlendingAttribute) spray.materials.first().get(BlendingAttribute.Type)).opacity = .9f * flash;
            batch.render(spray, light);
        }
        batch.end();
        // 0.28.26 星星与月亮：夜空下用无深度写入的 blending 球体贴在天空球面上。
        if (nightAmount > .02f) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glDepthMask(false);
            batch.begin(camera);
            float starAlpha = nightAmount * (.55f + .3f * MathUtils.sin(time * .8f));
            for (int i = 0; i < STAR_COUNT; i++) {
                float az = starAz[i] + time * .6f, el = starEl[i];
                float sr = 2400f;
                star.transform.setToTranslation(
                        camera.position.x + MathUtils.cosDeg(az) * MathUtils.cosDeg(el) * sr,
                        600f + MathUtils.sinDeg(el) * sr,
                        camera.position.z + MathUtils.sinDeg(az) * MathUtils.cosDeg(el) * sr)
                        .scl(starSize[i]);
                ((BlendingAttribute) star.materials.first().get(BlendingAttribute.Type)).opacity = starAlpha;
                batch.render(star, light);
            }
            moon.transform.setToTranslation(
                    camera.position.x + MathUtils.cosDeg(28f + time * .5f) * 2100f,
                    1400f,
                    camera.position.z + MathUtils.sinDeg(28f + time * .5f) * 2100f)
                    .scl(1f);
            ((BlendingAttribute) moon.materials.first().get(BlendingAttribute.Type)).opacity = .85f * nightAmount;
            batch.render(moon, light);
            batch.end();
            Gdx.gl.glDepthMask(true);
        }
        // Scene2D and ShapeRenderer must never inherit depth/culling state.
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glDepthMask(true);
    }

    private void damageFeedback(ModelInstance instance,GameState g,int slot) {
        instance.transform.rotate(Vector3.X,g.hitRock(slot));
        float flash=g.hitFlash(slot);
        for(Material material:instance.materials) {
            if(flash>0) material.set(ColorAttribute.createEmissive(.7f*flash,.035f*flash,.01f*flash,1));
            else material.remove(ColorAttribute.Emissive);
        }
    }
    private void renderNpc(ModelInstance instance,GameState g,float x,float y,float heading,int slot) {
        instance.transform.setToTranslation(x,water.surfaceHeight(x,-y,time,g.windStr,highWaterQuality),-y).rotate(Vector3.Y,heading);
        damageFeedback(instance,g,slot);batch.render(instance,light);
    }
    private void particle(float x,float y,float z,float size,float opacity) {
        // Flush before mutating the reused particle instance/material.
        batch.flush();
        spray.transform.setToTranslation(x,y,z).scale(size,size,size);
        ((BlendingAttribute)spray.materials.first().get(BlendingAttribute.Type)).opacity=opacity;
        batch.render(spray,light);batch.flush();
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
    public void beginLook() { looking = true; }
    /** Deltas are viewport-normalized, so the same gesture works on phones and desktop.
     * Slide left (dx<0) yaws the camera left — natural in both sail and combat zoom. */
    public void dragLook(float dx, float dy) {
        if (!looking) return;
        lookYaw = MathUtils.clamp(lookYaw + dx*180f, -150f, 150f);
        lookPitch = MathUtils.clamp(lookPitch + dy*100f, -50f, 60f);
    }
    public void endLook() {
        looking = false; returnYaw = lookYaw; returnPitch = lookPitch; returnTime = 0f;
    }
    public void resetLook() {
        looking = false; lookYaw = lookPitch = returnYaw = returnPitch = 0f;
        returnTime = LOOK_RETURN_SECONDS;
    }
    public float lookYaw() { return lookYaw; }
    public float lookPitch() { return lookPitch; }
    /** Low uses fewer water triangles/waves/normal layers and a shorter whitecap distance. */
    public void setWaterQuality(boolean high) { highWaterQuality = high; }
    public boolean hasWaterShader() { return water.available(); }
    @Override public void dispose() { water.dispose(); batch.dispose(); sceneryCache.dispose(); for (Model m:models) m.dispose(); models.clear(); }
}
