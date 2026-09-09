#ifdef GL_ES
precision mediump float;
#endif
varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform sampler2D u_mask;
uniform float u_phase;
void main() {
    vec2 uv = v_texCoords;
    // libGDX decodes the single-channel PNG as Pixmap.Format.Alpha.
    float water = texture2D(u_mask, uv).a;
    // Waves broaden toward the foreground. Everything outside the soft mask is still.
    float depth = smoothstep(0.63, 0.89, uv.y);
    float swell = sin(uv.y * 155.0 + uv.x * 19.0 - u_phase * 2.0);
    float ripple = sin(uv.y * 290.0 - uv.x * 37.0 + u_phase * 3.0);
    vec2 drift = vec2(swell * 0.008 + ripple * 0.0035,
                      ripple * 0.0028) * (0.45 + depth * 0.55) * water;
    // A second mask sample keeps hull, shoreline and foreground props out of the distortion.
    float interior = water * texture2D(u_mask, uv + drift).a;
    vec4 still = texture2D(u_texture, uv);
    vec4 moving = texture2D(u_texture, uv + drift);
    moving.rgb *= 1.0 + 0.12 * swell + 0.055 * ripple;
    gl_FragColor = v_color * mix(still, moving, interior);
}
