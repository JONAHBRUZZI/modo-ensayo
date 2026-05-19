-- 5 SEDES DE MUSICA (REGION METROPOLITANA) + 1 CLASE POR SEDE
-- Valores reales de direcciones y comunas de Santiago de Chile

-- Verificar que exista el teacher para asignar las clases
do $$ begin
    if not exists (select 1 from users where email = 'teacher@test.com') then
        insert into users (email, password_hash, full_name, phone)
        values ('teacher@test.com', '$2a$10$hash_placeholder', 'Profesor Demo', '+56912345678');
    end if;
end $$;

-- Insertar rol TEACHER al teacher si no lo tiene
insert into user_roles (user_id, role_id)
select u.id, r.id from users u, roles r
where u.email = 'teacher@test.com' and r.name = 'TEACHER'
and not exists (
    select 1 from user_roles ur
    join users u2 on ur.user_id = u2.id
    join roles r2 on ur.role_id = r2.id
    where u2.email = 'teacher@test.com' and r2.name = 'TEACHER'
);

-- ========================================
-- 5 SEDES DE MUSICA
-- ========================================
insert into venues (admin_id, name, address, description, image_url, phone, email, status)
select u.id, sede.name, sede.address, sede.description, sede.image_url, sede.phone, sede.email, 'APPROVED'
from users u, (values
    ('Escuela Moderna de Musica', 'Av. Providencia 2234, Providencia',
     'Escuela especializada en musica popular y jazz. 4 salas insonorizadas con pianos de cola, baterias y amplificadores. Profesores titulados del Conservatorio Nacional.',
     '/images/venues/musica-providencia.jpg', '+56982345671', 'providencia@modoensayo-musica.cl'),

    ('Conservatorio Bellas Artes', 'Av. Matucana 100, Santiago Centro',
     'Conservatorio con tradicion de 15 anos formando musicos clasicos. Salas con tratamiento acustico profesional, pianos Steinway y sala de conciertos para 80 personas.',
     '/images/venues/musica-centro.jpg', '+56982345672', 'centro@modoensayo-musica.cl'),

    ('Espacio Sonoro Nunoa', 'Av. Irarrazaval 3570, Nunoa',
     'Estudio y academia enfocada en produccion musical y generos urbanos. Cuenta con estudio de grabacion, sala de mezcla y 3 salas de ensayo equipadas con backline completo.',
     '/images/venues/musica-nunoa.jpg', '+56982345673', 'nunoa@modoensayo-musica.cl'),

    ('Academia Allegro Las Condes', 'Av. Apoquindo 7550, Las Condes',
     'Academia premium de musica clasica y contemporanea. 5 salas con acustica certificada, climatizacion independiente y sistema de grabacion digital RME en cada sala. Estacionamiento subterraneo.',
     '/images/venues/musica-lascondes.jpg', '+56982345674', 'lascondes@modoensayo-musica.cl'),

    ('Casa Musical del Maipo', 'Av. Concha y Toro 1590, Puente Alto',
     'Centro cultural y academia musical en la zona sur. Espacio de 400m2 con teatro, sala de ensayo y camerinos. Enfasis en folklore chileno, cueca y musica latinoamericana.',
     '/images/venues/musica-puentealto.jpg', '+56982345675', 'puentealto@modoensayo-musica.cl')
) as sede(name, address, description, image_url, phone, email)
where u.email = 'venueadmin@test.com'
and not exists (select 1 from venues where venues.name = sede.name);

-- ========================================
-- SALAS DE MUSICA (1 por sede)
-- ========================================

-- Sede 1: Escuela Moderna de Musica - Sala Jazz
with v as (select id from venues where name = 'Escuela Moderna de Musica')
insert into rooms (venue_id, name, capacity, floor_type, has_mirrors, has_sound, has_ballet_bar, has_air_conditioning, has_natural_light, lighting, wall_color, image_url)
select (select id from v), sala.name, sala.capacity, sala.floor_type, sala.has_mirrors, sala.has_sound, sala.has_ballet_bar, sala.has_air_conditioning, sala.has_natural_light, sala.lighting, sala.wall_color, sala.image_url
from (values
    ('Sala Jazz & Improvisacion', 12, 'Parquet acustico', false, true, false, true, true,
     'Luz calida regulable 2700K', 'Rojo terciopelo', '/images/rooms/music_jazz.jpg')
) as sala(name, capacity, floor_type, has_mirrors, has_sound, has_ballet_bar, has_air_conditioning, has_natural_light, lighting, wall_color, image_url)
where not exists (select 1 from rooms where rooms.name = sala.name);

