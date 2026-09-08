# 0.28.9 — 航行手感与全屏页面

One release implements both parts of the combined [brief](codex-briefs/0.28.9-voyage-feel-and-fullscreen.md).

## Voyage controls and scale

- The helm now controls relative yaw. Screen-left is negative input, and heading integration subtracts that input because map (x,y) is rendered as (x,height,-y). Left turns the bow left at rest, during undocking, and at low or cruising speed. A/D use the same convention. The helm cancels automatic navigation.
- Empty-sea dragging temporarily orbits the chase camera during manual or automatic sailing. Look and helm have separate pointer ownership; HUD touches retain priority. Release returns with a smoothstep over **0.32 seconds**. Pitch and camera height are bounded; pause, page opening, and canceled touches clear active controls.
- Stopped ships produce subtle animated hull-sized ripples with soft crests and disturbed reflections, without player input. Ripples fade as the stronger traveling wake takes over. Both water quality shaders and the legacy fallback support idle ripples; opening a page pauses their animation with the world.
- Port collision/visual radii are **4.5×** their former size (153–166.5 world units). Island radii are **4×** (100–112). Player ship size is retained. Normal chase distance is **185** (previously 155); combat remains 600.
- Dock range is **218**, island interaction range **164**, both outside the largest ship plus land envelope. Closely spaced locations were nudged apart to leave navigable channels, and northern/eastern land fits inside the map. Catalog positions drive the renderer, minimap, full map and navigation together.
- Port/island interaction still requires a tap in range. Closing a dock/island panel leaves the ship in place.

## Real 3D reference artwork

VoyageLandModels authors geometry from the canonical Gemini paintings under
android/assets/textures/ports/port_*.png and islands/island_*.png. The paintings
remain the chart/panel artwork; the chase view uses solid meshes with layered,
irregular ridges, swept tile roofs, galleries, canal bridges, piers, cargo,
moored junks, broadleaf trees, palms, sand shelves, reefs and exposed rock.

| Ports / corresponding painting | Mesh cues |
| --- | --- |
| 广州 / 泉州 | Dense working waterfront, warm roofs, warehouses and several piers |
| 潮州 | Grey courtyard roofs, pagoda and compact streets |
| 雷州 | Ochre buildings and low green coastal hills |
| 琼州 | Warm-roofed tropical buildings and palms |
| 崖州 | Cliff-backed harbor and raised settlement |
| 合浦 | Open working quay, warehouses and cargo |
| 交州 / 扬州 | Cross-canals, arched bridges, tiled neighborhood blocks; Yangzhou courtyard walls |
| 占城 / 真腊 / 佛逝 | Grouped temple towers, tiered roofs, spires and tropical vegetation |
| 福州 / 邕州 | Layered mountain backdrops and limestone ridges |
| 明州 / 钦州 | Cool tiled roofs, courtyard walls and gate buildings |
| 暹罗 | Golden temple spires, tiered roofs and palms |
| 渤泥 / 吕宋 / 苏禄 | Stilt houses, wooden waterfronts and wooded ridges |
| 爪哇 | Stepped stone temple terraces, tiered tower and tropical harbor |

| Islands / corresponding painting | Mesh cues |
| --- | --- |
| 南澳屿 / 琼东岛 / 担杆 / 川山 | Wooded coves, layered hills, beach and small landing |
| 西沙礁 / 东沙 | Open reef rings and sandy lagoon shores |
| 中沙 / 永兴 | Separate sand cays and palms |
| 黄岩 | Connected high limestone ridge, wooded slopes and exposed coastal rock |
| 万山 / 硇洲 | Separate rocky islets |
| 涠洲 | Ring of volcanic hills and dark coastal stacks |
| 海陵 | Low sandy coastline and small green hills |

No camera-facing location billboards, external mesh service, or glTF pipeline.

## Fullscreen pages and profile

Stage-filling pages: **船长、任务、货舱、情报、图鉴、商城、我的船、属性详情、新手说明、失败、钓鱼、各港行情**.
The design grid adapts to the actual stage, including wide screens; content and
controls remain proportional. Readable body type is at least 24 design pixels,
with larger body text on help, stats and account pages. Task rows have room for
both title and progress; help actions stay visible while the rules scroll.

All open overlays pause movement, weather, clock and combat. The explicitly
started dockside fishing activity continues only on the Yangzhou dock/fishing
page. Port, island and in-port market panels retain their existing layout.

Captain navigation is **个人信息 / 存档 / 调试 / 账号**. Nickname (up to 16 characters)
and avatar index are saved per account, and the HUD circle updates immediately.
Four circular procedural placeholder portraits are provided; adding
textures/avatars/avatar_01.png through avatar_04.png replaces them automatically.
Save, load, max-account confirmation/cancel, and logout remain available. The max
confirmation is inline in the captain page. Older saves default to 船长/avatar 1.

The font now includes common CJK plus Extension A, with glyphs generated on
demand for nicknames. The bundled Noto font license is in assets/fonts/NOTO-LICENSE.txt.

## Validation

- lwjgl3:geometryRegression: left/right at 12 headings and four speeds; profile and legacy defaults; **3,564** swept collision cases; all ships/locations docking, automatic arrival and legacy overlap recovery; **34 long automatic routes from Yangzhou**.
- lwjgl3:voyage3dSmoke: real OpenGL framing, combat zoom/picking/damage, docking, resize, renderer re-entry, all nine ship purchases/equips and all mesh collision envelopes.
- lwjgl3:voyageFeelSmoke: actual Scene2D and world input, stopped/slow/undock steering, separate multitouch look/helm, HUD ownership, camera return/clamps, all 12 pages at **1280×720 and 1600×720**, pause, profile persistence, avatar context reload, save/load and logout, unchanged dock/market/island shells. Screenshots include Yangzhou and other location meshes from front and side.
- lwjgl3:maxAccountSmoke: inline confirmation/cancel, saved account state, credential/account isolation and idempotence.
- lwjgl3:locationArtworkSmoke: all 50 named images and managed reloads; all 34 map targets/corners/taps; dock/island/codex artwork.
- python3 tools/check_ui_font.py: zero missing UI glyphs.
- lwjgl3:waterSmoke: both water shaders, stopped open-sea animation without input, identical frames while paused, stronger moving wake, resource reload, shoreline, combat and animated fallback at rest. Both water quality shaders and the sky also compile/link as GLSL ES 1.00.
- Android APK is built through the requested release script, attached as the Release asset, and excluded from git by Builds/ and *.apk.
