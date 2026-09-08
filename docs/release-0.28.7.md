# 0.28.7 main-voyage ocean

The default voyage now renders a single continuous displaced water surface with
eight Gerstner swell/chop components, analytic surface normals and crest compression.
Three scrolling normal-map layers add detail at different scales and directions.
Shore proximity controls absorption/color and wave damping; open water fades from
deep teal toward the sky at the horizon. Rendering-only ship height follows the swell.

The surface uses Schlick water Fresnel, a GGX sun highlight, reflected sky imagery,
crest-triggered wispy whitecaps, shoreline foam, and speed-scaled curling wake arms
with bubbly central churn. Foam is shaded into the water: no intersecting water
sheets or coplanar wake polygons. Camera position, game coordinates, collisions,
combat, HUD, updater and save formats are unchanged.

## Assets

`tools/bake_water_assets.py` (Pillow + NumPy) reproduces the committed runtime maps:

- `ocean_normal_choppy_01.png`, `ocean_normal_swell_01.png`: 1024² tangent RGB normals,
  height in alpha, baked from supplied JPG heights with periodic boundary correction.
- `ocean_foam_masks.png`: 1024², bubbly mask in R and wispy mask in G.
- `ocean_sky_environment.png`: 2048×1024 environment built from the valid sky plate,
  with the photographed sea removed, continuous azimuth and a blended zenith.

All live in `android/assets/textures/water/`. The file named
`sky_golden_horizon.jpg` actually contains grayscale wave heights; it is retained
as supplied and is not sampled as sky. No additional user assets are required.

The updated brief also requested the nine Gemini ship icons in this same release.
They are loaded by Catalog name in the existing shop, with the old atlas retained
as fallback. No shop economy or layout redesign was introduced.

`prepareWaterAssets` packages only runtime water PNGs and the nine ship PNGs into
Android and desktop resources. Unrelated beast/herb imports are not included.

## Quality and validation

High quality is the default: 100,352 water triangles, eight wave components, three
normal samples. `VoyageWorldRenderer.setWaterQuality(false)` selects 18,432
triangles, four components, two normal samples and shorter whitecap visibility.
Sky/wake/Fresnel remain active. Shader compilation or asset-load failure uses the
existing simple water fallback. Textures, meshes and shaders use libGDX managed
resource reloads for Android context loss.

Passed: high/low/sky GLSL ES 1.00 compile/link; real OpenGL water animation, wake,
resource reload and fallback tests; full voyage framing, combat, docking, resize,
all ship variants and mesh-bound smoke tests; 3,645 collision regression cases;
max-account save/reload/isolation checks; nine shop icons/grid/details; Android
assembleDebug. Rendered screenshots were inspected, including the actual HUD view.

Android hardware FPS has not been measured. This remains a real-time approximation:
shore depth is estimated from land envelopes, sky reflections are image-based
(not HDR capture, planar scene reflections or SSR), and foam/buoyancy are visual
effects rather than a fluid simulation. Ports and ships retain their existing art.
