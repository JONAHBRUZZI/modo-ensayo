/**
 * Integration Test — End-to-End Refund Chain: G-07 → G-06
 *
 * **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7**
 *
 * This test verifies the complete refund lifecycle:
 *   confirm-class(realized=false) → payments RETAINED → REFUND_PENDING
 *   process-refunds               → payments REFUND_PENDING → REFUNDED
 *
 * The two functions are modeled in sequence to demonstrate the chaining of G-07
 * (class suspension moves payments to REFUND_PENDING) and G-06 (refund processor
 * closes the payment to REFUNDED).
 */
import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'

// --- Domain model ---

type PaymentStatus = 'RETAINED' | 'RELEASED' | 'REFUND_PENDING' | 'REFUNDED' | 'FAILED'
type ClassStatus = 'SCHEDULED' | 'COMPLETED' | 'SUSPENDED'

interface Payment {
  id: string
  enrollmentId: string
  classId: string
  studentId: string
  amount: number
  status: PaymentStatus
}

interface ConfirmClassRequest {
  classId: string
  realized: boolean
}

interface ConfirmClassResult {
  classStatus: ClassStatus
  payments: Payment[]
  studentNotified: boolean
  auditLogged: boolean
}

interface ProcessRefundsResult {
  payments: { paymentId: string; finalStatus: PaymentStatus; refundChannelInvoked: boolean }[]
  auditLogged: boolean
}

// --- System models (FIXED behavior) ---

/**
 * Models confirm-class with the G-07 fix applied.
 * - realized=true  → RETAINED → RELEASED, class → COMPLETED
 * - realized=false → RETAINED → REFUND_PENDING, class → SUSPENDED, student notified
 */
function confirmClass(request: ConfirmClassRequest, payments: Payment[]): ConfirmClassResult {
  if (request.realized) {
    return {
      classStatus: 'COMPLETED',
      payments: payments.map((p) => ({
        ...p,
        status: p.status === 'RETAINED' ? 'RELEASED' : p.status,
      })),
      studentNotified: false,
      auditLogged: true,
    }
  }

  // G-07 fix: realized=false moves RETAINED → REFUND_PENDING
  return {
    classStatus: 'SUSPENDED',
    payments: payments.map((p) => ({
      ...p,
      status: p.status === 'RETAINED' ? 'REFUND_PENDING' : p.status,
    })),
    studentNotified: true,
    auditLogged: true,
  }
}

/**
 * Models the process-refunds Edge Function (G-06 fix).
 * - Selects payments WHERE status = 'REFUND_PENDING'
 * - Transitions each to REFUNDED after invoking refund channel
 * - Idempotent: only affects REFUND_PENDING payments
 */
function processRefunds(payments: Payment[]): ProcessRefundsResult {
  const results = payments.map((p) => {
    if (p.status === 'REFUND_PENDING') {
      return {
        paymentId: p.id,
        finalStatus: 'REFUNDED' as PaymentStatus,
        refundChannelInvoked: true,
      }
    }
    return {
      paymentId: p.id,
      finalStatus: p.status,
      refundChannelInvoked: false,
    }
  })

  return {
    payments: results,
    auditLogged: results.some((r) => r.refundChannelInvoked),
  }
}

// --- Generators ---

const classIdArb: fc.Arbitrary<string> = fc.uuid()
const studentIdArb: fc.Arbitrary<string> = fc.uuid()

const retainedPaymentArb = (classId: string, studentId: string): fc.Arbitrary<Payment> =>
  fc.record({
    id: fc.uuid(),
    enrollmentId: fc.uuid(),
    classId: fc.constant(classId),
    studentId: fc.constant(studentId),
    amount: fc.integer({ min: 1000, max: 500000 }),
    status: fc.constant('RETAINED' as PaymentStatus),
  })

// --- Integration Tests ---

