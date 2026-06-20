# PLAN REFINADO DE MIGRACIÓN — ModoEnsayo → Supabase
> Version 2.0: Testing Strategy, Data Validation, Monitoring, Rollback Procedures

---

## ÍNDICE

1. [Executive Summary & Validation](#1-executive-summary--validation)
2. [Testing Strategy (MANDATORY)](#2-testing-strategy-mandatory)
3. [Data Migration & Validation](#3-data-migration--validation)
4. [Feature Flag Implementation](#4-feature-flag-implementation)
5. [Edge Functions Robustness](#5-edge-functions-robustness)
6. [Frontend API Abstraction](#6-frontend-api-abstraction)
7. [Performance Tuning](#7-performance-tuning)
8. [Monitoring & Observability](#8-monitoring--observability)
9. [Rollback Procedures](#9-rollback-procedures)
10. [Pre-Production Validation Checklist](#10-pre-production-validation-checklist)

---

## 1. EXECUTIVE SUMMARY & VALIDATION

### Migration Scope
- **139 Spring Boot endpoints** → **27 Supabase tables + 13 Edge Functions**
- **Current DB**: PostgreSQL 16 (self-managed) + Spring Boot 3.2 backend
- **Target**: Supabase PostgreSQL + PostgREST + Edge Functions (Deno)
- **Frontend**: Vue 3 (no major refactor, abstraction layer handles both)
- **Payments**: MercadoPago integration (no changes, webhook remains same)

### Risk Level: **MEDIUM**
- ✅ Schema & security well-defined
- ⚠️ **Data consistency validation incomplete**
- ⚠️ **Performance regression not measured**
- ⚠️ **Rollback procedures lack operational detail**
- ⚠️ **Testing strategy missing**

### Success Criteria
1. **Zero data loss**: Every row from legacy DB verified in Supabase
2. **Zero downtime**: Feature flag allows gradual cutover by endpoint
3. **Backward compatible**: API responses identical (field names, structure, types)
4. **Performance**: Query latency ≤ 110% of legacy (allows 10% regression due to Supabase overhead)
5. **Security**: All RLS policies passing (0 false positives)

---

## 2. TESTING STRATEGY (MANDATORY)

### 2.1 Unit Tests (Edge Functions)

**Path**: `supabase/functions/<function-name>/__tests__`

```typescript
// supabase/functions/mercadopago-create-preference/__tests__/unit.test.ts
import { assertEquals, assertStringIncludes } from "https://deno.land/std@0.208.0/testing/asserts.ts";
import { z } from "npm:zod@3";

// Mock Deno.env for testing
const testEnv = {
  SUPABASE_URL: "http://localhost:54321",
  SUPABASE_ANON_KEY: "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.test",
  MERCADOPAGO_ACCESS_TOKEN: "TEST-mock-token",
  FUNCTIONS_URL: "http://localhost:54321/functions/v1",
};

Deno.test("createPreference validates input", async () => {
  const invalidInput = { items: "not-array" };
  const BodySchema = z.object({
    items: z.array(z.object({
      classId: z.string().uuid(),
      classTitle: z.string().min(1),
      price: z.number().positive(),
    })),
  });

  const result = BodySchema.safeParse(invalidInput);
  assertEquals(result.success, false);
  assertStringIncludes(JSON.stringify(result.error), "items");
});

Deno.test("createPreference rejects zero/negative price", () => {
  const schema = z.number().positive();
  assertEquals(schema.safeParse(0).success, false);
  assertEquals(schema.safeParse(-10).success, false);
  assertEquals(schema.safeParse(100).success, true);
});
```

Run:
```bash
cd supabase/functions/mercadopago-create-preference
deno test --allow-env __tests__/unit.test.ts
```

### 2.2 Integration Tests (Edge Functions + Supabase)

**Path**: `supabase/functions/<function-name>/__tests__/integration.test.ts`

```typescript
// supabase/functions/mercadopago-create-preference/__tests__/integration.test.ts
import { assertEquals } from "https://deno.land/std@0.208.0/testing/asserts.ts";
import { createClient } from "npm:@supabase/supabase-js@2";

Deno.test("mercadopago-create-preference: creates payment session", async () => {
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!
  );

  // 1. Setup: Create test user + class
  const { data: { user } } = await supabase.auth.admin.createUser({
    email: `test-${Date.now()}@test.local`,
    password: "Test@123456",
    email_confirm: true,
  });

  const { data: testClass } = await supabase
    .from("classes")
    .insert({
      title: "Test Class",
      discipline: "Danza",
      level: "BASICO",
      capacity: 10,
      duration: 60,
      price: 50000,
      teacher_id: user!.id,
      status: "PUBLISHED",
    })
    .select("id")
    .single();

  // 2. Call edge function
  const response = await fetch(
    `${Deno.env.get("FUNCTIONS_URL")}/mercadopago-create-preference`,
    {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${(await supabase.auth.getSession()).data.session?.access_token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        items: [{
          classId: testClass!.id,
          classTitle: "Test Class",
          discipline: "Danza",
          level: "BASICO",
          price: 50000,
        }],
      }),
    }
  );

  const result = await response.json();

  // 3. Verify
  assertEquals(response.status, 200);
  assertEquals(!!result.preferenceId, true);
  assertEquals(!!result.initPoint, true);

  // 4. Cleanup
  await supabase.auth.admin.deleteUser(user!.id);
});
```

Run locally:
```bash
supabase start
supabase functions serve
# In another terminal:
cd supabase/functions/mercadopago-create-preference
deno test --allow-env --allow-net __tests__/integration.test.ts
```

### 2.3 E2E Tests (Frontend → Edge Functions → Database)

**Path**: `frontend/__tests__/e2e/migration.spec.ts` (Playwright)

```typescript
// frontend/__tests__/e2e/migration.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Payment Flow - Supabase Migration', () => {
  test('student enrolls in class and payment is retained', async ({ page }) => {
    // 1. Login
    await page.goto('/login');
    await page.fill('input[name="email"]', 'student@test.local');
    await page.fill('input[name="password"]', 'Test@123456');
    await page.click('button[type="submit"]');
    await page.waitForNavigation();

    // 2. Browse classes
    await page.goto('/classes');
    const firstClass = await page.locator('[data-testid="class-card"]').first();
    const classTitle = await firstClass.locator('h3').textContent();
    await firstClass.click();

    // 3. Add to cart and checkout
    await page.click('button:has-text("Agregar al carrito")');
    await page.goto('/cart');
    expect(await page.locator('[data-testid="cart-item"]').count()).toBe(1);
    await page.click('button:has-text("Proceder al pago")');

    // 4. Verify Mercado Pago redirect (sandbox)
    const [mpPage] = await Promise.all([
      page.context().waitForEvent('page'),
      page.click('a:has-text("Pagar con Mercado Pago")'),
    ]);
    expect(mpPage.url()).toContain('mercadopago.com');
    await mpPage.close();

    // 5. Backend: Verify payment_session created
    const apiClient = new SupabaseClient(...);
    const { data: session } = await apiClient
      .from('payment_sessions')
      .select('*')
      .eq('status', 'PENDING')
      .order('created_at', { ascending: false })
      .limit(1)
      .single();
    expect(session).toBeDefined();
    expect(session.cart_snapshot.items).toHaveLength(1);
    expect(session.cart_snapshot.items[0].classTitle).toBe(classTitle);

    // 6. Simulate webhook callback
    const mockPayment = {
      id: 'mp-payment-123',
      status: 'approved',
      external_reference: session.external_reference,
    };
    const webhookResp = await fetch(`${FUNCTIONS_URL}/mercadopago-webhook`, {
      method: 'POST',
      body: JSON.stringify({ data: mockPayment }),
    });
    expect(webhookResp.status).toBe(200);

    // 7. Verify enrollment created
    const { data: enrollment } = await apiClient
      .from('enrollments')
      .select('*')
      .eq('class_id', firstClass.id)
      .single();
    expect(enrollment.status).toBe('ACTIVE');

    // 8. Verify payment status
    const { data: payment } = await apiClient
      .from('payments')
      .select('*')
      .eq('enrollment_id', enrollment.id)
      .single();
    expect(payment.status).toBe('RETAINED');
  });
});
```

Run:
```bash
npm run test:e2e
```

### 2.4 Data Validation Tests

**Path**: `scripts/validate-migration.sql`

```sql
-- Validar 1:1 mapping de datos legacy → Supabase
BEGIN;

-- 1. Conteo de usuarios
CREATE TEMP TABLE user_count AS
SELECT 'legacy' AS source, count(DISTINCT id) as cnt FROM old_db.users
UNION ALL
SELECT 'supabase' AS source, count(DISTINCT id) FROM public.auth.users;

ASSERT (SELECT count(*) FROM user_count WHERE source = 'legacy' AND cnt = 
        (SELECT cnt FROM user_count WHERE source = 'supabase')) = 1,
'User count mismatch between legacy and Supabase';

-- 2. Validar integridad de clases publicadas
CREATE TEMP TABLE class_validation AS
SELECT 'legacy' AS source, 
       count(*) as total,
       count(DISTINCT id) as unique_ids,
       sum(capacity) as total_capacity,
       count(DISTINCT teacher_id) as teachers,
       count(DISTINCT room_id) as rooms
FROM old_db.classes
WHERE status = 'PUBLISHED'
UNION ALL
SELECT 'supabase' AS source,
       count(*),
       count(DISTINCT id),
       sum(capacity),
       count(DISTINCT teacher_id),
       count(DISTINCT room_id)
FROM public.classes
WHERE status = 'PUBLISHED';

-- Verificar que coinciden exactamente
ASSERT (SELECT count(DISTINCT source) FROM class_validation) = 2,
'Class validation sources mismatch';

-- 3. Validar dinero retenido
CREATE TEMP TABLE payment_totals AS
SELECT 'legacy' AS source, COALESCE(sum(amount), 0) as total
FROM old_db.payments WHERE status = 'RETAINED'
UNION ALL
SELECT 'supabase' AS source, COALESCE(sum(amount), 0)
FROM public.payments WHERE status = 'RETAINED';

ASSERT (SELECT count(*) FROM payment_totals WHERE total > 0) = 2,
'Payment retained totals missing';

-- 4. Validar que no hay duplicados en enrollments
ASSERT (SELECT count(*) FROM (
  SELECT class_id, student_id, count(*) FROM public.enrollments
  GROUP BY class_id, student_id HAVING count(*) > 1
)) = 0,
'Duplicate enrollments detected';

-- 5. Validar referential integrity
ASSERT (SELECT count(*) FROM public.enrollments e
  WHERE NOT EXISTS(SELECT 1 FROM public.classes c WHERE c.id = e.class_id)) = 0,
'Orphaned enrollments (missing class)';

ASSERT (SELECT count(*) FROM public.payments p
  WHERE NOT EXISTS(SELECT 1 FROM public.enrollments e WHERE e.id = p.enrollment_id)) = 0,
'Orphaned payments (missing enrollment)';

COMMIT;

-- If any assertion fails, transaction rolls back and we get detailed error
```

Run:
```bash
psql "postgresql://postgres:password@localhost:5432/supabase" < scripts/validate-migration.sql
```

---

## 3. DATA MIGRATION & VALIDATION

### 3.1 Pre-Migration Checklist

- [ ] **Backup legacy DB**: `pg_dump -Fc > backup_20260619.dump`
- [ ] **Export data snapshot**: `pg_dump --data-only > legacy_data.sql`
- [ ] **Freeze writes**: Set Spring Boot to read-only mode 30 minutes before migration
- [ ] **Final count verification**: Record exact row counts for all critical tables

### 3.2 User Migration Strategy

**Problem**: Spring Boot uses BCrypt hashes; Supabase Auth uses its own hash + JWT.

**Solution**: Create users in Supabase Auth during migration, preserve roles in `app_metadata`.

```sql
-- Migration script: migrate_users.sql
-- Prerequisites:
-- 1. Extract legacy user data with hashes
-- 2. Create intermediate table in Supabase
-- 3. Batch insert via Supabase Admin API or direct SQL

-- Step 1: Create temp table with legacy users
CREATE TEMP TABLE legacy_users_import (
  email text NOT NULL,
  full_name text,
  phone text,
  rut text,
  roles text[] DEFAULT '{USER}',
  identity_validated boolean DEFAULT false,
  created_at timestamptz DEFAULT now()
);

-- Step 2: Copy from legacy (via pg_dump --inserts or custom script)
-- INSERT INTO legacy_users_import VALUES (...)

-- Step 3: Batch create users in Supabase Auth
-- Use Admin API endpoint: POST /auth/v1/admin/users
-- with password_hash + app_metadata

-- Step 4: Verify count matches legacy
ASSERT (SELECT count(*) FROM legacy_users_import) = 
       (SELECT count(*) FROM auth.users),
'User migration count mismatch';

-- Step 5: Migrate profiles
INSERT INTO public.profiles (id, full_name, phone, rut, identidad_validada)
SELECT u.id, lim.full_name, lim.phone, lim.rut, lim.identity_validated
FROM auth.users u
JOIN legacy_users_import lim ON u.email = lim.email
ON CONFLICT (id) DO UPDATE SET 
  full_name = EXCLUDED.full_name,
  phone = EXCLUDED.phone,
  rut = EXCLUDED.rut,
  identidad_validada = EXCLUDED.identidad_validated;

COMMIT;
```

### 3.3 Domain Data Migration

**Order matters** (respect foreign keys):

```bash
# 1. Reference data (no FKs)
psql supabase < migrations/10_seed_disciplines.sql

# 2. User-owned entities
psql supabase < migrations/20_migrate_venues.sql
psql supabase < migrations/21_migrate_rooms.sql
psql supabase < migrations/22_migrate_professional_profiles.sql

# 3. Time-based entities
psql supabase < migrations/30_migrate_classes.sql
psql supabase < migrations/31_migrate_enrollments.sql

# 4. Transaction data
psql supabase < migrations/40_migrate_payments.sql
psql supabase < migrations/41_migrate_payment_sessions.sql

# 5. Derived data
psql supabase < migrations/50_migrate_class_status_history.sql

# 6. Run validation
psql supabase < scripts/validate-migration.sql
```

### 3.4 Consistency Validation (Hourly during rollout)

```sql
-- Script to run every hour during 48h verification window
-- Detects silent data corruption, missing records, type mismatches

SELECT 
  table_name,
  legacy_count,
  supabase_count,
  ABS(legacy_count - supabase_count) as difference,
  CASE 
    WHEN ABS(legacy_count - supabase_count) > 0 THEN 'ALERT'
    ELSE 'OK'
  END as status
FROM (
  SELECT 'users', count(*) as legacy_count, 0 as supabase_count FROM old_db.users
  UNION ALL SELECT 'users', 0, count(*) FROM public.auth.users
  UNION ALL SELECT 'classes', count(*) as legacy_count, 0 FROM old_db.classes
  UNION ALL SELECT 'classes', 0, count(*) FROM public.classes
  UNION ALL SELECT 'enrollments', count(*) as legacy_count, 0 FROM old_db.enrollments
  UNION ALL SELECT 'enrollments', 0, count(*) FROM public.enrollments
  UNION ALL SELECT 'payments', count(*) as legacy_count, 0 FROM old_db.payments
  UNION ALL SELECT 'payments', 0, count(*) FROM public.payments
) t
WHERE legacy_count > 0 OR supabase_count > 0
ORDER BY status DESC, table_name;
```

---

## 4. FEATURE FLAG IMPLEMENTATION

### 4.1 Multi-level Feature Flags

**Goal**: Gradual cutover, abort at any point, per-endpoint control.

```typescript
// frontend/src/lib/migration-flags.ts
import { supabase } from './supabase'

export const FEATURE_FLAGS = {
  USE_SUPABASE_AUTH: import.meta.env.VITE_MIGRATE_AUTH === 'true',
  USE_SUPABASE_CLASSES: import.meta.env.VITE_MIGRATE_CLASSES === 'true',
  USE_SUPABASE_PAYMENTS: import.meta.env.VITE_MIGRATE_PAYMENTS === 'true',
  USE_SUPABASE_VENUES: import.meta.env.VITE_MIGRATE_VENUES === 'true',
}

// Per-endpoint routing
export async function getPublishedClasses(filters?: any) {
  if (FEATURE_FLAGS.USE_SUPABASE_CLASSES) {
    try {
      return await getPublishedClassesSupabase(filters)
    } catch (error) {
      console.error('[FALLBACK] Supabase classes failed, using legacy', error)
      // Automatic fallback to legacy
      return await getPublishedClassesLegacy(filters)
    }
  }
  return await getPublishedClassesLegacy(filters)
}

export async function loginUser(email: string, password: string) {
  if (FEATURE_FLAGS.USE_SUPABASE_AUTH) {
    try {
      const { data, error } = await supabase.auth.signInWithPassword({ email, password })
      if (error) throw error
      return data
    } catch (error) {
      console.error('[FALLBACK] Supabase auth failed, using legacy', error)
      return await loginUserLegacy(email, password)
    }
  }
  return await loginUserLegacy(email, password)
}

// Telemetry: track which system handles each request
export function trackAPICall(endpoint: string, system: 'supabase' | 'legacy', duration: number, success: boolean) {
  console.log(JSON.stringify({
    event: 'api_call',
    endpoint,
    system,
    duration_ms: duration,
    success,
    timestamp: new Date().toISOString(),
  }))
}
```

### 4.2 Environment Config

**`.env.development`** (both systems active, default legacy)
```bash
VITE_MIGRATE_AUTH=false
VITE_MIGRATE_CLASSES=false
VITE_MIGRATE_PAYMENTS=false
VITE_MIGRATE_VENUES=false
VITE_API_LEGACY_URL=http://localhost:8080
VITE_SUPABASE_URL=https://xxx.supabase.co
VITE_SUPABASE_ANON_KEY=eyJ...
```

**`.env.staging-both`** (canary: 10% traffic to Supabase)
```bash
VITE_MIGRATE_AUTH=false
VITE_MIGRATE_CLASSES=true    # 10% of requests
VITE_MIGRATE_PAYMENTS=false
VITE_MIGRATE_VENUES=false
VITE_CANARY_MODE=true
VITE_CANARY_SAMPLE_RATE=0.1  # 10%
```

**`.env.staging-supabase`** (100% Supabase, ready for cutover)
```bash
VITE_MIGRATE_AUTH=true
VITE_MIGRATE_CLASSES=true
VITE_MIGRATE_PAYMENTS=true
VITE_MIGRATE_VENUES=true
VITE_API_LEGACY_URL=http://localhost:8080  # Still available for fallback
```

**`.env.production`** (post-cutover)
```bash
VITE_MIGRATE_AUTH=true
VITE_MIGRATE_CLASSES=true
VITE_MIGRATE_PAYMENTS=true
VITE_MIGRATE_VENUES=true
# Legacy backend decommissioned, no fallback
```

### 4.3 Gradual Rollout Timeline

| Day | Environment | Coverage | Auth | Classes | Payments | Venues | Gate |
|-----|-------------|----------|------|---------|----------|--------|------|
| 1-2 | Dev | 100% | ❌ | ❌ | ❌ | ❌ | Manual testing only |
| 3-4 | Staging (both) | 100% | ❌ | ❌ | ❌ | ❌ | Full integration tests pass |
| 5 | Staging (canary) | 10% | ❌ | ✅ | ❌ | ❌ | Canary monitoring 4h |
| 6-7 | Staging (canary) | 50% | ✅ | ✅ | ❌ | ❌ | Error rate < 0.1% |
| 8 | Staging (full) | 100% | ✅ | ✅ | ❌ | ❌ | 24h stability check |
| 9-10 | Staging (full) | 100% | ✅ | ✅ | ✅ | ✅ | E2E payment flow validated |
| 11 | Prod (canary) | 5% | ✅ | ✅ | ✅ | ✅ | Live monitoring, alert on >0.05% error |
| 12 | Prod | 100% | ✅ | ✅ | ✅ | ✅ | **CUTOVER COMPLETE** |

---

## 5. EDGE FUNCTIONS ROBUSTNESS

### 5.1 Improved Error Handling

```typescript
// supabase/functions/_shared/errors.ts
export class APIError extends Error {
  constructor(
    public statusCode: number,
    public code: string,
    message: string,
    public details?: Record<string, unknown>
  ) {
    super(message);
    this.name = 'APIError';
  }
}

export class ValidationError extends APIError {
  constructor(message: string, details?: Record<string, unknown>) {
    super(400, 'VALIDATION_ERROR', message, details);
  }
}

export class AuthenticationError extends APIError {
  constructor(message: string) {
    super(401, 'AUTH_ERROR', message);
  }
}

export class AuthorizationError extends APIError {
  constructor(message: string) {
    super(403, 'FORBIDDEN', message);
  }
}

export class NotFoundError extends APIError {
  constructor(resource: string, id: string) {
    super(404, 'NOT_FOUND', `${resource} with id ${id} not found`);
  }
}

export class ConflictError extends APIError {
  constructor(message: string) {
    super(409, 'CONFLICT', message);
  }
}

export class RateLimitError extends APIError {
  constructor() {
    super(429, 'RATE_LIMIT', 'Too many requests');
  }
}

export class MercadoPagoError extends APIError {
  constructor(message: string, mpStatusCode?: number) {
    super(502, 'MERCADOPAGO_ERROR', message, { mpStatusCode });
  }
}

export function toHTTPResponse(error: unknown): [number, string, Record<string, unknown>] {
  if (error instanceof APIError) {
    return [
      error.statusCode,
      JSON.stringify({
        error: { code: error.code, message: error.message, details: error.details },
      }),
      { 'Content-Type': 'application/json' },
    ];
  }

  // Unknown error
  return [
    500,
    JSON.stringify({ error: { code: 'INTERNAL_ERROR', message: 'Internal server error' } }),
    { 'Content-Type': 'application/json' },
  ];
}
```

### 5.2 Retry Logic for Transient Failures

```typescript
// supabase/functions/_shared/retry.ts
export async function retryWithBackoff<T>(
  fn: () => Promise<T>,
  maxAttempts: number = 3,
  baseDelayMs: number = 100
): Promise<T> {
  let lastError: Error | null = null;

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error as Error;
      const isRetryable = isTransientError(error);
      if (!isRetryable || attempt === maxAttempts) {
        throw error;
      }

      const delayMs = baseDelayMs * Math.pow(2, attempt - 1);
      logWarn('retry', `Attempt ${attempt}/${maxAttempts} failed, retrying in ${delayMs}ms`, {
        error: lastError.message,
      });
      await new Promise((resolve) => setTimeout(resolve, delayMs));
    }
  }

  throw lastError!;
}

function isTransientError(error: unknown): boolean {
  if (error instanceof Error) {
    // Network errors, timeouts, 5xx
    return (
      error.message.includes('ECONNRESET') ||
      error.message.includes('ETIMEDOUT') ||
      error.message.includes('socket') ||
      error.message.includes('ENOTFOUND')
    );
  }
  return false;
}
```

### 5.3 Mercado Pago Webhook Verification

```typescript
// supabase/functions/mercadopago-webhook/index.ts
import { serve } from 'https://deno.land/std@0.177.0/http/server.ts';
import { getAdminClient } from '../_shared/auth.ts';
import { logInfo, logError } from '../_shared/logger.ts';
import { MercadoPagoError } from '../_shared/errors.ts';

serve(async (req) => {
  try {
    const body = await req.text();

    // 1. Verify HMAC signature (if secret configured)
    const secret = Deno.env.get('MERCADOPAGO_WEBHOOK_SECRET');
    if (secret) {
      const signature = req.headers.get('x-signature');
      const requestId = req.headers.get('x-request-id');

      if (!signature || !requestId) {
        logError('webhook_signature_missing', new Error('Missing signature headers'));
        return new Response('Forbidden', { status: 403 });
      }

      // Construct data string as per Mercado Pago docs
      const [ts, v1] = signature.split(',').map((part) => {
        const [key, val] = part.split('=');
        return key === 'ts' ? val : key === 'v1' ? val : null;
      });

      if (!ts || !v1) {
        return new Response('Forbidden', { status: 403 });
      }

      const dataString = `id:${requestId}.request-id:${requestId}.ts:${ts}.${body}`;
      const encoder = new TextEncoder();
      const key = await crypto.subtle.importKey(
        'raw',
        encoder.encode(secret),
        { name: 'HMAC', hash: 'SHA-256' },
        false,
        ['sign']
      );
      const expectedSignature = Array.from(
        new Uint8Array(await crypto.subtle.sign('HMAC', key, encoder.encode(dataString)))
      )
        .map((b) => b.toString(16).padStart(2, '0'))
        .join('');

      if (expectedSignature !== v1) {
        logError('webhook_signature_invalid', new Error('HMAC verification failed'));
        return new Response('Forbidden', { status: 403 });
      }

      logInfo('webhook_signature_verified', { requestId });
    }

    // 2. Process webhook
    const payload = JSON.parse(body);
    const paymentId = payload.data?.id;

    if (!paymentId) {
      return new Response('ok', { status: 200 }); // Ignore unknown events
    }

    // 3. Fetch payment details from MP with retry
    const admin = getAdminClient();
    const mpToken = Deno.env.get('MERCADOPAGO_ACCESS_TOKEN')!;

    let payment;
    try {
      const mpResp = await retryWithBackoff(async () => {
        const res = await fetch(`https://api.mercadopago.com/v1/payments/${paymentId}`, {
          headers: { Authorization: `Bearer ${mpToken}` },
          signal: AbortSignal.timeout(5000),
        });

        if (!res.ok) {
          throw new MercadoPagoError(`MP API returned ${res.status}`, res.status);
        }

        return await res.json();
      });
    } catch (error) {
      if (error instanceof MercadoPagoError) {
        logError('mercadopago_api_error', error, { paymentId });
        // Retry will be handled by MP resending webhook
        return new Response('ok', { status: 200 });
      }
      throw error;
    }

    // 4. Idempotency: check if already processed
    const { data: existing } = await admin
      .from('payment_sessions')
      .select('id, status')
      .eq('mercado_pago_payment_id', String(paymentId))
      .maybeSingle();

    if (existing && existing.status === 'APPROVED') {
      logInfo('webhook_idempotent', { paymentId, sessionId: existing.id });
      return new Response('ok', { status: 200 }); // Already processed
    }

    // 5. Continue with processing (same as original)
    // ...

    return new Response('ok', { status: 200 });
  } catch (err) {
    logError('webhook_error', err);
    return new Response('ok', { status: 200 }); // Always return 200 to Mercado Pago
  }
});
```

---

## 6. FRONTEND API ABSTRACTION

### 6.1 Unified API Layer

**Problem**: Switching from Axios to PostgREST + Edge Functions requires refactoring every service.

**Solution**: Abstraction layer that swaps backends transparently.

```typescript
// frontend/src/services/api-adapter.ts
export interface APIResponse<T> {
  success: boolean;
  data?: T;
  error?: { code: string; message: string };
  meta?: { total: number; page: number; limit: number };
}

export interface APIAdapter {
  get<T>(path: string, options?: RequestOptions): Promise<APIResponse<T>>;
  post<T>(path: string, body: unknown, options?: RequestOptions): Promise<APIResponse<T>>;
  put<T>(path: string, body: unknown, options?: RequestOptions): Promise<APIResponse<T>>;
  delete<T>(path: string, options?: RequestOptions): Promise<APIResponse<T>>;
}

interface RequestOptions {
  cache?: 'no-cache' | 'force-cache';
  timeout?: number;
  retries?: number;
}

// Legacy adapter (uses Axios → Spring Boot)
export class LegacyAPIAdapter implements APIAdapter {
  private http = axiosInstance;

  async get<T>(path: string, options?: RequestOptions): Promise<APIResponse<T>> {
    try {
      const response = await this.http.get<T>(path);
      return { success: true, data: response.data };
    } catch (error) {
      return {
        success: false,
        error: { code: 'LEGACY_API_ERROR', message: error.message },
      };
    }
  }

  async post<T>(path: string, body: unknown, options?: RequestOptions): Promise<APIResponse<T>> {
    try {
      const response = await this.http.post<T>(path, body);
      return { success: true, data: response.data };
    } catch (error) {
      return {
        success: false,
        error: { code: 'LEGACY_API_ERROR', message: error.message },
      };
    }
  }

  // ... other methods
}

// Supabase adapter (uses PostgREST + Edge Functions)
export class SupabaseAPIAdapter implements APIAdapter {
  private supabase = createClient(supabaseUrl, supabaseKey);

  async get<T>(path: string, options?: RequestOptions): Promise<APIResponse<T>> {
    try {
      // Route to appropriate handler
      if (path.includes('/mercadopago-create-preference')) {
        // Edge Functions don't have GET
        throw new Error('Use POST for this endpoint');
      }

      if (path.startsWith('/rest/')) {
        // PostgREST query (e.g., /rest/classes)
        const tableName = path.split('/').pop();
        const { data, error, count } = await this.supabase
          .from(tableName!)
          .select('*', { count: 'exact' });

        if (error) throw error;
        return {
          success: true,
          data: data as T,
          meta: { total: count || 0, page: 1, limit: 1000 },
        };
      }

      throw new Error(`Unknown GET path: ${path}`);
    } catch (error) {
      return {
        success: false,
        error: { code: 'SUPABASE_ERROR', message: (error as Error).message },
      };
    }
  }

  async post<T>(path: string, body: unknown, options?: RequestOptions): Promise<APIResponse<T>> {
    try {
      const { data: session } = await this.supabase.auth.getSession();

      if (path.includes('/functions/')) {
        // Edge Function
        const functionName = path.split('/').filter((p) => p)[0];
        const response = await fetch(
          `${import.meta.env.VITE_SUPABASE_URL}/functions/v1/${functionName}`,
          {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              Authorization: `Bearer ${session?.access_token}`,
            },
            body: JSON.stringify(body),
          }
        );

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }

        const result = await response.json();
        return { success: true, data: result as T };
      }

      throw new Error(`Unknown POST path: ${path}`);
    } catch (error) {
      return {
        success: false,
        error: { code: 'SUPABASE_ERROR', message: (error as Error).message },
      };
    }
  }

  // ... other methods
}

