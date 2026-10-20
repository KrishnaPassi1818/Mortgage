# ParcelFlow — Project Memory and Handoff Log

This file is the compact, persistent handoff note for the developer or coding agent working on ParcelFlow. Update it at the end of every phase. Keep decisions and current facts here; keep detailed specifications in the other documents.

## 1. Project Identity

- **Name:** ParcelFlow — Smart Parcel Tracking System
- **Purpose:** Educational parcel tracking app demonstrating Core Java, JDBC, HTTP networking, React + Vite, and small microservices.
- **Primary goal:** Simple enough to build and explain; technically meaningful enough for a Java project submission.
- **Current state:** Phase 1 complete — repo scaffold, schema, Maven projects, DB verified. No business-logic endpoints yet (Phase 2+ pending).
- **Current version:** 1.0 documentation baseline + Phase 0/1 implementation verified 2026-10-20.

## 2. Locked Technology Decisions

| Area | Decision |
|---|--- |
| Runtime | Java 21 |
| Java build/dependencies | Maven 3.9.9 |
| Inbound HTTP | JDK `com.sun.net.httpserver.HttpServer` |
| Outbound HTTP | JDK `java.net.http.HttpClient` |
| JSON | Jackson Databind 2.15.2 + jsr310 |
| Database | MariaDB 11.8.6 (MySQL 8.x compatible) — production spec MySQL 8.x |
| Database access | JDBC + MySQL Connector/J 8.0.33 |
| Frontend | React + Vite + JavaScript (Phase 5) |
| Styling | Plain CSS/CSS Modules (Phase 5) |
| API format | REST-style HTTP + JSON |
| Services | Shipment :8081, Tracking :8082, Notification :8083 |
| Frontend dev server | Vite :5173 (Phase 5) |
| Containers | Docker Compose optional/recommended after local success (Phase 7) |
| Tests | JUnit 5.10.2; curl for API checks |

Do not introduce Spring Boot, Hibernate/JPA, Kafka, Kubernetes, Redis, or an API gateway unless the project owner explicitly approves a scope change.

## 3. Service Boundaries

- **Shipment Service:** owns parcel identity and sender/recipient/origin/destination details; owns `shipments`.
- **Tracking Service:** owns current status and tracking history; owns `shipment_status` and `tracking_events`.
- **Notification Service:** owns in-app notification records; owns `notifications`.
- Services communicate through HTTP/JSON. They must not import each other's Java classes or query each other's tables.
- For simple local setup, all service-owned tables live in one MySQL/MariaDB database `parcelflow`. This is an educational compromise, not strict production isolation.

## 4. Baseline User Journey

1. Create a shipment.
2. Receive a server-generated tracking ID.
3. Initialize the `BOOKED` tracking status and initial event (Shipment → Tracking `POST /events` with BOOKED; if fails, return `trackingInitialized:false` + retry option).
4. Look up shipment details and tracking timeline (frontend calls Shipment + Tracking in parallel).
5. Update status using the demo operator panel.
6. Tracking Service commits current status + history event in one JDBC transaction.
7. Tracking Service asks Notification Service to record an in-app notification (2s connect / 3s read timeout).
8. If Notification Service is unavailable, the status remains committed and the UI reports partial success (`notificationRecorded:false`).

## 5. Canonical Status Values

- `BOOKED`
- `IN_TRANSIT`
- `OUT_FOR_DELIVERY`
- `DELIVERED`
- `DELAYED`

Wire values uppercase. Human-readable labels in frontend only. Terminal: `DELIVERED` → no further transitions (409). See frozen transition matrix in PHASE0_AUDIT_REPORT.md §4.4.

## 6. Canonical API Prefixes

- Shipment Service: `/api/v1/shipments`
- Tracking Service: `/api/v1/tracking`
- Notification Service: `/api/v1/notifications`
- All services: `GET /health` → `{"service":"<name>","status":"UP"}`
- Error JSON shape:
```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Readable message."
  }
}
```
Codes: `VALIDATION_ERROR`, `SHIPMENT_NOT_FOUND`, `TRACKING_NOT_FOUND`, `INVALID_STATUS`, `INVALID_TRANSITION`, `DOWNSTREAM_UNAVAILABLE`, `INTERNAL_ERROR`.