-- Sede 2: Conservatorio Bellas Artes - Sala Piano
with v as (select id from venues where name = 'Conservatorio Bellas Artes')
insert into rooms (venue_id, name, capacity, floor_type, has_mirrors, has_sound, has_ballet_bar, has_air_conditioning, has_natural_light, lighting, wall_color, image_url)
select (select id from v), sala.name, sala.capacity, sala.floor_type, sala.has_mirrors, sala.has_sound, sala.has_ballet_bar, sala.has_air_conditioning, sala.has_natural_light, sala.lighting, sala.wall_color, sala.image_url
from (values
    ('Sala Piano Steinway', 8, 'Parquet roble natural', false, false, false, true, true,
     'Luz natural + spots 4000K', 'Blanco marfil', '/images/rooms/music_piano.jpg')
) as sala(name, capacity, floor_type, has_mirrors, has_sound, has_ballet_bar, has_air_conditioning, has_natural_light, lighting, wall_color, image_url)
where not exists (select 1 from rooms where rooms.name = sala.name);

-- Sede 3: Espacio Sonoro Nunoa - Sala Electrica
with v as (select id from venues where name = 'Espacio Sonoro Nunoa')
insert into rooms (venue_id, name, capacity, floor_type, has_mirrors, has_sound, has_ballet_bar, has_air_conditioning, has_natural_light, lighting, wall_color, image_url)
select (select id from v), sala.name, sala.capacity, sala.floor_type, sala.has_mirrors, sala.has_sound, sala.has_ballet_bar, sala.has_air_conditioning, sala.has_natural_light, sala.lighting, sala.wall_color, sala.image_url
from (values
    ('Sala Electrica & Produccion', 10, 'Piso flotante acustico', true, true, false, true, false,
     'LED RGB programable + spots', 'Negro grafito', '/images/rooms/music_electric.jpg')
) as sala(name, capacity, floor_type, has_mirrors, has_sound, has_ballet_bar, has_air_conditioning, has_natural_light, lighting, wall_color, image_url)
where not exists (select 1 from rooms where rooms.name = sala.name);

-- Sede 4: Academia Allegro Las Condes - Sala Percusion
with v as (select id from venues where name = 'Academia Allegro Las Condes')
insert into rooms (venue_id, name, capacity, floor_type, has_mirrors, has_sound, has_ballet_bar, has_air_conditioning, has_natural_light, lighting, wall_color, image_url)
select (select id from v), sala.name, sala.capacity, sala.floor_type, sala.has_mirrors, sala.has_sound, sala.has_ballet_bar, sala.has_air_conditioning, sala.has_natural_light, sala.lighting, sala.wall_color, sala.image_url
from (values
    ('Sala Percusion & Bateria', 6, 'Piso amortiguado profesional', false, true, false, true, true,
     'Luz ambiente 3500K', 'Gris acustico', '/images/rooms/music_percussion.jpg')
) as sala(name, capacity, floor_type, has_mirrors, has_sound, has_ballet_bar, has_air_conditioning, has_natural_light, lighting, wall_color, image_url)
where not exists (select 1 from rooms where rooms.name = sala.name);

-- Sede 5: Casa Musical del Maipo - Sala Folklore
with v as (select id from venues where name = 'Casa Musical del Maipo')
insert into rooms (venue_id, name, capacity, floor_type, has_mirrors, has_sound, has_ballet_bar, has_air_conditioning, has_natural_light, lighting, wall_color, image_url)
select (select id from v), sala.name, sala.capacity, sala.floor_type, sala.has_mirrors, sala.has_sound, sala.has_ballet_bar, sala.has_air_conditioning, sala.has_natural_light, sala.lighting, sala.wall_color, sala.image_url
from (values
    ('Sala Folklore & Cuerdas', 15, 'Madera nativa chilena', true, true, false, true, true,
     'Luz calida 3000K', 'Terracota artesanal', '/images/rooms/music_folklore.jpg')
) as sala(name, capacity, floor_type, has_mirrors, has_sound, has_ballet_bar, has_air_conditioning, has_natural_light, lighting, wall_color, image_url)
where not exists (select 1 from rooms where rooms.name = sala.name);

