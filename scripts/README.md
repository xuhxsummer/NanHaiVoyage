# 一键打包发版脚本

任意 AI 改完代码后，在仓库根目录执行一条命令即可完成：改版本号 → 检查字体 →
打包 APK → git 提交推送 → 创建 GitHub Release 并附上 APK。

```bash
./scripts/release.sh 0.27.1 "修复港口菜单崩溃"
```

根目录的 `./release.sh` 是指向本脚本的软链，两种写法等价。

## 流程

1. 校验环境（git / gh / gradlew 存在、gh 已登录、版本号格式合法）。
2. 把 `android/build.gradle` 的 `versionName` 改为传入的版本号，
   `versionCode` 自动 +1（也可用 `--version-code N` 指定）。
3. 运行 `tools/check_ui_font.py` 检查 CJK 字体覆盖（缺失字默认仅警告）。
4. `./gradlew :android:assembleDebug` 打包，APK 复制到 `Builds/NanHaiVoyage.apk`。
5. `git add` 相关源码（自动排除 bug 截图、存档、临时产物），提交
   `vX.Y.Z: <说明>`，推送到 origin 当前分支。
6. `gh release create vX.Y.Z`（标题 vX.Y.Z、说明为传入文案），附带
   `Builds/NanHaiVoyage.apk`，最后打印 Release URL。

## 前置条件

- 已安装 `git`、`gh`（GitHub CLI），仓库根目录有可执行的 `gradlew`。
- **`gh auth login` 已登录**：脚本发版前会校验，未登录直接报错退出。
- **不要提交 bug 截图**：`bug-*.jpg` 已被脚本排除，也不要手动 `git add` 它们。
  若误提交过，用 `git rm --cached bug-*.jpg` 移出后再提交。

## 参数

| 参数 | 说明 |
| --- | --- |
| `<版本号>` | 必填，格式 X.Y.Z（纯数字），例如 `0.27.1` |
| `<说明>` | 必填，简短中文发版说明，会写进 git commit 与 Release notes |
| `--dry-run` | 只打印将要执行的步骤，不打包、不提交、不发版 |
| `--skip-commit` | 跳过 git 提交与推送 |
| `--skip-release` | 跳过 gh release 创建（同时跳过 gh 登录校验） |
| `--skip-font-check` | 跳过 CJK 字体检查 |
| `--version-code N` | 指定 versionCode（默认自动 +1） |
| `--strict` | 字体检查有缺失字时直接失败（默认仅警告） |

## 注意事项

- 先 `--dry-run` 预览一遍，再正式执行。
- 提交时会自动排除：`bug-*.jpg`、`assets/saves/`、`scratch/` 目录、
  `howto_*` 文件、`__pycache__`——调试截图和临时产物不会入库。
- 发版说明请写清楚这次改了什么（修复 / 新增 / 改版），玩家可见。