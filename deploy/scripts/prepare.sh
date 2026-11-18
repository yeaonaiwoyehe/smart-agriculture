#!/usr/bin/env bash
# ============================================================
# 部署前预检：确认 JAR、前端 dist、.env、SQL 脚本均已就位
# 用法：bash prepare.sh
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(dirname "$SCRIPT_DIR")"
fail=0

check() {
  local desc="$1"; shift
  if "$@" >/dev/null 2>&1; then
    echo "  [OK] $desc"
  else
    echo "  [缺失] $desc"
    fail=1
  fi
}

echo "==> 检查部署产物..."
check "CMS  JAR (cms/app.jar)"      test -f "$DEPLOY_DIR/cms/app.jar"
check "GPS  JAR (gps/app.jar)"      test -f "$DEPLOY_DIR/gps/app.jar"
check "前端 index.html"             test -f "$DEPLOY_DIR/frontend/dist/index.html"
check "环境变量 .env"               test -f "$DEPLOY_DIR/.env"
check "MySQL 脚本目录"              test -d "$DEPLOY_DIR/sql/mysql"
check "PostgreSQL 脚本目录"         test -d "$DEPLOY_DIR/sql/pg"

n_mysql=$(find "$DEPLOY_DIR/sql/mysql" -name '*.sql' | wc -l)
n_pg=$(find "$DEPLOY_DIR/sql/pg" -name '*.sql' | wc -l)
echo "    MySQL 脚本 $n_mysql 个，PostgreSQL 脚本 $n_pg 个"

if [[ "$fail" -ne 0 ]]; then
  echo "==> 存在缺失项，请补齐后再部署。"
  exit 1
fi
echo "==> 预检通过，可以执行 docker compose up -d --build"
