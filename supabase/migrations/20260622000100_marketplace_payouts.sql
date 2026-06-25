-- ============================================================
-- Marketplace MercadoPago — liquidaciones a profesores y settings
-- teacher_payouts: una liquidación por pago liberado (RETAINED→RELEASED).
-- La crea confirm-class (PENDING) y la ejecuta process-payouts (→PAID).
-- ============================================================

CREATE TABLE IF NOT EXISTS public.teacher_payouts (
  id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  payment_id        uuid NOT NULL UNIQUE REFERENCES public.payments(id) ON DELETE CASCADE,
  teacher_id        uuid NOT NULL REFERENCES auth.users(id),
  class_id          uuid REFERENCES public.classes(id),
  gross_amount      numeric NOT NULL,
  commission_amount numeric NOT NULL DEFAULT 0,
  net_amount        numeric NOT NULL,
  mp_reference      text,
  status            text NOT NULL DEFAULT 'PENDING'
                      CHECK (status IN ('PENDING', 'PAID', 'FAILED')),
  error_detail      text,
  created_at        timestamptz NOT NULL DEFAULT now(),
  paid_at           timestamptz
);
-- UNIQUE(payment_id) garantiza idempotencia: un pago no genera dos payouts.

CREATE INDEX IF NOT EXISTS idx_teacher_payouts_status ON public.teacher_payouts(status);
CREATE INDEX IF NOT EXISTS idx_teacher_payouts_teacher ON public.teacher_payouts(teacher_id);

ALTER TABLE public.teacher_payouts ENABLE ROW LEVEL SECURITY;

-- El profesor ve sus propias liquidaciones; el admin todas. Sin escrituras de
-- cliente (solo service_role inserta/actualiza, igual que payments/enrollments).
DROP POLICY IF EXISTS payout_select_own ON public.teacher_payouts;
CREATE POLICY payout_select_own ON public.teacher_payouts
  FOR SELECT TO authenticated
  USING (teacher_id = auth.uid());

DROP POLICY IF EXISTS payout_select_admin ON public.teacher_payouts;
CREATE POLICY payout_select_admin ON public.teacher_payouts
  FOR SELECT TO authenticated
  USING (public.has_role('ADMIN'));

-- ============================================================
-- app_settings: configuración de negocio editable sin redeploy.
-- Solo service_role la lee/escribe (Edge Functions). Sin políticas de cliente.
-- ============================================================
CREATE TABLE IF NOT EXISTS public.app_settings (
  key        text PRIMARY KEY,
  value      jsonb NOT NULL,
  updated_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE public.app_settings ENABLE ROW LEVEL SECURITY;
-- Intencional: sin políticas. Solo service_role accede.

-- Comisión de la plataforma (porcentaje sobre el monto bruto de la clase).
INSERT INTO public.app_settings (key, value)
VALUES ('marketplace_commission_pct', '10'::jsonb)
ON CONFLICT (key) DO NOTHING;
