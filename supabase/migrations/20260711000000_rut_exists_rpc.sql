-- ============================================================
-- Chequeo de RUT ya registrado (validación de identidad)
-- ------------------------------------------------------------
-- Al ingresar el RUT en la verificación de identidad, el frontend consulta si ese
-- RUT ya está en la plataforma para avisar "RUT existente...". Un usuario no puede
-- leer los RUT de otros (RLS), así que se expone un RPC SECURITY DEFINER que solo
-- devuelve booleano (existe / no existe), sin revelar de quién es.
--
-- Normaliza el RUT (quita puntos/guiones/espacios, mayúsculas) para comparar de
-- forma robusta. Excluye la propia cuenta (auth.uid()) para no marcar tu re-envío.
-- Considera "en uso" un RUT presente en identity_verifications (PENDING/APPROVED)
-- o en profiles.rut de otra cuenta.
-- ============================================================

CREATE OR REPLACE FUNCTION public.rut_ya_registrado(p_rut text)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  WITH norm AS (
    SELECT upper(regexp_replace(coalesce(p_rut, ''), '[^0-9kK]', '', 'g')) AS r
  )
  SELECT COALESCE((SELECT r <> '' FROM norm), false)
     AND EXISTS (
       SELECT 1 FROM public.identity_verifications iv, norm
       WHERE iv.user_id <> auth.uid()
         AND iv.status IN ('PENDING', 'APPROVED')
         AND upper(regexp_replace(coalesce(iv.document_number, ''), '[^0-9kK]', '', 'g')) = norm.r
       UNION ALL
       SELECT 1 FROM public.profiles p, norm
       WHERE p.id <> auth.uid()
         AND upper(regexp_replace(coalesce(p.rut, ''), '[^0-9kK]', '', 'g')) = norm.r
     );
$$;

REVOKE ALL ON FUNCTION public.rut_ya_registrado(text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.rut_ya_registrado(text) TO authenticated;
