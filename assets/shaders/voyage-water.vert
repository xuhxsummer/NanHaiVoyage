#ifdef GL_ES
precision highp float;
#endif
attribute vec3 a_position;
uniform mat4 u_projView;
uniform vec2 u_origin;
uniform float u_time, u_wind;
uniform vec4 u_waves[8];
uniform vec3 u_shores[8];
varying vec3 v_surface, v_normal;
varying vec2 v_uv, v_breaking;
varying float v_shore;
void main() {
    vec2 p=a_position.xz+u_origin;
    float shore=1000.0;
    for(int i=0;i<8;i++) shore=min(shore,length(p-u_shores[i].xy)-u_shores[i].z);
    float damping=smoothstep(0.0,38.0,shore);
    float detail=1.0-smoothstep(600.0,2600.0,length(a_position.xz));
    float strength=(0.65+u_wind*0.6)*damping;
    vec3 displacement=vec3(0.0);
    vec3 tx=vec3(1.0,0.0,0.0),tz=vec3(0.0,0.0,1.0);
    // Gerstner horizontal compression: sharper crests, rounded troughs.
#ifdef LOW_QUALITY
    for(int i=0;i<4;i++) {
#else
    for(int i=0;i<8;i++) {
#endif
        vec4 wave=u_waves[i];
        vec2 d=wave.xy;
        float k=wave.z,a=wave.w*strength,q=0.68;
        // Do not sample small geometry waves on the sparse outer grid (normal maps carry that detail).
        a*=1.0-smoothstep(24.0/k,100.0/k,length(a_position.xz));
        float phase=dot(p,d)*k-u_time*sqrt(9.81*k);
        float s=sin(phase),c=cos(phase);
        displacement+=vec3(q*a*d.x*c,a*s,q*a*d.y*c);
        tx+=vec3(-q*a*k*d.x*d.x*s,a*k*d.x*c,-q*a*k*d.x*d.y*s)*detail;
        tz+=vec3(-q*a*k*d.x*d.y*s,a*k*d.y*c,-q*a*k*d.y*d.y*s)*detail;
    }
    v_surface=vec3(a_position.x,-0.45,a_position.z)+displacement*detail;
    v_normal=normalize(cross(tz,tx));
    v_uv=p/180.0;
    v_shore=shore;
    v_breaking=vec2(displacement.y,tx.x*tz.z-tx.z*tz.x);
    gl_Position=u_projView*vec4(v_surface+vec3(u_origin.x,0.0,u_origin.y),1.0);
}
