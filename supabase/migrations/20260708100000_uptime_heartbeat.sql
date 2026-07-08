-- ============================================================
-- Disponibilidad (M4): latido interno de uptime
-- ------------------------------------------------------------
-- Cada 5 minutos pg_cron inserta un "latido" en uptime_checks. La métrica M4 se
-- calcula como (latidos registrados / latidos esperados) en la ventana: si el
-- backend se cae, el cron no corre y quedan huecos que bajan el porcentaje.
-- Autocontenido (sin servicios externos). Solo service_role lo lee (admin-metrics).
-- ============================================================

CREATE TABLE IF NOT EXISTS public.uptime_checks (
  id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  checked_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_uptime_checked_at ON public.uptime_checks(checked_at DESC);

ALTER TABLE public.uptime_checks ENABLE ROW LEVEL SECURITY;
-- Intencional: sin políticas de cliente. Solo service_role (Edge Functions) accede.

-- Latido cada 5 minutos (cron.schedule hace upsert por nombre: idempotente).
SELECT cron.schedule('uptime-heartbeat', '*/5 * * * *',
  $$INSERT INTO public.uptime_checks DEFAULT VALUES$$);

-- Retención: conserva 30 días.
SELECT cron.schedule('cleanup-uptime-checks', '0 3 * * *',
  $$DELETE FROM public.uptime_checks WHERE checked_at < now() - interval '30 days'$$);
