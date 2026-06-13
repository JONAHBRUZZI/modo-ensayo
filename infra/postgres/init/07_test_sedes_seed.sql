-- 20 sedes de prueba — Test 1 al 20
-- admin: a5db04ca-0dab-4a0a-8f27-a478325df778

INSERT INTO venues (id, created_at, name, city, address, description, phone, email, tipo, status, instagram, sitio_web, facebook, youtube, admin_id)
VALUES
(gen_random_uuid(), NOW(), 'Test 1 — Academia Ritmo y Movimiento', 'Santiago', 'Av. Providencia 1234', 'Academia de danza con 6 salas equipadas, espejos, barras de ballet y piso flotante profesional.', '+56222345678', 'ritmo@test.cl', 'SEDE', 'APROBADA', '@ritmo_movimiento', 'https://ritmo.cl', 'ritmo.movimiento', 'youtube.com/@ritmo', 'a5db04ca-0dab-4a0a-8f27-a478325df778'),

(gen_random_uuid(), NOW(), 'Test 2 — Estudio Sonido Vivo', 'Providencia', 'Av. Nueva Providencia 2345', 'Estudio de grabación y ensayo con sala insonorizada, amplificación profesional y entrada auxiliar.', '+56981234567', 'sonido@test.cl', 'SEDE', 'APROBADA', '@sonido_vivo', 'https://sonidovivo.cl', NULL, NULL, 'a5db04ca-0dab-4a0a-8f27-a478325df778'),

(gen_random_uuid(), NOW(), 'Test 3 — HomeStudio Las Condes', 'Las Condes', 'Camino El Alba 890', 'HomeStudio privado con piano de cola, sistema de sonido y capacidad para 10 personas.', '+56976543210', 'homestudio@test.cl', 'HOME_STUDIO', 'APROBADA', '@homestudio_lc', NULL, NULL, NULL, 'a5db04ca-0dab-4a0a-8f27-a478325df778'),

(gen_random_uuid(), NOW(), 'Test 4 — Centro Cultural la Casona', 'Ñuñoa', 'Irarrázaval 4321', 'Casona patrimonial con 4 salas multidisciplinarias, teatro íntimo y patio techado para ensayos.', '+56222987654', 'casona@test.cl', 'SEDE', 'APROBADA', '@centro_lacasona', 'https://casonacl.cl', 'centrolacasona', 'youtube.com/@casonacl', 'a5db04ca-0dab-4a0a-8f27-a478325df778'),

(gen_random_uuid(), NOW(), 'Test 5 — Academia Musical Crescendo', 'La Florida', 'Av. Vicuña Mackenna 10700', 'Academia musical con 8 salas, baterías, amplificadores, sistema PA y profesores certificados.', '+56222345001', 'crescendo@test.cl', 'SEDE', 'APROBADA', '@crescendo_music', 'https://crescendo.cl', 'crescendomusic', 'youtube.com/@crescendo', 'a5db04ca-0dab-4a0a-8f27-a478325df778'),

(gen_random_uuid(), NOW(), 'Test 6 — Taller de Arte Escénico', 'Santiago Centro', 'San Diego 1234', 'Taller especializado en teatro y expresión corporal con piso de madera, iluminación y espejos.', '+56988776655', 'arte@test.cl', 'SEDE', 'PENDIENTE_APROBACION', '@taller_arte_escenico', NULL, NULL, NULL, 'a5db04ca-0dab-4a0a-8f27-a478325df778'),

(gen_random_uuid(), NOW(), 'Test 7 — HomeStudio Musical Maipú', 'Maipú', 'Av. Pajaritos 4567', 'Espacio musical con batería, amplificación y micrófonos. Ideal para bandas pequeñas.', '+56977665544', 'homestudio_m@test.cl', 'HOME_STUDIO', 'APROBADA', '@homestudio_maipu', NULL, NULL, NULL, 'a5db04ca-0dab-4a0a-8f27-a478325df778'),

(gen_random_uuid(), NOW(), 'Test 8 — Escuela Danza Fusión', 'Vitacura', 'Av. Bicentenario 3800', 'Escuela de danza contemporánea y urbana con piso flotante, aire acondicionado y estacionamiento.', '+56222456123', 'danza@test.cl', 'SEDE', 'APROBADA', '@danzafusion_cl', 'https://danzafusion.cl', 'danzafusion', 'youtube.com/@danzafusion', 'a5db04ca-0dab-4a0a-8f27-a478325df778'),

(gen_random_uuid(), NOW(), 'Test 9 — Sala de Ensayo El Galpón', 'Recoleta', 'Av. Recoleta 2890', 'Galpón reciclado con 3 salas amplias, baterías, amplificadores e insonorización profesional.', '+56966554433', 'galpon@test.cl', 'SEDE', 'APROBADA', '@elgalponensayos', NULL, 'Ensayo El Galpon', 'youtube.com/@elgalpon', 'a5db04ca-0dab-4a0a-8f27-a478325df778'),

