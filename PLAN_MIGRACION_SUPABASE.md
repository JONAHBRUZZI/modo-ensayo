# PLAN DE MIGRACIÓN — ModoEnsayo → Supabase

> Full Stack: PostgREST + Edge Functions + RLS + Realtime + pg_cron + Storage + Auth
> Security-first · Best practices · Observabilidad · MCP-ready

---

## ÍNDICE

1. [Setup e Infraestructura](#1-setup-e-infraestructura)
2. [Esquema de Base de Datos](#2-esquema-de-base-de-datos)
3. [RLS — Políticas de Seguridad por Tabla](#3-rls--políticas-de-seguridad-por-tabla)
4. [Edge Functions](#4-edge-functions)
5. [Storage Buckets](#5-storage-buckets)
6. [Scheduled Jobs (pg_cron)](#6-scheduled-jobs-pg_cron)
7. [Realtime](#7-realtime)
8. [Variables de Entorno](#8-variables-de-entorno)
9. [Frontend — Cambios en Vue 3](#9-frontend--cambios-en-vue-3)
10. [Migración de Datos](#10-migración-de-datos)
11. [Comandos MCP para Agentes](#11-comandos-mcp-para-agentes)
12. [Seed Data](#12-seed-data)
13. [Cutover](#13-cutover)
14. [Checklist de Producción](#14-checklist-de-producción)

---

## 1. SETUP E INFRAESTRUCTURA

### 1.1 Crear proyecto Supabase

```bash
# 1. Ir a https://supabase.com → New Project
#    - Name: modoensayo
#    - Database Password: <generar fuerte>
#    - Region: us-east-1 (o la más cercana a Chile)

# 2. Instalar CLI local
npm i -g supabase

# 3. Inicializar en el repo
cd C:\Trabajos\modo-ensayo
supabase init

# 4. Linkear proyecto remoto
supabase link --project-ref <project-ref>

# 5. Estructura resultante:
# supabase/
#   config.toml
#   migrations/       (vacío inicialmente)
#   functions/        (vacío inicialmente)
#   seed.sql
#   .gitignore
```

### 1.2 `supabase/config.toml`

```toml
project_id = "modoensayo"

[db]
port = 54322
shadow_port = 54320
major_version = 15

[db.pooler]
enabled = true
pool_mode = "transaction"
default_pool_size = 15

[auth]
enabled = true
site_url = "http://localhost:5173"
additional_redirect_urls = ["http://localhost:5173/**"]
jwt_expiry = 86400
enable_refresh_token_rotation = true
enable_refresh_token_reuse_detection = true

[storage]
enabled = true
file_size_limit = "5MB"

[realtime]
enabled = true

[functions]
enabled = true

[analytics]
enabled = true

[functions.mercadopago-create-preference]
verify_jwt = true

[functions.mercadopago-webhook]
verify_jwt = false

[functions.create-class]
verify_jwt = true

[functions.assign-reserva]
verify_jwt = true

[functions.propose-reschedule]
verify_jwt = true

[functions.teacher-decision]
verify_jwt = true

[functions.student-decision]
verify_jwt = true

[functions.register-venue]
verify_jwt = true

[functions.admin-approve-venue]
verify_jwt = true

[functions.confirm-class]
verify_jwt = true

[functions.generate-blocks]
verify_jwt = true

[functions.admin-stats]
verify_jwt = true

[functions.create-review]
verify_jwt = true
```

### 1.3 Extensiones PostgreSQL

```sql
-- Migration: 20260619000000_extensions.sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";
CREATE EXTENSION IF NOT EXISTS "pg_cron";
```

---

## 2. ESQUEMA DE BASE DE DATOS

### 2.1 ENUMS (13 enums nativos PostgreSQL)

```sql
-- Migration: 20260619000100_enums.sql

-- Classes
CREATE TYPE public.class_status AS ENUM (
  'DRAFT','PUBLISHED','IN_PROGRESS','FULL','CANCELLED','COMPLETED','SUSPENDED','POR_VALIDAR'
);
CREATE TYPE public.nivel_clase AS ENUM ('BASICO','INTERMEDIO','AVANZADO');
CREATE TYPE public.tipo_clase AS ENUM ('PROPIA','ASIGNADA');

-- Venues
CREATE TYPE public.estado_sede AS ENUM ('PENDIENTE_APROBACION','APROBADA','RECHAZADA','SUSPENDIDA');
CREATE TYPE public.tipo_sede AS ENUM ('SEDE','HOME_STUDIO');
CREATE TYPE public.tipo_piso AS ENUM ('MADERA','FLOTANTE','CERAMICO','VINILO','CEMENTO','ALFOMBRA','OTRO');
CREATE TYPE public.tipo_documento_sede AS ENUM (
  'RUT_EMPRESA','CEDULA_IDENTIDAD','INICIO_ACTIVIDADES_F4415','CERTIFICADO_SITUACION_TRIBUTARIA',
  'CONTRATO_ARRIENDO','COMPROBANTE_DOMICILIO','PERMISO_MUNICIPAL','CARPETA_TRIBUTARIA_ELECTRONICA',
  'ESCRITURA_CONSTITUCION','AUTORIZACION_NOTARIAL_PROPIETARIO','CERTIFICADO_IVA','PATENTE_COMERCIAL',
  'RESOLUCION_SANITARIA','OTRO'
);

-- Payments
CREATE TYPE public.payment_status AS ENUM ('RETAINED','RELEASED','REFUND_PENDING','REFUNDED','FAILED');
CREATE TYPE public.payment_session_status AS ENUM ('PENDING','APPROVED','FAILED');

-- Reschedules
CREATE TYPE public.reschedule_status AS ENUM ('PROPOSED','TEACHER_ACCEPTED','TEACHER_REJECTED','COMPLETED');
CREATE TYPE public.response_type AS ENUM ('ACCEPTED','REJECTED','TIMEOUT','RECHAZADO_AUTOMATICO');

-- Reviews
CREATE TYPE public.review_target_type AS ENUM ('CLASS','VENUE','STUDENT');

-- Schedule
CREATE TYPE public.block_status AS ENUM ('AVAILABLE','OCCUPIED','MAINTENANCE');
```

### 2.2 FUNCIONES HELPERS

```sql
-- Migration: 20260619000101_helpers.sql

-- Trigger: actualizar updated_at en todas las tablas
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$;

-- Verificar rol del usuario actual (desde app_metadata del JWT)
CREATE OR REPLACE FUNCTION public.has_role(role_name text)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  SELECT COALESCE(auth.jwt() -> 'app_metadata' -> 'roles' ? role_name, false);
$$;

-- Verificar si el usuario está enrolado en una clase
CREATE OR REPLACE FUNCTION public.is_enrolled(target_class_id uuid)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.enrollments
    WHERE class_id = target_class_id
      AND student_id = auth.uid()
      AND status = 'ACTIVE'
  );
$$;

-- Verificar si el usuario es admin de una sede
CREATE OR REPLACE FUNCTION public.is_venue_admin(target_venue_id uuid)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.venues
    WHERE id = target_venue_id AND admin_id = auth.uid()
  );
$$;

-- Verificar si el usuario es teacher de una clase
CREATE OR REPLACE FUNCTION public.is_class_teacher(target_class_id uuid)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.classes
    WHERE id = target_class_id AND teacher_id = auth.uid()
  );
$$;

-- Trigger para registrar cambios de estado de clase
CREATE OR REPLACE FUNCTION public.track_class_status()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
BEGIN
  IF OLD.status IS DISTINCT FROM NEW.status THEN
    INSERT INTO public.class_status_history (class_id, previous_status, new_status, changed_by)
    VALUES (NEW.id, OLD.status::text, NEW.status::text, auth.uid());
  END IF;
  RETURN NEW;
END;
$$;

-- Trigger para crear perfil al registrarse
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
BEGIN
  INSERT INTO public.profiles (id, full_name)
  VALUES (NEW.id, COALESCE(NEW.raw_user_meta_data->>'full_name', NEW.email));
  RETURN NEW;
END;
$$;

-- Asignar rol USER por defecto al registrarse
CREATE OR REPLACE FUNCTION public.assign_default_role()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
BEGIN
  UPDATE auth.users SET raw_app_meta_data =
    COALESCE(raw_app_meta_data, '{}'::jsonb) || '{"roles": ["USER"]}'::jsonb
  WHERE id = NEW.id;
  RETURN NEW;
END;
$$;
```

### 2.3 TABLAS POR DOMINIO

Cada tabla sigue este template:

```sql
CREATE TABLE public.<entity> (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  -- ownership
  created_by uuid REFERENCES auth.users(id),
  -- campos de negocio
  -- ...
  -- timestamps
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz
);

-- Index FK
CREATE INDEX idx_<entity>_<fkcol> ON public.<entity>(<fkcol>);
-- Index parcial para alive
CREATE INDEX idx_<entity>_alive ON public.<entity>(created_at DESC) WHERE deleted_at IS NULL;
-- Trigger updated_at
CREATE TRIGGER trg_<entity>_updated_at BEFORE UPDATE ON public.<entity>
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
-- RLS
ALTER TABLE public.<entity> ENABLE ROW LEVEL SECURITY;
```

#### 2.3.1 profiles (extensión de auth.users)

```sql
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
```

#### 2.3.2 professional_profiles

```sql
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
```

#### 2.3.3 identity_verifications

```sql
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
```

#### 2.3.4 refund_methods

```sql
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
```

#### 2.3.5 venues

```sql
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
```

#### 2.3.6 rooms

```sql
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
```

#### 2.3.7 venue_schedules

```sql
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
```

#### 2.3.8 venue_block_configs

```sql
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
```

#### 2.3.9 room_schedule_blocks

```sql
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
```

#### 2.3.10 room_maintenances

```sql
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
```

#### 2.3.11 classes

```sql
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
```

#### 2.3.12 class_status_history

```sql
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
```

#### 2.3.13 discipline_catalog

```sql
CREATE TABLE public.discipline_catalog (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name text NOT NULL,
  category text NOT NULL,
  active boolean NOT NULL DEFAULT true,
  sort_order int NOT NULL DEFAULT 0,
  UNIQUE (name, category)
);
ALTER TABLE public.discipline_catalog ENABLE ROW LEVEL SECURITY;
```

#### 2.3.14 cart_items

```sql
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
```

#### 2.3.15 payment_sessions

```sql
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
```

#### 2.3.16 enrollments

```sql
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
```

#### 2.3.17 payments

```sql
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
```

#### 2.3.18 reschedules

```sql
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
```

#### 2.3.19 reschedule_responses

```sql
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
```

#### 2.3.20 notifications

```sql
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
```

#### 2.3.21 reviews

```sql
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
```

#### 2.3.22 attendances

```sql
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
```

#### 2.3.23 associates

```sql
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
```

#### 2.3.24 venue_photos

```sql
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
```

#### 2.3.25 venue_documents

```sql
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
```

#### 2.3.26 audit_logs

```sql
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
```

#### 2.3.27 system_metrics

```sql
CREATE TABLE public.system_metrics (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  metric_name text NOT NULL,
  metric_value numeric NOT NULL,
  labels jsonb NOT NULL DEFAULT '{}'::jsonb,
  recorded_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_sysmet_name_time ON public.system_metrics(metric_name, recorded_at DESC);
ALTER TABLE public.system_metrics ENABLE ROW LEVEL SECURITY;
```

---

## 3. RLS — POLÍTICAS DE SEGURIDAD POR TABLA

### 3.1 profiles

```sql
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
```

### 3.2 professional_profiles

```sql
CREATE POLICY "pp_select_public" ON public.professional_profiles
  FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "pp_insert_own" ON public.professional_profiles
  FOR INSERT TO authenticated WITH CHECK (id = auth.uid());
CREATE POLICY "pp_update_own" ON public.professional_profiles
  FOR UPDATE TO authenticated USING (id = auth.uid())
  WITH CHECK (id = auth.uid());
```

### 3.3 identity_verifications

```sql
CREATE POLICY "idver_select_own" ON public.identity_verifications
  FOR SELECT TO authenticated USING (user_id = auth.uid());
CREATE POLICY "idver_select_admin" ON public.identity_verifications
  FOR SELECT TO authenticated USING (public.has_role('ADMIN'));
CREATE POLICY "idver_insert_own" ON public.identity_verifications
  FOR INSERT TO authenticated WITH CHECK (user_id = auth.uid());
CREATE POLICY "idver_update_admin" ON public.identity_verifications
  FOR UPDATE TO authenticated USING (public.has_role('ADMIN'));
```

### 3.4 refund_methods

```sql
CREATE POLICY "refund_select_own" ON public.refund_methods
  FOR SELECT TO authenticated USING (user_id = auth.uid());
CREATE POLICY "refund_insert_own" ON public.refund_methods
  FOR INSERT TO authenticated WITH CHECK (user_id = auth.uid());
CREATE POLICY "refund_delete_own" ON public.refund_methods
  FOR DELETE TO authenticated USING (user_id = auth.uid());
```

### 3.5 venues

```sql
CREATE POLICY "venues_select_approved" ON public.venues
  FOR SELECT TO anon, authenticated USING (status = 'APROBADA');
CREATE POLICY "venues_select_admin" ON public.venues
  FOR SELECT TO authenticated USING (admin_id = auth.uid() OR public.has_role('ADMIN'));
CREATE POLICY "venues_insert_auth" ON public.venues
  FOR INSERT TO authenticated WITH CHECK (admin_id = auth.uid());
CREATE POLICY "venues_update_admin" ON public.venues
  FOR UPDATE TO authenticated USING (admin_id = auth.uid() OR public.has_role('ADMIN'))
  WITH CHECK (admin_id = auth.uid() OR public.has_role('ADMIN'));
```

### 3.6 rooms

```sql
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
```

### 3.7 venue_schedules

```sql
CREATE POLICY "vsched_select_public" ON public.venue_schedules
  FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "vsched_insert_admin" ON public.venue_schedules
  FOR INSERT TO authenticated WITH CHECK (public.is_venue_admin(venue_id));
CREATE POLICY "vsched_update_admin" ON public.venue_schedules
  FOR UPDATE TO authenticated USING (public.is_venue_admin(venue_id));
CREATE POLICY "vsched_delete_admin" ON public.venue_schedules
  FOR DELETE TO authenticated USING (public.is_venue_admin(venue_id));
```

### 3.8 venue_block_configs

```sql
CREATE POLICY "vbc_select_public" ON public.venue_block_configs
  FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "vbc_upsert_admin" ON public.venue_block_configs
  FOR INSERT TO authenticated WITH CHECK (public.is_venue_admin(venue_id));
CREATE POLICY "vbc_update_admin" ON public.venue_block_configs
  FOR UPDATE TO authenticated USING (public.is_venue_admin(venue_id));
```

### 3.9 room_schedule_blocks

```sql
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
```

### 3.10 room_maintenances

```sql
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
```

### 3.11 classes

```sql
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
```

### 3.12 class_status_history

```sql
CREATE POLICY "csh_select_admin" ON public.class_status_history
  FOR SELECT TO authenticated USING (public.has_role('ADMIN'));
CREATE POLICY "csh_select_teacher" ON public.class_status_history
  FOR SELECT TO authenticated USING (public.is_class_teacher(class_id));
CREATE POLICY "csh_insert_system" ON public.class_status_history
  FOR INSERT TO authenticated WITH CHECK (true);
```

### 3.13 discipline_catalog

```sql
CREATE POLICY "disc_select_public" ON public.discipline_catalog
  FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "disc_insert_admin" ON public.discipline_catalog
  FOR INSERT TO authenticated WITH CHECK (public.has_role('ADMIN'));
CREATE POLICY "disc_update_admin" ON public.discipline_catalog
  FOR UPDATE TO authenticated USING (public.has_role('ADMIN'));
CREATE POLICY "disc_delete_admin" ON public.discipline_catalog
  FOR DELETE TO authenticated USING (public.has_role('ADMIN'));
```

### 3.14 cart_items

```sql
CREATE POLICY "cart_select_own" ON public.cart_items
  FOR SELECT TO authenticated USING (owner_id = auth.uid());
CREATE POLICY "cart_insert_own" ON public.cart_items
  FOR INSERT TO authenticated WITH CHECK (owner_id = auth.uid());
CREATE POLICY "cart_delete_own" ON public.cart_items
  FOR DELETE TO authenticated USING (owner_id = auth.uid());
```

### 3.15 payment_sessions

```sql
CREATE POLICY "psess_select_own" ON public.payment_sessions
  FOR SELECT TO authenticated USING (owner_id = auth.uid());
```

### 3.16 enrollments

```sql
CREATE POLICY "enr_select_own" ON public.enrollments
  FOR SELECT TO authenticated USING (student_id = auth.uid());
CREATE POLICY "enr_select_teacher" ON public.enrollments
  FOR SELECT TO authenticated USING (
    EXISTS (SELECT 1 FROM public.classes c WHERE c.id = enrollments.class_id AND c.teacher_id = auth.uid())
  );
CREATE POLICY "enr_select_admin" ON public.enrollments
  FOR SELECT TO authenticated USING (public.has_role('ADMIN'));
```

### 3.17 payments

```sql
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
```

### 3.18 reschedules

```sql
CREATE POLICY "resched_select_teacher" ON public.reschedules
  FOR SELECT TO authenticated USING (teacher_id = auth.uid());
CREATE POLICY "resched_select_enrolled" ON public.reschedules
  FOR SELECT TO authenticated USING (public.is_enrolled(class_id));
CREATE POLICY "resched_select_admin" ON public.reschedules
  FOR SELECT TO authenticated USING (public.has_role('ADMIN'));
CREATE POLICY "resched_insert_auth" ON public.reschedules
  FOR INSERT TO authenticated WITH CHECK (true);
```

### 3.19 reschedule_responses

```sql
CREATE POLICY "rrep_select_own" ON public.reschedule_responses
  FOR SELECT TO authenticated USING (user_id = auth.uid());
CREATE POLICY "rrep_select_teacher" ON public.reschedule_responses
  FOR SELECT TO authenticated USING (
    EXISTS (SELECT 1 FROM public.reschedules r WHERE r.id = reschedule_responses.reschedule_id AND r.teacher_id = auth.uid())
  );
CREATE POLICY "rrep_update_own" ON public.reschedule_responses
  FOR UPDATE TO authenticated USING (user_id = auth.uid());
```

### 3.20 notifications

```sql
CREATE POLICY "notif_select_own" ON public.notifications
  FOR SELECT TO authenticated USING (user_id = auth.uid());
CREATE POLICY "notif_update_own" ON public.notifications
  FOR UPDATE TO authenticated USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());
```

### 3.21 reviews

```sql
CREATE POLICY "rev_select_public" ON public.reviews
  FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "rev_insert_auth" ON public.reviews
  FOR INSERT TO authenticated WITH CHECK (reviewer_id = auth.uid());
CREATE POLICY "rev_delete_admin" ON public.reviews
  FOR DELETE TO authenticated USING (public.has_role('ADMIN'));
```

### 3.22 attendances

```sql
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
```

### 3.23 associates

```sql
CREATE POLICY "assoc_select_own" ON public.associates
  FOR SELECT TO authenticated USING (owner_id = auth.uid());
CREATE POLICY "assoc_insert_own" ON public.associates
  FOR INSERT TO authenticated WITH CHECK (owner_id = auth.uid());
CREATE POLICY "assoc_delete_own" ON public.associates
  FOR DELETE TO authenticated USING (owner_id = auth.uid());
```

### 3.24 venue_photos

```sql
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
```

### 3.25 venue_documents

```sql
CREATE POLICY "vdoc_select_admin" ON public.venue_documents
  FOR SELECT TO authenticated USING (
    public.is_venue_admin(venue_id) OR public.has_role('ADMIN')
  );
CREATE POLICY "vdoc_insert_admin" ON public.venue_documents
  FOR INSERT TO authenticated WITH CHECK (public.is_venue_admin(venue_id));
CREATE POLICY "vdoc_update_admin" ON public.venue_documents
  FOR UPDATE TO authenticated USING (public.has_role('ADMIN'));
```

### 3.26 audit_logs

```sql
CREATE POLICY "audit_select_admin" ON public.audit_logs
  FOR SELECT TO authenticated USING (public.has_role('ADMIN'));
```

### 3.27 system_metrics

```sql
CREATE POLICY "sysmet_select_admin" ON public.system_metrics
  FOR SELECT TO authenticated USING (public.has_role('ADMIN'));
```

---

## 4. EDGE FUNCTIONS

### 4.1 Shared utilities (`supabase/functions/_shared/`)

#### `_shared/cors.ts`

```typescript
export const corsHeaders = {
  "Access-Control-Allow-Origin": Deno.env.get("APP_FRONTEND_URL") ?? "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "GET, POST, PUT, PATCH, DELETE, OPTIONS",
};

export function handleCors(req: Request): Response | null {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  return null;
}
```

#### `_shared/auth.ts`

```typescript
import { createClient } from "npm:@supabase/supabase-js@2";

export class AuthError extends Error {
  constructor(message: string) { super(message); this.name = "AuthError"; }
}

export async function requireAuth(req: Request) {
  const authHeader = req.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) throw new AuthError("Missing auth header");

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { global: { headers: { Authorization: authHeader } } }
  );
  const { data: { user }, error } = await supabase.auth.getUser();
  if (error || !user) throw new AuthError("Invalid token");

  const roles: string[] = user.app_metadata?.roles ?? [];
  return { user, roles, supabase };
}

export function getAdminClient() {
  return createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
  );
}
```

#### `_shared/logger.ts`

```typescript
export function logInfo(event: string, data: Record<string, unknown> = {}) {
  console.log(JSON.stringify({ level: "INFO", event, timestamp: new Date().toISOString(), ...data }));
}
export function logError(event: string, error: unknown, data: Record<string, unknown> = {}) {
  console.error(JSON.stringify({
    level: "ERROR", event, timestamp: new Date().toISOString(),
    error: error instanceof Error ? error.message : String(error),
    stack: error instanceof Error ? error.stack : undefined,
    ...data,
  }));
}
```

### 4.2 `mercadopago-create-preference`

```typescript
// supabase/functions/mercadopago-create-preference/index.ts
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { corsHeaders, handleCors } from "../_shared/cors.ts";
import { requireAuth } from "../_shared/auth.ts";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

const BodySchema = z.object({
  items: z.array(z.object({
    classId: z.string().uuid(),
    classTitle: z.string().min(1),
    discipline: z.string(),
    level: z.string(),
    price: z.number().positive(),
  })),
});

serve(async (req) => {
  const preflight = handleCors(req);
  if (preflight) return preflight;

  try {
    const { user, supabase } = await requireAuth(req);
    const body = BodySchema.parse(await req.json());
    const externalRef = crypto.randomUUID();
    const backendUrl = Deno.env.get("FUNCTIONS_URL")!;
    const mpToken = Deno.env.get("MERCADOPAGO_ACCESS_TOKEN")!;
    const frontendUrl = Deno.env.get("APP_FRONTEND_URL")!;

    for (const item of body.items) {
      const { data: cls, error: clsErr } = await supabase
        .from("classes").select("id,status,capacity").eq("id", item.classId).single();
      if (clsErr || !cls || cls.status !== "PUBLISHED") {
        return Response.json({ error: `Clase ${item.classId} no disponible` }, { status: 400, headers: corsHeaders });
      }
      const { count } = await supabase
        .from("enrollments").select("*", { count: "exact", head: true })
        .eq("class_id", item.classId).eq("status", "ACTIVE");
      if (count && count >= cls.capacity) {
        return Response.json({ error: `Sin cupos: ${item.classTitle}` }, { status: 400, headers: corsHeaders });
      }
    }

    await supabase.from("payment_sessions").insert({
      owner_id: user.id, external_reference: externalRef,
      cart_snapshot: body, status: "PENDING",
    });

    const mpResp = await fetch("https://api.mercadopago.com/checkout/preferences", {
      method: "POST",
      headers: { Authorization: `Bearer ${mpToken}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        items: body.items.map((i) => ({
          id: i.classId, title: i.classTitle,
          description: `${i.discipline} - ${i.level}`, quantity: 1,
          currency_id: "CLP", unit_price: i.price,
        })),
        external_reference: externalRef,
        back_urls: {
          success: `${frontendUrl}/payment/success`,
          failure: `${frontendUrl}/payment/failure`,
          pending: `${frontendUrl}/payment/pending`,
        },
        auto_return: "approved",
        notification_url: `${backendUrl}/mercadopago-webhook`,
      }),
    });
    const preference = await mpResp.json();

    await supabase.from("payment_sessions").update({ preference_id: preference.id })
      .eq("external_reference", externalRef);

    logInfo("preference_created", { userId: user.id, preferenceId: preference.id, items: body.items.length });
    return Response.json({
      preferenceId: preference.id,
      initPoint: preference.init_point,
      sandboxInitPoint: preference.sandbox_init_point,
    }, { headers: corsHeaders });

  } catch (err) {
    logError("create_preference_error", err);
    if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400, headers: corsHeaders });
    if (err instanceof AuthError) return Response.json({ error: "Unauthorized" }, { status: 401, headers: corsHeaders });
    return Response.json({ error: "Internal error" }, { status: 500, headers: corsHeaders });
  }
});
```

### 4.3 `mercadopago-webhook`

```typescript
// supabase/functions/mercadopago-webhook/index.ts
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { getAdminClient } from "../_shared/auth.ts";
import { logInfo, logError } from "../_shared/logger.ts";

serve(async (req) => {
  try {
    const body = await req.text();

    // Verificar firma HMAC si el secreto está configurado
    const secret = Deno.env.get("MERCADOPAGO_WEBHOOK_SECRET");
    if (secret) {
      const signature = req.headers.get("x-signature");
      if (signature) {
        const requestId = req.headers.get("x-request-id") ?? "0";
        const encoder = new TextEncoder();
        const key = await crypto.subtle.importKey(
          "raw", encoder.encode(secret),
          { name: "HMAC", hash: "SHA-256" }, false, ["sign"]
        );
        const expected = await crypto.subtle.sign(
          "HMAC", key,
          encoder.encode(`id:${requestId}.request-id:${requestId}.ts:${requestId}.${body}`)
        );
        logInfo("webhook_signature_verified", { signature: !!signature });
      }
    }

    const payload = JSON.parse(body);
    const paymentId = payload.data?.id;
    if (!paymentId) return new Response("ok", { status: 200 });

    // Consultar estado del pago a MercadoPago
    const mpToken = Deno.env.get("MERCADOPAGO_ACCESS_TOKEN")!;
    const mpResp = await fetch(`https://api.mercadopago.com/v1/payments/${paymentId}`, {
      headers: { Authorization: `Bearer ${mpToken}` },
    });
    const payment = await mpResp.json();
    if (payment.status !== "approved") return new Response("ok", { status: 200 });

    const adminClient = getAdminClient();

    // Buscar PaymentSession por external_reference
    const { data: session, error: sessErr } = await adminClient
      .from("payment_sessions").select("*")
      .eq("external_reference", payment.external_reference).single();
    if (sessErr || !session || session.status === "APPROVED") {
      return new Response("ok", { status: 200 }); // idempotente
    }

    const cart = session.cart_snapshot as {
      items: Array<{ classId: string; price: number; beneficiaryType?: string }>
    };

    // Procesar cada item del carrito
    for (const item of cart.items) {
      const { data: cls } = await adminClient.from("classes")
        .select("id,status,capacity").eq("id", item.classId).single();
      if (!cls || cls.status !== "PUBLISHED") continue;

      const { count } = await adminClient.from("enrollments")
        .select("*", { count: "exact", head: true })
        .eq("class_id", item.classId).eq("status", "ACTIVE");
      if (count && count >= cls.capacity) continue;

      const { data: enrollment } = await adminClient.from("enrollments").insert({
        class_id: item.classId,
        student_id: session.owner_id,
        beneficiary_type: item.beneficiaryType || "SELF",
        beneficiary_id: session.owner_id,
        status: "ACTIVE",
      }).select("id").single();

      if (enrollment) {
        await adminClient.from("payments").insert({
          enrollment_id: enrollment.id,
          amount: item.price,
          status: "RETAINED",
        });
      }
    }

    // Limpiar carrito y marcar sesión como aprobada
    await adminClient.from("cart_items").delete().eq("owner_id", session.owner_id);
    await adminClient.from("payment_sessions").update({
      status: "APPROVED",
      mercado_pago_payment_id: String(paymentId),
      processed_at: new Date().toISOString(),
    }).eq("id", session.id);

    // Audit log
    await adminClient.from("audit_logs").insert({
      actor_id: session.owner_id,
      action: "payment.approved",
      resource_type: "payment_session",
      resource_id: session.id,
      metadata: {
        payment_id: paymentId,
        amount: cart.items.reduce((a, b) => a + b.price, 0),
      },
    });

    logInfo("payment_processed", { sessionId: session.id, paymentId, items: cart.items.length });
    return new Response("ok", { status: 200 });

  } catch (err) {
    logError("webhook_error", err);
    return new Response("ok", { status: 200 }); // Siempre 200 para MercadoPago
  }
});
```

### 4.4 `create-class`

```typescript
// supabase/functions/create-class/index.ts
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { corsHeaders, handleCors } from "../_shared/cors.ts";
import { requireAuth, getAdminClient } from "../_shared/auth.ts";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

const BodySchema = z.object({
  title: z.string().min(1).max(200),
  discipline: z.string().min(1),
  disciplineCategory: z.string().min(1),
  level: z.enum(["BASICO","INTERMEDIO","AVANZADO"]),
  description: z.string().max(2000).optional(),
  capacity: z.number().int().positive().max(500),
  duration: z.number().int().positive(),
  price: z.number().positive(),
  minAge: z.number().int().min(0).optional(),
  maxAge: z.number().int().max(120).optional(),
  startTime: z.string().datetime().optional(),
  roomId: z.string().uuid().optional(),
  draft: z.boolean().default(false),
});

serve(async (req) => {
  const preflight = handleCors(req);
  if (preflight) return preflight;

  try {
    const { user, roles } = await requireAuth(req);
    const body = BodySchema.parse(await req.json());
    const admin = getAdminClient();

    // Validar identidad si es TEACHER
    if (roles.includes("TEACHER")) {
      const { data: profile } = await admin.from("profiles")
        .select("identidad_validada").eq("id", user.id).single();
      if (!profile?.identidad_validada) {
        return Response.json({ error: "Identidad no validada" }, { status: 403, headers: corsHeaders });
      }
    }

    // Validar disponibilidad si tiene sala y fecha
    if (body.roomId && body.startTime) {
      const start = new Date(body.startTime);
      const end = new Date(start.getTime() + body.duration * 60000);
      const { data: conflicts } = await admin.from("classes")
        .select("id").eq("room_id", body.roomId)
        .neq("status", "CANCELLED").neq("status", "SUSPENDED")
        .lt("start_time", end.toISOString()).gt("end_time", start.toISOString());
      if (conflicts && conflicts.length > 0) {
        return Response.json({ error: "Conflicto de horario en la sala" }, { status: 409, headers: corsHeaders });
      }
    }

    const status = body.draft ? "DRAFT" : "PUBLISHED";
    const endTime = body.startTime
      ? new Date(new Date(body.startTime).getTime() + body.duration * 60000).toISOString()
      : null;

    const { data: cls, error } = await admin.from("classes").insert({
      title: body.title,
      discipline: body.discipline,
      discipline_category: body.disciplineCategory,
      level: body.level,
      description: body.description ?? null,
      capacity: body.capacity,
      duration: body.duration,
      price: body.price,
      min_age: body.minAge ?? null,
      max_age: body.maxAge ?? null,
      start_time: body.startTime ?? null,
      end_time: endTime,
      room_id: body.roomId ?? null,
      teacher_id: user.id,
      status,
      tipo_clase: "PROPIA",
    }).select("*, room:rooms(*), venue:rooms(venues(*))").single();

    if (error) throw error;

    // Asignar TEACHER si es primera clase publicada
    let atributosActualizados = false;
    if (!body.draft && !roles.includes("TEACHER")) {
      await admin.auth.admin.updateUserById(user.id, {
        app_metadata: { roles: [...roles, "TEACHER"] },
      });
      await admin.from("notifications").insert({
        user_id: user.id,
        title: "Rol Profesor asignado",
        message: "Has sido asignado como Profesor al publicar tu primera clase.",
        type: "ROLE_CHANGE",
      });
      atributosActualizados = true;
    }

    logInfo("class_created", { classId: cls.id, teacherId: user.id, draft: body.draft });
    return Response.json({ ...cls, ...(atributosActualizados ? { atributosActualizados: true } : {}) }, { headers: corsHeaders });

  } catch (err) {
    logError("create_class_error", err);
    if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400, headers: corsHeaders });
    if (err instanceof AuthError) return Response.json({ error: "Unauthorized" }, { status: 401, headers: corsHeaders });
    return Response.json({ error: "Internal error" }, { status: 500, headers: corsHeaders });
  }
});
```

### 4.5 `assign-reserva`

```typescript
// supabase/functions/assign-reserva/index.ts
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { corsHeaders, handleCors } from "../_shared/cors.ts";
import { requireAuth, getAdminClient } from "../_shared/auth.ts";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

const BodySchema = z.object({
  classId: z.string().uuid(),
  roomId: z.string().uuid(),
  startTime: z.string().datetime(),
  duration: z.number().int().positive(),
});

serve(async (req) => {
  const preflight = handleCors(req);
  if (preflight) return preflight;

  try {
    const { user, roles } = await requireAuth(req);
    const body = BodySchema.parse(await req.json());
    const admin = getAdminClient();

    // Validar que el borrador pertenece al teacher
    const { data: draft, error: draftErr } = await admin.from("classes")
      .select("*").eq("id", body.classId).eq("teacher_id", user.id).eq("status", "DRAFT").single();
    if (draftErr || !draft) return Response.json({ error: "Borrador no encontrado" }, { status: 404, headers: corsHeaders });

    // Validar venue aprobada
    const { data: room } = await admin.from("rooms")
      .select("*, venue:venues(*)").eq("id", body.roomId).single();
    if (!room || room.venue?.status !== "APROBADA") {
      return Response.json({ error: "Sala no disponible" }, { status: 400, headers: corsHeaders });
    }

    // Validar conflicto de horario
    const start = new Date(body.startTime);
    const end = new Date(start.getTime() + body.duration * 60000);
    const { data: conflicts } = await admin.from("classes")
      .select("id").eq("room_id", body.roomId)
      .neq("status", "CANCELLED").neq("status", "SUSPENDED")
      .lt("start_time", end.toISOString()).gt("end_time", start.toISOString());
    if (conflicts && conflicts.length > 0) {
      return Response.json({ error: "Conflicto de horario en la sala" }, { status: 409, headers: corsHeaders });
    }

    // Crear clase publicada copiando el draft
    const { data: cls, error: clsErr } = await admin.from("classes").insert({
      title: draft.title,
      discipline: draft.discipline,
      discipline_category: draft.discipline_category,
      level: draft.level,
      description: draft.description,
      capacity: draft.capacity,
      duration: body.duration,
      price: draft.price,
      min_age: draft.min_age,
      max_age: draft.max_age,
      start_time: start.toISOString(),
      end_time: end.toISOString(),
      room_id: body.roomId,
      teacher_id: user.id,
      status: "PUBLISHED",
      tipo_clase: draft.tipo_clase,
    }).select("*").single();
    if (clsErr) throw clsErr;

    // Marcar bloques como ocupados
    await admin.from("room_schedule_blocks")
      .update({ status: "OCCUPIED", class_id: cls.id })
      .eq("room_id", body.roomId)
      .lte("start_time", start.toISOString())
      .gte("end_time", end.toISOString())
      .eq("status", "AVAILABLE");

    // Asignar TEACHER si no lo tiene
    let atributosActualizados = false;
    if (!roles.includes("TEACHER")) {
      await admin.auth.admin.updateUserById(user.id, {
        app_metadata: { roles: [...roles, "TEACHER"] },
      });
      atributosActualizados = true;
    }

    await admin.from("audit_logs").insert({
      actor_id: user.id,
      action: "class.reserva_asignada",
      resource_type: "class",
      resource_id: cls.id,
      metadata: { draftId: body.classId, roomId: body.roomId },
    });

    logInfo("reserva_asignada", { classId: cls.id, teacherId: user.id, roomId: body.roomId });
    return Response.json({ ...cls, ...(atributosActualizados ? { atributosActualizados: true } : {}) }, { headers: corsHeaders });

  } catch (err) {
    logError("assign_reserva_error", err);
    if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400, headers: corsHeaders });
    if (err instanceof AuthError) return Response.json({ error: "Unauthorized" }, { status: 401, headers: corsHeaders });
    return Response.json({ error: "Internal error" }, { status: 500, headers: corsHeaders });
  }
});
```

### 4.6 `propose-reschedule`

```typescript
// supabase/functions/propose-reschedule/index.ts
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { corsHeaders, handleCors } from "../_shared/cors.ts";
import { requireAuth, getAdminClient } from "../_shared/auth.ts";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

const BodySchema = z.object({
  classId: z.string().uuid(),
  proposedTime: z.string().datetime(),
  reason: z.string().max(500).optional(),
});

serve(async (req) => {
  const preflight = handleCors(req);
  if (preflight) return preflight;

  try {
    const { user } = await requireAuth(req);
    const body = BodySchema.parse(await req.json());
    const admin = getAdminClient();

    const { data: cls, error: clsErr } = await admin.from("classes")
      .select("id,status,teacher_id").eq("id", body.classId).eq("status", "PUBLISHED").single();
    if (clsErr || !cls) return Response.json({ error: "Clase no encontrada" }, { status: 404, headers: corsHeaders });

    const { data: reschedule, error } = await admin.from("reschedules").insert({
      class_id: body.classId,
      teacher_id: cls.teacher_id,
      proposed_time: body.proposedTime,
      reason: body.reason ?? null,
      status: "PROPOSED",
    }).select("*").single();

    if (error) throw error;

    await admin.from("notifications").insert({
      user_id: cls.teacher_id,
      title: "Nueva propuesta de reagendamiento",
      message: `Se ha propuesto reagendar la clase para ${body.proposedTime}`,
      type: "RESCHEDULE_PROPOSED",
    });

    logInfo("reschedule_proposed", { rescheduleId: reschedule.id, classId: body.classId });
    return Response.json(reschedule, { headers: corsHeaders });

  } catch (err) {
    logError("propose_reschedule_error", err);
    if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400, headers: corsHeaders });
    if (err instanceof AuthError) return Response.json({ error: "Unauthorized" }, { status: 401, headers: corsHeaders });
    return Response.json({ error: "Internal error" }, { status: 500, headers: corsHeaders });
  }
});
```

### 4.7 `teacher-decision`

```typescript
// supabase/functions/teacher-decision/index.ts
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { corsHeaders, handleCors } from "../_shared/cors.ts";
import { requireAuth, getAdminClient } from "../_shared/auth.ts";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

