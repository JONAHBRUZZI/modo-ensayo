# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Backend

All commands run from the `backend/` directory using the Maven wrapper.

```bash
# Build
./mvnw clean package           # Windows: mvnw.cmd clean package

# Run (requires PostgreSQL running)
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=AuthServiceTest

# Run a specific test method
./mvnw test -Dtest=AuthServiceTest#register_validInput_createsUser

# Generate JaCoCo coverage report (target/site/jacoco/index.html)
./mvnw clean test jacoco:report
```

### Frontend

All commands run from the `frontend/` directory.

```bash
npm run dev      # Dev server at http://localhost:5173
npm run build    # Production build
npm run test     # Run Vitest tests once
npm run test:watch  # Vitest in watch mode
```

### Docker Compose (recommended for local dev)

```bash
# Full stack (backend + PostgreSQL + pgAdmin)
docker compose up -d --build

# Database only (then run backend via Maven or frontend via npm)
docker compose up -d postgres pgadmin
```

Backend runs on `http://localhost:8080`, frontend dev server on `http://localhost:5173`, PostgreSQL on `localhost:5432`, pgAdmin on `http://localhost:5050`.

## Architecture

This is a full-stack platform for managing artistic classes with conditional payments tied to class completion.

### Backend

Java 21 / Spring Boot 3.2 REST API. Main source root: `backend/src/main/java/com/modoensayo/`.

#### Domain modules

Each top-level package is a self-contained domain with its own controller → service → repository stack:

| Package | Responsibility |
|---|---|
| `auth/` | JWT authentication, registration, login |
| `users/` | User entity, roles, professional profiles, identity verification |
| `admin/` | Admin panel endpoints, platform statistics, role management |
| `classes/` | Class lifecycle (creation, scheduling, status history, completion) |
| `payments/` | MercadoPago Checkout Pro integration, cart, enrollment |
| `venues/` | Venue/room CRUD, availability windows, schedule blocks |
| `reschedules/` | Reschedule request/response workflow, timeout processing |
| `notifications/` | In-app notification delivery |
| `attendance/` | Attendance tracking |
| `reviews/` | Student/teacher review system |
| `associates/` | Associate management |
| `shared/` | Cross-cutting concerns (see below) |

#### Shared infrastructure (`shared/`)

- `config/` — `BaseEntity` (all entities extend this for `createdAt`/`updatedAt`), data seeders, Caffeine cache setup, `StringListConverter` (JPA converter for `List<String>` fields)
- `exceptions/` — `GlobalExceptionHandler` (centralised `@RestControllerAdvice`)
- `security/` — JWT filter and utilities
- `storage/` — `UnifiedStorageService` abstracts local filesystem vs. Supabase storage (toggled via `STORAGE_PROVIDER` env var)
- `controller/` — File upload/access endpoints

#### Layered conventions

- **Entities** extend `BaseEntity` and carry JPA annotations
- **Repositories** extend `JpaRepository`; custom queries use JPQL `@Query`
- **Services** are `@Transactional` for writes; complex workflows use `@Scheduled` tasks (e.g., `ScheduleBlockRegenerator`)
- **Controllers** are `@RestController`; input validated with Bean Validation (`@Valid`)
- **DTOs** — separate request/response records; Jackson configured for non-null serialisation and ISO-8601 dates

#### Key integrations

- **Authentication:** Spring Security + JJWT 0.12.3 (stateless JWT)
- **Payments:** MercadoPago SDK Java 2.1.24
- **Caching:** Caffeine (`adminStats`, `publishedClasses`, `approvedVenues`, `userProfile` — 200 entries, 5 min TTL)
- **Rate limiting:** Bucket4j 0.7.6
- **Storage:** Local filesystem or Supabase (set `STORAGE_PROVIDER=supabase`)

### Frontend

Vue 3 (Composition API) + Vite + Tailwind CSS. Source root: `frontend/src/`.

#### Role-based views

The platform has four roles. Views are split by role under `src/views/`:

