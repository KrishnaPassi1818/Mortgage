# ParcelFlow — Smart Parcel Tracking System
## Product Requirements Document (PRD)

**Version:** 1.0  
**Status:** Initial implementation specification  
**Project type:** College project demonstrating Core Java, JDBC, networking, React, and microservices

---

## 1. Product Overview

ParcelFlow is a lightweight parcel-tracking application. A user can create a shipment, receive a unique tracking ID, view its current status, and inspect its tracking history. An administrator/demo operator can update the shipment status. When a status changes, the system asks the Notification Service to record a notification.

The goal is to demonstrate how several small Java services collaborate over HTTP while keeping the user experience easy to understand. This is a simulation: it does not connect to real courier companies, GPS devices, SMS gateways, or payment providers.

## 2. Problem Statement

A basic tracking experience needs shipment details, a current status, a chronological history, and a way to inform the user when the status changes. Putting every responsibility into one large application makes service boundaries and network communication difficult to demonstrate. ParcelFlow separates these responsibilities into small services with explicit APIs.

## 3. Goals

- Build a working React + Vite interface.
- Implement three independently runnable Java services.
- Demonstrate Core Java OOP, collections, exception handling, validation, and concurrency basics.
- Use JDBC and prepared SQL statements for persistence.
- Use HTTP/REST and JSON for client-to-service and service-to-service communication.
- Demonstrate a useful end-to-end flow: create shipment → update status → view history → record notification.
- Make the project straightforward to explain in a viva and simple to run locally with documented commands.
- Use Docker Compose as an optional repeatable way to run the services and MySQL.

## 4. Non-Goals

The initial version will not implement:
- Real GPS or live map tracking.
- Real email, SMS, WhatsApp, or push notifications.
- Online payments, courier integrations, route optimization, or delivery-agent apps.
- Kubernetes, Kafka, a service mesh, distributed tracing infrastructure, or complex cloud deployment.
- Production-grade identity verification or public internet exposure.
- A separate database server/container per microservice. Services own separate tables, but the college version may use one local MySQL instance.

## 5. Users and Roles

### 5.1 Visitor / Customer
- Create a demo shipment.
- Copy the generated tracking ID.
- Search for a shipment by tracking ID.
- View current status, shipment summary, and chronological tracking history.
- View notification records associated with a shipment.

### 5.2 Demo Operator
- Find a shipment.
- Move it through allowed statuses.
- Add a short location or status note.
- Inspect the response if a status update fails.

**Authentication scope:** For the first version, operator actions can be exposed as clearly labelled demo/admin actions without real authentication. Do not describe this as secure authentication. If time permits, add simple login in a separate stretch phase.

## 6. Core User Stories

1. As a customer, I can create a shipment by entering sender name, recipient name, origin, and destination so that I receive a tracking ID.
2. As a customer, I can enter a tracking ID and see whether the parcel is booked, in transit, out for delivery, delivered, or delayed.
3. As a customer, I can see status events in chronological order with timestamps and notes.
4. As a demo operator, I can update a shipment's status and add a location/note.
5. As a customer, I can see a notification record when a shipment's status changes.
6. As a developer/evaluator, I can run each backend service independently and observe what happens when another service is unavailable.

## 7. Functional Requirements

### FR-1: Shipment Creation
- Required fields: sender name, recipient name, origin, destination.
- Validate required fields and sensible maximum lengths.
- Generate a unique tracking ID on the server, for example `PF` + date segment + random alphanumeric suffix.
- Persist shipment data through JDBC.
- Return the created shipment and tracking ID.
- Do not trust a tracking ID supplied by the browser.

### FR-2: Shipment Lookup
- Look up a shipment by tracking ID.
- Return shipment details and current status.
- Return HTTP 404 when no shipment matches.
- Reject blank or malformed tracking IDs with HTTP 400.

### FR-3: Status Updates
- Support these statuses: `BOOKED`, `IN_TRANSIT`, `OUT_FOR_DELIVERY`, `DELIVERED`, `DELAYED`.
- Store a history event every time a valid update succeeds.
- Include event timestamp, optional location, and note.
- Prevent status changes after `DELIVERED`, except through an explicitly documented demo reset/admin path if implemented.
- Keep current shipment status and history consistent within the Shipment/Tracking ownership model chosen in `Architecture.md`.

### FR-4: Tracking History
- Return events oldest-to-newest, with a timestamp for each event.
- Include the initial `BOOKED` event.
- Do not fabricate location data; show a clear fallback such as “Location not provided”.

