#ifdef GL_ES
precision highp float;
#endif
attribute vec3 a_position;
uniform vec3 u_forward,u_right,u_up;
varying vec3 v_ray;
void main() {
    v_ray=u_forward+a_position.x*u_right+a_position.y*u_up;
    gl_Position=vec4(a_position.xy,1.0,1.0);
}
