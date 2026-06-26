# Bugfix Requirements Document

## Introduction

Este bugfix corrige los **tres hallazgos CRÍTICO** del flujo de pagos retenidos de
MercadoPago identificados en el análisis de integración del sistema "Modo Ensayo"
(`Documentación/10-Analisis-Integracion.md`, capítulo 4 — Matriz de Brechas y
Riesgos). El sistema corre sobre Supabase (PostgreSQL 16, Edge Functions en Deno)
con frontend Vue 3. Los tres defectos comprometen el **ciclo de vida del dinero
del alumno**: hay caminos por los que un pago retenido nunca se devuelve y caminos
por los que el estado del dinero se puede mover sin autorización.

Bugs en alcance:

- **G-06 (CRÍTICO) — Reembolso no procesado.** La transición
  `REFUND_PENDING → REFUNDED` no está implementada en ningún componente. Tres vías
  marcan pagos como `REFUND_PENDING` (rechazo de profesor en `teacher-decision`,
  rechazo de alumno en `student-decision`, timeout de 48 h en el job
  `process_reschedule_timeouts`), pero ninguna función escribe
  `payments.status = 'REFUNDED'` ni invoca la API de reembolsos de MercadoPago.
  Todo pago que entra a `REFUND_PENDING` queda atrapado de forma terminal: el
  dinero del alumno nunca se devuelve. Existe infraestructura de destino sin usar
  (`refund_methods`, `profiles.preferred_refund_method_id`).
- **G-07 (CRÍTICO) — Clase suspendida deja pagos huérfanos en `RETAINED`.** En
  `confirm-class`, cuando se invoca con `realized = false` la clase pasa a
  `SUSPENDED` pero los pagos no se tocan (la única escritura
  `RETAINED → RELEASED` está anidada dentro de `if (body.realized)`). El dinero
  del alumno queda en `RETAINED` indefinidamente para una clase que no se realizó:
  ni se libera ni se encamina a reembolso.
- **G-16 (CRÍTICO) — Funciones `SECURITY DEFINER` privilegiadas ejecutables por
  `anon`/`authenticated` vía PostgREST RPC.** Varias funciones reservadas a
  `pg_cron`/`service_role` no revocan el `EXECUTE` de `PUBLIC`, por lo que
  cualquiera (incluso sin sesión) puede invocarlas vía `/rest/v1/rpc/...`. La más
  grave, `process_reschedule_timeouts()`, mueve pagos de otros usuarios a
  `REFUND_PENDING` y cierra reagendamientos.

**Contexto (fuera de alcance):** los hallazgos ALTO relacionados (G-05
idempotencia del webhook, G-08 estado `FAILED`, G-09 cancelación, G-10 condición
de carrera entre liberar y reembolsar) interactúan con estas correcciones pero
**no** forman parte de este bugfix. En particular, la corrección de G-07 produce
nuevos pagos en `REFUND_PENDING` que dependen de G-06 para cerrarse, y el cierre
del reembolso (G-06) debe considerar la serialización descrita en G-10, pero su
resolución completa se aborda por separado.

### Bug Conditions — C(X)

Para anclar las verificaciones de corrección (Fix Checking) y preservación
(Preservation Checking), cada bug se reduce a una condición de bug `C(X)` sobre
la entrada/estado, con `F` = comportamiento actual (defectuoso) y `F'` =
comportamiento corregido.

- **G-06** `C(pago) = (pago.status == 'REFUND_PENDING')` — el pago reconoce deuda
  con el alumno pero no tiene salida.
- **G-07** `C(req) = (req.realized == false AND existe pago RETAINED de la clase)`
  — clase suspendida con pago retenido sin reencaminar.
- **G-16** `C(llamada) = (rol_invocador ∈ {anon, authenticated} AND
  funcion ∈ {process_reschedule_timeouts, process_class_completion,
  regenerate_schedule_blocks, snapshot_system_metrics, check_rls_coverage})` —
  invocación RPC de función privilegiada por un rol no autorizado.

## Bug Analysis

### Current Behavior (Defect)

Lo que ocurre hoy cuando se dispara cada bug.

**G-06 — Reembolso no procesado**

1.1 WHEN un pago llega al estado `REFUND_PENDING` por cualquiera de las tres vías (rechazo de profesor en `teacher-decision`, rechazo de alumno en `student-decision`, o timeout de 48 h en `process_reschedule_timeouts`) THEN the system deja el pago atrapado de forma terminal en `REFUND_PENDING` y nunca lo transiciona a `REFUNDED`.

1.2 WHEN un pago está en `REFUND_PENDING` THEN the system nunca invoca la API de reembolsos de MercadoPago ni registra una devolución bancaria contra `refund_methods`/`profiles.preferred_refund_method_id`, por lo que el dinero del alumno no se devuelve.

**G-07 — Clase suspendida deja pagos huérfanos**

1.3 WHEN se invoca `confirm-class` con `realized = false` para una clase con pagos en `RETAINED` THEN the system marca la clase como `SUSPENDED` pero deja los pagos en `RETAINED` (la única escritura sobre `payments` está anidada en la rama `if (body.realized)`), dejando el dinero del alumno retenido indefinidamente para una clase que no se realizó.

