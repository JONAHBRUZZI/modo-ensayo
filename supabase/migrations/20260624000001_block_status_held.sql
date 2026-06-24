-- ============================================================
-- Estado HELD para bloques de horario (reserva temporal durante el pago)
-- ============================================================
-- Cuando un profesor inicia el pago del arriendo de una sala, sus bloques se
-- marcan como HELD (reservados temporalmente) para que nadie más los tome
-- mientras paga en MercadoPago. Si paga, pasan a OCCUPIED; si no, un cron los
-- devuelve a AVAILABLE.
--
-- IMPORTANTE: 'ALTER TYPE ... ADD VALUE' debe ir SOLO en su propia migración/
-- transacción. NO referenciar 'HELD' en esta misma migración (las columnas y el
-- cron que lo usan van en la migración siguiente, 20260624000002).

ALTER TYPE public.block_status ADD VALUE IF NOT EXISTS 'HELD';
