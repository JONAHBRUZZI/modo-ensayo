-- ============================================================
-- Sede: clases ASIGNADA + ingresos + profesores dependientes
-- ============================================================

-- 1. Tabla venue_teachers: relación profesor dependiente ↔ sede
CREATE TABLE IF NOT EXISTS public.venue_teachers (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  venue_id    uuid NOT NULL REFERENCES public.venues(id) ON DELETE CASCADE,
  teacher_id  uuid NOT NULL REFERENCES auth.users(id),
  status      text NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
  created_at  timestamptz NOT NULL DEFAULT now(),
  UNIQUE(venue_id, teacher_id)
);
CREATE INDEX IF NOT EXISTS idx_vt_venue ON public.venue_teachers(venue_id);
CREATE INDEX IF NOT EXISTS idx_vt_teacher ON public.venue_teachers(teacher_id);

ALTER TABLE public.venue_teachers ENABLE ROW LEVEL SECURITY;

-- El admin de sede gestiona sus profesores; ADMIN ve todo
DROP POLICY IF EXISTS vt_select_own ON public.venue_teachers;
CREATE POLICY vt_select_own ON public.venue_teachers
  FOR SELECT TO authenticated
  USING (
    public.is_venue_admin(venue_id)
    OR public.has_role('ADMIN')
  );

DROP POLICY IF EXISTS vt_insert_own ON public.venue_teachers;
CREATE POLICY vt_insert_own ON public.venue_teachers
  FOR INSERT TO authenticated
  WITH CHECK (public.is_venue_admin(venue_id));

DROP POLICY IF EXISTS vt_delete_own ON public.venue_teachers;
CREATE POLICY vt_delete_own ON public.venue_teachers
  FOR DELETE TO authenticated
  USING (public.is_venue_admin(venue_id));

-- 2. Columna honorario en classes (costo fijo del profe dependiente en ASIGNADA)
ALTER TABLE public.classes
  ADD COLUMN IF NOT EXISTS honorario numeric(12,2);

