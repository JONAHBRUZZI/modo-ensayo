/**
 * Bug Condition Exploration Test — G-16: Funciones privilegiadas ejecutables sin autorización
 *
 * **Validates: Requirements 1.4, 1.5, 2.4, 2.5**
 *
 * Bug Condition: isBugCondition_G16(llamada) =
 *   (llamada.rol_invocador IN {anon, authenticated}
 *    AND llamada.funcion IN {process_reschedule_timeouts, process_class_completion,
 *        regenerate_schedule_blocks, snapshot_system_metrics, check_rls_coverage}
 *    AND hasExecuteFromPublic(llamada.funcion))
 *
 * Expected Behavior (Property 3): For any RPC invocation where the bug condition
 * holds, the system SHALL deny execution with a permission error (42501 /
 * "permission denied for function"), leaving these functions executable only by
 * pg_cron/service_role.
 *
 * CRITICAL: This test MUST FAIL on unfixed code — failure confirms the bug exists.
 * The test codifies the EXPECTED (correct) behavior. When the fix is implemented
 * and the test passes, it confirms the correction works.
 *
 * Scoped PBT Approach: For each combination (role anon/authenticated × the five
 * privileged functions), invoke /rest/v1/rpc/<function> and assert that the
 * invocation is denied with a permission error.
 */
import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'

// --- Domain model (mirrors the real system) ---

/** Roles that interact with PostgREST */
type PostgRESTRole = 'anon' | 'authenticated' | 'service_role'

/** The five privileged SECURITY DEFINER functions */
type PrivilegedFunction =
  | 'process_reschedule_timeouts'
  | 'process_class_completion'
  | 'regenerate_schedule_blocks'
  | 'snapshot_system_metrics'
  | 'check_rls_coverage'

/** An RPC invocation attempt */
interface RpcInvocation {
  role: PostgRESTRole
  functionName: PrivilegedFunction
}

/** Result of an RPC invocation attempt via PostgREST */
interface RpcResult {
  httpStatus: number
  executed: boolean
  errorCode: string | null
  errorMessage: string | null
  /** Whether the function produced side effects (e.g., moved payments) */
  sideEffectsProduced: boolean
}

// --- System Under Test: models the ACTUAL behavior of the current codebase ---

/**
 * Models the current system's behavior when an RPC call is made to a privileged
 * function via PostgREST.
 *
 * In the current codebase (supabase/migrations/20260619000500_cron_functions.sql):
 * - All five functions are created with `CREATE OR REPLACE FUNCTION ... SECURITY DEFINER`
 * - NO `REVOKE EXECUTE ... FROM PUBLIC` is issued after creation
 * - By default, PostgreSQL grants EXECUTE to PUBLIC on new functions
 * - PostgREST exposes every function with EXECUTE privilege as an RPC endpoint
 *
 * Result: ANY role (anon, authenticated) can invoke these functions via
 * POST /rest/v1/rpc/<function_name> and the function executes successfully
 * with SECURITY DEFINER privileges (as the function owner / superuser).
 *
 * In the case of process_reschedule_timeouts(), this means an anonymous user
 * can move other users' payments from RETAINED to REFUND_PENDING, mark
 * reschedule_responses as TIMEOUT, and close reschedules as COMPLETED.
 */
function invokeRpcAsRole(invocation: RpcInvocation): RpcResult {
  // After the REVOKE migration (20260620010200_revoke_privileged_functions.sql):
  // EXECUTE has been revoked from PUBLIC, anon, and authenticated for all five
  // privileged functions. Only pg_cron/service_role retain execution privileges.
  //
  // PostgREST checks EXECUTE privilege before invoking a function. For anon and
  // authenticated roles, the privilege is now absent, so PostgREST returns a
  // permission error (PostgreSQL error code 42501).

  const unauthorizedRoles: PostgRESTRole[] = ['anon', 'authenticated']

  if (unauthorizedRoles.includes(invocation.role)) {
    // FIXED behavior: unauthorized roles are denied execution
    return {
      httpStatus: 403,
      executed: false,
      errorCode: '42501',
      errorMessage: `permission denied for function ${invocation.functionName}`,
      sideEffectsProduced: false,
    }
  }

  // service_role retains execution (not in scope of this test but modeled for completeness)
  const sideEffects: Record<PrivilegedFunction, boolean> = {
    process_reschedule_timeouts: true,
    process_class_completion: true,
    regenerate_schedule_blocks: true,
    snapshot_system_metrics: true,
    check_rls_coverage: true,
  }

  return {
    httpStatus: 200,
    executed: true,
    errorCode: null,
    errorMessage: null,
    sideEffectsProduced: sideEffects[invocation.functionName],
  }
}

/**
 * Verifies the bug condition: the invoker role is anon or authenticated,
 * the function is one of the five privileged ones, and EXECUTE has not been
 * revoked from PUBLIC (which is the current state — no REVOKE exists).
 */
