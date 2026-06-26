# CLAUDE.md

This file provides guidance to AI coding assistants when working with code in this repository.

## Overview

Modo Ensayo is a full-stack platform for managing artistic classes with
conditional payments tied to class completion. The project originally used a
Spring Boot (Java) backend and was **fully migrated to Supabase**. The Spring
backend no longer exists in this repository.

- **Frontend**: Vue 3 (Composition API) + Vite + Tailwind CSS (`frontend/`)
- **Backend**: Supabase — PostgreSQL + Auth + Storage + Realtime + Edge Functions (`supabase/`)
- **Payments**: MercadoPago Checkout Pro, integrated via Edge Functions

## Commands

### Frontend

All commands run from the `frontend/` directory.

```bash
npm install        # install dependencies
npm run dev        # dev server at http://localhost:3001
npm run build      # production build
npm run preview    # preview the production build
npm run lint       # ESLint (flat config, eslint.config.js)
npm run test       # Vitest run once
npm run test:watch # Vitest watch mode
```

### Supabase (CLI)

```bash
supabase link --project-ref <project-ref>   # link the workspace to a hosted project
supabase db push                            # apply local migrations to the linked project
supabase migration fetch --yes              # pull remote migration history locally
supabase functions deploy                   # deploy all Edge Functions
supabase functions deploy <name>            # deploy a single Edge Function
supabase gen types --linked > frontend/src/types/database.ts  # generate TS types
```

The linked project ref is stored in `supabase/.temp/project-ref`.

## Architecture

### Frontend (`frontend/src/`)

Vue 3 SPA. Talks directly to Supabase (PostgREST + Auth + Storage + Realtime)
and calls Edge Functions for sensitive business logic.

#### Service layer (`services/`)

One module per domain (`classService.js`, `venueService.js`,
`paymentService.js`, `adminService.js`, etc.). All import the singleton client
from `services/supabase.js` and use one of three access paths:

- **PostgREST** — `supabase.from('table')...` for RLS-scoped CRUD
- **Edge Functions** — `invokeFunction(name, { method, body })` for privileged
  business operations (create class, book slot, confirm class, payments, etc.)
- **RPC** — `supabase.rpc('get_my_attributes')` and similar PostgreSQL functions

`services/supabase.js` exposes:
- `supabase` — singleton client (cached on `globalThis` to avoid multiple
  GoTrueClient instances under HMR)
- `camelize(value)` — recursively converts snake_case keys → camelCase so views
  written for the old Spring API (camelCase) keep working
- `currentUserId()` — reads the `sub` claim from the JWT in localStorage
- `invokeFunction(name, options)` — invokes an Edge Function and normalizes
  errors to the axios-style shape (`error.response = { status, data }`) the
  views expect

#### Auth store (`stores/auth.js`)

A class-based singleton (not Pinia) backed by Supabase Auth. Holds `token`,
`user`, and `modoActual` (active role context: `'alumno' | 'profesor' | 'sede'`).

- `login` / `register` / `googleLogin` wrap `supabase.auth.signInWithPassword`,
  `signUp`, `signInWithIdToken`
- `supabase.auth.onAuthStateChange` keeps token + user in sync (auto refresh,
  cross-tab logout)
- Roles come from the `app_metadata.roles` JWT claim
- Derived attributes (identity validation, active bookings, teacher state, etc.)
  come from the `get_my_attributes` RPC, normalized by `mapAtributos`
- Keeps the same `localStorage` contract as the Spring version
  (`auth_token`, `auth_user`, `modoActual`) so the router and views are unchanged

#### Role-based views (`views/`)

| Directory | Role |
|---|---|
| `views/alumno/` | Student — browse/enroll, calendar, payment history |
| `views/profesor/` | Teacher — manage classes, drafts, calendar, metrics |
| `views/sede/` | Venue manager — room scheduling, confirmations, metrics |
| `views/admin/` | Admin — users, venues, dynamic role management |

Top-level `views/*.vue` are shared (login, register, cart, home, etc.).
Route guards in `router/index.js` read the auth store via `decodeJwt` to enforce
`requiresAuth` and role-specific routes.

#### Other conventions

- `composables/` — reusable Composition API utilities (`useToast`, `useTheme`,
  `usePlacesAutocomplete`)
- `hooks/useNotifications.js` — notification hook
- `features/` — self-contained feature slices (`auth`, `cart`, `classes`,
  `payments`, `reschedules`)
- `layouts/DefaultLayout.vue` — single shared shell
- No Pinia; no global state beyond the auth store singleton

### Supabase backend (`supabase/`)

#### Migrations (`migrations/`)

Versioned SQL migrations applied in order: extensions → enums → helpers →
tables → table-dependent RLS helpers → RLS policies → storage → cron functions →
realtime → seed data, plus incremental migrations. The hosted database is the
**source of truth** for migration history; sync local changes with the CLI.

#### Edge Functions (`functions/`)

13 Deno/TypeScript functions for privileged business logic, e.g.:
`create-class`, `book-slot`, `confirm-class`, `assign-reserva`,
`propose-reschedule`, `teacher-decision`, `student-decision`, `create-review`,
`register-venue`, `admin-approve-venue`, `admin-stats`, `admin-users`,
`generate-blocks`, `mercadopago-create-preference`, `mercadopago-webhook`.

- Shared helpers live in `functions/_shared/`
- `config.toml` sets `verify_jwt` per function (webhooks `false`, the rest `true`)
- Functions use the service-role client to bypass RLS where needed; never expose
  the service key to the Vue frontend

## Configuration

### Frontend env (`frontend/.env`, copy from `.env.example`)

| Variable | Purpose |
|---|---|
| `VITE_SUPABASE_URL` | Supabase project URL (public) |
| `VITE_SUPABASE_ANON_KEY` | Anon/publishable key (public, RLS-protected) |
| `VITE_API_BASE_URL` | Legacy; unused — kept empty during transition |

### Edge Functions secrets (set via `supabase secrets set`)

`MERCADOPAGO_ACCESS_TOKEN`, `MERCADOPAGO_WEBHOOK_SECRET`, `APP_FRONTEND_URL`.
On the Supabase platform the `SUPABASE_*` keys are auto-provisioned.

## Testing

### Frontend

- Vitest + `@vue/test-utils` + jsdom
- Test files live alongside views (e.g., `views/CartPage.test.js`,
  `views/PaymentSuccessPage.test.js`)
- Run with `npm test` from `frontend/`

### Database

- Validate schema changes against the linked project and run `get_advisors`
  (security + performance) after DDL changes
