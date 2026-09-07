# 商城 · 买船换船

已阅读 `assets/ui-refs/prompts/05-shop-ships.md`、全局布局规范，并打开 `05-shop-ships.png`。以参考图的中央深海蓝面板、铜金边框、左分类、右四列船卡为结构，复用 QuestUi 的纹理与按钮样式。

面板为 1248×800 设计像素，8px 网格，在 VoyageScreen 现有 1280×720 HUD 中按 2/3 缩放，对应 1920×1080 横屏。固定标题、真实银两与当前船属性、说明、四分类、4/4/1 船卡和底部提示；关闭使用明确的“关闭”按钮。选中分类为玉色文字，当前船卡高亮，购买为朱砂主操作。详情页展示大图、描述、属性、换乘效果、实际船价和购买／免费换乘操作。

`ShopShipsPanel` 负责展示及筛选；VoyageScreen 保留选择状态、详情效果计算、原有 `buyShip` / `equipShip`、toast、persist、关闭与暂停流程。面板及图集延迟创建、跨 rebuild 复用，在两个屏幕清理路径释放。原有商城整页 ScrollPane 替换为固定面板，九艘船全部可见。

Catalog 没有分类字段，因此仅在展示层分组：船只显示全部；战船为楼船、蒙冲、斗舰、走舸；货船为漕舫、货舶；特种船为游艇、海鹘（按参考图标签，海鹘的战船属性仍保留）。初始小商船仅在“船只”中。名称与价格完全读取 Catalog：小商船 0、楼船 900、蒙冲 1400、漕舫 1800、斗舰 2100、走舸 3000、货舶 3600、游艇 4200、海鹘 6000 两。示例价格和“货船”商品名没有覆盖真实数据。所有船卡显示价格，并独立展示当前／已拥有状态。

新增 `assets/textures/shop/ships-atlas.png`，1536×1024，3×3 九船插画，按 Catalog 顺序使用 TextureRegion；边缘内缩 2px 避免图集分隔线，Nearest 采样。图集无文字，名称、价格和状态均为 Scene2D。已有小图标继续用于其他页。补齐 ui-chars.txt 和字体子集缺失的“特”，保留原字体全部 Unicode 字符。

验证：`./gradlew :core:compileJava` 通过；`python3 tools/check_ui_font.py` 通过。使用 /tmp 隔离桌面预览检查网格、特种船筛选与详情，并验证内存 GameState 的购买扣款、已拥有免费换乘和银两不足不扣款。未打包、未发布、未改版本、未 commit；不写 assets/saves/，不改 bug-*.jpg，不操作 FreeBuff 进程。未做 Android 真机视觉验收。

插画使用内置 image_gen 工具生成，最终提示词如下：

```text
Use case: stylized-concept. Asset type: single game ship illustration atlas for 南海航程. Create exactly 3 columns x 3 rows of equal rectangular illustrations, edge-to-edge with NO gaps, NO borders, NO text, NO UI. Overall landscape 1536x1024. Each tile shows one complete Chinese junk ship centered at sea with cloudy blue sky, distant ancient Chinese harbor, golden sunlight, ornate wooden hull, cream battened sails. Advanced HD pixel art, crisp pixel texture, ink navy, muted sea teal and antique gold. Keep ships fully inside each tile with margins. Row-major subjects: 1 small humble merchant junk; 2 elaborate tall tower warship; 3 low leather-armored assault junk; 4 broad cargo barge with large sail; 5 fortified war junk with red banners; 6 narrow fast cutter; 7 massive ocean cargo junk; 8 elegant pleasure yacht; 9 imposing elite seabird-shaped warship. Consistent side three-quarter view and horizon. Each tile must be a separate complete scene, no cross-tile elements. No Western pirate ships, no letters, no numerals, no watermarks.
```
