package com.shipgame.nanhai.ui;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Disposable;
import com.shipgame.nanhai.data.*;

/** A true circular mesh samples an unmasked chart; tracking never stretches at world edges. */
public final class VoyageMinimap extends Actor implements Disposable {
    private final Texture chart, pixel;
    private final Drawable rim;
    private final float[] vertices=new float[20];
    private GameState g;
    private static final float HALF=640;
    public VoyageMinimap(VoyageHudChrome ui){
        rim=ui.compass;
        Pixmap p=new Pixmap(1024,768,Pixmap.Format.RGBA8888);
        p.setColor(Color.valueOf("153E50"));p.fill();
        p.setColor(Color.valueOf("214D59"));for(int y=0;y<768;y+=32)p.drawLine(0,y,1023,y);for(int x=0;x<1024;x+=32)p.drawLine(x,0,x,767);
        // Coast outline follows the existing chart's geographic bands, in world coordinates.
        float[][] land={{0,2350,4800,3600},{0,700,1500,2350},{0,500,1150,800},{2150,1300,2550,2050}};
        for(float[] r:land){int x=(int)(r[0]/Catalog.WORLD_W*1024),y=(int)((1-r[3]/Catalog.WORLD_H)*768),w=(int)((r[2]-r[0])/Catalog.WORLD_W*1024),h=(int)((r[3]-r[1])/Catalog.WORLD_H*768);p.setColor(Color.valueOf("817B52"));p.fillRectangle(x,y,w,h);p.setColor(Color.valueOf("ADB07B"));p.drawRectangle(x,y,w,h);}
        for(int i=0;i<Catalog.ISLANDS.length;i++){p.setColor(Color.valueOf("859567"));p.fillCircle((int)(Catalog.ISLAND_X[i]/Catalog.WORLD_W*1024),(int)((1-Catalog.ISLAND_Y[i]/Catalog.WORLD_H)*768),5);}
        // Small southern port islands keep the sailing region recognizable,
        // including 苏禄 and 吕宋, rather than presenting an empty blue disc.
        for(int i:new int[]{16,17,18,19}) {
            int x=(int)(Catalog.PORT_X[i]/Catalog.WORLD_W*1024);
            int y=(int)((1-Catalog.PORT_Y[i]/Catalog.WORLD_H)*768);
            p.setColor(Color.valueOf("55786A"));p.fillCircle(x-9,y-3,12);p.fillCircle(x-12,y-14,9);
            p.setColor(Color.valueOf("A7A272"));p.fillCircle(x-10,y-4,9);p.fillCircle(x-12,y-14,6);
        }
        chart=new Texture(p);p.dispose();chart.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);
        p=new Pixmap(1,1,Pixmap.Format.RGBA8888);p.setColor(Color.WHITE);p.fill();pixel=new Texture(p);p.dispose();
    }
    public void update(GameState state){g=state;}
    private void quad(Batch batch,Texture texture,float cx,float cy,float x1,float y1,float x2,float y2,float u,float v,float u1,float v1,float u2,float v2,float color){
        float[] a={cx,cy,color,u,v,x1,y1,color,u1,v1,x2,y2,color,u2,v2,x2,y2,color,u2,v2};
        System.arraycopy(a,0,vertices,0,20);batch.draw(texture,vertices,0,20);
    }
    @Override public void draw(Batch batch,float alpha){
        if(g==null)return;
        float cx=getX()+getWidth()/2,cy=getY()+getHeight()/2,r=getWidth()*.445f;
        float wx=MathUtils.clamp(g.x,HALF,Catalog.WORLD_W-HALF),wy=MathUtils.clamp(g.y,HALF,Catalog.WORLD_H-HALF);
        float white=Color.WHITE.toFloatBits();
        // Draw the frame before chart so its opaque disc cannot obscure the map.
        rim.draw(batch,getX(),getY(),getWidth(),getHeight());
        for(int i=0;i<96;i++){float a=i*MathUtils.PI2/96,b=(i+1)*MathUtils.PI2/96;
            float ax=MathUtils.cos(a),ay=MathUtils.sin(a),bx=MathUtils.cos(b),by=MathUtils.sin(b);
            quad(batch,chart,cx,cy,cx+ax*r,cy+ay*r,cx+bx*r,cy+by*r,wx/Catalog.WORLD_W,1-wy/Catalog.WORLD_H,
                    (wx+ax*HALF)/Catalog.WORLD_W,1-(wy+ay*HALF)/Catalog.WORLD_H,(wx+bx*HALF)/Catalog.WORLD_W,1-(wy+by*HALF)/Catalog.WORLD_H,white);
        }
        float scale=r/HALF;
        batch.setColor(VoyageHudChrome.GOLD);
        for(int i=0;i<Catalog.PORTS.length;i++){float dx=(Catalog.PORT_X[i]-wx)*scale,dy=(Catalog.PORT_Y[i]-wy)*scale;if(dx*dx+dy*dy<(r-10)*(r-10)){batch.draw(pixel,cx+dx-5,cy+dy-1,10,2);batch.draw(pixel,cx+dx-1,cy+dy-5,2,10);}}
        batch.setColor(Color.WHITE);
        float sx=cx+(g.x-wx)*scale,sy=cy+(g.y-wy)*scale,a=g.headingDeg*MathUtils.degreesToRadians;
        quad(batch,pixel,sx+MathUtils.cos(a)*13,sy+MathUtils.sin(a)*13,sx+MathUtils.cos(a+2.5f)*10,sy+MathUtils.sin(a+2.5f)*10,sx+MathUtils.cos(a-2.5f)*10,sy+MathUtils.sin(a-2.5f)*10,0,0,0,0,0,0,Color.valueOf("A5D69A").toFloatBits());
    }
    @Override public void dispose(){chart.dispose();pixel.dispose();}
}
