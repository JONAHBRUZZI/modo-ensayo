-- ============================================================
-- Pulido de venue_teachers (detalles menores pospuestos)
-- ============================================================
-- 1. teacher_id con ON DELETE CASCADE: si se borra el usuario, se limpian sus
--    vinculaciones en vez de bloquear el borrado.
-- 2. vt_select_own: el profe dependiente puede ver sus propias vinculaciones
--    (antes solo las veía el admin de la sede o el ADMIN global).
-- ============================================================

ALTER TABLE public.venue_teachers
  DROP CONSTRAINT IF EXISTS venue_teachers_teacher_id_fkey;
ALTER TABLE public.venue_teachers
  ADD CONSTRAINT venue_teachers_teacher_id_fkey
  FOREIGN KEY (teacher_id) REFERENCES auth.users(id) ON DELETE CASCADE;

DROP POLICY IF EXISTS vt_select_own ON public.venue_teachers;
CREATE POLICY vt_select_own ON public.venue_teachers
  FOR SELECT TO authenticated
  USING (
    public.is_venue_admin(venue_id)
    OR public.has_role('ADMIN')
    OR teacher_id = auth.uid()
  );