**G-16 — Funciones privilegiadas ejecutables sin autorización**

1.4 WHEN un cliente con rol `anon` o `authenticated` invoca `process_reschedule_timeouts()` vía `/rest/v1/rpc/...` THEN the system ejecuta la función y mueve pagos `RETAINED → REFUND_PENDING` de otros usuarios, marca `reschedule_responses` como `TIMEOUT` y cierra reagendamientos como `COMPLETED`, manipulando estado de dinero sin autorización.

1.5 WHEN un cliente con rol `anon` o `authenticated` invoca vía RPC `process_class_completion()`, `regenerate_schedule_blocks()`, `snapshot_system_metrics()` o `check_rls_coverage()` THEN the system ejecuta esas operaciones batch reservadas a `pg_cron`/`service_role` porque no revocan el `EXECUTE` de `PUBLIC`.

### Expected Behavior (Correct)

Lo que debe ocurrir tras la corrección. Cada cláusula corresponde a la del mismo
índice en *Current Behavior*.

**G-06 — Reembolso no procesado**

2.1 WHEN un pago llega al estado `REFUND_PENDING` por cualquiera de las tres vías THEN the system SHALL procesar el cierre del reembolso y transicionar el pago a `REFUNDED`, de forma idempotente y dejando registro en `audit_logs`.

2.2 WHEN se procesa un pago en `REFUND_PENDING` THEN the system SHALL invocar la API de reembolsos de MercadoPago (o registrar la devolución bancaria vía `refund_methods`/`profiles.preferred_refund_method_id`) antes de marcarlo `REFUNDED`, de modo que el valor `REFUNDED` del enum sea alcanzable y el dinero efectivamente se encamine al alumno.

**G-07 — Clase suspendida deja pagos huérfanos**

2.3 WHEN se invoca `confirm-class` con `realized = false` para una clase con pagos en `RETAINED` THEN the system SHALL transicionar esos pagos `RETAINED → REFUND_PENDING`, notificar al alumno y registrar la operación en `audit_logs`, de modo que el dinero entre al circuito de reembolso (que se cierra mediante G-06).

**G-16 — Funciones privilegiadas ejecutables sin autorización**

2.4 WHEN un cliente con rol `anon` o `authenticated` intenta invocar `process_reschedule_timeouts()` vía RPC THEN the system SHALL denegar la ejecución (error de permiso), porque el `EXECUTE` debe estar revocado de `PUBLIC`, `anon` y `authenticated`.

2.5 WHEN un cliente con rol `anon` o `authenticated` intenta invocar vía RPC `process_class_completion()`, `regenerate_schedule_blocks()`, `snapshot_system_metrics()` o `check_rls_coverage()` THEN the system SHALL denegar la ejecución, dejando estas funciones ejecutables únicamente desde `pg_cron`/`service_role`.

### Unchanged Behavior (Regression Prevention)

Comportamiento existente que debe preservarse (para toda entrada que **no**
cumple `C(X)`, `F(X) = F'(X)`).

3.1 WHEN se invoca `confirm-class` con `realized = true` para una clase con pagos en `RETAINED` THEN the system SHALL CONTINUE TO transicionar esos pagos a `RELEASED` y marcar la clase `COMPLETED`, igual que hoy.

3.2 WHEN un profesor rechaza un reagendamiento, un alumno lo rechaza, o expira el timeout de 48 h THEN the system SHALL CONTINUE TO marcar los pagos `RETAINED` afectados como `REFUND_PENDING` (las tres vías aguas arriba no cambian; la corrección de G-06 actúa después de ese estado).

3.3 WHEN un pago ya está en `RELEASED`, `FAILED` o `REFUNDED` (es decir, no está en `REFUND_PENDING`) THEN the system SHALL CONTINUE TO dejar su estado sin cambios al ejecutarse el procesamiento de reembolsos.

3.4 WHEN el scheduler `pg_cron` o el `service_role` ejecutan `process_reschedule_timeouts()`, `process_class_completion()`, `regenerate_schedule_blocks()`, `snapshot_system_metrics()` o `check_rls_coverage()` THEN the system SHALL CONTINUE TO ejecutarlas con su lógica actual y producir los mismos efectos que hoy.

3.5 WHEN un usuario `authenticated` invoca las funciones RPC de negocio legítimas (p. ej. `get_my_attributes()`) THEN the system SHALL CONTINUE TO permitir su ejecución; la revocación de G-16 aplica solo a las funciones de jobs privilegiados.

3.6 WHEN MercadoPago notifica un pago aprobado al `mercadopago-webhook` y cuando se libera un pago de una clase realizada THEN the system SHALL CONTINUE TO crear el pago en `RETAINED` y liberarlo a `RELEASED` respectivamente, sin alteración por estas correcciones.

3.7 WHEN se evalúa el alcance de la revocación de G-16 THEN the system SHALL CONTINUE TO excluir `rls_auto_enable` (es función de event trigger, no invocable por RPC; falso positivo del análisis) y no se modifica como parte de este bugfix.
