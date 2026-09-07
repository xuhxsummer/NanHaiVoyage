# 港口经营 · 世界暂停

已阅读 `01-port-dock.md`、`00-global-app-layout.md`，打开 `assets/ui-refs/01-port-dock.png`。采用参考图的墨蓝铜金框、港名标题签、资源横栏与羊皮纸经营卡片；布局优先遵循提示词明确要求：市场为最醒目的独立主入口，其下为两列四行按钮，而非参考图中的五列/四列卡片排布。

`PortDockPanel` 为 1152×816 设计像素，8px 网格，在现有 HUD 中按 2/3 缩放。顶部显示真实港名、银两、补给、耐久、船员以及欠债/货舱占用。市场 1104×80、朱砂金边；次级卡片 544×80、列间距 16、行间距 8，配现有像素图标、动作与简短用途。排列为补补给/还债、修理/离港、升仓库/升炮火、升编制/雇人。价格来自实时 GameState 与 Catalog，离港保留醒目的朱砂按钮。底部展示船员、固定每发伤 1、实际射速与耐久；扬州保留独立 1104×72「渔务」入口。

展示层使用 Action 枚举，VoyageScreen.portAction 分发原有处理函数。保留市场双层导航、返回仍停港、经济操作后 toast/persist/rebuild、扬州渔务、主动离港与“关闭后原地离港”的区别。没有调整经济数值、存档格式或世界暂停条件。移除旧 portTable/portPair；延迟创建、重建复用，并在两个屏幕清理路径释放。复用 QuestUi，没有新增整页位图。

字体扫描补齐“偿充复扩等营资”7 字；从同一 Noto Sans CJK SC 字体生成子集，保留已有字体全部 Unicode 字符。

验证：
- `./gradlew :core:compileJava :lwjgl3:compileJava` 通过；A/B/C 各自实施后均已通过 core 编译。
- `python3 tools/check_ui_font.py` 通过，0 missing glyphs。
- 独立 Scene2D 预览验证普通港与扬州，11 个按钮经 Stage 坐标命中触发正确回调；按预览结果增高卡片，消除说明文字挤压。
- /tmp 隔离环境运行真实 VoyageScreen，走完整输入链验证：市场进入/返回、补给、修理、全额还债、三种升级、雇人、关闭原地离港、渔务入口、主动离港；同时验证船长菜单冻结时钟/位置/海盗计时、保存/读档、退出登录，以及“我的”容量不足拒绝换船、免费换船保留货物。
- 集成截图 `/tmp/voyage-port.png`、`/tmp/voyage-yangzhou.png`、`/tmp/voyage-captain.png`、`/tmp/voyage-mine.png`；测试存档和偏好写入 /tmp，未触碰玩家 assets/saves。未做 Android 真机视觉验收。

三个页面全部完成并通过编译后，按用户授权统一执行 0.27.3 发布脚本；此前未进行中途打包或版本修改。
