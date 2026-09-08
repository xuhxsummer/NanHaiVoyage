# 0.28.1

修船穿模；港口岛屿差异化造型；换船外观同步主界面。

- 港口、岛屿与船只共用稳定 Catalog 序号的几何参数。碰撞包络覆盖岸线、码头、礁石及完整船体；短步移动防止低帧率或高速穿越陆地，自动航行使用相同包络绕行。
- 21 个港口和 13 个岛屿分别生成独立模型，变化包括岸线、建筑数量与高度、屋顶色、码头长度、楼塔层数、山丘、树林、崖壁及礁湖。
- 9 艘船分别缓存模型；购买或换乘后下一次主界面渲染读取 GameState.ship，船体长宽、甲板长度、桅杆高度、帆数与帆色随船变化。
- 保持本地离线 AccountStore 存档、Catalog 坐标、HUD、追尾镜头和战斗缩放。

Collision deliberately uses conservative circular envelopes of the actual meshes, including the selected ship at every heading. It does not derive obstacle size from interaction ranges. Every ship retains room to approach within the existing port/island interaction range. Legacy overlap and larger ship switches are corrected before rendering, including while docked.

Validation:

- `./gradlew :lwjgl3:geometryRegression`: 3,645 high-speed impacts; every Catalog ship/land combination checked for docking clearance, auto-sail arrival and legacy overlap recovery. Directions starting beyond the world boundary are excluded from the head-on impact test.
- `xvfb-run -a -s '-screen 0 1920x1080x24' ./gradlew :lwjgl3:voyage3dSmoke`: real GL mesh-envelope checks, distinct models, all nine purchases and equips, next-render model selection, movement, combat zoom/damage, perspective picking, docking, map, resizing and screen re-entry. Unsaved in-memory state only.
- `./gradlew :android:assembleDebug` and `python3 tools/check_ui_font.py` pass.
