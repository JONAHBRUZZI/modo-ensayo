# Payment Critical Fixes — Bugfix Design

## Overview

Este documento de diseño formaliza la corrección de los **tres hallazgos CRÍTICO**
del flujo de pagos retenidos de MercadoPago del sistema "Modo Ensayo" (Supabase
PostgreSQL 16, Edge Functions en Deno, frontend Vue 3), descritos en
`bugfix.md` y en `Documentación/10-Analisis-Integracion.md` (capítulo 4).

Los tres defectos comprometen el **ciclo de vida del dinero del alumno**:

- **G-06** — La transición `REFUND_PENDING → REFUNDED` no existe en ningún
  componente; todo pago que entra a `REFUND_PENDING` queda atrapado de forma
  terminal y el dinero nunca se devuelve.
- **G-07** — `confirm-class` con `realized = false` suspende la clase pero deja
  los pagos en `RETAINED` (la única escritura `RETAINED → RELEASED` está anidada
  dentro de `if (body.realized)`), dejando pagos huérfanos.
- **G-16** — Las funciones `SECURITY DEFINER` privilegiadas
  (`process_reschedule_timeouts`, `process_class_completion`,
  `regenerate_schedule_blocks`, `snapshot_system_metrics`, `check_rls_coverage`)
  son ejecutables por `anon`/`authenticated` vía PostgREST RPC porque no revocan
  el `EXECUTE` de `PUBLIC`.

La estrategia de corrección sigue la **metodología de condición de bug**: cada
defecto se reduce a una condición `C(X)` sobre la entrada/estado. La corrección
`F'` debe (a) **Fix Checking**: para toda entrada que cumple `C(X)`, producir el
comportamiento correcto `P(result)`; y (b) **Preservation Checking**: para toda
entrada que **no** cumple `C(X)`, producir el mismo resultado que el código
original `F` (`F(X) = F'(X)`).

El enfoque es **mínimo y dirigido**:

- **G-06** se resuelve con un **nuevo procesador de reembolsos** (Edge Function
  `process-refunds`, alineado con el patrón existente donde las llamadas a la API
  de MercadoPago viven en Edge Functions) que transiciona idempotentemente los
  pagos `REFUND_PENDING → REFUNDED` tras encaminar la devolución.
- **G-07** se resuelve **moviendo/duplicando** la lógica de pagos fuera de la
  rama `if (body.realized)` en `confirm-class`, encaminando los pagos `RETAINED`
  a `REFUND_PENDING` cuando `realized = false` (de donde los recoge G-06).
- **G-16** se resuelve con una **migración SQL** que revoca el `EXECUTE` de
  `PUBLIC`/`anon`/`authenticated` sobre las cinco funciones privilegiadas.

**Fuera de alcance** (hallazgos ALTO relacionados, no se modifican): G-04, G-05,
G-08, G-09, G-10. La corrección de G-07 produce nuevos pagos en `REFUND_PENDING`
que dependen de G-06 para cerrarse; el cierre del reembolso considera —pero no
resuelve— la condición de carrera de G-10.

## Glossary

- **Bug_Condition (C)**: La condición que dispara cada bug, definida formalmente
  por bug en la sección *Bug Details* (`isBugCondition`).
- **Property (P)**: El comportamiento correcto esperado cuando se cumple `C(X)`
  (`expectedBehavior`).
- **Preservation**: Comportamiento existente que debe permanecer inalterado para
  toda entrada que **no** cumple `C(X)` (`F(X) = F'(X)`).
- **F / F'**: Función/flujo original (defectuoso) vs. corregido.
- **`payments.status`** — enum `payment_status` con valores
  `RETAINED`, `RELEASED`, `REFUND_PENDING`, `REFUNDED`, `FAILED`
  (`supabase/migrations/20260619000100_enums.sql`). El ciclo del dinero: cobro
  retiene en `RETAINED`; clase realizada libera a `RELEASED`; rechazo/timeout
  marca `REFUND_PENDING`; el reembolso efectivo cierra en `REFUNDED`.
- **`confirm-class`** — Edge Function en
  `supabase/functions/confirm-class/index.ts` que recibe `{ classId, realized }`
  y, hoy, solo transiciona pagos cuando `realized = true`.
