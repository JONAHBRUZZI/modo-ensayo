-- ============================================================
-- SCHEDULED JOB FUNCTIONS + pg_cron schedules
-- ============================================================

-- 1. Completar clases expiradas
CREATE OR REPLACE FUNCTION public.process_class_completion()
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
BEGIN
  UPDATE public.classes SET status = 'POR_VALIDAR'
  WHERE status = 'PUBLISHED' AND start_time < now();
END;
$$;

-- 2. Procesar timeouts de reagendamiento
CREATE OR REPLACE FUNCTION public.process_reschedule_timeouts()
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
  r record;
BEGIN
  FOR r IN
    SELECT res.id, res.class_id FROM public.reschedules res
    WHERE res.status = 'TEACHER_ACCEPTED' AND res.response_deadline < now()
  LOOP
    UPDATE public.reschedule_responses SET response_type = 'TIMEOUT', responded_at = now()
    WHERE reschedule_id = r.id AND response_type IS NULL;

    UPDATE public.payments p SET status = 'REFUND_PENDING'
    FROM public.enrollments e
    JOIN public.reschedule_responses rr ON rr.user_id = e.student_id
    WHERE e.class_id = r.class_id AND p.enrollment_id = e.id
      AND p.status = 'RETAINED' AND rr.reschedule_id = r.id AND rr.response_type = 'TIMEOUT';

    UPDATE public.reschedules SET status = 'COMPLETED' WHERE id = r.id;
  END LOOP;
END;
$$;

-- 3. Regenerar bloques de horario
CREATE OR REPLACE FUNCTION public.regenerate_schedule_blocks()
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
  venue record;
  room record;
  schedule record;
  cfg record;
  day_date date;
  day_offset int;
  block_start timestamptz;
  block_end timestamptz;
  day_start timestamptz;
  day_end timestamptz;
BEGIN
  FOR venue IN SELECT id FROM public.venues WHERE status = 'APROBADA' LOOP
    SELECT * INTO cfg FROM public.venue_block_configs WHERE venue_id = venue.id;
    IF cfg IS NULL THEN CONTINUE; END IF;

    FOR schedule IN SELECT * FROM public.venue_schedules WHERE venue_id = venue.id LOOP
      FOR day_offset IN 0..6 LOOP
        day_date := current_date + day_offset;
        IF trim(to_char(day_date, 'DAY')) = schedule.day_of_week THEN
          day_start := day_date + schedule.open_time;
          day_end := day_date + schedule.close_time;
          block_start := day_start;

          WHILE block_start + make_interval(mins => cfg.block_duration_min) <= day_end LOOP
            block_end := block_start + make_interval(mins => cfg.block_duration_min);

            FOR room IN SELECT id FROM public.rooms WHERE venue_id = venue.id AND activa = true LOOP
              INSERT INTO public.room_schedule_blocks (room_id, start_time, end_time, status)
              VALUES (room.id, block_start, block_end, 'AVAILABLE')
              ON CONFLICT DO NOTHING;
            END LOOP;

            block_start := block_start + make_interval(mins => cfg.block_duration_min + cfg.gap_between_blocks_min);
          END LOOP;
        END IF;
      END LOOP;
    END LOOP;
  END LOOP;
END;
$$;

-- 4. Health check RLS
CREATE OR REPLACE FUNCTION public.check_rls_coverage()
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
  missing_count int;
BEGIN
  SELECT count(*) INTO missing_count
  FROM pg_tables WHERE schemaname = 'public' AND rowsecurity = false;

  IF missing_count > 0 THEN
    INSERT INTO public.system_metrics (metric_name, metric_value, labels)
    VALUES ('rls_missing_tables', missing_count,
      (SELECT jsonb_object_agg(tablename, 'missing_rls')
       FROM pg_tables WHERE schemaname = 'public' AND rowsecurity = false));
  END IF;
END;
$$;

-- 5. Snapshot métricas
CREATE OR REPLACE FUNCTION public.snapshot_system_metrics()
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
BEGIN
  INSERT INTO public.system_metrics (metric_name, metric_value, labels) VALUES
    ('total_users',       (SELECT count(*) FROM auth.users), '{}'::jsonb),
    ('active_classes',    (SELECT count(*) FROM public.classes WHERE status = 'PUBLISHED'), '{}'::jsonb),
    ('completed_classes', (SELECT count(*) FROM public.classes WHERE status = 'COMPLETED'), '{}'::jsonb),
    ('pending_venues',    (SELECT count(*) FROM public.venues WHERE status = 'PENDIENTE_APROBACION'), '{}'::jsonb),
    ('pending_identity',  (SELECT count(*) FROM public.identity_verifications WHERE status = 'PENDING'), '{}'::jsonb),
    ('retained_total',    (SELECT coalesce(sum(amount),0) FROM public.payments WHERE status = 'RETAINED'), '{}'::jsonb),
    ('released_total',    (SELECT coalesce(sum(amount),0) FROM public.payments WHERE status = 'RELEASED'), '{}'::jsonb),
    ('active_enrollments',(SELECT count(*) FROM public.enrollments WHERE status = 'ACTIVE'), '{}'::jsonb);
END;
$$;

-- ============================================================
-- pg_cron schedules
-- ============================================================
SELECT cron.schedule('process-class-completion', '*/30 * * * *',
  $$SELECT public.process_class_completion()$$);
SELECT cron.schedule('process-reschedule-timeouts', '0 * * * *',
  $$SELECT public.process_reschedule_timeouts()$$);
SELECT cron.schedule('regenerate-schedule-blocks', '0 4 * * 1',
  $$SELECT public.regenerate_schedule_blocks()$$);
SELECT cron.schedule('health-check-rls', '*/15 * * * *',
  $$SELECT public.check_rls_coverage()$$);
SELECT cron.schedule('snapshot-metrics', '0 * * * *',
  $$SELECT public.snapshot_system_metrics()$$);
SELECT cron.schedule('cleanup-old-metrics', '0 3 * * *',
  $$DELETE FROM public.system_metrics WHERE recorded_at < now() - interval '90 days'$$);
