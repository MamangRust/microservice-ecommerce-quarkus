#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

# ─── Kill any existing Java services ─────────────────────────────────────────
echo "🔪 Killing existing Java services..."
ps aux | grep java | grep -v grep | grep -E 'target/quarkus|quarkus-run' | awk '{print $2}' | xargs kill -9 2>/dev/null || true
sleep 2

# ─── Environment ─────────────────────────────────────────────────────────────
export DB_HOST="${DB_HOST:-localhost}"
export DB_PORT="${DB_PORT:-5432}"
export DB_NAME="${DB_NAME:-PAYMENT_GATEWAY}"
export DB_USER="${DB_USER:-DRAGON}"
export DB_PASS="${DB_PASS:-DRAGON}"

export REDIS_HOSTS="${REDIS_HOSTS:-redis://:dragon_knight@localhost:6379}"
export REDIS_CLIENT_TYPE="${REDIS_CLIENT_TYPE:-standalone}"

export KAFKA_BROKERS="${KAFKA_BROKERS:-localhost:9092}"
export KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}"

export CLICKHOUSE_HOST="${CLICKHOUSE_HOST:-localhost}"
export CLICKHOUSE_PORT="${CLICKHOUSE_PORT:-8123}"
export CLICKHOUSE_USER="${CLICKHOUSE_USER:-default}"
export CLICKHOUSE_PASSWORD="${CLICKHOUSE_PASSWORD:-none}"

export GRPC_STATS_READER_PORT="${GRPC_STATS_READER_PORT:-9029}"

LOG_DIR=/tmp/ecommerce-host-logs
mkdir -p "$LOG_DIR"
rm -f "$LOG_DIR"/*.log "$LOG_DIR"/*.pid

JAVA_OPTS="${JAVA_OPTS:--Xmx256m -Xms128m}"

start_svc() {
    local name=$1 module=$2 http=$3 grpc=${4:-}
    # Use setsid to fully detach the process so it survives parent shell exit
    setsid env \
        DB_HOST="$DB_HOST" DB_PORT="$DB_PORT" DB_NAME="$DB_NAME" DB_USER="$DB_USER" DB_PASS="$DB_PASS" \
        REDIS_HOSTS="$REDIS_HOSTS" REDIS_CLIENT_TYPE="$REDIS_CLIENT_TYPE" \
        KAFKA_BROKERS="$KAFKA_BROKERS" KAFKA_BOOTSTRAP_SERVERS="$KAFKA_BOOTSTRAP_SERVERS" \
        CLICKHOUSE_HOST="$CLICKHOUSE_HOST" CLICKHOUSE_PORT="$CLICKHOUSE_PORT" \
        CLICKHOUSE_USER="$CLICKHOUSE_USER" CLICKHOUSE_PASSWORD="$CLICKHOUSE_PASSWORD" \
        GRPC_STATS_READER_PORT="$GRPC_STATS_READER_PORT" \
        java $JAVA_OPTS -jar "$module/target/quarkus-app/quarkus-run.jar" \
        >"$LOG_DIR/$name.log" 2>&1 &
    local pid=$!
    echo $pid > "$LOG_DIR/$name.pid"
    for i in $(seq 1 40); do
        if timeout 1 bash -c "</dev/tcp/127.0.0.1/$http" 2>/dev/null; then
            echo "  ✅ $name (http=$http grpc=$grpc) [pid=$pid]"
            return 0
        fi
        # Check if process is still alive
        if ! kill -0 $pid 2>/dev/null; then
            echo "  ❌ $name (http=$http grpc=$grpc) — DIED (check $LOG_DIR/$name.log)"
            tail -5 "$LOG_DIR/$name.log" 2>/dev/null || true
            return 1
        fi
        sleep 1
    done
    echo "  ❌ $name (http=$http grpc=$grpc) — TIMEOUT"
    return 1
}

# ─── Wave 1: Core identity (no deps) ────────────────────────────────────────
echo ""
echo "🚀 Starting Java services..."
echo "  Wave 1: user + role..."
start_svc user user 8091 9011
start_svc role role 8086 9006

# ─── Wave 2: Auth + merchant (depends on identity) ───────────────────────────
echo "  Wave 2: auth + merchant..."
start_svc auth auth 8092 9012
start_svc merchant merchant 8085 9005
start_svc merchant_detail merchant_detail 8087 9022
start_svc merchant_business merchant_business 8101 9021
start_svc merchant_award merchant_award 8102 9020
start_svc merchant_policy merchant_policy 8088 9023

# ─── Wave 3: category + product (depends on merchant) ────────────────────────
echo "  Wave 3: category + product..."
start_svc category category 8094 9014
start_svc product product 8095 9015

# ─── Wave 4: order (depends on identity + merchant + catalog) ────────────────
echo "  Wave 4: order..."
start_svc order order 8098 9018
start_svc order_item order_item 8103 9019

# ─── Wave 5: transaction (depends on order + merchant) ──────────────────────
echo "  Wave 5: transaction..."
start_svc transaction transaction 8089 9009

# ─── Wave 6: rest (no cross-schema deps) ────────────────────────────────────
echo "  Wave 6: cart + review + banner + slider + email..."
start_svc cart cart 8083 9003
start_svc review review 8097 9017
start_svc review_detail review_detail 8099 9027
start_svc shipping_address shipping_address 8100 9028
start_svc banner banner 8093 9013
start_svc slider slider 8104 9016
start_svc email-service email-service 8105

# ─── Wave 7: Gateway + stats ────────────────────────────────────────────────
echo "  Wave 7: gateway + stats..."
start_svc stats-writer stats-writer 8106
sleep 5
start_svc stats-reader stats-reader 8096 9029
sleep 3
start_svc gateway gateway 8080

# ─── Verify ──────────────────────────────────────────────────────────────────
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Service Summary"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
HEALTH=$(curl -s http://localhost:8080/q/health/ready 2>/dev/null)
if echo "$HEALTH" | grep -q '"UP"'; then
    echo "  ✅ Gateway ready at http://localhost:8080"
else
    echo "  ❌ Gateway NOT ready!"
fi

# Count running services
RUNNING=$(ps aux | grep 'quarkus-app' | grep -v grep | wc -l)
echo "  📊 Running services: $RUNNING"
echo ""
echo "  Logs: $LOG_DIR/"
echo "  To run e2e: bash load-test/run-hurl-e2e.sh"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
