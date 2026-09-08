#ifdef GL_ES
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
#endif
uniform sampler2D u_detail;
uniform vec3 u_camera;
uniform vec3 u_sky;
uniform vec2 u_ship;
uniform vec2 u_forward;
uniform vec2 u_hull;
uniform float u_speed;
uniform float u_time;
uniform float u_wind;
varying vec3 v_surface;
varying vec3 v_normal;
varying vec2 v_uv;
varying float v_shore;
varying float v_crest;

void main() {
    vec3 view = normalize(u_camera - v_surface);
    float distanceToEye = length(u_camera - v_surface);
    float closeDetail = 1.0 - smoothstep(180.0, 1400.0, distanceToEye);
    vec2 uv = v_uv + vec2(-0.027, 0.013) * u_time;
    vec4 tex = texture2D(u_detail, uv);
    vec2 ripple = tex.rg * 2.0 - 1.0;
#ifndef LOW_QUALITY
    vec4 small = texture2D(u_detail, v_uv.yx * 2.73 + vec2(0.021, -0.041) * u_time);
    ripple += (small.rg * 2.0 - 1.0) * 0.46;
#endif
    vec3 n = normalize(v_normal + vec3(ripple.x, 0.0, ripple.y) * (0.24 + u_wind * 0.13) * closeDetail);
    float facing = clamp(dot(n, view), 0.0, 1.0);
    float fresnel = 0.045 + 0.955 * pow(1.0 - facing, 5.0);
    float shallow = 1.0 - smoothstep(2.0, 90.0, v_shore);
    vec3 deep = mix(vec3(0.022, 0.115, 0.19), vec3(0.025, 0.235, 0.28), closeDetail * 0.65);
    vec3 water = mix(deep, vec3(0.09, 0.48, 0.43), shallow * 0.8);
    water *= 0.82 + 0.22 * n.y + v_crest * 0.06;
    vec3 reflection = reflect(-view, n);
    vec3 sky = mix(u_sky, vec3(0.18, 0.38, 0.60), clamp(reflection.y, 0.0, 1.0));
    vec3 color = mix(water, sky, fresnel * 0.85);
    vec3 sun = normalize(vec3(0.48, 0.63, 0.61));
    vec3 halfVector = normalize(view + sun);
    float sunDot = max(dot(n, halfVector), 0.0);
    float glint = pow(sunDot, 110.0) * 1.5 + pow(sunDot, 18.0) * 0.13;
    color += vec3(1.0, 0.88, 0.64) * glint * (0.35 + fresnel);

    float cap = smoothstep(0.60, 1.30, v_crest) * smoothstep(0.53, 0.76, tex.b);
    cap *= 0.25 + u_wind * 0.6;
#ifdef LOW_QUALITY
    cap *= 1.0 - smoothstep(100.0, 360.0, distanceToEye);
#else
    cap *= 1.0 - smoothstep(500.0, 1600.0, distanceToEye);
#endif
    float shoreFoam = (1.0 - smoothstep(1.0, 5.0, abs(v_shore - 2.0 + sin(u_time * 1.8) * 1.3)))
                     * smoothstep(0.4, 0.72, tex.b) * 0.48;

    // Continuous curling wake on the displaced surface, with no coplanar foam sheets.
    vec2 relative = v_surface.xz - u_ship;
    float aft = -dot(relative, u_forward) - u_hull.x;
    float side = abs(dot(relative, vec2(-u_forward.y, u_forward.x)));
    float wakeLength = 35.0 + u_speed * 2.5;
    float progress = clamp(aft / wakeLength, 0.0, 1.0);
    float wakeNoise = texture2D(u_detail, vec2(aft * 0.027 - u_time * 0.13, side * 0.065)).b;
    float curl = sin(aft * 0.16 - u_time * 2.8) * (0.7 + progress * 4.0) + (wakeNoise - 0.5) * 2.2;
    float edge = u_hull.y + max(aft, 0.0) * 0.18 + curl;
    float width = 1.4 + progress * 4.5;
    float vWake = 1.0 - smoothstep(width * 0.25, width, abs(side - edge));
    float churn = (1.0 - smoothstep(u_hull.y * 0.3, u_hull.y + max(aft, 0.0) * 0.035, side)) * (1.0 - progress) * 0.6;
    churn *= smoothstep(0.30, 0.64, wakeNoise);
    float wake = max(vWake, churn) * smoothstep(-4.0, 7.0, aft)
                 * (1.0 - smoothstep(0.42, 1.0, progress)) * smoothstep(1.0, 30.0, u_speed);
    wake *= 0.25 + smoothstep(0.25, 0.65, wakeNoise) * 0.9;
    float foam = clamp(max(max(cap, shoreFoam), wake), 0.0, 0.95);
    color = mix(color, vec3(0.87, 0.96, 0.92), foam);
    float fog = smoothstep(700.0, 5100.0, distanceToEye);
    color = mix(color, u_sky, fog);
    gl_FragColor = vec4(color, 1.0);
}
