-- 30 profesores + perfiles + clases (independientes, dependientes y mixtos)
-- Roles: 1=USER, 2=TEACHER, 3=VENUE_ADMIN, 4=ADMIN

-- ========================================
-- 1. CREAR 30 USUARIOS CON ROL TEACHER
-- ========================================
INSERT INTO users (id, created_at, email, enabled, full_name, identidad_estado, identidad_validada, password_hash, phone, rut, tiene_sede_aprobada)
VALUES
-- Independientes (1-10): crean sus propias clases
(gen_random_uuid(),NOW(),'prof01_ind@test.cl',true,'María González Sepúlveda','APROBADO',true,'$2a$10$dummy','+56911110001','11111111-1',false),
(gen_random_uuid(),NOW(),'prof02_ind@test.cl',true,'Carlos Muñoz Tapia','APROBADO',true,'$2a$10$dummy','+56911110002','22222222-2',false),
(gen_random_uuid(),NOW(),'prof03_ind@test.cl',true,'Ana Valdés Contreras','APROBADO',true,'$2a$10$dummy','+56911110003','33333333-3',false),
(gen_random_uuid(),NOW(),'prof04_ind@test.cl',true,'Pedro Rojas Fuentes','APROBADO',true,'$2a$10$dummy','+56911110004','44444444-4',false),
(gen_random_uuid(),NOW(),'prof05_ind@test.cl',true,'Carmen Silva Morales','APROBADO',true,'$2a$10$dummy','+56911110005','55555555-5',false),
(gen_random_uuid(),NOW(),'prof06_ind@test.cl',true,'Juan Herrera Lagos','APROBADO',true,'$2a$10$dummy','+56911110006','66666666-6',false),
(gen_random_uuid(),NOW(),'prof07_ind@test.cl',true,'Sofía Paredes Ortiz','APROBADO',true,'$2a$10$dummy','+56911110007','77777777-7',false),
(gen_random_uuid(),NOW(),'prof08_ind@test.cl',true,'Diego Castro Vera','APROBADO',true,'$2a$10$dummy','+56911110008','88888888-8',false),
(gen_random_uuid(),NOW(),'prof09_ind@test.cl',true,'Laura Araya Rivas','APROBADO',true,'$2a$10$dummy','+56911110009','99999999-9',false),
(gen_random_uuid(),NOW(),'prof10_ind@test.cl',true,'Francisco Medina Jara','APROBADO',true,'$2a$10$dummy','+56911110010','10101010-0',false),
-- Dependientes (11-20): asignados por sedes
(gen_random_uuid(),NOW(),'prof11_dep@test.cl',true,'Valentina Campos Ríos','APROBADO',true,'$2a$10$dummy','+56911110011','11111111-1',false),
(gen_random_uuid(),NOW(),'prof12_dep@test.cl',true,'Andrés Figueroa Pinto','APROBADO',true,'$2a$10$dummy','+56911110012','22222222-2',false),
(gen_random_uuid(),NOW(),'prof13_dep@test.cl',true,'Catalina Mora Sáez','APROBADO',true,'$2a$10$dummy','+56911110013','33333333-3',false),
(gen_random_uuid(),NOW(),'prof14_dep@test.cl',true,'Rodrigo Peña Alarcón','APROBADO',true,'$2a$10$dummy','+56911110014','44444444-4',false),
(gen_random_uuid(),NOW(),'prof15_dep@test.cl',true,'Isabel Venegas Díaz','APROBADO',true,'$2a$10$dummy','+56911110015','55555555-5',false),
(gen_random_uuid(),NOW(),'prof16_dep@test.cl',true,'Tomás Fuentes Leiva','APROBADO',true,'$2a$10$dummy','+56911110016','66666666-6',false),
(gen_random_uuid(),NOW(),'prof17_dep@test.cl',true,'Gabriela Cáceres Núñez','APROBADO',true,'$2a$10$dummy','+56911110017','77777777-7',false),
(gen_random_uuid(),NOW(),'prof18_dep@test.cl',true,'Matías Espinoza Reyes','APROBADO',true,'$2a$10$dummy','+56911110018','88888888-8',false),
(gen_random_uuid(),NOW(),'prof19_dep@test.cl',true,'Daniela Guerrero Ponce','APROBADO',true,'$2a$10$dummy','+56911110019','99999999-9',false),
(gen_random_uuid(),NOW(),'prof20_dep@test.cl',true,'Felipe Ortega Vidal','APROBADO',true,'$2a$10$dummy','+56911110020','10101010-0',false),
-- Mixtos (21-30): ambos tipos
(gen_random_uuid(),NOW(),'prof21_mix@test.cl',true,'Constanza Farías León','APROBADO',true,'$2a$10$dummy','+56911110021','11111111-1',false),
(gen_random_uuid(),NOW(),'prof22_mix@test.cl',true,'Nicolás Sanhueza Molina','APROBADO',true,'$2a$10$dummy','+56911110022','22222222-2',false),
(gen_random_uuid(),NOW(),'prof23_mix@test.cl',true,'Javiera Tapia Carrasco','APROBADO',true,'$2a$10$dummy','+56911110023','33333333-3',false),
(gen_random_uuid(),NOW(),'prof24_mix@test.cl',true,'Cristóbal Rubio Salinas','APROBADO',true,'$2a$10$dummy','+56911110024','44444444-4',false),
(gen_random_uuid(),NOW(),'prof25_mix@test.cl',true,'Fernanda Mardones Avendaño','APROBADO',true,'$2a$10$dummy','+56911110025','55555555-5',false),
(gen_random_uuid(),NOW(),'prof26_mix@test.cl',true,'Sebastián Zambrano Parra','APROBADO',true,'$2a$10$dummy','+56911110026','66666666-6',false),
(gen_random_uuid(),NOW(),'prof27_mix@test.cl',true,'Paz Villalobos Cofré','APROBADO',true,'$2a$10$dummy','+56911110027','77777777-7',false),
(gen_random_uuid(),NOW(),'prof28_mix@test.cl',true,'Joaquín Saavedra Briones','APROBADO',true,'$2a$10$dummy','+56911110028','88888888-8',false),
(gen_random_uuid(),NOW(),'prof29_mix@test.cl',true,'Florencia Manríquez Aravena','APROBADO',true,'$2a$10$dummy','+56911110029','99999999-9',false),
(gen_random_uuid(),NOW(),'prof30_mix@test.cl',true,'Emilio Retamal Bustos','APROBADO',true,'$2a$10$dummy','+56911110030','10101010-0',false);