-- 3. RPC get_venue_professors: profesores dependientes de las sedes del usuario
CREATE OR REPLACE FUNCTION public.get_venue_professors()
RETURNS TABLE (
  id              uuid,
  venue_id        uuid,
  teacher_id      uuid,
  status          text,
  email           text,
  full_name       text
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  SELECT
    vt.id,
    vt.venue_id,
    vt.teacher_id,
    vt.status,
    u.email::text,
    (u.raw_user_meta_data ->> 'full_name')::text AS full_name
  FROM public.venue_teachers vt
  JOIN auth.users u ON u.id = vt.teacher_id
  JOIN public.venues  v ON v.id = vt.venue_id
  WHERE v.admin_id = auth.uid()
  ORDER BY u.email;
$$;

REVOKE ALL ON FUNCTION public.get_venue_professors() FROM public;
REVOKE ALL ON FUNCTION public.get_venue_professors() FROM anon;
GRANT EXECUTE ON FUNCTION public.get_venue_professors() TO authenticated;

-- 4. RPC get_venue_metrics: ingresos por fuente (arriendo / clases) + totales
--    p_granularidad = 'month' (por defecto) o 'year'
CREATE OR REPLACE FUNCTION public.get_venue_metrics(p_granularidad text DEFAULT 'month')
RETURNS TABLE (
  periodo          text,
  ingreso_arriendo numeric,
  ingreso_clases   numeric,
  ingreso_total    numeric
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  WITH venue_rooms AS (
    -- Salas de las sedes del usuario
    SELECT r.id AS room_id, v.id AS venue_id, v.admin_id
    FROM public.rooms r
    JOIN public.venues v ON v.id = r.venue_id
    WHERE v.admin_id = auth.uid()
  ),
  -- Ingreso por arriendo: bloques OCCUPIED con clase vinculada (clase pagada)
  -- Sumamos el precio de la clase asociada al bloque como proxy del ingreso
  -- (en producción el cobro real viene de payment_sessions; en MVP usamos el precio de clase)
  arriendos AS (
    SELECT
      CASE WHEN p_granularidad = 'year'
        THEN to_char(c.start_time, 'YYYY')
        ELSE to_char(c.start_time, 'YYYY-MM')
      END AS periodo,
      COALESCE(SUM(c.price), 0) AS total
    FROM public.room_schedule_blocks rsb
    JOIN venue_rooms vr ON vr.room_id = rsb.room_id
    LEFT JOIN public.classes c ON c.id = rsb.class_id
    WHERE rsb.status = 'OCCUPIED'
      AND rsb.class_id IS NOT NULL
      AND c.tipo_clase = 'PROPIA'
    GROUP BY 1
  ),
  -- Ingreso por clases: clases ASIGNADA donde la sede es la dueña
  -- Revenue = precio × alumnos inscritos; margen = revenue − honorario
  clases_asignadas AS (
    SELECT
      CASE WHEN p_granularidad = 'year'
        THEN to_char(c.start_time, 'YYYY')
        ELSE to_char(c.start_time, 'YYYY-MM')
      END AS periodo,
      COALESCE(SUM(
        (SELECT COUNT(*) FROM public.enrollments e
         WHERE e.class_id = c.id AND e.status = 'ACTIVE')
        * COALESCE(c.price, 0)
        - COALESCE(c.honorario, 0)
      ), 0) AS total
    FROM public.classes c
    JOIN public.rooms r ON r.id = c.room_id
    JOIN venue_rooms vr ON vr.room_id = r.id
    WHERE c.tipo_clase = 'ASIGNADA'
      AND c.status NOT IN ('DRAFT', 'CANCELLED')
    GROUP BY 1
  ),
  -- Union de todas las fuentes
  combined AS (
    SELECT periodo, total, 'arriendo' AS fuente FROM arriendos
    UNION ALL
    SELECT periodo, total, 'clases' AS fuente FROM clases_asignadas
  )
  SELECT
    c.periodo,
    COALESCE(SUM(c.total) FILTER (WHERE c.fuente = 'arriendo'), 0) AS ingreso_arriendo,
    COALESCE(SUM(c.total) FILTER (WHERE c.fuente = 'clases'), 0)   AS ingreso_clases,
    COALESCE(SUM(c.total), 0) AS ingreso_total
  FROM combined c
  GROUP BY c.periodo
  ORDER BY c.periodo;
$$;

REVOKE ALL ON FUNCTION public.get_venue_metrics(text) FROM public;
REVOKE ALL ON FUNCTION public.get_venue_metrics(text) FROM anon;
GRANT EXECUTE ON FUNCTION public.get_venue_metrics(text) TO authenticated;

-- 5. Policy admin en payment_sessions
DROP POLICY IF EXISTS psess_select_admin ON public.payment_sessions;
CREATE POLICY psess_select_admin ON public.payment_sessions
  FOR SELECT TO authenticated
  USING (public.has_role('ADMIN'));

-- 6. Policies admin en app_settings (lectura + escritura desde admin)
DROP POLICY IF EXISTS appset_select_admin ON public.app_settings;
CREATE POLICY appset_select_admin ON public.app_settings
  FOR SELECT TO authenticated
  USING (public.has_role('ADMIN'));

DROP POLICY IF EXISTS appset_update_admin ON public.app_settings;
CREATE POLICY appset_update_admin ON public.app_settings
  FOR UPDATE TO authenticated
  USING (public.has_role('ADMIN'))
  WITH CHECK (public.has_role('ADMIN'));

-- 7. Settings por defecto para comisiones
INSERT INTO public.app_settings (key, value)
VALUES ('room_reservation_commission_pct', '10'::jsonb)
ON CONFLICT (key) DO NOTHING;

INSERT INTO public.app_settings (key, value)
VALUES ('marketplace_commission_pct', '10'::jsonb)
ON CONFLICT (key) DO NOTHING;
