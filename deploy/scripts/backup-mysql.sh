#!/usr/bin/env bash
# Щоденний backup MySQL (retention 30 днів)
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-$(dirname "$0")/../backups}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3307}"
MYSQL_USER="${MYSQL_USER:-rmpd}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-rmpd}"
MYSQL_DATABASE="${MYSQL_DATABASE:-rmpd}"
CONTAINER="${MYSQL_CONTAINER:-rmpd-mysql}"

mkdir -p "$BACKUP_DIR"
STAMP=$(date +%Y%m%d_%H%M%S)
OUTFILE="$BACKUP_DIR/rmpd_${STAMP}.sql.gz"

echo "Backup to $OUTFILE"
docker exec "$CONTAINER" mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" | gzip > "$OUTFILE"

find "$BACKUP_DIR" -name 'rmpd_*.sql.gz' -mtime +"$RETENTION_DAYS" -delete
echo "Done."