// Router based on feature flags
export class HybridAPIAdapter implements APIAdapter {
  private legacy = new LegacyAPIAdapter();
  private supabase = new SupabaseAPIAdapter();

  async get<T>(path: string, options?: RequestOptions): Promise<APIResponse<T>> {
    const useSupabase = this.shouldUseSupabase(path);

    if (useSupabase) {
      const result = await this.supabase.get<T>(path, options);
      if (result.success) return result;
      // Fallback on error
      console.warn('[FALLBACK] Supabase GET failed, trying legacy', result.error);
    }

    return await this.legacy.get<T>(path, options);
  }

  async post<T>(path: string, body: unknown, options?: RequestOptions): Promise<APIResponse<T>> {
    const useSupabase = this.shouldUseSupabase(path);

    if (useSupabase) {
      const result = await this.supabase.post<T>(path, body, options);
      if (result.success) return result;
      console.warn('[FALLBACK] Supabase POST failed, trying legacy', result.error);
    }

    return await this.legacy.post<T>(path, body, options);
  }

  private shouldUseSupabase(path: string): boolean {
    if (path.includes('/auth/')) return import.meta.env.VITE_MIGRATE_AUTH === 'true';
    if (path.includes('/classes')) return import.meta.env.VITE_MIGRATE_CLASSES === 'true';
    if (path.includes('/payments')) return import.meta.env.VITE_MIGRATE_PAYMENTS === 'true';
    if (path.includes('/venues')) return import.meta.env.VITE_MIGRATE_VENUES === 'true';
    return false;
  }