-- Asignar rol TEACHER (id=2) a los 30 profesores
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, 2 FROM users u
WHERE u.email LIKE 'prof%_@test.cl' AND NOT EXISTS (
  SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = 2
);

-- ========================================
-- 2. PERFILES PROFESIONALES
-- ========================================
INSERT INTO professional_profiles (id, user_id, description, especialidad, nivel_ensenanza, formacion, experience_years, biografia, disciplina_principal, disciplinas_secundarias, tipo_formacion, detalle_formacion, average_rating, instagram, sitio_web)
SELECT gen_random_uuid(), u.id,
  'Profesor de ' || CASE WHEN random() < 0.5 THEN 'música' ELSE 'danza' END || ' con amplia trayectoria.',
  CASE WHEN random() < 0.3 THEN 'Música' WHEN random() < 0.6 THEN 'Danza' ELSE 'Teatro' END,
  CASE WHEN random() < 0.5 THEN 'INTERMEDIO' ELSE 'AVANZADO' END,
  CASE WHEN random() < 0.5 THEN 'Universitaria' ELSE 'Autodidacta' END,
  floor(random() * 20 + 3)::int,
  'Profesor apasionado con ' || floor(random() * 15 + 5)::int || ' años de experiencia en enseñanza de ' ||
  CASE WHEN random() < 0.3 THEN 'guitarra y canto' WHEN random() < 0.6 THEN 'danza contemporánea y ballet' ELSE 'teatro y expresión corporal' END || '.',
  (ARRAY['GUITARRA','BATERIA','BAJO','CANTO','PIANO','DANZA','TEATRO'])[floor(random()*7+1)],
  (ARRAY['CANTO','PIANO','GUITARRA'])[floor(random()*3+1)],
  CASE WHEN random() < 0.5 THEN 'Grado académico' ELSE 'Certificación profesional' END,
  'Formación en ' || (ARRAY['Universidad de Chile','Pontificia Universidad Católica','Universidad Mayor','Escuela Moderna de Música','Academia de Humanidades'])[floor(random()*5+1)],
  floor(random() * 3 + 3)::int + random()::numeric(3,1),
  '@prof_' || replace(u.email,'@test.cl',''),
  NULL
FROM users u
WHERE u.email LIKE 'prof%_@test.cl' AND NOT EXISTS (
  SELECT 1 FROM professional_profiles pp WHERE pp.user_id = u.id
);