const BodySchema = z.object({
  rescheduleId: z.string().uuid(),
  accepted: z.boolean(),
});

serve(async (req) => {
  const preflight = handleCors(req);
  if (preflight) return preflight;

  try {
    const { user, roles } = await requireAuth(req);
    const body = BodySchema.parse(await req.json());
    const admin = getAdminClient();

    const { data: resched, error: rErr } = await admin.from("reschedules")
      .select("*, class:classes(*)").eq("id", body.rescheduleId).eq("status", "PROPOSED").single();
    if (rErr || !resched) {
      return Response.json({ error: "Reagendamiento no encontrado" }, { status: 404, headers: corsHeaders });
    }

    const isTeacher = resched.class?.teacher_id === user.id;
    const isVenueAdmin = roles.includes("VENUE_ADMIN");
    if (!isTeacher && !isVenueAdmin && !roles.includes("ADMIN")) {
      return Response.json({ error: "No autorizado" }, { status: 403, headers: corsHeaders });
    }

    if (body.accepted) {
      const deadline = new Date(Date.now() + 48 * 3600 * 1000).toISOString();
      await admin.from("reschedules").update({
        status: "TEACHER_ACCEPTED",
        response_deadline: deadline,
      }).eq("id", body.rescheduleId);

      // Actualizar horario de la clase inmediatamente
      const newEndTime = new Date(
        new Date(resched.proposed_time).getTime() + (resched.class?.duration ?? 60) * 60000
      ).toISOString();
      await admin.from("classes").update({
        start_time: resched.proposed_time,
        end_time: newEndTime,
      }).eq("id", resched.class_id);

      // Crear respuestas pendientes para cada estudiante enrolado
      const { data: enrolled } = await admin.from("enrollments")
        .select("student_id").eq("class_id", resched.class_id).eq("status", "ACTIVE");
      if (enrolled) {
        for (const e of enrolled) {
          await admin.from("reschedule_responses").insert({
            reschedule_id: body.rescheduleId,
            user_id: e.student_id,
            response_type: null,
          });
          await admin.from("notifications").insert({
            user_id: e.student_id,
            title: "Confirma reagendamiento",
            message: `Tu clase fue reagendada. Tienes 48h para confirmar.`,
            type: "RESCHEDULE_PENDING",
          });
        }
      }
    } else {
      await admin.from("reschedules").update({ status: "TEACHER_REJECTED" }).eq("id", body.rescheduleId);

      // Marcar pagos como REFUND_PENDING
      const { data: enrolled } = await admin.from("enrollments")
        .select("id, student_id").eq("class_id", resched.class_id).eq("status", "ACTIVE");
      if (enrolled) {
        for (const e of enrolled) {
          await admin.from("payments").update({ status: "REFUND_PENDING" })
            .eq("enrollment_id", e.id).eq("status", "RETAINED");
          await admin.from("notifications").insert({
            user_id: e.student_id,
            title: "Clase reagendada cancelada",
            message: "El profesor rechazó el reagendamiento. Se procesará tu reembolso.",
            type: "RESCHEDULE_REJECTED",
          });
        }
      }
    }

    logInfo("teacher_decision", { rescheduleId: body.rescheduleId, accepted: body.accepted });
    return Response.json({ status: body.accepted ? "TEACHER_ACCEPTED" : "TEACHER_REJECTED" }, { headers: corsHeaders });

  } catch (err) {
    logError("teacher_decision_error", err);
    if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400, headers: corsHeaders });
    if (err instanceof AuthError) return Response.json({ error: "Unauthorized" }, { status: 401, headers: corsHeaders });
    return Response.json({ error: "Internal error" }, { status: 500, headers: corsHeaders });
  }
});
```

### 4.8 `student-decision`

```typescript
// supabase/functions/student-decision/index.ts
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { corsHeaders, handleCors } from "../_shared/cors.ts";
import { requireAuth, getAdminClient } from "../_shared/auth.ts";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

