#!/bin/bash
# 落地页测试数据导入脚本
# 用法: ./import-test-data.sh [base_url]
# 默认: http://localhost:8080
#
# 前置条件:
#   1. 应用已启动 (mvn spring-boot:run 或 java -jar)
#   2. Erupt 管理后台已登录 (默认: erupt / erupt)

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SEEDS_FILE="$SCRIPT_DIR/seeds.json"
AUTH_COOKIE=""

# ---------- 颜色 ----------
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# ---------- 检查依赖 ----------
command -v curl  >/dev/null 2>&1 || error "需要 curl，请先安装: brew install curl"
[[ -f "$SEEDS_FILE" ]] || error "找不到 seeds.json: $SEEDS_FILE"

# ---------- Cookie 登录 ----------
login() {
  info "正在登录管理后台..."
  local resp
  resp=$(curl -s -c /tmp/erupt_cookie.txt -X POST \
    -H "Content-Type: application/json" \
    -d '{"username":"erupt","password":"erupt"}' \
    "$BASE_URL/api/login")

  if echo "$resp" | grep -q '"code":0\|"success":true'; then
    info "登录成功"
  else
    warn "登录响应: $resp"
    warn "若认证失败，请确认账号密码后在浏览器手动登录后再运行此脚本"
  fi
}

# ---------- 发请求 ----------
post_json() {
  local path="$1" payload="$2"
  curl -s -b /tmp/erupt_cookie.txt \
    -H "Content-Type: application/json" \
    -d "$payload" \
    "$BASE_URL$path"
}

get_json() {
  local path="$1"
  curl -s -b /tmp/erupt_cookie.txt \
    "$BASE_URL$path"
}

# ---------- 导入模板 ----------
import_templates() {
  local tpl_dir="$SCRIPT_DIR/templates"
  [[ -d "$tpl_dir" ]] || { warn "templates/ 目录不存在，跳过模板导入"; return; }

  info "导入模板..."
  local count=0
  for f in "$tpl_dir"/*.json; do
    [[ -f "$f" ]] || continue
    local name; name=$(jq -r '.name // "unnamed"' "$f")
    local body; body=$(jq -c '.' "$f")
    local resp; resp=$(post_json "/api/landing/template/save" "{\"name\":$(printf '%s' "$name" | jq -Rs),\"schema\":$(printf '%s' "$body" | jq -Rs)}")
    if echo "$resp" | grep -q '"code":0'; then
      ((count++)); info "  ✓ $name"
    else
      warn "  ✗ $name → $resp"
    fi
  done
  info "模板导入完成: $count 个"
}

# ---------- 导入种子数据 ----------
import_seeds() {
  info "导入测试种子数据..."

  # 1. 落地页
  info "  → 落地页..."
  local pages='{
    "pages":[
      {"slug":"saas-product","name":"CloudSync 产品介绍","status":10,"templateId":1},
      {"slug":"webinar-registration","name":"AI营销峰会报名","status":10,"templateId":2},
      {"slug":"course-promo","name":"数据分析课程推广","status":10,"templateId":3},
      {"slug":"consulting","name":"企业数字化转型咨询","status":10,"templateId":4},
      {"slug":"app-download","name":"GlowUp App下载页","status":10,"templateId":5},
      {"slug":"recruitment","name":"招聘信息页","status":10,"templateId":6},
      {"slug":"draft-page","name":"草稿测试页","status":0}
    ]
  }'
  local pr; pr=$(post_json "/api/landing/page/import-pages" "$pages")
  if echo "$pr" | grep -q '"code":0'; then
    info "  ✓ 落地页导入完成"
  else
    warn "  落地页导入: $pr"
  fi

  # 2. 秒杀活动
  info "  → 秒杀活动..."
  local sq; sq=$(post_json "/api/landing/seckill/import-seeds" "$(jq -c '.seckill' "$SEEDS_FILE")")
  echo "$sq" | grep -q '"code":0' && info "  ✓ 秒杀活动导入完成" || warn "  秒杀: $(echo $sq | jq -r '.msg // .')"

  # 3. 秒杀订单
  info "  → 秒杀订单..."
  local so; so=$(post_json "/api/landing/seckill-order/import-seeds" "$(jq -c '.seckillOrders' "$SEEDS_FILE")")
  echo "$so" | grep -q '"code":0' && info "  ✓ 秒杀订单导入完成" || warn "  订单: $(echo $so | jq -r '.msg // .')"

  # 4. 优惠券
  info "  → 优惠券..."
  local cp; cp=$(post_json "/api/landing/coupon/import-seeds" "$(jq -c '.coupons' "$SEEDS_FILE")")
  echo "$cp" | grep -q '"code":0' && info "  ✓ 优惠券导入完成" || warn "  优惠券: $(echo $cp | jq -r '.msg // .')"

  # 5. 用户优惠券
  info "  → 用户优惠券..."
  local uc; uc=$(post_json "/api/landing/user-coupon/import-seeds" "$(jq -c '.userCoupons' "$SEEDS_FILE")")
  echo "$uc" | grep -q '"code":0' && info "  ✓ 用户优惠券导入完成" || warn "  用户券: $(echo $uc | jq -r '.msg // .')"

  # 6. 留资记录
  info "  → 留资记录..."
  local ld; ld=$(post_json "/api/landing/lead/import-seeds" "$(jq -c '.leads' "$SEEDS_FILE")")
  echo "$ld" | grep -q '"code":0' && info "  ✓ 留资记录导入完成" || warn "  留资: $(echo $ld | jq -r '.msg // .')"

  # 7. 访问日志
  info "  → 访问日志..."
  local al; al=$(post_json "/api/landing/access-log/import-seeds" "$(jq -c '.accessLogs' "$SEEDS_FILE")")
  echo "$al" | grep -q '"code":0' && info "  ✓ 访问日志导入完成" || warn "  访问日志: $(echo $al | jq -r '.msg // .')"

  info "种子数据导入完成!"
}

# ---------- 验证导入结果 ----------
verify() {
  info "验证导入结果..."
  local endpoints=(
    "/api/landing/template/list"
    "/api/landing/page/draft-and-published"
    "/api/landing/seckill/list"
    "/api/landing/seckill-order/list"
    "/api/landing/coupon/list"
    "/api/landing/user-coupon/list"
    "/api/landing/lead/list"
    "/api/landing/access-log/list"
  )
  for ep in "${endpoints[@]}"; do
    local r; r=$(get_json "$ep")
    local cnt; cnt=$(echo "$r" | jq -r '.data.total // (.data | length) // "N/A"' 2>/dev/null)
    printf "  %-40s %s\n" "$ep" "→ $cnt 条"
  done
}

# ---------- 主流程 ----------
main() {
  echo "=========================================="
  echo "  落地页测试数据导入脚本"
  echo "  目标: $BASE_URL"
  echo "=========================================="

  login
  import_templates
  import_seeds
  verify

  echo ""
  info "导入完成！请在浏览器访问 $BASE_URL 查看管理后台数据"
  rm -f /tmp/erupt_cookie.txt
}

main "$@"
