# Implementation Plan

## Overview

Este plan sigue el flujo exploratorio de bugfix: primero se escriben pruebas que
**exponen** cada bug (deben FALLAR sobre el código sin corregir), luego pruebas de
**preservación** que capturan el comportamiento que NO debe cambiar (deben PASAR
sobre el código sin corregir), después la **implementación** de las correcciones, y
finalmente la **verificación** de que las correcciones funcionan sin regresiones.

Las tres correcciones del flujo de pagos retenidos: **G-06** (reembolso no
procesado), **G-07** (clase suspendida deja pagos huérfanos) y **G-16** (funciones
privilegiadas ejecutables sin autorización).

## Task Dependency Graph

```json
{
  "waves": [
    {
      "wave": 1,
      "tasks": ["1", "2", "3", "4"],
      "description": "Pruebas previas: exploratorias (1-3, deben FALLAR) y preservación (4, debe PASAR) sobre el código sin corregir"
    },
    {
      "wave": 2,
      "tasks": ["5", "6", "7"],
      "description": "Correcciones independientes: G-06, G-07 y G-16 (incluyen su verificación 5.2/6.2/7.2)"
    },
    {
      "wave": 3,
      "tasks": ["8"],
      "description": "Re-ejecutar pruebas de preservación tras las correcciones (sin regresiones)"
    },
    {
      "wave": 4,
      "tasks": ["9"],
      "description": "Checkpoint final: toda la suite verde, incluido el flujo end-to-end G-07 -> G-06"
    }
  ]
}
```

- Las tareas 1–4 (pruebas) deben completarse antes de cualquier corrección.
- Las correcciones 5, 6 y 7 son independientes entre sí, aunque G-07 alimenta el circuito que cierra G-06.
- Las verificaciones 5.2/6.2/7.2 dependen de su corrección respectiva.
- La tarea 8 (preservación) depende de las tres correcciones; la tarea 9 (checkpoint) depende de todo lo anterior.

## Tasks

- [x] 1. Escribir prueba exploratoria de la condición de bug G-06 (reembolso no procesado)
  - **Property 1: Bug Condition** - Cierre de reembolso `REFUND_PENDING → REFUNDED`
  - **CRITICAL**: Esta prueba DEBE FALLAR sobre el código sin corregir — el fallo confirma que el bug existe
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: Esta prueba codifica el comportamiento esperado — validará la corrección cuando pase tras implementarla
  - **GOAL**: Exponer contraejemplos que demuestren que ningún componente cierra el reembolso
  - **Scoped PBT Approach**: Generar pagos en estado `REFUND_PENDING` (por cualquiera de las tres vías: rechazo de profesor, rechazo de alumno, timeout 48 h) y aseverar que tras el procesamiento el pago queda en `REFUNDED`
  - Condición de bug (de design): `isBugCondition_G06(pago) = (pago.status == 'REFUND_PENDING' AND NOT existsRefundProcessingFor(pago))`
  - Comportamiento esperado a aseverar (de design, Expected Behavior): el pago se encamina a devolución (API MP o registro bancario) y transiciona a `REFUNDED` de forma idempotente, con registro en `audit_logs`
  - Ejecutar la prueba sobre el código SIN corregir
  - **EXPECTED OUTCOME**: La prueba FALLA — no existe procesador que lleve el pago a `REFUNDED`; el valor `REFUNDED` del enum es inalcanzable
  - Documentar el contraejemplo (p. ej. "un pago `REFUND_PENDING` permanece atrapado; ningún flujo escribe `REFUNDED`")
  - Marcar la tarea completa cuando la prueba esté escrita, ejecutada y el fallo documentado
  - _Requirements: 1.1, 1.2, 2.1, 2.2_

- [x] 2. Escribir prueba exploratoria de la condición de bug G-07 (clase suspendida deja pagos huérfanos)
  - **Property 2: Bug Condition** - Reencaminamiento de clase suspendida
  - **CRITICAL**: Esta prueba DEBE FALLAR sobre el código sin corregir — el fallo confirma que el bug existe
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: Esta prueba codifica el comportamiento esperado — validará la corrección cuando pase tras implementarla
  - **GOAL**: Exponer el contraejemplo del pago huérfano en `RETAINED`
  - **Scoped PBT Approach**: Para una clase con al menos un pago `RETAINED`, invocar `confirm-class({ classId, realized: false })` y aseverar el estado resultante de los pagos
  - Condición de bug (de design): `isBugCondition_G07(req) = (req.realized == false AND EXISTS pago WHERE pago.enrollment.class_id == req.classId AND pago.status == 'RETAINED')`
  - Comportamiento esperado a aseverar (de design, Expected Behavior): los pagos `RETAINED` transicionan a `REFUND_PENDING`, la clase queda `SUSPENDED`, se notifica al alumno y se registra en `audit_logs`
  - Ejecutar la prueba sobre el código SIN corregir
  - **EXPECTED OUTCOME**: La prueba FALLA — la clase queda `SUSPENDED` pero el pago permanece en `RETAINED` (la escritura está anidada en `if (body.realized)`)
  - Documentar el contraejemplo (p. ej. "`confirm-class({realized:false})` deja `payments.status = 'RETAINED'`")
  - Marcar la tarea completa cuando la prueba esté escrita, ejecutada y el fallo documentado
  - _Requirements: 1.3, 2.3_

