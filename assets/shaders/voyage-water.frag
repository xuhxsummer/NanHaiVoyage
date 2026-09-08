#ifdef GL_ES
precision highp float;
#endif
uniform sampler2D u_chop,u_swell,u_foam;
uniform vec3 u_camera;
uniform vec2 u_ship,u_forward,u_hull;
uniform float u_speed,u_time,u_wind;
varying vec3 v_surface,v_normal;
varying vec2 v_uv,v_breaking;
varying float v_shore;
// SKY_FUNCTIONS
vec2 slope(vec4 normalMap) {
    vec3 n=normalMap.rgb*2.0-1.0;
    return n.xy/max(n.z,0.32);
}
float smith(float cosine,float roughness) {
    float k=roughness*roughness*0.5;
    return cosine/(cosine*(1.0-k)+k);
}
void main() {
    vec3 view=normalize(u_camera-v_surface);
    float distanceToEye=length(u_camera-v_surface);
    float nearDetail=1.0-smoothstep(280.0,1700.0,distanceToEye);
    vec2 drift=vec2(-0.019,0.009)*u_time;
    vec4 swell=texture2D(u_swell,v_uv*0.73+drift*0.39);
    vec4 chop=texture2D(u_chop,v_uv*2.31+drift);
    vec2 slopes=slope(swell)*0.60+slope(chop)*0.38;
#ifndef LOW_QUALITY
    // Incommensurate scales and opposing flow remove an obvious repeating tile.
    vec2 rotated=vec2(v_uv.x*0.8-v_uv.y*0.6,v_uv.x*0.6+v_uv.y*0.8);
    vec4 chop2=texture2D(u_chop,rotated*5.73-vec2(0.038,0.021)*u_time);
    vec2 fine=slope(chop2);
    slopes+=vec2(fine.x*.8+fine.y*.6,-fine.x*.6+fine.y*.8)*0.16;
#endif
    // Hull-sized wavelets continue at rest; their clock never depends on input or speed.
    vec2 relative=v_surface.xz-u_ship;
    vec2 sideAxis=vec2(-u_forward.y,u_forward.x);
    float along=dot(relative,u_forward),signedSide=dot(relative,sideAxis);
    float aspect=u_hull.y/u_hull.x;
    float hullDistance=length(vec2(along*aspect,signedSide))-u_hull.y;
    float idleEnvelope=smoothstep(-0.8,1.0,hullDistance)*(1.0-smoothstep(2.0,13.0,hullDistance));
    idleEnvelope*=1.0-smoothstep(3.0,22.0,u_speed);
    float idlePhase=hullDistance*1.1-u_time*2.4+sin(along*0.09+u_time*0.7)*0.24;
    vec2 radial=normalize(u_forward*(along*aspect*aspect)+sideAxis*signedSide+vec2(0.0001));
    slopes+=radial*cos(idlePhase)*idleEnvelope*0.10;
    vec3 n=normalize(v_normal+vec3(slopes.x,0.0,slopes.y)*(0.45+u_wind*0.55)*nearDetail);
    float nv=max(dot(n,view),0.001);
    float fresnel=0.0204+0.9796*pow(1.0-nv,5.0);
    vec3 reflected=reflect(-view,n);
    vec3 sky=environment(vec3(reflected.x,max(reflected.y,0.005),reflected.z));
    // Approximate bathymetry from nearby shore envelopes; never changes navigation.
    float depth=2.0+max(v_shore,0.0)*0.17;
    vec3 transmission=exp(-vec3(0.19,0.060,0.043)*depth);
    vec3 deep=vec3(0.005,0.033,0.055);
    vec3 water=mix(deep,vec3(0.030,0.23,0.19),transmission);
    water+=vec3(0.002,0.012,0.014)*nearDetail;
    float forwardScatter=pow(max(dot(view,-u_sun),0.0),3.0);
    water+=vec3(0.002,0.025,0.026)*max(v_breaking.x,0.0)*(0.3+forwardScatter);
    vec3 color=water*(1.0-fresnel)+sky*fresnel;
    // GGX sun with Fresnel and geometry terms. Mipmaps suppress distant sparkle aliasing.
    vec3 halfway=normalize(view+u_sun);
    float nh=max(dot(n,halfway),0.0),nl=max(dot(n,u_sun),0.0);
    float roughness=mix(0.28,0.16,nearDetail);
    float a2=pow(roughness,4.0);
    float denominator=nh*nh*(a2-1.0)+1.0;
    float distribution=a2/(3.14159*denominator*denominator+0.00001);
    float fh=0.0204+0.9796*pow(1.0-max(dot(view,halfway),0.0),5.0);
    float spec=distribution*smith(nv,roughness)*smith(nl,roughness)*fh/(4.0*nv+0.001);
    color+=vec3(1.5,1.32,1.05)*spec;
    vec3 foamTex=texture2D(u_foam,v_uv*3.4+vec2(-0.024,0.011)*u_time).rgb;
    float breaking=(1.0-smoothstep(0.60,0.94,v_breaking.y))*smoothstep(0.1,1.2,v_breaking.x);
    float caps=breaking*smoothstep(0.44,0.71,chop.a)*foamTex.g*(0.35+u_wind*0.5);
#ifdef LOW_QUALITY
    caps*=1.0-smoothstep(150.0,450.0,distanceToEye);
#else
    caps*=1.0-smoothstep(800.0,2100.0,distanceToEye);
#endif
    float shoreFoam=(1.0-smoothstep(0.5,5.0,abs(v_shore-2.0+sin(u_time*1.3)*1.1)))*foamTex.g*0.65;
    // Bright V arms, eddies and bubbly central churn share the displaced surface.
    float aft=-dot(relative,u_forward)-u_hull.x;
    float side=abs(signedSide),lengthOfWake=40.0+u_speed*2.8;
    float progress=clamp(aft/lengthOfWake,0.0,1.0);
    vec2 wakeUV=vec2(aft*0.034-u_time*0.11,signedSide*0.049+sin(aft*0.035)*0.14);
    vec3 wakeTex=texture2D(u_foam,wakeUV).rgb;
    float curl=sin(aft*0.17-u_time*2.5+sign(signedSide)*1.1)*(0.8+progress*4.0);
    float edge=u_hull.y+max(aft,0.0)*0.21+curl;
    float width=2.2+sqrt(max(aft,0.0))*0.34;
    float arm=1.0-smoothstep(width*0.15,width,abs(side-edge));
    float churn=(1.0-smoothstep(u_hull.y*0.25,u_hull.y+max(aft,0.0)*0.07,side))*(1.0-progress)*0.7;
    float wake=max(arm*(0.24+wakeTex.g*1.2),churn*wakeTex.r);
    wake*=smoothstep(-5.0,4.0,aft)*(1.0-smoothstep(0.35,1.0,progress))*smoothstep(1.0,35.0,u_speed);
    float idleFoam=pow(max(sin(idlePhase),0.0),6.0)*idleEnvelope*(0.035+foamTex.g*0.055)*nearDetail;
    float foam=clamp(max(max(max(caps,shoreFoam),wake),idleFoam),0.0,0.98);
    color=mix(color,vec3(0.72,0.83,0.81),foam);
    vec3 horizon=environment(normalize(vec3(-view.x,0.015,-view.z)));
    float fog=1.0-exp(-pow(distanceToEye/4300.0,1.6));
    color=mix(color,horizon,fog);
    gl_FragColor=vec4(pow(max(color,vec3(0.0)),vec3(0.454545)),1.0);
}