See `Architecture.md` for full endpoint contracts.

## 7. Repository Layout

```text
parcelflow/
  database/schema.sql
  shipment-service/      # port 8081
  tracking-service/      # port 8082
  notification-service/  # port 8083
  frontend/              # Vite :5173 (Phase 5)
  docker-compose.yml     # Phase 7
  docs/                  # PRD, Architecture, Rules, Design, Tasks, Memory, PHASE0_AUDIT_REPORT
  .env.example
  .gitignore
  README.md
```

Docs also available at root via `uploads/` (original spec upload). Source of truth is `docs/`.

## 8. Important Engineering Constraints

- JDBC SQL must use prepared statements — demonstrated in `*Repository.java` with `?` placeholders and try-with-resources.
- Use try-with-resources for `Connection`, `PreparedStatement`, `ResultSet`.
- Keep SQL in repositories and business rules in service classes — enforced from Phase 1 skeleton.
- Use finite HTTP timeouts (2s connect / 3s read for service-to-service).
- Do not keep a JDBC transaction open during a downstream HTTP request — transaction commits, then HTTP call.
- Keep status update + event insertion atomic — `TrackingRepository.updateStatusAtomically` shows pattern.
- Do not expose secrets or stack traces in HTTP responses — `HttpResponses.sendError` uses fixed shape.
- Frontend `VITE_*` vars are public; never put `DB_PASSWORD` there.
- Do not claim in-app notifications are emails or SMS.
- Configure CORS for `FRONTEND_ORIGIN` — implemented in `HttpResponses.addCors`.
- Container-to-container URLs must use Compose service names, not `localhost` — documented in `.env.example`.

## 9. Environment Variable Names

```text
DB_HOST=localhost
DB_PORT=3306
DB_NAME=parcelflow
DB_USER=parcelflow_app
DB_PASSWORD=change_me_locally  (placeholder, never commit real value)
SHIPMENT_SERVICE_PORT=8081
TRACKING_SERVICE_PORT=8082
NOTIFICATION_SERVICE_PORT=8083
SHIPMENT_SERVICE_URL=http://localhost:8081
TRACKING_SERVICE_URL=http://localhost:8082
NOTIFICATION_SERVICE_URL=http://localhost:8083
FRONTEND_ORIGIN=http://localhost:5173
VITE_SHIPMENT_API_URL=http://localhost:8081
VITE_TRACKING_API_URL=http://localhost:8082
VITE_NOTIFICATION_API_URL=http://localhost:8083
```
Docker overrides: `DB_HOST=mysql`, `*_SERVICE_URL=http://<service-name>:808x`.

## 10. Phase Status

- [x] Phase 0 — Scope and contracts frozen (2026-10-20)
- [x] Phase 1 — Repository/database foundation (2026-10-20) — verified
- [ ] Phase 2 — Shipment Service
- [ ] Phase 3 — Notification Service
- [ ] Phase 4 — Tracking Service
- [ ] Phase 5 — React + Vite frontend
- [ ] Phase 6 — Integration and regression
- [ ] Phase 7 — Docker, docs, and final demo

**Last completed phase:** Phase 1 — Repository/database foundation  
**Last verified build/test:** 2026-10-20 — all three services compile + unit tests pass (see below)  
**Current blocker:** None for Phase 1. MariaDB 11.8.6 running via `mysqld --user=mysql` (systemd inactive due to sandbox policy). Docker unavailable — deferred per baseline.  
**Next action:** Phase 2 — Implement Shipment Service (`POST /api/v1/shipments`, `GET /api/v1/shipments/{id}`, `/health` already done, validation, tracking ID generation, persistence).

## 11. Decision Log

