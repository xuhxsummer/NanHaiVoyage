package com.shipgame.nanhai.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.shipgame.nanhai.data.*;

/** Voyage-only visual layer; world coordinates, collision radii and navigation stay unchanged. */
public final class VoyageSceneArt implements Disposable {
    private final Texture atlas,water,foam;
    private final TextureRegion harbor,ship;
    public VoyageSceneArt(){
        Pixmap source=new Pixmap(Gdx.files.internal("textures/voyage-hud/harbor-junk-atlas.png"));
        // The source may be RGB: alpha removal requires an explicit RGBA canvas.
        Pixmap art=new Pixmap(source.getWidth(),source.getHeight(),Pixmap.Format.RGBA8888);
        art.setBlending(Pixmap.Blending.None);art.drawPixmap(source,0,0);source.dispose();
        // The generator supplied a neutral preview matte. Remove only bright neutral
        // pixels connected to its border; enclosed sail highlights remain intact.
        removeMatte(art);
        atlas=new Texture(art);art.dispose();atlas.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);
        // Individual sprite bounds, retaining generated alpha and a transparent gutter.
        harbor=new TextureRegion(atlas,112,128,768,784);ship=new TextureRegion(atlas,976,64,512,864);
        Pixmap p=new Pixmap(512,512,Pixmap.Format.RGBA8888);
        for(int y=0;y<512;y++)for(int x=0;x<512;x++){float wave=MathUtils.sin(x*MathUtils.PI2/512+y*MathUtils.PI2/128)*.012f+MathUtils.sin(y*MathUtils.PI2/256)*.018f;
            p.setColor(.065f+wave,.235f+wave,.31f+wave,1);p.drawPixel(x,y);}
        for(int i=0;i<160;i++){int x=(i*137)%512,y=(i*73)%512,w=8+i%28;p.setColor(.34f,.58f,.61f,.15f+(i%4)*.05f);p.drawLine(x,y,Math.min(511,x+w),y);p.drawLine(x+3,y+1,Math.min(511,x+w-4),y+1);}
        Pixmap dot=new Pixmap(1,1,Pixmap.Format.RGBA8888);dot.setColor(Color.WHITE);dot.fill();foam=new Texture(dot);dot.dispose();
        water=new Texture(p);p.dispose();water.setWrap(Texture.TextureWrap.Repeat,Texture.TextureWrap.Repeat);water.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);
    }
    private static void removeMatte(Pixmap p) {
        int w=p.getWidth(),h=p.getHeight();boolean[] seen=new boolean[w*h];int[] queue=new int[w*h];int head=0,tail=0;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(x==0||y==0||x==w-1||y==h-1){int k=y*w+x;if(!seen[k]&&matte(p.getPixel(x,y))){seen[k]=true;queue[tail++]=k;}}
        p.setBlending(Pixmap.Blending.None);
        int[] neighbors={-1,1,-w,w};
        while(head<tail){int k=queue[head++],x=k%w,y=k/w;p.drawPixel(x,y,0);
            for(int d:neighbors){int n=k+d;if(n<0||n>=w*h||seen[n]||Math.abs(n%w-x)>1)continue;
                if(matte(p.getPixel(n%w,n/w))){seen[n]=true;queue[tail++]=n;}}
        }
    }
    private static boolean matte(int rgba){int r=rgba>>>24,g=(rgba>>>16)&255,b=(rgba>>>8)&255;return Math.min(r,Math.min(g,b))>218&&Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))<15;}
    public void sea(SpriteBatch b,GameState g,float time){
        float x=g.x-800,y=g.y-480;b.setColor(Color.WHITE);b.draw(water,x,y,1600,960,x/384+time*.002f,y/384,(x+1600)/384+time*.002f,(y+960)/384);
    }
    public void ports(SpriteBatch b,GameState g){
        for(int i=0;i<Catalog.PORTS.length;i++){float x=Catalog.PORT_X[i],y=Catalog.PORT_Y[i];if(Math.abs(x-g.x)>900||Math.abs(y-g.y)>600)continue;
            b.draw(harbor,x-230,y-56,260,266);}
    }
    public void ship(SpriteBatch b,GameState g){
        // Heading-driven foam trails sit underneath the junk and vanish at rest.
        float strength=MathUtils.clamp(g.speed/40f,0,1);
        float ux=MathUtils.cosDeg(g.headingDeg),uy=MathUtils.sinDeg(g.headingDeg);
        for(int i=0;i<18;i++) {
            float distance=48+i*5,spread=9+i*.8f;
            b.setColor(.68f,.87f,.85f,strength*(1-i/18f)*.35f);
            for(int side:new int[]{-1,1}) {
                float x=g.x-ux*distance-uy*spread*side,y=g.y-uy*distance+ux*spread*side;
                b.draw(foam,x,y,0,1,7+i*.35f,1.4f,1,1,g.headingDeg,0,0,1,1,false,false);
            }
        }
        b.setColor(Color.WHITE);
        b.draw(ship,g.x-42,g.y-71,42,71,84,142,1,1,g.headingDeg+90);
    }
    @Override public void dispose(){atlas.dispose();water.dispose();foam.dispose();}
}
