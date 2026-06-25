-- ============================================================
-- Marketplace MercadoPago — estados OAuth anti-CSRF
-- Tabla temporal para el flujo OAuth de MercadoPago.
-- El Edge Function mp-oauth-start inserta un state UUID vinculado
-- al usuario; mp-oauth-callback lo valida, lo consume (DELETE)
-- y procede con el intercambio de código por tokens.
--
-- TTL implícito de 10 minutos (validado en código).
-- Un cron job podría limpiar filas antiguas; por ahora el callback
-- elimina el state en cuanto lo consume.
--
-- SEGURIDAD: RLS habilitado SIN políticas de cliente → solo
-- service_role (Edge Functions) puede leer/escribir.
-- ============================================================

CREATE TABLE IF NOT EXISTS public.mp_oauth_states (
  state      text        PRIMARY KEY,
  user_id    uuid        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE public.mp_oauth_states ENABLE ROW LEVEL SECURITY;
-- Intencional: sin políticas. Solo service_role accede (bypassa RLS).

-- Índice para limpieza por TTL (futuro cron job)
CREATE INDEX IF NOT EXISTS mp_oauth_states_created_at_idx
  ON public.mp_oauth_states (created_at);
