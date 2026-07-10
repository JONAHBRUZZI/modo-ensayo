-- ============================================================
-- get_venue_classes: agregar tipo_clase y nombre del profesor
-- ------------------------------------------------------------
-- La lista de clases de la sede (y "por confirmar") necesita distinguir de un
-- vistazo si la clase es de la propia sede (tipo_clase = 'ASIGNADA', dictada por
-- un Maestro Dependiente) o de un profesor independiente que arrendó la sala
-- (tipo_clase = 'PROPIA'). Se agrega también teacher_name para mostrarlo.
--
-- Cambia la firma (RETURNS TABLE), así que hay que DROP antes de recrear.
-- ============================================================

DROP FUNCTION IF EXISTS public.get_venue_classes(text);

CREATE OR REPLACE FUNCTION public.get_venue_classes(p_status text DEFAULT NULL)
RETURNS TABLE (
  id               uuid,
  title            text,
  discipline       text,
  level            text,
  status           text,
  start_time       timestamptz,
  end_time         timestamptz,
  price            numeric,
  capacity         int,
  room_id          uuid,
  room_name        text,
  venue_id         uuid,
  venue_name       text,
  teacher_id       uuid,
  teacher_name     text,
  tipo_clase       text,
  attendance_count bigint
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  SELECT
    c.id,
    c.title,
    c.discipline::text,
    c.level::text,
    c.status::text,
    c.start_time,
    c.end_time,
    c.price,
    c.capacity,
    c.room_id,
    r.name AS room_name,
    v.id   AS venue_id,
    v.name AS venue_name,
    c.teacher_id,
    tpr.full_name    AS teacher_name,
    c.tipo_clase::text,
    (SELECT count(*) FROM public.attendances a
       WHERE a.class_id = c.id AND a.present) AS attendance_count
  FROM public.classes c
  JOIN public.rooms  r ON r.id = c.room_id
  JOIN public.venues v ON v.id = r.venue_id
  LEFT JOIN public.profiles tpr ON tpr.id = c.teacher_id
  WHERE v.admin_id = auth.uid()
    AND (p_status IS NULL OR c.status::text = p_status)
    AND (p_status IS NOT NULL OR c.status <> 'DRAFT')
  ORDER BY c.start_time DESC NULLS LAST;
$$;

REVOKE ALL ON FUNCTION public.get_venue_classes(text) FROM public;
REVOKE ALL ON FUNCTION public.get_venue_classes(text) FROM anon;
GRANT EXECUTE ON FUNCTION public.get_venue_classes(text) TO authenticated;