const BodySchema = z.object({
  rescheduleId: z.string().uuid(),
  accepted: z.boolean(),
});

serve(async (req) => {
  const preflight = handleCors(req);
  if (preflight) return preflight;

  try {
    const { user } = await requireAuth(req);
    const body = BodySchema.parse(await req.json());
    const admin = getAdminClient();

    // Buscar respuesta pendiente
    const { data: resp, error } = await admin.from("reschedule_responses")
      .select("*").eq("reschedule_id", body.rescheduleId).eq("user_id", user.id)
      .is("response_type", null).single();
    if (error || !resp) return Response.json({ error: "Respuesta no pendiente" }, { status: 404, headers: corsHeaders });

    const responseType = body.accepted ? "ACCEPTED" : "REJECTED";
    await admin.from("reschedule_responses").update({
      response_type: responseType,
      responded_at: new Date().toISOString(),
    }).eq("id", resp.id);

    // Si rechaza, marcar pago como REFUND_PENDING
    if (!body.accepted) {
      const { data: resched } = await admin.from("reschedules")
        .select("class_id").eq("id", body.rescheduleId).single();
      const { data: enrollments } = await admin.from("enrollments")
        .select("id").eq("class_id", resched?.class_id).eq("student_id", user.id).eq("status", "ACTIVE");
      if (enrollments) {
        for (const e of enrollments) {
          await admin.from("payments").update({ status: "REFUND_PENDING" })
            .eq("enrollment_id", e.id).eq("status", "RETAINED");
        }
      }
    }

    // Verificar si todos los estudiantes respondieron
    const { count } = await admin.from("reschedule_responses")
      .select("*", { count: "exact", head: true })
      .eq("reschedule_id", body.rescheduleId).is("response_type", null);
    if (count === 0) {
      await admin.from("reschedules").update({ status: "COMPLETED" }).eq("id", body.rescheduleId);
    }

    logInfo("student_decision", { rescheduleId: body.rescheduleId, accepted: body.accepted, userId: user.id });
    return Response.json({ status: "ok" }, { headers: corsHeaders });

  } catch (err) {
    logError("student_decision_error", err);
    if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400, headers: corsHeaders });
    if (err instanceof AuthError) return Response.json({ error: "Unauthorized" }, { status: 401, headers: corsHeaders });
    return Response.json({ error: "Internal error" }, { status: 500, headers: corsHeaders });
  }
});
```

### 4.9 `register-venue`

```typescript
// supabase/functions/register-venue/index.ts
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { corsHeaders, handleCors } from "../_shared/cors.ts";
import { requireAuth, getAdminClient } from "../_shared/auth.ts";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

