-- ============================================================
-- Guard: una clase no puede pasar a PUBLISHED si el profesor no tiene
-- MercadoPago conectado (al confirmarse la clase se le liquida el pago).
-- Trigger a nivel de BD: cubre tanto create-class (insert) como el publish de
-- borradores vía PostgREST (update), sin poder bypassearse desde el cliente.
-- ============================================================

CREATE OR REPLACE FUNCTION public.enforce_teacher_mp_connected()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
  IF NEW.status = 'PUBLISHED'
     AND (TG_OP = 'INSERT' OR OLD.status IS DISTINCT FROM 'PUBLISHED') THEN
    IF NOT EXISTS (
      SELECT 1 FROM public.mp_seller_accounts a
      WHERE a.user_id = NEW.teacher_id AND a.status = 'CONNECTED'
    ) THEN
      RAISE EXCEPTION 'TEACHER_MP_NOT_CONNECTED'
        USING HINT = 'El profesor debe conectar su cuenta de MercadoPago antes de publicar.';
    END IF;
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_enforce_mp_connected ON public.classes;
CREATE TRIGGER trg_enforce_mp_connected
  BEFORE INSERT OR UPDATE ON public.classes
  FOR EACH ROW EXECUTE FUNCTION public.enforce_teacher_mp_connected();
