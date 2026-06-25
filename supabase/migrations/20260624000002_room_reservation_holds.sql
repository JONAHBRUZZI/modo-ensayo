-- ============================================================
-- Soporte de reserva temporal (HELD) para el pago de arriendo de salas
-- ============================================================
-- Depende de 20260624000001 (que agrega el valor 'HELD' al enum block_status).

-- 1. Columnas para rastrear el hold temporal.
ALTER TABLE public.room_schedule_blocks
  ADD COLUMN IF NOT EXISTS held_until timestamptz,
  ADD COLUMN IF NOT EXISTS held_by   uuid REFERENCES auth.users(id);

-- 2. Libera los holds vencidos: bloques HELD cuyo held_until ya pasó vuelven a
--    estar disponibles (el profesor no completó el pago a tiempo).
CREATE OR REPLACE FUNCTION public.release_expired_holds()
RETURNS void LANGUAGE sql SECURITY DEFINER SET search_path = public
AS $$
  UPDATE public.room_schedule_blocks
  SET status = 'AVAILABLE', held_until = NULL, held_by = NULL
  WHERE status = 'HELD' AND held_until IS NOT NULL AND held_until < now();
$$;

REVOKE EXECUTE ON FUNCTION public.release_expired_holds() FROM PUBLIC, anon, authenticated;

-- 3. Cron cada 5 minutos (idempotente: re-programa si ya existía).
DO $$ BEGIN
  PERFORM cron.unschedule('release-expired-holds');
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
SELECT cron.schedule('release-expired-holds', '*/5 * * * *',
  $$SELECT public.release_expired_holds()$$);
