#!/usr/bin/env bash
set -e

# Change to script's directory (project root)
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

echo "========================================="
echo "   ParcelFlow System Runner"
echo "========================================="

# 1. Load environment variables from .env if present
if [ -f .env ]; then
  echo "[1/4] Loading .env configuration..."
  set -a
  source .env
  set +a
else
  echo "[1/4] .env not found! Creating from .env.example..."
  cp .env.example .env
  set -a
  source .env
  set +a
fi

# Set defaults if not provided
export DB_HOST="${DB_HOST:-localhost}"
export DB_PORT="${DB_PORT:-3306}"
export DB_NAME="${DB_NAME:-parcelflow}"
export DB_USER="${DB_USER:-parcelflow_app}"
export DB_PASSWORD="${DB_PASSWORD:-change_me_locally}"

export SHIPMENT_SERVICE_PORT="${SHIPMENT_SERVICE_PORT:-8081}"
export TRACKING_SERVICE_PORT="${TRACKING_SERVICE_PORT:-8082}"
export NOTIFICATION_SERVICE_PORT="${NOTIFICATION_SERVICE_PORT:-8083}"

export SHIPMENT_SERVICE_URL="${SHIPMENT_SERVICE_URL:-http://localhost:$SHIPMENT_SERVICE_PORT}"
export TRACKING_SERVICE_URL="${TRACKING_SERVICE_URL:-http://localhost:$TRACKING_SERVICE_PORT}"
export NOTIFICATION_SERVICE_URL="${NOTIFICATION_SERVICE_URL:-http://localhost:$NOTIFICATION_SERVICE_PORT}"

# 2. Check for port conflicts before starting
echo "[2/4] Checking ports..."
CONFLICT=0
for port in "$SHIPMENT_SERVICE_PORT" "$TRACKING_SERVICE_PORT" "$NOTIFICATION_SERVICE_PORT" 5173; do
  if ss -tulpn 2>/dev/null | grep -q ":$port "; then
    echo "  [WARNING] Port $port is already in use by another process!"
    CONFLICT=1
  fi
done

if [ "$CONFLICT" -eq 1 ]; then
  echo ""
  echo "Error: One or more target ports are occupied."
  echo "Please stop any conflicting processes (e.g. check 'ss -tulpn | grep -E \"$SHIPMENT_SERVICE_PORT|$TRACKING_SERVICE_PORT|$NOTIFICATION_SERVICE_PORT|5173\"') and retry."
  exit 1
fi

LOG_DIR="$ROOT_DIR/logs"
mkdir -p "$LOG_DIR"

cleanup() {
  echo ""
  echo "Shutting down ParcelFlow services..."
  kill $(jobs -p) 2>/dev/null || true
  wait $(jobs -p) 2>/dev/null || true
  echo "All services stopped."
}
trap cleanup SIGINT SIGTERM EXIT

# 3. Start Microservices in background
echo "[3/4] Starting backend microservices..."

echo "  -> Starting Notification Service (:$NOTIFICATION_SERVICE_PORT)..."
(cd notification-service && mvn exec:java) > "$LOG_DIR/notification.log" 2>&1 &
NOTIFICATION_PID=$!

echo "  -> Starting Shipment Service (:$SHIPMENT_SERVICE_PORT)..."
(cd shipment-service && mvn exec:java) > "$LOG_DIR/shipment.log" 2>&1 &
SHIPMENT_PID=$!

echo "  -> Starting Tracking Service (:$TRACKING_SERVICE_PORT)..."
(cd tracking-service && mvn exec:java) > "$LOG_DIR/tracking.log" 2>&1 &
TRACKING_PID=$!

echo "Waiting for backend services to be healthy..."
for i in {1..30}; do
  if curl -s "http://localhost:$SHIPMENT_SERVICE_PORT/health" | grep -q "UP" && \
     curl -s "http://localhost:$TRACKING_SERVICE_PORT/health" | grep -q "UP" && \
     curl -s "http://localhost:$NOTIFICATION_SERVICE_PORT/health" | grep -q "UP"; then
    echo "  [✓] All backend services are healthy and running!"
    break
  fi
  sleep 1
  if [ $i -eq 30 ]; then
    echo "  [!] Warning: Timed out waiting for backend services. Check $LOG_DIR/*.log for errors."
  fi
done

# 4. Start Frontend
echo "[4/4] Starting React + Vite Frontend (:5173)..."
echo "========================================="
echo "ParcelFlow is running!"
echo "  - Frontend:             http://localhost:5173"
echo "  - Shipment Service:     $SHIPMENT_SERVICE_URL (Health: $SHIPMENT_SERVICE_URL/health)"
echo "  - Tracking Service:     $TRACKING_SERVICE_URL (Health: $TRACKING_SERVICE_URL/health)"
echo "  - Notification Service: $NOTIFICATION_SERVICE_URL (Health: $NOTIFICATION_SERVICE_URL/health)"
echo "  - Logs:                 $LOG_DIR/"
echo "Press Ctrl+C to stop all services."
echo "========================================="

cd frontend && npm run dev
