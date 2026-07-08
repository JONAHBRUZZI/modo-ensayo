-- ============================================================
-- RPC: clases de la(s) sede(s) administradas por el usuario actual
-- ============================================================
-- Las policies de SELECT de `classes` solo dejan ver al profesor dueño, a los
-- inscritos y a las clases públicas. El admin de una sede NO puede ver las
-- clases agendadas en sus salas (necesario para "Clases de mi sede" y
-- "Clases por confirmar"). Esta función SECURITY DEFINER cubre ese hueco SIN
-- ampliar las policies: es de solo lectura y se auto-restringe a las sedes
-- cuyo `admin_id` es el usuario autenticado (auth.uid()).
--
-- p_status: si se pasa, filtra por ese estado (ej. 'POR_VALIDAR' para las
-- clases pendientes de confirmación). Si es NULL, devuelve todas menos DRAFT.

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
    (SELECT count(*) FROM public.attendances a
       WHERE a.class_id = c.id AND a.present) AS attendance_count
  FROM public.classes c
  JOIN public.rooms  r ON r.id = c.room_id
  JOIN public.venues v ON v.id = r.venue_id
  WHERE v.admin_id = auth.uid()
    AND (p_status IS NULL OR c.status::text = p_status)
    AND (p_status IS NOT NULL OR c.status <> 'DRAFT')
  ORDER BY c.start_time DESC NULLS LAST;
$$;

-- Solo usuarios autenticados; nunca anon. La función ya se auto-restringe por
-- auth.uid(), así que un usuario sin sedes simplemente recibe 0 filas.
REVOKE ALL ON FUNCTION public.get_venue_classes(text) FROM public;
REVOKE ALL ON FUNCTION public.get_venue_classes(text) FROM anon;
GRANT EXECUTE ON FUNCTION public.get_venue_classes(text) TO authenticated;