- [x] 3. Escribir prueba exploratoria de la condición de bug G-16 (funciones privilegiadas ejecutables sin autorización)
  - **Property 3: Bug Condition** - Denegación de funciones privilegiadas
  - **CRITICAL**: Esta prueba DEBE FALLAR sobre el código sin corregir — el fallo confirma que el bug existe
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: Esta prueba codifica el comportamiento esperado — validará la corrección cuando pase tras implementarla
  - **GOAL**: Exponer que las cinco funciones privilegiadas son invocables por roles no autorizados vía PostgREST RPC
  - **Scoped PBT Approach**: Para cada combinación (rol `anon`/`authenticated` × las cinco funciones privilegiadas), invocar `/rest/v1/rpc/<función>` y aseverar que se deniega con error de permiso
  - Condición de bug (de design): `isBugCondition_G16(llamada) = (llamada.rol_invocador IN {anon, authenticated} AND llamada.funcion IN {process_reschedule_timeouts, process_class_completion, regenerate_schedule_blocks, snapshot_system_metrics, check_rls_coverage} AND hasExecuteFromPublic(llamada.funcion))`
  - Comportamiento esperado a aseverar (de design, Expected Behavior): la ejecución se deniega con error de permiso (`42501` / `permission denied for function`)
  - Ejecutar la prueba sobre el código SIN corregir
  - **EXPECTED OUTCOME**: La prueba FALLA — la RPC privilegiada ejecuta (200) para roles no autorizados y, en el caso de `process_reschedule_timeouts`, mueve pagos a `REFUND_PENDING`
  - Documentar el contraejemplo (p. ej. "`POST /rest/v1/rpc/process_reschedule_timeouts` con `anon key` ejecuta y altera estado de dinero")
  - Marcar la tarea completa cuando la prueba esté escrita, ejecutada y el fallo documentado
  - _Requirements: 1.4, 1.5, 2.4, 2.5_

- [x] 4. Escribir pruebas de preservación (ANTES de implementar las correcciones)
  - **Property 2: Preservation** - Comportamiento existente que NO debe cambiar
  - **IMPORTANT**: Seguir la metodología de observación primero (observation-first)
  - Observar y registrar el comportamiento sobre el código SIN corregir para entradas que NO cumplen `C(X)`, luego escribir pruebas basadas en propiedades que capturen esos patrones observados
  - **Property 4: Preservation — Liberación de clase realizada**: observar que `confirm-class({realized:true})` transiciona pagos `RETAINED → RELEASED` y marca la clase `COMPLETED`; escribir PBT que aseveren este resultado para toda invocación con `realized == true` _(Requirements: 3.1)_
  - **Property 5: Preservation — Vías aguas arriba de `REFUND_PENDING`**: observar que rechazo de profesor (`teacher-decision`), rechazo de alumno (`student-decision`) y timeout de 48 h (`process_reschedule_timeouts`) siguen marcando pagos `RETAINED → REFUND_PENDING`; escribir pruebas que lo aseveren _(Requirements: 3.2)_
  - **Property 6: Preservation — Pagos fuera de `REFUND_PENDING`**: para cualquier pago en `RELEASED`/`FAILED`/`REFUNDED` (donde `isBugCondition_G06` es false), aseverar que el procesamiento de reembolsos NO cambia su estado (`F(X) = F'(X)`) _(Requirements: 3.3)_
  - **Property 7: Preservation — Ejecución privilegiada y RPC de negocio**: observar que `service_role`/`pg_cron` ejecutan las cinco funciones y que `authenticated` ejecuta RPC de negocio legítimas (`get_my_attributes()`); aseverar que se siguen permitiendo; confirmar que `rls_auto_enable` no se modifica _(Requirements: 3.4, 3.5, 3.7)_
  - **Property 8: Preservation — Creación y liberación del webhook**: observar que `mercadopago-webhook` crea pagos en `RETAINED` y que las clases realizadas liberan a `RELEASED`; aseverar que sigue igual _(Requirements: 3.6)_
  - Property-based testing genera muchos casos sobre el dominio de entrada para garantías más fuertes de no-regresión
  - Ejecutar las pruebas sobre el código SIN corregir
  - **EXPECTED OUTCOME**: Las pruebas PASAN — esto confirma el comportamiento base a preservar
  - Marcar la tarea completa cuando las pruebas estén escritas, ejecutadas y pasando sobre el código sin corregir
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

