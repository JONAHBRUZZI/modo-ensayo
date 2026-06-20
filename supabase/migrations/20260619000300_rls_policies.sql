-- ============================================================
-- RLS POLICIES (94 policies)
-- ============================================================

-- 1. profiles
CREATE POLICY "profiles_select_own" ON public.profiles
  FOR SELECT TO authenticated USING (id = auth.uid());
CREATE POLICY "profiles_select_admin" ON public.profiles
  FOR SELECT TO authenticated USING (public.has_role('ADMIN'));
CREATE POLICY "profiles_insert_own" ON public.profiles
  FOR INSERT TO authenticated WITH CHECK (id = auth.uid());
CREATE POLICY "profiles_update_own" ON public.profiles
  FOR UPDATE TO authenticated USING (id = auth.uid())
  WITH CHECK (id = auth.uid());
CREATE POLICY "profiles_update_admin" ON public.profiles
  FOR UPDATE TO authenticated USING (public.has_role('ADMIN'));

-- 2. professional_profiles
CREATE POLICY "pp_select_public" ON public.professional_profiles
  FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "pp_insert_own" ON public.professional_profiles
  FOR INSERT TO authenticated WITH CHECK (id = auth.uid());
CREATE POLICY "pp_update_own" ON public.professional_profiles
  FOR UPDATE TO authenticated USING (id = auth.uid())
  WITH CHECK (id = auth.uid());

-- 3. identity_verifications
CREATE POLICY "idver_select_own" ON public.identity_verifications
  FOR SELECT TO authenticated USING (user_id = auth.uid());
CREATE POLICY "idver_select_admin" ON public.identity_verifications
  FOR SELECT TO authenticated USING (public.has_role('ADMIN'));
CREATE POLICY "idver_insert_own" ON public.identity_verifications
  FOR INSERT TO authenticated WITH CHECK (user_id = auth.uid());
CREATE POLICY "idver_update_admin" ON public.identity_verifications
  FOR UPDATE TO authenticated USING (public.has_role('ADMIN'));

-- 4. refund_methods
CREATE POLICY "refund_select_own" ON public.refund_methods
  FOR SELECT TO authenticated USING (user_id = auth.uid());
CREATE POLICY "refund_insert_own" ON public.refund_methods
  FOR INSERT TO authenticated WITH CHECK (user_id = auth.uid());
CREATE POLICY "refund_delete_own" ON public.refund_methods
  FOR DELETE TO authenticated USING (user_id = auth.uid());

-- 5. venues
CREATE POLICY "venues_select_approved" ON public.venues
  FOR SELECT TO anon, authenticated USING (status = 'APROBADA');
CREATE POLICY "venues_select_admin" ON public.venues
  FOR SELECT TO authenticated USING (admin_id = auth.uid() OR public.has_role('ADMIN'));
CREATE POLICY "venues_insert_auth" ON public.venues
  FOR INSERT TO authenticated WITH CHECK (admin_id = auth.uid());
CREATE POLICY "venues_update_admin" ON public.venues
  FOR UPDATE TO authenticated USING (admin_id = auth.uid() OR public.has_role('ADMIN'))
  WITH CHECK (admin_id = auth.uid() OR public.has_role('ADMIN'));

-- 6. rooms
CREATE POLICY "rooms_select_public" ON public.rooms
  FOR SELECT TO anon, authenticated USING (
    EXISTS (SELECT 1 FROM public.venues v WHERE v.id = rooms.venue_id AND v.status = 'APROBADA')
  );
CREATE POLICY "rooms_select_admin" ON public.rooms
  FOR SELECT TO authenticated USING (
    EXISTS (SELECT 1 FROM public.venues v WHERE v.id = rooms.venue_id AND v.admin_id = auth.uid())
    OR public.has_role('ADMIN')
  );
CREATE POLICY "rooms_insert_admin" ON public.rooms
  FOR INSERT TO authenticated WITH CHECK (
    EXISTS (SELECT 1 FROM public.venues v WHERE v.id = rooms.venue_id AND v.admin_id = auth.uid())
  );
