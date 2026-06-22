# Producto · Modo Ensayo

Artefactos entregables del producto y referencias al código fuente.

## Contenido de esta carpeta

| Recurso | Ubicación |
|---|---|
| Scripts de base de datos | [`scripts-bd/`](./scripts-bd/) |
| Capturas del sistema | [`capturas/`](./capturas/) |
| Credenciales de prueba | [`01-Credenciales-Prueba.md`](./01-Credenciales-Prueba.md) |

## Código fuente del producto

El código vive en las siguientes carpetas de la raíz del repo:

| Componente | Ruta | Descripción |
|---|---|---|
| Frontend | [`../frontend/`](../frontend/) | SPA Vue 3 + Vite + Tailwind |
| Backend (BaaS) | [`../supabase/`](../supabase/) | Migraciones SQL + Edge Functions (Supabase) |

> **Nota**: el backend original (Spring Boot + Java) fue migrado a Supabase y ya
> no existe en el repositorio. La lógica de negocio vive ahora en PostgreSQL
> (RLS, funciones, triggers, cron) y en Edge Functions (Deno).

### Estructura del backend Supabase

```
supabase/
  migrations/    Migraciones SQL versionadas (schema, RLS, cron, realtime, seed)
  functions/     Edge Functions (Deno): create-class, book-slot, confirm-class,
                 mercadopago-webhook, admin-stats, etc.
  config.toml    Configuración del proyecto y verify_jwt por función
```

### Estructura del frontend (feature-sliced)

```
frontend/src/
  components/    Componentes reutilizables (EstadoBadge, ConfirmModal, etc.)
  composables/   Utilidades de Composition API (useToast, useTheme, etc.)
  features/      Módulos por dominio (auth, cart, classes, payments, reschedules)
  hooks/         Hooks (useNotifications)
  layouts/       DefaultLayout (navbar + footer)
  router/        Vue Router con guards (requiresAuth, requiresIdentity, etc.)
  services/      Clientes de dominio sobre el SDK de Supabase (classService, etc.)
  stores/        Auth store (singleton sobre Supabase Auth)
  views/         Vistas organizadas por contexto: alumno/, profesor/, sede/, admin/
```

## Base de datos

El schema se gestiona con migraciones SQL versionadas en
[`../supabase/migrations/`](../supabase/migrations/). La base hosteada de
Supabase es la fuente de verdad; los cambios se sincronizan con la CLI
(`supabase db push` / `supabase migration fetch`).

## Verificación del producto desplegado

Ver las credenciales y URLs operativas en [`01-Credenciales-Prueba.md`](./01-Credenciales-Prueba.md).

## Capturas del sistema

Capturas reales del producto desplegado en [`capturas/`](./capturas/), organizadas por contexto (alumno, maestro, sede, admin).