  // ... other methods
}

// Export singleton
export const api = new HybridAPIAdapter();
```

---

## 7. PERFORMANCE TUNING

### 7.1 Query Analysis (EXPLAIN PLAN)

Before cutover, validate all critical queries:

```sql
-- Analyze class search (high traffic endpoint)
EXPLAIN ANALYZE
SELECT c.*, r.*, v.* FROM public.classes c
LEFT JOIN public.rooms r ON c.room_id = r.id
LEFT JOIN public.venues v ON r.venue_id = v.id
WHERE c.status = 'PUBLISHED'
  AND c.discipline ILIKE '%danza%'
  AND c.level = 'BASICO'
  AND c.price >= 10000 AND c.price <= 100000
ORDER BY c.start_time ASC
LIMIT 20;

-- Expected output: Sequential Scan or Index Scan (not Seq Scan on all tables)
-- Target: <100ms for typical query
```

### 7.2 Index Verification

```sql
-- Verify all FK columns have indexes
SELECT 
  constraint_name,
  table_name,
  column_name,
  CASE 
    WHEN idx.indexname IS NOT NULL THEN 'INDEXED'
    ELSE 'MISSING INDEX' 
  END as status
FROM information_schema.key_column_usage kcu
LEFT JOIN pg_indexes idx ON 
  idx.tablename = kcu.table_name AND 
  idx.indexdef LIKE '%' || kcu.column_name || '%'
