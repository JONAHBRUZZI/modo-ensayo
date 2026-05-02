# API Endpoints

Base URL: `/api`

## Auth

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | Public | Registrar nuevo usuario |
| POST | `/api/auth/login` | Public | Iniciar sesion (retorna JWT) |

## Users

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/users/me` | JWT | Obtener perfil propio |
| PUT | `/api/users/me` | JWT | Actualizar perfil propio |
| GET | `/api/users/me/refund-methods` | JWT | Listar metodos de reembolso |
| POST | `/api/users/me/refund-methods` | JWT | Agregar metodo de reembolso |
| DELETE | `/api/users/me/refund-methods/{id}` | JWT | Eliminar metodo de reembolso |
| POST | `/api/users/me/identity` | JWT | Subir verificacion de identidad |
| GET | `/api/users/me/identity` | JWT | Ver estado de verificacion |

## Associates

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/associates` | JWT | Listar asociados del usuario |
| POST | `/api/associates` | JWT | Crear asociado |
| DELETE | `/api/associates/{id}` | JWT | Eliminar asociado |

## Classes

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/classes` | Public | Listar clases publicadas |
| GET | `/api/classes/{id}` | Public | Detalle de clase |
| POST | `/api/classes` | TEACHER/ADMIN | Publicar nueva clase |
| PATCH | `/api/classes/{id}/status` | TEACHER/ADMIN | Cambiar estado de clase |

## Payments / Cart

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/payments/cart` | JWT | Ver items del carrito |
| POST | `/api/payments/cart` | JWT | Agregar clase al carrito |
| DELETE | `/api/payments/cart/{id}` | JWT | Quitar item del carrito |
| POST | `/api/payments/checkout` | JWT | Iniciar checkout |
| POST | `/api/payments/mercadopago/create-preference` | JWT | Crear preferencia de pago MP |

## Venues

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/venues` | Public | Listar sedes aprobadas |
| POST | `/api/venues` | JWT | Registrar nueva sede |
| GET | `/api/venues/{id}` | Public | Detalle de sede |
| GET | `/api/venues/{id}/rooms` | Public | Salas de una sede |
| POST | `/api/venues/rooms` | TEACHER/ADMIN | Registrar nueva sala |

## Admin

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/admin/roles` | ADMIN | Listar roles disponibles |
| GET | `/api/admin/users` | ADMIN | Listar todos los usuarios |
| POST | `/api/admin/users/{id}/roles` | ADMIN | Asignar rol a usuario |
| DELETE | `/api/admin/users/{id}/roles/{role}` | ADMIN | Revocar rol de usuario |
| GET | `/api/admin/identity-verifications` | ADMIN | Listar verificaciones pendientes |
| PATCH | `/api/admin/identity-verifications/{id}` | ADMIN | Aprobar/rechazar verificacion |
| GET | `/api/admin/venues/pending` | ADMIN | Listar sedes pendientes |
| PATCH | `/api/admin/venues/{id}/status` | ADMIN | Aprobar/rechazar sede |

## Upload

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/upload` | JWT | Subir archivo (multipart/form-data) |
