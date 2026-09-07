# 南海海图 overlay

遵循 assets/ui-refs/prompts/00-global-app-layout.md、07-world-map.md，参考 assets/ui-refs/07-world-map.png。

- 独立 1920×1080 画板，基础布局采用 8px 网格；与现有 1280×720 HUD 等比换算，不改小地图和航海 HUD。
- 底图 assets/textures/world-map/sea-chart-hd.png 仅含海面、浮雕地貌、铜金框和海图装饰。所有名称及可交互地点取自 Catalog：21 个港口、13 个岛屿，不增删玩法地点。
- 世界投影矩形为左下角 (144,120)、尺寸 1568×824。地点、船位、航线和点击共用投影；标签避让地点和其他标签，标签本身可点击。海岸美术为示意，不参与碰撞或航行判定。
- 标题“南海海图”，副说明“点港口/岛屿自动驶向”；左下图例“港口城市”“岛屿/礁盘”，右上“关闭”。
- 金色虚线为已有港口间的示意贸易连线；自动航行时以玉青虚线标出本船至当前目标。本船标记使用 GameState.x/y，绘制朱砂红圈与中式硬帆船符号，不绑定苏禄或任何港口。
- 原有 undockIfNeeded、startAutoSail、startAutoSailIsle、rebuildMenu 调用保持。空白不关闭；关闭按钮及外框外点击关闭。全图保持拦截底层 HUD 输入。
- 地图资源延迟加载并在航海页隐藏时释放；批次投影、字体比例与颜色在绘制后恢复。字库仅补齐“盘”。

本轮仅运行 compileJava 编译，不生成 APK、不打包、不发布、不修改版本，也不运行桌面截图或交互测试。

底图使用内置 imagegen，生成提示词如下：

```
Use case: style-transfer. Production bitmap background for a Tang dynasty Chinese maritime game's 1920x1080 landscape world map overlay, premium HD pixel art. Image 1 is the visual reference: faithfully match dark navy/teal engraved sea, antique parchment olive-gold relief islands with intricate mountain/coast hatching, fine copper-gold ornamental border, brass compass at left top and bottom left, scroll on far right edge, restrained corner mist. Image 2 is the REQUIRED geographical layout guide, replace its crude circles with detailed irregular mountainous land masses preserving their centers. NO text, letters, numbers, labels, location markers, ships, player rings or UI buttons; those are runtime layers. Map world region in final 1920x1080 image is x144 to1712, y136 to960 (y down). Map Image 2 precisely into that rectangle; maintain centers of all islands/coastal chains when refining contours. Keep top-left x56..640 y48..224 dark and quiet for native title; bottom-left x72..400 y832..1008 quiet for legend. Do not introduce land in these title/legend areas. Chinese nautical compass roses integrated into borders and faint rhumb lines on sea. Elegant detailed gold stepped frame at x24 y24 to1896 y1056, dark blue outside mat. No western pirate elements, no photoreal 3D. Artwork must look like mature commercial pixel art, subtle fine pixel clusters and exquisite textured detail. Only background map art; all Chinese text will be rendered in Java.
```

