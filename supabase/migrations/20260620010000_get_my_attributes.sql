-- RPC: atributos del usuario autenticado (réplica de GET /users/me/atributos del backend Spring).
-- Centraliza la lógica de negocio R20 (estadoProfesor) y derivados en un solo round-trip.
-- SECURITY DEFINER + auth.uid(): cada usuario solo obtiene sus propios atributos.

CREATE OR REPLACE FUNCTION public.get_my_attributes()
RETURNS jsonb
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
  uid uuid := auth.uid();
  prof public.profiles%ROWTYPE;
  is_teacher boolean := public.has_role('TEACHER');
  is_venue_admin boolean := public.has_role('VENUE_ADMIN');
  sede_aprobada boolean;
  v_estado text;
  v_motivo text;
  tiene_reservas boolean;
  tiene_asignaciones boolean;
  reservas_sin_clase int;
  estado_profesor text;
  perfil_completo boolean;
BEGIN
  IF uid IS NULL THEN
    RETURN jsonb_build_object('error', 'no auth');
  END IF;

  SELECT * INTO prof FROM public.profiles WHERE id = uid;

  sede_aprobada := COALESCE(prof.tiene_sede_aprobada, false) OR is_venue_admin;

  -- Última solicitud de sede del usuario
  SELECT v.status::text, v.rejection_reason INTO v_estado, v_motivo
  FROM public.venues v
  WHERE v.admin_id = uid
  ORDER BY v.created_at DESC
  LIMIT 1;

  -- Actividad como profesor sobre sus clases
  SELECT
    bool_or(c.tipo_clase = 'PROPIA'   AND c.status <> 'DRAFT' AND c.end_time IS NOT NULL AND c.end_time > now()),
    bool_or(c.tipo_clase = 'ASIGNADA' AND c.status <> 'DRAFT' AND c.end_time IS NOT NULL AND c.end_time > now()),
    count(*) FILTER (WHERE c.tipo_clase = 'PROPIA' AND c.status = 'DRAFT' AND c.room_id IS NOT NULL)
  INTO tiene_reservas, tiene_asignaciones, reservas_sin_clase
  FROM public.classes c
  WHERE c.teacher_id = uid;

  tiene_reservas      := COALESCE(tiene_reservas, false);
  tiene_asignaciones  := COALESCE(tiene_asignaciones, false);
  reservas_sin_clase  := COALESCE(reservas_sin_clase, 0);

  -- Regla R20
  IF NOT is_teacher THEN
    estado_profesor := 'INACTIVO';
  ELSIF tiene_reservas OR tiene_asignaciones THEN
    estado_profesor := 'ACTIVO';
  ELSE
    estado_profesor := 'DORMIDO';
  END IF;

  -- Perfil profesional completo: biografía + (disciplina principal o especialidad)
  SELECT (p.biografia IS NOT NULL AND length(btrim(p.biografia)) > 0)
     AND ((p.disciplina_principal IS NOT NULL AND length(btrim(p.disciplina_principal)) > 0)
       OR (p.especialidad IS NOT NULL AND length(btrim(p.especialidad)) > 0))
  INTO perfil_completo
  FROM public.professional_profiles p
  WHERE p.id = uid;
  perfil_completo := COALESCE(perfil_completo, false);

  RETURN jsonb_build_object(
    'identidadValidada', COALESCE(prof.identidad_validada, false),
    'identidadEstado', COALESCE(prof.identidad_estado, 'SIN_VALIDAR'),
    'hasRoleTeacher', is_teacher,
    'tieneSedeAprobada', sede_aprobada,
    'estadoSolicitudSede', v_estado,
    'motivoRechazoSede', v_motivo,
    'tieneReservasActivas', tiene_reservas,
    'tieneAsignacionesActivas', tiene_asignaciones,
    'reservasSinClase', reservas_sin_clase > 0,
    'reservasSinClaseCount', reservas_sin_clase,
    'estadoProfesor', estado_profesor,
    'perfilProfesionalCompleto', perfil_completo
  );
END;
$$;

GRANT EXECUTE ON FUNCTION public.get_my_attributes() TO authenticated;
