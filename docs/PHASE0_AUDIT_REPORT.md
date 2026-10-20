# Phase 0 — Audit, Requirements & Contract Freeze
**Date:** 2026-10-20  
**Project:** ParcelFlow — Smart Parcel Tracking System  
**Environment:** E2B Sandbox (Debian trixie), Ludhiana, IN

---

## 1. Repository & Environment Inspection

### 1.1 Repository Status
- **Workspace root:** `/home/user` — **empty except `uploads/`** containing the six spec docs.
- **Git:** NOT initialized (`fatal: not a git repository`). No commits, no history to preserve.
- **Existing implementation:** **None** — only documentation baseline. No `shipment-service/`, `tracking-service/`, `notification-service/`, `frontend/`, `database/`, `pom.xml`, or `schema.sql` exists yet.
- **AGENT_PROMPT.md:** Not present under `docs/` or root — no additional guidance to reconcile.

**Conclusion:** Clean slate. Safe to scaffold from scratch without overwriting work.

### 1.2 Installed Toolchain

| Tool | Installed Version | Required | Status |
|------|-------------------|----------|--------|
| **Java** | `openjdk 11` (11+28) at `/usr/lib/jvm/jdk-11` | **Java 21 LTS** | ❌ MISMATCH — must install `openjdk-21-jdk` |
| **Maven** | not found (`mvn: command not found`) | **Maven 3.9+** | ❌ Missing |
| **Node** | `v20.20.2` | Node 18+ (Vite) | ✅ OK |
| **npm** | `10.8.2` | npm 9+ | ✅ OK |
| **MySQL client/server** | not found | MySQL 8.x | ❌ Missing (no server, no client) |
| **Docker / Docker Compose** | not found | Docker 24+ (optional, post-local) | ❌ Missing — local-first workflow will be used per Rules §7 |
| **Git** | available (implied by `git status` error) | Git | ✅ OK |

**No database service is reachable.** Phase 1 DB smoke tests will be marked **not run** until MySQL is installed or an alternative is approved.

### 1.3 Documents Read
All six specs read completely from `/home/user/uploads/`:
- `prd.md` (v1.0, 180 lines)
- `Architecture.md` (v1.0, 402 lines)
- `Rules.md` (116 lines)
- `Design.md` (206 lines)
- `Tasks.md` (242 lines)
- `Memory.md` (193 lines)

---

## 2. Contradiction Analysis (6 Required Checks)

Per your instruction §1.5, compared across all six docs:

### C1 — Shipment Creation & First `BOOKED` Event Initialization ⚠️ MATERIAL

| Source | Statement |
|--------|-----------|
| **Architecture §6.1.5** | *"Service creates a `BOOKED` status record and initial event through a documented initialization path. Preferred: Shipment Service calls Tracking Service after creating shipment; if this call fails, return a clear error and provide a retry/admin recovery option."* |
| **Tasks Phase 0** | *"Decide exact shipment initialization flow: after shipment insert, initialize `BOOKED` state through Tracking Service; document recovery if the call fails."* |
| **Memory §4** | *"Initialize the `BOOKED` tracking status and initial event."* (implies it happens as part of creation) |
| **Agent Prompt §4** | *"Initial `BOOKED` status/event initialization using the frozen contract"* owned by Tracking Service; critical rule: *"Do not hold a JDBC transaction open while making an HTTP call."* |
| **PRD FR-3/FR-4** | History must include initial `BOOKED` event. |

**Problem:** No doc defines the **exact HTTP endpoint** for initialization. Architecture suggests Shipment→Tracking call but never names the path. Risk of circular dependency if Tracking also needs to verify shipment exists (Phase 4 rule: verify via Shipment Service HTTP).

**Proposed Resolution (SMALLEST CONSISTENT):**
1. **Primary flow (local dev):** Shipment Service `POST /api/v1/shipments` does:
   - Insert into `shipments` table, generate `trackingId`.
   - **Synchronously call** `POST http://TRACKING_SERVICE_URL/api/v1/tracking/{trackingId}/events` with body `{"status":"BOOKED","location":"<origin>","note":"Shipment registered"}` using `java.net.http.HttpClient` with 2s connect / 3s request timeout.
   - If Tracking call succeeds → return `201` with shipment + note that tracking initialized.
   - If Tracking call fails/timeout → **still return `201`** but with field `trackingInitialized:false` and log warning. Provide admin retry via `POST /api/v1/tracking/{trackingId}/events` with `BOOKED` (idempotent — Tracking will reject duplicate BOOKED if already exists with `409` or accept as no-op). This avoids failing the shipment creation entirely while making the failure visible.

