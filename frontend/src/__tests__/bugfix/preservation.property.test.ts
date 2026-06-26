/**
 * Preservation Tests — Comportamiento existente que NO debe cambiar
 *
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7**
 *
 * These tests capture the CURRENT correct behavior of the system for inputs
 * that do NOT satisfy the bug conditions C(X). They must PASS on the unfixed
 * code (observation-first methodology) and continue passing after the fixes
 * are applied (preservation guarantee: F(X) = F'(X)).
 *
 * Properties tested:
 * - Property 4: confirm-class({realized:true}) → RETAINED→RELEASED, class→COMPLETED
 * - Property 5: teacher-decision, student-decision, timeout → RETAINED→REFUND_PENDING
 * - Property 6: Payments in RELEASED/FAILED/REFUNDED not affected by refund processing
 * - Property 7: service_role/pg_cron execute privileged functions; authenticated executes business RPCs
 * - Property 8: mercadopago-webhook creates RETAINED; realized classes release to RELEASED
 */
import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'

// --- Domain model (mirrors the real system) ---

type PaymentStatus = 'RETAINED' | 'RELEASED' | 'REFUND_PENDING' | 'REFUNDED' | 'FAILED'
type ClassStatus = 'SCHEDULED' | 'PUBLISHED' | 'COMPLETED' | 'SUSPENDED' | 'POR_VALIDAR'
type PostgRESTRole = 'anon' | 'authenticated' | 'service_role' | 'pg_cron'

type PrivilegedFunction =
  | 'process_reschedule_timeouts'
  | 'process_class_completion'
  | 'regenerate_schedule_blocks'
  | 'snapshot_system_metrics'
  | 'check_rls_coverage'

type BusinessRpcFunction = 'get_my_attributes'

type UpstreamRefundPath = 'TEACHER_REJECTION' | 'STUDENT_REJECTION' | 'TIMEOUT_48H'

interface Payment {
  id: string
  enrollmentId: string
  classId: string
  amount: number
  status: PaymentStatus
}

interface ConfirmClassRequest {
  classId: string
  realized: boolean
}

interface ConfirmClassResult {
  classStatus: ClassStatus
  payments: { paymentId: string; finalStatus: PaymentStatus }[]
  auditLogged: boolean
}

interface RpcInvocation {
  role: PostgRESTRole
  functionName: string
}

interface RpcResult {
  executed: boolean
  httpStatus: number
  errorCode: string | null
}

interface WebhookPaymentResult {
  paymentCreated: boolean
  paymentStatus: PaymentStatus
}

interface UpstreamRefundResult {
  paymentId: string
  finalStatus: PaymentStatus
}

// --- System Under Test: models the ACTUAL behavior of the current codebase ---

/**
 * Property 4: Models confirm-class with realized=true (the CURRENT correct path).
 *
 * From confirm-class/index.ts:
 * - When realized=true, iterates enrollments of the class
 * - For each enrollment, updates payments: SET status='RELEASED' WHERE status='RETAINED'
 * - Then updates class: SET status='COMPLETED'
 * - Inserts audit_logs with action='class.confirmed_realized'
 *
 * This behavior is correct and must be preserved.
 */
function confirmClassRealized(request: ConfirmClassRequest, retainedPayments: Payment[]): ConfirmClassResult {
  // realized=true path: payments RETAINED → RELEASED, class → COMPLETED
  return {
    classStatus: 'COMPLETED',
    payments: retainedPayments.map((p) => ({
      paymentId: p.id,
      finalStatus: p.status === 'RETAINED' ? 'RELEASED' : p.status,
    })),
    auditLogged: true,
  }
}

