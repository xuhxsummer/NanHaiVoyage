# 0.28.0 — 3D chase-camera voyage slice

VoyageScreen draws VoyageWorldRenderer first, then the existing Scene2D HUD and
modal screens. The full map and minimap remain 2D. AccountStore, save formats,
GameState, Catalog, and the release script are unchanged.

## Coordinates and camera

- Gameplay `(x, y)` maps to render `(x, height, -y)`; heading 0 points +X.
- Ship meshes point +X locally and rotate around +Y by the existing heading.
- Sailing distance: 155 units, camera height: 85. The camera looks toward the
  upper ship and slightly ahead, placing the hull in the lower-middle frame.
- Pirate encounter distance: 600, height: 340; FOV stays 58 degrees. Encounter
  state uses `pirateAlive`, so incoming fire pulls back even before target lock.
  Defeat, escape, or encounter clearing returns to normal chase. Camera distance,
  heading and position use exponential interpolation with a clamped render delta.
- Perspective rays select ship/land proxies. Port/island interactions additionally
  use the original gameplay distance gates. Pirate taps toggle target lock.
- Existing auto-sail reaches the destination range and stops, waiting for a tap;
  it does not automatically open the port/island menu.

## Placeholder assets and cost

No new files under `assets/` and no model loader dependencies. All new geometry is
procedural: tapered wooden hull, stern cabins/windows, rails, cannons, two masts,
canvas sail panels, subdivided ocean, wavelets/wake, port piers/buildings and
island cones. Land proxies fit within the existing collision circles. Models and
static scenery cache are built once per screen and disposed on hide/dispose.
Static land is cached to reduce draw calls; water meshes use 16-bit indices.

The reference image is composition guidance. Water, sky, ships and ports remain
simple placeholders; photoreal water and full port cities are outside this slice.

## Validation

```sh
export JAVA_HOME=/home/box/.local/jdk/current
export PATH="$JAVA_HOME/bin:$PATH"
xvfb-run -a ./gradlew lwjgl3:voyage3dSmoke
./gradlew android:assembleDebug
python3 tools/check_ui_font.py
```

The GL smoke launcher uses an in-memory voyage with no logged-in account and
writes no account save. It checks lower-middle ship framing, movement, combat
zoom and smooth restoration, projected pirate taps/toggle and cannon damage,
auto-sail arrival, minimap/full-map opening, wide-aspect resize, perspective port
docking, screen hide/show and GL errors. Screenshots go to ignored
`Builds/voyage3d-*.png`. APK assembly and signature checks validate packaging;
physical Android device performance remains to be checked on hardware.
