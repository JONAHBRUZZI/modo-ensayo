# Historias de Usuario · Modo Ensayo

> **Versión:** 1.0 — Sprint 0 actualizado al 30-may-2026
> **Total:** 22 historias de usuario distribuidas por rol

Cada historia sigue el formato: **Como** [rol] **quiero** [acción] **para** [valor]. Incluye criterios de aceptación verificables al cierre de cada sprint.

---

## Roles del sistema

| Rol | Sigla | Descripción |
|---|---|---|
| Alumno | A | Usuario titular que reserva clases para sí mismo o sus asociados |
| Maestro Independiente | MI | Profesor que crea sus propias clases en salas de cualquier sede |
| Maestro Dependiente | MD | Profesor asignado por una Sede a clases específicas |
| Admin de Sede | AS | Administrador de una sede que gestiona sus salas y clases |
| Admin General | AG | Administrador del sistema completo |

---

## Épica 1 — Registro, identidad y perfil

### HU01 — Registro de cuenta
**Como** persona interesada
**quiero** crear una cuenta con correo, RUT y contraseña
**para** acceder a la plataforma como Alumno por defecto.

**Criterios de aceptación:**
- Correo y RUT son únicos en el sistema (HTTP 409 si duplicados).
- Contraseña tiene mínimo 8 caracteres.
- Al registrarme recibo automáticamente el rol `USER`.
- Tras el registro soy redirigido a mi Dashboard de Alumno.

### HU02 — Inicio de sesión
**Como** usuario registrado
**quiero** iniciar sesión con correo y contraseña
**para** acceder a mi cuenta.

**Criterios de aceptación:**
- El login emite un JWT con duración de 24 horas.
- Si las credenciales son incorrectas, recibo error 401 sin detalles ("Credenciales inválidas").
- El JWT contiene mi ID, email y roles actuales.

### HU03 — Validar mi identidad
**Como** usuario registrado
**quiero** subir mi documento de identidad para validación
**para** poder operar como Maestro o Admin de Sede.

**Criterios de aceptación:**
- Puedo subir un archivo (PDF, JPG, PNG) máximo 10MB.
- Mi estado pasa a `PENDING` y el Admin General lo revisa.
- Si es aprobado, mi `identidadValidada` es `true` permanentemente.
- Si es rechazado, recibo el motivo y puedo reintentar.
- Mi documento no puede estar aprobado en otra cuenta (R05).

### HU04 — Editar mi perfil profesional (Maestro)
**Como** Maestro
**quiero** completar mi perfil profesional (biografía, disciplinas, formación, redes)
**para** que los alumnos me conozcan y se inscriban a mis clases.

**Criterios de aceptación:**
- El sistema considera mi perfil "completo" con biografía + disciplina principal.
- Mientras esté incompleto veo un banner amarillo persistente en el contexto Maestro.
- Tras la primera reserva de sala, soy redirigido a este formulario con un banner de bienvenida.
- Puedo agregar disciplinas secundarias (checkbox múltiple) y tipos de formación.

---

## Épica 2 — Búsqueda y reserva de clases

### HU05 — Buscar clases por disciplina
**Como** Alumno
**quiero** buscar clases por disciplina (danza, música), nivel, ubicación, precio y fecha
**para** encontrar la clase que me interesa.

**Criterios de aceptación:**
- Los filtros aplican simultáneamente (AND).
- Los resultados muestran título, foto, sede, fecha, precio y reputación del Maestro.
- Solo se muestran clases `PUBLISHED` con cupos disponibles.

### HU06 — Ver detalle de clase
**Como** Alumno
**quiero** ver el detalle completo de una clase (descripción, sede, Maestro, requisitos)
**para** decidir si me inscribo.

**Criterios de aceptación:**
- Veo: título, descripción, disciplina, nivel, capacidad, cupos disponibles, precio, fecha, duración, sede, sala, Maestro con su reputación.
- Veo botón "Agregar al carrito" con selector de beneficiario.

### HU07 — Agregar clase al carrito para mí o asociado
**Como** Alumno
**quiero** agregar una clase al carrito eligiendo "para mí" o "para mi asociado X"
**para** inscribir a quien corresponda.

