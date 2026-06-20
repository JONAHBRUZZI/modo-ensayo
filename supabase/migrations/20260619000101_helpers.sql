-- Helper functions & triggers

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

-- NOTA: is_enrolled / is_venue_admin / is_class_teacher se definen en
-- 20260619000250_helpers_rls.sql (después de crear las tablas), porque al ser
-- LANGUAGE sql Postgres valida el cuerpo al crearlas y referencian tablas.

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