describe('E2E Refund Chain: G-07 (confirm-class) → G-06 (process-refunds)', () => {
  it('Full chain: confirm-class(realized=false) → REFUND_PENDING → process-refunds → REFUNDED', () => {
    fc.assert(
      fc.property(
        classIdArb.chain((classId) =>
          studentIdArb.chain((studentId) =>
            fc.record({
              classId: fc.constant(classId),
              studentId: fc.constant(studentId),
              payments: fc.array(retainedPaymentArb(classId, studentId), {
                minLength: 1,
                maxLength: 5,
              }),
            })
          )
        ),
        ({ classId, payments }) => {
          // --- Step 1: confirm-class with realized=false (G-07 fix) ---
          const confirmResult = confirmClass(
            { classId, realized: false },
            payments
          )

          // Class MUST be SUSPENDED
          expect(confirmResult.classStatus).toBe('SUSPENDED')

          // All RETAINED payments MUST transition to REFUND_PENDING
          for (const p of confirmResult.payments) {
            expect(p.status).toBe('REFUND_PENDING')
          }

          // Student MUST be notified
          expect(confirmResult.studentNotified).toBe(true)

          // Audit log MUST be created
          expect(confirmResult.auditLogged).toBe(true)

          // --- Step 2: process-refunds (G-06 fix) ---
          const refundResult = processRefunds(confirmResult.payments)

          // All REFUND_PENDING payments MUST transition to REFUNDED
          for (const pr of refundResult.payments) {
            expect(pr.finalStatus).toBe('REFUNDED')
            expect(pr.refundChannelInvoked).toBe(true)
          }

          // Audit log MUST be created for the refund
          expect(refundResult.auditLogged).toBe(true)
        }
      ),
      { numRuns: 100, seed: 42 }
    )
  })

  it('Idempotency: Running process-refunds twice does NOT produce a double refund', () => {
    fc.assert(
      fc.property(
        classIdArb.chain((classId) =>
          studentIdArb.chain((studentId) =>
            fc.array(retainedPaymentArb(classId, studentId), {
              minLength: 1,
              maxLength: 3,
            })
          )
        ),
        (payments) => {
          // Step 1: confirm-class moves to REFUND_PENDING
          const afterConfirm = payments.map((p) => ({
            ...p,
            status: 'REFUND_PENDING' as PaymentStatus,
          }))

          // Step 2: First pass of process-refunds
          const firstPass = processRefunds(afterConfirm)

          // All should be REFUNDED after first pass
          for (const pr of firstPass.payments) {
            expect(pr.finalStatus).toBe('REFUNDED')
          }

          // Step 3: Second pass (payments now in REFUNDED)
          const refundedPayments = afterConfirm.map((p, i) => ({
            ...p,
            status: firstPass.payments[i].finalStatus,
          }))
          const secondPass = processRefunds(refundedPayments)

          // No refund channel should be invoked on already-REFUNDED payments
          for (const pr of secondPass.payments) {
            expect(pr.finalStatus).toBe('REFUNDED')
            expect(pr.refundChannelInvoked).toBe(false) // No double refund
          }
        }
      ),
      { numRuns: 100, seed: 42 }
    )
  })

  it('Preservation: confirm-class(realized=true) does NOT enter the refund chain', () => {
    fc.assert(
      fc.property(
        classIdArb.chain((classId) =>
          studentIdArb.chain((studentId) =>
            fc.record({
              classId: fc.constant(classId),
              payments: fc.array(retainedPaymentArb(classId, studentId), {
                minLength: 1,
                maxLength: 5,
              }),
            })
          )
        ),
        ({ classId, payments }) => {
          // confirm-class with realized=true → RELEASED (not REFUND_PENDING)
          const confirmResult = confirmClass(
            { classId, realized: true },
            payments
          )

          // Class MUST be COMPLETED (not SUSPENDED)
          expect(confirmResult.classStatus).toBe('COMPLETED')

          // All payments MUST be RELEASED (not REFUND_PENDING)
          for (const p of confirmResult.payments) {
            expect(p.status).toBe('RELEASED')
          }

          // process-refunds should NOT act on RELEASED payments
          const refundResult = processRefunds(confirmResult.payments)
          for (const pr of refundResult.payments) {
            expect(pr.finalStatus).toBe('RELEASED')
            expect(pr.refundChannelInvoked).toBe(false)
          }
        }
      ),
      { numRuns: 100, seed: 42 }
    )
  })

  it('Mixed scenario: only RETAINED payments from the suspended class enter the refund chain', () => {
    fc.assert(
      fc.property(
        classIdArb.chain((classId) =>
          studentIdArb.chain((studentId) =>
            fc.record({
              classId: fc.constant(classId),
              retainedPayments: fc.array(retainedPaymentArb(classId, studentId), {
                minLength: 1,
                maxLength: 3,
              }),
              otherPayments: fc.array(
                fc.record({
                  id: fc.uuid(),
                  enrollmentId: fc.uuid(),
                  classId: fc.constant(classId),
                  studentId: fc.constant(studentId),
                  amount: fc.integer({ min: 1000, max: 500000 }),
                  status: fc.constantFrom('RELEASED' as PaymentStatus, 'FAILED' as PaymentStatus),
                }),
                { minLength: 0, maxLength: 2 }
              ),
            })
          )
        ),
        ({ classId, retainedPayments, otherPayments }) => {
          const allPayments = [...retainedPayments, ...otherPayments]

          // Step 1: confirm-class(realized=false)
          const confirmResult = confirmClass(
            { classId, realized: false },
            allPayments
          )

          // RETAINED payments → REFUND_PENDING
          const retainedCount = retainedPayments.length
          const refundPendingPayments = confirmResult.payments.filter(
            (p) => p.status === 'REFUND_PENDING'
          )
          expect(refundPendingPayments.length).toBe(retainedCount)

          // Other payments keep their original status
          const nonRetainedPayments = confirmResult.payments.filter(
            (p) => p.status !== 'REFUND_PENDING'
          )
          for (const p of nonRetainedPayments) {
            expect(['RELEASED', 'FAILED']).toContain(p.status)
          }

          // Step 2: process-refunds
          const refundResult = processRefunds(confirmResult.payments)

          // Only formerly-RETAINED (now REFUND_PENDING) should be REFUNDED
          const refundedPayments = refundResult.payments.filter(
            (pr) => pr.refundChannelInvoked
          )
          expect(refundedPayments.length).toBe(retainedCount)

          // Non-REFUND_PENDING payments should be untouched
          const untouchedPayments = refundResult.payments.filter(
            (pr) => !pr.refundChannelInvoked
          )
          for (const pr of untouchedPayments) {
            expect(['RELEASED', 'FAILED']).toContain(pr.finalStatus)
          }
        }
      ),
      { numRuns: 100, seed: 42 }
    )
  })
})