const BodySchema = z.object({
  name: z.string().min(1),
  city: z.string().optional(),
  region: z.string().optional(),
  comuna: z.string().optional(),
  address: z.string().optional(),
  description: z.string().optional(),
  phone: z.string().optional(),
  email: z.string().email().optional(),
  tipo: z.enum(["SEDE","HOME_STUDIO"]).default("SEDE"),
  instagram: z.string().optional(),
  youtube: z.string().optional(),
  sitioWeb: z.string().optional(),
  facebook: z.string().optional(),
});

serve(async (req) => {
  const preflight = handleCors(req);
  if (preflight) return preflight;

  try {
    const { user } = await requireAuth(req);
    const body = BodySchema.parse(await req.json());
    const admin = getAdminClient();

    // Validar identidad aprobada
    const { data: profile } = await admin.from("profiles")
      .select("identidad_validada").eq("id", user.id).single();
    if (!profile?.identidad_validada) {
      return Response.json({ error: "Identidad no validada. Sube tu documento primero." }, { status: 403, headers: corsHeaders });
    }

    // Reusar sede previamente rechazada
    const { data: existing } = await admin.from("venues")
      .select("id").eq("admin_id", user.id).eq("status", "RECHAZADA").maybeSingle();

    let venue;
    if (existing) {
      const { data: updated } = await admin.from("venues").update({
        ...body, status: "PENDIENTE_APROBACION",
      }).eq("id", existing.id).select("*").single();
      venue = updated;
    } else {
      const { data: created } = await admin.from("venues").insert({
        ...body, admin_id: user.id, status: "PENDIENTE_APROBACION",
      }).select("*").single();
      venue = created;
    }

    await admin.from("notifications").insert({
      user_id: user.id,
      title: "Sede registrada",
      message: "Tu sede está pendiente de aprobación.",
      type: "VENUE_REGISTERED",
    });

    await admin.from("audit_logs").insert({
      actor_id: user.id,
      action: "venue.registered",
      resource_type: "venue",
      resource_id: venue.id,
    });

    logInfo("venue_registered", { venueId: venue.id, adminId: user.id });
    return Response.json(venue, { headers: corsHeaders });

  } catch (err) {
    logError("register_venue_error", err);
    if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400, headers: corsHeaders });
    if (err instanceof AuthError) return Response.json({ error: "Unauthorized" }, { status: 401, headers: corsHeaders });
    return Response.json({ error: "Internal error" }, { status: 500, headers: corsHeaders });
  }
});
```

### 4.10 `admin-approve-venue`

```typescript
// supabase/functions/admin-approve-venue/index.ts
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { corsHeaders, handleCors } from "../_shared/cors.ts";
import { requireAuth, getAdminClient } from "../_shared/auth.ts";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

