-- ============================================================
-- Módulo de pago agnóstico (paso 1): desembolso a profesores
-- ------------------------------------------------------------
-- MercadoPago no tiene API de money-out (su propia documentación dice que
-- su API es solo para vender productos/servicios, no para transferencias
-- entre cuentas) — por eso process-payouts quedó en "Fase 0". El desembolso
-- real se hará vía Fintoc (open banking chileno, transferencia directa a
-- datos bancarios, sin necesitar que el profesor "conecte" nada).
--
-- Esto reemplaza el requisito de publicación de "MercadoPago conectado" por
-- "tiene datos bancarios cargados" (refund_methods, mismo formulario que ya
-- usan los alumnos para reembolsos) — es lo que realmente se va a usar para
-- pagarle. La integración real con la API de Fintoc queda para cuando haya
-- credenciales (ver Documentación/15-Roadmap-y-Pendientes.md).
-- ============================================================

CREATE OR REPLACE FUNCTION public.enforce_teacher_payout_method()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
  IF NEW.status = 'PUBLISHED'
     AND (TG_OP = 'INSERT' OR OLD.status IS DISTINCT FROM 'PUBLISHED') THEN
    IF NOT EXISTS (
      SELECT 1 FROM public.refund_methods rm
      WHERE rm.user_id = NEW.teacher_id
    ) THEN
      RAISE EXCEPTION 'TEACHER_PAYOUT_METHOD_MISSING'
        USING HINT = 'El profesor debe cargar sus datos bancarios antes de publicar.';
    END IF;
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_enforce_mp_connected ON public.classes;
DROP FUNCTION IF EXISTS public.enforce_teacher_mp_connected();

CREATE TRIGGER trg_enforce_payout_method
  BEFORE INSERT OR UPDATE ON public.classes
  FOR EACH ROW EXECUTE FUNCTION public.enforce_teacher_payout_method();

-- Distingue payouts históricos (manuales, vía admin-payments) de futuros
-- payouts automáticos por proveedor. mp_reference se mantiene sin cambios
-- de nombre (lo usa admin-payments) aunque deje de ser específico de MP.
ALTER TABLE public.teacher_payouts
  ADD COLUMN IF NOT EXISTS provider text NOT NULL DEFAULT 'MANUAL';
