-- ============================================================
-- REALTIME publication
-- ============================================================
ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications;
ALTER PUBLICATION supabase_realtime ADD TABLE public.classes;
ALTER PUBLICATION supabase_realtime ADD TABLE public.reschedules;
ALTER PUBLICATION supabase_realtime ADD TABLE public.room_schedule_blocks;
