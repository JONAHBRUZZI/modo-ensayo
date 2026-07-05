-- ============================================================
-- Separar ingreso de clases (recaudación) del egreso a profesores (honorarios)
-- ============================================================
-- Antes get_venue_metrics.ingreso_clases era el MARGEN (alumnos − honorario), que
-- salía negativo si el honorario superaba lo recaudado. Ahora:
--   ingreso_clases = recaudación de alumnos (alumnos activos × precio)
--   egreso_profes  = suma de honorarios comprometidos a profes dependientes
--   total          = arriendo + ingreso_clases − egreso_profes (resultado neto)
-- Además get_venue_teacher_payouts: cuánto se le debe a cada profesor.
-- ============================================================

CREATE OR REPLACE FUNCTION public.get_venue_metrics(p_granularidad text DEFAULT 'month')
RETURNS TABLE (
  periodo          text,
  ingreso_arriendo numeric,
  ingreso_clases   numeric,
  egreso_profes    numeric,
  total            numeric
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  WITH fmt AS (
    SELECT CASE WHEN p_granularidad = 'year' THEN 'YYYY' ELSE 'YYYY-MM' END AS pattern
  ),
  mis_sedes AS (
    SELECT id FROM public.venues WHERE admin_id = auth.uid()
  ),
  arriendos AS (
    SELECT to_char(COALESCE(ps.processed_at, ps.created_at), (SELECT pattern FROM fmt)) AS periodo,
           SUM( (ps.cart_snapshot->>'amount')::numeric
                - COALESCE((ps.cart_snapshot->>'marketplaceFee')::numeric, 0) ) AS monto
    FROM public.payment_sessions ps
    WHERE ps.status = 'APPROVED'
      AND ps.cart_snapshot->>'type' = 'ROOM_RESERVATION'
      AND (ps.cart_snapshot->>'venueId')::uuid IN (SELECT id FROM mis_sedes)
    GROUP BY 1
  ),
  clases AS (
    SELECT to_char(COALESCE(c.start_time, c.created_at), (SELECT pattern FROM fmt)) AS periodo,
           SUM(
             (SELECT COUNT(*) FROM public.enrollments e
                WHERE e.class_id = c.id AND e.status = 'ACTIVE') * COALESCE(c.price, 0)
           ) AS ingreso,
           SUM(COALESCE(c.honorario, 0)) AS egreso
    FROM public.classes c
    JOIN public.rooms r ON r.id = c.room_id
    WHERE r.venue_id IN (SELECT id FROM mis_sedes)
      AND c.tipo_clase = 'ASIGNADA'
      AND c.status NOT IN ('DRAFT', 'CANCELLED')
    GROUP BY 1
  ),
  periodos AS (
    SELECT periodo FROM arriendos WHERE periodo IS NOT NULL
    UNION
    SELECT periodo FROM clases WHERE periodo IS NOT NULL
  )
  SELECT pr.periodo,
         COALESCE(a.monto, 0)    AS ingreso_arriendo,
         COALESCE(cl.ingreso, 0) AS ingreso_clases,
         COALESCE(cl.egreso, 0)  AS egreso_profes,
         COALESCE(a.monto, 0) + COALESCE(cl.ingreso, 0) - COALESCE(cl.egreso, 0) AS total
  FROM periodos pr
  LEFT JOIN arriendos a  ON a.periodo  = pr.periodo
  LEFT JOIN clases    cl ON cl.periodo = pr.periodo
  ORDER BY pr.periodo;
$$;
REVOKE ALL ON FUNCTION public.get_venue_metrics(text) FROM public;
REVOKE ALL ON FUNCTION public.get_venue_metrics(text) FROM anon;
GRANT EXECUTE ON FUNCTION public.get_venue_metrics(text) TO authenticated;

-- Cuánto se le debe a cada profesor dependiente (suma de honorarios de sus
-- clases ASIGNADA en las sedes del usuario). Para el desglose de "egreso profes".
CREATE OR REPLACE FUNCTION public.get_venue_teacher_payouts()
RETURNS TABLE (
  teacher_id      uuid,
  full_name       text,
  email           text,
  total_honorario numeric,
  clases          bigint
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  SELECT c.teacher_id,
         pr.full_name,
         u.email::text,
         SUM(COALESCE(c.honorario, 0)) AS total_honorario,
         COUNT(*) AS clases
  FROM public.classes c
  JOIN public.rooms  r ON r.id = c.room_id
  JOIN public.venues v ON v.id = r.venue_id
  LEFT JOIN public.profiles pr ON pr.id = c.teacher_id
  LEFT JOIN auth.users      u  ON u.id  = c.teacher_id
  WHERE v.admin_id = auth.uid()
    AND c.tipo_clase = 'ASIGNADA'
    AND c.status NOT IN ('DRAFT', 'CANCELLED')
    AND c.honorario IS NOT NULL
  GROUP BY c.teacher_id, pr.full_name, u.email
  ORDER BY SUM(COALESCE(c.honorario, 0)) DESC;
$$;
REVOKE ALL ON FUNCTION public.get_venue_teacher_payouts() FROM public;
REVOKE ALL ON FUNCTION public.get_venue_teacher_payouts() FROM anon;
GRANT EXECUTE ON FUNCTION public.get_venue_teacher_payouts() TO authenticated;