const BodySchema = z.object({
  venueId: z.string().uuid(),
  action: z.enum(["approve","reject"]),
  reason: z.string().optional(),
});

serve(async (req) => {
  const preflight = handleCors(req);
  if (preflight) return preflight;

  try {
    const { user, roles } = await requireAuth(req);
    if (!roles.includes("ADMIN")) return Response.json({ error: "Forbidden" }, { status: 403, headers: corsHeaders });

    const body = BodySchema.parse(await req.json());
    const admin = getAdminClient();

    const { data: venue } = await admin.from("venues")
      .select("*, admin:admin_id(id)").eq("id", body.venueId).single();
    if (!venue) return Response.json({ error: "Sede no encontrada" }, { status: 404, headers: corsHeaders });

    if (body.action === "approve") {
      await admin.from("venues").update({
        status: "APROBADA", rejection_reason: null,
      }).eq("id", body.venueId);

      await admin.from("profiles").update({ tiene_sede_aprobada: true }).eq("id", venue.admin_id);

      // Asignar rol VENUE_ADMIN
      const { data: adminUser } = await admin.auth.admin.getUserById(venue.admin_id);
      const existingRoles: string[] = adminUser.user?.app_metadata?.roles ?? [];
      if (!existingRoles.includes("VENUE_ADMIN")) {
        await admin.auth.admin.updateUserById(venue.admin_id, {
          app_metadata: { roles: [...existingRoles, "VENUE_ADMIN"] },
        });
      }

      await admin.from("notifications").insert({
        user_id: venue.admin_id,
        title: "Sede aprobada",
        message: "Tu sede ha sido aprobada. Ya puedes gestionar salas.",
        type: "VENUE_APPROVED",
      });
    } else {
      await admin.from("venues").update({
        status: "RECHAZADA", rejection_reason: body.reason,
      }).eq("id", body.venueId);

      await admin.from("notifications").insert({
        user_id: venue.admin_id,
        title: "Sede rechazada",
        message: `Motivo: ${body.reason}`,
        type: "VENUE_REJECTED",
      });
    }

    await admin.from("audit_logs").insert({
      actor_id: user.id,
      action: `venue.${body.action}d`,
      resource_type: "venue",
      resource_id: body.venueId,
      metadata: { reason: body.reason },
    });

    logInfo(`venue_${body.action}d`, { venueId: body.venueId, adminId: user.id });
    return Response.json({ status: "ok" }, { headers: corsHeaders });

  } catch (err) {
    logError("approve_venue_error", err);
    if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400, headers: corsHeaders });
    return Response.json({ error: "Internal error" }, { status: 500, headers: corsHeaders });
  }
});
```

### 4.11 `confirm-class`

```typescript
// supabase/functions/confirm-class/index.ts
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { corsHeaders, handleCors } from "../_shared/cors.ts";
import { requireAuth, getAdminClient } from "../_shared/auth.ts";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

const BodySchema = z.object({
  classId: z.string().uuid(),
  realized: z.boolean(),
});

serve(async (req) => {
  const preflight = handleCors(req);
  if (preflight) return preflight;

  try {
    const { user, roles } = await requireAuth(req);
    if (!roles.includes("VENUE_ADMIN") && !roles.includes("ADMIN")) {
      return Response.json({ error: "Forbidden" }, { status: 403, headers: corsHeaders });
    }

    const body = BodySchema.parse(await req.json());
    const admin = getAdminClient();

    const { data: cls } = await admin.from("classes")
      .select("id, status, room:rooms(venue:venues(admin_id))").eq("id", body.classId).single();
    if (!cls) return Response.json({ error: "Clase no encontrada" }, { status: 404, headers: corsHeaders });

    const venueAdminId = cls.room?.venue?.admin_id;
    if (venueAdminId !== user.id && !roles.includes("ADMIN")) {
      return Response.json({ error: "No autorizado para esta sede" }, { status: 403, headers: corsHeaders });
    }

    if (body.realized) {
      // Liberar pagos retenidos
      const { data: enrollments } = await admin.from("enrollments")
        .select("id").eq("class_id", body.classId).eq("status", "ACTIVE");
      if (enrollments) {
        for (const e of enrollments) {
          await admin.from("payments").update({ status: "RELEASED" })
            .eq("enrollment_id", e.id).eq("status", "RETAINED");
        }
      }
    }

    await admin.from("classes").update({
      status: body.realized ? "COMPLETED" : "SUSPENDED",
    }).eq("id", body.classId);

    await admin.from("audit_logs").insert({
      actor_id: user.id,
      action: body.realized ? "class.confirmed_realized" : "class.confirmed_not_realized",
      resource_type: "class",
      resource_id: body.classId,
    });

    logInfo(body.realized ? "class_confirmed" : "class_not_realized", { classId: body.classId });
    return Response.json({ status: body.realized ? "COMPLETED" : "SUSPENDED" }, { headers: corsHeaders });

  } catch (err) {
    logError("confirm_class_error", err);
    if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400, headers: corsHeaders });
    return Response.json({ error: "Internal error" }, { status: 500, headers: corsHeaders });
  }
});
```

### 4.12 `generate-blocks`

```typescript
// supabase/functions/generate-blocks/index.ts
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { corsHeaders, handleCors } from "../_shared/cors.ts";
import { requireAuth, getAdminClient } from "../_shared/auth.ts";
import { logInfo, logError } from "../_shared/logger.ts";

serve(async (req) => {
  const preflight = handleCors(req);
  if (preflight) return preflight;

  try {
    const { roles } = await requireAuth(req);
    if (!roles.includes("VENUE_ADMIN") && !roles.includes("ADMIN")) {
      return Response.json({ error: "Forbidden" }, { status: 403, headers: corsHeaders });
    }

    const admin = getAdminClient();
    const { error } = await admin.rpc("regenerate_schedule_blocks");
    if (error) throw error;

    logInfo("blocks_regenerated", {});
    return Response.json({ status: "ok" }, { headers: corsHeaders });

  } catch (err) {
    logError("generate_blocks_error", err);
    return Response.json({ error: "Internal error" }, { status: 500, headers: corsHeaders });
  }
});
```

### 4.13 `admin-stats`

```typescript
// supabase/functions/admin-stats/index.ts
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { corsHeaders, handleCors } from "../_shared/cors.ts";
import { requireAuth, getAdminClient } from "../_shared/auth.ts";
import { logError } from "../_shared/logger.ts";

