# Producto · Modo Ensayo

Artefactos entregables del producto y referencias al código fuente.

## Contenido de esta carpeta

| Recurso | Ubicación |
|---|---|
| Scripts de base de datos | [`scripts-bd/`](./scripts-bd/) |
| Capturas del sistema | [`capturas/`](./capturas/) |
| Credenciales de prueba | [`01-Credenciales-Prueba.md`](./01-Credenciales-Prueba.md) |

## Código fuente del producto

El código vive en las siguientes carpetas de la raíz del repo (mantenidas allí para no romper los pipelines de CI/CD):

| Componente | Ruta | Descripción |
|---|---|---|
| Backend | [`../backend/`](../backend/) | API REST Spring Boot 3.2 + Java 21 |
| Frontend | [`../frontend/`](../frontend/) | SPA Vue 3 + Vite + Tailwind |
| Infraestructura | [`../infra/`](../infra/) | Docker, Nginx, Terraform AWS, scripts SQL fuente |
| Scripts de despliegue | [`../scripts/`](../scripts/) | Helpers de operación y despliegue |
| Despliegue cloud | [`../deploy/`](../deploy/) | Configuración de despliegue en producción |

### Estructura del backend (package by feature)

```
backend/src/main/java/com/modoensayo/
  admin/         Panel de administración
  associates/    Gestión de asociados/familiares
  auth/          Autenticación, JWT, registro/login
  classes/       Clases, horarios, confirmaciones
  notifications/ Notificaciones in-app
  payments/      Carrito, MercadoPago, pagos retenidos
  reschedules/   Reagendamiento con timeout 48h
  reviews/       Reseñas y reputación
  upload/        Upload seguro a Cloudinary/Supabase
  users/         Perfil, roles, verificación identidad
  venues/        Sedes, salas, documentos
  shared/        Configuración, seguridad, excepciones
```

### Estructura del frontend (feature-sliced)

```
frontend/src/
  components/    Componentes reutilizables (EstadoBadge, ConfirmModal, etc.)
  features/      Módulos por dominio
  hooks/         Composables Vue 3
  layouts/       DefaultLayout (navbar + footer)
  pages/         Vistas/route components
  router/        Vue Router con guards (requiresAuth, requiresIdentity, etc.)
  services/      Axios services (api, classService, paymentService, etc.)
  stores/        Auth store (Pinia)
  views/         Vistas organizadas por contexto: alumno/, profesor/, sede/, admin/
```

## Scripts de Base de Datos

Los archivos en [`scripts-bd/`](./scripts-bd/) son copias de los scripts SQL fuente que viven en `../infra/postgres/init/`:

| Script | Descripción |
|---|---|
| `01_schema.sql` | DDL de todas las tablas, índices, constraints |
| `02_seed.sql` | Datos semilla iniciales (admin, roles, refund methods) |
| `03_procedures.sql` | Procedimientos almacenados y triggers de negocio |
| `04_venues_rooms_seed.sql` | Sedes y salas semilla (precondición MVP) |
| `05_reschedules_enhance.sql` | Mejoras a la tabla de reagendamientos |

Estos scripts se ejecutan automáticamente al levantar el contenedor PostgreSQL vía Docker Compose.

## Verificación del producto desplegado

Ver las credenciales y URLs operativas en [`01-Credenciales-Prueba.md`](./01-Credenciales-Prueba.md).

## Capturas del sistema

Capturas reales del producto desplegado en [`capturas/`](./capturas/), organizadas por contexto (alumno, maestro, sede, admin).