- **`process_reschedule_timeouts()`** — Función `SECURITY DEFINER` en
  `supabase/migrations/20260619000500_cron_functions.sql`, agendada por `pg_cron`
  cada hora; mueve pagos `RETAINED → REFUND_PENDING` al expirar un reagendamiento.
- **`teacher-decision` / `student-decision`** — Edge Functions que, al rechazar,
  marcan pagos `RETAINED → REFUND_PENDING`.
- **`refund_methods`** — Tabla con los datos bancarios de devolución del usuario
  (`user_id`, `bank`, `account_type`, `account_number`, `account_holder`, `rut`).
- **`profiles.preferred_refund_method_id`** — `uuid` (relación blanda, sin FK)
  que apunta al `refund_methods` preferido del usuario.
- **`payment_sessions.mercado_pago_payment_id`** — `text` con el id del pago de
  MercadoPago; es el único lugar donde se persiste el id MP (la tabla `payments`
  **no** lo guarda). Vincula un pago con su transacción MP vía `cart_snapshot`
  (`items[].classId`) y `owner_id`.
- **`audit_logs`** — Bitácora de auditoría (`actor_id`, `action`,
  `resource_type`, `resource_id`, `old_values`, `new_values`, `metadata`).
- **PostgREST RPC** — Endpoint `/rest/v1/rpc/<función>` que expone funciones SQL;
  toda función con `EXECUTE` para `PUBLIC` queda invocable por `anon`/`authenticated`.

## Bug Details

### Bug Condition — G-06 (Reembolso no procesado)

El bug se manifiesta para **todo pago que alcanza `REFUND_PENDING`**: el sistema
reconoce la deuda con el alumno pero no tiene ninguna salida implementada hacia
`REFUNDED`. No existe componente que invoque la API de reembolsos de MercadoPago
ni que registre la devolución bancaria.

**Formal Specification:**
```
FUNCTION isBugCondition_G06(pago)
  INPUT: pago of type payments row
  OUTPUT: boolean

  RETURN pago.status == 'REFUND_PENDING'
         AND NOT existsRefundProcessingFor(pago)   -- ningún componente lo cierra
END FUNCTION
```

`expectedBehavior` (P): el pago debe encaminarse a devolución (API MP o registro
bancario) y transicionar a `REFUNDED` de forma idempotente, con registro en
`audit_logs`.

### Bug Condition — G-07 (Clase suspendida deja pagos huérfanos)

El bug se manifiesta cuando se invoca `confirm-class` con `realized = false`
para una clase que tiene al menos un pago en `RETAINED`. La clase pasa a
`SUSPENDED` pero los pagos no se tocan (la única escritura sobre `payments` está
anidada en `if (body.realized)`).

**Formal Specification:**
```
FUNCTION isBugCondition_G07(req)
  INPUT: req = { classId, realized } de confirm-class
  OUTPUT: boolean

  RETURN req.realized == false
         AND EXISTS pago WHERE pago.enrollment.class_id == req.classId
                          AND pago.status == 'RETAINED'
END FUNCTION
```

`expectedBehavior` (P): esos pagos `RETAINED` deben transicionar a
`REFUND_PENDING`, notificar al alumno y registrarse en `audit_logs`, entrando al
circuito de reembolso que cierra G-06.

### Bug Condition — G-16 (Funciones privilegiadas ejecutables sin autorización)

El bug se manifiesta cuando un invocador con rol `anon` o `authenticated` llama
vía RPC a una de las cinco funciones privilegiadas, porque su `EXECUTE` no fue
revocado de `PUBLIC`.

**Formal Specification:**
```
FUNCTION isBugCondition_G16(llamada)
  INPUT: llamada = { rol_invocador, funcion }
  OUTPUT: boolean

  RETURN llamada.rol_invocador IN {'anon', 'authenticated'}
         AND llamada.funcion IN {
               'process_reschedule_timeouts',
               'process_class_completion',
               'regenerate_schedule_blocks',
               'snapshot_system_metrics',
               'check_rls_coverage'
             }
         AND hasExecuteFromPublic(llamada.funcion)   -- privilegio no revocado
END FUNCTION
```

`expectedBehavior` (P): la ejecución debe **denegarse** con error de permiso; las
funciones quedan ejecutables solo desde `pg_cron`/`service_role`.

### Examples

