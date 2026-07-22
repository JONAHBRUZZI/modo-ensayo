# PRD

## Producto

Modo Ensayo es una plataforma para la gestion de clases artisticas (danza, teatro, musica, etc.) con un sistema de **pagos condicionados**. El alumno paga al reservar, pero el profesor solo recibe el pago cuando la clase se completa efectivamente.

## Problema

Los profesores freelance de artes escenicas pierden dinero cuando los alumnos cancelan a ultima hora. Las plataformas actuales no ofrecen un mecanismo de retencion de pagos condicionado a la asistencia real.

## MVP

### Funcionalidades Core

- **Registro y autenticacion** de usuarios con roles (ADMIN, TEACHER, USER)
- **Gestion de sedes y salas** con atributos artisticos (tipo de piso, espejos, sonido, barras de ballet, iluminacion, color de pared)
- **Publicacion de clases** con disciplina, capacidad, precio, horario
- **Carrito de compras** para agregar clases con beneficiarios (asociados)
- **Pago con MercadoPago Checkout Pro** (sandbox en dev)
- **Sistema de pagos retenidos**: pago queda en estado RETAINED hasta que la clase se completa
- **Liberacion de pagos** al completar la clase (Edge Function `confirm-class`, no un trigger de BD — ver `02-Reglas-de-Negocio.md` R01/R13)
- **Verificacion de identidad** de usuarios (documentos)
- **Gestion de reembolsos** siempre via la API de MercadoPago (no via metodos configurados por el usuario — ver R09, corregida)
- **Panel de administracion** con aprobacion de sedes, verificaciones de identidad, gestion de roles y panel de pagos/giros (`/admin/pagos`)

### Funcionalidades Futuras (post-MVP) ⚠️ ya implementadas, quedaron desactualizadas

> Esta lista describía funcionalidades como pendientes al momento de escribir
> este PRD. Todas ya están implementadas (ver `11-Mejoras-Incorporadas.md`):

- ~~Notificaciones en-app y por email~~ → notificaciones in-app implementadas (`notifications`, campana); email no implementado
- ~~Reagendamiento de clases~~ → implementado end-to-end (`02-Reglas-de-Negocio.md` R16, R16.1, R16.2)
- ~~Reportes y estadisticas~~ → implementado (`admin-stats`, `admin-metrics`, dashboards por rol)
- ~~Calificaciones/resenas de clases y profesores~~ → implementado (`create-review`, tabla `reviews`)

## Roles

| Rol | Permisos |
|-----|---------|
| ADMIN | Gestion total de la plataforma, aprobacion de sedes/identidades, roles dinamicos |
| TEACHER | Publicar clases, gestionar sus sedes y salas |
| USER | Buscar y pagar clases, gestionar perfil, asociados, metodos de reembolso |

## Estados Operacionales

### Clase

> ⚠️ El enum real `class_status` no tiene `SCHEDULED` — es `PUBLISHED`. Estado
> real (ver `05-Modelo-de-Datos.md` §8):

`DRAFT -> PUBLISHED -> IN_PROGRESS -> POR_VALIDAR -> COMPLETED | SUSPENDED`
(más `CANCELLED` — hoy inalcanzable en el código, ver R17 — y `FULL`, cupo lleno).

### Pago
`RETAINED -> RELEASED` (al completar la clase)
`RETAINED -> REFUND_PENDING -> REFUNDED` (si se cancela)

### Sede / Identidad
`PENDING -> APPROVED | REJECTED`