- [x] 5. Corrección G-06 — Procesador de reembolsos (`REFUND_PENDING → REFUNDED`)

  - [x] 5.1 Implementar la Edge Function `process-refunds`
    - Crear `supabase/functions/process-refunds/index.ts` (patrón consistente: las llamadas a la API de MercadoPago viven en Edge Functions); invocación privilegiada (`service_role`), agendada vía `pg_cron` + `pg_net`
    - Selección idempotente: leer pagos `WHERE status = 'REFUND_PENDING'`; todo cierre condicionado a `status = 'REFUND_PENDING'` (un `UPDATE ... WHERE status = 'REFUND_PENDING'` que afecta 0 filas en la segunda pasada garantiza idempotencia)
    - Resolución del canal de reembolso por pago: resolver el alumno vía `payments.enrollment_id → enrollments.student_id`; si tiene `profiles.preferred_refund_method_id` (→ `refund_methods`), registrar devolución bancaria; en caso contrario, resolver el id MP vía `payment_sessions` (`owner_id` + `cart_snapshot.items[].classId`) e invocar `POST https://api.mercadopago.com/v1/payments/{mp_payment_id}/refunds` con `Bearer ${MERCADOPAGO_ACCESS_TOKEN}`
    - Cierre del estado: solo tras devolución exitosa, `UPDATE payments SET status = 'REFUNDED' WHERE id = $1 AND status = 'REFUND_PENDING'`
    - Auditoría: insertar en `audit_logs` (`action = 'payment.refunded'`, `resource_type = 'payment'`, `resource_id = pago.id`, `metadata` con canal y referencia)
    - Manejo de fallo: si la devolución falla, dejar el pago en `REFUND_PENDING` (no avanzar a `REFUNDED`) y registrar el error para reintento en la siguiente pasada
    - _Bug_Condition: isBugCondition_G06(pago) = (pago.status == 'REFUND_PENDING' AND NOT existsRefundProcessingFor(pago))_
    - _Expected_Behavior: encaminar la devolución (API MP o registro bancario) y transicionar a `REFUNDED` idempotentemente, con registro en `audit_logs`_
    - _Preservation: pagos fuera de `REFUND_PENDING` no se alteran (Property 6); vías aguas arriba intactas (Property 5)_
    - _Requirements: 2.1, 2.2_

  - [x] 5.2 Verificar que la prueba exploratoria G-06 ahora pasa
    - **Property 1: Expected Behavior** - Cierre de reembolso `REFUND_PENDING → REFUNDED`
    - **IMPORTANT**: Re-ejecutar la MISMA prueba de la tarea 1 — NO escribir una prueba nueva
    - La prueba de la tarea 1 codifica el comportamiento esperado; cuando pasa, confirma que la corrección funciona (incluida la idempotencia)
    - Ejecutar la prueba exploratoria de la tarea 1
    - **EXPECTED OUTCOME**: La prueba PASA (confirma que el bug está corregido)
    - _Requirements: 2.1, 2.2_

- [x] 6. Corrección G-07 — `confirm-class` reencamina pagos de clase no realizada

  - [x] 6.1 Implementar el reencaminamiento en `confirm-class`
    - Archivo: `supabase/functions/confirm-class/index.ts`, handler `fetch`
    - Sacar la escritura de pagos de la rama `if (body.realized)` y bifurcar: `realized == true` → pagos `RETAINED → RELEASED` (comportamiento actual, sin cambios funcionales); `realized == false` → pagos `RETAINED → REFUND_PENDING`
    - Notificación al alumno en el caso `realized == false` (insertar en `notifications`, tipo coherente con las vías existentes, p. ej. `CLASS_SUSPENDED`)
    - Auditoría: enriquecer el `metadata` del `audit_logs` existente (`class.confirmed_not_realized`) con el número de pagos reencaminados a `REFUND_PENDING`
    - Idempotencia: el `UPDATE ... WHERE status = 'RETAINED'` solo afecta pagos aún retenidos, evitando re-disparos
    - _Bug_Condition: isBugCondition_G07(req) = (req.realized == false AND EXISTS pago RETAINED de la clase)_
    - _Expected_Behavior: pagos `RETAINED → REFUND_PENDING`, clase `SUSPENDED`, alumno notificado, registro en `audit_logs`; el dinero entra al circuito de reembolso (cerrado por G-06)_
    - _Preservation: la rama `realized == true` no cambia (Property 4)_
    - _Requirements: 2.3_

  - [x] 6.2 Verificar que la prueba exploratoria G-07 ahora pasa
    - **Property 2: Expected Behavior** - Reencaminamiento de clase suspendida
    - **IMPORTANT**: Re-ejecutar la MISMA prueba de la tarea 2 — NO escribir una prueba nueva
    - Ejecutar la prueba exploratoria de la tarea 2
    - **EXPECTED OUTCOME**: La prueba PASA (confirma que el bug está corregido)
    - _Requirements: 2.3_

