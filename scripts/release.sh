#!/usr/bin/env bash
# =============================================================================
# release.sh — 南海航程一键打包发版脚本
#
# 用法:
#   ./scripts/release.sh <版本号> "<简短中文说明>"
#
# 示例:
#   ./scripts/release.sh 0.27.1 "修复港口菜单崩溃"
#
# 可选参数:
#   --dry-run          只打印将执行的步骤，不打包 / 不提交 / 不发版
#   --skip-commit      跳过 git 提交与推送
#   --skip-release     跳过 gh release 创建（同时跳过 gh 登录校验）
#   --skip-font-check  跳过 CJK 字体检查（tools/check_ui_font.py）
#   --version-code N   指定 versionCode（默认在 android/build.gradle 基础上自动 +1）
#   --strict           字体检查失败时直接退出（默认仅警告）
#
# 流程:
#   1. 校验依赖（git / gh / gradlew）、gh 登录状态与版本号格式
#   2. 改写 android/build.gradle 的 versionName / versionCode
#   3. 运行 tools/check_ui_font.py（若存在）
#   4. ./gradlew :android:assembleDebug 打包
#   5. 复制 APK 到 Builds/NanHaiVoyage.apk
#   6. git add（排除 bug-*.jpg / assets/saves/ / scratch / howto_* / __pycache__）
#      并提交 "vX.Y.Z: <说明>"，推送 origin 当前分支
#   7. gh release create vX.Y.Z（标题 vX.Y.Z、说明为传入文案，附 Builds/NanHaiVoyage.apk）
#      最后打印 Release URL
# =============================================================================
set -euo pipefail

VERSION=""
DESCRIPTION=""
VERSION_CODE=""
DRY_RUN=0
SKIP_COMMIT=0
SKIP_RELEASE=0
SKIP_FONT=0
STRICT=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run) DRY_RUN=1; shift ;;
    --skip-commit) SKIP_COMMIT=1; shift ;;
    --skip-release) SKIP_RELEASE=1; shift ;;
    --skip-font-check) SKIP_FONT=1; shift ;;
    --strict) STRICT=1; shift ;;
    --version-code)
      shift
      if [[ $# -lt 1 ]]; then
        echo "错误: --version-code 需要数字参数，例如 --version-code 26" >&2
        exit 1
      fi
      VERSION_CODE="$1"
      shift
      ;;
    -*)
      echo "错误: 未知参数: $1（用法见脚本头部注释）" >&2
      exit 1
      ;;
    *)
      if [[ -z "$VERSION" ]]; then
        VERSION="$1"
      elif [[ -z "$DESCRIPTION" ]]; then
        DESCRIPTION="$1"
      else
        echo "错误: 多余的位置参数: $1" >&2
        exit 1
      fi
      shift
      ;;
  esac
done

fail() { echo "错误: $*" >&2; exit 1; }

