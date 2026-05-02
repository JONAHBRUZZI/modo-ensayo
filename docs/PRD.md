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
- **Liberacion automatica** de pagos al completar la clase (trigger en BD)
- **Verificacion de identidad** de usuarios (documentos)
- **Gestion de reembolsos** con metodos configurados por usuario
- **Panel de administracion** con aprobacion de sedes, verificaciones de identidad, y gestion de roles

### Funcionalidades Futuras (post-MVP)

- Notificaciones en-app y por email
- Reagendamiento de clases
- Reportes y estadisticas
- Calificaciones/resenas de clases y profesores

## Roles

| Rol | Permisos |
|-----|---------|
| ADMIN | Gestion total de la plataforma, aprobacion de sedes/identidades, roles dinamicos |
| TEACHER | Publicar clases, gestionar sus sedes y salas |
| USER | Buscar y pagar clases, gestionar perfil, asociados, metodos de reembolso |

## Estados Operacionales

### Clase
`SCHEDULED -> IN_PROGRESS -> COMPLETED -> [archivada]`

### Pago
`RETAINED -> RELEASED` (al completar la clase)
`RETAINED -> REFUND_PENDING -> REFUNDED` (si se cancela)

### Sede / Identidad
`PENDING -> APPROVED | REJECTED`