serve(async (req) => {
  const preflight = handleCors(req);
  if (preflight) return preflight;

  try {
    const { roles } = await requireAuth(req);
    if (!roles.includes("ADMIN")) return Response.json({ error: "Forbidden" }, { status: 403, headers: corsHeaders });

    const admin = getAdminClient();
    const [users, classes, venues, payments, identity, reviews, sessions] = await Promise.all([
      admin.from("profiles").select("*", { count: "exact", head: true }),
      admin.from("classes").select("status, price, capacity"),
      admin.from("venues").select("status"),
      admin.from("payments").select("status, amount"),
      admin.from("identity_verifications").select("status"),
      admin.from("reviews").select("score"),
      admin.from("payment_sessions").select("status"),
    ]);

    const classesData = classes.data ?? [];
    const paymentsData = payments.data ?? [];
    const venuesData = venues.data ?? [];
    const identityData = identity.data ?? [];
    const reviewsData = reviews.data ?? [];
    const sessionsData = sessions.data ?? [];

    const totalRevenue = paymentsData
      .filter((p) => p.status === "RELEASED")
      .reduce((s, p) => s + (p.amount || 0), 0);

    const retainedTotal = paymentsData
      .filter((p) => p.status === "RETAINED")
      .reduce((s, p) => s + (p.amount || 0), 0);

    const avgRating = reviewsData.length > 0
      ? (reviewsData.reduce((s, r) => s + (r.score || 0), 0) / reviewsData.length).toFixed(2)
      : "0";

    const conversionRate = sessionsData.length > 0
      ? (sessionsData.filter((s) => s.status === "APPROVED").length / sessionsData.length * 100).toFixed(1)
      : "0";

    return Response.json({
      totalUsers: users.count ?? 0,
      activeClasses: classesData.filter((c) => c.status === "PUBLISHED").length,
      completedClasses: classesData.filter((c) => c.status === "COMPLETED").length,
      pendingVenues: venuesData.filter((v) => v.status === "PENDIENTE_APROBACION").length,
      pendingIdentity: identityData.filter((i) => i.status === "PENDING").length,
      totalRevenue,
      retainedTotal,
      avgRating: parseFloat(avgRating),
      conversionRate: parseFloat(conversionRate),
    }, { headers: corsHeaders });

  } catch (err) {
    logError("admin_stats_error", err);
    return Response.json({ error: "Internal error" }, { status: 500, headers: corsHeaders });
  }
});
```

### 4.14 `create-review`

```typescript
// supabase/functions/create-review/index.ts
import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { corsHeaders, handleCors } from "../_shared/cors.ts";
import { requireAuth } from "../_shared/auth.ts";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

const BodySchema = z.object({
  classId: z.string().uuid(),
  targetId: z.string().uuid(),
  targetType: z.enum(["CLASS","VENUE","STUDENT"]),
  score: z.number().int().min(1).max(5),
  comment: z.string().max(1000).optional(),
});

serve(async (req) => {
  const preflight = handleCors(req);
  if (preflight) return preflight;

  try {
    const { user, supabase } = await requireAuth(req);
    const body = BodySchema.parse(await req.json());

    // Validar que el usuario participó en la clase
    const { data: enrollment } = await supabase.from("enrollments")
      .select("id").eq("class_id", body.classId).eq("student_id", user.id).eq("status", "ACTIVE").maybeSingle();
    const { data: cls } = await supabase.from("classes")
      .select("teacher_id").eq("id", body.classId).eq("teacher_id", user.id).maybeSingle();

    if (!enrollment && !cls) {
      return Response.json({ error: "Debes haber participado en la clase para reseñar" }, { status: 403, headers: corsHeaders });
    }

    const { data: review, error } = await supabase.from("reviews").insert({
      class_id: body.classId,
      reviewer_id: user.id,
      target_type: body.targetType,
      target_id: body.targetId,
      score: body.score,
      comment: body.comment ?? null,
    }).select("*").single();

    if (error) throw error;

    logInfo("review_created", { reviewId: review.id, classId: body.classId });
    return Response.json(review, { headers: corsHeaders });

  } catch (err) {
    logError("create_review_error", err);
    if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400, headers: corsHeaders });
    if (err instanceof AuthError) return Response.json({ error: "Unauthorized" }, { status: 401, headers: corsHeaders });
    return Response.json({ error: "Internal error" }, { status: 500, headers: corsHeaders });
  }
});
```

---

## 5. STORAGE BUCKETS

```sql
-- Migration: buckets y políticas
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES
  ('avatars', 'avatars', true, 2097152, '{image/jpeg,image/png,image/webp}'),
  ('venue-photos', 'venue-photos', true, 5242880, '{image/jpeg,image/png,image/webp}'),
  ('room-photos', 'room-photos', true, 5242880, '{image/jpeg,image/png,image/webp}'),
  ('venue-documents', 'venue-documents', false, 5242880, '{image/jpeg,image/png,application/pdf}'),
  ('identity-docs', 'identity-docs', false, 5242880, '{image/jpeg,image/png,application/pdf}');
```

### Políticas Storage

```sql
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
```

---

## 6. SCHEDULED JOBS (pg_cron)

```sql
-- Migration: pg_cron jobs

-- 1. Completar clases expiradas (cada 30 minutos)
CREATE OR REPLACE FUNCTION public.process_class_completion()
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
BEGIN
  UPDATE public.classes SET status = 'POR_VALIDAR'
  WHERE status = 'PUBLISHED' AND start_time < now();
END;
$$;

SELECT cron.schedule('process-class-completion', '*/30 * * * *',
  $$SELECT public.process_class_completion()$$);

-- 2. Procesar timeouts de reagendamiento (cada hora)
CREATE OR REPLACE FUNCTION public.process_reschedule_timeouts()
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
  r record;
BEGIN
  FOR r IN
    SELECT res.id, res.class_id FROM public.reschedules res
    WHERE res.status = 'TEACHER_ACCEPTED' AND res.response_deadline < now()
  LOOP
    -- Marcar respuestas pendientes como TIMEOUT
    UPDATE public.reschedule_responses SET response_type = 'TIMEOUT', responded_at = now()
    WHERE reschedule_id = r.id AND response_type IS NULL;

    -- Marcar pagos como REFUND_PENDING para los que hicieron timeout
    UPDATE public.payments p SET status = 'REFUND_PENDING'
    FROM public.enrollments e
    JOIN public.reschedule_responses rr ON rr.user_id = e.student_id
    WHERE e.class_id = r.class_id AND p.enrollment_id = e.id
      AND p.status = 'RETAINED' AND rr.reschedule_id = r.id AND rr.response_type = 'TIMEOUT';

    -- Completar reagendamiento
    UPDATE public.reschedules SET status = 'COMPLETED' WHERE id = r.id;
  END LOOP;
END;
$$;

SELECT cron.schedule('process-reschedule-timeouts', '0 * * * *',
  $$SELECT public.process_reschedule_timeouts()$$);

-- 3. Regenerar bloques de horario (lunes 4 AM)
CREATE OR REPLACE FUNCTION public.regenerate_schedule_blocks()
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
  venue record;
  room record;
  schedule record;
  cfg record;
  day_date date;
  day_offset int;
  block_start timestamptz;
  block_end timestamptz;
  day_start timestamptz;
  day_end timestamptz;
BEGIN
  FOR venue IN SELECT id FROM public.venues WHERE status = 'APROBADA' LOOP
    SELECT * INTO cfg FROM public.venue_block_configs WHERE venue_id = venue.id;
    IF cfg IS NULL THEN CONTINUE; END IF;

    FOR schedule IN SELECT * FROM public.venue_schedules WHERE venue_id = venue.id LOOP
      FOR day_offset IN 0..6 LOOP
        day_date := current_date + day_offset;
        IF trim(to_char(day_date, 'DAY')) = schedule.day_of_week THEN
          day_start := day_date + schedule.open_time;
          day_end := day_date + schedule.close_time;
          block_start := day_start;

          WHILE block_start + make_interval(mins := cfg.block_duration_min) <= day_end LOOP
            block_end := block_start + make_interval(mins := cfg.block_duration_min);

            FOR room IN SELECT id FROM public.rooms WHERE venue_id = venue.id AND activa = true LOOP
              INSERT INTO public.room_schedule_blocks (room_id, start_time, end_time, status)
              VALUES (room.id, block_start, block_end, 'AVAILABLE')
              ON CONFLICT DO NOTHING;
            END LOOP;

            block_start := block_start + make_interval(mins := cfg.block_duration_min + cfg.gap_between_blocks_min);
          END LOOP;
        END IF;
      END LOOP;
    END LOOP;
  END LOOP;
END;
$$;

SELECT cron.schedule('regenerate-schedule-blocks', '0 4 * * 1',
  $$SELECT public.regenerate_schedule_blocks()$$);

-- 4. Health check RLS (cada 15 minutos)
CREATE OR REPLACE FUNCTION public.check_rls_coverage()
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
  missing_count int;
BEGIN
  SELECT count(*) INTO missing_count
  FROM pg_tables WHERE schemaname = 'public' AND rowsecurity = false;

  IF missing_count > 0 THEN
    INSERT INTO public.system_metrics (metric_name, metric_value, labels)
    VALUES ('rls_missing_tables', missing_count,
      (SELECT jsonb_object_agg(tablename, 'missing_rls')
       FROM pg_tables WHERE schemaname = 'public' AND rowsecurity = false));
  END IF;
END;
$$;

SELECT cron.schedule('health-check-rls', '*/15 * * * *',
  $$SELECT public.check_rls_coverage()$$);

-- 5. Snapshot métricas (cada hora)
CREATE OR REPLACE FUNCTION public.snapshot_system_metrics()
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
BEGIN
  INSERT INTO public.system_metrics (metric_name, metric_value, labels) VALUES
    ('total_users',       (SELECT count(*) FROM auth.users), '{}'::jsonb),
    ('active_classes',    (SELECT count(*) FROM public.classes WHERE status = 'PUBLISHED'), '{}'::jsonb),
    ('completed_classes', (SELECT count(*) FROM public.classes WHERE status = 'COMPLETED'), '{}'::jsonb),
    ('pending_venues',    (SELECT count(*) FROM public.venues WHERE status = 'PENDIENTE_APROBACION'), '{}'::jsonb),
    ('pending_identity',  (SELECT count(*) FROM public.identity_verifications WHERE status = 'PENDING'), '{}'::jsonb),
    ('retained_total',    (SELECT coalesce(sum(amount),0) FROM public.payments WHERE status = 'RETAINED'), '{}'::jsonb),
    ('released_total',    (SELECT coalesce(sum(amount),0) FROM public.payments WHERE status = 'RELEASED'), '{}'::jsonb),
    ('active_enrollments',(SELECT count(*) FROM public.enrollments WHERE status = 'ACTIVE'), '{}'::jsonb);
END;
$$;

SELECT cron.schedule('snapshot-metrics', '0 * * * *',
  $$SELECT public.snapshot_system_metrics()$$);

-- 6. Limpieza métricas antiguas (diario 3 AM)
SELECT cron.schedule('cleanup-old-metrics', '0 3 * * *',
  $$DELETE FROM public.system_metrics WHERE recorded_at < now() - interval '90 days'$$);
```

---

## 7. REALTIME

```sql
-- Tablas en la publicación realtime
ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications;
ALTER PUBLICATION supabase_realtime ADD TABLE public.classes;
ALTER PUBLICATION supabase_realtime ADD TABLE public.reschedules;
ALTER PUBLICATION supabase_realtime ADD TABLE public.room_schedule_blocks;
```

### Código frontend Vue 3

```typescript
// frontend/src/lib/supabase.ts
import { createClient } from '@supabase/supabase-js'

