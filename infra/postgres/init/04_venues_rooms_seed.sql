-- 5 SEDES APROBADAS + 20 SALAS ARTISTICAS

insert into venues (admin_id, name, address, description, image_url, phone, email, status)
select u.id, sede.name, sede.address, sede.description, sede.image_url, sede.phone, sede.email, 'APPROVED'
from users u, (values
    ('Sede Centro', 'Calle Artes 123, Santiago', 'Sede principal con 8 salas equipadas para danza clasica y contemporanea. Pisos de madera flotante y barras de ballet profesionales.', '/images/venues/centro.jpg', '+56222221000', 'centro@modoensayo.cl'),
    ('Sede Providencia', 'Av. Los Leones 456, Providencia', 'Espacio moderno con iluminacion natural, salas insonorizadas para musica y teatro. Climatizacion HVAC central.', '/images/venues/providencia.jpg', '+56222222000', 'providencia@modoensayo.cl'),
    ('Sede La Florida', 'Gran Avenida 789, La Florida', 'Sede amplia con estacionamiento, 5 salas multidisciplinarias. Especialidad en danza urbana y folclorica.', '/images/venues/florida.jpg', '+56222223000', 'florida@modoensayo.cl'),
    ('Sede Maipu', 'Av. Pajaritos 101, Maipu', 'Espacio cultural con teatro integrado, salas con espejos de pared completa y sistema de audio profesional.', '/images/venues/maipu.jpg', '+56222224000', 'maipu@modoensayo.cl'),
    ('Sede Las Condes', 'Av. Las Condes 567', 'Sede premium con salas de ballet de alto rendimiento, piso Harlequin, climatizacion independiente por sala.', '/images/venues/lascondes.jpg', '+56222225000', 'lascondes@modoensayo.cl')
) as sede(name, address, description, image_url, phone, email)
where u.email = 'admin@test.com'
and not exists (select 1 from venues where venues.name = sede.name);

-- 20 SALAS CON CARACTERISTICAS ARTISTICAS

-- Sede Centro (8 salas)
with v as (select id from venues where name = 'Sede Centro')
insert into rooms (venue_id, name, capacity, floor_type, has_mirrors, has_sound, has_ballet_bar, has_air_conditioning, has_natural_light, lighting, wall_color, image_url)
select (select id from v), sala.name, sala.capacity, sala.floor_type, sala.has_mirrors, sala.has_sound, sala.has_ballet_bar, sala.has_air_conditioning, sala.has_natural_light, sala.lighting, sala.wall_color, sala.image_url
from (values
    ('Sala Ballet 1', 25, 'Madera flotante (Harlequin)', true, true, true, true, false, 'LED regulable 4000K', 'Blanco perla', '/images/rooms/ce_ballet1.jpg'),
    ('Sala Ballet 2', 20, 'Madera flotante', true, true, true, true, true, 'LED natural 5000K', 'Gris suave', '/images/rooms/ce_ballet2.jpg'),
    ('Sala Danza Contemporanea', 30, 'Linoleo profesional', true, true, false, true, true, 'Luz calida regulable', 'Negro mate', '/images/rooms/ce_contemporanea.jpg'),
    ('Sala de Musica 1', 15, 'Parquet acustico', false, true, false, true, false, 'Luz ambiente + spots', 'Beige', '/images/rooms/ce_musica1.jpg')
) as sala(name, capacity, floor_type, has_mirrors, has_sound, has_ballet_bar, has_air_conditioning, has_natural_light, lighting, wall_color, image_url)
where not exists (select 1 from rooms where rooms.name = sala.name);

-- Sede Providencia (4 salas)
with v as (select id from venues where name = 'Sede Providencia')
insert into rooms (venue_id, name, capacity, floor_type, has_mirrors, has_sound, has_ballet_bar, has_air_conditioning, has_natural_light, lighting, wall_color, image_url)
select (select id from v), sala.name, sala.capacity, sala.floor_type, sala.has_mirrors, sala.has_sound, sala.has_ballet_bar, sala.has_air_conditioning, sala.has_natural_light, sala.lighting, sala.wall_color, sala.image_url
from (values
    ('Sala Teatro 1', 40, 'Parquet', false, true, false, true, false, 'Spotlights direccionales', 'Negro teatral', '/images/rooms/pr_teatro1.jpg'),
    ('Sala Musica Ensamble', 12, 'Alfombra acustica', false, true, false, true, false, 'Luz tenue ambiental', 'Burdeo', '/images/rooms/pr_ensamble.jpg'),
    ('Sala Expresion Corporal', 20, 'Linoleo', true, true, false, true, true, 'Luz natural + LED', 'Blanco', '/images/rooms/pr_expresion.jpg'),
    ('Sala Yoga 1', 18, 'Madera natural', true, false, false, false, true, 'Luz natural difusa', 'Verde salvia', '/images/rooms/pr_yoga1.jpg')
) as sala(name, capacity, floor_type, has_mirrors, has_sound, has_ballet_bar, has_air_conditioning, has_natural_light, lighting, wall_color, image_url)
where not exists (select 1 from rooms where rooms.name = sala.name);

