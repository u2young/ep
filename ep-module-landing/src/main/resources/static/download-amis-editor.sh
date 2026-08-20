#!/bin/bash
# amis-editor SDK 离线下载脚本
# 用途:离线部署时下载 amis-editor 到本地(管理端编辑器,约 50MB)
# H5 渲染 SDK 已内置在 static/amis/,无需下载
#
# 用法:
#   cd ep-module-landing/src/main/resources/static
#   bash download-amis-editor.sh
#
# 下载完成后,编辑 landing-editor.html 把 CDN 链接改为本地路径

set -e
AMIS_VER=6.13.0
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
EDITOR_DIR="$SCRIPT_DIR/amis-editor"

echo "=========================================="
echo " amis-editor $AMIS_VER 离线下载"
echo " 目标: $EDITOR_DIR"
echo "=========================================="

mkdir -p "$EDITOR_DIR"
cd "$EDITOR_DIR"

# amis-editor 核心文件(lib 目录)
echo "[1/4] 下载 amis-editor lib..."
curl -sL -o editor.js   "https://cdn.jsdelivr.net/npm/amis-editor@$AMIS_VER/lib/editor.js"
curl -sL -o editor.css  "https://cdn.jsdelivr.net/npm/amis-editor@$AMIS_VER/lib/editor.css"
curl -sL -o app.js      "https://cdn.jsdelivr.net/npm/amis-editor@$AMIS_VER/lib/app.js"

# amis 运行时(编辑器依赖,复用 static/amis/ 也可)
echo "[2/4] 下载 amis SDK..."
curl -sL -o sdk.js      "https://cdn.jsdelivr.net/npm/amis@$AMIS_VER/sdk/sdk.js"
curl -sL -o sdk.css     "https://cdn.jsdelivr.net/npm/amis@$AMIS_VER/sdk/sdk.css"
curl -sL -o helper.css  "https://cdn.jsdelivr.net/npm/amis@$AMIS_VER/sdk/helper.css"
curl -sL -o iconfont.css "https://cdn.jsdelivr.net/npm/amis@$AMIS_VER/sdk/iconfont.css"

# React 依赖(amis-editor 基于 React)
echo "[3/4] 下载 React..."
curl -sL -o react.production.min.js     "https://cdn.jsdelivr.net/npm/react@18/umd/react.production.min.js"
curl -sL -o react-dom.production.min.js "https://cdn.jsdelivr.net/npm/react-dom@18/umd/react-dom.production.min.js"

# 图标字体
echo "[4/4] 下载 iconfont..."
curl -sL -o iconfont.woff2 "https://cdn.jsdelivr.net/npm/amis@$AMIS_VER/sdk/fonts/iconfont.woff2" || true
mkdir -p fonts && mv iconfont.woff2 fonts/ 2>/dev/null || true

echo ""
echo "=========================================="
echo " 下载完成! 文件列表:"
echo "=========================================="
ls -lh *.js *.css 2>/dev/null | awk '{print $5, $9}'

echo ""
echo "下一步: 编辑 landing-editor.html,把 CDN 链接改为本地:"
echo "  https://cdn.jsdelivr.net/npm/amis-editor@$AMIS_VER/... → /static/amis-editor/..."
echo "  https://cdn.jsdelivr.net/npm/amis@$AMIS_VER/...       → /static/amis-editor/..."
