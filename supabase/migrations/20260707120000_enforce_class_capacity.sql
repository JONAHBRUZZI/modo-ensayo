-- ============================================================
-- Cupo de clase a prueba de concurrencia (control a nivel de BD)
-- ============================================================
-- El webhook contaba inscripciones y luego insertaba, en dos pasos: dos pagos
-- concurrentes a la misma clase podían leer el mismo count y ambos insertar,
-- sobrepasando el cupo por 1 (over-selling).
--
-- Este trigger cierra la carrera dentro de la base de datos: antes de insertar
-- una inscripción ACTIVE, bloquea la fila de la clase (SELECT ... FOR UPDATE)
-- y recién ahí cuenta. Dos transacciones concurrentes a la misma clase se
-- serializan sobre ese lock, así que la segunda ve el conteo real y se rechaza
-- si el cupo ya está lleno.
--
-- Se usa count(*) en vivo (no una columna denormalizada) para no depender de
-- que cada flujo de cancelación mantenga un contador: no hay drift posible.
-- SECURITY DEFINER para que el count() no quede acotado por la RLS del que inserta.
-- ============================================================

CREATE OR REPLACE FUNCTION public.enforce_class_capacity()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
  v_cap int;
  v_cnt int;
BEGIN
  -- Solo limitan las inscripciones activas; otras (CANCELLED, etc.) no ocupan cupo.
  IF NEW.status IS DISTINCT FROM 'ACTIVE' THEN
    RETURN NEW;
  END IF;

  -- Lock de la fila de la clase: serializa inscripciones concurrentes a la misma clase.
  SELECT capacity INTO v_cap FROM public.classes WHERE id = NEW.class_id FOR UPDATE;

  -- Clase inexistente o sin cupo definido: no se limita acá.
  IF v_cap IS NULL THEN
    RETURN NEW;
  END IF;

  SELECT count(*) INTO v_cnt
  FROM public.enrollments
  WHERE class_id = NEW.class_id AND status = 'ACTIVE';

  IF v_cnt >= v_cap THEN
    RAISE EXCEPTION 'CLASS_FULL: cupo % alcanzado para la clase %', v_cap, NEW.class_id
      USING ERRCODE = 'check_violation';
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_enforce_class_capacity ON public.enrollments;
CREATE TRIGGER trg_enforce_class_capacity
  BEFORE INSERT ON public.enrollments
  FOR EACH ROW EXECUTE FUNCTION public.enforce_class_capacity();
