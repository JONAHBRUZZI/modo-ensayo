-- ============================================================
-- R05: unicidad de documento de identidad aprobado
-- ------------------------------------------------------------
-- Un mismo documento no puede quedar APPROVED en más de una cuenta. El aviso
-- de RUT duplicado (rut_ya_registrado, 20260711000000) solo bloquea en el
-- formulario del frontend antes de subir el documento; esto cierra el gap
-- con un constraint real en BD, que también cubre aprobaciones vía API
-- directa o dos revisiones simultáneas del mismo documento.
--
-- Normaliza igual que rut_ya_registrado (quita puntos/guiones/espacios,
-- mayúsculas) para no dejar pasar duplicados por formato distinto del mismo
-- documento.
-- ============================================================

-- Diagnóstico previo: si ya existen aprobaciones duplicadas en producción,
-- el CREATE UNIQUE INDEX de abajo va a fallar. Este bloque las loguea como
-- WARNING antes de intentarlo, para saber cuáles resolver manualmente
-- (rechazar/reabrir una de las dos) antes de reintentar la migración.
DO $$
DECLARE
  dup record;
BEGIN
  FOR dup IN
    SELECT upper(regexp_replace(document_number, '[^0-9kK]', '', 'g')) AS doc, count(*) AS n
    FROM public.identity_verifications
    WHERE status = 'APPROVED'
    GROUP BY 1
    HAVING count(*) > 1
  LOOP
    RAISE WARNING 'Documento aprobado duplicado detectado: % (% cuentas) — resolver antes de aplicar el índice único', dup.doc, dup.n;
  END LOOP;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS identity_verifications_document_approved_unique
  ON public.identity_verifications (
    (upper(regexp_replace(document_number, '[^0-9kK]', '', 'g')))
  )
  WHERE status = 'APPROVED';
