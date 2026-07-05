-- ============================================================
-- Detalle de clase + alumnos: acceso también para el PROFESOR de la clase
-- ============================================================
-- Antes solo el admin de la sede (v.admin_id) podía ver el detalle. Ahora el
-- profesor asignado (c.teacher_id) también, para ver de qué trata su clase,
-- los alumnos y su honorario. El tipo de retorno no cambia (CREATE OR REPLACE).
-- ============================================================

CREATE OR REPLACE FUNCTION public.get_venue_class_detail(p_class_id uuid)
RETURNS TABLE (
  id             uuid,
  title          text,
  discipline     text,
  level          text,
  description    text,
  status         text,
  start_time     timestamptz,
  end_time       timestamptz,
  duration       int,
  price          numeric,
  honorario      numeric,
  capacity       int,
  tipo_clase     text,
  room_name      text,
  venue_name     text,
  teacher_id     uuid,
  teacher_name   text,
  teacher_email  text,
  enrolled_count bigint
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  SELECT c.id, c.title, c.discipline, c.level::text, c.description, c.status::text,
         c.start_time, c.end_time, c.duration, c.price, c.honorario,
         c.capacity, c.tipo_clase::text, r.name, v.name,
         c.teacher_id, tpr.full_name, tu.email::text,
         (SELECT COUNT(*) FROM public.enrollments e WHERE e.class_id = c.id AND e.status = 'ACTIVE')
  FROM public.classes c
  JOIN public.rooms  r ON r.id = c.room_id
  JOIN public.venues v ON v.id = r.venue_id
  LEFT JOIN public.profiles tpr ON tpr.id = c.teacher_id
  LEFT JOIN auth.users      tu  ON tu.id  = c.teacher_id
  WHERE c.id = p_class_id
    AND (v.admin_id = auth.uid() OR c.teacher_id = auth.uid());
$$;
REVOKE ALL ON FUNCTION public.get_venue_class_detail(uuid) FROM public;
REVOKE ALL ON FUNCTION public.get_venue_class_detail(uuid) FROM anon;
GRANT EXECUTE ON FUNCTION public.get_venue_class_detail(uuid) TO authenticated;

CREATE OR REPLACE FUNCTION public.get_venue_class_students(p_class_id uuid)
RETURNS TABLE (
  enrollment_id    uuid,
  attendee_name    text,
  student_email    text,
  beneficiary_type text,
  status           text,
  present          boolean
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  SELECT e.id,
         COALESCE(a.name, pr.full_name, u.email::text) AS attendee_name,
         u.email::text,
         e.beneficiary_type,
         e.status,
         att.present
  FROM public.enrollments e
  JOIN public.classes c ON c.id = e.class_id
  JOIN public.rooms   r ON r.id = c.room_id
  JOIN public.venues  v ON v.id = r.venue_id
  LEFT JOIN public.profiles   pr ON pr.id = e.student_id
  LEFT JOIN auth.users        u  ON u.id  = e.student_id
  LEFT JOIN public.associates a  ON a.id  = e.beneficiary_id
  LEFT JOIN public.attendances att
         ON att.class_id = e.class_id
        AND att.beneficiary_id = COALESCE(e.beneficiary_id, e.student_id)
  WHERE e.class_id = p_class_id
    AND (v.admin_id = auth.uid() OR c.teacher_id = auth.uid())
  ORDER BY 2;
$$;
REVOKE ALL ON FUNCTION public.get_venue_class_students(uuid) FROM public;
REVOKE ALL ON FUNCTION public.get_venue_class_students(uuid) FROM anon;
GRANT EXECUTE ON FUNCTION public.get_venue_class_students(uuid) TO authenticated;