- **G-06**: Un alumno rechaza un reagendamiento (`student-decision`,
  `accepted=false`); su pago pasa a `REFUND_PENDING`. Hoy: el pago queda en
  `REFUND_PENDING` para siempre, sin devolución. Esperado: tras el procesamiento,
  el pago queda en `REFUNDED` y el dinero se encamina (API MP o banco).
- **G-06 (timeout)**: Expira el plazo de 48 h; `process_reschedule_timeouts()`
  marca el pago `REFUND_PENDING`. Hoy: atrapado. Esperado: `REFUNDED`.
- **G-07**: `confirm-class({classId, realized:false})` sobre una clase con un pago
  `RETAINED`. Hoy: clase `SUSPENDED`, pago sigue `RETAINED`. Esperado: clase
  `SUSPENDED`, pago `REFUND_PENDING`, alumno notificado, `audit_logs` registrado.
- **G-16**: `POST /rest/v1/rpc/process_reschedule_timeouts` con la `anon key`.
  Hoy: ejecuta y mueve pagos de otros usuarios a `REFUND_PENDING`. Esperado:
  error de permiso (`42501` / `permission denied for function`).
- **Edge case (G-06 idempotencia)**: ejecutar el procesador de reembolsos dos
  veces sobre el mismo pago. Esperado: la segunda ejecución no produce una
  segunda devolución ni cambia un pago ya `REFUNDED`.

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors (comportamiento que debe seguir funcionando igual):**

- `confirm-class` con `realized = true` sobre pagos `RETAINED` debe seguir
  transicionándolos a `RELEASED` y marcar la clase `COMPLETED`.
- Las tres vías aguas arriba que marcan `RETAINED → REFUND_PENDING`
  (`teacher-decision` rechazo, `student-decision` rechazo, timeout de 48 h en
  `process_reschedule_timeouts`) deben seguir marcando ese estado sin cambios.
- Los pagos ya en `RELEASED`, `FAILED` o `REFUNDED` (no `REFUND_PENDING`) no
  deben cambiar de estado al ejecutarse el procesamiento de reembolsos.
- `pg_cron`/`service_role` deben seguir ejecutando las cinco funciones
  privilegiadas con su lógica y efectos actuales.
- Las funciones RPC de negocio legítimas (p. ej. `get_my_attributes()`) deben
  seguir ejecutables por `authenticated`.
- El `mercadopago-webhook` debe seguir creando pagos en `RETAINED` y la
  liberación a `RELEASED` de clases realizadas debe permanecer intacta.
- `rls_auto_enable` (función de event trigger, no invocable por RPC; falso
  positivo del análisis) **no** se modifica.

**Scope:**
Toda entrada que **no** cumple la `C(X)` correspondiente debe quedar
completamente inalterada por estas correcciones. Esto incluye:
- Confirmaciones de clase con `realized = true` (no entra a G-07).
- Pagos que no están en `REFUND_PENDING` (no entra a G-06).
- Invocaciones de `pg_cron`/`service_role` y RPC de negocio legítimas (no entra
  a G-16).

> El comportamiento correcto positivo (lo que **sí** debe cambiar) está definido
> en la sección *Correctness Properties* (Propiedades 1–3).

## Hypothesized Root Cause

### G-06 — Reembolso no procesado

1. **Transición no implementada**: ningún Edge Function ni función SQL escribe
   `payments.status = 'REFUNDED'`. El valor `REFUNDED` del enum es inalcanzable.
2. **Sin integración con la API de reembolsos**: a diferencia del cobro
   (`mercadopago-create-preference`/`mercadopago-webhook`, que llaman a la API de
   MP), no existe ninguna llamada a `POST /v1/payments/{id}/refunds`.
3. **Infraestructura de destino sin consumir**: `refund_methods` y
   `profiles.preferred_refund_method_id` existen pero ningún flujo las lee.
4. **Id MP no accesible desde `payments`**: el id de pago MP solo vive en
   `payment_sessions.mercado_pago_payment_id`; falta un puente para reembolsar
   por API contra el pago concreto.

### G-07 — Clase suspendida deja pagos huérfanos

1. **Escritura de pagos mal anidada**: en `confirm-class`, el bucle que actualiza
   `payments` (`RETAINED → RELEASED`) está dentro de `if (body.realized)`. La
   rama `else` (implícita) solo escribe `classes.status = 'SUSPENDED'`.