| ID | Decision | Reason |
|---|---|---|
| D-001 | Use JDK HttpServer rather than Spring Boot for baseline | Makes Java HTTP/networking concepts visible and keeps setup light |
| D-002 | Use JDBC rather than JPA/Hibernate | Explicitly demonstrates SQL, prepared statements, and transactions |
| D-003 | Use HTTP/JSON for service-to-service calls | Straightforward to inspect with curl/Postman and explain in a viva |
| D-004 | Use one MySQL instance with service-owned tables | Reduces local setup complexity while retaining logical service ownership |
| D-005 | Use in-app notification records only | Avoids external provider setup and secrets |
| D-006 | Use synchronous notification call with partial-success handling | Easy to explain; reliable retries/outbox remain a stretch goal |
| D-007 | Build locally before Docker | Keeps troubleshooting focused; containers are added after the baseline works |
| D-008 | Strict ownership: Shipment returns NO currentStatus (Phase 0 Q2 A) | Keeps service boundaries clean; Architecture is source of truth |
| D-009 | Shipment create returns 201 with trackingInitialized:false on Tracking failure (Phase 0 Q1 A) | Resilient demo; provides explicit retry path without rolling back valid shipment |
| D-010 | Transition matrix: BOOKED→IN_TRANSIT/DELAYED, IN_TRANSIT→OUT_FOR_DELIVERY/DELAYED/DELIVERED, OUT_FOR_DELIVERY→DELIVERED/DELAYED/IN_TRANSIT, DELAYED→IN_TRANSIT/OUT_FOR_DELIVERY/DELIVERED, DELIVERED terminal | Permissive but safe; covers PRD demo flow |
| D-011 | Timestamps: DB TIMESTAMP DEFAULT CURRENT_TIMESTAMP (UTC), API ISO-8601 via Jackson JavaTimeModule | Consistent, viva-explainable |

## 12. Known Limitations

- No real authentication/authorization in baseline.
- No real carrier, GPS, email, SMS integration.
- Synchronous notification delivery not guaranteed; failed notifications need manual retry unless outbox added (Phase 7 stretch).
- Shared MySQL instance is a learning convenience; no cross-service FKs enforced by DB.
- Health endpoints are basic liveness checks, not full readiness (DB not probed in /health — logged at startup).
- Project is a local educational demo, not production logistics software.
- Docker not available in current sandbox; container workflow documented but not yet verified (Phase 7).
- MariaDB 11.8.6 used in sandbox as MySQL 8.x compatible drop-in; production spec remains MySQL 8.x.

## 13. Handoff Notes

### 2026-10-20 — Phase 0 — Scope and contracts frozen
Owner: Coordinator (Agent)
Files changed: `PHASE0_AUDIT_REPORT.md` (new, 400+ lines), `docs/PHASE0_AUDIT_REPORT.md` (copy)
Implementation decisions:
- Inspected empty workspace, no git repo, Java 11 present (need 21), Maven missing, MySQL missing, Node 20 OK.
- Compared 6 docs for contradictions: C1 BOOKED init (material), C2 currentStatus ownership (material), C3 endpoints/JSON (editorial), C4 table ownership (consistent), C5 local vs Docker URLs (clarification), C6 env vars/ports (consistent).
- Proposed frozen baseline: strict ownership (Shipment NO currentStatus), Shipment→Tracking BOOKED via POST /events with trackingInitialized flag on failure, transition matrix, error shape, dual-mode URLs.
- User approved Q1 A, Q2 A, Q3 A, Q4 A.
Commands run:
- `java -version`, `mvn -version`, `node -v`, `mysql --version`, `docker --version`, `git status` (all documented)
- `ls -la /home/user && ls -la /home/user/uploads`
Tests run and actual results:
- No code to test yet; inspection only. Reported actual tool versions (see PHASE0_AUDIT_REPORT §1.2).
Known issues: Java 21/Maven/MySQL missing at audit time; resolved in Phase 1.
API/schema changes: None yet — froze proposed contracts, pending implementation.
Next action: Install Java 21 + Maven + MariaDB, git init, scaffold repo, create schema.sql, pom.xml, verify compile.