2. **Tracking Service** allows `BOOKED` as the **only** valid first status when no `shipment_status` row exists. Subsequent `BOOKED` is rejected `409`.

3. **Alternative fallback (documented):** Operator can manually POST `BOOKED` to Tracking to recover a missing initial event.

This keeps transaction boundaries clean (no DB tx open during HTTP) and satisfies Architecture's "return clear error + retry option" without rolling back a valid shipment.

**Requires your approval** — this is an API behavior decision.

### C2 — Ownership of `currentStatus` & Tracking History ⚠️ MATERIAL

| Source | Statement |
|--------|-----------|
| **Architecture §3.1** | Shipment Service owns shipment details. Tracking owns status/history. *"Suggested: Shipment Service owns `shipments`; Tracking owns `shipment_status` + `tracking_events`."* |
| **Architecture §5 Shipment response note** | *"Shipment Service must not claim that status is owned by it. `currentStatus` may be omitted ... Preferred: shipment response contains shipment details only; tracking endpoint returns current status. Pick one shape and keep it consistent."* |
| **Architecture §5 Example** | Shipment create response **includes** `"currentStatus":"BOOKED"` — contradicts the "preferred omit" note. |
| **PRD FR-2** | *"Return shipment details and current status"* on lookup — implies shipment lookup includes status. |
| **Agent Prompt §3** | Shipment Service *"Exposes shipment lookup and /health"*; Tracking *"Owns current shipment status and tracking history."* |

**Problem:** If Shipment Service strictly omits `currentStatus`, PRD FR-2 is violated; if it includes it, service ownership is violated (would require cross-table read or duplication).

**Proposed Resolution (Architecture as source of truth per Rules §1):**
- **Freeze to STRICT OWNERSHIP:**
  - `GET /api/v1/shipments/{trackingId}` returns **only** shipment-owned fields: `trackingId, senderName, recipientName, origin, destination, createdAt`. **No `currentStatus`.**
  - `GET /api/v1/tracking/{trackingId}` returns `trackingId, currentStatus, events[]`.
  - `POST /api/v1/shipments` returns `trackingId, senderName, recipientName, origin, destination, createdAt, trackingInitialized (bool)` — **no `currentStatus`** to avoid ownership confusion.
- **Frontend** will call **both** services to assemble the full view (as Architecture §6.3 already describes: *"React requests shipment details from Shipment Service; React requests current status and timeline from Tracking Service"*).
- Document this choice in `Architecture.md` and `README.md` so viva explanation is consistent.

**Requires your approval** — changes the example JSON in Architecture §5.

### C3 — Exact Endpoint Paths & JSON Fields ✅ MOSTLY CONSISTENT

All docs agree on:

| Service | Method | Path |
|---------|--------|------|
| All | GET | `/health` |
| Shipment | POST | `/api/v1/shipments` |
| Shipment | GET | `/api/v1/shipments/{trackingId}` |
| Tracking | GET | `/api/v1/tracking/{trackingId}` |
| Tracking | POST | `/api/v1/tracking/{trackingId}/events` |
| Notification | POST | `/api/v1/notifications` |
| Notification | GET | `/api/v1/notifications?trackingId={id}` |

**Minor editorial inconsistencies resolved:**
- **JSON field names:** Freeze to **camelCase** as in Architecture examples: `senderName, recipientName, origin, destination, trackingId, createdAt, eventTime, status, location, note, title, message, deliveryState, currentStatus, notificationRecorded`.
- **Timestamps:** Store as `TIMESTAMP DEFAULT CURRENT_TIMESTAMP` in MySQL (UTC); expose as ISO-8601 `yyyy-MM-dd'T'HH:mm:ss` (e.g., `2026-10-20T10:30:00`) via Jackson `JavaTimeModule` — no timezone suffix in baseline for simplicity, document as UTC.
- **Error shape:** Freeze to Architecture §9:
  ```json
  {"error":{"code":"SHIPMENT_NOT_FOUND","message":"No shipment was found..."}}
  ```
  Never include `stackTrace`, `sql`, or `env`.

### C4 — Database Table Ownership ✅ CONSISTENT