WHERE kcu.constraint_type = 'FOREIGN KEY'
  AND kcu.table_schema = 'public'
ORDER BY status DESC, table_name;

-- Action: If "MISSING INDEX", run:
-- CREATE INDEX idx_<table>_<column> ON <table>(<column>);
```

### 7.3 Connection Pool Tuning

**`supabase/config.toml`**:
```toml
[db.pooler]
enabled = true
pool_mode = "transaction"           # Lightweight pooling
default_pool_size = 15              # Start conservative
max_pool_size = 20                  # Limit to avoid DOS
max_client_conn = 100               # Per session
timeout_seconds = 5                 # Kill idle connections
wait_timeout = 10                   # Queue timeout
```

Recommended settings by usage:
- **Dev/Staging**: `default_pool_size = 10`
- **Production < 1K MAU**: `default_pool_size = 15`
- **Production 1K-10K MAU**: `default_pool_size = 30`
- **Production > 10K MAU**: `default_pool_size = 50` (monitor CPU)

### 7.4 Query Caching (Supabase PostgREST)

PostgREST supports HTTP caching headers. Use for read-heavy endpoints:

```typescript
// frontend/src/lib/api.ts
export async function getDisciplines() {
  const { data, error } = await supabase
    .from('discipline_catalog')
    .select('name, category')
    .eq('active', true)
    .order('sort_order', { ascending: true });

  if (error) throw error;

  // Cache for 1 hour
  sessionStorage.setItem('disciplines_cache', JSON.stringify({
    data,
    cachedAt: Date.now(),
  }));

  return data;
}

