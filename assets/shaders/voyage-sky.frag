#ifdef GL_ES
precision highp float;
#endif
varying vec3 v_ray;
uniform float u_night;
// SKY_FUNCTIONS
void main() {
    vec3 sky=pow(environment(v_ray),vec3(0.454545));
    vec3 night=vec3(0.05,0.08,0.16);
    // 0.28.26 昼夜天空：夜间整体压暗，保留少量冷色。u_night 0=白天 1=深夜。
    gl_FragColor=vec4(mix(sky,night,u_night*0.82),1.0);
}