**Criterios de aceptación:**
- Si ya estoy inscrito o ya el asociado está inscrito, el sistema lo impide (R10).
- Si la clase llegó a su capacidad, el sistema lo impide (R02).
- Veo el carrito acumulado con el total a pagar.

### HU08 — Pagar el carrito completo con MercadoPago
**Como** Alumno
**quiero** pagar mi carrito completo en una sola transacción MercadoPago
**para** inscribir a varias personas a varias clases con un solo cobro.

**Criterios de aceptación:**
- El botón "Pagar" solicita confirmación explícita "¿Confirma su pago de $X?" (R14).
- Soy redirigido a MercadoPago (sandbox en dev).
- Tras el pago, mi carrito queda vacío y veo una página de éxito con detalle de inscripciones.
- Las inscripciones quedan creadas y los pagos en `RETAINED` (R01).

### HU09 — Ver mis clases inscritas y cancelar
**Como** Alumno
**quiero** ver mis clases inscritas (próximas, pasadas, canceladas) y poder cancelar las próximas
**para** gestionar mi agenda.

**Criterios de aceptación:**
- Veo las clases con estado de inscripción y de pago.
- Si la clase aún no ha comenzado, puedo cancelar mi inscripción.
- Al cancelar, recibo aviso de devolución manual y mi pago pasa a `REFUND_PENDING`.
- Si la clase ya inició, la cancelación está deshabilitada.

---

## Épica 3 — Gestión de Maestro

### HU10 — Reservar sala
**Como** Maestro
**quiero** reservar una sala de una sede para una fecha y hora
**para** dictar mi clase allí.

**Criterios de aceptación:**
- Veo el calendario de disponibilidad de la sala.
- Al confirmar la reserva, se crea un `DRAFT` con la sala asignada.
- Si no tenía rol `TEACHER`, se me asigna automáticamente (R08).
- Soy redirigido a "Clases por Asignar" para configurar la clase.

### HU11 — Crear borrador de clase
**Como** Maestro
**quiero** preparar borradores de clase con sus características (título, disciplina, precio, capacidad)
**para** tenerlas listas para asignarles una sala cuando la reserve.

**Criterios de aceptación:**
- Puedo crear borradores sin sala asignada.
- Veo mis borradores en una lista y puedo editarlos o eliminarlos.
- Necesito tener identidad validada para crear borradores (R04).

### HU12 — Asignar sala a borrador o crear clase nueva
**Como** Maestro
**quiero** para cada reserva de sala elegir entre "crear clase nueva" o "usar borrador existente"
**para** publicar mi clase eficientemente.

**Criterios de aceptación:**
- En "Clases por Asignar" veo cada reserva con los dos botones.
- "Usar Borrador" abre modal con mis borradores sin sala.
- Al asignar, el borrador toma la sala/horario de la reserva y se publica.
- El draft placeholder de la reserva se elimina.

### HU13 — Ver mis clases agendadas y dictarlas
**Como** Maestro
**quiero** ver mi calendario de clases agendadas
**para** prepararme y dictarlas.

**Criterios de aceptación:**
- Veo mis clases con fecha, hora, sala, lista de alumnos inscritos.
- Cuando es el día de la clase, puedo marcar asistencia individual de cada alumno.

### HU14 — Recibir pagos de clases realizadas
**Como** Maestro
**quiero** ver mis pagos retenidos y liberados acumulados por mes
**para** saber mi ingreso esperado y real.

**Criterios de aceptación:**
- Veo un dashboard de pagos con: total retenido, total liberado este mes, total liberado acumulado.
- Los pagos se liberan automáticamente cuando el Admin de Sede confirma `REALIZADA` (R13).

---

## Épica 4 — Gestión de Sede

### HU15 — Registrar mi sede con sus salas y fotos
**Como** Admin de Sede
**quiero** registrar mi sede (datos, dirección, fotos, documentos legales) y sus salas (capacidad, tipo de piso, espejos, sonido)
**para** ofrecer mis espacios a Maestros y Alumnos.

**Criterios de aceptación:**
- Necesito identidad validada (R04).
- Mi sede queda en estado `PENDIENTE` esperando aprobación de Admin General (R06).
- Puedo subir mínimo 3 fotos por sala.

