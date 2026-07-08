-- ============================================================
-- Asistencia: clave única para permitir upsert idempotente
-- ------------------------------------------------------------
-- El flujo de "pasar lista" ahora marca a todos presentes por defecto y el
-- profesor solo desmarca ausentes, guardando toda la lista de una. Para que
-- re-guardar corrija en vez de duplicar filas, se necesita una clave única por
-- (clase, beneficiario). La Edge Function save-attendance hace el upsert con
-- service role (el profesor no tiene policy de UPDATE sobre attendances).
--
-- El marcado anterior nunca funcionó (enviaba beneficiary_id undefined → el
-- INSERT violaba NOT NULL), así que la tabla no debería tener choques; se deja
-- un dedupe defensivo idempotente por si acaso antes de crear el índice.
-- ============================================================

-- Dedupe defensivo: conserva la fila más reciente por (class_id, beneficiary_id).
DELETE FROM public.attendances a
USING public.attendances b
WHERE a.class_id = b.class_id
  AND a.beneficiary_id = b.beneficiary_id
  AND a.ctid < b.ctid;

CREATE UNIQUE INDEX IF NOT EXISTS attendances_class_beneficiary_key
  ON public.attendances (class_id, beneficiary_id);