CREATE POLICY "rooms_update_admin" ON public.rooms
  FOR UPDATE TO authenticated USING (
    EXISTS (SELECT 1 FROM public.venues v WHERE v.id = rooms.venue_id AND v.admin_id = auth.uid())
    OR public.has_role('ADMIN')
  );

-- 7. venue_schedules
CREATE POLICY "vsched_select_public" ON public.venue_schedules
  FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "vsched_insert_admin" ON public.venue_schedules
  FOR INSERT TO authenticated WITH CHECK (public.is_venue_admin(venue_id));
CREATE POLICY "vsched_update_admin" ON public.venue_schedules
  FOR UPDATE TO authenticated USING (public.is_venue_admin(venue_id));
CREATE POLICY "vsched_delete_admin" ON public.venue_schedules
  FOR DELETE TO authenticated USING (public.is_venue_admin(venue_id));

-- 8. venue_block_configs
CREATE POLICY "vbc_select_public" ON public.venue_block_configs
  FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "vbc_upsert_admin" ON public.venue_block_configs
  FOR INSERT TO authenticated WITH CHECK (public.is_venue_admin(venue_id));
CREATE POLICY "vbc_update_admin" ON public.venue_block_configs
  FOR UPDATE TO authenticated USING (public.is_venue_admin(venue_id));

-- 9. room_schedule_blocks
CREATE POLICY "rsb_select_public" ON public.room_schedule_blocks
  FOR SELECT TO anon, authenticated USING (status = 'AVAILABLE');
CREATE POLICY "rsb_select_admin" ON public.room_schedule_blocks
  FOR SELECT TO authenticated USING (
    EXISTS (
      SELECT 1 FROM public.rooms r
      JOIN public.venues v ON v.id = r.venue_id
      WHERE r.id = room_schedule_blocks.room_id AND v.admin_id = auth.uid()
    )
    OR public.has_role('ADMIN')
  );
CREATE POLICY "rsb_update_admin" ON public.room_schedule_blocks
  FOR UPDATE TO authenticated USING (
    EXISTS (
      SELECT 1 FROM public.rooms r
      JOIN public.venues v ON v.id = r.venue_id
      WHERE r.id = room_schedule_blocks.room_id AND v.admin_id = auth.uid()
    )
  );

-- 10. room_maintenances
CREATE POLICY "rmaint_select_admin" ON public.room_maintenances
  FOR SELECT TO authenticated USING (
    EXISTS (
      SELECT 1 FROM public.rooms r JOIN public.venues v ON v.id = r.venue_id
      WHERE r.id = room_maintenances.room_id AND v.admin_id = auth.uid()
    )
    OR public.has_role('ADMIN')
  );
CREATE POLICY "rmaint_insert_admin" ON public.room_maintenances
  FOR INSERT TO authenticated WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.rooms r JOIN public.venues v ON v.id = r.venue_id
      WHERE r.id = room_maintenances.room_id AND v.admin_id = auth.uid()
    )
  );
CREATE POLICY "rmaint_delete_admin" ON public.room_maintenances
  FOR DELETE TO authenticated USING (
    EXISTS (
      SELECT 1 FROM public.rooms r JOIN public.venues v ON v.id = r.venue_id
      WHERE r.id = room_maintenances.room_id AND v.admin_id = auth.uid()
    )
  );

-- 11. classes
CREATE POLICY "classes_select_public" ON public.classes
  FOR SELECT TO anon, authenticated USING (status = 'PUBLISHED');
CREATE POLICY "classes_select_teacher" ON public.classes
  FOR SELECT TO authenticated USING (teacher_id = auth.uid());
CREATE POLICY "classes_select_enrolled" ON public.classes
  FOR SELECT TO authenticated USING (public.is_enrolled(id));
CREATE POLICY "classes_insert_teacher" ON public.classes
  FOR INSERT TO authenticated WITH CHECK (teacher_id = auth.uid());