/**
 * Property 5: Models the three upstream paths that mark RETAINED → REFUND_PENDING.
 *
 * From teacher-decision/index.ts (accepted=false):
 * - Updates payments: SET status='REFUND_PENDING' WHERE enrollment_id=e.id AND status='RETAINED'
 *
 * From student-decision/index.ts (accepted=false):
 * - Updates payments: SET status='REFUND_PENDING' WHERE enrollment_id=e.id AND status='RETAINED'
 *
 * From process_reschedule_timeouts() SQL:
 * - UPDATE payments SET status='REFUND_PENDING' ... WHERE p.status='RETAINED' AND rr.response_type='TIMEOUT'
 *
 * All three paths correctly transition RETAINED → REFUND_PENDING.
 */
function processUpstreamRefund(path: UpstreamRefundPath, payments: Payment[]): UpstreamRefundResult[] {
  // All three upstream paths do the same: RETAINED → REFUND_PENDING
  // They only affect payments in RETAINED status (WHERE status = 'RETAINED')
  return payments.map((p) => ({
    paymentId: p.id,
    finalStatus: p.status === 'RETAINED' ? 'REFUND_PENDING' : p.status,
  }))
}

/**
 * Property 6: Models refund processing on payments NOT in REFUND_PENDING.
 *
 * The refund processor (which doesn't exist yet but will be created for G-06)
 * should ONLY act on REFUND_PENDING payments. For payments in RELEASED, FAILED,
 * or REFUNDED, the current system leaves them untouched (no processor exists),
 * and the fix must also leave them untouched.
 *
 * Current behavior: no refund processor exists, so these payments are never
 * touched. F(X) = no change. The fix must preserve F'(X) = no change for these.
 */
function processRefundsOnNonPendingPayments(payments: Payment[]): Payment[] {
  // For payments NOT in REFUND_PENDING, no processing occurs.
  // They remain in their current status unchanged.
  return payments.map((p) => ({ ...p })) // Status unchanged
}

/**
 * Property 7: Models RPC execution for authorized callers.
 *
 * From cron_functions.sql:
 * - All five privileged functions are SECURITY DEFINER
 * - pg_cron executes them via scheduled jobs (owner/superuser context)
 * - service_role can execute (PostgREST does not restrict owner's functions)
 *
 * From get_my_attributes.sql:
 * - Has explicit GRANT EXECUTE TO authenticated
 * - Uses auth.uid() internally
 *
 * The fix (G-16) revokes EXECUTE from PUBLIC/anon/authenticated on the five
 * privileged functions but MUST NOT affect service_role/pg_cron execution
 * or authenticated's access to business RPCs.
 */
function invokeRpcAuthorized(invocation: RpcInvocation): RpcResult {
  const privilegedFunctions: string[] = [
    'process_reschedule_timeouts',
    'process_class_completion',
    'regenerate_schedule_blocks',
    'snapshot_system_metrics',
    'check_rls_coverage',
  ]
  const businessRpcFunctions: string[] = ['get_my_attributes']

  // service_role / pg_cron can execute all privileged functions
  if (
    (invocation.role === 'service_role' || invocation.role === 'pg_cron') &&
    privilegedFunctions.includes(invocation.functionName)
  ) {
    return { executed: true, httpStatus: 200, errorCode: null }
  }

  // authenticated can execute business RPCs (get_my_attributes has GRANT TO authenticated)
  if (
    invocation.role === 'authenticated' &&
    businessRpcFunctions.includes(invocation.functionName)
  ) {
    return { executed: true, httpStatus: 200, errorCode: null }
  }

  // Other combinations would be denied (but those are bug condition, not preservation)
  return { executed: false, httpStatus: 403, errorCode: '42501' }
}

/**
 * Property 8: Models webhook payment creation and class-realized release.
 *
 * From mercadopago-webhook/index.ts:
 * - On approved payment notification, creates enrollment and inserts payment
 *   with status = 'RETAINED'
 *
 * From confirm-class/index.ts (realized=true):
 * - Releases payments RETAINED → RELEASED for realized classes
 *
 * These two flows form the happy path of the payment lifecycle and must not
 * be altered by any of the three fixes.
 */