-- ========================================
-- 3. SALAS PARA LAS SEDES APROBADAS
-- ========================================
INSERT INTO rooms (id, created_at, name, capacity, activa, type, price_per_hour, tamanom2, tipo_piso, floor_type,
  has_mirrors, tiene_barra_ballet, tiene_aire_acondicionado, tiene_calefaccion, tiene_insonorizacion,
  has_sound, tiene_amplificacion, tiene_entrada_auxiliar, tiene_microfono, tiene_equipo_grabacion,
  tiene_piano, tiene_guitarra, tiene_bateria, venue_id)
SELECT gen_random_uuid(), NOW(),
  'Sala ' || (gs % 3 + 1)::text || ' - ' || v.name,
  CASE WHEN random() < 0.5 THEN 15 ELSE 25 END, true,
  CASE WHEN random() < 0.4 THEN 'GRUPAL' WHEN random() < 0.4 THEN 'INDIVIDUAL' ELSE 'BANDA' END,
  CASE WHEN random() < 0.5 THEN 15000 ELSE 25000 END, floor(random()*40+20)::int,
  (ARRAY['MADERA','FLOTANTE','CEMENTO','ALFOMBRA'])[floor(random()*4+1)],
  NULL, random() < 0.5, random() < 0.3, random() < 0.7, random() < 0.4, random() < 0.6,
  random() < 0.6, random() < 0.5, random() < 0.5, random() < 0.4, random() < 0.3,
  random() < 0.5, random() < 0.4, random() < 0.5, v.id
FROM (SELECT id, name FROM venues WHERE status = 'APROBADA' LIMIT 15) v,
     generate_series(1,2) as gs;

-- ========================================
-- 4. CLASES — Independientes (PROPIA, 2-3 por profesor)
-- ========================================
DO $$
DECLARE
  prof RECORD;
  room RECORD;
  i INT;
  disciplinas TEXT[] := ARRAY['GUITARRA','BATERIA','BAJO','CANTO','PIANO','DANZA','TEATRO','CUECA','BALLET','VIOLIN','SAXOFON','OTRO'];
  niveles TEXT[] := ARRAY['BASICO','INTERMEDIO','AVANZADO'];
  titulos TEXT[] := ARRAY['Clase intensiva','Taller práctico','Workshop','Sesión grupal','Clase magistral','Curso express','Seminario'];
BEGIN
  FOR prof IN SELECT id, full_name FROM users WHERE email LIKE 'prof%_ind@test.cl' ORDER BY email
  LOOP
    FOR i IN 1..(floor(random()*2+2))::int LOOP
      SELECT id INTO room FROM rooms ORDER BY random() LIMIT 1;
      INSERT INTO classes (id, created_at, title, description, discipline, level, capacity, duration, min_age, max_age, price, start_time, end_time, status, tipo_clase, teacher_id, room_id)
      VALUES (
        gen_random_uuid(), NOW() - (random()*30||' days')::interval,
        titulos[floor(random()*7+1)] || ' de ' || disciplinas[floor(random()*12+1)],
        'Clase de ' || prof.full_name || ' — ' || niveles[floor(random()*3+1)],
        disciplinas[floor(random()*12+1)],
        niveles[floor(random()*3+1)],
        floor(random()*15+5)::int,
        floor(random()*2+1)*30+30,
        8, 99,
        floor(random()*10000+5000)::int,
        NOW() + (random()*14||' days')::interval,
        NOW() + (random()*14+2||' days')::interval,
        CASE WHEN random() < 0.7 THEN 'PUBLISHED' ELSE 'DRAFT' END,
        'PROPIA', prof.id, room
      );
    END LOOP;
  END LOOP;
END $$;

-- ========================================
-- 5. CLASES — Dependientes (ASIGNADA, 2-3 por profesor)
-- ========================================
DO $$
DECLARE
  prof RECORD;
  room RECORD;
  i INT;
  disciplinas TEXT[] := ARRAY['GUITARRA','BATERIA','BAJO','CANTO','PIANO','DANZA','TEATRO','CUECA','BALLET','VIOLIN','SAXOFON','OTRO'];
  niveles TEXT[] := ARRAY['BASICO','INTERMEDIO','AVANZADO'];
  titulos TEXT[] := ARRAY['Clase sede','Programa regular','Curso institucional','Taller abierto','Clase demostrativa','Curso anual','Sesión libre'];