All docs agree:
- `shipments` → Shipment Service
- `shipment_status` + `tracking_events` → Tracking Service
- `notifications` → Notification Service
- Shared MySQL instance `parcelflow` allowed; **no cross-service FKs**, logical `tracking_id` reference only.
- Phase 1 will create `database/schema.sql` with these tables + indexes.

**Frozen schema columns** (explicit, no `SELECT *`):

```sql
-- shipments (owned by shipment-service)
id BIGINT AUTO_INCREMENT PK,
tracking_id VARCHAR(32) UNIQUE NOT NULL,
sender_name VARCHAR(100) NOT NULL,
recipient_name VARCHAR(100) NOT NULL,
origin VARCHAR(120) NOT NULL,
destination VARCHAR(120) NOT NULL,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
INDEX idx_tracking_id (tracking_id)

-- shipment_status (owned by tracking-service)
tracking_id VARCHAR(32) PK,
current_status VARCHAR(32) NOT NULL,
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

-- tracking_events (owned by tracking-service)
id BIGINT AUTO_INCREMENT PK,
tracking_id VARCHAR(32) NOT NULL,
status VARCHAR(32) NOT NULL,
location VARCHAR(120) NULL,
note VARCHAR(255) NULL,
event_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
INDEX idx_tracking_events_tracking_id (tracking_id),
INDEX idx_tracking_events_time (event_time)

-- notifications (owned by notification-service)
id BIGINT AUTO_INCREMENT PK,
tracking_id VARCHAR(32) NOT NULL,
title VARCHAR(120) NOT NULL,
message VARCHAR(255) NOT NULL,
status VARCHAR(32) NOT NULL,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
delivery_state VARCHAR(20) NOT NULL DEFAULT 'RECORDED',
INDEX idx_notifications_tracking_id (tracking_id)
```

### C5 — Local vs Docker Service URLs ⚠️ CLARIFICATION NEEDED

| Source | Statement |
|--------|-----------|
| **Architecture §10** | Local: `SHIPMENT_SERVICE_URL=http://localhost:8081` etc. |
| **Architecture §10 note** | *"For Docker Compose, service URLs must use Compose service names, not `localhost`"* |
| **Rules §5** | Container-to-container URLs must use Compose DNS names |
| **Agent Prompt §3** | Frontend uses `VITE_*` vars for browser → service calls (always localhost:808x in local dev) |

**No contradiction**, but need to freeze dual-mode config:

- **Local dev (default):** All `*_SERVICE_URL` = `http://localhost:808x`; `FRONTEND_ORIGIN=http://localhost:5173`; `DB_HOST=localhost`
- **Docker Compose:** `SHIPMENT_SERVICE_URL=http://shipment-service:8081`, `TRACKING_SERVICE_URL=http://tracking-service:8082`, `NOTIFICATION_SERVICE_URL=http://notification-service:8083`, `DB_HOST=mysql`
- Frontend `VITE_*` **always** `http://localhost:808x` even in Docker (browser runs outside Compose network).

Will document both in `.env.example` with comments.

### C6 — Environment Variable Names & Ports ✅ CONSISTENT

All docs agree on ports: **8081 / 8082 / 8083 / 5173**. Env vars: `DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, SHIPMENT_SERVICE_PORT, TRACKING_SERVICE_PORT, NOTIFICATION_SERVICE_PORT, SHIPMENT_SERVICE_URL, TRACKING_SERVICE_URL, NOTIFICATION_SERVICE_URL, FRONTEND_ORIGIN, VITE_SHIPMENT_API_URL, VITE_TRACKING_API_URL, VITE_NOTIFICATION_API_URL`.

No conflicts. Freeze as listed. Add safe defaults for non-secrets; secrets only via env/untracked `.env`.

---

## 3. Additional Gaps Found

1. **Status Transition Matrix not defined anywhere.** PRD lists 5 values; Architecture lists them but no allowed transitions. **Must freeze** to prevent `409` ambiguity.
   > Proposed matrix (see §4.4 below) — permissive but safe, terminal `DELIVERED`.

2. **CORS:** Architecture says "narrowly configured for Vite origin" — freeze to `FRONTEND_ORIGIN` env var, `Access-Control-Allow-Origin: http://localhost:5173` (or `*` without credentials in dev), handle `OPTIONS`.

