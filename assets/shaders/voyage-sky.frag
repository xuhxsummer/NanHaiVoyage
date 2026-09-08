#ifdef GL_ES
precision highp float;
#endif
varying vec3 v_ray;
// SKY_FUNCTIONS
void main() { gl_FragColor=vec4(pow(environment(v_ray),vec3(0.454545)),1.0); }
