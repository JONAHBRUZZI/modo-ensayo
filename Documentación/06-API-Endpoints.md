# API / Superficie de acceso a datos

Tras la migración a Supabase ya no existe un backend REST propio bajo `/api`.
El frontend accede a los datos por tres vías:

1. **Supabase Auth** — autenticación (login, registro, OAuth)
2. **PostgREST** — CRUD directo sobre las tablas, protegido por RLS
3. **Edge Functions** — lógica de negocio sensible y privilegiada

Todas las llamadas viajan con el JWT de Supabase; las políticas RLS deciden qué
puede ver o modificar cada usuario.

---

## 1. Autenticación (Supabase Auth)

No son endpoints REST propios: se invocan mediante el SDK
(`frontend/src/stores/auth.js`).

| Acción | SDK | Descripción |
|--------|-----|-------------|
| Registro | `supabase.auth.signUp` | Crea usuario; trigger `handle_new_user` crea su `profiles` |
| Login | `supabase.auth.signInWithPassword` | Email/password, retorna sesión JWT |
| Login Google | `supabase.auth.signInWithIdToken` | OAuth con ID token de Google |
| Logout | `supabase.auth.signOut` | Cierra sesión |
| Refresh | `supabase.auth.refreshSession` | Renueva el access token |

Los roles viajan en el claim `app_metadata.roles` del JWT.

---

## 2. Acceso a datos vía PostgREST

CRUD directo con `supabase.from('<tabla>')`, sujeto a las políticas RLS de cada
tabla. Ejemplos representativos (la autorización real la imponen las políticas):

| Tabla | Operaciones típicas | Quién (según RLS) |
|-------|---------------------|-------------------|
| `profiles` | `select` / `update` propio | Dueño del perfil |
| `classes` | `select` publicadas | Público / autenticado |
| `discipline_catalog` | `select` | Público |
| `venues` / `rooms` | `select` aprobadas | Público |
| `associates` | `select` / `insert` / `delete` | Dueño |
| `notifications` | `select` / `update` (leído) | Destinatario |
| `reviews` | `select` públicas | Público |
| `refund_methods` | CRUD propio | Dueño |

Funciones RPC (PostgreSQL) invocadas con `supabase.rpc(...)`:

| RPC | Descripción |
|-----|-------------|
| `get_my_attributes` | Atributos derivados del usuario (identidad, reservas, estado de profesor, sede, etc.) |

---

## 3. Edge Functions (lógica de negocio)

Invocadas con `supabase.functions.invoke(name)` (helper `invokeFunction`).
Definidas en `supabase/functions/`. `verify_jwt` se configura por función en
`supabase/config.toml` (los webhooks van sin verificación de JWT).

| Función | Propósito |
|---------|-----------|
| `create-class` | Crear/publicar una clase (validaciones de negocio) |
| `book-slot` | Reservar un cupo en una clase |
| `assign-reserva` | Asignar una reserva a una clase |
| `confirm-class` | Confirmar la realización de una clase |
| `propose-reschedule` | Proponer una reprogramación |
| `teacher-decision` | Decisión del profesor sobre reprogramación |
| `student-decision` | Decisión del alumno sobre reprogramación |
| `generate-blocks` | Generar bloques de horario |
| `create-review` | Crear una reseña |
| `register-venue` | Registrar una sede |
| `admin-approve-venue` | Aprobar/rechazar sede (admin) |
| `admin-stats` | Estadísticas de la plataforma (admin) |
| `admin-users` | Gestión de usuarios y roles (admin) |
| `mercadopago-create-preference` | Crear preferencia de pago (inscripción a clases) |
| `mercadopago-webhook` | Webhook de notificaciones de pago (sin JWT); discrimina entre inscripción y reserva de sala |
| `mp-connect-start` | Inicia OAuth de MercadoPago Connect; genera state anti-CSRF y devuelve URL de autorización |
| `mp-connect-callback` | Callback OAuth (sin JWT); valida state, canjea code→tokens y guarda cuenta del vendedor |
| `reserve-room-preference` | Crea preferencia de arriendo de sala con split automático a la cuenta MercadoPago de la sede |

---

## 4. Storage

Archivos (verificación de identidad, fotos de sedes, etc.) se suben con
`supabase.storage` a través de `frontend/src/services/uploadService.js`, que
mapea cada tipo lógico a su bucket y construye el path. El acceso a los buckets
también está regido por políticas.

---

> **Nota**: la tabla histórica de endpoints REST `/api/...` del backend Spring
> Boot quedó obsoleta con la migración. Este documento refleja la superficie
> actual basada en Supabase.
