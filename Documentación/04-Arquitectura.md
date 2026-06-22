# Arquitectura

## Stack

- **Frontend**: Vue 3 (Composition API) + Vite + Tailwind CSS
- **Backend (BaaS)**: Supabase
  - PostgreSQL 16 como base de datos
  - PostgREST: API REST autogenerada sobre las tablas
  - Supabase Auth: autenticación (email/password + Google OAuth, JWT)
  - Supabase Storage: archivos (verificación de identidad, fotos de sedes, etc.)
  - Realtime: suscripciones a cambios (notificaciones)
- **Lógica de servidor**: Supabase Edge Functions (Deno + TypeScript)
- **Seguridad de datos**: Row Level Security (RLS) en cada tabla
- **Pagos**: MercadoPago Checkout Pro, integrado vía Edge Functions
- **Hosting frontend**: Vercel
- **CI/CD**: GitHub Actions

> **Nota histórica**: el proyecto nació con un backend Spring Boot (Java 21) y
> fue migrado a Supabase. El backend Java ya no existe en el repositorio; la
> lógica vive ahora en PostgreSQL (RLS, funciones, triggers, cron) y en Edge
> Functions.

## Estructura del Proyecto

```
modo-ensayo/
  frontend/                 # Vue 3 SPA
    src/
      components/           # Componentes reutilizables
      composables/          # Utilidades de Composition API (useToast, etc.)
      features/             # Módulos por dominio (auth, cart, classes, payments, reschedules)
      hooks/                # Hooks (useNotifications)
      layouts/              # Layout compartido
      pages/                # Vistas de acceso
      router/               # Vue Router con guards
      services/             # Clientes de dominio sobre el SDK de Supabase
      stores/               # Store de auth (singleton sobre Supabase Auth)
      utils/                # Utilidades (jwt, formato de fecha, validación RUT)
      views/                # Vistas por rol (alumno, profesor, sede, admin)
  supabase/
    migrations/             # Migraciones SQL versionadas (schema, RLS, cron, etc.)
    functions/              # Edge Functions (Deno + TypeScript)
    config.toml             # Configuración del proyecto y verify_jwt por función
  Documentación/            # Documentación del proyecto
  Producto/                 # Artefactos del producto
  Gestión/                  # Gestión del equipo
```

## Capa de servicios del frontend

`frontend/src/services/` contiene un módulo por dominio (`classService.js`,
`venueService.js`, `paymentService.js`, etc.). Todos importan el cliente
singleton de `services/supabase.js` y usan:

- **PostgREST** (`supabase.from('tabla')...`) para CRUD directo sujeto a RLS
- **Edge Functions** (`invokeFunction(...)`) para operaciones de negocio que
  requieren validación o privilegios elevados (crear clase, reservar cupo,
  confirmar clase, webhooks de pago, etc.)
- **RPC** (`supabase.rpc(...)`) para funciones de PostgreSQL como
  `get_my_attributes`

Para mantener compatibilidad con las vistas (escritas originalmente para el
backend Spring en camelCase), las respuestas de PostgREST/Edge Functions se
normalizan de snake_case a camelCase con el helper `camelize`.

## Autenticación

`frontend/src/stores/auth.js` es un singleton respaldado por **Supabase Auth**:

- `login` / `register` / `googleLogin` usan `supabase.auth.signInWithPassword`,
  `signUp` y `signInWithIdToken`
- La sesión (access token + refresh token) se sincroniza vía
  `supabase.auth.onAuthStateChange`, con refresh automático del token
- Los roles del usuario viajan en el claim `app_metadata.roles` del JWT
- Los atributos derivados (identidad validada, reservas activas, estado de
  profesor, etc.) se obtienen de la RPC `get_my_attributes`
- Mantiene el mismo contrato de `localStorage` (`auth_token`, `auth_user`,
  `modoActual`) que la versión Spring Boot, por lo que el router y las vistas
  no cambiaron

## Flujo de Datos

```
Usuario -> Frontend (Vue SPA en Vercel)
              |
              |-- PostgREST (CRUD con RLS) --> PostgreSQL (Supabase)
              |-- Supabase Auth (login/JWT) --> auth.users
              |-- Supabase Storage (archivos)
              |-- Realtime (notificaciones)
              |
              '-- Edge Functions (Deno) --> PostgreSQL (privilegios elevados)
                                              |
                                              '-- MercadoPago API (pagos)
```

## Principios de Diseño

1. **RLS como primera línea de defensa**: cada tabla tiene Row Level Security;
   el frontend usa la clave pública (anon/publishable) y sólo accede a lo que
   las políticas permiten.
2. **Edge Functions para lógica sensible**: operaciones que no deben confiarse
   al cliente (pagos, transiciones de estado, acciones de admin) corren en Edge
   Functions con la clave de servicio (`supabaseAdmin`).
3. **Lógica de negocio crítica en la base de datos**: reglas como liberación de
   pagos, control de capacidad y regeneración de bloques de horario se
   implementan con funciones, triggers y `pg_cron`.
4. **Enums + CHECK constraints** para todos los estados del sistema.
5. **Migraciones versionadas**: el schema se gestiona con migraciones SQL en
   `supabase/migrations/`; la base hosteada es la fuente de verdad y se
   sincroniza con la CLI.

## Patrones

- **Singleton** para el cliente de Supabase y el store de auth en el frontend
- **Row Level Security** por tabla en lugar de autorización en el servidor
- **JWT stateless** emitido por Supabase Auth
- **Adapter / normalización** (`camelize`) para compatibilidad de las vistas
