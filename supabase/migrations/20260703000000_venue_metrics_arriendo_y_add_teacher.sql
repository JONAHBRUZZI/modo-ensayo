-- ============================================================
-- Ajustes aditivos sobre 20260628000000_sede_clases_ingresos
-- ============================================================
-- 1. get_venue_metrics: el ingreso por ARRIENDO se calcula desde payment_sessions
--    (fuente real del cobro), NO desde el precio de clases PROPIA sobre bloques
--    OCCUPIED (que mezclaba la plata de profes independientes con el arriendo).
-- 2. add_venue_teacher: alta de profe dependiente por email (resuelve el email en
--    auth.users, inaccesible desde el cliente).
-- ============================================================

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
  WITH fmt AS (
    SELECT CASE WHEN p_granularidad = 'year' THEN 'YYYY' ELSE 'YYYY-MM' END AS pattern
  ),
  mis_sedes AS (
    SELECT id FROM public.venues WHERE admin_id = auth.uid()
  ),
  -- Ingreso real por arriendo: reservas de sala aprobadas (amount − comisión).
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
  -- Ingreso por clases ASIGNADA: (alumnos activos × precio) − honorario del profe.
  clases AS (
    SELECT to_char(COALESCE(c.start_time, c.created_at), (SELECT pattern FROM fmt)) AS periodo,
           SUM(
             (SELECT COUNT(*) FROM public.enrollments e
                WHERE e.class_id = c.id AND e.status = 'ACTIVE') * COALESCE(c.price, 0)
             - COALESCE(c.honorario, 0)
           ) AS monto
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
         COALESCE(a.monto, 0)  AS ingreso_arriendo,
         COALESCE(cl.monto, 0) AS ingreso_clases,
         COALESCE(a.monto, 0) + COALESCE(cl.monto, 0) AS ingreso_total
  FROM periodos pr
  LEFT JOIN arriendos a  ON a.periodo  = pr.periodo
  LEFT JOIN clases    cl ON cl.periodo = pr.periodo
  ORDER BY pr.periodo;
$$;
REVOKE ALL ON FUNCTION public.get_venue_metrics(text) FROM public;
REVOKE ALL ON FUNCTION public.get_venue_metrics(text) FROM anon;
GRANT EXECUTE ON FUNCTION public.get_venue_metrics(text) TO authenticated;

-- Alta de profe dependiente por email. Valida que el caller administre la sede
-- y resuelve el email en auth.users (el cliente no puede leerla).
CREATE OR REPLACE FUNCTION public.add_venue_teacher(p_venue_id uuid, p_email text)
RETURNS text
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
  v_uid uuid;
BEGIN
  IF NOT public.is_venue_admin(p_venue_id) THEN
    RAISE EXCEPTION 'No administras esta sede' USING ERRCODE = '42501';
  END IF;
  SELECT id INTO v_uid FROM auth.users WHERE lower(email) = lower(trim(p_email)) LIMIT 1;
  IF v_uid IS NULL THEN
    RAISE EXCEPTION 'No existe un usuario con ese email' USING ERRCODE = 'P0002';
  END IF;
  INSERT INTO public.venue_teachers (venue_id, teacher_id)
  VALUES (p_venue_id, v_uid)
  ON CONFLICT (venue_id, teacher_id) DO NOTHING;
  RETURN 'ok';
END;
$$;
REVOKE ALL ON FUNCTION public.add_venue_teacher(uuid, text) FROM public;
REVOKE ALL ON FUNCTION public.add_venue_teacher(uuid, text) FROM anon;
GRANT EXECUTE ON FUNCTION public.add_venue_teacher(uuid, text) TO authenticated;
