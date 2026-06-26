-- ============================================================
-- SEED: discipline_catalog (reference data)
-- ============================================================
INSERT INTO public.discipline_catalog (name, category, active, sort_order) VALUES
  ('Folclore', 'Danza', true, 1),
  ('Contemporaneo', 'Danza', true, 2),
  ('Ballet', 'Danza', true, 3),
  ('Urbano', 'Danza', true, 4),
  ('Folclore', 'Musica', true, 5),
  ('Instrumental', 'Musica', true, 6),
  ('Moderno', 'Musica', true, 7),
  ('Guitarra', 'Musica', true, 8),
  ('Bateria', 'Musica', true, 9),
  ('Bajo', 'Musica', true, 10),
  ('Canto', 'Musica', true, 11),
  ('Piano', 'Musica', true, 12),
  ('Violin', 'Musica', true, 13),
  ('Saxofon', 'Musica', true, 14),
  ('Clasico', 'Teatro', true, 15),
  ('Contemporaneo', 'Teatro', true, 16),
  ('Musical', 'Teatro', true, 17),
  ('Improvisacion', 'Teatro', true, 18),
  ('Dramaturgia', 'Teatro', true, 19)
ON CONFLICT (name, category) DO NOTHING;
