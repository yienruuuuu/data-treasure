# AGENTS.md

## Project Context

This project is a crawler and data-processing system. Its long-term goal is to power a customer-facing data site for TOC customers.

Design decisions should favor reliable data collection, repeatable processing, traceable failures, and maintainable APIs for exposing processed data.

## Architecture

- Use a three-layer architecture: `controller -> service -> dao`.
- Controllers handle HTTP request/response concerns only.
- Services contain business flow, validation, transaction boundaries, and orchestration.
- DAOs contain persistence access only.
- Controllers must not call DAOs directly.
- Keep scheduler framework code reusable and separate from specific business task implementations.
- Prefer constructor injection for dependencies.

## Bean Organization

- Put passive data carrier classes under a `bean` package instead of mixing them into controller, service, or dao packages.
- Use `bean.dto` for API request/response objects and other transport-facing DTOs.
- Use `bean.po` for JPA persistence objects/entities.
- Keep business behavior in services, handlers, or domain classes rather than DTO/PO classes.

## DAO Query Strategy

- DAO methods may use JPQL or native SQL depending on the use case.
- Prefer JPQL for normal entity queries and simple updates.
- Use native SQL when database-specific behavior is required, especially PostgreSQL locking, concurrency control, or performance-sensitive data access.
- For scheduler task claiming, PostgreSQL-native locking such as `FOR UPDATE SKIP LOCKED` is acceptable.
- Keep native SQL localized inside DAO classes.

## Exception Handling

- API-facing errors must use `ApiException` or subclasses.
- Error codes must implement `ErrorCode`.
- Use `SysCode` for common system-level responses.
- Controllers should rely on `GlobalExceptionHandler`; avoid per-controller try/catch blocks unless there is a specific recovery path.
- Validation and malformed request errors should return a consistent API error response.
- Unexpected exceptions should be logged and mapped to an internal error response.

## Logging

- Use Lombok `@Slf4j`.
- Do not manually declare loggers with `LoggerFactory.getLogger(...)`.
- Log operationally useful information, especially crawler failures, scheduler execution failures, retry behavior, and data-processing errors.
- For manually triggered flows (for example `sync-once`), log at least one `INFO` at start and one `INFO` at completion, with key fields such as source URL, updated date, declared count, fetched count, and elapsed time.
- Add `DEBUG` logs at important internal boundaries (fetch/parse/ingest) to help diagnose data-shape or parsing issues without changing code during incidents.
- If a branch intentionally skips persistence or retries (for example duplicate snapshot or partial/empty crawl result), log the reason and the decision inputs.
- Avoid logging sensitive customer data or secrets.

## Lombok

- Use Lombok annotations when they reduce boilerplate without hiding important behavior.
- Prefer `@Getter` and `@Setter` for JPA entities.
- Avoid `@Data` on JPA entities because generated `equals`, `hashCode`, and `toString` can conflict with persistence identity and lazy relationships.
- Constructor injection should remain explicit unless using Lombok would keep the dependency contract clear.

## ObjectMapper

- Do not instantiate `ObjectMapper` directly inside business classes.
- Use the shared Spring-managed `ObjectMapper` bean from configuration.
- Inject `ObjectMapper` through constructors when JSON serialization or deserialization is needed.
- Put global Jackson customization in config instead of scattering mapper settings across services.

## Comments and Javadocs

- Add Javadocs for public interfaces, extension points, framework services, shared config, and exception/error-code contracts.
- Add short comments for non-obvious behavior such as retry rules, database locking, concurrency control, and failure recovery.
- Do not add comments that merely restate the code.
- Prefer concise comments that explain intent, invariants, or operational risk.

## API Documentation

- Use Swagger/OpenAPI annotations for public REST APIs.
- Provide Chinese descriptions and examples for controller operations, request DTOs, response DTOs, and common error responses.
- Keep examples realistic for crawler, data processing, scheduler, and TOC customer-facing data-site use cases.
- Swagger UI is expected at `/swagger-ui.html`, and the OpenAPI JSON is expected at `/v3/api-docs`.

## Scheduler Framework

- Scheduler tasks must be persistent.
- Task execution must support retries.
- Execution errors must be written to the database.
- Task types are managed by enum constants.
- Scheduler handlers should implement `ScheduledTaskHandler`.
- Handler implementations should focus on one specific business task.
- The scheduler executor is responsible for claiming tasks, running handlers, applying retry rules, and recording failures.
- DB locking must prevent duplicate execution across multiple application instances.

## Testing

- Run tests before finishing code changes:

```powershell
.\gradlew.bat test --no-daemon
```

- Prefer lightweight BDD-style test structure for behavior-heavy changes:
  - `// given`
  - `// when`
  - `// then`
- Use unit tests for isolated service and handler behavior.
- Use integration or DAO tests for persistence, transaction, locking, and concurrency behavior.
- Keep test names behavior-oriented, for example `failedTaskRetriesWhenAttemptIsBelowMaxAttempts`.

## General Engineering Rules

- Keep changes scoped to the requested behavior.
- Follow existing package structure and naming conventions.
- Avoid unrelated refactors.
- For crawler-style features, first use the agent/tooling to call the target source API or page directly before coding. Confirm the real HTTP behavior, required parameters, TLS/certificate behavior, headers, status codes, pagination, and response shape, then implement Java code from those observed facts instead of guessing from docs alone.
- Preserve Flyway migration history; add new migration files instead of editing applied migrations.
- When adding or changing database schema, always add both table comments and column comments in migrations.
- Keep customer-facing data correctness and traceability as primary concerns.