BEGIN
  FOR prof IN SELECT id, full_name FROM users WHERE email LIKE 'prof%_dep@test.cl' ORDER BY email
  LOOP
    FOR i IN 1..(floor(random()*2+2))::int LOOP
      SELECT id INTO room FROM rooms ORDER BY random() LIMIT 1;
      INSERT INTO classes (id, created_at, title, description, discipline, level, capacity, duration, min_age, max_age, price, start_time, end_time, status, tipo_clase, teacher_id, room_id)
      VALUES (
        gen_random_uuid(), NOW() - (random()*30||' days')::interval,
        titulos[floor(random()*7+1)] || ' de ' || disciplinas[floor(random()*12+1)],
        'Clase de sede con ' || prof.full_name || ' — ' || niveles[floor(random()*3+1)],
        disciplinas[floor(random()*12+1)],
        niveles[floor(random()*3+1)],
        floor(random()*15+5)::int,
        floor(random()*2+1)*30+30,
        10, 90,
        floor(random()*8000+5000)::int,
        NOW() + (random()*14||' days')::interval,
        NOW() + (random()*14+2||' days')::interval,
        CASE WHEN random() < 0.6 THEN 'PUBLISHED' WHEN random() < 0.9 THEN 'COMPLETED' ELSE 'DRAFT' END,
        'ASIGNADA', prof.id, room
      );
    END LOOP;
  END LOOP;
END $$;

-- ========================================
-- 6. CLASES — Mixtos (2 PROPIA + 2 ASIGNADA por profesor)
-- ========================================
DO $$
DECLARE
  prof RECORD;
  room RECORD;
  i INT;
  disciplinas TEXT[] := ARRAY['GUITARRA','BATERIA','BAJO','CANTO','PIANO','DANZA','TEATRO','CUECA','BALLET','VIOLIN','SAXOFON','OTRO'];
  niveles TEXT[] := ARRAY['BASICO','INTERMEDIO','AVANZADO'];
  titulos TEXT[] := ARRAY['Clase libre','Sesión personalizada','Clase de especialidad','Workshop avanzado','Curso intensivo','Taller de perfeccionamiento','Masterclass'];
BEGIN
  FOR prof IN SELECT id, full_name FROM users WHERE email LIKE 'prof%_mix@test.cl' ORDER BY email
  LOOP
    -- PROPIA
    FOR i IN 1..2 LOOP
      SELECT id INTO room FROM rooms ORDER BY random() LIMIT 1;
      INSERT INTO classes (id, created_at, title, description, discipline, level, capacity, duration, min_age, max_age, price, start_time, end_time, status, tipo_clase, teacher_id, room_id)
      VALUES (
        gen_random_uuid(), NOW() - (random()*30||' days')::interval,
        titulos[floor(random()*7+1)] || ' de ' || disciplinas[floor(random()*12+1)],
        'Clase propia de ' || prof.full_name || ' — ' || niveles[floor(random()*3+1)],
        disciplinas[floor(random()*12+1)],
        niveles[floor(random()*3+1)],
        floor(random()*15+5)::int,
        floor(random()*2+1)*30+30,
        12, 90,
        floor(random()*12000+8000)::int,
        NOW() + (random()*21||' days')::interval,
        NOW() + (random()*21+3||' days')::interval,
        CASE WHEN random() < 0.7 THEN 'PUBLISHED' ELSE 'DRAFT' END,
        'PROPIA', prof.id, room
      );
    END LOOP;
    -- ASIGNADA
    FOR i IN 1..2 LOOP
      SELECT id INTO room FROM rooms ORDER BY random() LIMIT 1;
      INSERT INTO classes (id, created_at, title, description, discipline, level, capacity, duration, min_age, max_age, price, start_time, end_time, status, tipo_clase, teacher_id, room_id)
      VALUES (
        gen_random_uuid(), NOW() - (random()*30||' days')::interval,
        titulos[floor(random()*7+1)] || ' de ' || disciplinas[floor(random()*12+1)],
        'Clase asignada a ' || prof.full_name || ' — ' || niveles[floor(random()*3+1)],
        disciplinas[floor(random()*12+1)],
        niveles[floor(random()*3+1)],
        floor(random()*15+10)::int,
        floor(random()*2+1)*30+30,
        14, 95,
        floor(random()*10000+6000)::int,
        NOW() + (random()*21||' days')::interval,
        NOW() + (random()*21+3||' days')::interval,
        CASE WHEN random() < 0.5 THEN 'PUBLISHED' WHEN random() < 0.85 THEN 'COMPLETED' ELSE 'DRAFT' END,
        'ASIGNADA', prof.id, room
      );
    END LOOP;
  END LOOP;
END $$;
