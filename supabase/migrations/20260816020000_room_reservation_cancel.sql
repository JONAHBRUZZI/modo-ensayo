-- ============================================================
-- R19: cancelación y reembolso total de un arriendo de sala
-- ------------------------------------------------------------
-- El profesor o la sede pueden cancelar un arriendo pagado hasta 24h antes
-- del horario reservado, con reembolso total (ver 02-Reglas-de-Negocio.md
-- R19). Requiere:
--
-- 1. Un nuevo estado 'REFUNDED' en payment_session_status (hoy solo
--    PENDING/APPROVED/FAILED — las reservas de sala no pasan por `payments`,
--    así que el estado de reembolso vive en payment_sessions).
-- 2. classes.payment_session_id: permite ubicar el pago de una reserva de
--    sala en O(1) al cancelar. cart_snapshot no siempre tiene el classId
--    (solo en el caso de reagendamiento vía borradorId), así que sin esta
--    columna habría que reconstruir la relación buscando en jsonb.
-- ============================================================

ALTER TYPE public.payment_session_status ADD VALUE IF NOT EXISTS 'REFUNDED';

ALTER TABLE public.classes
  ADD COLUMN IF NOT EXISTS payment_session_id uuid REFERENCES public.payment_sessions(id);