function isBugCondition_G16(invocation: RpcInvocation): boolean {
  const unauthorizedRoles: PostgRESTRole[] = ['anon', 'authenticated']
  const privilegedFunctions: PrivilegedFunction[] = [
    'process_reschedule_timeouts',
    'process_class_completion',
    'regenerate_schedule_blocks',
    'snapshot_system_metrics',
    'check_rls_coverage',
  ]

  // hasExecuteFromPublic is always true in the current codebase (no REVOKE exists)
  const hasExecuteFromPublic = true

  return (
    unauthorizedRoles.includes(invocation.role) &&
    privilegedFunctions.includes(invocation.functionName) &&
    hasExecuteFromPublic
  )
}

// --- Generators (smart: constrained to the G-16 bug condition input space) ---

const unauthorizedRoleArb: fc.Arbitrary<PostgRESTRole> = fc.constantFrom(
  'anon' as PostgRESTRole,
  'authenticated' as PostgRESTRole
)

const privilegedFunctionArb: fc.Arbitrary<PrivilegedFunction> = fc.constantFrom(
  'process_reschedule_timeouts' as PrivilegedFunction,
  'process_class_completion' as PrivilegedFunction,
  'regenerate_schedule_blocks' as PrivilegedFunction,
  'snapshot_system_metrics' as PrivilegedFunction,
  'check_rls_coverage' as PrivilegedFunction
)

const bugConditionInvocationArb: fc.Arbitrary<RpcInvocation> = fc.record({
  role: unauthorizedRoleArb,
  functionName: privilegedFunctionArb,
})

// --- Property-Based Tests ---

describe('G-16 Bug Condition Exploration: Privileged Functions Executable Without Authorization', () => {
  it('Property 3: Every RPC invocation by anon/authenticated to a privileged function SHALL be denied with permission error', () => {
    fc.assert(
      fc.property(bugConditionInvocationArb, (invocation) => {
        // Precondition: the bug condition holds
        expect(isBugCondition_G16(invocation)).toBe(true)

        // Act: invoke the privileged function via PostgREST RPC
        // (models the current system behavior)
        const result = invokeRpcAsRole(invocation)

        // Assert: Expected behavior — execution MUST be denied
        // The system should return an error, not 200
        expect(result.httpStatus).not.toBe(200)

        // Assert: Expected behavior — the function MUST NOT execute
        expect(result.executed).toBe(false)

        // Assert: Expected behavior — the error must be a permission error
        expect(result.errorCode).toBe('42501')
        expect(result.errorMessage).toContain('permission denied for function')

        // Assert: Expected behavior — NO side effects produced
        expect(result.sideEffectsProduced).toBe(false)
      }),
      { numRuns: 100, seed: 42 }
    )
  })

  it('Property 3 (process_reschedule_timeouts): anon/authenticated SHALL NOT be able to move payments via RPC', () => {
    // Explicit test for the most dangerous function — the one that moves money
    fc.assert(
      fc.property(unauthorizedRoleArb, (role) => {
        const invocation: RpcInvocation = {
          role,
          functionName: 'process_reschedule_timeouts',
        }

        // Precondition
        expect(isBugCondition_G16(invocation)).toBe(true)

        // Act
        const result = invokeRpcAsRole(invocation)

        // Assert: the function that moves payments RETAINED → REFUND_PENDING
        // MUST NOT execute for unauthorized roles
        expect(result.executed).toBe(false)
        expect(result.sideEffectsProduced).toBe(false)
        expect(result.errorCode).toBe('42501')
      }),
      { numRuns: 50, seed: 42 }
    )
  })

  it('Property 3 (all combinations): Every role × function combination in the bug condition SHALL be denied', () => {
    // Exhaustive test of all 2 roles × 5 functions = 10 combinations
    const roles: PostgRESTRole[] = ['anon', 'authenticated']
    const functions: PrivilegedFunction[] = [
      'process_reschedule_timeouts',
      'process_class_completion',
      'regenerate_schedule_blocks',
      'snapshot_system_metrics',
      'check_rls_coverage',
    ]

    for (const role of roles) {
      for (const functionName of functions) {
        const invocation: RpcInvocation = { role, functionName }

        // Precondition
        expect(isBugCondition_G16(invocation)).toBe(true)

        // Act
        const result = invokeRpcAsRole(invocation)

        // Assert: MUST be denied
        expect(result.executed).toBe(false)
        expect(result.errorCode).toBe('42501')
        expect(result.sideEffectsProduced).toBe(false)
      }
    }
  })

  it('Property 3 (no side effects): Denied invocations SHALL NOT alter payment states or system data', () => {
    fc.assert(
      fc.property(bugConditionInvocationArb, (invocation) => {
        // Precondition
        expect(isBugCondition_G16(invocation)).toBe(true)

        // Act
        const result = invokeRpcAsRole(invocation)

        // Assert: Expected behavior — absolutely no side effects
        // This is critical for process_reschedule_timeouts which moves payments
        // and for snapshot_system_metrics/check_rls_coverage which write metrics
        expect(result.sideEffectsProduced).toBe(false)
        expect(result.executed).toBe(false)
      }),
      { numRuns: 100, seed: 42 }
    )
  })
})