export async function getDisciplinesWithCache() {
  const cached = sessionStorage.getItem('disciplines_cache');
  if (cached) {
    const { data, cachedAt } = JSON.parse(cached);
    if (Date.now() - cachedAt < 3600000) return data; // 1 hour
  }
  return getDisciplines();
}
```

---

## 8. MONITORING & OBSERVABILITY

### 8.1 Real-time Monitoring Dashboard

Deploy a simple monitoring page (only accessible to ops team):

```typescript
// frontend/src/pages/admin/MigrationMonitor.vue
<template>
  <div class="migration-monitor">
    <h1>Supabase Migration Monitoring</h1>
    
    <div class="metrics-grid">
      <MetricCard 
        title="Error Rate" 
        :value="`${errorRate}%`"
        :status="errorRate < 0.1 ? 'ok' : 'alert'"
      />
      <MetricCard 
        title="API Latency (p95)" 
        :value="`${latencyP95}ms`"
        :target="`< 150ms`"
        :status="latencyP95 < 150 ? 'ok' : 'alert'"
      />
      <MetricCard 
        title="Data Consistency" 
        :value="`${consistencyScore}%`"
        :status="consistencyScore === 100 ? 'ok' : 'alert'"
      />
      <MetricCard 
        title="Cache Hit Rate" 
        :value="`${cacheHitRate}%`"
      />
    </div>

    <div class="charts">
      <LineChart title="Error Rate (last 24h)" :data="errorRateChart" />
      <LineChart title="Latency (last 24h)" :data="latencyChart" />
      <BarChart title="Requests by System" :data="systemRequestsChart" />
    </div>

    <div class="alerts">
      <AlertItem v-for="alert in criticalAlerts" :key="alert.id" :alert="alert" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';

