#!/usr/bin/env bash
# ============================================================
# 数据库初始化：在容器全部启动后执行
# 按顺序执行 sql/mysql 与 sql/pg 下的脚本，并把图片 URL 重写为公网地址
# 用法：bash 02-db-init.sh
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(dirname "$SCRIPT_DIR")"

if [[ ! -f "$DEPLOY_DIR/.env" ]]; then
  echo "未找到 $DEPLOY_DIR/.env，请先复制 .env.example 并填写"
  exit 1
fi
set -a; . "$DEPLOY_DIR/.env"; set +a

MYSQL_C="docker exec -i agrinav-mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} --default-character-set=utf8mb4"
PG_C="docker exec -i agrinav-postgres psql -U ${PG_USER} -d ${PG_DB} -v ON_ERROR_STOP=1"

echo "==> 等待 MySQL / PostgreSQL 就绪..."
for i in $(seq 1 60); do
  if docker exec agrinav-mysql mysqladmin -uroot -p"${MYSQL_ROOT_PASSWORD}" ping >/dev/null 2>&1 \
     && docker exec agrinav-postgres pg_isready -U "${PG_USER}" -d "${PG_DB}" >/dev/null 2>&1; then
    break
  fi
  sleep 3
done

echo "==> [1/2] 初始化 MySQL 库：${MYSQL_DB}"
for f in "$DEPLOY_DIR"/sql/mysql/*.sql; do
  echo "    - $(basename "$f")"
  $MYSQL_C "${MYSQL_DB}" < "$f"
done

echo "==> [2/2] 初始化 PostgreSQL 库：${PG_DB}"
for f in "$DEPLOY_DIR"/sql/pg/*.sql; do
  name="$(basename "$f")"
  echo "    - $name"
  if [[ "$name" == "11_photo_replace.sql" ]]; then
    # 把本地 127.0.0.1:8088 重写为公网地址（nginx 80 端口的 /profile）
    sed "s|http://127.0.0.1:8088|${PUBLIC_BASE_URL}|g" "$f" | $PG_C
  else
    $PG_C < "$f"
  fi
done

echo "==> 关键表计数核对："
$PG_C <<'SQL'
SELECT 'land_plot' AS t, count(*) FROM land_plot
UNION ALL SELECT 'boundary', count(*) FROM boundary
UNION ALL SELECT 'agricultural_machinery', count(*) FROM agricultural_machinery
UNION ALL SELECT 'machinery_image', count(*) FROM agricultural_machinery_image
UNION ALL SELECT 'trajectory_info', count(*) FROM trajectory_info
UNION ALL SELECT 'effective_point_buffer', count(*) FROM effective_point_buffer
UNION ALL SELECT 'pricing_rule', count(*) FROM pricing_rule;
SQL

echo "==> 数据库初始化完成。"