2. **Sin reencaminamiento**: no hay ninguna rama que envíe los pagos `RETAINED`
   de una clase no realizada hacia `REFUND_PENDING`.

### G-16 — Funciones privilegiadas ejecutables sin autorización

1. **Falta de `REVOKE`**: las cinco funciones se crean con `CREATE OR REPLACE
   FUNCTION ... SECURITY DEFINER` sin `REVOKE EXECUTE ... FROM PUBLIC`. Por
   defecto, PostgreSQL otorga `EXECUTE` a `PUBLIC`, y PostgREST expone toda
   función ejecutable como RPC.
2. **Contraste con el patrón correcto**: `get_my_attributes()` tiene un `GRANT
   EXECUTE ... TO authenticated` explícito; las funciones de job nunca recibieron
   el tratamiento inverso (revocar de roles no privilegiados).

## Correctness Properties

Property 1: Bug Condition (G-06) — Cierre de reembolso `REFUND_PENDING → REFUNDED`

_For any_ pago donde la condición de bug G-06 se cumple
(`isBugCondition_G06` devuelve true: el pago está en `REFUND_PENDING`), el flujo
corregido SHALL encaminar la devolución (invocar la API de reembolsos de
MercadoPago o registrar la devolución bancaria vía
`refund_methods`/`profiles.preferred_refund_method_id`) y transicionar el pago a
`REFUNDED` de forma idempotente, dejando registro en `audit_logs`. Una segunda
ejecución sobre el mismo pago no produce una devolución adicional ni altera un
pago ya `REFUNDED`.

**Validates: Requirements 2.1, 2.2**

Property 2: Bug Condition (G-07) — Reencaminamiento de clase suspendida

_For any_ invocación de `confirm-class` donde la condición de bug G-07 se cumple
(`isBugCondition_G07` devuelve true: `realized == false` y existe pago `RETAINED`
de la clase), el flujo corregido SHALL transicionar esos pagos
`RETAINED → REFUND_PENDING`, marcar la clase `SUSPENDED`, notificar al alumno y
registrar la operación en `audit_logs`, de modo que el dinero entre al circuito
de reembolso (cerrado por la Propiedad 1).

**Validates: Requirements 2.3**

Property 3: Bug Condition (G-16) — Denegación de funciones privilegiadas

_For any_ invocación RPC donde la condición de bug G-16 se cumple
(`isBugCondition_G16` devuelve true: el rol invocador es `anon` o `authenticated`
y la función es una de las cinco privilegiadas), el flujo corregido SHALL denegar
la ejecución con error de permiso, dejando estas funciones ejecutables únicamente
desde `pg_cron`/`service_role`.

**Validates: Requirements 2.4, 2.5**

Property 4: Preservation — Liberación de clase realizada

_For any_ invocación de `confirm-class` donde la condición de bug G-07 NO se
cumple por `realized == true`, el flujo corregido SHALL producir el mismo
resultado que el original: transicionar los pagos `RETAINED → RELEASED` y marcar
la clase `COMPLETED`.

**Validates: Requirements 3.1**

Property 5: Preservation — Vías aguas arriba de `REFUND_PENDING`

_For any_ rechazo de profesor, rechazo de alumno o timeout de 48 h, el flujo
corregido SHALL CONTINUAR marcando los pagos `RETAINED` afectados como
`REFUND_PENDING` igual que hoy; la corrección de G-06 actúa solo después de ese
estado.

**Validates: Requirements 3.2**

Property 6: Preservation — Pagos fuera de `REFUND_PENDING`

_For any_ pago cuyo estado NO es `REFUND_PENDING` (`RELEASED`, `FAILED` o
`REFUNDED`) — es decir, donde `isBugCondition_G06` devuelve false — el
procesamiento de reembolsos SHALL dejar su estado sin cambios
(`F(X) = F'(X)`).

**Validates: Requirements 3.3**

Property 7: Preservation — Ejecución privilegiada y RPC de negocio

_For any_ invocación donde `isBugCondition_G16` devuelve false —ejecución desde
`pg_cron`/`service_role` de las cinco funciones, o invocación `authenticated` de
RPC de negocio legítimas como `get_my_attributes()`— el flujo corregido SHALL
CONTINUAR permitiendo la ejecución con los mismos efectos que hoy. `rls_auto_enable`
no se modifica.

