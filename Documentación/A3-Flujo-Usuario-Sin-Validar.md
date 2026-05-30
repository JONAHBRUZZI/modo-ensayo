# USUARIO SIN VALIDAR - Modo Ensayo

## Estado: Registrado, identidad NO validada
## Roles: USER
## Interruptores: [Alumno]

---

## Registro

| Campo | Validacion |
|-------|-----------|
| Nombre Completo | Requerido |
| Email | Requerido, type=email, unico |
| Telefono | Opcional, type=tel |
| Contrasena | Requerida, min 8 chars, 1 mayuscula + 1 minuscula + 1 numero |
| Terminos | Requerido (checkbox Ley 19.628) |

Al registrarse redirige a `/alumno/dashboard` (Mi Espacio).

---

## Pantallas disponibles

### Navbar
```
Inicio | Cronograma | Mi Espacio | Mis Clases | Asociados | Pagos | Carrito
```

### Dashboard (Mi Espacio)
- Stats: Clases Tomadas, Proximas Clases
- Identidad: "No solicitada" (badge azul)
- Accesos: Mis Clases, Historial de Pagos, Asociados

### Cronograma (/classes)
- Busqueda de clases con filtros: TODAS, disciplina, nivel, comuna, precio max
- Detalle de clase con opcion "Agregar al Carrito"
- Seleccion de beneficiario al agregar (yo o asociado)

### Carrito (/cart)
- Items con clase, beneficiario, precio
- Eliminar items
- Total consolidado
- Checkout (pago simulado)

### Pagos (/alumno/pagos)
- Historial de pagos realizados

### Mis Clases (/alumno/mis-clases)
- Clases donde el usuario esta inscrito

### Asociados (/alumno/asociados)
- Formulario: Nombre*, Relacion*, Fecha Nacimiento*, RUT* (validado modulo 11), Email (opcional)
- Listado con nombre, relacion, fecha, RUT
- Eliminacion con confirmacion explicita

### Perfil (/profile)
- Editar datos personales
- Cambiar contrasena (actual + nueva)
- Metodo de devolucion preferido

---

## Acciones BLOQUEADAS (requieren identidad validada)

| Accion | Mensaje |
|--------|---------|
| Registrar sede | "Debes validar tu identidad antes de registrar una sede" |
| Agendar sala / Crear clase | "Debes validar tu identidad antes de crear clases" |
| Ver interruptor Maestro | No aparece (requiere TEACHER + identidad validada) |
| Ver interruptor Mi Sede | No aparece (requiere VENUE_ADMIN + identidad validada) |

---

## Verificacion de Identidad (/profile/identity)

Formulario completo para solicitar validacion:

| Campo | Validacion |
|-------|-----------|
| Tipo documento | Requerido (RUT / Pasaporte) |
| Numero documento | Requerido, RUT con auto-formato XX.XXX.XXX-X y modulo 11 |
| Nombre completo | Requerido (segun documento) |
| Fecha nacimiento | Requerida, min 14 anos, no futura |
| Archivo | Requerido, JPG/PNG/PDF, max 5MB |

Estados: PENDING (amarillo) → APPROVED (verde) → aparece interruptor Maestro
         PENDING (amarillo) → REJECTED (rojo) → puede corregir y reenviar

---

## Que cambia al validar identidad

Una vez que el Administrador General APRUEBA la identidad:
1. Aparece el boton "Maestro" en el interruptor
2. Puede buscar salas y crear clases propias
3. Puede registrar una sede (SEDE o HOME_STUDIO)
4. Al crear su primera clase → auto-asignacion de rol TEACHER (R12)
5. Si registra sede aprobada → aparece boton "Mi Sede"

---

## Notificaciones
- Campanita en header con badge de no leidas
- Notificaciones in-app: sede aprobada, identidad validada, clase reagendada, pago liberado