CREATE POLICY "classes_update_teacher" ON public.classes
  FOR UPDATE TO authenticated USING (teacher_id = auth.uid() OR public.has_role('ADMIN'))
  WITH CHECK (teacher_id = auth.uid() OR public.has_role('ADMIN'));
CREATE POLICY "classes_delete_draft" ON public.classes
  FOR DELETE TO authenticated USING (teacher_id = auth.uid() AND status = 'DRAFT');

-- 12. class_status_history
CREATE POLICY "csh_select_admin" ON public.class_status_history
  FOR SELECT TO authenticated USING (public.has_role('ADMIN'));
CREATE POLICY "csh_select_teacher" ON public.class_status_history
  FOR SELECT TO authenticated USING (public.is_class_teacher(class_id));
CREATE POLICY "csh_insert_system" ON public.class_status_history
  FOR INSERT TO authenticated WITH CHECK (true);

-- 13. discipline_catalog
CREATE POLICY "disc_select_public" ON public.discipline_catalog
  FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "disc_insert_admin" ON public.discipline_catalog
  FOR INSERT TO authenticated WITH CHECK (public.has_role('ADMIN'));
CREATE POLICY "disc_update_admin" ON public.discipline_catalog
  FOR UPDATE TO authenticated USING (public.has_role('ADMIN'));
CREATE POLICY "disc_delete_admin" ON public.discipline_catalog
  FOR DELETE TO authenticated USING (public.has_role('ADMIN'));

-- 14. cart_items
CREATE POLICY "cart_select_own" ON public.cart_items
  FOR SELECT TO authenticated USING (owner_id = auth.uid());
CREATE POLICY "cart_insert_own" ON public.cart_items
  FOR INSERT TO authenticated WITH CHECK (owner_id = auth.uid());
CREATE POLICY "cart_delete_own" ON public.cart_items
  FOR DELETE TO authenticated USING (owner_id = auth.uid());

-- 15. payment_sessions
CREATE POLICY "psess_select_own" ON public.payment_sessions
  FOR SELECT TO authenticated USING (owner_id = auth.uid());

-- 16. enrollments
CREATE POLICY "enr_select_own" ON public.enrollments
  FOR SELECT TO authenticated USING (student_id = auth.uid());
CREATE POLICY "enr_select_teacher" ON public.enrollments
  FOR SELECT TO authenticated USING (
    EXISTS (SELECT 1 FROM public.classes c WHERE c.id = enrollments.class_id AND c.teacher_id = auth.uid())
  );
CREATE POLICY "enr_select_admin" ON public.enrollments
  FOR SELECT TO authenticated USING (public.has_role('ADMIN'));

-- 17. payments
CREATE POLICY "pay_select_own" ON public.payments
  FOR SELECT TO authenticated USING (
    EXISTS (SELECT 1 FROM public.enrollments e WHERE e.id = payments.enrollment_id AND e.student_id = auth.uid())
  );
CREATE POLICY "pay_select_teacher" ON public.payments
  FOR SELECT TO authenticated USING (
    EXISTS (
      SELECT 1 FROM public.enrollments e
      JOIN public.classes c ON c.id = e.class_id
      WHERE e.id = payments.enrollment_id AND c.teacher_id = auth.uid()
    )
  );
CREATE POLICY "pay_select_admin" ON public.payments
  FOR SELECT TO authenticated USING (public.has_role('ADMIN'));

-- 18. reschedules
CREATE POLICY "resched_select_teacher" ON public.reschedules
  FOR SELECT TO authenticated USING (teacher_id = auth.uid());
CREATE POLICY "resched_select_enrolled" ON public.reschedules
  FOR SELECT TO authenticated USING (public.is_enrolled(class_id));
CREATE POLICY "resched_select_admin" ON public.reschedules
  FOR SELECT TO authenticated USING (public.has_role('ADMIN'));
