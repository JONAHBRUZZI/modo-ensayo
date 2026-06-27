-- ============================================================
-- Política DELETE para salas (rooms): el gestor de la sede puede eliminar
-- sus propias salas.
-- ============================================================
-- Contexto: la página /sede/salas permite eliminar una sala siempre que no
-- tenga reservas. La tabla rooms solo tenía políticas SELECT/INSERT/UPDATE, por
-- lo que cualquier DELETE quedaba denegado por RLS.
--
-- Las tablas hijas room_schedule_blocks y room_maintenances tienen ON DELETE
-- CASCADE, así que sus filas (bloques disponibles, mantenciones) se borran solas
-- al eliminar la sala. En cambio classes.room_id es RESTRICT: si la sala tiene
-- clases asociadas, la base de datos impide el borrado (protección de reservas).

CREATE POLICY "rooms_delete_admin" ON public.rooms
  FOR DELETE TO authenticated USING (
    EXISTS (SELECT 1 FROM public.venues v WHERE v.id = rooms.venue_id AND v.admin_id = auth.uid())
  );