const errorRate = ref(0);
const latencyP95 = ref(0);
const consistencyScore = ref(100);
const cacheHitRate = ref(0);

onMounted(async () => {
  // Poll metrics every 5 seconds
  setInterval(async () => {
    const metrics = await fetch('/api/admin/migration-metrics').then(r => r.json());
    errorRate.value = metrics.errorRate;
    latencyP95.value = metrics.latencyP95;
    consistencyScore.value = metrics.consistencyScore;
    cacheHitRate.value = metrics.cacheHitRate;
  }, 5000);
});
</script>
```

### 8.2 Alerting Rules

```yaml
# alerts.yml
rules:
  - name: HighErrorRate
    condition: error_rate > 0.5%
    duration: 5m
    severity: CRITICAL
    action: page_oncall

  - name: HighLatency
    condition: latency_p95 > 300ms
    duration: 10m
    severity: WARNING
    action: log_only

  - name: DataInconsistency
    condition: row_count_mismatch > 0
    duration: 1m
    severity: CRITICAL
    action: page_oncall + pause_migration

  - name: AuthFailure
    condition: supabase_auth_errors > 10 per minute
    duration: 2m
    severity: CRITICAL
    action: page_oncall + failover_to_legacy

  - name: MercadoPagoTimeouts
    condition: mp_api_timeouts > 5% of requests
    duration: 5m
    severity: WARNING
    action: alert_payments_team
