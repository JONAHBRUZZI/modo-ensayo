-- ============================================================
-- Fotos de sala: permisos de subida/borrado para el gestor de la sede
-- ============================================================
-- Contexto: al crear una sala se exige subir 1–2 fotos. Faltaban permisos para
-- que el gestor (dueño de la sede de esa sala) suba al bucket room-photos y
-- registre filas ROOM en venue_photos. El primer segmento del path en
-- room-photos es el id de la sala (room-photos/{roomId}/archivo).

-- 1. Storage: subir/borrar en room-photos si el usuario administra la sede de la sala.
CREATE POLICY "rphoto_insert" ON storage.objects FOR INSERT TO authenticated
  WITH CHECK (
    bucket_id = 'room-photos' AND EXISTS (
      SELECT 1 FROM public.rooms r
      JOIN public.venues v ON v.id = r.venue_id
      WHERE r.id = ((storage.foldername(name))[1])::uuid AND v.admin_id = auth.uid()
    )
  );

CREATE POLICY "rphoto_delete" ON storage.objects FOR DELETE TO authenticated
  USING (
    bucket_id = 'room-photos' AND EXISTS (
      SELECT 1 FROM public.rooms r
      JOIN public.venues v ON v.id = r.venue_id
      WHERE r.id = ((storage.foldername(name))[1])::uuid AND v.admin_id = auth.uid()
    )
  );

-- 2. Tabla venue_photos: permitir filas ROOM al gestor dueño de la sede de la sala.
CREATE POLICY "vphoto_insert_room_admin" ON public.venue_photos
  FOR INSERT TO authenticated WITH CHECK (
    owner_type = 'ROOM' AND EXISTS (
      SELECT 1 FROM public.rooms r
      JOIN public.venues v ON v.id = r.venue_id
      WHERE r.id = venue_photos.owner_id AND v.admin_id = auth.uid()
    )
  );

CREATE POLICY "vphoto_delete_room_admin" ON public.venue_photos
  FOR DELETE TO authenticated USING (
    owner_type = 'ROOM' AND EXISTS (
      SELECT 1 FROM public.rooms r
      JOIN public.venues v ON v.id = r.venue_id
      WHERE r.id = venue_photos.owner_id AND v.admin_id = auth.uid()
    )
  );