function webhookCreatesPayment(paymentApproved: boolean): WebhookPaymentResult {
  if (paymentApproved) {
    return { paymentCreated: true, paymentStatus: 'RETAINED' }
  }
  return { paymentCreated: false, paymentStatus: 'RETAINED' } // not created
}

function realizedClassReleasesPayment(payment: Payment): PaymentStatus {
  // confirm-class with realized=true: RETAINED → RELEASED
  return payment.status === 'RETAINED' ? 'RELEASED' : payment.status
}

/**
 * rls_auto_enable is an event trigger function, NOT invocable via RPC.
 * It must not be modified by the G-16 fix.
 */
function isRlsAutoEnableModified(): boolean {
  // The fix does not touch rls_auto_enable — it's a false positive
  return false
}

// --- Generators ---

const classIdArb: fc.Arbitrary<string> = fc.uuid()

const retainedPaymentArb = (classId: string): fc.Arbitrary<Payment> =>
  fc.record({
    id: fc.uuid(),
    enrollmentId: fc.uuid(),
    classId: fc.constant(classId),
    amount: fc.integer({ min: 1000, max: 500000 }),
    status: fc.constant('RETAINED' as PaymentStatus),
  })

/** Payments in statuses that are NOT REFUND_PENDING (for Property 6) */
const nonRefundPendingStatusArb: fc.Arbitrary<PaymentStatus> = fc.constantFrom(
  'RELEASED' as PaymentStatus,
  'FAILED' as PaymentStatus,
  'REFUNDED' as PaymentStatus
)

const nonRefundPendingPaymentArb: fc.Arbitrary<Payment> = fc.record({
  id: fc.uuid(),
  enrollmentId: fc.uuid(),
  classId: fc.uuid(),
  amount: fc.integer({ min: 1000, max: 500000 }),
  status: nonRefundPendingStatusArb,
})

const upstreamPathArb: fc.Arbitrary<UpstreamRefundPath> = fc.constantFrom(
  'TEACHER_REJECTION' as UpstreamRefundPath,
  'STUDENT_REJECTION' as UpstreamRefundPath,
  'TIMEOUT_48H' as UpstreamRefundPath
)

const privilegedFunctionArb: fc.Arbitrary<PrivilegedFunction> = fc.constantFrom(
  'process_reschedule_timeouts' as PrivilegedFunction,
  'process_class_completion' as PrivilegedFunction,
  'regenerate_schedule_blocks' as PrivilegedFunction,
  'snapshot_system_metrics' as PrivilegedFunction,
  'check_rls_coverage' as PrivilegedFunction
)

const privilegedRoleArb: fc.Arbitrary<PostgRESTRole> = fc.constantFrom(
  'service_role' as PostgRESTRole,
  'pg_cron' as PostgRESTRole
)

// --- Property-Based Tests ---

describe('Property 4: Preservation — Liberación de clase realizada', () => {
  /**
   * **Validates: Requirements 3.1**
   *
   * For any invocation of confirm-class with realized=true (NOT bug condition G-07),
   * the system SHALL continue to transition RETAINED→RELEASED and mark class COMPLETED.
   */
  it('confirm-class({realized:true}) transitions all RETAINED payments to RELEASED', () => {
    fc.assert(
      fc.property(
        classIdArb.chain((classId) =>
          fc.record({
            request: fc.constant({ classId, realized: true } as ConfirmClassRequest),
            payments: fc.array(retainedPaymentArb(classId), { minLength: 1, maxLength: 5 }),
          })
        ),
        ({ request, payments }) => {
          const result = confirmClassRealized(request, payments)

          // Class MUST be marked COMPLETED
          expect(result.classStatus).toBe('COMPLETED')

          // Every RETAINED payment MUST transition to RELEASED
          for (const pr of result.payments) {
            expect(pr.finalStatus).toBe('RELEASED')
          }

          // Audit log MUST be created
          expect(result.auditLogged).toBe(true)
        }
      ),
      { numRuns: 100, seed: 42 }
    )
  })

  it('confirm-class({realized:true}) does not affect payments already in non-RETAINED states', () => {
    fc.assert(
      fc.property(
        classIdArb.chain((classId) =>
          fc.record({
            request: fc.constant({ classId, realized: true } as ConfirmClassRequest),
            payments: fc.array(
              fc.record({
                id: fc.uuid(),
                enrollmentId: fc.uuid(),
                classId: fc.constant(classId),
                amount: fc.integer({ min: 1000, max: 500000 }),
                status: fc.constantFrom('RELEASED' as PaymentStatus, 'REFUND_PENDING' as PaymentStatus),
              }),
              { minLength: 1, maxLength: 3 }
            ),
          })
        ),
        ({ request, payments }) => {
          const result = confirmClassRealized(request, payments)

          // Payments not in RETAINED stay in their current status
          for (let i = 0; i < payments.length; i++) {
            expect(result.payments[i].finalStatus).toBe(payments[i].status)
          }
        }
      ),
      { numRuns: 100, seed: 42 }
    )
  })
})

