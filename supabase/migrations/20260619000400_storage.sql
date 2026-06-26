-- ============================================================
-- STORAGE BUCKETS + POLICIES
-- ============================================================

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES
  ('avatars', 'avatars', true, 2097152, '{image/jpeg,image/png,image/webp}'),
  ('venue-photos', 'venue-photos', true, 5242880, '{image/jpeg,image/png,image/webp}'),
  ('room-photos', 'room-photos', true, 5242880, '{image/jpeg,image/png,image/webp}'),
  ('venue-documents', 'venue-documents', false, 5242880, '{image/jpeg,image/png,application/pdf}'),
  ('identity-docs', 'identity-docs', false, 5242880, '{image/jpeg,image/png,application/pdf}')
ON CONFLICT (id) DO NOTHING;

-- avatars: público lectura, owner escritura
CREATE POLICY "avatars_select" ON storage.objects FOR SELECT USING (bucket_id = 'avatars');
CREATE POLICY "avatars_insert" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'avatars' AND auth.uid()::text = (storage.foldername(name))[1]);
CREATE POLICY "avatars_update" ON storage.objects FOR UPDATE USING (bucket_id = 'avatars' AND auth.uid()::text = (storage.foldername(name))[1]);
CREATE POLICY "avatars_delete" ON storage.objects FOR DELETE USING (bucket_id = 'avatars' AND auth.uid()::text = (storage.foldername(name))[1]);

-- venue-photos: público lectura, venue admin escritura
CREATE POLICY "vphoto_select" ON storage.objects FOR SELECT USING (bucket_id = 'venue-photos');
CREATE POLICY "vphoto_insert" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'venue-photos' AND (SELECT public.is_venue_admin((storage.foldername(name))[1]::uuid)));
CREATE POLICY "vphoto_delete" ON storage.objects FOR DELETE USING (bucket_id = 'venue-photos' AND (SELECT public.is_venue_admin((storage.foldername(name))[1]::uuid)));

-- room-photos: público lectura
CREATE POLICY "rphoto_select" ON storage.objects FOR SELECT USING (bucket_id = 'room-photos');

-- venue-documents: privado, venue admin + admin
CREATE POLICY "vdoc_select" ON storage.objects FOR SELECT USING (bucket_id = 'venue-documents' AND (SELECT public.is_venue_admin((storage.foldername(name))[1]::uuid) OR public.has_role('ADMIN')));
CREATE POLICY "vdoc_insert" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'venue-documents' AND (SELECT public.is_venue_admin((storage.foldername(name))[1]::uuid)));

-- identity-docs: privado, owner + admin
CREATE POLICY "idoc_select" ON storage.objects FOR SELECT USING (bucket_id = 'identity-docs' AND (auth.uid()::text = (storage.foldername(name))[1] OR public.has_role('ADMIN')));
CREATE POLICY "idoc_insert" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'identity-docs' AND auth.uid()::text = (storage.foldername(name))[1]);
