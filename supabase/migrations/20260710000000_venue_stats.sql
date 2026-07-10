-- ============================================================
-- Estadísticas por datos para el dueño de sede (rol VENUE_ADMIN)
-- ------------------------------------------------------------
-- Cubre la necesidad del perfil "Administrador de Sede" del informe: además de
-- ver ingresos, poder tomar decisiones con datos de SU propia sede —
--   • Ocupación real por sala (y salas con menor uso)
--   • Disciplinas/clases con mayor demanda ("las más cotizadas")
--   • Franjas horarias con baja ocupación
--   • KPIs: total de clases, alumnos, ocupación promedio, ingresos
--
-- Todo scoped a las sedes del usuario (admin_id = auth.uid()); SECURITY DEFINER
-- para poder cruzar enrollments/rooms sin exponerlos por RLS. Devuelve un único
-- jsonb con las claves ya en camelCase (el frontend lo consume tal cual).
--
-- Ocupación = inscripciones ACTIVE ÷ capacidad de la sala, consistente con la
-- métrica M1 del panel del administrador general. Franja horaria calculada en
-- horario de Chile (America/Santiago), no en UTC.
-- ============================================================

CREATE OR REPLACE FUNCTION public.get_venue_stats()
RETURNS jsonb
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  WITH mis_sedes AS (
    SELECT id FROM public.venues WHERE admin_id = auth.uid()
  ),
  -- Una fila por clase ofertada (no borrador ni cancelada) en las salas del usuario.
  --   alumnos   = inscripciones ACTIVE          → numerador de OCUPACIÓN
  --   capacidad = capacidad de la SALA          → denominador de OCUPACIÓN
  --   presentes = asistentes marcados presentes → numerador de ASISTENCIA
  --   marcados  = alumnos con lista pasada      → denominador de ASISTENCIA
  base AS (
    SELECT c.id,
           c.discipline,
           c.price                                   AS precio,
           c.start_time,
           r.name                                    AS sala,
           COALESCE(r.capacity, 0)                   AS capacidad,
           (SELECT COUNT(*) FROM public.enrollments e
              WHERE e.class_id = c.id AND e.status = 'ACTIVE')    AS alumnos,
           (SELECT COUNT(*) FROM public.attendances a
              WHERE a.class_id = c.id AND a.present)              AS presentes,
           (SELECT COUNT(*) FROM public.attendances a
              WHERE a.class_id = c.id)                            AS marcados
    FROM public.classes c
    JOIN public.rooms r ON r.id = c.room_id
    WHERE r.venue_id IN (SELECT id FROM mis_sedes)
      AND c.status NOT IN ('DRAFT', 'CANCELLED')
  ),
  kpis AS (
    SELECT
      COUNT(*)                                        AS "totalClases",
      COALESCE(SUM(alumnos), 0)                       AS "totalAlumnos",
      CASE WHEN SUM(capacidad) > 0
           THEN ROUND(100.0 * SUM(alumnos) / SUM(capacidad))
           ELSE 0 END                                 AS "ocupacionPromedio",
      -- Asistencia = presentes ÷ inscritos con lista pasada. NULL si aún no se
      -- ha pasado lista en ninguna clase (el frontend muestra "—").
      CASE WHEN SUM(marcados) > 0
           THEN ROUND(100.0 * SUM(presentes) / SUM(marcados))
           ELSE NULL END                              AS "asistencia",
      COALESCE(SUM(alumnos * precio), 0)              AS "ingresos"
    FROM base
  ),
  salas AS (
    SELECT sala,
           COUNT(*)          AS clases,
           SUM(alumnos)      AS alumnos,
           CASE WHEN SUM(capacidad) > 0
                THEN ROUND(100.0 * SUM(alumnos) / SUM(capacidad)) ELSE 0 END AS ocupacion,
           CASE WHEN SUM(marcados) > 0
                THEN ROUND(100.0 * SUM(presentes) / SUM(marcados)) ELSE NULL END AS asistencia
    FROM base
    GROUP BY sala
  ),
  disciplinas AS (
    SELECT discipline    AS disciplina,
           COUNT(*)      AS clases,
           SUM(alumnos)  AS alumnos,
           CASE WHEN SUM(capacidad) > 0
                THEN ROUND(100.0 * SUM(alumnos) / SUM(capacidad)) ELSE 0 END AS ocupacion,
           CASE WHEN SUM(marcados) > 0
                THEN ROUND(100.0 * SUM(presentes) / SUM(marcados)) ELSE NULL END AS asistencia
    FROM base
    GROUP BY discipline
  ),
  horarios AS (
    SELECT franja,
           COUNT(*)      AS clases,
           SUM(alumnos)  AS alumnos,
           CASE WHEN SUM(capacidad) > 0
                THEN ROUND(100.0 * SUM(alumnos) / SUM(capacidad)) ELSE 0 END AS ocupacion,
           CASE WHEN SUM(marcados) > 0
                THEN ROUND(100.0 * SUM(presentes) / SUM(marcados)) ELSE NULL END AS asistencia
    FROM (
      SELECT alumnos, capacidad, presentes, marcados,
             CASE
               WHEN start_time IS NULL THEN 'Sin fecha'
               WHEN EXTRACT(HOUR FROM (start_time AT TIME ZONE 'America/Santiago')) < 12 THEN 'Mañana (06–12 h)'
               WHEN EXTRACT(HOUR FROM (start_time AT TIME ZONE 'America/Santiago')) < 18 THEN 'Tarde (12–18 h)'
               ELSE 'Noche (18–24 h)'
             END AS franja
      FROM base
    ) b
    GROUP BY franja
  )
  SELECT jsonb_build_object(
    'kpis',        (SELECT to_jsonb(k) FROM kpis k),
    'salas',       COALESCE((SELECT jsonb_agg(to_jsonb(s) ORDER BY s.ocupacion ASC)  FROM salas s), '[]'::jsonb),
    'disciplinas', COALESCE((SELECT jsonb_agg(to_jsonb(d) ORDER BY d.alumnos DESC)   FROM disciplinas d), '[]'::jsonb),
    'horarios',    COALESCE((SELECT jsonb_agg(to_jsonb(h) ORDER BY h.ocupacion ASC)  FROM horarios h), '[]'::jsonb)
  );
$$;

REVOKE ALL ON FUNCTION public.get_venue_stats() FROM public;
REVOKE ALL ON FUNCTION public.get_venue_stats() FROM anon;
GRANT EXECUTE ON FUNCTION public.get_venue_stats() TO authenticated;