### FR-5: Notification Records
- After a successful status change, the calling service sends a JSON HTTP request to Notification Service.
- Notification Service stores a notification record and exposes it through an API.
- A notification failure must not silently undo an already committed shipment status update. Return a clear partial-success signal or log the failure; see `Architecture.md`.
- Notifications are in-app records only.

### FR-6: Error Handling
- Use consistent JSON error responses with a code and human-readable message.
- Never return SQL credentials, stack traces, or internal exception details to the browser.
- Use appropriate status codes: 200/201, 400, 404, 409, 500, and 503 as applicable.

### FR-7: Health Checks
- Each service exposes `GET /health`.
- Return service name and a simple `UP` state.
- The health endpoint should not disclose secrets.

## 8. Non-Functional Requirements

- **Clarity:** A new student should be able to explain each service's responsibility.
- **Maintainability:** Keep HTTP handlers, service/business logic, repositories, and DTOs in separate classes.
- **Reliability:** Validate inputs, use prepared statements, close JDBC resources, and handle unavailable downstream services.
- **Performance:** For a local demo, normal lookups should respond quickly; no strict production SLA is required.
- **Security basics:** Do not concatenate user input into SQL. Keep database credentials in environment variables or an untracked local config file. Configure CORS only for the frontend origin in development.
- **Accessibility:** Use labels, keyboard-accessible controls, readable contrast, and visible loading/error states.
- **Portability:** Document Windows/Linux commands; Docker Compose is the preferred cross-platform path if Docker is available.

## 9. Technology Decisions

### Backend
- **Java 21 LTS:** language/runtime; use records where useful, but keep core classes easy to explain.
- **Maven:** dependency management and repeatable compilation.
- **JDK `com.sun.net.httpserver.HttpServer`:** built-in lightweight HTTP server; no Spring Boot required.
- **Jackson Databind:** JSON serialization/deserialization for HTTP request/response bodies.
- **MySQL Connector/J:** JDBC driver for MySQL.
- **JDBC:** explicit SQL, `Connection`, `PreparedStatement`, `ResultSet`, and transaction handling.
- **JUnit 5 (recommended):** unit tests for validation and business rules.
- **No Lombok, Hibernate/JPA, or Spring in the baseline.** This keeps the Java concepts visible. If a framework is required by the course, document the change across all files first.

### Frontend
- **React:** component-based UI.
- **Vite:** development server and build tool.
- **JavaScript (ES modules):** keep the frontend accessible without adding TypeScript as another learning requirement.
- **React Router (optional):** use only if separate routes are needed; a single-page dashboard can use state instead.
- **Plain CSS or CSS Modules:** no heavy UI framework is required.
- **Fetch API:** call backend REST endpoints.

### Infrastructure
- **MySQL 8.x:** relational database.
- **Docker + Docker Compose (optional but recommended):** run MySQL and services consistently.
- **Postman or curl:** API testing.

## 10. Success Criteria / Acceptance Criteria

The project is considered complete when:
- The frontend starts using Vite and can reach the Java APIs.
- All three Java services start independently on their documented ports.
- Creating a shipment persists it and returns a unique tracking ID.
- Looking up that ID displays the correct shipment and history.
- A status update creates a history event and attempts to record a notification.
- Unknown IDs, invalid inputs, and unavailable services produce understandable errors.
- JDBC resources are closed reliably and user inputs use prepared statements.
- The README explains setup, environment variables, endpoints, architecture, and a five-minute demo.
- The application can be demonstrated without real external courier services.

## 11. Suggested Demo Script

1. Open the ParcelFlow dashboard.
2. Create a shipment from “Amritsar” to “Delhi”.
3. Copy the generated tracking ID.
4. Search that ID and show the `BOOKED` event.
5. Use the demo operator panel to set `IN_TRANSIT`, then `OUT_FOR_DELIVERY`.
6. Refresh the tracking view and show the updated timeline.
7. Open the notification panel and show the recorded status notifications.
8. Stop Notification Service, perform another valid status update, and show the documented partial-failure behavior.
9. Restart Notification Service and show each service's `/health` endpoint.

## 12. Future Enhancements (Not Baseline)

- Simple authentication and role-based access.
- Retry queue/outbox pattern for reliable notification delivery.
- Pagination and search filters.
- Email notifications using a sandbox provider.
- API gateway and service discovery.
- WebSocket updates for live tracking.
- Deployment to a cloud platform.
