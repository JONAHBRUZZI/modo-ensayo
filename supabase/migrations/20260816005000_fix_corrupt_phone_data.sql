-- ============================================================
-- Limpieza de dato corrupto en profiles.phone
-- ------------------------------------------------------------
-- Detectado en la auditoría del 16-ago al intentar aplicar
-- chk_profiles_phone (20260816010000_profile_validation_constraints.sql):
-- dos filas tenían un nombre de persona guardado en `phone` en vez de un
-- número de teléfono ("Rosa Farias", "Victor Silva Farias") — data corrupta
-- de un bug anterior, no recuperable. Se limpia a NULL; el usuario puede
-- volver a cargar su teléfono real desde su perfil.
-- ============================================================

UPDATE public.profiles
SET phone = NULL
WHERE phone IS NOT NULL
  AND phone !~ '^(\+?56)?9\d{8}$';