-- Sede La Florida (4 salas)
with v as (select id from venues where name = 'Sede La Florida')
insert into rooms (venue_id, name, capacity, floor_type, has_mirrors, has_sound, has_ballet_bar, has_air_conditioning, has_natural_light, lighting, wall_color, image_url)
select (select id from v), sala.name, sala.capacity, sala.floor_type, sala.has_mirrors, sala.has_sound, sala.has_ballet_bar, sala.has_air_conditioning, sala.has_natural_light, sala.lighting, sala.wall_color, sala.image_url
from (values
    ('Sala Hip Hop', 35, 'Linoleo vinilico', true, true, false, true, true, 'Neon + LED programable', 'Graffiti urbano', '/images/rooms/fl_hiphop.jpg'),
    ('Sala Folklorica', 25, 'Madera', true, true, false, true, true, 'Luz calida 3500K', 'Terracota', '/images/rooms/fl_folclor.jpg'),
    ('Sala Break Dance', 20, 'Linoleo antideslizante', true, true, false, true, false, 'UV + LEDs', 'Negro', '/images/rooms/fl_break.jpg'),
    ('Sala Multiuso', 30, 'Parquet flotante', true, true, true, true, true, 'Luz regulable full spectrum', 'Blanco neutro', '/images/rooms/fl_multi.jpg')
) as sala(name, capacity, floor_type, has_mirrors, has_sound, has_ballet_bar, has_air_conditioning, has_natural_light, lighting, wall_color, image_url)
where not exists (select 1 from rooms where rooms.name = sala.name);

-- Sede Maipu (4 salas)
with v as (select id from venues where name = 'Sede Maipu')
insert into rooms (venue_id, name, capacity, floor_type, has_mirrors, has_sound, has_ballet_bar, has_air_conditioning, has_natural_light, lighting, wall_color, image_url)
select (select id from v), sala.name, sala.capacity, sala.floor_type, sala.has_mirrors, sala.has_sound, sala.has_ballet_bar, sala.has_air_conditioning, sala.has_natural_light, sala.lighting, sala.wall_color, sala.image_url
from (values
    ('Teatro Principal', 50, 'Parquet', false, true, false, true, false, 'Luz teatral 24 canales', 'Negro profundo', '/images/rooms/ma_teatro.jpg'),
    ('Sala Danza Jazz', 22, 'Madera flotante', true, true, true, true, false, 'LED 4500K regulable', 'Azul medianoche', '/images/rooms/ma_jazz.jpg'),
    ('Sala Tap', 15, 'Madera amplificada', true, true, false, true, false, 'Luz tenue intima', 'Rojo pasion', '/images/rooms/ma_tap.jpg'),
    ('Sala Pilates', 15, 'Alfombra', true, false, false, true, true, 'Luz natural', 'Verde menta', '/images/rooms/ma_pilates.jpg')
) as sala(name, capacity, floor_type, has_mirrors, has_sound, has_ballet_bar, has_air_conditioning, has_natural_light, lighting, wall_color, image_url)
where not exists (select 1 from rooms where rooms.name = sala.name);

-- Sede Las Condes (4 salas)
with v as (select id from venues where name = 'Sede Las Condes')
insert into rooms (venue_id, name, capacity, floor_type, has_mirrors, has_sound, has_ballet_bar, has_air_conditioning, has_natural_light, lighting, wall_color, image_url)
select (select id from v), sala.name, sala.capacity, sala.floor_type, sala.has_mirrors, sala.has_sound, sala.has_ballet_bar, sala.has_air_conditioning, sala.has_natural_light, sala.lighting, sala.wall_color, sala.image_url
from (values
    ('Ballet Premium 1', 15, 'Harlequin Cascade', true, true, true, true, true, 'LED profesional 5000K', 'Gris perla', '/images/rooms/lc_balletp1.jpg'),
    ('Ballet Premium 2', 15, 'Harlequin Cascade', true, true, true, true, true, 'LED profesional 5000K', 'Gris perla', '/images/rooms/lc_balletp2.jpg'),
    ('Sala Performance', 30, 'Harlequin Studio', true, true, true, true, true, 'Luz completa + Follow spot', 'Negro', '/images/rooms/lc_performance.jpg'),
    ('Sala Privada Rehearsal', 8, 'Harlequin Activity', true, true, true, true, true, 'LED smart + luz natural', 'Blanco roto', '/images/rooms/lc_rehearsal.jpg')
) as sala(name, capacity, floor_type, has_mirrors, has_sound, has_ballet_bar, has_air_conditioning, has_natural_light, lighting, wall_color, image_url)
where not exists (select 1 from rooms where rooms.name = sala.name);
