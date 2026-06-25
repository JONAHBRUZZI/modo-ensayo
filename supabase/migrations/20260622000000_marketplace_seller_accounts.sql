-- ============================================================
-- Marketplace MercadoPago — cuentas de vendedor (profesores)
-- Almacena las credenciales OAuth de la cuenta MercadoPago de cada profesor,
-- usadas para liquidarle su parte cuando una clase se confirma como realizada.
--
-- SEGURIDAD: esta tabla guarda tokens OAuth. RLS habilitado SIN políticas de
-- cliente → ningún rol anon/authenticated puede leer ni escribir. Solo el
-- service_role (Edge Functions privilegiadas) la maneja, bypassando RLS.
-- El frontend obtiene únicamente el estado de conexión vía la RPC
-- get_my_seller_status() (no expone tokens).
-- TODO(hardening): migrar access_token/refresh_token a Supabase Vault.
-- ============================================================

CREATE TABLE IF NOT EXISTS public.mp_seller_accounts (
  user_id          uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  mp_user_id       text,                       -- collector_id de MercadoPago
  access_token     text,
  refresh_token    text,
  token_expires_at timestamptz,
  scope            text,
  public_key       text,
  status           text NOT NULL DEFAULT 'CONNECTED'
                     CHECK (status IN ('CONNECTED', 'DISCONNECTED', 'ERROR')),
  connected_at     timestamptz NOT NULL DEFAULT now(),
  updated_at       timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE public.mp_seller_accounts ENABLE ROW LEVEL SECURITY;
-- Intencional: sin políticas. Solo service_role accede (bypassa RLS).

-- Mantener updated_at
DROP TRIGGER IF EXISTS set_updated_at_mp_seller_accounts ON public.mp_seller_accounts;
CREATE TRIGGER set_updated_at_mp_seller_accounts
  BEFORE UPDATE ON public.mp_seller_accounts
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- ------------------------------------------------------------
-- RPC: estado de conexión MP del usuario autenticado.
-- Expone SOLO si está conectado y su collector_id; nunca los tokens.
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.get_my_seller_status()
RETURNS jsonb
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  SELECT jsonb_build_object(
    'connected', COALESCE(a.status = 'CONNECTED', false),
    'status',    COALESCE(a.status, 'DISCONNECTED'),
    'mpUserId',  a.mp_user_id,
    'connectedAt', a.connected_at
  )
  FROM (SELECT auth.uid() AS uid) ctx
  LEFT JOIN public.mp_seller_accounts a ON a.user_id = ctx.uid;
$$;

REVOKE EXECUTE ON FUNCTION public.get_my_seller_status() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.get_my_seller_status() TO authenticated;