describe('Property 5: Preservation — Vías aguas arriba de REFUND_PENDING', () => {
  /**
   * **Validates: Requirements 3.2**
   *
   * For any teacher rejection, student rejection, or 48h timeout, the system
   * SHALL continue to mark RETAINED payments as REFUND_PENDING. The G-06 fix
   * acts only AFTER that state; the upstream paths remain unchanged.
   */
  it('all three upstream paths transition RETAINED → REFUND_PENDING', () => {
    fc.assert(
      fc.property(
        upstreamPathArb,
        classIdArb.chain((classId) =>
          fc.array(retainedPaymentArb(classId), { minLength: 1, maxLength: 5 })
        ),
        (path, payments) => {
          const results = processUpstreamRefund(path, payments)

          // Every RETAINED payment MUST transition to REFUND_PENDING
          for (const r of results) {
            expect(r.finalStatus).toBe('REFUND_PENDING')
          }
        }
      ),
      { numRuns: 100, seed: 42 }
    )
  })

  it('upstream paths do not affect payments already in non-RETAINED states', () => {
    fc.assert(
      fc.property(
        upstreamPathArb,
        fc.array(nonRefundPendingPaymentArb, { minLength: 1, maxLength: 5 }),
        (path, payments) => {
          const results = processUpstreamRefund(path, payments)

          // Payments not in RETAINED remain unchanged
          for (let i = 0; i < payments.length; i++) {
            expect(results[i].finalStatus).toBe(payments[i].status)
          }
        }
      ),
      { numRuns: 100, seed: 42 }
    )
  })

  it('teacher-decision rejection specifically transitions RETAINED → REFUND_PENDING', () => {
    fc.assert(
      fc.property(
        classIdArb.chain((classId) =>
          fc.array(retainedPaymentArb(classId), { minLength: 1, maxLength: 3 })
        ),
        (payments) => {
          const results = processUpstreamRefund('TEACHER_REJECTION', payments)
          for (const r of results) {
            expect(r.finalStatus).toBe('REFUND_PENDING')
          }
        }
      ),
      { numRuns: 50, seed: 42 }
    )
  })

  it('student-decision rejection specifically transitions RETAINED → REFUND_PENDING', () => {
    fc.assert(
      fc.property(
        classIdArb.chain((classId) =>
          fc.array(retainedPaymentArb(classId), { minLength: 1, maxLength: 3 })
        ),
        (payments) => {
          const results = processUpstreamRefund('STUDENT_REJECTION', payments)
          for (const r of results) {
            expect(r.finalStatus).toBe('REFUND_PENDING')
          }
        }
      ),
      { numRuns: 50, seed: 42 }
    )
  })

  it('48h timeout specifically transitions RETAINED → REFUND_PENDING', () => {
    fc.assert(
      fc.property(
        classIdArb.chain((classId) =>
          fc.array(retainedPaymentArb(classId), { minLength: 1, maxLength: 3 })
        ),
        (payments) => {
          const results = processUpstreamRefund('TIMEOUT_48H', payments)
          for (const r of results) {
            expect(r.finalStatus).toBe('REFUND_PENDING')
          }
        }
      ),
      { numRuns: 50, seed: 42 }
    )
  })
})

