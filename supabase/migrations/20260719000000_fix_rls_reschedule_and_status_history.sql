-- ============================================================
-- Cierre de RLS permisivo en reschedules y class_status_history (G-19, G-20)
-- ------------------------------------------------------------
-- Ambas políticas quedaron como "WITH CHECK (true)" desde la migración base
-- (20260619000300_rls_policies.sql), pensadas para que el INSERT lo hiciera
-- "el sistema" — pero al estar otorgadas al rol `authenticated`, cualquier
-- usuario logueado puede insertar directo vía PostgREST filas con datos
-- arbitrarios:
--   - reschedules: class_id/teacher_id/proposed_time de una clase que no
--     dicta ni administra (inyecta un reagendamiento falso).
--   - class_status_history: class_id/previous_status/new_status/changed_by
--     arbitrarios (falsifica la auditoría de estados de una clase).
--
-- Las escrituras legítimas de ambas tablas NO dependen de estas políticas:
--   - reschedules: la Edge Function `propose-reschedule` (y el resto del
--     mecanismo de reagendamiento) usa el cliente `service_role`, que
--     bypasea RLS por completo.
--   - class_status_history: el único escritor pensado es el trigger
--     `track_class_status()` (`SECURITY DEFINER`), que corre con los
--     privilegios del dueño de la función — también bypasea RLS.
--
-- Por eso el fix es simplemente retirar la política permisiva de INSERT para
-- `authenticated`, sin reemplazarla: sin una política permisiva, RLS deniega
-- el INSERT por defecto. Mismo patrón ya usado en `payment_sessions`,
-- `enrollments`, `payments`, `audit_logs` y `system_metrics` (solo
-- escritura vía service_role/trigger, sin política de cliente).
-- ============================================================

DROP POLICY IF EXISTS "resched_insert_auth" ON public.reschedules;
DROP POLICY IF EXISTS "csh_insert_system" ON public.class_status_history;
