#!/usr/bin/env bash
# Відновлення MySQL з gzip-дампу
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: $0 <backup.sql.gz>"
  exit 1
fi

BACKUP_FILE="$1"
MYSQL_USER="${MYSQL_USER:-rmpd}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-rmpd}"
MYSQL_DATABASE="${MYSQL_DATABASE:-rmpd}"
CONTAINER="${MYSQL_CONTAINER:-rmpd-mysql}"

echo "Restoring $BACKUP_FILE into $MYSQL_DATABASE..."
gunzip -c "$BACKUP_FILE" | docker exec -i "$CONTAINER" mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"
echo "Restore complete."