3. **Tracking ID format:** PRD says `PF` + date segment + random alphanumeric. Freeze to `PF` + `YY` (2-digit year) + 6-char uppercase alphanumeric (e.g., `PF26A1B2C3` → 9 chars). Uniqueness enforced by DB `UNIQUE` constraint + retry on collision.

4. **Notification `delivery_state`:** Architecture says `RECORDED` only — freeze to always `RECORDED` in baseline.

---

## 4. Frozen Baseline Proposal (Requires Approval)

### 4.1 API Contract Freeze

**Shipment Service :8081**
- `GET /health` → `200 { "service":"shipment-service","status":"UP" }`
- `POST /api/v1/shipments` → Body `{senderName, recipientName, origin, destination}` (all required, max 100/100/120/120 chars). Returns `201 {trackingId, senderName, recipientName, origin, destination, createdAt, trackingInitialized}`. `400` on validation, `500` on DB error, `503` if tracking init fails (but still created).
- `GET /api/v1/shipments/{trackingId}` → Returns shipment details or `404 {error:{code:"SHIPMENT_NOT_FOUND"}}` / `400` on blank/malformed ID.

**Tracking Service :8082**
- `GET /health` → `200 { "service":"tracking-service","status":"UP" }`
- `GET /api/v1/tracking/{trackingId}` → Returns `{trackingId, currentStatus, events:[{status,location,note,eventTime}]}` sorted oldest→newest. `404` if no status row, `400` on bad ID. Calls Shipment Service to verify existence — if Shipment returns 404, Tracking returns 404.
- `POST /api/v1/tracking/{trackingId}/events` → Body `{status, location?, note?}`. Validates status enum, transition, shipment exists. Atomically updates `shipment_status` + inserts `tracking_events` in one JDBC transaction (commit both, rollback both). After commit, calls `POST /api/v1/notifications` (2s connect, 3s read timeout). Returns `200 {trackingId, currentStatus, notificationRecorded, events}`. On notification failure → `notificationRecorded:false` with `200` (partial success, status still committed). `409` on invalid transition or `DELIVERED` terminal. `404` on unknown shipment. `503` if Shipment Service unreachable.

**Notification Service :8083**
- `GET /health` → `200 { "service":"notification-service","status":"UP" }`
- `POST /api/v1/notifications` → Body `{trackingId, status, title, message}` all required. Returns `201 {id, trackingId, title, message, status, createdAt, deliveryState:"RECORDED"}`. `400` on invalid.
- `GET /api/v1/notifications?trackingId=...` → Returns `[{...}]` (empty array if none, not 404). `400` if `trackingId` missing/blank.

**Error Shape (all services):**
```json
{ "error": { "code": "ERROR_CODE", "message": "Human-readable message." } }
```
Codes: `VALIDATION_ERROR`, `SHIPMENT_NOT_FOUND`, `TRACKING_NOT_FOUND`, `INVALID_STATUS`, `INVALID_TRANSITION`, `NOTIFICATION_FAILED`, `DOWNSTREAM_UNAVAILABLE`, `INTERNAL_ERROR`.

### 4.2 Database Schema Freeze
As in §2 C4 above. Single DB `parcelflow`, file `database/schema.sql`, idempotent with `CREATE TABLE IF NOT EXISTS`.

### 4.3 Service URLs Freeze
Local defaults:
```
DB_HOST=localhost
DB_PORT=3306
DB_NAME=parcelflow
DB_USER=parcelflow_app
DB_PASSWORD=change_me_locally
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
Docker overrides use service names (`http://shipment-service:8081` etc.) — will be in `docker-compose.yml` environment section, not `.env.example` default.

### 4.4 Status Transition Matrix Freeze

| From \ To | BOOKED | IN_TRANSIT | OUT_FOR_DELIVERY | DELIVERED | DELAYED |
|-----------|--------|------------|------------------|-----------|---------|
| *(none → init)* | ✅ (only first) | ❌ | ❌ | ❌ | ❌ |
| BOOKED | ❌ | ✅ | ❌ | ❌ | ✅ |
| IN_TRANSIT | ❌ | ❌ | ✅ | ✅ | ✅ |
| OUT_FOR_DELIVERY | ❌ | ✅ (return) | ❌ | ✅ | ✅ |
| DELAYED | ❌ | ✅ | ✅ | ✅ | ❌ |
| DELIVERED | ❌ | ❌ | ❌ | ❌ | ❌ |