-- ========================================
-- 1 CLASE POR SEDE (usando el teacher existente)
-- ========================================

-- Piano para Principiantes en Conservatorio Bellas Artes
insert into classes (room_id, teacher_id, title, discipline, capacity, price, start_time, end_time, status)
select r.id, u.id, 'Piano para Principiantes', 'MUSICA', 6, 18000,
       (current_date + interval '2 days' + interval '10 hours')::timestamptz,
       (current_date + interval '2 days' + interval '11 hours 30 minutes')::timestamptz,
       'PUBLISHED'
from rooms r
cross join users u
cross join venues v
where r.venue_id = v.id
and v.name = 'Conservatorio Bellas Artes'
and r.name = 'Sala Piano Steinway'
and u.email = 'teacher@test.com'
and not exists (select 1 from classes c where c.title = 'Piano para Principiantes');

-- Canto Lirico en Escuela Moderna de Musica
insert into classes (room_id, teacher_id, title, discipline, capacity, price, start_time, end_time, status)
select r.id, u.id, 'Canto Lirico', 'MUSICA', 10, 15000,
       (current_date + interval '3 days' + interval '14 hours')::timestamptz,
       (current_date + interval '3 days' + interval '16 hours')::timestamptz,
       'PUBLISHED'
from rooms r
cross join users u
cross join venues v
where r.venue_id = v.id
and v.name = 'Escuela Moderna de Musica'
and r.name = 'Sala Jazz & Improvisacion'
and u.email = 'teacher@test.com'
and not exists (select 1 from classes c where c.title = 'Canto Lirico');

-- Guitarra Electrica en Espacio Sonoro Nunoa
insert into classes (room_id, teacher_id, title, discipline, capacity, price, start_time, end_time, status)
select r.id, u.id, 'Guitarra Electrica', 'MUSICA', 8, 12000,
       (current_date + interval '1 day' + interval '17 hours')::timestamptz,
       (current_date + interval '1 day' + interval '18 hours 30 minutes')::timestamptz,
       'PUBLISHED'
from rooms r
cross join users u
cross join venues v
where r.venue_id = v.id
and v.name = 'Espacio Sonoro Nunoa'
and r.name = 'Sala Electrica & Produccion'
and u.email = 'teacher@test.com'
and not exists (select 1 from classes c where c.title = 'Guitarra Electrica');

-- Bateria y Percusion en Academia Allegro Las Condes
insert into classes (room_id, teacher_id, title, discipline, capacity, price, start_time, end_time, status)
select r.id, u.id, 'Bateria y Percusion', 'MUSICA', 4, 25000,
       (current_date + interval '4 days' + interval '10 hours')::timestamptz,
       (current_date + interval '4 days' + interval '12 hours')::timestamptz,
       'PUBLISHED'
from rooms r
cross join users u
cross join venues v
where r.venue_id = v.id
and v.name = 'Academia Allegro Las Condes'
and r.name = 'Sala Percusion & Bateria'
and u.email = 'teacher@test.com'
and not exists (select 1 from classes c where c.title = 'Bateria y Percusion');

-- Violin Clasico en Casa Musical del Maipo
insert into classes (room_id, teacher_id, title, discipline, capacity, price, start_time, end_time, status)
select r.id, u.id, 'Violin Clasico', 'MUSICA', 10, 14000,
       (current_date + interval '5 days' + interval '15 hours')::timestamptz,
       (current_date + interval '5 days' + interval '16 hours 30 minutes')::timestamptz,
       'PUBLISHED'
from rooms r
cross join users u
cross join venues v
where r.venue_id = v.id
and v.name = 'Casa Musical del Maipo'
and r.name = 'Sala Folklore & Cuerdas'
and u.email = 'teacher@test.com'
and not exists (select 1 from classes c where c.title = 'Violin Clasico');
