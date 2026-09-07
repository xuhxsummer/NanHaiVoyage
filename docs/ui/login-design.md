# 登录页

参考：`assets/ui-refs/11-login.png`。1920×1080，8px 网格，Scene2D FitViewport 等比适配。

背景资源：`assets/textures/login/harbor-hd.png`，含书法标题与竖章；其余文案、输入框、按钮由 Scene2D 独立渲染。金边使用可复用 NinePatch，保持原有本机账号与默认 summer 行为。公告、客服、设置显示暂未开放提示。无后端和协议链接占位跳转。

内置 imagegen 生成提示词：

```
Use case: style-transfer. Asset: production login backdrop for Chinese Tang maritime game. Reference image is composition guidance. Create 1920x1080 16:9 high-end HD pixel art version of this Tang Chinese harbor at dusk, intricate deliberate crisp pixel clusters, rich navy teal sea, gold sunset clouds, lit Chinese pagodas on LEFT, enormous authentic Chinese batten-sail merchant junk on RIGHT, foreground wood pier, brass compass and parchment charts bottom corners. Keep center from x480 to1440 y440 to920 dark low-detail open sea for native form overlay. At top center y120-360 ONLY put magnificent readable gold Chinese brush calligraphy logo exactly “南海航程”, small red vertical seal “大唐海贸” to its right. No other text whatsoever. REMOVE all reference input fields, buttons, subtitles, footer, auxiliary icons and labels. Thin antique brass ornamental outer border. Sophisticated commercial pixel art not photorealistic not 3D; preserve cinematic depth and epic about-to-sail atmosphere. No western pirates.
```

