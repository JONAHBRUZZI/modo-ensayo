-- ============================================================
-- Validaciones de perfil a nivel de base de datos
-- ------------------------------------------------------------
-- El formato de RUT (dígito verificador) y de teléfono chileno solo se
-- validaban en el frontend (utils/rutValidator.js, utils/profileValidator.js).
-- Un insert/update directo vía API podía guardar un RUT o teléfono inválido;
-- refund_methods.rut en particular alimenta reembolsos bancarios reales.
--
-- rut_valido() replica el mismo algoritmo (módulo 11) y la misma
-- normalización que ya usa rut_ya_registrado (20260711000000), para no
-- introducir un segundo criterio de "RUT válido" en el sistema.
-- ============================================================

CREATE OR REPLACE FUNCTION public.rut_valido(p_rut text)
RETURNS boolean
LANGUAGE sql
IMMUTABLE
SET search_path = public, pg_temp
AS $$
  WITH norm AS (
    SELECT upper(regexp_replace(coalesce(p_rut, ''), '[^0-9kK]', '', 'g')) AS r
  ),
  partes AS (
    SELECT
      substring(r from 1 for length(r) - 1) AS cuerpo,
      right(r, 1) AS dv
    FROM norm
    WHERE length(r) >= 2
  ),
  calculo AS (
    SELECT
      cuerpo, dv,
      (
        SELECT sum(digito::int * multiplo)
        FROM (
          SELECT
            substring(cuerpo from gs for 1) AS digito,
            2 + ((length(cuerpo) - gs) % 6) AS multiplo
          FROM generate_series(1, length(cuerpo)) AS gs
        ) t
      ) AS suma
    FROM partes
  )
  SELECT CASE
    WHEN calculo.cuerpo IS NULL THEN false
    ELSE calculo.dv = (
      CASE (11 - (calculo.suma % 11))
        WHEN 11 THEN '0'
        WHEN 10 THEN 'K'
        ELSE (11 - (calculo.suma % 11))::text
      END
    )
  END
  FROM calculo;
$$;

-- Diagnóstico previo: loguea como WARNING cualquier fila existente que
-- violaría los constraints, para poder limpiarla a mano antes de que el
-- ALTER TABLE falle.
DO $$
DECLARE
  bad record;
BEGIN
  FOR bad IN
    SELECT 'profiles.rut' AS tabla_columna, id::text AS fila, rut AS valor
    FROM public.profiles WHERE rut IS NOT NULL AND NOT public.rut_valido(rut)
    UNION ALL
    SELECT 'refund_methods.rut', id::text, rut
    FROM public.refund_methods WHERE rut IS NOT NULL AND NOT public.rut_valido(rut)
    UNION ALL
    SELECT 'identity_verifications.document_number', id::text, document_number
    FROM public.identity_verifications
    WHERE document_number IS NOT NULL AND document_type = 'RUT' AND NOT public.rut_valido(document_number)
    UNION ALL
    SELECT 'profiles.phone', id::text, phone
    FROM public.profiles WHERE phone IS NOT NULL AND phone !~ '^(\+?56)?9\d{8}$'
  LOOP
    RAISE WARNING 'Valor inválido detectado en % (fila %): % — resolver antes de aplicar el constraint', bad.tabla_columna, bad.fila, bad.valor;
  END LOOP;
END $$;

ALTER TABLE public.profiles
  ADD CONSTRAINT chk_profiles_rut CHECK (rut IS NULL OR public.rut_valido(rut)),
  ADD CONSTRAINT chk_profiles_phone CHECK (phone IS NULL OR phone ~ '^(\+?56)?9\d{8}$');

ALTER TABLE public.refund_methods
  ADD CONSTRAINT chk_refund_methods_rut CHECK (rut IS NULL OR public.rut_valido(rut));

-- Solo exige formato de RUT cuando document_type = 'RUT' — el formulario
-- también acepta 'PASSPORT' (IdentityUploadPage.vue), que no sigue este formato.
ALTER TABLE public.identity_verifications
  ADD CONSTRAINT chk_identity_verifications_document_number
    CHECK (document_number IS NULL OR document_type <> 'RUT' OR public.rut_valido(document_number));