**Validates: Requirements 3.4, 3.5, 3.7**

Property 8: Preservation — Creación y liberación del webhook

_For any_ notificación de pago aprobado al `mercadopago-webhook` y para la
liberación de pagos de clases realizadas, el flujo corregido SHALL CONTINUAR
creando el pago en `RETAINED` y liberándolo a `RELEASED` respectivamente, sin
alteración por estas correcciones.

**Validates: Requirements 3.6**

## Fix Implementation

### Cambios requeridos

Asumiendo que el análisis de causa raíz es correcto.

#### G-06 — Procesador de reembolsos (`REFUND_PENDING → REFUNDED`)

**Componente nuevo**: Edge Function `supabase/functions/process-refunds/index.ts`
(patrón consistente: las llamadas a la API de MercadoPago viven en Edge
Functions). Se invoca de forma privilegiada (`service_role`), agendada vía
`pg_cron` + `pg_net` con la misma cadencia que el resto de jobs de pagos.

**Cambios específicos**:
1. **Selección idempotente**: leer los pagos `WHERE status = 'REFUND_PENDING'`.
   Toda actualización de cierre se hace condicionada a `status = 'REFUND_PENDING'`
   (un `UPDATE ... WHERE status = 'REFUND_PENDING'` que afecta 0 filas en la
   segunda pasada garantiza idempotencia).
2. **Resolución del canal de reembolso** por pago:
   - Resolver el alumno vía `payments.enrollment_id → enrollments.student_id`.
   - Si el alumno tiene `profiles.preferred_refund_method_id` (→ `refund_methods`),
     **registrar la devolución bancaria** contra ese método.
   - En caso contrario, resolver el id MP vía `payment_sessions` (por `owner_id`
     del alumno y `cart_snapshot.items[].classId` de la clase) e invocar
     `POST https://api.mercadopago.com/v1/payments/{mp_payment_id}/refunds`
     con `Bearer ${MERCADOPAGO_ACCESS_TOKEN}`.
3. **Cierre del estado**: solo tras una devolución exitosa, ejecutar
   `UPDATE payments SET status = 'REFUNDED' WHERE id = $1 AND status = 'REFUND_PENDING'`.
4. **Auditoría**: insertar en `audit_logs` (`action = 'payment.refunded'`,
   `resource_type = 'payment'`, `resource_id = pago.id`, `metadata` con canal y
   referencia de la devolución).
5. **Manejo de fallo**: si la devolución falla, dejar el pago en `REFUND_PENDING`
   (no avanzar a `REFUNDED`) y registrar el error; será reintentado en la
   siguiente pasada.

> Nota: el puente pago → id MP es indirecto (el id MP solo está en
> `payment_sessions`). El canal bancario vía `refund_methods` es el camino
> primario cuando el alumno tiene método preferido; el canal API MP es el
> respaldo. La condición de carrera con `confirm-class` (G-10) se reconoce pero
> su serialización completa queda fuera de alcance.

#### G-07 — `confirm-class` reencamina pagos de clase no realizada

**Archivo**: `supabase/functions/confirm-class/index.ts`
**Función**: handler `fetch` de `confirm-class`.

**Cambios específicos**:
1. **Sacar la escritura de pagos de la rama `if (body.realized)`** y bifurcar:
   - `realized == true` → pagos `RETAINED → RELEASED` (comportamiento actual).
   - `realized == false` → pagos `RETAINED → REFUND_PENDING`.
2. **Notificación al alumno** en el caso `realized == false` (insertar en
   `notifications`, tipo coherente con las vías existentes, p. ej.
   `CLASS_SUSPENDED`).
3. **Auditoría**: la inserción en `audit_logs` ya existe
   (`class.confirmed_not_realized`); enriquecer `metadata` con el número de pagos
   reencaminados a `REFUND_PENDING`.
4. **Idempotencia**: el `UPDATE ... WHERE status = 'RETAINED'` solo afecta pagos
   aún retenidos, evitando re-disparos.
5. **Preservación**: la rama `realized == true` no se modifica funcionalmente.

#### G-16 — Revocar `EXECUTE` de funciones privilegiadas

**Componente nuevo**: migración SQL
`supabase/migrations/<timestamp>_revoke_privileged_functions.sql`.

