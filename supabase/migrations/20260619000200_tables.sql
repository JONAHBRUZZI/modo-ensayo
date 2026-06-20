-- ============================================================
-- 27 TABLES (in FK dependency order)
-- ============================================================

-- 1. profiles (extends auth.users)
CREATE TABLE public.profiles (
  id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  full_name text NOT NULL,
  social_name text,
  phone text,
  rut text UNIQUE,
  identidad_validada boolean NOT NULL DEFAULT false,
  identidad_estado text NOT NULL DEFAULT 'SIN_VALIDAR'
    CHECK (identidad_estado IN ('SIN_VALIDAR','PENDIENTE','APROBADO','RECHAZADO')),
  tiene_sede_aprobada boolean NOT NULL DEFAULT false,
  preferred_refund_method_id uuid,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_profiles_updated_at BEFORE UPDATE ON public.profiles
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

CREATE TRIGGER trg_new_user_profile
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
CREATE TRIGGER trg_assign_default_role
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.assign_default_role();

-- 2. professional_profiles
CREATE TABLE public.professional_profiles (
  id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  description text,
  photo_url text,
  average_rating numeric(3,2),
  specialty text,
  experience_years int,
  especialidad text,
  nivel_ensenanza text,
  formacion text,
  instagram text,
  youtube text,
  sitio_web text,
  linkedin text,
  biografia text,
  disciplina_principal text,
  disciplinas_secundarias text[] NOT NULL DEFAULT '{}',
  tipo_formacion text[] NOT NULL DEFAULT '{}',
  detalle_formacion text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_professional_profiles_updated_at BEFORE UPDATE ON public.professional_profiles
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
ALTER TABLE public.professional_profiles ENABLE ROW LEVEL SECURITY;

-- 3. identity_verifications
CREATE TABLE public.identity_verifications (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  document_url text CHECK (char_length(document_url) <= 500),
  document_type text,
  document_number text,
  full_name text,
  birth_date date,
  status text NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED')),
  reviewed_by uuid REFERENCES auth.users(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_idver_user ON public.identity_verifications(user_id);
CREATE INDEX idx_idver_status ON public.identity_verifications(status);
CREATE TRIGGER trg_idver_updated_at BEFORE UPDATE ON public.identity_verifications
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
ALTER TABLE public.identity_verifications ENABLE ROW LEVEL SECURITY;

-- 4. refund_methods
CREATE TABLE public.refund_methods (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  bank text,
  account_type text,
  account_number text,
  account_holder text,
  rut text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_refund_user ON public.refund_methods(user_id);
CREATE TRIGGER trg_refund_updated_at BEFORE UPDATE ON public.refund_methods
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
ALTER TABLE public.refund_methods ENABLE ROW LEVEL SECURITY;

-- 5. venues
CREATE TABLE public.venues (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  admin_id uuid NOT NULL REFERENCES auth.users(id),
  name text NOT NULL,
  city text,
  region text,
  comuna text,
  address text,
  description text,
  image_url text,
  phone text,
  email text,
  status public.estado_sede NOT NULL DEFAULT 'PENDIENTE_APROBACION',
  tipo public.tipo_sede NOT NULL DEFAULT 'SEDE',
  rejection_reason text CHECK (char_length(rejection_reason) <= 1000),
  instagram text,
  youtube text,
  sitio_web text,
  facebook text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_venues_status ON public.venues(status);
CREATE INDEX idx_venues_admin ON public.venues(admin_id);
CREATE TRIGGER trg_venues_updated_at BEFORE UPDATE ON public.venues
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
ALTER TABLE public.venues ENABLE ROW LEVEL SECURITY;

-- 6. rooms
CREATE TABLE public.rooms (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  venue_id uuid NOT NULL REFERENCES public.venues(id) ON DELETE CASCADE,
  name text NOT NULL,
  capacity int,
  tamano_m2 int,
  tipo_piso public.tipo_piso,
  floor_type text,
  type text,
  price_per_hour numeric(12,2),
  activa boolean NOT NULL DEFAULT true,
  has_mirrors boolean NOT NULL DEFAULT false,
  tiene_barra_ballet boolean NOT NULL DEFAULT false,
  tiene_aire_acondicionado boolean NOT NULL DEFAULT false,
  tiene_calefaccion boolean NOT NULL DEFAULT false,
  tiene_insonorizacion boolean NOT NULL DEFAULT false,
  has_sound boolean NOT NULL DEFAULT false,
  tiene_amplificacion boolean NOT NULL DEFAULT false,
  tiene_entrada_auxiliar boolean NOT NULL DEFAULT false,
  tiene_microfono boolean NOT NULL DEFAULT false,
  tiene_equipo_grabacion boolean NOT NULL DEFAULT false,
  tiene_piano boolean NOT NULL DEFAULT false,
  tiene_guitarra boolean NOT NULL DEFAULT false,
  tiene_bateria boolean NOT NULL DEFAULT false,
  equipment text,
  image_url text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_rooms_venue ON public.rooms(venue_id);
CREATE TRIGGER trg_rooms_updated_at BEFORE UPDATE ON public.rooms
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
ALTER TABLE public.rooms ENABLE ROW LEVEL SECURITY;

-- 7. venue_schedules
CREATE TABLE public.venue_schedules (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  venue_id uuid NOT NULL REFERENCES public.venues(id) ON DELETE CASCADE,
  day_of_week text NOT NULL CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
  open_time time NOT NULL,
  close_time time NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (venue_id, day_of_week)
);
CREATE INDEX idx_venue_sched_venue ON public.venue_schedules(venue_id);
CREATE TRIGGER trg_venue_sched_updated_at BEFORE UPDATE ON public.venue_schedules
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
ALTER TABLE public.venue_schedules ENABLE ROW LEVEL SECURITY;

-- 8. venue_block_configs
CREATE TABLE public.venue_block_configs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  venue_id uuid NOT NULL REFERENCES public.venues(id) ON DELETE CASCADE UNIQUE,
  block_duration_min int NOT NULL DEFAULT 60,
  gap_between_blocks_min int NOT NULL DEFAULT 15,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_block_cfg_updated_at BEFORE UPDATE ON public.venue_block_configs
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
ALTER TABLE public.venue_block_configs ENABLE ROW LEVEL SECURITY;

-- 9. room_schedule_blocks
CREATE TABLE public.room_schedule_blocks (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  room_id uuid NOT NULL REFERENCES public.rooms(id) ON DELETE CASCADE,
  start_time timestamptz NOT NULL,
  end_time timestamptz NOT NULL,
  status public.block_status NOT NULL DEFAULT 'AVAILABLE',
  class_id uuid,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_rsb_room_time_status ON public.room_schedule_blocks(room_id, start_time, status);
CREATE INDEX idx_rsb_class ON public.room_schedule_blocks(class_id);
CREATE TRIGGER trg_rsb_updated_at BEFORE UPDATE ON public.room_schedule_blocks
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
ALTER TABLE public.room_schedule_blocks ENABLE ROW LEVEL SECURITY;

-- 10. room_maintenances
CREATE TABLE public.room_maintenances (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  room_id uuid NOT NULL REFERENCES public.rooms(id) ON DELETE CASCADE,
  start_time timestamptz NOT NULL,
  end_time timestamptz NOT NULL,
  reason text CHECK (char_length(reason) <= 500),
  created_by uuid REFERENCES auth.users(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_maint_room ON public.room_maintenances(room_id);
CREATE TRIGGER trg_maint_updated_at BEFORE UPDATE ON public.room_maintenances
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
ALTER TABLE public.room_maintenances ENABLE ROW LEVEL SECURITY;

-- 11. classes
CREATE TABLE public.classes (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  title text NOT NULL,
  discipline text NOT NULL,
  discipline_category text NOT NULL,
  level public.nivel_clase NOT NULL,
  description text CHECK (char_length(description) <= 2000),
  capacity int NOT NULL,
  duration int NOT NULL,
  price numeric(12,2) NOT NULL,
  min_age int,
  max_age int,
  start_time timestamptz,
  end_time timestamptz,
  room_id uuid REFERENCES public.rooms(id),
  teacher_id uuid NOT NULL REFERENCES auth.users(id),
  status public.class_status NOT NULL DEFAULT 'DRAFT',
  tipo_clase public.tipo_clase NOT NULL DEFAULT 'PROPIA',
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_classes_status ON public.classes(status);
CREATE INDEX idx_classes_teacher ON public.classes(teacher_id);
CREATE INDEX idx_classes_status_time ON public.classes(status, end_time) WHERE status = 'PUBLISHED';
CREATE INDEX idx_classes_tipo ON public.classes(tipo_clase);
CREATE INDEX idx_classes_room ON public.classes(room_id);
CREATE TRIGGER trg_classes_updated_at BEFORE UPDATE ON public.classes
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_classes_status BEFORE UPDATE ON public.classes
  FOR EACH ROW EXECUTE FUNCTION public.track_class_status();
ALTER TABLE public.classes ENABLE ROW LEVEL SECURITY;

-- 12. class_status_history
CREATE TABLE public.class_status_history (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  class_id uuid NOT NULL REFERENCES public.classes(id) ON DELETE CASCADE,
  previous_status text NOT NULL,
  new_status text NOT NULL,
  changed_by uuid REFERENCES auth.users(id),
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_csh_class ON public.class_status_history(class_id);
ALTER TABLE public.class_status_history ENABLE ROW LEVEL SECURITY;

-- 13. discipline_catalog
CREATE TABLE public.discipline_catalog (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name text NOT NULL,
  category text NOT NULL,
  active boolean NOT NULL DEFAULT true,
  sort_order int NOT NULL DEFAULT 0,
  UNIQUE (name, category)
);
ALTER TABLE public.discipline_catalog ENABLE ROW LEVEL SECURITY;

-- 14. cart_items
CREATE TABLE public.cart_items (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  class_id uuid NOT NULL,
  class_title text,
  discipline text,
  level text,
  price numeric(12,2),
  beneficiary_type text NOT NULL DEFAULT 'SELF',
  beneficiary_id uuid,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_cart_owner ON public.cart_items(owner_id);
CREATE TRIGGER trg_cart_updated_at BEFORE UPDATE ON public.cart_items
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
ALTER TABLE public.cart_items ENABLE ROW LEVEL SECURITY;

-- 15. payment_sessions
CREATE TABLE public.payment_sessions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id uuid NOT NULL REFERENCES auth.users(id),
  external_reference text NOT NULL UNIQUE,
  preference_id text UNIQUE,
  cart_snapshot jsonb NOT NULL DEFAULT '{}'::jsonb,
  status public.payment_session_status NOT NULL DEFAULT 'PENDING',
  mercado_pago_payment_id text,
  created_at timestamptz NOT NULL DEFAULT now(),
  processed_at timestamptz
);
CREATE INDEX idx_psession_owner ON public.payment_sessions(owner_id);
CREATE INDEX idx_psession_extref ON public.payment_sessions(external_reference);
ALTER TABLE public.payment_sessions ENABLE ROW LEVEL SECURITY;

-- 16. enrollments
CREATE TABLE public.enrollments (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  class_id uuid NOT NULL REFERENCES public.classes(id) ON DELETE CASCADE,
  student_id uuid NOT NULL REFERENCES auth.users(id),
  beneficiary_type text NOT NULL DEFAULT 'SELF',
  beneficiary_id uuid,
  status text NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','CANCELLED')),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (class_id, student_id)
);
CREATE INDEX idx_enrollment_class ON public.enrollments(class_id);
CREATE INDEX idx_enrollment_student ON public.enrollments(student_id);
CREATE TRIGGER trg_enrollment_updated_at BEFORE UPDATE ON public.enrollments
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
ALTER TABLE public.enrollments ENABLE ROW LEVEL SECURITY;

-- 17. payments
CREATE TABLE public.payments (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  enrollment_id uuid NOT NULL REFERENCES public.enrollments(id) ON DELETE CASCADE,
  amount numeric(12,2) NOT NULL,
  status public.payment_status NOT NULL DEFAULT 'RETAINED',
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_payment_status ON public.payments(status);
CREATE INDEX idx_payment_enrollment ON public.payments(enrollment_id);
CREATE TRIGGER trg_payments_updated_at BEFORE UPDATE ON public.payments
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
ALTER TABLE public.payments ENABLE ROW LEVEL SECURITY;

-- 18. reschedules
CREATE TABLE public.reschedules (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  class_id uuid NOT NULL REFERENCES public.classes(id) ON DELETE CASCADE,
  teacher_id uuid NOT NULL REFERENCES auth.users(id),
  proposed_time timestamptz NOT NULL,
  reason text,
  status public.reschedule_status NOT NULL DEFAULT 'PROPOSED',
  response_deadline timestamptz,
  new_class_id uuid REFERENCES public.classes(id),
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_resched_class ON public.reschedules(class_id);
CREATE INDEX idx_resched_teacher ON public.reschedules(teacher_id);
CREATE INDEX idx_resched_deadline ON public.reschedules(status, response_deadline)
  WHERE status = 'TEACHER_ACCEPTED';
ALTER TABLE public.reschedules ENABLE ROW LEVEL SECURITY;

-- 19. reschedule_responses
CREATE TABLE public.reschedule_responses (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  reschedule_id uuid NOT NULL REFERENCES public.reschedules(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES auth.users(id),
  response_type public.response_type,
  responded_at timestamptz
);
CREATE INDEX idx_resprep_resched ON public.reschedule_responses(reschedule_id);
CREATE INDEX idx_resprep_user ON public.reschedule_responses(user_id);
ALTER TABLE public.reschedule_responses ENABLE ROW LEVEL SECURITY;

-- 20. notifications
CREATE TABLE public.notifications (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  title text NOT NULL,
  message text NOT NULL,
  type text,
  read boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_notif_user ON public.notifications(user_id);
CREATE INDEX idx_notif_user_unread ON public.notifications(user_id, read) WHERE read = false;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

-- 21. reviews
CREATE TABLE public.reviews (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  class_id uuid REFERENCES public.classes(id) ON DELETE SET NULL,
  reviewer_id uuid NOT NULL REFERENCES auth.users(id),
  target_type public.review_target_type NOT NULL,
  target_id uuid NOT NULL,
  score int NOT NULL CHECK (score BETWEEN 1 AND 5),
  comment text,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_reviews_class ON public.reviews(class_id);
CREATE INDEX idx_reviews_reviewer ON public.reviews(reviewer_id);
CREATE INDEX idx_reviews_target ON public.reviews(target_type, target_id);
ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY;

-- 22. attendances
CREATE TABLE public.attendances (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  class_id uuid NOT NULL REFERENCES public.classes(id) ON DELETE CASCADE,
  beneficiary_id uuid NOT NULL REFERENCES auth.users(id),
  beneficiary_type text NOT NULL DEFAULT 'SELF',
  present boolean NOT NULL,
  marked_by text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_att_class ON public.attendances(class_id);
CREATE INDEX idx_att_benef ON public.attendances(beneficiary_id);
CREATE TRIGGER trg_att_updated_at BEFORE UPDATE ON public.attendances
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
ALTER TABLE public.attendances ENABLE ROW LEVEL SECURITY;

-- 23. associates
CREATE TABLE public.associates (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  email text,
  name text NOT NULL,
  relationship text,
  birth_date date,
  rut text,
  status text NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE')),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_assoc_owner ON public.associates(owner_id);
CREATE TRIGGER trg_assoc_updated_at BEFORE UPDATE ON public.associates
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
ALTER TABLE public.associates ENABLE ROW LEVEL SECURITY;

-- 24. venue_photos
CREATE TABLE public.venue_photos (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id uuid NOT NULL,
  owner_type text NOT NULL DEFAULT 'VENUE' CHECK (owner_type IN ('VENUE','ROOM')),
  photo_url text NOT NULL CHECK (char_length(photo_url) <= 1000),
  alt_text text,
  display_order int NOT NULL DEFAULT 0,
  principal boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_vphoto_owner ON public.venue_photos(owner_id, owner_type);
CREATE TRIGGER trg_vphoto_updated_at BEFORE UPDATE ON public.venue_photos
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
ALTER TABLE public.venue_photos ENABLE ROW LEVEL SECURITY;

-- 25. venue_documents
CREATE TABLE public.venue_documents (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  venue_id uuid NOT NULL REFERENCES public.venues(id) ON DELETE CASCADE,
  file_url text NOT NULL CHECK (char_length(file_url) <= 1000),
  tipo public.tipo_documento_sede NOT NULL,
  nombre text,
  tipo_archivo text,
  estado text NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE','APROBADO','RECHAZADO')),
  motivo_rechazo text CHECK (char_length(motivo_rechazo) <= 500),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_vdoc_venue ON public.venue_documents(venue_id);
CREATE TRIGGER trg_vdoc_updated_at BEFORE UPDATE ON public.venue_documents
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
ALTER TABLE public.venue_documents ENABLE ROW LEVEL SECURITY;

-- 26. audit_logs
CREATE TABLE public.audit_logs (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  actor_id uuid REFERENCES auth.users(id),
  action text NOT NULL,
  resource_type text NOT NULL,
  resource_id text NOT NULL,
  old_values jsonb,
  new_values jsonb,
  metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
  ip inet,
  user_agent text,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_actor ON public.audit_logs(actor_id, created_at DESC);
CREATE INDEX idx_audit_action ON public.audit_logs(action, created_at DESC);
CREATE INDEX idx_audit_resource ON public.audit_logs(resource_type, resource_id);
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;

-- 27. system_metrics
CREATE TABLE public.system_metrics (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  metric_name text NOT NULL,
  metric_value numeric NOT NULL,
  labels jsonb NOT NULL DEFAULT '{}'::jsonb,
  recorded_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_sysmet_name_time ON public.system_metrics(metric_name, recorded_at DESC);
ALTER TABLE public.system_metrics ENABLE ROW LEVEL SECURITY;