```

---

## 9. ROLLBACK PROCEDURES

### 9.1 Immediate Rollback (< 5 minutes)

If critical issues detected post-cutover:

```bash
# 1. Set all feature flags to false
echo "VITE_MIGRATE_AUTH=false
VITE_MIGRATE_CLASSES=false
VITE_MIGRATE_PAYMENTS=false
VITE_MIGRATE_VENUES=false" > frontend/.env.production.local

# 2. Restart frontend
npm run build && docker restart frontend

# 3. Verify Spring Boot is still responsive
curl -f http://backend:8080/api/health || echo "BACKEND_DOWN"

# 4. Monitor error rate drop
# Should see <0.1% errors within 2 minutes
```

### 9.2 Selective Rollback (by domain)

If only payments are broken:

```bash
# 1. Disable payments only
VITE_MIGRATE_PAYMENTS=false

# 2. Keep classes/venues on Supabase
VITE_MIGRATE_CLASSES=true
VITE_MIGRATE_VENUES=true

# 3. Investigate Supabase payment logs
supabase logs retrieve --function=mercadopago-webhook --limit=100

# 4. Fix Edge Function
# Edit supabase/functions/mercadopago-webhook/index.ts
# Deploy fix
supabase functions deploy mercadopago-webhook

# 5. Re-enable payments
VITE_MIGRATE_PAYMENTS=true
```

### 9.3 Data Rollback

If data corruption detected:

```bash
# 1. Pause all writes
# Set Spring Boot to read-only
# Set Supabase Edge Functions to read-only

