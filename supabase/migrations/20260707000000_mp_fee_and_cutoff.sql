-- ============================================================
-- Costo real de MercadoPago por transacción + día de corte mensual
-- ------------------------------------------------------------
-- payment_sessions guarda ahora el fee que MercadoPago cobró en cada pago
-- (mp_fee_amount) y el neto que efectivamente se recibió (net_received_amount).
-- Los llena mercadopago-webhook al procesar un pago aprobado; los pagos
-- históricos quedan NULL (MP no expone el fee retroactivo por esta vía).
--
-- app_settings.payout_cutoff_day: día del mes en que se cierra el ciclo de
-- giros a profesores (ej. cerrar el 24 y pagar antes del 26). El panel de
-- admin agrupa los payouts por ese ciclo. Editable sin redeploy.
-- ============================================================

ALTER TABLE public.payment_sessions
  ADD COLUMN IF NOT EXISTS mp_fee_amount numeric,
  ADD COLUMN IF NOT EXISTS net_received_amount numeric;

-- Día de corte del ciclo mensual de giros (número, jsonb). Default 24.
INSERT INTO public.app_settings (key, value)
VALUES ('payout_cutoff_day', '24'::jsonb)
ON CONFLICT (key) DO NOTHING;
