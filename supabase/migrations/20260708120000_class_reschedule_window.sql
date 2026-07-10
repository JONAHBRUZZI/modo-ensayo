-- ============================================================
-- Ventana de reagendamiento tras "clase no realizada" (reembolso diferido)
-- ============================================================
-- Antes, marcar una clase como no realizada reembolsaba de inmediato. Ahora
-- confirm-class abre una ventana de 24h (reschedule_deadline) durante la cual el
-- profesor independiente (PROPIA) o la sede (ASIGNADA) puede reagendar. Los pagos
-- quedan RETAINED y las inscripciones ACTIVE en ese plazo.
--
-- Este cron es la versión DIFERIDA del reembolso: al vencer la ventana sin que se
-- haya reagendado, reembolsa a los alumnos y cierra la clase.
-- ============================================================

ALTER TABLE public.classes
  ADD COLUMN IF NOT EXISTS reschedule_deadline timestamptz;

CREATE INDEX IF NOT EXISTS idx_classes_reschedule_deadline
  ON public.classes(reschedule_deadline)
  WHERE reschedule_deadline IS NOT NULL;

CREATE OR REPLACE FUNCTION public.process_class_reschedule_timeouts()
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
  c record;
BEGIN
  FOR c IN
    SELECT id FROM public.classes
    WHERE status = 'SUSPENDED'
      AND reschedule_deadline IS NOT NULL
      AND reschedule_deadline < now()
  LOOP
    -- Reembolso: pagos retenidos de las inscripciones activas → REFUND_PENDING.
    UPDATE public.payments p SET status = 'REFUND_PENDING'
    FROM public.enrollments e
    WHERE p.enrollment_id = e.id AND e.class_id = c.id
      AND e.status = 'ACTIVE' AND p.status = 'RETAINED';

    -- Notifica a los alumnos inscritos.
    INSERT INTO public.notifications (user_id, title, message, type)
    SELECT e.student_id, 'Clase no reagendada',
           'La clase no se reagendó a tiempo. Se procesará tu reembolso.', 'CLASS_SUSPENDED'
    FROM public.enrollments e
    WHERE e.class_id = c.id AND e.status = 'ACTIVE';

    -- Cancela las inscripciones y cierra la ventana.
    UPDATE public.enrollments SET status = 'CANCELLED'
    WHERE class_id = c.id AND status = 'ACTIVE';

    UPDATE public.room_schedule_blocks SET status = 'AVAILABLE', class_id = NULL
    WHERE class_id = c.id AND status = 'OCCUPIED';

    UPDATE public.classes SET reschedule_deadline = NULL WHERE id = c.id;
  END LOOP;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.process_class_reschedule_timeouts()
  FROM PUBLIC, anon, authenticated;

-- Agenda cada hora (idempotente ante re-aplicación).
DO $$
BEGIN
  PERFORM cron.unschedule('process-class-reschedule-timeouts');
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

SELECT cron.schedule('process-class-reschedule-timeouts', '0 * * * *',
  $$SELECT public.process_class_reschedule_timeouts()$$);