**Cambios específicos**:
1. Por cada función privilegiada, revocar el `EXECUTE` de los roles no
   autorizados:
   ```sql
   REVOKE EXECUTE ON FUNCTION public.process_reschedule_timeouts()   FROM PUBLIC, anon, authenticated;
   REVOKE EXECUTE ON FUNCTION public.process_class_completion()      FROM PUBLIC, anon, authenticated;
   REVOKE EXECUTE ON FUNCTION public.regenerate_schedule_blocks()    FROM PUBLIC, anon, authenticated;
   REVOKE EXECUTE ON FUNCTION public.snapshot_system_metrics()       FROM PUBLIC, anon, authenticated;
   REVOKE EXECUTE ON FUNCTION public.check_rls_coverage()            FROM PUBLIC, anon, authenticated;
   ```
2. **No** alterar `get_my_attributes()` ni otras RPC de negocio (mantienen su
   `GRANT EXECUTE TO authenticated`).
3. **No** tocar `rls_auto_enable` (event trigger, no invocable por RPC).
4. `pg_cron`/`service_role` retienen ejecución (no se revoca de esos roles; el
   propietario y `service_role` conservan el privilegio).
5. La migración es idempotente (`REVOKE` sobre un privilegio ya ausente es
   no-op).

## Testing Strategy

### Validation Approach

El enfoque sigue dos fases: primero **exponer contraejemplos** que demuestren el
bug sobre el código sin corregir (confirmar/refutar la causa raíz); luego
verificar que la corrección funciona (**Fix Checking**) y preserva el
comportamiento existente (**Preservation Checking**).

### Exploratory Bug Condition Checking

**Goal**: Exponer contraejemplos que demuestren cada bug ANTES de implementar la
corrección. Confirmar o refutar el análisis de causa raíz. Si se refuta, hay que
re-hipotetizar.

**Test Plan**: escribir pruebas que ejerzan cada `C(X)` y observar el fallo sobre
el código sin corregir.

**Test Cases**:
1. **G-06 — Reembolso nunca ocurre**: dado un pago en `REFUND_PENDING`, verificar
   que ningún componente lo lleva a `REFUNDED` (no existe procesador). Sobre el
   código actual, no hay transición posible (fallará/quedará atrapado).
2. **G-07 — Pago huérfano**: invocar `confirm-class({realized:false})` sobre una
   clase con pago `RETAINED`; aserción: el pago queda en `REFUND_PENDING`. Sobre
   el código actual el pago sigue `RETAINED` (falla).
3. **G-16 — RPC privilegiada accesible**: invocar
   `process_reschedule_timeouts()` con rol `anon`/`authenticated`; aserción:
   error de permiso. Sobre el código actual ejecuta sin error (falla) y produce
   efectos de estado de dinero.
4. **Edge case — Idempotencia G-06**: ejecutar el procesador dos veces; aserción:
   sin doble devolución. (No aplicable hasta implementar; documenta el riesgo.)

**Expected Counterexamples**:
- G-06: ningún pago alcanza `REFUNDED`; el valor del enum es inalcanzable.
- G-07: `payments.status` permanece `RETAINED` tras suspender la clase.
- G-16: la RPC privilegiada responde 200/ejecuta para roles no autorizados.
- Posibles causas confirmadas: transición no implementada (G-06), escritura mal
  anidada (G-07), `EXECUTE` no revocado de `PUBLIC` (G-16).

### Fix Checking

**Goal**: Verificar que para toda entrada que cumple `C(X)`, el flujo corregido
produce el comportamiento esperado `P(result)`.

**Pseudocode:**
```
-- G-06
FOR ALL pago WHERE isBugCondition_G06(pago) DO
  processRefunds()
  ASSERT pago.status == 'REFUNDED'
  ASSERT refundChannelInvoked(pago)        -- API MP o registro bancario
  ASSERT auditLogged('payment.refunded', pago)
END FOR

-- G-07
FOR ALL req WHERE isBugCondition_G07(req) DO
  result := confirmClass_fixed(req)
  ASSERT classStatus(req.classId) == 'SUSPENDED'
  ASSERT FORALL p IN retainedPaymentsOf(req.classId): p.status == 'REFUND_PENDING'
  ASSERT studentNotified(req.classId) AND auditLogged('class.confirmed_not_realized', req)
END FOR

-- G-16
FOR ALL llamada WHERE isBugCondition_G16(llamada) DO
  ASSERT invokeRpc(llamada) raises permission_denied
END FOR
```