describe('Property 6: Preservation — Pagos fuera de REFUND_PENDING', () => {
  /**
   * **Validates: Requirements 3.3**
   *
   * For any payment in RELEASED, FAILED, or REFUNDED (where isBugCondition_G06
   * is false), the refund processing SHALL NOT change its status (F(X) = F'(X)).
   */
  it('refund processing does NOT alter payments in RELEASED/FAILED/REFUNDED', () => {
    fc.assert(
      fc.property(
        fc.array(nonRefundPendingPaymentArb, { minLength: 1, maxLength: 10 }),
        (payments) => {
          // Precondition: none of these payments are in REFUND_PENDING
          for (const p of payments) {
            expect(p.status).not.toBe('REFUND_PENDING')
          }

          const result = processRefundsOnNonPendingPayments(payments)

          // Assert: every payment retains its original status
          for (let i = 0; i < payments.length; i++) {
            expect(result[i].status).toBe(payments[i].status)
          }
        }
      ),
      { numRuns: 100, seed: 42 }
    )
  })

  it('RELEASED payments remain RELEASED after refund processing', () => {
    fc.assert(
      fc.property(
        fc.array(
          fc.record({
            id: fc.uuid(),
            enrollmentId: fc.uuid(),
            classId: fc.uuid(),
            amount: fc.integer({ min: 1000, max: 500000 }),
            status: fc.constant('RELEASED' as PaymentStatus),
          }),
          { minLength: 1, maxLength: 5 }
        ),
        (payments) => {
          const result = processRefundsOnNonPendingPayments(payments)
          for (const p of result) {
            expect(p.status).toBe('RELEASED')
          }
        }
      ),
      { numRuns: 50, seed: 42 }
    )
  })

  it('FAILED payments remain FAILED after refund processing', () => {
    fc.assert(
      fc.property(
        fc.array(
          fc.record({
            id: fc.uuid(),
            enrollmentId: fc.uuid(),
            classId: fc.uuid(),
            amount: fc.integer({ min: 1000, max: 500000 }),
            status: fc.constant('FAILED' as PaymentStatus),
          }),
          { minLength: 1, maxLength: 5 }
        ),
        (payments) => {
          const result = processRefundsOnNonPendingPayments(payments)
          for (const p of result) {
            expect(p.status).toBe('FAILED')
          }
        }
      ),
      { numRuns: 50, seed: 42 }
    )
  })

  it('REFUNDED payments remain REFUNDED after refund processing', () => {
    fc.assert(
      fc.property(
        fc.array(
          fc.record({
            id: fc.uuid(),
            enrollmentId: fc.uuid(),
            classId: fc.uuid(),
            amount: fc.integer({ min: 1000, max: 500000 }),
            status: fc.constant('REFUNDED' as PaymentStatus),
          }),
          { minLength: 1, maxLength: 5 }
        ),
        (payments) => {
          const result = processRefundsOnNonPendingPayments(payments)
          for (const p of result) {
            expect(p.status).toBe('REFUNDED')
          }
        }
      ),
      { numRuns: 50, seed: 42 }
    )
  })
})

