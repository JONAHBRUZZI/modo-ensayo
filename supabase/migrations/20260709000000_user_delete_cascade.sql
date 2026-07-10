-- ============================================================
-- Eliminación de usuario en cascada
-- ------------------------------------------------------------
-- Al eliminar un usuario de auth.users (desde el panel de admin), sus datos deben
-- limpiarse automáticamente. Hasta ahora muchas FKs a auth.users no tenían acción
-- de borrado, por lo que el DELETE fallaba con violación de llave foránea.
--
-- Regla aplicada a cada FK que apunta a auth.users(id):
--   • Columna NOT NULL (propiedad del usuario: sus clases, inscripciones, pagos,
--     sedes, reseñas, asistencia...) → ON DELETE CASCADE (se borra con el usuario).
--   • Columna NULLABLE (quién revisó / quién cambió / actor de auditoría) →
--     ON DELETE SET NULL (se conserva el registro histórico sin el usuario).
--
-- Idempotente: si la FK ya tiene la acción deseada, se omite.
-- ============================================================

DO $$
DECLARE
  r record;
  col_name text;
  is_nullable boolean;
  new_action text;
BEGIN
  FOR r IN
    SELECT con.oid,
           con.conname,
           con.conrelid              AS relid,
           con.conrelid::regclass    AS tbl,
           con.conkey[1]             AS colnum,
           con.confdeltype
    FROM pg_constraint con
    WHERE con.contype = 'f'
      AND con.confrelid = 'auth.users'::regclass
      AND array_length(con.conkey, 1) = 1   -- sólo FKs de una columna (todas lo son)
  LOOP
    SELECT attname, NOT attnotnull
      INTO col_name, is_nullable
    FROM pg_attribute
    WHERE attrelid = r.relid AND attnum = r.colnum;

    new_action := CASE WHEN is_nullable THEN 'SET NULL' ELSE 'CASCADE' END;

    -- confdeltype: 'c' = CASCADE, 'n' = SET NULL, 'a'/'r' = NO ACTION/RESTRICT
    IF (r.confdeltype = 'c' AND new_action = 'CASCADE')
       OR (r.confdeltype = 'n' AND new_action = 'SET NULL') THEN
      CONTINUE;  -- ya está como se quiere
    END IF;

    EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', r.tbl, r.conname);
    EXECUTE format(
      'ALTER TABLE %s ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES auth.users(id) ON DELETE %s',
      r.tbl, r.conname, col_name, new_action
    );

    RAISE NOTICE 'FK % en % (%): ON DELETE %', r.conname, r.tbl, col_name, new_action;
  END LOOP;
END $$;
