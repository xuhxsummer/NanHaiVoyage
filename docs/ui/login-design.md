# 登录页

参考：`assets/ui-refs/17-login-dynamic-ref.png`。1920×1080，8px 网格，Scene2D ExtendViewport 铺满屏幕。

0.28.12 动态背景由 `LoginHarbor` 绘制：静态港口与书法标题、局部动态海水、三个帆与三个旗帜。其余文案、输入框、按钮由 Scene2D 独立渲染。金边使用可复用 NinePatch，保持原有本机账号与默认 summer 行为。公告、客服、设置显示暂未开放提示。无后端和协议链接占位跳转。

0.28.13 根据 `assets/ui-refs/18-login-too-subtle.jpg` 加强动效与表单对比度：海水水平扰动约为上版 3.2 倍、垂直扰动约 3.1 倍，相位速度从 0.35 增至 0.9 rad/s，反光变化约 2.2 倍；保留原遮罩和四次纹理采样。帆摆幅为 1.7–2.4°、旗帜为 7–8.5°，同时加快风摆与增大布面伸缩；`layers.json` 和切片工具的参数同步更新。

输入区新增约 92% 不透明的深蓝底板，金边从 2px 加粗到 6px（1920 UI 坐标），标签 32→40px，输入文字 32→38px，按钮文字 48→56px。登录为亮朱红、注册为更亮的海蓝，均配浅金文字。底板只覆盖输入区，按钮周围与下方继续显示动态海水。

图层位于 `android/assets/textures/login/`：

- `harbor-base.png`：从原画移除活动帆旗，并用邻近像素填补背面；天空、岸线、船体、前景、标题保持静止。
- `sea-mask.png`：半分辨率灰度软遮罩。libGDX 以 Alpha 纹理加载，`assets/shaders/login-sea.frag` 读取 `.a`，只在海水区域做 UV 扰动与波浪反光变化。
- `sail-rear.png`、`sail-main.png`、`sail-front.png` 与 `flag-main.png`、`flag-front.png`、`flag-stern.png`：透明裁片，按 `layers.json` 的顺序叠加；正弦旋转与缩放模拟风。
- `layers.json`：原画尺寸 1672×941，裁片位置从左上角计；pivotX/pivotY 已转换为裁片左下角坐标。旋转幅度单位为度，rate 为弧度/秒，phase 为弧度。

从仓库根目录运行 `python3 tools/slice_login_layers.py` 可重建图层（需要 Pillow），源图为 `assets/textures/login/harbor-hd.png`；运行时不再加载源图。`prepareWaterAssets` 将新图层同步到 Android 与桌面共用的生成资源目录。

绘制开销为一次港口/海水 pass 与六个小裁片，不创建航海世界；每帧不生成贴图。背景统一缩放以保持遮罩和裁片对齐。时间步长限制为 0.1 秒并循环相位，避免恢复应用时跳变和长时间运行后的精度下降。水 shader 编译失败时保留静态海面与帆旗动画；切回 UI 前恢复 batch shader 和颜色。隐藏时释放所有背景贴图与 shader，重新显示时重建。

验证命令：`xvfb-run -a ./gradlew :lwjgl3:loginDynamicSmoke`。测试真实 GL 的海水/帆旗像素变化、静止标题/输入框、暂停、不同宽高比、资源重载、shader 降级、页面生命周期，以及隔离本机账号的注册/登录/存档读取。0.28.13 额外检查 4.8 秒内每个帆/旗端点的屏幕移动范围均超过 8px，避免只有细小像素变化却难以察觉。截图与 15fps 连续帧输出到 gitignored 的 `Builds/login2813/`。

输入框获得焦点时使用青蓝底色。页面重新显示时重置切换状态；已隐藏页面的延迟切换任务失效；航海页面初始化失败时恢复实际登录页面及输入处理。登录页仅释放自身持有的资源和输入处理器。

内置 imagegen 生成提示词：

```
Use case: style-transfer. Asset: production login backdrop for Chinese Tang maritime game. Reference image is composition guidance. Create 1920x1080 16:9 high-end HD pixel art version of this Tang Chinese harbor at dusk, intricate deliberate crisp pixel clusters, rich navy teal sea, gold sunset clouds, lit Chinese pagodas on LEFT, enormous authentic Chinese batten-sail merchant junk on RIGHT, foreground wood pier, brass compass and parchment charts bottom corners. Keep center from x480 to1440 y440 to920 dark low-detail open sea for native form overlay. At top center y120-360 ONLY put magnificent readable gold Chinese brush calligraphy logo exactly “南海航程”, small red vertical seal “大唐海贸” to its right. No other text whatsoever. REMOVE all reference input fields, buttons, subtitles, footer, auxiliary icons and labels. Thin antique brass ornamental outer border. Sophisticated commercial pixel art not photorealistic not 3D; preserve cinematic depth and epic about-to-sail atmosphere. No western pirates.
```