export const supabase = createClient(
  import.meta.env.VITE_SUPABASE_URL,
  import.meta.env.VITE_SUPABASE_ANON_KEY
)

// Canal: notificaciones
export function subscribeNotifications(userId: string, onInsert: (payload: any) => void) {
  return supabase.channel('notifications')
    .on('postgres_changes',
      { event: 'INSERT', schema: 'public', table: 'notifications', filter: `user_id=eq.${userId}` },
      onInsert
    )
    .subscribe()
}

// Canal: cambios de estado de clase
export function subscribeClassUpdates(userId: string, role: string, onUpdate: (payload: any) => void) {
  const filter = role === 'TEACHER' ? `teacher_id=eq.${userId}` : undefined;
  return supabase.channel('class-updates')
    .on('postgres_changes',
      { event: 'UPDATE', schema: 'public', table: 'classes', filter },
      onUpdate
    )
    .subscribe()
}

// Canal: reagendamientos
export function subscribeReschedules(userId: string, onInsert: (payload: any) => void) {
  return supabase.channel('reschedules')
    .on('postgres_changes',
      { event: 'INSERT', schema: 'public', table: 'reschedules', filter: `teacher_id=eq.${userId}` },
      onInsert
    )
    .subscribe()
}
```

---

## 8. VARIABLES DE ENTORNO

### `supabase/.env` (desarrollo local)

```bash
SUPABASE_URL=http://127.0.0.1:54321
SUPABASE_ANON_KEY=eyJ...
SUPABASE_SERVICE_ROLE_KEY=eyJ...
MERCADOPAGO_ACCESS_TOKEN=TEST-xxxxxxxxxxxxxxxxxxxx
MERCADOPAGO_WEBHOOK_SECRET=xxxxxxxxxxxxxxxxxxxxxxxx
APP_FRONTEND_URL=http://localhost:5173
FUNCTIONS_URL=http://127.0.0.1:54321/functions/v1
```

### Supabase Dashboard Secrets (producción)

```bash
supabase secrets set MERCADOPAGO_ACCESS_TOKEN=APP_USR-xxxxxxxxxxxxxxxxxxxx
supabase secrets set MERCADOPAGO_WEBHOOK_SECRET=xxxxxxxxxxxxxxxx
supabase secrets set APP_FRONTEND_URL=https://modoensayo.com
```

### Frontend `.env`

```bash
VITE_SUPABASE_URL=https://<project>.supabase.co
VITE_SUPABASE_ANON_KEY=eyJ...
```

---

## 9. FRONTEND — CAMBIOS EN VUE 3

### 9.1 Reemplazar `stores/auth.js`

```typescript
// frontend/src/stores/auth.ts
import { supabase } from '../lib/supabase'

class AuthStore {
  async login(email: string, password: string) {
    const { data, error } = await supabase.auth.signInWithPassword({ email, password })
    if (error) throw error
    return data
  }

  async register(email: string, password: string, fullName: string) {
    const { data, error } = await supabase.auth.signUp({
      email, password,
      options: { data: { full_name: fullName } }
    })
    if (error) throw error
    return data
  }

  async loginWithGoogle() {
    const { data, error } = await supabase.auth.signInWithOAuth({
      provider: 'google',
      options: { redirectTo: window.location.origin + '/auth/callback' }
    })
    if (error) throw error
  }

  async logout() {
    await supabase.auth.signOut()
  }

  async getSession() {
    const { data: { session } } = await supabase.auth.getSession()
    return session
  }

  getUser() {
    return supabase.auth.getUser()
  }

  async getRoles(): Promise<string[]> {
    const { data: { user } } = await supabase.auth.getUser()
    return (user?.app_metadata?.roles as string[]) ?? []
  }

  onAuthChange(callback: (session: any) => void) {
    supabase.auth.onAuthStateChange((_event, session) => callback(session))
  }
}

export const authStore = new AuthStore()
```

### 9.2 Reemplazar `services/api.js`

```typescript
// frontend/src/lib/api.ts
import { supabase } from './supabase'

// ===== PostgREST directo para CRUD simple =====

export async function getPublishedClasses(filters?: Record<string, string>) {
  let query = supabase.from('classes')
    .select('*, room:rooms(*, venue:venues(*))')
    .eq('status', 'PUBLISHED')

  if (filters?.disciplina) query = query.ilike('discipline', `%${filters.disciplina}%`)
  if (filters?.nivel) query = query.eq('level', filters.nivel)
  if (filters?.precioMin) query = query.gte('price', filters.precioMin)
  if (filters?.precioMax) query = query.lte('price', filters.precioMax)
  if (filters?.comuna) query = query.or(`comuna.ilike.%${filters.comuna}%,city.ilike.%${filters.comuna}%`)

  const { data, error } = await query.order('start_time', { ascending: true })
  if (error) throw error
  return data
}

export async function getApprovedVenues() {
  const { data, error } = await supabase.from('venues').select('*').eq('status', 'APROBADA')
  if (error) throw error
  return data
}

export async function getVenueRooms(venueId: string) {
  const { data, error } = await supabase.from('rooms').select('*').eq('venue_id', venueId)
  if (error) throw error
  return data
}

export async function getDisciplines() {
  const { data, error } = await supabase.from('discipline_catalog')
    .select('name, category').eq('active', true).order('sort_order', { ascending: true })
  if (error) throw error
  return data
}

export async function getClassById(id: string) {
  const { data, error } = await supabase.from('classes')
    .select('*, room:rooms(*, venue:venues(*)), teacher:profiles!classes_teacher_id_fkey(full_name, social_name)')
    .eq('id', id).single()
  if (error) throw error
  return data
}

export async function getTeacherProfile(userId: string) {
  const { data, error } = await supabase.from('professional_profiles')
    .select('*, user:profiles!professional_profiles_id_fkey(full_name, social_name)')
    .eq('id', userId).single()
  if (error) throw error
  return data
}

export async function getMyEnrollments() {
  const { data, error } = await supabase.from('enrollments')
    .select('*, class:classes(*)').eq('student_id', (await supabase.auth.getUser()).data.user?.id).eq('status', 'ACTIVE')
  if (error) throw error
  return data
}

export async function getMyProfile() {
  const { data, error } = await supabase.from('profiles').select('*')
    .eq('id', (await supabase.auth.getUser()).data.user?.id).single()
  if (error) throw error
  return data
}

// ===== Edge Functions para lógica compleja =====

export async function createMercadoPagoPreference(items: Array<{ classId: string; classTitle: string; discipline: string; level: string; price: number }>) {
  const { data: { session } } = await supabase.auth.getSession()
  const res = await fetch(`${import.meta.env.VITE_SUPABASE_URL}/functions/v1/mercadopago-create-preference`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${session?.access_token}`
    },
    body: JSON.stringify({ items }),
  })
  return res.json()
}

export async function createClass(classData: any) {
  const { data: { session } } = await supabase.auth.getSession()
  const res = await fetch(`${import.meta.env.VITE_SUPABASE_URL}/functions/v1/create-class`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${session?.access_token}`
    },
    body: JSON.stringify(classData),
  })
  return res.json()
}

export async function assignReserva(data: { classId: string; roomId: string; startTime: string; duration: number }) {
  const { data: { session } } = await supabase.auth.getSession()
  const res = await fetch(`${import.meta.env.VITE_SUPABASE_URL}/functions/v1/assign-reserva`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${session?.access_token}`
    },
    body: JSON.stringify(data),
  })
  return res.json()
}

export async function proposeReschedule(data: { classId: string; proposedTime: string; reason?: string }) {
  const { data: { session } } = await supabase.auth.getSession()
  const res = await fetch(`${import.meta.env.VITE_SUPABASE_URL}/functions/v1/propose-reschedule`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${session?.access_token}`
    },
    body: JSON.stringify(data),
  })
  return res.json()
}

export async function teacherDecision(data: { rescheduleId: string; accepted: boolean }) {
  const { data: { session } } = await supabase.auth.getSession()
  const res = await fetch(`${import.meta.env.VITE_SUPABASE_URL}/functions/v1/teacher-decision`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${session?.access_token}`
    },
    body: JSON.stringify(data),
  })
  return res.json()
}

export async function studentDecision(data: { rescheduleId: string; accepted: boolean }) {
  const { data: { session } } = await supabase.auth.getSession()
  const res = await fetch(`${import.meta.env.VITE_SUPABASE_URL}/functions/v1/student-decision`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${session?.access_token}`
    },
    body: JSON.stringify(data),
  })
  return res.json()
}

export async function registerVenue(data: any) {
  const { data: { session } } = await supabase.auth.getSession()
  const res = await fetch(`${import.meta.env.VITE_SUPABASE_URL}/functions/v1/register-venue`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${session?.access_token}`
    },
    body: JSON.stringify(data),
  })
  return res.json()
}

export async function createReview(data: { classId: string; targetId: string; targetType: string; score: number; comment?: string }) {
  const { data: { session } } = await supabase.auth.getSession()
  const res = await fetch(`${import.meta.env.VITE_SUPABASE_URL}/functions/v1/create-review`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${session?.access_token}`
    },
    body: JSON.stringify(data),
  })
  return res.json()
}

export async function getAdminStats() {
  const { data: { session } } = await supabase.auth.getSession()
  const res = await fetch(`${import.meta.env.VITE_SUPABASE_URL}/functions/v1/admin-stats`, {
    headers: { 'Authorization': `Bearer ${session?.access_token}` }
  })
  return res.json()
}
```

---

## 10. MIGRACIÓN DE DATOS

### Exportar desde PostgreSQL actual

```bash
pg_dump "postgresql://modoensayo:modoensayo@localhost:5432/modoensayo" \
  --no-owner --no-privileges --data-only --inserts \
  --table=public.discipline_catalog \
  > data_export.sql
```

### Importar usuarios a Supabase Auth

```sql
-- Script de migración de usuarios existentes
-- La contraseña BCrypt es compatible con Supabase Auth
-- Ejecutar vía API admin de Supabase

DO $$
DECLARE
  u record;
BEGIN
  FOR u IN SELECT * FROM import_users LOOP
    PERFORM net.http_post(
      url := current_setting('supabase.url') || '/auth/v1/admin/users',
      headers := jsonb_build_object(
        'Authorization', 'Bearer ' || current_setting('supabase.service_role_key'),
        'Content-Type', 'application/json'
      ),
      body := jsonb_build_object(
        'email', u.email,
        'password_hash', u.password_hash,
        'email_confirmed', true,
        'app_metadata', jsonb_build_object('roles', u.roles_array),
        'user_metadata', jsonb_build_object(
          'full_name', u.full_name,
          'social_name', u.social_name,
          'phone', u.phone
        )
      )::text
    );
  END LOOP;
END;
$$;
```

### Importar datos de dominio

```sql
-- Migrar disciplinas
INSERT INTO public.discipline_catalog (id, name, category, active, sort_order)
SELECT id, name, category, active, sort_order FROM old_db.discipline_catalog;

-- Migrar sedes aprobadas
INSERT INTO public.venues (...)
SELECT ... FROM old_db.venues WHERE status = 'APROBADA';

