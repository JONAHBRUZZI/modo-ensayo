-- ============================================================
-- Cascada de borrado completa (usuario + árbol de contenido)
-- ------------------------------------------------------------
-- Borrar un admin de sede fallaba SIN mensaje claro: el borrado del usuario
-- encadena venues → rooms, pero varias FKs quedaban sin acción de borrado y
-- bloqueaban (RESTRICT/NO ACTION). Bloqueadores detectados:
--   • classes.room_id → rooms            (sin ON DELETE)
--   • reschedules.new_class_id → classes (sin ON DELETE)
--   • teacher_payouts.class_id → classes (sin ON DELETE)
--
-- Este script extiende el mismo criterio de 20260709000000_user_delete_cascade
-- a TODO el árbol de contenido (auth.users, venues, rooms, classes, enrollments,
-- payments): por cada FK que apunte a esas tablas,
--   • columna NOT NULL  → ON DELETE CASCADE (dato propio, se borra en cascada)
--   • columna NULLABLE   → ON DELETE SET NULL (se conserva la fila sin la referencia)
-- Idempotente: si la FK ya tiene la acción deseada, se omite. Solo cambia el
-- comportamiento de borrado; no toca datos.
-- ============================================================

DO $$
DECLARE
  r record;
  col_name text;
  is_nullable boolean;
  new_action text;
BEGIN
  FOR r IN
    SELECT con.conname,
           con.conrelid            AS relid,
           con.conrelid::regclass  AS tbl,
           con.confrelid::regclass AS parent,
           con.conkey[1]           AS colnum,
           con.confdeltype
    FROM pg_constraint con
    WHERE con.contype = 'f'
      AND con.connamespace = 'public'::regnamespace
      AND con.confrelid IN (
        'auth.users'::regclass,
        'public.venues'::regclass,
        'public.rooms'::regclass,
        'public.classes'::regclass,
        'public.enrollments'::regclass,
        'public.payments'::regclass
      )
      AND array_length(con.conkey, 1) = 1
  LOOP
    SELECT attname, NOT attnotnull
      INTO col_name, is_nullable
    FROM pg_attribute
    WHERE attrelid = r.relid AND attnum = r.colnum;

    new_action := CASE WHEN is_nullable THEN 'SET NULL' ELSE 'CASCADE' END;

    IF (r.confdeltype = 'c' AND new_action = 'CASCADE')
       OR (r.confdeltype = 'n' AND new_action = 'SET NULL') THEN
      CONTINUE;
    END IF;

    EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', r.tbl, r.conname);
    EXECUTE format(
      'ALTER TABLE %s ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES %s(id) ON DELETE %s',
      r.tbl, r.conname, col_name, r.parent, new_action
    );

    RAISE NOTICE 'FK % en % (%) → % ON DELETE %', r.conname, r.tbl, col_name, r.parent, new_action;
  END LOOP;
END $$;
