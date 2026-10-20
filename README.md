# ParcelFlow — Smart Parcel Tracking System

> College project demonstrating Core Java 21, JDBC, HTTP (HttpServer + HttpClient), Jackson, MySQL, React + Vite, and microservices.

**Stack:** Java 21 · Maven · JDK `HttpServer`/`HttpClient` · Jackson · MySQL 8.x / MariaDB 11 · React + Vite

## Architecture (Phase 0 frozen)

```
Browser (Vite :5173) ──HTTP/JSON──▶ Shipment Service :8081 ──JDBC──┐
                      ──HTTP/JSON──▶ Tracking Service :8082 ──JDBC──┼─▶ MySQL `parcelflow`
                      ──HTTP/JSON──▶ Notification Service :8083 ──JDBC──┘
                      Tracking ──HTTP──▶ Shipment (verify exists)
                      Tracking ──HTTP──▶ Notification (record)
```

- `shipments` owned by Shipment; `shipment_status` + `tracking_events` by Tracking; `notifications` by Notification.
- No cross-service FKs; `tracking_id` is logical reference.
- Error shape: `{error:{code,message}}`. Status values: `BOOKED`, `IN_TRANSIT`, `OUT_FOR_DELIVERY`, `DELIVERED`, `DELAYED`.
- Frontend calls Shipment + Tracking in parallel; Shipment responses contain **no** `currentStatus` (strict ownership — Tracking is source of truth).

See `docs/Architecture.md` for full contracts and `docs/PHASE0_AUDIT_REPORT.md` for frozen decisions.

## Prerequisites

- Java 21 (`java -version` should show 21)
- Maven 3.9+ (`mvn -v`)
- MySQL 8.x or MariaDB 10/11 (`mysql --version`)
- Node 18+ (`node -v`, `npm -v`) for frontend (Phase 5+)

## Quick Start (Local — Phase 1)

```bash
# 1. Clone & env
git clone <repo> parcelflow && cd parcelflow
cp .env.example .env   # edit DB_PASSWORD

# 2. Start MySQL and create DB
sudo service mysql start   # or: sudo mysqld --user=mysql &
sudo mysql -e "CREATE DATABASE IF NOT EXISTS parcelflow CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
sudo mysql -e "CREATE USER IF NOT EXISTS 'parcelflow_app'@'localhost' IDENTIFIED BY 'change_me_locally'; GRANT ALL ON parcelflow.* TO 'parcelflow_app'@'localhost'; FLUSH PRIVILEGES;"
mysql -u parcelflow_app -pchange_me_locally parcelflow < database/schema.sql
# verify
mysql -u parcelflow_app -pchange_me_locally -e "SHOW TABLES FROM parcelflow;"

# 3. Build each service (from its own directory)
cd shipment-service   && mvn clean compile && cd ..
cd notification-service && mvn clean compile && cd ..
cd tracking-service   && mvn clean compile && cd ..

# 4. Run services (each in its own terminal, env vars optional)
DB_PASSWORD=change_me_locally mvn -pl shipment-service exec:java  # or: java -cp target/classes:target/dependency/* com.parcelflow.shipment.ShipmentApplication (after package)
# Expected: Shipment Service listening on :8081, GET /health → {service:"shipment-service",status:"UP"}
```

## Environment Variables

See `.env.example`. Key vars:

| Var | Default (local) | Docker |
|-----|-----------------|--------|
| `DB_HOST` | `localhost` | `mysql` |
| `DB_PORT` | `3306` | `3306` |
| `DB_NAME` | `parcelflow` | `parcelflow` |
| `DB_USER` | `parcelflow_app` | `parcelflow_app` |
| `DB_PASSWORD` | `change_me_locally` | set via secret |
| `SHIPMENT_SERVICE_URL` | `http://localhost:8081` | `http://shipment-service:8081` |
| `TRACKING_SERVICE_URL` | `http://localhost:8082` | `http://tracking-service:8082` |
| `NOTIFICATION_SERVICE_URL` | `http://localhost:8083` | `http://notification-service:8083` |
| `FRONTEND_ORIGIN` | `http://localhost:5173` | `http://localhost:5173` |
| `VITE_SHIPMENT_API_URL` | `http://localhost:8081` | same |
| `VITE_TRACKING_API_URL` | `http://localhost:8082` | same |
| `VITE_NOTIFICATION_API_URL` | `http://localhost:8083` | same |

Never put `DB_PASSWORD` in `VITE_*`.

## API Endpoints (frozen)

| Service | Method | Path | Notes |
|---------|--------|------|-------|
| All | `GET` | `/health` | `{service,status:"UP"}` |
| Shipment :8081 | `POST` | `/api/v1/shipments` | `{senderName,recipientName,origin,destination}` → `201 {trackingId,...}` |
| Shipment :8081 | `GET` | `/api/v1/shipments/{trackingId}` | `404` if unknown |
| Tracking :8082 | `GET` | `/api/v1/tracking/{trackingId}` | `{trackingId,currentStatus,events[]}` |
| Tracking :8082 | `POST` | `/api/v1/tracking/{trackingId}/events` | `{status,location?,note?}` → `200 + notificationRecorded` |
| Notification :8083 | `POST` | `/api/v1/notifications` | `{trackingId,status,title,message}` → `201` |
| Notification :8083 | `GET` | `/api/v1/notifications?trackingId=...` | `[]` if none |

See `docs/Architecture.md` §5 for request/response examples.

## Project Structure

```
parcelflow/
  database/schema.sql
  shipment-service/      # port 8081
  tracking-service/      # port 8082
  notification-service/  # port 8083
  frontend/              # Vite :5173 (Phase 5)
  docker-compose.yml     # Phase 7
  docs/                  # PRD, Architecture, Rules, Design, Tasks, Memory
  .env.example
  README.md
```

## Phase Status

- [x] Phase 0 — Scope & contracts frozen (2026-10-20)
- [ ] Phase 1 — Repo & DB foundation (in progress)
- [ ] Phase 2 — Shipment Service
- [ ] Phase 3 — Notification Service
- [ ] Phase 4 — Tracking Service
- [ ] Phase 5 — React + Vite frontend
- [ ] Phase 6 — E2E integration
- [ ] Phase 7 — Docker + final docs

See `docs/Memory.md` for handoff log.

## Troubleshooting

- **Java version wrong:** `sudo update-alternatives --config java` → pick Java 21.
- **MySQL access denied for root:** use `sudo mysql` (socket auth) to create app user.
- **Port in use:** `lsof -i :8081` or `ss -tulpn | grep 8081`, adjust `*_SERVICE_PORT`.
- **Maven compile fails:** ensure `java -version` is 21, run `mvn clean compile -e`.
- **DB connection refused:** ensure `mysqld` is running, `DB_HOST`/`DB_PORT` correct.

## License

Educational project — no real courier, payment, or SMS integration.
