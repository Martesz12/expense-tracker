# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Status

This project is being built from scratch. `SPEC.md` is the source of truth for all requirements, data models, API contracts, and architecture decisions. Read it before making any significant decisions.

## Repository Layout

```
backend/    Spring Boot 3.x (Java 21) — Maven
frontend/   Angular 21 — Angular CLI
```

The two are separate projects with their own build systems. A `docker-compose.yml` at the root orchestrates local dev.

## Backend Commands

```bash
# From backend/
mvn spring-boot:run                          # Start dev server (port 8080)
mvn test                                     # Run all tests
mvn test -Dtest=ClassName                    # Run a single test class
mvn test -Dtest=ClassName#methodName         # Run a single test method
mvn package -DskipTests                      # Build JAR
mvn flyway:migrate                           # Run pending DB migrations manually
```

## Frontend Commands

```bash
# From frontend/
ng serve                                     # Start dev server (port 4200)
ng test                                      # Run unit tests (Vitest)
ng test --include="src/**/*foo.spec.ts" --watch=false   # Run specific spec(s), no watch
ng build                                     # Production build
ng generate component features/foo/bar       # Scaffold a component
```

## Local Dev Stack

```bash
docker compose up -d postgres    # Start only the database
docker compose up                # Start full stack (postgres + backend + frontend)
```

Backend env vars needed for local dev (see `SPEC.md §10` for full list):

| Variable | Local value |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/expensetracker` |
| `JWT_SECRET` | any string ≥ 32 characters |

## Architecture

### Request Flow

```
Browser → Angular SPA
           ↓ REST/JSON (JWT Bearer)
         Spring Boot (port 8080)
           ↓ JPA / Flyway
         PostgreSQL 16 (port 5432)
```

### Backend Package Structure (`com.yourapp.*`)

Feature packages mirror the domain: `auth`, `user`, `account`, `transaction`, `category`, `budget`, `tag`, `recurring`. Each package owns its entity, repository, service, controller, and `dto/` subpackage. Cross-cutting concerns live in `common/` (exception handling, shared DTOs). Cross-cutting config (JWT, security, scheduler) lives in `config/`. Note: `report/` is planned per SPEC but not yet created.

### Security Model

- `JwtAuthFilter` (extends `OncePerRequestFilter`) validates the `Authorization: Bearer` header on every request except `POST /api/auth/**`.
- The authenticated user's UUID is extracted from the JWT and passed as the first argument (`UUID userId`) to every service method — this is how cross-user data leakage is prevented. **Never query without scoping to `userId`.**
- Access tokens: 15 min / HS256. Refresh tokens: 30 days, stored as SHA-256 hashes in `refresh_tokens`, rotated on every use.

### Database

- Flyway manages all schema changes. Migration files go in `src/main/resources/db/migration/` following the `V{n}__{description}.sql` naming convention. Never alter the schema outside Flyway.
- Balance is always computed on-the-fly: `initial_balance + SUM(income) − SUM(expense)`. There is no denormalized balance column.
- Transfers are two sides of the same logical operation and must be committed atomically (`@Transactional`).

### Frontend Architecture

- **Route guards** live in `app/core/guards/`. All guards are functional (`CanActivateFn`). Guards that check auth state must wait for `selectInitialized` before evaluating `selectIsAuthenticated` — session restoration from localStorage is async on startup.
- **NgRx slices:** `auth`, `accounts`, `transactions`, `categories`, `budgets`. Effects own all HTTP calls; components only dispatch actions and select from the store.
- **Lazy-loaded feature modules** under `app/features/`. Core services and models live in `app/core/`.
- **`ShellComponent`** at `app/core/layout/shell/` is the root route component — a collapsible icon sidebar that wraps all feature views. New routes go as `children` of the shell route in `app.routes.ts`.
- **`JwtInterceptor`** attaches the Bearer token to every outgoing request. On `401` it silently refreshes the token pair and retries; on refresh failure it logs the user out.
- **Theming:** PrimeNG Aura theme with teal design tokens. Overrides live in `frontend/src/styles/` — `_variables.scss` (tokens), `_primeng.scss` (component overrides), `_layout.scss`, `_typography.scss`, `_components.scss`.

## Key Conventions

### Planning
- All implementation plans MUST include a dedicated step to write tests for any new feature or bugfix.

### Frontend Testing
- Use `provideMockStore` (`@ngrx/store/testing`) and `provideMockActions` (`@ngrx/effects/testing`) for NgRx unit tests.
- After `store.overrideSelector(selector, value)`, call `store.refreshState()` to trigger emission; call `store.resetSelectors()` in `afterEach`.
- Test functional guards with `TestBed.runInInjectionContext(() => guardFn(route, state))`, then `firstValueFrom()` to resolve the Observable result.
- When providing a mock service via `useValue`, pass the object reference directly — never spread it — so `vi.fn().mockReturnValue()` calls affect the instance the class actually injects.

- **No barrel files:** Never create `index.ts` re-export files. Always import directly from individual files (e.g. `core/models/auth/user.model.ts`).

- All REST responses on error use the shared JSON error shape defined in `SPEC.md §7` (`status`, `error`, `message`, `timestamp`). `GlobalExceptionHandler` handles this.
- Category seeding (default categories like Food, Transport, etc.) happens server-side when a new user registers.
- Recurring transaction generation runs as a daily Spring `@Scheduled` job in `RecurringScheduler`.
- Reports (`/api/reports/**`) are read-only aggregate queries — no writes allowed there.
- CORS: explicitly list allowed origins via `ALLOWED_ORIGINS` env var; never use `*` in production.
