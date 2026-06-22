-- ============================================================
-- G-16 FIX: Revoke EXECUTE on privileged SECURITY DEFINER functions
-- from PUBLIC, anon, and authenticated roles.
--
-- These functions are intended to run only via pg_cron (as superuser)
-- or service_role. Revoking from PUBLIC removes PostgREST RPC access
-- for anon/authenticated.
--
-- REVOKE is idempotent: revoking a privilege already absent is a no-op.
-- Does NOT affect: get_my_attributes() (legitimate authenticated RPC),
--                  rls_auto_enable (event trigger, not callable via RPC).
-- ============================================================

REVOKE EXECUTE ON FUNCTION public.process_reschedule_timeouts() FROM PUBLIC, anon, authenticated;
REVOKE EXECUTE ON FUNCTION public.process_class_completion() FROM PUBLIC, anon, authenticated;
REVOKE EXECUTE ON FUNCTION public.regenerate_schedule_blocks() FROM PUBLIC, anon, authenticated;
REVOKE EXECUTE ON FUNCTION public.snapshot_system_metrics() FROM PUBLIC, anon, authenticated;
REVOKE EXECUTE ON FUNCTION public.check_rls_coverage() FROM PUBLIC, anon, authenticated;