# ---- 版本号 / 说明校验 --------------------------------------------------------
[[ -n "$VERSION" ]] || fail "缺少版本号。用法: ./scripts/release.sh <版本号> \"<说明>\""
[[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] \
  || fail "版本号格式非法: '$VERSION'（应为 X.Y.Z，例如 0.27.1）"
[[ -n "$DESCRIPTION" ]] || fail "缺少发版说明。用法: ./scripts/release.sh <版本号> \"<说明>\""
if [[ -n "$VERSION_CODE" ]]; then
  [[ "$VERSION_CODE" =~ ^[0-9]+$ ]] || fail "--version-code 必须是数字: '$VERSION_CODE'"
fi

# ---- 定位仓库根目录 -----------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT"
echo ">> 仓库根目录: $ROOT"

# ---- 依赖校验 -----------------------------------------------------------------
for cmd in git gh; do
  command -v "$cmd" >/dev/null 2>&1 || fail "未找到命令 '$cmd'，请先安装。"
done
[[ -x "$ROOT/gradlew" ]] || fail "未找到可执行文件 gradlew（$ROOT/gradlew）"
if [[ "$SKIP_RELEASE" -eq 0 ]]; then
  if ! gh auth status >/dev/null 2>&1; then
    fail "gh 未登录。请先运行 gh auth login，再执行本脚本。"
  fi
fi

# ---- 从 git remote 解析 GitHub 仓库名 -----------------------------------------
REPO="$(git config --get remote.origin.url 2>/dev/null \
  | sed -E 's#\.git$##' \
  | sed -E 's#.*[:/]([^/:]+/[^/:]+)$#\1#' || true)"
[[ -n "$REPO" ]] || REPO="xuhxsummer/NanHaiVoyage"
echo ">> GitHub 仓库: $REPO"

# ---- 计算 versionCode 并改写 android/build.gradle ------------------------------
BUILD_FILE="$ROOT/android/build.gradle"
[[ -f "$BUILD_FILE" ]] || fail "找不到 $BUILD_FILE"
if [[ -z "$VERSION_CODE" ]]; then
  CUR_CODE="$(sed -nE 's/^[[:space:]]*versionCode[[:space:]]+([0-9]+).*/\1/p' "$BUILD_FILE" | head -n1)"
  [[ -n "$CUR_CODE" ]] || fail "无法从 android/build.gradle 解析当前 versionCode"
  VERSION_CODE=$((CUR_CODE + 1))
fi
echo ">> versionName -> $VERSION，versionCode -> $VERSION_CODE"

if [[ "$DRY_RUN" -eq 1 ]]; then
  echo "[dry-run] 修改 $BUILD_FILE: versionName '$VERSION' / versionCode $VERSION_CODE"
else
  sed -i -E "s/^([[:space:]]*)versionName[[:space:]]*'[^']*'/\1versionName '$VERSION'/" "$BUILD_FILE"
  sed -i -E "s/^([[:space:]]*)versionCode[[:space:]]+[0-9]+/\1versionCode $VERSION_CODE/" "$BUILD_FILE"
fi

# ---- CJK 字体检查 ---------------------------------------------------------------
FONT_CHECK="$ROOT/tools/check_ui_font.py"
if [[ "$SKIP_FONT" -eq 1 ]]; then
  echo ">> 已跳过字体检查（--skip-font-check）"
elif [[ ! -f "$FONT_CHECK" ]]; then
  echo "警告: 未找到 tools/check_ui_font.py，跳过字体检查" >&2
elif [[ "$DRY_RUN" -eq 1 ]]; then
  echo "[dry-run] python3 $FONT_CHECK"
else
  echo ">> 运行 CJK 字体检查: tools/check_ui_font.py"
  if python3 "$FONT_CHECK"; then
    echo ">> 字体检查通过：0 missing glyphs"
  else
    if [[ "$STRICT" -eq 1 ]]; then
      fail "字体检查失败（--strict），请先补全字体子集"
    fi
    echo "警告: 字体检查失败，继续打包（可用 --strict 改为失败）" >&2
  fi
fi

# ---- 打包 APK --------------------------------------------------------------------
if [[ "$DRY_RUN" -eq 1 ]]; then
  echo "[dry-run] ./gradlew :android:assembleDebug"
else
  echo ">> 构建 APK: ./gradlew :android:assembleDebug"
  (cd "$ROOT" && ./gradlew :android:assembleDebug)
fi

# ---- 复制 APK 到 Builds -----------------------------------------------------------
APK="$(find "$ROOT/android/build/outputs/apk/debug" -maxdepth 1 -name '*.apk' -print -quit 2>/dev/null || true)"
if [[ -n "$APK" ]]; then
  if [[ "$DRY_RUN" -eq 1 ]]; then
    echo "[dry-run] mkdir -p Builds && cp $(basename "$APK") Builds/NanHaiVoyage.apk"
  else
    mkdir -p "$ROOT/Builds"
    cp "$APK" "$ROOT/Builds/NanHaiVoyage.apk"
    echo ">> APK 已复制: Builds/NanHaiVoyage.apk（$(du -h "$ROOT/Builds/NanHaiVoyage.apk" | cut -f1)）"
  fi
elif [[ "$DRY_RUN" -eq 1 ]]; then
  echo "[dry-run] 构建后定位 APK 并复制到 Builds/NanHaiVoyage.apk"
else
  fail "未找到 APK（android/build/outputs/apk/debug 下无 .apk）"
fi

# ---- git 提交与推送 ---------------------------------------------------------------
if [[ "$SKIP_COMMIT" -eq 1 ]]; then
  echo ">> 已跳过 git 提交与推送（--skip-commit）"
elif [[ "$DRY_RUN" -eq 1 ]]; then
  echo "[dry-run] git add（排除 bug-*.jpg / assets/saves/ / scratch / howto_* / __pycache__）"
  echo "[dry-run] git commit -m \"v${VERSION}: ${DESCRIPTION}\""
  echo "[dry-run] git push origin $(git branch --show-current)"
else
  echo ">> git add（排除 bug-*.jpg / assets/saves/ / scratch / howto_* / __pycache__）"
  git add -A -- \
    ':(exclude)bug-*.jpg' \
    ':(exclude)assets/saves/' \
    ':(exclude,glob)**/scratch/**' \
    ':(exclude,glob)**/howto_*' \
    ':(exclude,glob)**/__pycache__/**'
  if git diff --cached --quiet; then
    echo "警告: 没有待提交的改动，跳过 commit/push" >&2
  else
    git commit -m "v${VERSION}: ${DESCRIPTION}"
    BRANCH="$(git branch --show-current)"
    git push origin "$BRANCH"
    echo ">> 已推送 origin/$BRANCH"
  fi
fi

# ---- gh release --------------------------------------------------------------------
if [[ "$SKIP_RELEASE" -eq 1 ]]; then
  echo ">> 已跳过 GitHub Release（--skip-release）"
  echo ">> Release URL: （未创建）"
  exit 0
fi

if [[ "$DRY_RUN" -eq 1 ]]; then
  echo "[dry-run] gh release create v${VERSION} --repo $REPO --title v${VERSION} --notes \"${DESCRIPTION}\" Builds/NanHaiVoyage.apk"
  echo "[dry-run] Release URL: https://github.com/$REPO/releases/tag/v${VERSION}"
  exit 0
fi

echo ">> 创建 GitHub Release: v${VERSION}"
RELEASE_URL="$(gh release create "v${VERSION}" \
  --repo "$REPO" \
  --title "v${VERSION}" \
  --notes "$DESCRIPTION" \
  "$ROOT/Builds/NanHaiVoyage.apk")"

echo
echo "=============================================="
echo "  发布完成！"
echo "  Release URL: $RELEASE_URL"
echo "=============================================="