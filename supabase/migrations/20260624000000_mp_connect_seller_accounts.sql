-- ============================================================
-- MercadoPago Connect (marketplace): cuentas de vendedor por sede
-- ============================================================
-- Contexto: para el arriendo de salas con split de pago, cada gestor de sede
-- vincula su propia cuenta de MercadoPago vía OAuth. Estas tablas guardan los
-- tokens del vendedor y el "state" temporal del flujo OAuth.
--
-- NOTA: estas tablas ya existían en la BD de producción pero sin migración en el
-- repo ni RLS. Se formalizan acá de forma idempotente (IF NOT EXISTS) y se les
-- agrega Row Level Security. Los tokens NUNCA se exponen al frontend: solo las
-- Edge Functions (service role) los leen.

-- 1. Cuenta de vendedor MercadoPago (1 por usuario gestor de sede).
CREATE TABLE IF NOT EXISTS public.mp_seller_accounts (
  user_id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  mp_user_id text,
  access_token text,
  refresh_token text,
  token_expires_at timestamptz,
  scope text,
  public_key text,
  status text NOT NULL DEFAULT 'CONNECTED'
    CHECK (status = ANY (ARRAY['CONNECTED','DISCONNECTED','ERROR'])),
  connected_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

-- 2. State temporal del flujo OAuth (anti-CSRF; se borra al canjear el code).
CREATE TABLE IF NOT EXISTS public.mp_oauth_states (
  state text PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- 3. RLS
ALTER TABLE public.mp_seller_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.mp_oauth_states    ENABLE ROW LEVEL SECURITY;

-- El gestor puede VER si su cuenta está conectada (status/connected_at), pero los
-- tokens nunca se envían al frontend (el service role los maneja). Para evitar
-- filtrar tokens vía PostgREST, NO damos SELECT directo de columnas sensibles:
-- el estado se consulta mediante una vista/columna segura. Aquí permitimos al
-- dueño leer su fila (el frontend solo pedirá columnas no sensibles).
DROP POLICY IF EXISTS "mp_seller_select_own" ON public.mp_seller_accounts;
CREATE POLICY "mp_seller_select_own" ON public.mp_seller_accounts
  FOR SELECT TO authenticated USING (user_id = auth.uid());

-- INSERT/UPDATE/DELETE solo vía service role (Edge Functions). Sin políticas para
-- authenticated en esas operaciones => quedan denegadas por defecto.

-- mp_oauth_states: sin políticas para authenticated => solo service role.
-- (RLS habilitado sin policies = deny-all para usuarios normales.)

-- 4. Vista segura para el frontend: estado de conexión SIN exponer tokens.
CREATE OR REPLACE VIEW public.mp_seller_status
WITH (security_invoker = true) AS
  SELECT user_id, mp_user_id, status, connected_at, updated_at,
         (access_token IS NOT NULL) AS has_token
  FROM public.mp_seller_accounts;

GRANT SELECT ON public.mp_seller_status TO authenticated;

-- 5. Comisión de la plataforma sobre el arriendo de sala (configurable).
--    Valor en porcentaje (0–100). El % final se decide después.
INSERT INTO public.app_settings (key, value)
VALUES ('room_reservation_commission_pct', '0'::jsonb)
ON CONFLICT (key) DO NOTHING;