(gen_random_uuid(), NOW(), 'Test 10 — HomeStudio Peñalolén', 'Peñalolén', 'Av. Grecia 7600', 'Espacio para clases de canto, piano y guitarra. Incluye teclado, amplificación y aire acondicionado.', '+56955443322', 'homestudio_p@test.cl', 'HOME_STUDIO', 'PENDIENTE_APROBACION', NULL, NULL, NULL, NULL, 'a5db04ca-0dab-4a0a-8f27-a478325df778'),

(gen_random_uuid(), NOW(), 'Test 11 — Estudio Danza Vital', 'La Reina', 'Av. Ossa 3400', 'Estudio boutique de danza y yoga con luz natural, piso de madera y calefacción central.', '+56222999001', 'danzavital@test.cl', 'SEDE', 'APROBADA', '@danza_vital', NULL, NULL, NULL, 'a5db04ca-0dab-4a0a-8f27-a478325df778'),

(gen_random_uuid(), NOW(), 'Test 12 — Academia de Batería Percute', 'San Miguel', 'Gran Av. 5200', 'Academia especializada en percusión y batería con 4 salas insonorizadas y equipamiento completo.', '+56944332211', 'percute@test.cl', 'SEDE', 'APROBADA', '@academia_percute', 'https://percute.cl', NULL, NULL, 'a5db04ca-0dab-4a0a-8f27-a478325df778'),

(gen_random_uuid(), NOW(), 'Test 13 — HomeStudio La Cisterna', 'La Cisterna', 'Av. Américo Vespucio 1200', 'Espacio para ensayos pequeños, clases de bajo y guitarra eléctrica. Amplificación y micrófonos disponibles.', '+56933221100', 'homestudio_cisterna@test.cl', 'HOME_STUDIO', 'APROBADA', NULL, NULL, NULL, NULL, 'a5db04ca-0dab-4a0a-8f27-a478325df778'),

(gen_random_uuid(), NOW(), 'Test 14 — Centro Artístico Quinta Normal', 'Quinta Normal', 'Av. Carrascal 3400', 'Centro con 5 salas, teatro, estudio de grabación y cafetería. Espacio multidisciplinario.', '+56222567890', 'centroartistico@test.cl', 'SEDE', 'APROBADA', '@centro_artistico', 'https://centroartistico.cl', 'centro.artistico', 'youtube.com/@centroartistico', 'a5db04ca-0dab-4a0a-8f27-a478325df778'),

(gen_random_uuid(), NOW(), 'Test 15 — Escuela de Voz y Canto', 'Independencia', 'Av. Independencia 2900', 'Escuela especializada en técnica vocal con 3 salas equipadas, pianos y sistema de grabación.', '+56922110099', 'voz@test.cl', 'SEDE', 'SUSPENDIDA', '@escuela_voz', NULL, 'escueladevoz', NULL, 'a5db04ca-0dab-4a0a-8f27-a478325df778'),

(gen_random_uuid(), NOW(), 'Test 16 — Taller de Ensayo Los Leones', 'Providencia', 'Av. Los Leones 1200', 'Taller con 2 salas equipadas para ensayos de bandas y clases grupales. Batería y amplificación.', '+56988770011', 'losleones@test.cl', 'SEDE', 'APROBADA', '@ensayo_leones', NULL, NULL, NULL, 'a5db04ca-0dab-4a0a-8f27-a478325df778'),

(gen_random_uuid(), NOW(), 'Test 17 — HomeStudio El Bosque', 'El Bosque', 'Av. San Bernardo 2300', 'Espacio familiar adaptado para clases de danza folklórica con piso de madera y espejos.', '+56999887766', 'homestudio_bosque@test.cl', 'HOME_STUDIO', 'APROBADA', NULL, NULL, NULL, NULL, 'a5db04ca-0dab-4a0a-8f27-a478325df778'),

(gen_random_uuid(), NOW(), 'Test 18 — Academia Integral de Arte', 'Macul', 'Av. Macul 5500', 'Academia con 6 salas multiuso, todas con aire acondicionado e insonorización. Piano en cada sala.', '+56222334001', 'integral@test.cl', 'SEDE', 'RECHAZADA', NULL, 'https://academiaintegral.cl', NULL, NULL, 'a5db04ca-0dab-4a0a-8f27-a478325df778'),

(gen_random_uuid(), NOW(), 'Test 19 — Sala Alternativa El Árbol', 'Estación Central', 'Av. Libertador Bernardo OHiggins 4500', 'Sala amplia tipo galpón con escenario, luces y sonido profesional. Ideal para presentaciones y clases.', '+56955667788', 'arbol@test.cl', 'SEDE', 'APROBADA', '@elarbol_cultural', NULL, 'El Arbol Cultural', 'youtube.com/@elarbol', 'a5db04ca-0dab-4a0a-8f27-a478325df778'),

(gen_random_uuid(), NOW(), 'Test 20 — HomeStudio San Joaquín', 'San Joaquín', 'Av. Carlos Valdovinos 2100', 'Espacio para clases de guitarra y canto. Amplificación, micrófonos y teclado disponibles.', '+56944556677', 'homestudio_sj@test.cl', 'HOME_STUDIO', 'PENDIENTE_APROBACION', '@homestudio_sj', NULL, NULL, NULL, 'a5db04ca-0dab-4a0a-8f27-a478325df778');
