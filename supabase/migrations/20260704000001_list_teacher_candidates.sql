-- ============================================================
-- Sugerencias de correo al agregar un profesor dependiente en la sede
-- ============================================================
-- Devuelve candidatos (usuarios con rol TEACHER o con perfil profesional creado)
-- con su email y nombre, para autocompletar el campo en /sede/profesores.
-- SECURITY DEFINER porque el email vive en auth.users (inaccesible al cliente).
-- Solo devuelve datos si el que llama administra alguna sede (evita exponer
-- correos a cualquier usuario).
-- ============================================================

CREATE OR REPLACE FUNCTION public.list_teacher_candidates()
RETURNS TABLE (email text, full_name text)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  SELECT DISTINCT u.email::text, pr.full_name
  FROM auth.users u
  LEFT JOIN public.profiles pr ON pr.id = u.id
  WHERE EXISTS (SELECT 1 FROM public.venues v WHERE v.admin_id = auth.uid())
    AND (
      (u.raw_app_meta_data -> 'roles') ? 'TEACHER'
      OR EXISTS (SELECT 1 FROM public.professional_profiles pp WHERE pp.id = u.id)
    )
  ORDER BY 1
  LIMIT 200;
$$;
REVOKE ALL ON FUNCTION public.list_teacher_candidates() FROM public;
REVOKE ALL ON FUNCTION public.list_teacher_candidates() FROM anon;
GRANT EXECUTE ON FUNCTION public.list_teacher_candidates() TO authenticated;
