# 任务 overlay

已重新阅读全局布局规范及任务页说明，打开并参考 `assets/ui-refs/02-quests.png`。

中央面板采用 1152×752 设计像素，嵌入 1920×1080 横屏构图；以 2/3 比例接入现有 1280×720 HUD，不修改航行背景、资源栏或小地图。设计尺寸与间距采用 8px 网格。

朱砂红任务题签、铜金双边框、羊皮纸任务行、朱砂红选中项、深海蓝详情区与玉石青完成状态均为独立 Scene2D 组件。QuestUi 生成细颗粒纹理及角饰 NinePatch，复用现有 HUD 奖励图标；不依赖新增 shader 或整页截图。

左侧展示原有 19 条任务，独立滚动，选择任务后保留列表滚动位置；右侧展示真实任务描述、Catalog 目标、进度和奖励，长详情独立滚动。标题计数使用 QUESTS.length。仅展示实际银两、补给、耐久奖励，无新增声望等玩法奖励。

领取继续复核完成与领取状态，沿用 claimQuest、persist 和下一任务选择。未完成按钮禁用。导航沿用现有离港条件、startAutoSail / startAutoSailIsle 和关闭逻辑。GameState、Catalog、任务定义和奖励数值均不改变。

验证范围：仅 core / lwjgl3 compileJava；本轮不运行游戏、截图、APK、打包或发布操作。