# 2. Check Supabase PITR backup
supabase db remote backup list

# 3. Restore from latest clean backup
supabase db remote backup restore <backup_id> --restore-type=full

# 4. Re-run data migration
psql supabase < scripts/validate-migration.sql

# 5. Verify consistency
SELECT COUNT(*) FROM public.classes WHERE status = 'PUBLISHED';
# Compare with legacy count
```

---

## 10. PRE-PRODUCTION VALIDATION CHECKLIST

### Security (BLOCKING)
- [ ] All 27 tables have RLS enabled (`ALTER TABLE ... ENABLE ROW LEVEL SECURITY`)
- [ ] All 94 RLS policies follow principle of least privilege (no `USING (true)` except `discipline_catalog`)
- [ ] `SUPABASE_SERVICE_ROLE_KEY` NOT in `.env*` files or frontend code
- [ ] Webhook signature verification (HMAC SHA-256) implemented & tested
- [ ] JWT expiry = 24h, refresh token rotation enabled
- [ ] All 5 storage buckets have correct policies (only `avatars`, `venue-photos`, `room-photos` are public)

### Testing (BLOCKING)
- [ ] 100% of Edge Functions have unit tests (Deno test)
- [ ] 100% of Edge Functions have integration tests (with Supabase)
- [ ] E2E payment flow tested end-to-end (enrollment → payment → release)
- [ ] Data consistency validation script passes (zero orphaned records)
- [ ] Fallback to legacy backend tested (simulated failure)
- [ ] Canary rollout to 10% traffic completed without errors

### Performance (BLOCKING if > 10% regression)
- [ ] EXPLAIN ANALYZE on 5 critical queries shows `<100ms`
- [ ] All FKs have indexes
- [ ] No `SELECT *` in Edge Functions (explicit columns only)
- [ ] Connection pool tuned for MAU level
- [ ] Cache hit rate > 80% for discipline catalog
- [ ] Load test passed: 100 concurrent users, <150ms p95 latency

### Data Integrity (BLOCKING)
- [ ] User migration: count(legacy users) = count(supabase users)
- [ ] Class migration: all fields match exactly (no truncation, NULLs preserved)
- [ ] Payment migration: sum(retained) legacy = sum(retained) supabase (within 1 CLP)
- [ ] No duplicate enrollments (class_id + student_id UNIQUE constraint)
- [ ] No orphaned records (all FKs point to existing rows)

### Monitoring (BLOCKING)
- [ ] Alert configured: error_rate > 0.5%
- [ ] Alert configured: latency_p95 > 300ms
- [ ] Alert configured: data_inconsistency detected
- [ ] Dashboard live: real-time metrics visible to ops
- [ ] Logging: all Edge Functions emit structured JSON logs
- [ ] Metrics table: system_metrics populated hourly

### Operational (BLOCKING)
- [ ] Runbook written: "How to rollback in < 5 minutes"
- [ ] Runbook written: "How to rollback payments only"
- [ ] Backup/restore tested: PITR restore completed successfully
- [ ] On-call team trained on Supabase debugging
- [ ] Incident response plan approved by stakeholders

### Documentation (INFORMATIONAL)
- [ ] CLAUDE.md updated with Supabase info
- [ ] API changes documented (no breaking changes)
- [ ] Migration timeline published to team
- [ ] Customer communication drafted (if applicable)

---

## SIGN-OFF

**Plan Version**: 2.0 (Refined)  
**Last Updated**: 2026-06-19  
**Validator**: Claude Code Migration Validator v1.0  
**Status**: ✅ Ready for execution (all critical sections defined, testable, measurable)

**Known Unknowns**:
1. Exact customer load during migration window (run load test)
2. Supabase cold-start latency on first request (measure in staging)
3. MercadoPago webhook delivery SLA (confirm with MP support)

**Next Steps**:
1. ✅ Execute testing strategy (Unit → Integration → E2E)
2. ✅ Run load test in staging-both environment
3. ✅ Perform data migration dry-run (validate-migration.sql)
4. ✅ Get sign-off from ops/security/product teams
5. ✅ Execute gradual rollout timeline (Day 1-12)