### 2026-10-20 — Phase 1 — Repository/database foundation
Owner: Coordinator
Files changed:
- New: `.gitignore`, `.env.example`, `README.md`, `database/schema.sql`, `docs/PHASE0_AUDIT_REPORT.md`
- New: `shipment-service/pom.xml`, `tracking-service/pom.xml`, `notification-service/pom.xml` (each Java 21, Jackson 2.15.2, MySQL Connector/J 8.0.33, JUnit 5.10.2)
- New: `*/src/main/java/com/parcelflow/*/config/Config.java` (env var defaults, safe secrets handling)
- New: `*/src/main/java/com/parcelflow/*/util/Json.java` (Jackson + JavaTimeModule)
- New: `*/src/main/java/com/parcelflow/*/util/HttpResponses.java` (consistent JSON + CORS)
- New: `*/src/main/java/com/parcelflow/*/repository/Database.java` (DriverManager, checkConnection smoke test)
- New: `*/src/main/java/com/parcelflow/*/http/HealthHandler.java` (GET /health)
- New: `shipment-service/src/main/java/com/parcelflow/shipment/model/Shipment.java`, `dto/CreateShipmentRequest.java`, `dto/ShipmentResponse.java`
- New: `shipment-service/src/main/java/com/parcelflow/shipment/repository/ShipmentRepository.java` (prepared statements, try-with-resources)
- New: `tracking-service/src/main/java/com/parcelflow/tracking/model/TrackingEvent.java`, `repository/TrackingRepository.java` (transaction example)
- New: `notification-service/src/main/java/com/parcelflow/notification/model/Notification.java`, `repository/NotificationRepository.java`
- New: `*/src/main/java/com/parcelflow/*/*Application.java` (HttpServer on 0.0.0.0, DB check at startup)
- New: `*/src/test/java/com/parcelflow/*/ConfigTest.java` (sanity unit tests)
- Updated: `docs/Memory.md` (this file), `README.md` with Phase 1 quick-start
Implementation decisions:
- Used MariaDB 11.8.6 as MySQL-compatible in sandbox (apt default-mysql-server); production still MySQL 8.x.
- Kept one shared DB `parcelflow` with service-owned tables, no FKs, idempotent `CREATE TABLE IF NOT EXISTS`.
- Duplicated DTO/Config/Json per service (no shared library) per Architecture §11.
- CORS set to `FRONTEND_ORIGIN` env var, handling OPTIONS with 204.
Commands run:
- `sudo apt-get install openjdk-21-jdk maven` → Java 21.0.12.1, Maven 3.9.9
- `sudo apt-get install default-mysql-server` → MariaDB 11.8.6, started via `sudo mysqld --user=mysql &`
- `git init -b main`, `git config user.*`
- `mysql -u parcelflow_app ... < database/schema.sql` — verified 4 tables exist, idempotent on second run
- `mvn clean compile -q` per service — all 3 pass after fixing Json.java IOException signature
- `mvn test` per service — 2 + 1 + 1 = 4 tests pass (Config defaults + JDBC URL checks)
- JDBC smoke: `DBCheck.java` via `DriverManager.getConnection` → VERSION 11.8.6, shipments count 0
- Service smoke: `mvn package`, run each `*Application` with `DB_PASSWORD=change_me_locally` on 8081/8082/8083, `curl http://localhost:808x/health` → 200 {"service":"...","status":"UP"} with CORS headers; preflight 204 OK
Tests run and actual results:
- Schema loads clean: PASS (4 tables, idempotent)
- Each service compiles independently: PASS (shipment-service, tracking-service, notification-service — all `mvn clean compile` exit 0)
- Database connectivity verified: PASS (via mysql client + JDBC checkConnection + service startup logs showing DB connected)
- No real credentials committed: PASS (only placeholder in .env.example, DB_PASSWORD defaults to "" in Config)
- Commands documented: PASS (README.md quick-start + this log)
- Clean DB re-init: PASS (DROP + CREATE + < schema.sql twice)
Known issues:
- `systemctl start mariadb` blocked by policy-rc.d; using manual `mysqld` — works but not persistent across sandbox restarts. Documented for user: `sudo mysqld --user=mysql &`.
- Docker unavailable — Phase 7 deferred.
- Health endpoint does not probe DB (by design for Phase 1 liveness only).
API/schema changes: None — implemented frozen schema exactly; API contracts unchanged (only /health implemented so far).
Next action: Phase 2 — Shipment Service full implementation (validation, tracking ID generation PF+YY+6alnum, POST/GET shipments, integrate Tracking BOOKED call, unit + API tests, 201/400/404 checks, no SQL in handlers).
