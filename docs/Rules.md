# ParcelFlow — Engineering Rules and Agent Collaboration Protocol

These rules apply to every contributor and coding agent. If a change conflicts with these rules or the architecture, stop and update the relevant documentation before implementing it.

## 1. Source of Truth

Read these files before starting work:
1. `prd.md` — scope and acceptance criteria.
2. `Architecture.md` — service boundaries, APIs, database ownership, and flows.
3. `Design.md` — frontend behavior and visual system.
4. `Tasks.md` — phase order, owners, dependencies, and acceptance checks.
5. `Memory.md` — current decisions, known issues, and handoff state.

When documents disagree, do not guess. Raise the conflict and agree on one decision, then update all affected files.

## 2. Scope Control

- Implement the baseline before stretch features.
- Do not add Kafka, Kubernetes, Redis, Spring Cloud, an API gateway, ORM, or authentication frameworks without an explicit decision.
- Do not integrate real courier, email, SMS, or payment providers.
- Do not add a dependency solely for a feature that can be implemented clearly with the JDK.
- Keep the application explainable in a short viva.

## 3. Backend Rules

- Use Java 21 and Maven.
- Use `com.sun.net.httpserver.HttpServer` for inbound HTTP and `java.net.http.HttpClient` for outbound HTTP calls.
- Use Jackson for JSON parsing/serialization.
- Use MySQL Connector/J and JDBC for database access.
- Use `PreparedStatement` for all parameterized SQL. Never concatenate user input into SQL.
- Use try-with-resources for `Connection`, `PreparedStatement`, and `ResultSet`.
- Keep HTTP handlers thin. SQL belongs in repositories; business rules belong in services.
- Keep one application entry point per microservice.
- Do not import another service's Java classes or access another service's tables.
- Use explicit DTOs rather than exposing internal database models directly.
- Validate all incoming data on the server, even if React already validates it.
- Return consistent JSON errors. Never send stack traces, SQL messages, credentials, or environment variables to clients.
- Use finite HTTP connection and request timeouts.
- Handle request bodies with a reasonable size limit and reject malformed JSON.
- Ensure response streams are closed and content types/status codes are correct.
- Do not use Java object serialization over the network.

## 4. JDBC and Database Rules

- Schema changes belong in `database/schema.sql` or a numbered migration script.
- Use explicit column names in SQL; avoid `SELECT *`.
- Use transactions when updating current status and inserting tracking history.
- Commit status and history together; roll back both on failure.
- Never hold a database transaction open while making an HTTP request to another service.
- Service-owned tables must be accessed only by their owning service.
- Use unique constraints and indexes where appropriate, especially on `tracking_id`.
- Store timestamps consistently. Document whether database timestamps are UTC or local; preferred is UTC in storage and ISO-8601 in API responses.
- Credentials must come from environment variables or a local untracked config file.
- Do not commit real secrets or local database dumps containing personal information.

## 5. API and Networking Rules

- All application routes start with `/api/v1`; health checks use `/health`.
- JSON request/response bodies use UTF-8.
- Document every endpoint, method, request body, response body, and error code.
- Use HTTP status codes consistently.
- Service base URLs and ports must be configurable.
- Configure CORS only for the expected frontend origin during development; do not use unrestricted origins with credentials.
- Handle `OPTIONS` preflight if the browser needs it.
- URL-decode path/query values correctly and reject invalid path shapes.
- Do not assume a downstream service is always available.
- If notification delivery fails after a status transaction commits, preserve the committed status and return a clear partial-success response.

## 6. Frontend Rules

- Use React + Vite and JavaScript ES modules.
- Keep API calls in a small API client module, not scattered through components.
- Store service base URLs in Vite environment variables prefixed with `VITE_`.
- Never place database credentials or backend secrets in frontend environment variables.
- Use reusable components for forms, status badges, timeline events, notifications, and error messages.
- Provide loading, empty, success, and error states.
- Disable submit buttons while a request is in progress to reduce accidental duplicate submissions.
- Show server validation errors in understandable language.
- Do not fabricate tracking events, locations, or notification success.
- Keep layout responsive and controls keyboard-accessible.
- Do not use mock data as if it came from the backend. If demo data is included, label it clearly.

## 7. Git and Collaboration Rules

- Work on a feature branch; do not commit directly to the main branch unless explicitly asked.
- Use small, focused commits with messages such as `feat(tracking): add status history endpoint`.
- Do not overwrite another agent's files without coordinating ownership.
- Each task must have one primary owner. Other agents may review but should not edit the same files concurrently.
- Do not modify API contracts or database schema without notifying frontend and other service owners.
- Every phase ends with a review, tests, and a short update to `Memory.md`.
- Do not claim a test passed unless it was actually run. Record commands and results.
- Avoid unrelated formatting or dependency changes.

## 8. Agent Output Contract

When completing a task, report:
1. Files created/changed.
2. Key implementation decisions.
3. Commands executed.
4. Tests run and actual results.
5. Known limitations or follow-up tasks.
6. Any API/schema changes requiring other agents to integrate.

If blocked, do not invent a workaround that breaks architecture. Record the blocker and propose the smallest safe option.

## 9. Definition of Done

A task is done only when:
- It matches the relevant requirements.
- Its code compiles or frontend builds.
- Relevant tests or manual checks have been run.
- Errors are handled consistently.
- Documentation is updated if behavior or contracts changed.
- No secrets, generated build directories, or local-only files are committed.
- The owner has supplied the Agent Output Contract summary.
