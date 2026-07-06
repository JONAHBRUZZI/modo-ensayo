-- ============================================================
-- Fix del UNIQUE de enrollments para permitir varios beneficiarios por clase
-- ============================================================
-- El constraint original UNIQUE (class_id, student_id) impedía que un mismo
-- alumno inscribiera a más de un beneficiario (o a sí mismo + un asociado) en
-- la misma clase: la segunda inscripción fallaba.
--
-- La clave correcta es (class_id, beneficiary_type, COALESCE(beneficiary_id,
-- student_id)): un mismo beneficiario no se repite en la clase, pero sí puede
-- haber varios beneficiarios distintos del mismo student.
--
-- NOTA: PostgreSQL no admite expresiones (COALESCE) en un CONSTRAINT UNIQUE de
-- tabla; hay que usar CREATE UNIQUE INDEX sobre la expresión. Como student_id y
-- beneficiary_type son NOT NULL y student_id nunca es NULL, la expresión del
-- índice nunca es NULL (no hay problema de "NULLS DISTINCT").
-- ============================================================

-- 1. Quitar el constraint viejo (nombre autogenerado por Postgres).
ALTER TABLE public.enrollments
  DROP CONSTRAINT IF EXISTS enrollments_class_id_student_id_key;

-- 2. Dedupe defensivo: bajo el constraint viejo no deberían existir choques con
--    la clave nueva, pero se deja como red de seguridad idempotente.
DELETE FROM public.enrollments a
USING public.enrollments b
WHERE a.class_id = b.class_id
  AND a.beneficiary_type = b.beneficiary_type
  AND COALESCE(a.beneficiary_id, a.student_id) = COALESCE(b.beneficiary_id, b.student_id)
  AND a.ctid > b.ctid;

-- 3. Clave nueva por beneficiario.
CREATE UNIQUE INDEX IF NOT EXISTS enrollments_unique_beneficiary
  ON public.enrollments (class_id, beneficiary_type, COALESCE(beneficiary_id, student_id));