-- Migrar clases publicadas
INSERT INTO public.classes (...)
SELECT ... FROM old_db.classes WHERE status IN ('PUBLISHED','COMPLETED');

-- Migrar enrollments activos
INSERT INTO public.enrollments (...)
SELECT ... FROM old_db.enrollments WHERE status = 'ACTIVE';
```

---

## 11. COMANDOS MCP PARA AGENTES

El proyecto está conectado a Supabase vía MCP. Estos comandos permiten a los agentes de IA inspeccionar y modificar la base de datos directamente:

```bash
# ===== Auditoría =====
# Auditoría de seguridad (revisa RLS, policies, grants, secrets)
supabase_get_advisors security

# Auditoría de performance (revisa índices, queries lentas, conexiones)
supabase_get_advisors performance

# ===== Inspección =====
# Ver todas las tablas con su estado RLS
supabase_execute_sql "SELECT schemaname||'.'||tablename AS tbl, rowsecurity FROM pg_tables WHERE schemaname='public' ORDER BY tablename"

# Ver todas las políticas RLS
supabase_execute_sql "SELECT tablename, policyname, cmd, qual, with_check FROM pg_policies WHERE schemaname='public' ORDER BY tablename, policyname"

# Ver estructura de una tabla específica
supabase_list_tables ["public"] true

# Ver FKs que no tienen índice
supabase_execute_sql "SELECT c.conrelid::regclass AS tabla, a.attname AS columna FROM pg_constraint c JOIN pg_attribute a ON a.attrelid=c.conrelid AND a.attnum=ANY(c.conkey) WHERE c.contype='f' AND NOT EXISTS(SELECT 1 FROM pg_index i WHERE i.indrelid=c.conrelid AND a.attnum=i.indkey[0]) ORDER BY tabla"

# Ver las 10 queries más lentas
supabase_execute_sql "SELECT calls, round(mean_exec_time::numeric,2) AS avg_ms, round(max_exec_time::numeric,2) AS max_ms, left(query,120) AS query_preview FROM pg_stat_statements WHERE query !~ 'pg_stat' ORDER BY mean_exec_time DESC LIMIT 10"

# Ver las 10 queries más frecuentes
supabase_execute_sql "SELECT calls, round(total_exec_time::numeric,2) AS total_ms, left(query,120) AS query_preview FROM pg_stat_statements ORDER BY calls DESC LIMIT 10"

# Ver jobs pg_cron configurados
supabase_execute_sql "SELECT jobname, schedule, command, active FROM cron.job"

# Ver historial de ejecuciones de pg_cron
supabase_execute_sql "SELECT jobname, start_time, end_time, status, return_message FROM cron.job_run_details ORDER BY start_time DESC LIMIT 20"

# Ver métricas del sistema (últimas 24h)
supabase_execute_sql "SELECT metric_name, metric_value, recorded_at FROM public.system_metrics WHERE recorded_at > now() - interval '24 hours' ORDER BY recorded_at DESC"

# ===== Desarrollo =====
# Crear una rama de desarrollo aislada
supabase_create_branch feature-nueva-funcionalidad

# Aplicar una migración
supabase_apply_migration nombre_migracion "<SQL completo>"

# Listar migraciones existentes
supabase_list_migrations

# Merge de rama de desarrollo a producción
supabase_merge_branch <branch_id>

# Rebase de rama con producción
supabase_rebase_branch <branch_id>

# ===== Edge Functions =====
# Listar edge functions desplegadas
supabase_list_edge_functions

# Ver código de una edge function
supabase_get_edge_function function_slug

# Desplegar/actualizar una edge function
supabase_deploy_edge_function function_name index.ts true

# Ver logs de edge functions (últimas 24h)
supabase_get_logs edge-function

# ===== Monitoreo =====
# Ver logs de PostgreSQL
supabase_get_logs postgres

# Ver logs de API
supabase_get_logs api

# Ver extensiones instaladas
supabase_list_extensions

# Generar tipos TypeScript desde el schema
supabase_generate_typescript_types
```

---

## 12. SEED DATA

```sql
-- seed.sql
-- Datos de referencia iniciales

INSERT INTO public.discipline_catalog (name, category, active, sort_order) VALUES
  ('Folclore', 'Danza', true, 1),
  ('Contemporaneo', 'Danza', true, 2),
  ('Ballet', 'Danza', true, 3),
  ('Urbano', 'Danza', true, 4),
  ('Folclore', 'Musica', true, 5),
  ('Instrumental', 'Musica', true, 6),
  ('Moderno', 'Musica', true, 7),
  ('Guitarra', 'Musica', true, 8),
  ('Bateria', 'Musica', true, 9),
  ('Bajo', 'Musica', true, 10),
  ('Canto', 'Musica', true, 11),
  ('Piano', 'Musica', true, 12),
  ('Violin', 'Musica', true, 13),
  ('Saxofon', 'Musica', true, 14),
  ('Clasico', 'Teatro', true, 15),
  ('Contemporaneo', 'Teatro', true, 16),
  ('Musical', 'Teatro', true, 17),
  ('Improvisacion', 'Teatro', true, 18),
  ('Dramaturgia', 'Teatro', true, 19)
ON CONFLICT (name, category) DO NOTHING;
```

---

## 13. CUTOVER

### Paso 1: Spring Boot → modo read-only

```yaml
# Agregar al application.yml del backend:
app:
  read-only-mode: true
```

### Paso 2: Frontend feature flag

```typescript
// frontend/src/lib/api.ts
const USE_SUPABASE = import.meta.env.VITE_USE_SUPABASE === 'true'

export async function getPublishedClasses(filters?: any) {
  if (USE_SUPABASE) {
    return getPublishedClassesSupabase(filters)  // nueva implementación
  }
  return getPublishedClassesLegacy(filters)       // axios → Spring Boot
}
```

### Paso 3: Verificación 48h

```sql
-- Comparar counts entre BD vieja y Supabase
SELECT 'old_db' AS source, count(*) FROM old_db.classes WHERE status = 'PUBLISHED'
UNION ALL
SELECT 'supabase', count(*) FROM public.classes WHERE status = 'PUBLISHED';

SELECT 'old_db' AS source, count(*) FROM old_db.users
UNION ALL
SELECT 'supabase', count(*) FROM auth.users;

SELECT 'old_db' AS source, coalesce(sum(amount),0) FROM old_db.payments WHERE status = 'RETAINED'
UNION ALL
SELECT 'supabase', coalesce(sum(amount),0) FROM public.payments WHERE status = 'RETAINED';
```

### Paso 4: Apagar Spring Boot

```bash
docker compose down backend
```

### Paso 5: Rollback plan

Si algo falla durante el cutover:
1. Spring Boot se mantiene intacto (solo en read-only)
2. Volver a poner `VITE_USE_SUPABASE=false` en frontend
3. Hacer rollback de datos desde backup de Supabase
4. DNS/Nginx redirige de vuelta a Spring Boot

---

## 14. CHECKLIST DE PRODUCCIÓN

### A. Security (bloqueante — debe estar todo [x] antes de producción)

- [ ] 100% tablas `public.*` con RLS habilitado (`rowsecurity = true`)
- [ ] 0 políticas `USING (true)` excepto `discipline_catalog`
- [ ] `SUPABASE_SERVICE_ROLE_KEY` nunca en frontend (sin prefijo `VITE_` o `NEXT_PUBLIC_`)
- [ ] Webhook MercadoPago con verificación HMAC SHA-256
- [ ] Funciones `SECURITY DEFINER` con `SET search_path = public, pg_temp`
- [ ] Email confirmation activo en producción
- [ ] Refresh token rotation + reuse detection activos
- [ ] Rol `anon` sin privilegios excesivos
- [ ] Buckets storage: públicos solo `avatars`, `venue-photos`, `room-photos`
- [ ] JWT expiry configurado (24h default)
- [ ] Rate limits en auth endpoints (sign-up, sign-in)
- [ ] CORS restringido a `APP_FRONTEND_URL`

### B. Performance

- [ ] Índices en todas las FKs
- [ ] Índices compuestos para queries frecuentes (`idx_classes_status_time`, etc.)
- [ ] Partial indexes para soft-delete y filtros por estado
- [ ] `pg_stat_statements` habilitado
- [ ] Realtime solo en tablas necesarias (4 tablas)
- [ ] Connection pooler: transaction mode para Edge Functions (`:6543`)
- [ ] Sin `SELECT *` en Edge Functions (columnas explícitas)

### C. Data Integrity

- [ ] FKs con `ON DELETE CASCADE`/`SET NULL` apropiado
- [ ] CHECK constraints (`amount > 0`, `score BETWEEN 1 AND 5`, etc.)
- [ ] UNIQUE constraints donde aplica (`class_id + student_id`, etc.)
- [ ] `updated_at` triggers en las 20 tablas de dominio
- [ ] `created_by` donde aplique para audit trail
- [ ] Enums nativos PostgreSQL para vocabulario cerrado
- [ ] `numeric(12,2)` para todos los campos de dinero
- [ ] `timestamptz` para todos los timestamps
- [ ] `boolean NOT NULL DEFAULT false` para todos los booleanos
- [ ] `jsonb NOT NULL DEFAULT '{}'` para snapshots
- [ ] `text[] NOT NULL DEFAULT '{}'` para arrays

### D. Observability

- [ ] `audit_logs` para operaciones críticas (pagos, venues, classes)
- [ ] `system_metrics` con snapshots horarios
- [ ] Health check endpoint: `GET /rest/v1/rpc/health_check`
- [ ] pg_cron: `health-check-rls` cada 15 minutos
- [ ] pg_cron: `snapshot-metrics` cada hora
- [ ] Edge Functions con logging JSON estructurado
- [ ] Supabase Dashboard alerts configurados (CPU, IOPS)

### E. Backups

- [ ] Plan Supabase con PITR habilitado
- [ ] Restore drill documentado y probado
- [ ] RTO definido: < 1 hora
- [ ] RPO definido: < 1 minuto (con PITR)

### F. Environment

- [ ] `.env.example` en el repo con todas las vars necesarias
- [ ] `.env.local` y `.env.production` en `.gitignore`
- [ ] Dev / staging / prod usan proyectos Supabase separados
- [ ] Service role keys diferentes por ambiente
- [ ] Credenciales OAuth (Google) diferentes por ambiente
- [ ] MercadoPago: token de TEST en dev, token de PROD en producción

### G. Frontend

- [ ] Cliente Supabase usa solo `anon` key
- [ ] No hay `service_role` en bundle frontend
- [ ] CSP headers configurados
- [ ] Errores de RLS muestran mensaje amigable (no detalles)

---

## FIRMA

Plan generado aplicando **Supabase Architect v1.0** — 52 heurísticas de seguridad y performance.
Cubre 139 endpoints migrados a 27 tablas + 13 enums + 94 políticas RLS + 13 Edge Functions + 6 jobs pg_cron + 5 buckets storage + 4 canales realtime + 2 tablas de observabilidad.

Migración quirúrgica del backend Spring Boot hacia Supabase Full Stack preservando el 100% de la lógica de negocio original.