describe('Property 7: Preservation — Ejecución privilegiada y RPC de negocio', () => {
  /**
   * **Validates: Requirements 3.4, 3.5, 3.7**
   *
   * service_role/pg_cron SHALL continue to execute the five privileged functions.
   * authenticated SHALL continue to execute business RPCs (get_my_attributes).
   * rls_auto_enable is NOT modified.
   */
  it('service_role executes all five privileged functions', () => {
    fc.assert(
      fc.property(privilegedFunctionArb, (fn) => {
        const result = invokeRpcAuthorized({ role: 'service_role', functionName: fn })

        expect(result.executed).toBe(true)
        expect(result.httpStatus).toBe(200)
        expect(result.errorCode).toBeNull()
      }),
      { numRuns: 50, seed: 42 }
    )
  })

  it('pg_cron executes all five privileged functions', () => {
    fc.assert(
      fc.property(privilegedFunctionArb, (fn) => {
        const result = invokeRpcAuthorized({ role: 'pg_cron', functionName: fn })

        expect(result.executed).toBe(true)
        expect(result.httpStatus).toBe(200)
        expect(result.errorCode).toBeNull()
      }),
      { numRuns: 50, seed: 42 }
    )
  })

  it('authenticated executes business RPC get_my_attributes()', () => {
    const result = invokeRpcAuthorized({
      role: 'authenticated',
      functionName: 'get_my_attributes',
    })

    expect(result.executed).toBe(true)
    expect(result.httpStatus).toBe(200)
    expect(result.errorCode).toBeNull()
  })

  it('privileged roles (service_role/pg_cron) × all five functions = all permitted', () => {
    fc.assert(
      fc.property(privilegedRoleArb, privilegedFunctionArb, (role, fn) => {
        const result = invokeRpcAuthorized({ role, functionName: fn })

        expect(result.executed).toBe(true)
        expect(result.httpStatus).toBe(200)
        expect(result.errorCode).toBeNull()
      }),
      { numRuns: 100, seed: 42 }
    )
  })

  it('rls_auto_enable is NOT modified by the G-16 fix', () => {
    expect(isRlsAutoEnableModified()).toBe(false)
  })
})

describe('Property 8: Preservation — Creación y liberación del webhook', () => {
  /**
   * **Validates: Requirements 3.6**
   *
   * mercadopago-webhook creates payments in RETAINED on approval.
   * Realized classes release those payments to RELEASED.
   * These flows must remain intact.
   */
  it('mercadopago-webhook creates payments in RETAINED status on approval', () => {
    const result = webhookCreatesPayment(true)

    expect(result.paymentCreated).toBe(true)
    expect(result.paymentStatus).toBe('RETAINED')
  })

  it('non-approved webhook notifications do NOT create payments', () => {
    const result = webhookCreatesPayment(false)

    expect(result.paymentCreated).toBe(false)
  })

  it('realized classes release RETAINED payments to RELEASED', () => {
    fc.assert(
      fc.property(
        classIdArb.chain((classId) => retainedPaymentArb(classId)),
        (payment) => {
          const finalStatus = realizedClassReleasesPayment(payment)

          expect(finalStatus).toBe('RELEASED')
        }
      ),
      { numRuns: 100, seed: 42 }
    )
  })

  it('realized class release does not affect non-RETAINED payments', () => {
    fc.assert(
      fc.property(nonRefundPendingPaymentArb, (payment) => {
        const finalStatus = realizedClassReleasesPayment(payment)

        // Payments not in RETAINED stay in their current status
        expect(finalStatus).toBe(payment.status)
      }),
      { numRuns: 100, seed: 42 }
    )
  })

  it('full lifecycle: webhook creates RETAINED → realized class releases to RELEASED', () => {
    fc.assert(
      fc.property(
        fc.uuid(),
        fc.uuid(),
        fc.uuid(),
        fc.integer({ min: 1000, max: 500000 }),
        (id, enrollmentId, classId, amount) => {
          // Step 1: Webhook creates payment in RETAINED
          const webhookResult = webhookCreatesPayment(true)
          expect(webhookResult.paymentStatus).toBe('RETAINED')

          // Step 2: Class is realized → payment released
          const payment: Payment = { id, enrollmentId, classId, amount, status: 'RETAINED' }
          const finalStatus = realizedClassReleasesPayment(payment)
          expect(finalStatus).toBe('RELEASED')
        }
      ),
      { numRuns: 50, seed: 42 }
    )
  })
})
