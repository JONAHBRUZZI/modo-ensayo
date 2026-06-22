# Usuarios de prueba y acceso a la base de datos

Tras la migración a Supabase, los usuarios viven en **Supabase Auth**
(`auth.users`) y su perfil en la tabla `public.profiles`. Los roles viajan en el
claim `app_metadata.roles` del JWT (no hay tablas `users` / `roles` /
`user_roles`).

## 1) Usuarios de prueba

Los usuarios de prueba se gestionan desde el **Supabase Dashboard → Authentication
→ Users** (o vía la Edge Function `admin-users` para asignar roles). Para crear
uno nuevo de prueba puedes usar el dashboard o registrarte desde la app.

> Las credenciales concretas de las cuentas demo no se versionan en el repo. Si
> necesitas acceso, pídelas al responsable del proyecto o créalas desde el
> dashboard.

## 2) Acceso a la base de datos

No hay PostgreSQL local en Docker. La base es el Postgres hosteado de Supabase.
Opciones de acceso:

- **Supabase Dashboard → Table Editor / SQL Editor** (lo más simple)
- **Supabase CLI**: `supabase link --project-ref <ref>` y luego usar el SQL Editor
  del dashboard o un cliente SQL con la connection string del dashboard
  (Project Settings → Database)

## 3) Consultas útiles

Listar usuarios y su perfil:

```sql
select u.id, u.email, u.email_confirmed_at, u.raw_app_meta_data->'roles' as roles, p.full_name
from auth.users u
left join public.profiles p on p.id = u.id
order by u.created_at desc;
```

Ver clases:

```sql
select id, title, discipline, status, start_time, end_time
from public.classes
order by created_at desc;
```

Ver inscripciones y pagos:

```sql
select p.id, p.amount, p.status, e.class_id, e.beneficiary_type, e.beneficiary_id
from public.payments p
join public.enrollments e on e.id = p.enrollment_id
order by p.created_at desc;
```

## 4) Atributos derivados de un usuario

La app obtiene los atributos del usuario (identidad, reservas, estado de
profesor, sede) con la función RPC:

```sql
select public.get_my_attributes();
```

(se evalúa en el contexto del usuario autenticado).

## 5) Revisar seguridad y performance

Tras cualquier cambio de schema, conviene revisar los *advisors* de Supabase
(seguridad y performance) desde el dashboard o vía MCP, para detectar tablas sin
RLS, políticas faltantes o índices recomendados.