CREATE POLICY "resched_insert_auth" ON public.reschedules
  FOR INSERT TO authenticated WITH CHECK (true);

-- 19. reschedule_responses
CREATE POLICY "rrep_select_own" ON public.reschedule_responses
  FOR SELECT TO authenticated USING (user_id = auth.uid());
CREATE POLICY "rrep_select_teacher" ON public.reschedule_responses
  FOR SELECT TO authenticated USING (
    EXISTS (SELECT 1 FROM public.reschedules r WHERE r.id = reschedule_responses.reschedule_id AND r.teacher_id = auth.uid())
  );
CREATE POLICY "rrep_update_own" ON public.reschedule_responses
  FOR UPDATE TO authenticated USING (user_id = auth.uid());

-- 20. notifications
CREATE POLICY "notif_select_own" ON public.notifications
  FOR SELECT TO authenticated USING (user_id = auth.uid());
CREATE POLICY "notif_update_own" ON public.notifications
  FOR UPDATE TO authenticated USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());

-- 21. reviews
CREATE POLICY "rev_select_public" ON public.reviews
  FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "rev_insert_auth" ON public.reviews
  FOR INSERT TO authenticated WITH CHECK (reviewer_id = auth.uid());
CREATE POLICY "rev_delete_admin" ON public.reviews
  FOR DELETE TO authenticated USING (public.has_role('ADMIN'));

-- 22. attendances
CREATE POLICY "att_select_teacher" ON public.attendances
  FOR SELECT TO authenticated USING (
    EXISTS (SELECT 1 FROM public.classes c WHERE c.id = attendances.class_id AND c.teacher_id = auth.uid())
  );
CREATE POLICY "att_select_own" ON public.attendances
  FOR SELECT TO authenticated USING (beneficiary_id = auth.uid());
CREATE POLICY "att_insert_teacher" ON public.attendances
  FOR INSERT TO authenticated WITH CHECK (
    EXISTS (SELECT 1 FROM public.classes c WHERE c.id = attendances.class_id AND c.teacher_id = auth.uid())
  );

-- 23. associates
CREATE POLICY "assoc_select_own" ON public.associates
  FOR SELECT TO authenticated USING (owner_id = auth.uid());
CREATE POLICY "assoc_insert_own" ON public.associates
  FOR INSERT TO authenticated WITH CHECK (owner_id = auth.uid());
CREATE POLICY "assoc_delete_own" ON public.associates
  FOR DELETE TO authenticated USING (owner_id = auth.uid());

-- 24. venue_photos
CREATE POLICY "vphoto_select_public" ON public.venue_photos
  FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "vphoto_insert_admin" ON public.venue_photos
  FOR INSERT TO authenticated WITH CHECK (
    (owner_type = 'VENUE' AND public.is_venue_admin(owner_id))
    OR public.has_role('ADMIN')
  );
CREATE POLICY "vphoto_delete_admin" ON public.venue_photos
  FOR DELETE TO authenticated USING (
    (owner_type = 'VENUE' AND public.is_venue_admin(owner_id))
    OR public.has_role('ADMIN')
  );

-- 25. venue_documents
CREATE POLICY "vdoc_select_admin" ON public.venue_documents
  FOR SELECT TO authenticated USING (
    public.is_venue_admin(venue_id) OR public.has_role('ADMIN')
  );
CREATE POLICY "vdoc_insert_admin" ON public.venue_documents
  FOR INSERT TO authenticated WITH CHECK (public.is_venue_admin(venue_id));
CREATE POLICY "vdoc_update_admin" ON public.venue_documents
  FOR UPDATE TO authenticated USING (public.has_role('ADMIN'));

-- 26. audit_logs
CREATE POLICY "audit_select_admin" ON public.audit_logs
  FOR SELECT TO authenticated USING (public.has_role('ADMIN'));

-- 27. system_metrics
CREATE POLICY "sysmet_select_admin" ON public.system_metrics
  FOR SELECT TO authenticated USING (public.has_role('ADMIN'));
