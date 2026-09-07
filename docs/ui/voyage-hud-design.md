# Main voyage HUD

Implemented against `assets/ui-refs/10-voyage-hud.png`, after inspecting the old device screenshot `10-voyage-hud-current.jpg` and both task specifications. Scope is the voyage scene and its HUD; existing popup entry points remain connected.

## Layout and appearance

The Scene2D HUD uses a 1920 × 1080 design canvas with navy panels, gilt sliced frames, parchment quest cards, jade acceleration and cinnabar deceleration buttons. The four live resource values have Chinese captions beneath them. Seven circular shortcuts, compass minimap with north and coordinates, weather chip, two quest trackers, rudder joystick, automatic navigation, clock, port plaque and system-message bar follow the reference's hierarchy. A thin speed indicator reflects speed against the catalog's nominal maximum.

`VoyageHud.layoutViewport` preserves circular proportions and anchors the right controls to the extended viewport. Additional width stays in the sea between the left resources and right shortcuts. Steering hit coordinates use the same scale as the visible joystick. Decorative actors do not intercept navigation touches; the location plaque explicitly accepts taps.

The existing harbor/junk atlas supplies detailed Chinese pavilions and sails. It is copied into RGBA before border-connected matte removal, avoiding black rectangles from RGB input. The sea is a repeating procedural texture; moving ships have heading-driven foam trails. Original port tiles are suppressed only in this voyage draw pass. All presentation changes retain world coordinates, navigation, collision and combat rules.

## Data and interactions

- Resources, debt, cargo, wind, speed, crew, day/time and docking state come from `GameState`.
- 货舱 / 图鉴 / 船坞 / 商行 / 任务 open their existing panels. 商行 opens the market when docked, otherwise the existing port context. 活动 / 福利 display Chinese availability messages.
- 世界 and the minimap open the existing world map. 自动航行 opens destination selection or cancels active navigation. Existing cancellation and pirate-lock controls remain available.
- Quest cards display the current and next real sequential quests, with live progress and reward readiness. The next quest is clearly marked as awaiting the previous reward. The mock's East Asia main/side quest text is not fabricated as playable content.
- The plaque displays the actual nearby/docked port, including 苏禄港, and a docking/action subtitle. There is no ownership field in the current game, so the reference's `占有度 60%` is not presented as a fictitious live statistic.
- Weather is 晴 because the current weather model only supports CLEAR.

## Files and assets

- `core/src/main/java/com/shipgame/nanhai/ui/VoyageHud.java`: layout, controls and live display.
- `VoyageHudChrome.java`: HUD-owned 28px CJK font, procedural sliceable frames, compass discs and gold icons. No shared popup skin mutation.
- `VoyageMinimap.java`: circular chart mesh, islands, port marks and heading arrow.
- `VoyageSceneArt.java`: sea, harbor, junk, transparency handling and wake.
- `screen/VoyageScreen.java`: gameplay callbacks, responsive steering and render integration.
- `PixelMapRenderer.java`: optional omission of old port markers; existing callers retain the previous behavior.
- `assets/textures/voyage-hud/harbor-junk-atlas.png`: existing resumed-work atlas; no additional generated artwork.
- `assets/fonts/nanhai-cjk.ttf` and `ui-chars.txt`: Noto Sans CJK SC subset with previous glyph coverage retained and 您 added for the system toast.

`RotatingGoldBorder.java` remains attached to both quest cards, preserving FreeBuff's continuous clockwise gold sweep. The border actors are touch-disabled, retain the original shader, and are disposed with the HUD. This explicitly requested behavior takes precedence over the earlier task file's preference for static rendering.

## Preserved FreeBuff fixes

- Collision and automatic land avoidance in `GameState` are unchanged.
- Decorative trade-route polylines remain removed. The existing live auto-sail destination indicator is retained.
- Both quest-card buttons and the task shortcut keep their original gameplay callbacks; the animated border does not intercept taps.
- Concentric compass/joystick rings and world-map player rings remain present.
- The rotating gold border is restored on both quest cards.
- World and HUD cameras continue using `ExtendViewport`; safe-edge HUD layout keeps circular controls undistorted and aligns joystick input.
- Restored the world-map filled-shape `begin()` that had been removed alongside decorative routes. This allows map markers and rings to render without reintroducing route polylines.

## Validation

- `./gradlew :core:compileJava`: passed.
- `python3 tools/check_ui_font.py`: passed, including 您.
- Desktop GL preview at 1920 × 1080 and 2400 × 1080: checked transparency, text, chart and control bounds.
- Temporary desktop input harness: passed shortcut/panel navigation, both quest cards, location plaque, automatic-navigation entry, activity/benefit messages, acceleration/deceleration holds, joystick undock/release and wide-screen bounds. Runs used an isolated `/tmp` save directory.
- FreeBuff regression checks: both gold-border shaders compiled and advanced their animation clocks; touch-disabled borders preserved quest taps. Actual map frames rendered successfully, collision stopped an inward-moving ship at the existing boundary, avoidance deflected around blocking land and left a clear lane unchanged, and both cameras remained `ExtendViewport`.
- Desktop shutdown emitted pre-existing `Pixmap already disposed` warnings; no input assertions or rendering checks failed.

The composition approaches the reference's HUD arrangement and palette. The scene remains a top-down navigable world with layered sprites rather than the mock's painted horizon, mountains and sky. Device verification is still appropriate before release. No Android packaging, release or commit was performed; unrelated pre-existing working-tree edits were preserved.
