-- ============================================================
-- Datos de capacidad de la sede asociados al perfil de la sede
-- ============================================================
-- Contexto: el formulario de registro de sede captura la cantidad de salas y la
-- capacidad máxima de la sede. Estos datos ahora se guardan en el perfil de la
-- sede (tabla venues) en vez de descartarse.

ALTER TABLE public.venues
  ADD COLUMN IF NOT EXISTS capacidad_maxima int,
  ADD COLUMN IF NOT EXISTS cantidad_salas int;
