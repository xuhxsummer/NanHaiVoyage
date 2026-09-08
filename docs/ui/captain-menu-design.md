> 0.28.9 replaces this historical popup with a fullscreen captain page. See [release notes](../release-0.28.9.md) for profile, navigation, and persistence behavior.

# 船长菜单 · 世界暂停

已阅读 `03-captain-menu.md`、全局布局规范，并打开 `assets/ui-refs/03-captain-menu.png`。独立 `CaptainMenuPanel` 对应参考图的居中窄面板、铜金边框、墨蓝底、暂停说明及三枚纵向大按钮。尺寸 752×560 设计像素，8px 网格，沿用现有 HUD 的 2/3 缩放，背景继续显示航行场景。

保存进度为深蓝金字，读取存档为深蓝玉色文字，退出登录为朱砂金边；复用现有头像、卷轴和锚点像素图标。按钮 544×80，间隔 16，右上“关闭”96×56。文字与图标均为 Scene2D Actor，没有把参考图作为整张可点击背景。

VoyageScreen 用固定面板替换旧 ScrollPane 菜单；按钮直接调用原 `saveNow`、`tryReloadLatestSave`、`logoutToLogin`、`closePopup`，保留世界暂停条件与延迟切换登录屏幕逻辑。移除不再调用的旧 avatarTable，面板延迟创建并在两个屏幕清理路径释放。

验证：`./gradlew :core:compileJava` 通过；/tmp 隔离桌面预览及 Stage 坐标命中测试通过，保存/读档/退出/关闭各触发对应回调一次。截图 `/tmp/captain-menu.png`，标题、说明及按钮无溢出。预览未执行真实存档写入。本步骤未打包或改版本，待港口经营完成后统一发布。文档使用仓库惯例 `.md` 扩展名。
