package com.shipgame.nanhai.render;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.*;
import com.shipgame.nanhai.data.VoyageGeometry;
import java.util.Random;

/** Authored from the 21 port_*.png and 13 island_*.png Gemini paintings.
 * Everything here is solid world geometry: shores, roofs, bridges, trees and ridges.
 * See docs/release-0.28.9.md for the Catalog-indexed visual reference mapping. */
final class VoyageLandModels {
    private static final long ATTR = Usage.Position | Usage.Normal;
    private static final int TANG=0, CANAL=1, STILT=2, TEMPLE=3, KARST=4, CLIFF=5;
    // 广州 潮州 雷州 琼州 崖州 合浦 交州 占城 真腊 佛逝 泉州 福州 明州 钦州 邕州 暹罗 渤泥 吕宋 苏禄 爪哇 扬州
    private static final int[] PORT_KIND = {TANG,TANG,TANG,STILT,CLIFF,TANG,CANAL,TEMPLE,TEMPLE,TEMPLE,
            TANG,KARST,TANG,TANG,KARST,TEMPLE,STILT,STILT,STILT,TEMPLE,CANAL};
    private static final int[] ROOF = {0x78513b,0x454b46,0x887049,0x9b4931,0x876c45,0x68614c,0x776a45,
            0xa5522d,0x9a6936,0xa07b36,0x995138,0x596459,0x435a65,0x5c655a,0x666448,0xb57330,
            0x816544,0x8e734c,0x994e36,0x776247,0x555b51};
    private static final int[] WALL = {0xc8ad7e,0xd0c4a0,0xcdb884,0xcdb285,0xbb9867,0xc4b887,0xc1ab78,
            0xc5854c,0xc79c60,0xc6a65c,0xc9ac7d,0xcbbd94,0xb0b8b1,0xc1b998,0xc4b283,0xd8b263,
            0xb79562,0xc2a574,0xcbbb88,0xb7a575,0xd4c8a3};
    // Wooded coves, flat reefs, scattered sand cays, volcanic bowls and high limestone ridges.
    private static final int[] ISLAND_KIND = {0,0,1,1,2,2,4,3,0,0,5,6,3};

    private final ModelBuilder b = new ModelBuilder();
    private final Random random;
    private final float r;
    private final Material sand=mat(0xbeb38b), stone=mat(0x8b8b70), rock=mat(0x697963),
            green=mat(0x4a6342), leaves=mat(0x657847), wood=mat(0x6e4a2b), trim=mat(0xbe9554),
            water=mat(0x319d9c), dark=mat(0x414d43);

