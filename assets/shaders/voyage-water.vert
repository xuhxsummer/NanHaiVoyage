#ifdef GL_ES
precision highp float;
#endif
attribute vec3 a_position;
uniform mat4 u_projView;
uniform vec2 u_origin;
uniform float u_time;
uniform float u_wind;
uniform vec3 u_shores[8];
varying vec3 v_surface;
varying vec3 v_normal;
varying vec2 v_uv;
varying float v_shore;
varying float v_crest;

// Analytic height derivatives keep lighting attached to moving wave volume.
void wave(vec2 p, vec2 direction, float frequency, float amplitude, float rate,
          inout float height, inout vec2 slope) {
    float phase = dot(p, direction) * frequency + u_time * rate;
    height += sin(phase) * amplitude;
    slope += direction * (cos(phase) * amplitude * frequency);
}
void main() {
    vec2 p = a_position.xz + u_origin;
    float shore = 1000.0;
    for (int i = 0; i < 8; i++) {
        shore = min(shore, length(p - u_shores[i].xy) - u_shores[i].z);
    }
    float damping = smoothstep(0.0, 42.0, shore);
    // Coarser outer grid smoothly loses displacement; fine normal detail remains.
    float detail = 1.0 - smoothstep(500.0, 1800.0, length(a_position.xz));
    float height = 0.0;
    vec2 slope = vec2(0.0);
    wave(p, vec2(0.94, 0.34), 0.065, 1.10, -1.3, height, slope);
    wave(p, vec2(-0.38, 0.92), 0.112, 0.56, -1.85, height, slope);
    wave(p, vec2(0.65, -0.76), 0.23, 0.23, 2.6, height, slope);
#ifndef LOW_QUALITY
    wave(p, vec2(0.22, 0.98), 0.37, 0.12, -3.3, height, slope);
#endif
    float strength = (0.62 + u_wind * 0.6) * damping;
    v_crest = height * strength;
    v_normal = normalize(vec3(-slope.x * strength * detail, 1.0, -slope.y * strength * detail));
    v_surface = vec3(a_position.x, height * strength * detail - 0.45, a_position.z);
    v_uv = p / 72.0;
    v_shore = shore;
    gl_Position = u_projView * vec4(v_surface + vec3(u_origin.x, 0.0, u_origin.y), 1.0);
}