- [x] 7. Corrección G-16 — Revocar `EXECUTE` de funciones privilegiadas

  - [x] 7.1 Crear la migración SQL de revocación
    - Crear `supabase/migrations/<timestamp>_revoke_privileged_functions.sql`
    - Por cada función privilegiada, revocar el `EXECUTE` de los roles no autorizados:
      `REVOKE EXECUTE ON FUNCTION public.process_reschedule_timeouts() FROM PUBLIC, anon, authenticated;`
      `REVOKE EXECUTE ON FUNCTION public.process_class_completion() FROM PUBLIC, anon, authenticated;`
      `REVOKE EXECUTE ON FUNCTION public.regenerate_schedule_blocks() FROM PUBLIC, anon, authenticated;`
      `REVOKE EXECUTE ON FUNCTION public.snapshot_system_metrics() FROM PUBLIC, anon, authenticated;`
      `REVOKE EXECUTE ON FUNCTION public.check_rls_coverage() FROM PUBLIC, anon, authenticated;`
    - NO alterar `get_my_attributes()` ni otras RPC de negocio (mantienen su `GRANT EXECUTE TO authenticated`)
    - NO tocar `rls_auto_enable` (event trigger, no invocable por RPC)
    - `pg_cron`/`service_role` retienen ejecución (no se revoca de esos roles; propietario y `service_role` conservan el privilegio)
    - La migración es idempotente (`REVOKE` sobre un privilegio ya ausente es no-op)
    - _Bug_Condition: isBugCondition_G16(llamada) = (rol IN {anon, authenticated} AND funcion IN {las 5 privilegiadas} AND hasExecuteFromPublic(funcion))_
    - _Expected_Behavior: denegar la ejecución con error de permiso; ejecutables solo desde `pg_cron`/`service_role`_
    - _Preservation: ejecución privilegiada y RPC de negocio legítimas siguen permitidas; `rls_auto_enable` sin cambios (Property 7)_
    - _Requirements: 2.4, 2.5_

  - [x] 7.2 Verificar que la prueba exploratoria G-16 ahora pasa
    - **Property 3: Expected Behavior** - Denegación de funciones privilegiadas
    - **IMPORTANT**: Re-ejecutar la MISMA prueba de la tarea 3 — NO escribir una prueba nueva
    - Ejecutar la prueba exploratoria de la tarea 3
    - **EXPECTED OUTCOME**: La prueba PASA (las cinco funciones se deniegan para `anon`/`authenticated`)
    - _Requirements: 2.4, 2.5_

- [x] 8. Verificar que las pruebas de preservación siguen pasando
  - **Property 2: Preservation** - Comportamiento existente sin regresiones
  - **IMPORTANT**: Re-ejecutar las MISMAS pruebas de la tarea 4 — NO escribir pruebas nuevas
  - Ejecutar las pruebas de preservación de la tarea 4 (Properties 4, 5, 6, 7, 8)
  - **EXPECTED OUTCOME**: Las pruebas PASAN (confirma que no hay regresiones)
  - Confirmar que todas las pruebas siguen pasando tras las correcciones
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

- [x] 9. Checkpoint - Asegurar que todas las pruebas pasan
  - Ejecutar la suite completa: pruebas exploratorias (tareas 1–3, ahora en verde), pruebas de preservación (tarea 4) y pruebas de integración del flujo completo de reembolso
  - Verificar el encadenamiento G-07 + G-06: `confirm-class(realized=false)` → `REFUND_PENDING` → `process-refunds` → `REFUNDED`
  - Asegurar que todas las pruebas pasan; consultar al usuario si surgen dudas
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

## Notes

- **Orden estricto**: las pruebas (1–4) van ANTES de cualquier corrección. Las exploratorias (1–3) deben FALLAR sobre el código sin corregir; las de preservación (4) deben PASAR sobre el código sin corregir.
- **No tocar las pruebas al verificar**: en 5.2/6.2/7.2 y 8 se re-ejecutan las MISMAS pruebas escritas antes, sin reescribirlas.
- **Encadenamiento G-07 → G-06**: la corrección de G-07 genera pagos en `REFUND_PENDING` que cierra el procesador de G-06; el checkpoint valida el flujo end-to-end.
- **Fuera de alcance**: G-04, G-05, G-08, G-09, G-10 (hallazgos ALTO relacionados) no se modifican en este bugfix.
- **Comandos de prueba**: usar ejecución única (p. ej. `--run`) en lugar de modo watch; ejecutar los servidores/funciones localmente de forma manual si se requiere.