| Directory | Role |
|---|---|
| `views/alumno/` | Student — browse/enroll in classes, calendar, payments history |
| `views/profesor/` | Teacher — manage own classes, drafts, calendar, metrics, scheduling |
| `views/sede/` | Venue manager — room scheduling, class confirmations, calendar, metrics |
| `views/admin/` | Admin — users, venues, dynamic role management |

Top-level views (`views/*.vue`) are shared (login, register, cart, home, etc.).

#### Service layer

`src/services/` contains one Axios module per domain (`classService.js`, `venueService.js`, `paymentService.js`, etc.) that all import from `src/services/api.js`. The `api.js` singleton:
- Attaches the JWT from `localStorage` on every request
- Handles 401 responses by clearing auth state and redirecting to `/login`
- Triggers `auth.syncAtributos()` when the backend signals a role/attribute change via `atributosActualizados: true`

#### Auth store

`src/stores/auth.js` is a custom class-based singleton (not Pinia). It holds `token`, `user`, and `modoActual` (the active role context: `'alumno'` | `'profesor'` | `'sede'`). Guards in `src/router/index.js` read it via `decodeJwt` to enforce `requiresAuth` and role-specific routes.

#### Feature modules

`src/features/` contains self-contained feature slices (`auth/`, `cart/`, `classes/`, `payments/`, `reschedules/`) with their own components and logic.

#### Other frontend conventions

- `src/composables/` — reusable Composition API utilities (`useToast`, `usePlacesAutocomplete`)
- `src/hooks/useNotifications.js` — polling-based notification hook
- `src/layouts/DefaultLayout.vue` — single shared shell layout
- No Pinia; no global state beyond the auth store singleton

## Configuration

Key environment variables (copy `.env.example` to `.env`):

| Variable | Purpose |
|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | DB credentials |
| `JWT_SECRET` | JWT signing key |
| `MERCADOPAGO_ACCESS_TOKEN` | Payment gateway token |
| `STORAGE_PROVIDER` | `local` or `supabase` |
| `SUPABASE_URL` / `SUPABASE_KEY` | Required when `STORAGE_PROVIDER=supabase` |
| `APP_FRONTEND_URL` / `APP_BACKEND_URL` | CORS and callback URLs |
| `VITE_API_BASE_URL` | Frontend: backend origin (empty = same origin via proxy) |

Schema is managed by `hibernate.ddl-auto: update` — no Flyway/Liquibase. Data seeders in `shared/config/` populate reference data on startup.

## Testing

### Backend

- **Unit tests:** service layer, mocked repositories/externals with Mockito (`@ExtendWith(MockitoExtension.class)`)
- **Integration tests:** full Spring context + MockMvc + H2 in-memory DB (`@ActiveProfiles("test")`)
- Tests live in `backend/src/test/java/com/modoensayo/`, mirroring the main package structure, with integration tests under `integration/`
- JaCoCo minimum coverage threshold: **25%** (configured in `pom.xml`)
- `TestDataSeeder` utility class handles consistent test data creation for integration tests

### Frontend

- Vitest + `@vue/test-utils` + jsdom
- Test files live alongside views (e.g., `CartPage.test.js`, `PaymentSuccessPage.test.js`)

## Supabase Migration

**Status**: Implementation — schema validated locally, pending cloud deploy

### Local structure (`supabase/`)

- `migrations/` — 10 SQL migrations (extensions → enums → helpers → tables → table-dependent helpers → RLS → storage → cron → realtime → seed). Validated against real Postgres via `supabase start` (27 tables, 27 with RLS, 85 policies, 13 enums, 13 functions, 6 cron jobs, 5 buckets).
- `functions/` — 13 Edge Functions (Deno) using the official **`@supabase/server`** SDK (`withSupabase`). `ctx.supabase` is RLS-scoped, `ctx.supabaseAdmin` bypasses RLS. CORS + auth + error handling are handled by the wrapper. Shared `_shared/logger.ts` for structured logs.
- `config.toml` — per-function `verify_jwt` (webhook `false`, rest `true`).

> **`@supabase/server`**: imported in Deno via `npm:@supabase/server` (no `npm install` — never add it to the Vue frontend, which would expose the secret key). On Supabase Platform the `SUPABASE_*` keys are auto-provisioned; locally an older CLI may need them wired in `supabase/functions/.env`.

