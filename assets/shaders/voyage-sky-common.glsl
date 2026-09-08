uniform sampler2D u_skyMap;
uniform vec3 u_sun;
vec3 environment(vec3 direction) {
    vec3 ray=normalize(direction);
    vec2 uv=vec2(atan(ray.z,ray.x)*0.15915494+0.5,acos(clamp(ray.y,-1.0,1.0))*0.31830989);
    vec3 sky=pow(texture2D(u_skyMap,uv).rgb,vec3(2.2));
    float sun=dot(ray,u_sun);
    sky+=vec3(8.0,6.9,4.8)*smoothstep(0.99985,0.99998,sun);
    sky+=vec3(0.16,0.12,0.065)*pow(max(sun,0.0),28.0);
    return sky;
}
