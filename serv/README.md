# Venus backend

Spring Boot 4.1 (Java 21, Gradle 8.14 Kotlin DSL), package `com.serv`.

```bash
./gradlew bootRun     # profile from application.properties (dev data: TestDataInitializer, profile "dev")
./gradlew test        # H2 in MySQL mode, ~370 tests; compilation runs with -Xlint:all, no warning allowed
```

## Layout

| Package | Content |
|---|---|
| `controllers` | REST endpoints, one class per audience: `AuthController` (`/auth`), `CommonController` and `WorkerController` (public), `AccountController*` (`/account`, `/account/client`, `/account/worker`), `AdminController` (`/admin`) |
| `configuration` | Security (`SecurityConfig`, JWT), request logging, startup seeders |
| `services` | Business logic: media storage, emails, SSE streams, abuse protection |
| `database` | JPA entities, repositories, `filters` (gallery specifications) |
| `dto`, `requests` | Response and request records (requests carry Bean Validation annotations) |
| `common` | Enums, `InputLimits`, `PasswordPolicy`, `TableNames` |

## Security model

- **Routes** (`SecurityConfig`): `/auth/**`, GET `/common/**`, `/workers/**`, `/media/**` are public; `/account/client/**`
  CLIENT, `/account/worker/**` WORKER, `/admin/**` ADMIN or SUPER_ADMIN; everything else is denied. `/payment/**` is
  closed while the site is free.
- **Tokens**: HS256 JWT with `userId` and `tv` (token version) claims. `VenusJwtAuthenticationConverter` reloads the
  account: unknown, banned, or revoked (`tv` changed by a password change or a ban) → 401. On public routes an invalid
  token is ignored (anonymous access). SSE endpoints accept `?token=` because `EventSource` cannot send headers.
- **Input**: limits in `InputLimits`, Bean Validation + `@Valid`, passwords through `PasswordPolicy`, emails through
  `Email` (lower case). Invalid input → 4xx, never 500; `GlobalExceptionHandler` returns no internal detail.
- **Ownership**: account endpoints act on the authenticated user only; admins cannot act on other admins unless super
  admin. Non-public worker profiles answer 404 except to admins and the owner.
- **Uploads** (`MediaStorageService`): type detected from content, images re-encoded (EXIF removed), size and
  decompression limits, per-worker storage quota. Certification photos live outside `/media` and are served to admins
  only.
- **Abuse**: login and password-reset throttling per account (`AbuseProtectionService`).

## Conventions

- Logs: SLF4J only (no `System.out`), request id in every line and in the `X-Request-Id` header. Security events are
  prefixed `SECURITY`, admin actions `ADMIN`. Never log passwords, tokens or emails: identify accounts by id
  (`LoggingTest` checks it).
- API error messages are in English and meant for developers; the front shows its own translated texts.
- Emails are rendered by `EmailTemplates` in the language of the request (`Accept-Language`, sent by the front).
- Legal texts (`LegalText`) are versioned per language and never overwritten: each change adds a version.
- Table names are declared in `TableNames`; never rename one (`ddl-auto: update` would start an empty table).
- Live updates (`SseStreamService`) are sent after the transaction commits.

## Tests

Integration tests extend `support/IntegrationTest`: real Spring context and security chain, real JWTs, database
emptied before each test, temporary media folders. No `@Transactional` on tests.

- Every new endpoint goes into `security/AuthorizationMatrixTest` (anonymous / client / worker / admin).
- Every new input gets tests for its invalid cases (empty, too long, wrong format, another user's id).