### Preservation Checking

**Goal**: Verificar que para toda entrada que **no** cumple `C(X)`, el flujo
corregido produce el mismo resultado que el original (`F(X) = F'(X)`).

**Pseudocode:**
```
-- G-07 / G-06 preservación de pagos
FOR ALL input WHERE NOT isBugCondition_G07(input) DO
  ASSERT confirmClass_original(input) == confirmClass_fixed(input)
END FOR

FOR ALL pago WHERE NOT isBugCondition_G06(pago) DO   -- status != 'REFUND_PENDING'
  ASSERT processRefunds_effectOn(pago) == NO_CHANGE
END FOR

-- G-16 preservación de ejecución autorizada
FOR ALL llamada WHERE NOT isBugCondition_G16(llamada) DO
  ASSERT invokeRpc_original(llamada) == invokeRpc_fixed(llamada)
END FOR
```

**Testing Approach**: las pruebas basadas en propiedades (PBT) se recomiendan
para Preservation Checking porque generan muchos casos sobre el dominio de
entrada y capturan casos límite que las pruebas manuales podrían omitir,
ofreciendo garantías fuertes de que el comportamiento no cambió para entradas no
bug. Para Preservation, observar primero el comportamiento sobre el código sin
corregir y luego escribir las pruebas que lo capturan.

**Test Cases**:
1. **`confirm-class` realized=true**: observar que pagos `RETAINED → RELEASED` y
   clase `COMPLETED` sobre el código actual; verificar que se mantiene tras el fix.
2. **Pagos fuera de `REFUND_PENDING`**: generar pagos en `RELEASED`/`FAILED`/
   `REFUNDED` y verificar que el procesador de reembolsos no los altera.
3. **Vías aguas arriba**: rechazo de profesor/alumno y timeout siguen marcando
   `REFUND_PENDING`.
4. **Ejecución privilegiada legítima**: `service_role`/`pg_cron` siguen
   ejecutando las cinco funciones; `authenticated` sigue ejecutando
   `get_my_attributes()`.
5. **Webhook**: notificación aprobada sigue creando pago `RETAINED`.

### Unit Tests

- `confirm-class`: ramas `realized=true` (→`RELEASED`/`COMPLETED`) y
  `realized=false` (→`REFUND_PENDING`/`SUSPENDED` + notificación + auditoría).
- `process-refunds`: cierre `REFUND_PENDING → REFUNDED` por canal bancario y por
  canal API MP; idempotencia; manejo de fallo (permanece `REFUND_PENDING`).
- Migración G-16: `REVOKE` aplicado a las cinco funciones; `get_my_attributes`
  intacta.

### Property-Based Tests

- **Idempotencia G-06**: para cualquier multiconjunto de pagos en `REFUND_PENDING`,
  ejecutar el procesador N veces produce exactamente una devolución por pago y
  estado final `REFUNDED` (Propiedad 1).
- **Preservación G-06**: para cualquier pago con estado ≠ `REFUND_PENDING`, el
  procesador no cambia su estado (Propiedad 6).
- **Preservación G-07**: para cualquier `confirm-class` con `realized=true`, el
  resultado coincide con el original (Propiedad 4).
- **G-16**: para cualquier combinación (rol no privilegiado × función
  privilegiada), la invocación RPC es denegada; para (`service_role`/`pg_cron` ×
  función) o (`authenticated` × RPC de negocio), se permite (Propiedades 3, 7).

### Integration Tests

- **Flujo completo de reembolso**: rechazo de alumno → `REFUND_PENDING` → ejecutar
  `process-refunds` → `REFUNDED` + `audit_logs`, end-to-end.
- **Clase suspendida → reembolso**: `confirm-class(realized=false)` →
  `REFUND_PENDING` → `process-refunds` → `REFUNDED` (encadenamiento G-07 + G-06).
- **Seguridad RPC**: invocar las cinco funciones con `anon key` y `authenticated
  key` (denegadas) vs. `service_role`/cron (permitidas); verificar que no se
  alteró estado de dinero por la vía no autorizada.
