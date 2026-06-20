-- Paridad con backend: Class.discipline / disciplineCategory son nullable
-- (sin @Column(nullable=false)). El flujo "reservar sala" crea drafts con
-- discipline = null, por lo que el NOT NULL original rompía ese caso.
ALTER TABLE public.classes ALTER COLUMN discipline DROP NOT NULL;
ALTER TABLE public.classes ALTER COLUMN discipline_category DROP NOT NULL;