### HU16 — Crear clase asignada a un Maestro Dependiente
**Como** Admin de Sede
**quiero** crear clases que asigno a un Maestro mío
**para** ofrecer clases con mis propios Maestros.

**Criterios de aceptación:**
- La clase queda con `tipoClase = ASIGNADA`.
- El Maestro asignado puede dictarla pero no decidir su reagendamiento (R18).
- Al publicar, los Alumnos la ven en búsqueda como una clase normal.

### HU17 — Confirmar clases realizadas o no realizadas
**Como** Admin de Sede
**quiero** ver "Clases por Confirmar" y marcar cada una como REALIZADA o NO_REALIZADA
**para** activar la liberación o devolución de pagos.

**Criterios de aceptación:**
- Veo todas las clases en mi sede que han terminado pero no están confirmadas.
- Cada acción requiere confirmación explícita (R14).
- `REALIZADA` libera los pagos al Maestro (R01, R13).
- `NO_REALIZADA` deja los pagos en `REFUND_PENDING` para devolución (R13).

---

## Épica 5 — Reagendamiento

### HU18 — Proponer reagendamiento de mi clase
**Como** Maestro o Admin de Sede (según R18)
**quiero** proponer un reagendamiento cuando una clase no se puede realizar en su fecha
**para** mover los alumnos a una fecha alternativa.

**Criterios de aceptación:**
- El sistema me sugiere fechas según agenda real de la sala (R15).
- Al confirmar la propuesta, los alumnos reciben notificación con contador 48h (R16).

### HU19 — Aceptar o rechazar reagendamiento como Alumno
**Como** Alumno con clase reagendada
**quiero** decidir individualmente si acepto la nueva fecha o pido devolución
**para** mantener mi inscripción o recuperar mi dinero.

**Criterios de aceptación:**
- Veo la notificación con contador visible.
- La decisión requiere confirmación explícita (R14).
- Si acepto, mi inscripción se mueve a la nueva fecha y mi pago sigue retenido.
- Si rechazo, mi pago pasa a `REFUND_PENDING` según mi método preferido.
- Si no decido en 48h, se interpreta como rechazo automáticamente (R16).

---

## Épica 6 — Reputación

### HU20 — Evaluar mi clase tras realizarse
**Como** Alumno con clase realizada
**quiero** evaluar la clase (1-5 estrellas + comentario)
**para** ayudar a otros alumnos y al Maestro.

**Criterios de aceptación:**
- Solo puedo evaluar clases con estado `REALIZADA` o `COMPLETED`.
- Veo "Reseñas Pendientes" con las clases por evaluar.
- Mi evaluación afecta el score promedio del Maestro y la Sede.

---

## Épica 7 — Administración General

### HU21 — Aprobar sedes e identidades pendientes
**Como** Admin General
**quiero** ver y procesar sedes pendientes e identidades pendientes
**para** activar Maestros y Sedes en el sistema.

**Criterios de aceptación:**
- Veo un panel con dos listas: identidades pendientes y sedes pendientes.
- Cada acción requiere confirmación explícita (R14).
- Aprobar o rechazar dispara notificación al usuario afectado.

### HU22 — Gestionar usuarios (suspender, asignar admin)
**Como** Admin General
**quiero** ver todos los usuarios y poder asignarles rol `ADMIN` o suspenderlos
**para** mantener el control de la plataforma.

**Criterios de aceptación:**
- Veo lista de usuarios con búsqueda por email/nombre.
- Puedo asignar/revocar rol `ADMIN`.
- Puedo suspender (deshabilitar login) a un usuario problemático.

---

## Trazabilidad con sprints

| HU | Sprint en que se entregó | Estado |
|---|---|---|
| HU01, HU02 | Sprint 1 | ✓ |
| HU03, HU04, HU15 | Sprint 2 | ✓ |
| HU05, HU06, HU07, HU10, HU11, HU16 | Sprint 3 | ✓ |
| HU08, HU09 | Sprint 4 | ✓ |
| HU12, HU13, HU14, HU17 | Sprint 5 | ✓ |
| HU18, HU19 | Sprint 6 (80% adelantado) | ► |
| HU21, HU22 | Sprint 7 (60% adelantado) | ► |
| HU20 | Sprint 8 (50% adelantado) | ► |
