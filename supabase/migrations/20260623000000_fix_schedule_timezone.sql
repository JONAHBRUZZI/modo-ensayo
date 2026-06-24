-- ============================================================
-- Fix de zona horaria y robustez del generador de bloques de horario
-- ============================================================
-- Contexto: el horario de la sede genera los bloques de disponibilidad de cada
-- sala (room_schedule_blocks) mediante regenerate_schedule_blocks(). Tenía 4
-- problemas:
--   1. Guardaba las horas como UTC sin aplicar la zona de la sede
--      (America/Santiago), produciendo un desfase de 1 hora en el calendario.
--   2. El día de la semana se calculaba con to_char(...,'DAY'), dependiente del
--      locale del servidor (en español no calzaba con 'MONDAY', etc.).
--   3. No existía un constraint único, así que el cron semanal podía DUPLICAR
--      bloques (el ON CONFLICT DO NOTHING no tenía columna a la cual apuntar).
--   4. Sólo generaba 7 días; se amplía a 30 para permitir reservas con antelación.

-- 1. Eliminar posibles duplicados (mismo room_id + start_time) antes del constraint.
DELETE FROM public.room_schedule_blocks a
USING public.room_schedule_blocks b
WHERE a.ctid < b.ctid
  AND a.room_id = b.room_id
  AND a.start_time = b.start_time;

-- 2. Constraint único: hace idempotente la regeneración (re-correr no duplica).
ALTER TABLE public.room_schedule_blocks
  DROP CONSTRAINT IF EXISTS uq_rsb_room_start;
ALTER TABLE public.room_schedule_blocks
  ADD CONSTRAINT uq_rsb_room_start UNIQUE (room_id, start_time);

-- 3. Generador corregido: zona horaria de la sede + día de semana robusto + 30 días.
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
  dow_name text;
BEGIN
  FOR venue IN SELECT id FROM public.venues WHERE status = 'APROBADA' LOOP
    SELECT * INTO cfg FROM public.venue_block_configs WHERE venue_id = venue.id;
    IF cfg IS NULL THEN CONTINUE; END IF;

    FOR schedule IN SELECT * FROM public.venue_schedules WHERE venue_id = venue.id LOOP
      -- 30 días hacia adelante (el cron semanal mantiene la ventana fresca).
      FOR day_offset IN 0..29 LOOP
        day_date := current_date + day_offset;

        -- Día de la semana independiente del locale (0=domingo .. 6=sábado).
        dow_name := CASE EXTRACT(DOW FROM day_date)::int
          WHEN 1 THEN 'MONDAY'   WHEN 2 THEN 'TUESDAY'  WHEN 3 THEN 'WEDNESDAY'
          WHEN 4 THEN 'THURSDAY' WHEN 5 THEN 'FRIDAY'   WHEN 6 THEN 'SATURDAY'
          WHEN 0 THEN 'SUNDAY'
        END;

        IF dow_name = schedule.day_of_week THEN
          -- Interpreta apertura/cierre como hora local de la sede y la convierte
          -- al instante UTC correcto (maneja horario de verano automáticamente).
          day_start := (day_date + schedule.open_time)  AT TIME ZONE 'America/Santiago';
          day_end   := (day_date + schedule.close_time) AT TIME ZONE 'America/Santiago';
          block_start := day_start;

          WHILE block_start + make_interval(mins => cfg.block_duration_min) <= day_end LOOP
            block_end := block_start + make_interval(mins => cfg.block_duration_min);

            FOR room IN SELECT id FROM public.rooms WHERE venue_id = venue.id AND activa = true LOOP
              INSERT INTO public.room_schedule_blocks (room_id, start_time, end_time, status)
              VALUES (room.id, block_start, block_end, 'AVAILABLE')
              ON CONFLICT (room_id, start_time) DO NOTHING;
            END LOOP;

            block_start := block_start + make_interval(mins => cfg.block_duration_min + cfg.gap_between_blocks_min);
          END LOOP;
        END IF;
      END LOOP;
    END LOOP;
  END LOOP;
END;
$$;

-- 4. Mantener la función como privilegiada (solo se invoca vía Edge Function / cron).
REVOKE EXECUTE ON FUNCTION public.regenerate_schedule_blocks() FROM PUBLIC, anon, authenticated;

-- 5. Reset de disponibilidad: limpia los bloques AVAILABLE (NO toca OCCUPIED ni
--    MAINTENANCE) y regenera con la zona horaria corregida.
DELETE FROM public.room_schedule_blocks WHERE status = 'AVAILABLE';
SELECT public.regenerate_schedule_blocks();