### Deploy to cloud (`modoensayo`, ref `remznaanexwgzeeupctv`)

```bash
supabase login                                    # account that owns modoensayo
supabase link --project-ref remznaanexwgzeeupctv
supabase db push                                  # apply 10 migrations
supabase functions deploy                         # deploy 13 functions
supabase secrets set MERCADOPAGO_ACCESS_TOKEN=... MERCADOPAGO_WEBHOOK_SECRET=... APP_FRONTEND_URL=...
```

### MCP Integration

The project uses **Mercado Pago MCP Server** for payment system validation during the Supabase migration. Available commands (via Claude Code):

```bash
# Agentes IA pueden ejecutar:
mpag_search_mcp_documentation           # Buscar docs de Mercado Pago
mpag_create_test_user                   # Crear usuarios de prueba sandbox
mpag_configure_webhooks                 # Configurar notificaciones webhook
mpag_improve_integration                # Validación pre-producción de Edge Functions
```

**Setup MCP locally** (for testing):
```bash
cd ~
git clone https://github.com/mercadopago/mcp-server-mercadopago
cd mcp-server-mercadopago
npm install
npm run dev
```

Configure in `~/.claude/settings.json` (or Claude Desktop `claude_desktop_config.json`):
```json
{
  "mcpServers": {
    "mercadopago": {
      "command": "node",
      "args": ["/path/to/mcp-server-mercadopago/dist/index.js"],
      "env": {
        "MERCADOPAGO_ACCESS_TOKEN": "YOUR_ACCESS_TOKEN",
        "MERCADOPAGO_WEBHOOK_SECRET": "YOUR_WEBHOOK_SECRET"
      }
    }
  }
}
```

### Migration Plan & Documentation

**Status**: ✅ Planning Phase (Ready for Staging Execution)

Core Documents:
- [`PLAN_MIGRACION_SUPABASE.md`](./PLAN_MIGRACION_SUPABASE.md) — Original comprehensive plan (schema, RLS, Edge Functions, storage, seed data)
- [`PLAN_MIGRACION_SUPABASE_REFINED.md`](./PLAN_MIGRACION_SUPABASE_REFINED.md) — Enhanced plan with testing strategy, data migration, feature flags, monitoring, rollback procedures
- [`PLAN_MIGRACION_VALIDATION_REPORT.md`](./PLAN_MIGRACION_VALIDATION_REPORT.md) — Strict validation audit (92/100 score, 10 critical checkpoints)
- [`MIGRATION_DELIVERY_SUMMARY.md`](./MIGRATION_DELIVERY_SUMMARY.md) — Executive summary, action plan, success criteria

**What's Covered**:
- ✅ 27 tables with RLS policies (94 policies total)
- ✅ 13 Edge Functions (Deno/TypeScript) with error handling & retry logic
- ✅ Payment flow via MercadoPago → Supabase webhooks
- ✅ Testing strategy (Unit, Integration, E2E, Data validation)
- ✅ Gradual rollout timeline (12-day plan with 10 checkpoints)
- ✅ Feature flag implementation for transparent backend switching
- ✅ Performance analysis (EXPLAIN ANALYZE, query optimization)
- ✅ Monitoring & observability (dashboard, alerts, metrics)
- ✅ Rollback procedures (3 scenarios, <5m recovery)
- ✅ Pre-production checklist (50+ items)

**Quick Navigation**:
- Planning phase details → [`PLAN_MIGRACION_SUPABASE_REFINED.md`](./PLAN_MIGRACION_SUPABASE_REFINED.md) Section 4 (Feature Flags)
- Testing requirements → [`PLAN_MIGRACION_SUPABASE_REFINED.md`](./PLAN_MIGRACION_SUPABASE_REFINED.md) Section 2
- Validation status → [`PLAN_MIGRACION_VALIDATION_REPORT.md`](./PLAN_MIGRACION_VALIDATION_REPORT.md) Executive Summary
- Action plan → [`MIGRATION_DELIVERY_SUMMARY.md`](./MIGRATION_DELIVERY_SUMMARY.md) Next Steps
