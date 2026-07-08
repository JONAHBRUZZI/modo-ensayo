-- ============================================================
-- Nombre público del profesor para las vistas de clases del alumno
-- ============================================================
-- Los nombres viven en public.profiles, que tiene RLS profiles_select_own
-- (cada quien solo lee su propia fila). Un alumno navegando "Clases
-- Disponibles" necesita ver quién imparte la clase, pero no puede leer el
-- profile del profesor por PostgREST.
--
-- Este RPC expone SOLO id + full_name, y únicamente de usuarios que son
-- profesores de al menos una clase (no filtra cualquier perfil del sistema).
-- SECURITY DEFINER para saltar la RLS de profiles de forma acotada.
-- ============================================================

CREATE OR REPLACE FUNCTION public.get_teacher_names(p_ids uuid[])
RETURNS TABLE (id uuid, full_name text)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  SELECT pr.id, pr.full_name
  FROM public.profiles pr
  WHERE pr.id = ANY(p_ids)
    AND EXISTS (SELECT 1 FROM public.classes c WHERE c.teacher_id = pr.id);
$$;
REVOKE ALL ON FUNCTION public.get_teacher_names(uuid[]) FROM public;
GRANT EXECUTE ON FUNCTION public.get_teacher_names(uuid[]) TO anon, authenticated;
