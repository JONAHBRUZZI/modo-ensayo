-- Table-dependent helper functions (must run AFTER tables are created,
-- because LANGUAGE sql validates the body at creation time).

-- Verificar si el usuario está enrolado en una clase
CREATE OR REPLACE FUNCTION public.is_enrolled(target_class_id uuid)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.enrollments
    WHERE class_id = target_class_id
      AND student_id = auth.uid()
      AND status = 'ACTIVE'
  );
$$;

-- Verificar si el usuario es admin de una sede
CREATE OR REPLACE FUNCTION public.is_venue_admin(target_venue_id uuid)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.venues
    WHERE id = target_venue_id AND admin_id = auth.uid()
  );
$$;

-- Verificar si el usuario es teacher de una clase
CREATE OR REPLACE FUNCTION public.is_class_teacher(target_class_id uuid)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.classes
    WHERE id = target_class_id AND teacher_id = auth.uid()
  );
$$;
