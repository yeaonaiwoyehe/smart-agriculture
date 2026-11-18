#!/bin/bash
# ============================================================
# AgriNav 数据备份：导出 MySQL/PostgreSQL 并压缩，保留最近 14 天
# 可配合 cron 每日执行，例如：
#   0 3 * * * /root/smart-agriculture/deploy/scripts/backup.sh
# ============================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEPLOY_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

set -a
. "$DEPLOY_DIR/.env"
set +a

BACKUP_ROOT="${BACKUP_PATH:-/data/agrinav-backup}"
STAMP=$(date +%Y%m%d_%H%M%S)
DEST="$BACKUP_ROOT/$STAMP"
mkdir -p "$DEST"

echo "==> 备份 MySQL：$MYSQL_DB"
docker exec agrinav-mysql mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" \
  --single-transaction --default-character-set=utf8mb4 "$MYSQL_DB" \
  | gzip > "$DEST/mysql_${MYSQL_DB}.sql.gz"

echo "==> 备份 PostgreSQL：$PG_DB"
docker exec agrinav-postgres pg_dump -U"$PG_USER" -d"$PG_DB" \
  | gzip > "$DEST/pg_${PG_DB}.sql.gz"

echo "==> 清理 14 天前的旧备份"
find "$BACKUP_ROOT" -maxdepth 1 -type d -mtime +14 -name "20*" -exec rm -rf {} \;

echo "备份完成：$DEST"
du -sh "$DEST"