- Any `BOOKED` after initial → `409 INVALID_TRANSITION`.
- `DELIVERED` terminal → `409`.
- Invalid enum → `400 INVALID_STATUS`.

### 4.5 Cross-Service Flow Freeze
1. **Create:** Frontend → Shipment `POST /shipments` → Shipment inserts → Shipment calls Tracking `POST /tracking/{id}/events` with `BOOKED` → Tracking verifies shipment via Shipment `GET` → Tracking creates `shipment_status=BOOKED` + event atomically → Tracking returns to Shipment → Shipment returns `201` to Frontend.
2. **Update:** Frontend → Tracking `POST /tracking/{id}/events` → Tracking verifies shipment exists → validates transition → DB transaction (update status + insert event) → commit → call Notification `POST` → return partial-success aware response.
3. **View:** Frontend calls Shipment `GET` + Tracking `GET` in parallel → merges for display. Notification list fetched on demand.

---

## 5. Existing State Summary

- **State:** Documentation-only baseline (v1.0). No code, no DB, no running services.
- **Blockers:**
  1. Java 21 not installed (have 11).
  2. Maven not installed.
  3. MySQL not installed/running.
  4. Docker not available (defer to Phase 7; not a blocker for local workflow).
- **Decisions pending your approval:** C1 (BOOKED init handling), C2 (strict ownership vs PRD), transition matrix.

---

## 6. Proposed Phase Order & First Implementation Step

Follow **Tasks.md** order exactly:

1. **Phase 0** (this report) → **Gate: your approval** on frozen contracts.
2. **Phase 1** — Repo & DB foundation:
   - **Files to create:** `/.gitignore`, `/.env.example`, `/README.md` skeleton, `/database/schema.sql`, `/{shipment,tracking,notification}-service/pom.xml`, `/frontend/` scaffold (Vite) after backend stabilizes.
   - **Commands:** `git init`, `sudo apt-get install openjdk-21-jdk maven mysql-server`, `mysql -u root -e "CREATE DATABASE parcelflow..."`, `mvn compile` per service.
   - **Acceptance:** schema loads clean, each `pom.xml` compiles, `Memory.md` updated.
3. **Phase 2** — Shipment Service → independent run on 8081.
4. **Phase 3** — Notification Service → independent run on 8083.
5. **Phase 4** — Tracking Service → independent run on 8082 (depends on 2+3).
6. **Phase 5** — Frontend (Vite 5173).
7. **Phase 6** — E2E integration + failure tests.
8. **Phase 7** — Docker + final docs.

**Immediate next step after your approval:** Install Java 21 + Maven, `git init`, lay skeleton files, create `schema.sql`, configure each `pom.xml`, verify `mvn compile` — all within Phase 1.

---

## 7. Questions for You (Material Decisions)

Please confirm (reply with option IDs or free text):

**Q1 — BOOKED initialization handling:**
- A) Shipment creates row, calls Tracking for `BOOKED`; if Tracking fails, still return `201` with `trackingInitialized:false` + docs retry (recommended above).
- B) Shipment creation fails with `503` if Tracking unavailable (strict, PRD says no parcel without initial state).
- C) Frontend calls both services separately (Shipment then Tracking) — simpler but shifts responsibility to UI.

**Q2 — Shipment response shape:**
- A) Strict ownership: Shipment returns NO `currentStatus` (recommended above).
- B) Convenience: Shipment returns `currentStatus:"BOOKED"` on create for immediate display.

**Q3 — Toolchain in this sandbox:**
- A) Proceed to install Java 21 + Maven + attempt MySQL install here (best for verified `Memory.md`).
- B) Scaffold code/compiles only; mark DB tests "not run — MySQL unavailable" and document local setup commands for your machine.

**Q4 — Proceed to Phase 1 scaffolding now?**
- A) Yes, after you answer Q1/Q2, start Phase 1 immediately.
- B) Hold — you want to review/edit contracts first.

---

## 8. References

- Stack locked per Rules §3: Java 21, Maven, `HttpServer`, `HttpClient`, Jackson, JDBC/MySQL Connector/J, JUnit 5, React+Vite.
- No Spring/JPA/Lombok/Kafka/Redis introduced — per baseline.
- All contracts in `Architecture.md` are source of truth for API; PRD for product scope; Rules for process.

**Awaiting your approval to freeze contracts and begin Phase 1.**