    private VoyageLandModels(boolean port,int id) {
        r=VoyageGeometry.landRadius(port,id);
        random=new Random(7109L+id*104729L+(port?0:1907));
    }
    static Model create(boolean port,int id) {
        VoyageLandModels m=new VoyageLandModels(port,id); m.b.begin();
        if(port) m.port(id); else m.island(id);
        return m.b.end();
    }
    private static Material mat(int rgb) {
        return new Material(ColorAttribute.createDiffuse(((rgb>>16)&255)/255f,((rgb>>8)&255)/255f,(rgb&255)/255f,1));
    }
    private MeshPartBuilder part(String name,Material material) { return b.part(name,GL20.GL_TRIANGLES,ATTR,material); }
    private void box(String name,Material material,float x,float y,float z,float w,float h,float d) {
        part(name,material).box(x,y,z,w,h,d);
    }
    private void beam(String name,Material material,Vector3 a,Vector3 c,float width) {
        Vector3 v=c.cpy().sub(a);
        MeshPartBuilder p=part(name,material);
        p.setVertexTransform(new Matrix4().set(a.cpy().add(c).scl(.5f),
                new Quaternion().setFromCross(Vector3.Y,v.cpy().nor()),new Vector3(1,1,1)));
        p.box(width,v.len(),width);
    }
    private void quad(MeshPartBuilder p,Vector3 a,Vector3 c,Vector3 d,Vector3 e) {
        Vector3 n=c.cpy().sub(a).crs(d.cpy().sub(a)).nor();
        if(n.y<0) p.rect(e,d,c,a,n.scl(-1)); else p.rect(a,c,d,e,n);
    }
    /** Closed irregular sand shelf. Reef shelves have a real opening to the sea. */
    private void shore(float x,float z,float radius,boolean ring,boolean port,int id) {
        int n=40;
        for(int i=0;i<n;i++) {
            float a=i*MathUtils.PI2/n,c=(i+1)*MathUtils.PI2/n;
            // Port quays face +X, with an indentation between the two outer arms.
            float ra=radius*(.91f+.06f*MathUtils.sin(a*5+id)),rc=radius*(.91f+.06f*MathUtils.sin(c*5+id));
            if(port) { ra*=1-.24f*Math.max(0,MathUtils.cos(a)); rc*=1-.24f*Math.max(0,MathUtils.cos(c)); }
            if(ring && i<4) continue;
            Vector3 p=new Vector3(x+MathUtils.cos(a)*ra,2.2f,z+MathUtils.sin(a)*ra);
            Vector3 q=new Vector3(x+MathUtils.cos(c)*rc,2.2f,z+MathUtils.sin(c)*rc);
            MeshPartBuilder beach=part("sand-shore",sand);
            beach.rect(p,q,new Vector3(q.x,-3,q.z),new Vector3(p.x,-3,p.z),new Vector3(MathUtils.cos(a),0,MathUtils.sin(a)));
            if(ring) {
                Vector3 inP=new Vector3(x+MathUtils.cos(a)*radius*.65f,1.1f,z+MathUtils.sin(a)*radius*.65f);
                Vector3 inQ=new Vector3(x+MathUtils.cos(c)*radius*.65f,1.1f,z+MathUtils.sin(c)*radius*.65f);
                quad(beach,p,q,inQ,inP);
            } else {
                Vector3 inP=new Vector3(x+(p.x-x)*.85f,4,z+(p.z-z)*.85f);
                Vector3 inQ=new Vector3(x+(q.x-x)*.85f,4,z+(q.z-z)*.85f);
                quad(beach,p,q,inQ,inP);
                part("soil",green).triangle(new Vector3(x,4,z),inQ,inP);
            }
        }
    }
    /** Faceted, layered ridge rather than a single cone; the vegetation caps follow its profile. */
    private void mountain(float x,float z,float w,float d,float height,boolean limestone) {
        int sides=13,tiers=5;
        float phase=x*.031f+z*.021f;
        float[] profile=limestone?new float[]{1f,.91f,.70f,.73f,.48f,.31f}:new float[]{1f,.90f,.76f,.61f,.45f,.25f};
        for(int tier=0;tier<tiers;tier++) {
            float t=tier/(float)tiers,u=(tier+1f)/tiers;
            float s=profile[tier],ns=profile[tier+1];
            for(int j=0;j<sides;j++) {
                Material surface=tier>1 && (j+tier)%4!=0 ? green : limestone ? stone : rock;
                MeshPartBuilder p=part("ridge-layer",surface);
                float a=j*MathUtils.PI2/sides,c=(j+1)*MathUtils.PI2/sides;
                float ra=1+.12f*MathUtils.sin(a*3+phase),rc=1+.12f*MathUtils.sin(c*3+phase);
                float ha=height*(.94f+.10f*MathUtils.sin(a*3+phase)),hc=height*(.94f+.10f*MathUtils.sin(c*3+phase));
                Vector3 v1=new Vector3(x+w*.20f*t+MathUtils.cos(a)*w*s*ra,3+t*ha,z+d*.13f*t+MathUtils.sin(a)*d*s*ra);
                Vector3 v2=new Vector3(x+w*.20f*t+MathUtils.cos(c)*w*s*rc,3+t*hc,z+d*.13f*t+MathUtils.sin(c)*d*s*rc);
                Vector3 v3=new Vector3(x+w*.20f*u+MathUtils.cos(c)*w*ns*rc,3+u*hc,z+d*.13f*u+MathUtils.sin(c)*d*ns*rc);
                Vector3 v4=new Vector3(x+w*.20f*u+MathUtils.cos(a)*w*ns*ra,3+u*ha,z+d*.13f*u+MathUtils.sin(a)*d*ns*ra);
                quad(p,v1,v2,v3,v4);
                if(tier==tiers-1) p.triangle(new Vector3(x+w*.20f,3+height*.98f,z+d*.13f),v4,v3);
            }
        }
    }
    private void roof(Material tile,float x,float y,float z,float w,float d) {
        MeshPartBuilder surface=part("swept-tile-roof",tile);
        for(int side:new int[]{-1,1}) {
            for(int strip=0;strip<3;strip++) {
                float t=strip/3f,u=(strip+1)/3f;
                float ya=y+w*.20f*(1-t)*(1-t)+w*.04f*t*t*t;
                float yc=y+w*.20f*(1-u)*(1-u)+w*.04f*u*u*u;
                quad(surface,new Vector3(x-w*(.38f+.12f*t),ya,z+side*d*.5f*t),
                        new Vector3(x+w*(.38f+.12f*t),ya,z+side*d*.5f*t),
                        new Vector3(x+w*(.38f+.12f*u),yc,z+side*d*.5f*u),
                        new Vector3(x-w*(.38f+.12f*u),yc,z+side*d*.5f*u));
            }
        }
        box("roof-ridge",trim,x,y+w*.205f,z,w*.87f,.65f,.75f);
        for(int side:new int[]{-1,1}) {
            box("eave",trim,x,y+w*.04f,z+side*d*.5f,w,.4f,.5f);
            for(int rib=-2;rib<=2;rib++) beam("tile-rib",tile,new Vector3(x+rib*w*.16f,y+w*.2f,z),
                    new Vector3(x+rib*w*.20f,y+w*.04f,z+side*d*.5f),.32f);
        }
    }
    private void house(Material wall,Material tile,float x,float z,float w,float h,boolean stilts) {
        float base=stilts?10:5, d=w*.76f;
        box("foundation",stilts?wood:stone,x,base*.5f,z,w*.88f,base,d*.86f);
        if(stilts) for(int side:new int[]{-1,1}) for(int end:new int[]{-1,1})
            box("stilt",wood,x+side*w*.40f,4,z+end*d*.40f,1.2f,10,1.2f);
        box("plaster-house",wall,x,base+h/2,z,w*.8f,h,d*.82f);
        roof(tile,x,base+h,z,w,d);
        for(int window=-1;window<=1;window++)
            box("window",dark,x+w*.406f,base+h*.58f,z+window*d*.23f,.4f,h*.32f,d*.14f);
        box("gallery",wood,x+w*.42f,base+1,z,2,1,d);
    }
    private void tree(float x,float z,float size,boolean palm) {
        box("tree-trunk",wood,x,4+size*.6f,z,size*.10f,size*1.2f,size*.10f);
        if(palm) {
            for(int f=0;f<6;f++) {
                float a=f*60;
                Vector3 top=new Vector3(x,4+size*1.4f,z),end=new Vector3(x+MathUtils.cosDeg(a)*size*.65f,4+size,z+MathUtils.sinDeg(a)*size*.65f);
                Vector3 mid=top.cpy().lerp(end,.5f).add(0,size*.16f,0), side=new Vector3(-MathUtils.sinDeg(a)*size*.18f,0,MathUtils.cosDeg(a)*size*.18f);
                MeshPartBuilder p=part("palm-frond",leaves);
                quad(p,top,mid.cpy().add(side),end,mid.cpy().sub(side));
            }
        } else {
            for(int c=0;c<3;c++) {
                MeshPartBuilder p=part("tree-crown",c==1?leaves:green);
                p.setVertexTransform(new Matrix4().setToTranslation(x+(c-1)*size*.22f,4+size*(1.1f+c*.14f),z));
                p.sphere(size*.85f,size*.65f,size*.80f,7,4);
            }
        }
    }
    private void pier(float z,float length) {
        float x=r*.60f;
        box("wooden-pier",wood,x+length/2,5,z,length,2.4f,10);
        for(int p=0;p<=8;p++) {
            float px=x+length*p/8;
            box("pier-plank",trim,px,6.3f,z,.7f,.35f,10);
            if(p%2==0) for(int side:new int[]{-1,1})
                box("pier-pile",wood,px,3.2f,z+side*4.4f,1,9,1);
        }
    }
    private void mooredJunk(float z) {
        float x=r*.78f, length=r*.18f;
        box("moored-hull",wood,x,3,z,length,4,8);
        box("moored-deck",trim,x,5,z,length*.90f,1,7);
        box("moored-cabin",wood,x-length*.27f,8,z,7,5,6);
        box("moored-mast",wood,x,15,z,.75f,25,.75f);
        for(int batten=0;batten<7;batten++) {
            float width=13-batten*.8f,y=11+batten*2.2f;
            box("moored-sail",sand,x+.5f,y,z,.25f,2.1f,width);
            box("moored-batten",wood,x+.7f,y-1,z,.4f,.3f,width);
        }
    }
    private void bridge(float x,float z,boolean alongX) {
        for(int i=-3;i<=3;i++) {
            float offset=i*3.1f,y=7+(3-Math.abs(i))*1.4f;
            box("arched-bridge",stone,x+(alongX?offset:0),y,z+(alongX?0:offset),alongX?3.4f:11,2,alongX?11:3.4f);
            for(int side:new int[]{-1,1}) box("bridge-parapet",trim,x+(alongX?offset:side*5),y+2,z+(alongX?side*5:offset),alongX?3.4f:.8f,2,alongX?.8f:3.4f);
        }
    }
    private void tower(Material wall,Material tile,float x,float z,int levels,boolean temple) {
        for(int level=0;level<levels;level++) {
            float w=r*(.18f-level*.022f),y=5+level*13;
            box("temple-storey",wall,x,y+6,z,w*.75f,12,w*.75f);
            roof(tile,x,y+11,z,w,w);
        }
        if(temple) {
            MeshPartBuilder p=part("temple-spire",trim);
            p.setVertexTransform(new Matrix4().setToTranslation(x,5+levels*13+9,z));
            p.cone(8,24,8,8);
        }
    }
    private void port(int id) {
        int kind=PORT_KIND[id]; boolean tropical=kind==STILT||kind==TEMPLE||id==3||id==4;
        Material wall=mat(WALL[id]),tile=mat(ROOF[id]);
        shore(0,0,r,false,true,id);
        if(kind==KARST||kind==CLIFF||kind==STILT) {
            for(int k=0;k<4;k++) mountain(-r*.48f+(k%2)*r*.12f,(k-1.5f)*r*.29f,r*.19f,r*.19f,
                    r*(kind==KARST?.64f:kind==CLIFF?.53f:.35f)+(k%2)*r*.16f,kind==KARST||kind==CLIFF);
        } else {
            for(int k=0;k<3;k++) mountain(-r*.59f,(k-1)*r*.33f,r*.17f,r*.21f,r*(.17f+k*.04f),false);
        }
        // Streets and blocks leave the central cross-canal / market square legible.
        for(int row=-2;row<=2;row++) for(int col=0;col<4;col++) {
            if(id==5 && Math.abs(row)>1) continue; // Hepu's open working quay.
            if(kind==TEMPLE && col<2 && Math.abs(row)<2) continue;
            float x=r*(-.28f+col*.235f),z=row*r*.255f;
            if(col==3 && Math.abs(row)==2) continue;
            if(kind==CANAL && row==0) continue;
            house(wall,tile,x,z,r*(.14f+random.nextFloat()*.025f),10+random.nextFloat()*8,kind==STILT);
        }
        if(kind==CANAL) {
            box("canal-water",water,0,4.15f,0,r*1.3f,.2f,17);
            box("cross-canal",water,r*.10f,4.2f,0,14,.2f,r*1.25f);
            bridge(-r*.16f,0,false); bridge(r*.40f,0,false); bridge(r*.10f,r*.35f,true);
        }
        if(kind==TEMPLE) {
            if(id==19) { // Java: stepped stone stupa, as in the Borobudur reference.
                for(int step=0;step<5;step++) box("stupa-terrace",stone,-r*.21f,5+step*5,0,65-step*10,5,65-step*10);
                tower(stone,tile,-r*.21f,0,4,true);
            } else {
                tower(wall,tile,-r*.20f,0,id==7?4:3,true);
                tower(wall,tile,-r*.18f,-r*.28f,2,true); tower(wall,tile,-r*.18f,r*.28f,2,true);
            }
        } else {
            tower(wall,tile,-r*.27f,-r*.38f,id==12?3:2,false);
        }
        if(id==20||id==13||id==12) { // Courtyard walls and gate: Yangzhou / Qinzhou / Mingzhou.
            for(int side:new int[]{-1,1}) box("city-wall",stone,0,8,side*r*.66f,r*.84f,9,3);
            box("gate-wing",stone,r*.47f,8,-r*.49f,5,10,r*.19f);
            house(wall,tile,r*.46f,-r*.32f,r*.18f,16,false);
        }
        for(int p=-1;p<=1;p++) pier(p*r*.30f,r*(p==0?.34f:.25f));
        mooredJunk(-r*.13f); mooredJunk(r*.13f);
        for(int t=0;t<18;t++) {
            float a=(t+1)*MathUtils.PI2/20,rad=r*.73f;
            if(MathUtils.cos(a)>.60f) continue;
            tree(MathUtils.cos(a)*rad,MathUtils.sin(a)*rad,12+random.nextFloat()*7,tropical);
        }
        for(int c=0;c<7;c++) { // Cargo stacked along the working waterfront.
            float z=(c-3)*r*.12f;
            box("quay-crates",wood,r*.55f,7,z,5+(c%2)*3,5,6);
            box("quay-sacks",sand,r*.51f,6,z+5,4,3,4);
        }
    }
    private void island(int id) {
        int kind=ISLAND_KIND[id];
        if(kind==1) {
            shore(0,0,r,true,false,id);
            for(int i=1;i<=8;i++) {
                float a=i*39f; tree(MathUtils.cosDeg(a)*r*.79f,MathUtils.sinDeg(a)*r*.79f,12,true);
            }
        } else if(kind==2||kind==3) {
            for(int i=0;i<5;i++) {
                float a=i*72f,x=MathUtils.cosDeg(a)*r*.53f,z=MathUtils.sinDeg(a)*r*.53f;
                shore(x,z,r*.29f,false,false,id+i);
                if(kind==3) mountain(x,z,r*.17f,r*.17f,22+i*6,false);
                tree(x+8,z-5,12,true);
            }
        } else {
            shore(0,0,r,false,true,id);
            int count=kind==4?6:kind==6?3:5;
            for(int k=0;k<count;k++) {
                float a=k*360f/count,x=MathUtils.cosDeg(a)*r*.43f,z=MathUtils.sinDeg(a)*r*.43f;
                float height=kind==4?48+(k*5%7)*9:kind==6?12+k*4:30+k*7;
                if(kind==4) { x=r*(-.30f+(k%2)*.32f); z=(k-2.5f)*r*.23f; }
                mountain(x,z,r*(kind==4?.30f:.23f),r*.26f,height,kind==4||kind==5);
            }
            if(kind==5) { // Weizhou: a volcanic bowl with exposed dark coastal rock.
                shore(0,0,r*.35f,true,false,id);
                for(int k=0;k<8;k++) mountain(MathUtils.cosDeg(k*45)*r*.78f,MathUtils.sinDeg(k*45)*r*.78f,r*.10f,r*.10f,12,false);
            }
            for(int t=0;t<22;t++) {
                float a=t*137.5f,rad=r*(.27f+.43f*(t%5)/5f);
                tree(MathUtils.cosDeg(a)*rad,MathUtils.sinDeg(a)*rad,11+(t%4)*2,id==1||id==8||id==9||t%4==0);
            }
            if(kind!=4) {
                pier(0,r*.27f);
                house(mat(0xc6a87b),mat(0x8c663f),r*.35f,r*.20f,14,8,true);
            }
        }
    }
}
