# Preguntas Técnicas de Defensa — Parte de Jonathan (Lógica de Negocio + Backend/Edge Functions)

Guía de estudio para la defensa oral. Cada respuesta está verificada contra el código real del repositorio
(no son respuestas genéricas) — se cita el archivo exacto para que puedas abrirlo en vivo si te piden
mostrarlo. Corresponde a la sección "Preguntas técnicas" generada previamente en la sesión.

---

## 1. Pagos condicionados y ciclo del dinero

### 1. ¿Por qué el pago queda en `RETAINED` y no se libera directamente al confirmar la inscripción? ¿Qué evento lo pasa a `RELEASED`?
El pago queda `RETAINED` porque el riesgo de negocio no es "que paguen", es "que la clase no se dicte" —
si se liberara al inscribirse, el profesor cobraría aunque cancele o no aparezca. El único evento que lo
pasa a `RELEASED` es que el **Admin de Sede confirme la clase como realizada** vía la Edge Function
`confirm-class` (`body.realized = true`), que recorre las inscripciones `ACTIVE` de la clase y hace
`UPDATE payments SET status = 'RELEASED' WHERE enrollment_id = e.id AND status = 'RETAINED'`.
→ `supabase/functions/confirm-class/index.ts`

### 2. En `confirm-class`, si la sede confirma la clase, ¿qué pasa con los `room_schedule_blocks`? ¿Por qué se liberan a `AVAILABLE` en vez de eliminarse?
Se actualizan de `OCCUPIED` a `AVAILABLE` con `class_id: null`, no se borran, porque el bloque de horario
es un recurso reutilizable de la sala (una franja horaria semanal), no un objeto ligado a esa clase
específica. Si se eliminara, se perdería la definición del bloque para la semana siguiente; al liberarlo
queda disponible para que otra clase lo ocupe.
→ `supabase/functions/confirm-class/index.ts` líneas ~92-95

### 3. ¿Qué diferencia hay entre el flujo de "clase confirmada" y "clase no realizada" en qué tabla se actualiza primero?
- **Realizada:** primero `payments` (RETAINED→RELEASED) y `teacher_payouts` (se crea PENDING), después
  `room_schedule_blocks` (libera), y al final `classes.status = COMPLETED`.
- **No realizada:** NO se toca `payments` de inmediato (siguen RETAINED). Se libera `room_schedule_blocks`,
  se inserta una `notification` y se marca `classes.status = SUSPENDED` con `reschedule_deadline` = ahora + 24h.
  El cambio a `payments` solo ocurre después, vía el cron, si nadie reagenda.
→ `supabase/functions/confirm-class/index.ts`

### 4. Si nadie reprograma dentro de la ventana de 24h (R16.1), ¿qué proceso hace el reembolso automático?
El cron `process_class_reschedule_timeouts` (corre cada hora vía `pg_cron`, ver R16.1) detecta las clases
`SUSPENDED` cuyo `reschedule_deadline` ya pasó, pasa sus `payments` de `RETAINED` a `REFUND_PENDING`,
cancela las `enrollments` y cierra la ventana. El reembolso real hacia MercadoPago lo ejecuta después la
Edge Function `process-refunds` (cron cada 10 min), que toma los `payments` en `REFUND_PENDING` y llama a
la API de reembolsos de MercadoPago con `X-Idempotency-Key` por `payment.id`.
→ `Documentación/02-Reglas-de-Negocio.md` R13/R16.1, `supabase/functions/process-refunds/`

### 5. ¿Por qué el reembolso diferido usa 24h y el reagendamiento del alumno usa 48h? ¿Son ventanas independientes?
Sí, son dos relojes independientes y consecutivos, no el mismo timeout:
- **24h (R16.1):** ventana para que el **responsable de la clase** (profesor independiente o sede) decida
  y pague/gestione un reagendamiento, contada desde que se marca "no realizada".
- **48h (R16):** una vez que el reagendamiento se propone y el nuevo horario existe, es la ventana para que
  **cada alumno** acepte o rechace la nueva fecha. Este segundo timer arranca solo si el primero se cumplió
  a tiempo — son secuenciales, no paralelos.
→ `Documentación/02-Reglas-de-Negocio.md` R16 y R16.1

### 6. `process-payouts` es un stub de Fase 0 — ¿qué le falta para desembolsar dinero real? ¿Por qué no se implementó?
Le falta el `disburseToSeller` real: la integración de money-out de MercadoPago Chile (transferencia de
saldo del vendedor plataforma hacia la cuenta bancaria del profesor) no estaba disponible/definida en el
alcance del semestre. Hoy el registro `teacher_payouts` se crea correctamente en estado `PENDING` con el
monto neto calculado (bruto menos comisión), pero el giro efectivo del dinero queda pendiente — es una
decisión de alcance documentada explícitamente como "fuera de alcance" en las restricciones del proyecto
(alcance de un semestre, sin financiamiento, pasarela con capacidades limitadas de payout en Chile).
→ `Documentación/11-Mejoras-Incorporadas.md` sección 12, `Documentación/06-API-Endpoints.md`

---

## 2. Webhook de MercadoPago y seguridad

### 7. `verify_jwt = false` en el webhook — ¿cómo se protege de un POST falso?
Con verificación **HMAC-SHA256** manual, no JWT: MercadoPago firma cada notificación con un secreto
compartido (`MERCADOPAGO_WEBHOOK_SECRET`). La función arma el mismo `manifest` (`id:...;request-id:...;ts:...;`)
que MercadoPago usó para firmar, calcula el HMAC con `crypto.subtle.sign`, y compara byte a byte con el
valor `v1` que viene en el header `x-signature`. Si no coincide, responde `403 Forbidden` sin tocar la BD.
→ `supabase/functions/mercadopago-webhook/index.ts` líneas 99-152

### 8. Explica cómo se arma el `manifest` firmado — ¿qué campos del header se parsean?
El header `x-signature` llega como `ts=<timestamp>,v1=<hash>`. La función separa por coma, y por cada
segmento separa clave=valor para extraer `ts` y `v1`. Luego arma `manifest = "id:${dataId};request-id:${requestId};ts:${ts};"`
donde `dataId` sale de `data.id` (query param o body, siempre en minúsculas — MercadoPago es sensible a
mayúsculas en IDs alfanuméricos) y `requestId` del header `x-request-id`. Ese manifest se firma con la
clave importada vía `crypto.subtle.importKey` y se compara contra `v1`.
→ `supabase/functions/mercadopago-webhook/index.ts` líneas 124-151

### 9. Si un atacante reenvía (replay) una notificación válida capturada antes, ¿qué lo impide?
Dos capas: (1) la firma HMAC sigue siendo válida en un replay puro (mismo `ts`, mismo `request-id`), así
que la defensa real es la **idempotencia por estado**: antes de procesar, la función busca la
`payment_session` por `external_reference` y si `session.status === 'APPROVED'` retorna `200 "ok"` sin
volver a crear inscripciones ni pagos. (2) Además valida contra la API real de MercadoPago
(`fetchMpPayment`) que el pago siga `approved`, no confía ciegamente en el payload reenviado.
→ `supabase/functions/mercadopago-webhook/index.ts` líneas 178-183 (comentario explícito `// idempotente`)

### 10. ¿Por qué el webhook usa `service_role` en vez del cliente autenticado por JWT del usuario?
Porque MercadoPago no envía JWT de Supabase — es un servidor externo notificando de forma asíncrona, no
hay "usuario logueado" en ese request. La función necesita escribir en `payments`, `enrollments` y
`payment_sessions` sin que exista sesión de usuario, así que usa el cliente admin (`ctx.supabaseAdmin`,
service role) que bypassa RLS. La seguridad no depende de RLS aquí, depende de la verificación HMAC previa.

---

## 3. Concurrencia y control de cupos

### 11. En `book-slot`, ¿qué evita que dos personas reserven el mismo bloque al mismo tiempo?
Un **guard atómico a nivel de UPDATE**: la sentencia es
`UPDATE room_schedule_blocks SET status='OCCUPIED' ... WHERE id = blockId AND status = 'AVAILABLE'`.
PostgreSQL garantiza que solo una de las dos requests concurrentes puede matchear la condición
`status = 'AVAILABLE'` — la segunda, al ejecutar después, ya no encuentra filas que actualizar
(`updated` viene `null`) y la función responde `409 Conflict`. No se usa un lock explícito
(`SELECT FOR UPDATE`) porque el `UPDATE ... WHERE` ya es atómico por sí mismo en Postgres.
→ `supabase/functions/book-slot/index.ts` líneas 63-70 (comentario: `// guard atómico contra carrera`)

### 12. R11 "Atomicidad del checkout" — si el pago se aprueba pero falla la inscripción a mitad de camino, ¿qué garantiza consistencia?
En el flujo actual (post-migración a Supabase), el checkout no corre en una transacción SQL explícita del
lado del webhook — corre en un loop `for (const item of cart.items)` dentro de `mercadopago-webhook`. La
garantía real no es atomicidad transaccional clásica sino **idempotencia + reconciliación por estado**: si
falla a mitad de camino, el `payment_session` no llega a `APPROVED`, y el reintento de MercadoPago (o una
reconciliación manual) puede reprocesar sin duplicar, porque cada `enrollment` insertada exitosamente ya
existe y el conteo de cupo (`agregadosPorClase`) se recalcula. *(Nota honesta si te preguntan más a fondo:
la regla R11 en el documento de reglas de negocio referencia la implementación original en Spring
`@Transactional`, que ya no existe tras la migración — vale la pena mencionar este matiz si profundizan.)*
→ `supabase/functions/mercadopago-webhook/index.ts` líneas 192-222, `Documentación/02-Reglas-de-Negocio.md` R11

### 13. ¿Cómo se implementó el "cupo a prueba de concurrencia a nivel de BD" (PR #44)?
No es un lock de fila sino un **conteo defensivo en memoria durante el mismo request del webhook**: la
variable `agregadosPorClase` cuenta cuántas inscripciones ya se insertaron a cada clase **dentro del mismo
carrito que se está procesando ahora mismo**, porque el `count` que llega de la base de datos no ve los
inserts anteriores del mismo loop hasta que cada uno hace commit individual. Sin esto, un carrito con
varias inscripciones a la misma clase podría superar el cupo porque todas leerían el mismo `count` inicial.
→ `supabase/functions/mercadopago-webhook/index.ts` líneas 194-198 (comentario explícito)

### 14. El índice único que evita inscripciones duplicadas — ¿sobre qué columnas está? ¿Cubre beneficiarios familiares?
`CREATE UNIQUE INDEX enrollments_unique_beneficiary ON enrollments (class_id, beneficiary_type, COALESCE(beneficiary_id, student_id))`.
Sí cubre el caso familiar: el constraint original era `UNIQUE (class_id, student_id)`, que impedía que un
mismo alumno inscribiera a dos beneficiarios distintos (él mismo + un hijo, por ejemplo) en la misma clase
— la segunda inscripción fallaba. El índice nuevo usa la expresión `COALESCE(beneficiary_id, student_id)`
para que la unicidad sea por **beneficiario real**, no por quien paga, permitiendo múltiples beneficiarios
del mismo alumno en la misma clase sin permitir que el mismo beneficiario se duplique.
→ `supabase/migrations/20260706000000_fix_enrollments_unique.sql`

---

## 4. Reglas de negocio y reagendamiento

### 15. R16 vs R16.1 — ¿por qué son reglas separadas?
R16 regula la **decisión del alumno** ante cualquier reagendamiento ya propuesto (48h para aceptar/rechazar
una fecha nueva, aplica siempre que hay una propuesta). R16.1 regula específicamente **qué pasa cuando una
clase se marca como "no realizada"**: abre la ventana previa de 24h para que el responsable decida *si*
reagenda, y solo si reagenda dentro de esa ventana se dispara el flujo de R16 sobre los alumnos. Son
capas distintas del mismo problema: R16.1 es "¿se va a reagendar?" y R16 es "¿el alumno acepta la nueva
fecha?".
→ `Documentación/02-Reglas-de-Negocio.md`

### 16. R17 "reagendamiento masivo si el maestro rechaza" — ¿qué pasa técnicamente con los registros?
Si el maestro rechaza o no responde, la clase pasa a `CANCELLED` y **todos** los `payments` de sus
`enrollments` pasan a `REFUND_PENDING` en un solo `UPDATE` masivo (no uno por uno con lógica condicional),
y se notifica a todos los alumnos inscritos. No hay ventana de espera adicional — es una cancelación
directa y total, a diferencia de R16.1 donde solo el que rechaza individualmente se reembolsa.
→ `Documentación/02-Reglas-de-Negocio.md` R17

### 17. R18 "quién decide según tipo de clase" — ¿cambia qué Edge Function se invoca?
Sí. Si `tipoClase = PROPIA` (profesor independiente en su propia sala), decide el profesor, y el
reagendamiento pasa por el flujo de `/profesor/reagendamientos` con `teacher-decision` /
`propose-reschedule` — además el profesor **paga un arriendo nuevo** de sala. Si `tipoClase = ASIGNADA`
(clase creada por la sede, profesor dependiente), decide el Admin General de la Sede vía
`sede-reschedule-class`, eligiendo una sala propia **sin pago**, y solo se notifica al profesor (no decide).
Si el actor incorrecto intenta la acción, el sistema responde `403`.
→ `Documentación/02-Reglas-de-Negocio.md` R18 y R16.1, `supabase/functions/sede-reschedule-class/index.ts`

### 18. En `teacher-decision`, ¿por qué el deadline se calcula en el momento de la decisión y no se fija desde la creación de la clase?
Porque el timer de 48h es para que **los alumnos** decidan sobre una **fecha reagendada específica**, que
no existe hasta que el profesor propone/acepta una nueva fecha. No tendría sentido fijar el deadline desde
la creación de la clase original, ya que en ese momento no se sabe si habrá reagendamiento ni cuándo. El
deadline se calcula como `Date.now() + 48*3600*1000` justo cuando el profesor confirma la nueva fecha,
momento en que recién empieza a correr la decisión de los alumnos.
→ `supabase/functions/teacher-decision/index.ts` línea 31

### 19. ¿Qué hace el cron `process-reschedule-timeouts` que corre cada hora? ¿Qué pasa si se solapan ejecuciones?
Busca en `reschedules` las filas en estado `TEACHER_ACCEPTED` cuyo `response_deadline` ya pasó, marca como
`TIMEOUT` las `reschedule_responses` que quedaron sin `response_type`, pasa a `REFUND_PENDING` los
`payments` de los alumnos que no respondieron, y cierra el `reschedule` como `COMPLETED`. Sobre el
solapamiento: la función es `SECURITY DEFINER` con `search_path` fijo y opera con `WHERE ... IS NULL` /
`WHERE status = 'TEACHER_ACCEPTED'` como condición de idempotencia — una segunda ejecución concurrente no
encontraría filas para re-marcar porque la primera ya las dejó en `TIMEOUT`/`COMPLETED`. `pg_cron` en la
práctica no solapa la misma tarea si la anterior sigue corriendo, pero el diseño ya es tolerante a eso.
→ `supabase/migrations/20260619000500_cron_functions.sql` líneas 15-34

---

## 5. Arquitectura, RLS y Edge Functions

### 20. ¿Dónde está la frontera entre "va en Edge Function" y "va en trigger de BD"?
Los **triggers/funciones de BD** (`SECURITY DEFINER`) se usan para reglas que deben cumplirse **siempre**,
sin excepción, incluso si alguien accede directo a PostgREST sin pasar por una Edge Function — ejemplo:
constraints `CHECK`, el índice único de beneficiarios, y las funciones de cron que corren sin usuario
autenticado. Las **Edge Functions** se usan para lógica que necesita **orquestar múltiples pasos con
validación de rol/contexto de negocio** (verificar quién es el actor, llamar a una API externa como
MercadoPago, decidir entre varias ramas de negocio) — eso no cabe bien en un trigger SQL. La regla general:
si la validación es "estructural" (un dato no puede existir sin cumplir X), va en BD; si es "de proceso"
(una secuencia de pasos con lógica condicional y llamadas externas), va en Edge Function.

### 21. Ejemplo concreto de "defensa en profundidad": una regla validada en Edge Function Y en RLS/constraint.
El control de cupo de una clase: en `mercadopago-webhook` se valida en aplicación
(`(count ?? 0) + yaAgregados >= cls.capacity`) antes de insertar la inscripción, **y** a nivel de BD existe
el índice único `enrollments_unique_beneficiary` que impide duplicados aunque la validación de aplicación
fallara o se saltara. Otro ejemplo: `confirm-class` valida en código que el usuario tenga rol
`VENUE_ADMIN`/`ADMIN` y sea dueño de la sede, y **además** las políticas RLS de la tabla `classes`
restringen igualmente quién puede leer/escribir esa fila — si un atacante encontrara un bypass en la Edge
Function, RLS seguiría bloqueando el acceso directo a la tabla.

### 22. Si alguien llama directo a `payments` vía PostgREST (sin pasar por una Edge Function), ¿RLS basta?
Depende de la operación. Para **lectura**, sí: RLS restringe qué filas de `payments` puede ver cada rol
(el alumno solo las suyas, la sede solo las de sus clases). Para **escritura/cambio de estado**, la
protección real no es solo RLS de columna sino que las transiciones de estado (`RETAINED`→`RELEASED`,
etc.) están diseñadas para ocurrir **solo** desde Edge Functions con `service_role` (que bypassa RLS por
diseño) — un usuario autenticado normal no debería tener política RLS que le permita hacer
`UPDATE payments SET status = 'RELEASED'` directamente. Es la razón por la que existe la migración que
revoca `EXECUTE` de las funciones privilegiadas de PUBLIC/anon/authenticated (ver pregunta 25): la
plataforma asume que ciertas mutaciones solo deben poder dispararse desde código server-side controlado.

### 23. ¿Por qué 27 Edge Functions en vez de un monolito de rutas? ¿Por qué `confirm-class`, `teacher-decision` y `student-decision` separadas?
Se separaron por **actor y por operación de negocio**, no por recurso CRUD genérico: cada función
representa una acción de negocio específica con su propio contrato de autorización (quién puede llamarla,
qué rol necesita). `confirm-class` la invoca la sede/admin; `teacher-decision` el profesor;
`student-decision` el alumno — cada una valida un actor distinto con lógica de negocio distinta (por
ejemplo, `teacher-decision` calcula el deadline de 48h de los alumnos, algo que no aplica a
`student-decision`). Fusionarlas en una sola función con un parámetro `action` habría mezclado
autorización de tres roles distintos en un único punto de entrada, aumentando el riesgo de un bypass de
rol y complicando el testing aislado de cada regla de negocio.

### 24. `admin-approve-venue`, `admin-users`, `admin-stats`, `admin-payments`, `admin-metrics` — ¿por qué 5 funciones separadas?
Mismo criterio: cada una tiene un contrato de datos y un costo de query distinto. `admin-metrics`, por
ejemplo, hace `Promise.all` sobre 6 tablas distintas con límites de hasta 50.000 filas — mezclarla con
`admin-users` (gestión CRUD de usuarios) haría una función más pesada, más lenta de invocar cuando solo se
necesita una parte, y más difícil de cachear o invalidar de forma independiente en el frontend.
→ `supabase/functions/admin-metrics/index.ts`

### 25. La migración `revoke_privileged_functions.sql` — ¿qué riesgo mitiga exactamente? (bugfix real G-16)
Antes de esta migración, funciones `SECURITY DEFINER` como `process_reschedule_timeouts()`,
`process_class_completion()`, `snapshot_system_metrics()` y `check_rls_coverage()` eran invocables vía RPC
de PostgREST por **cualquier usuario autenticado** (o incluso anónimo), porque por defecto Postgres otorga
`EXECUTE` a `PUBLIC`. Como son `SECURITY DEFINER`, corren con los privilegios del dueño de la función
(superuser/service role), no del que las llama — es decir, un usuario común podría haber disparado
manualmente timeouts de reagendamiento, completado clases antes de tiempo, o generado snapshots de
métricas fuera de su horario, saltándose el flujo controlado por `pg_cron`. La migración hace
`REVOKE EXECUTE ... FROM PUBLIC, anon, authenticated`, dejando esas funciones invocables solo por
`pg_cron`/`service_role`. Este bugfix está cubierto por un test dedicado:
`frontend/src/__tests__/bugfix/g16-privileged-functions-no-auth.property.test.ts`.
→ `supabase/migrations/20260620010200_revoke_privileged_functions.sql`

---

## 6. MercadoPago Connect / split payments

### 26. Diferencia entre Checkout Pro (inscripción a clases) y Connect (arriendo de salas) — ¿por qué uno necesita split y el otro no?
En la inscripción a clases, el dinero entra a la **cuenta de la plataforma** (Modo Ensayo) y luego se
liquida internamente al profesor vía `teacher_payouts` — no hay split de MercadoPago porque el
"desembolso" es un proceso propio (aunque aún stub). En el arriendo de salas, el dinero debe entrar
**directamente a la cuenta de la sede** (son ellos vendiendo el uso de su sala), pero la plataforma se
queda con una comisión — eso sí requiere el mecanismo de marketplace/split de MercadoPago Connect, donde
la preferencia de pago se crea con el token OAuth del vendedor (la sede) y un `marketplace_fee` que va a la
cuenta de la plataforma automáticamente en la misma transacción.
→ `Documentación/04-Arquitectura.md` sección "Flujo de pagos"

### 27. En `mp-connect-start`/`mp-connect-callback`, ¿qué se autoriza exactamente vía OAuth?
La **sede autoriza a Modo Ensayo** a operar pagos usando el token OAuth de la cuenta de MercadoPago de la
sede (no al revés). El flujo: `mp-connect-start` genera un `state` anti-CSRF, lo guarda en
`mp_oauth_states` ligado al `user_id` del gestor de sede, y devuelve la URL de autorización de
MercadoPago. La sede autoriza en el sitio de MercadoPago; MercadoPago redirige a
`mp-connect-callback` con `code` + `state`; la función valida el `state`, canjea el `code` por
`access_token`/`refresh_token` del **vendedor** (la sede) vía `POST /oauth/token`, y los guarda en
`mp_seller_accounts`. Con ese token, la plataforma puede crear preferencias de pago *en nombre de la sede*
con split automático.
→ `supabase/functions/mp-connect-start/index.ts`, `supabase/functions/mp-connect-callback/index.ts`

### 28. Si una sede desconecta MercadoPago a mitad de un arriendo con pago pendiente, ¿qué pasa?
No hay una regla de negocio documentada específicamente para este caso (no aparece en R01-R18) — es una
laguna real. Lo esperable según el diseño actual: el `payment_session` ya creado seguiría intentando
resolverse vía el token guardado en `mp_seller_accounts` en el momento del pago; si el webhook llega
después de que el token fue revocado, `fetchMpPayment` fallaría (401/404) y el pago quedaría sin
procesar — es un caso de borde que vale la pena reconocer como tal si te preguntan, en vez de inventar una
respuesta. Recomendación honesta en la defensa: "no está cubierto explícitamente en las 18 reglas de
negocio; es un candidato a mejora futura de manejo de tokens revocados".

---

## 7. Métricas del sistema (M1-M5)

### 29. M4 "disponibilidad" con latido interno cada 5 min — ¿por qué no depender de un servicio externo?
Para no depender de la disponibilidad de un tercero (ni de su costo) para medir la disponibilidad propia:
si el "monitor de uptime" fuera un servicio externo y ese servicio fallara o tuviera su propio downtime,
la métrica M4 sería inútil justo cuando más se necesita. El latido interno corre dentro del propio stack
(Supabase + `pg_cron`), registrando periódicamente que el sistema sigue vivo, sin costo adicional ni
dependencia de red externa.

### 30. ¿Cómo se calcula M2 "pagos aprobados / sesiones iniciadas"? ¿Qué cuenta como "sesión iniciada"?
Se calcula en `admin-metrics` como `pct(aprobadas, aprobadas + fallidas)` — nota: la fórmula real
implementada es sobre `payment_sessions` filtradas por estado, no divide por *todas* las sesiones iniciadas
sino específicamente aprobadas vs. el universo relevante de resultados de pago. Una "sesión iniciada"
corresponde a una fila creada en `payment_sessions`, que se genera en el momento en que
`mercadopago-create-preference` arma la preferencia de pago (antes de que el usuario complete el checkout
en MercadoPago) — es decir, cuenta el intento, no solo el pago exitoso.
→ `supabase/functions/admin-metrics/index.ts` líneas 1-13

### 31. Las métricas ¿se calculan en vivo o desde el snapshot del cron?
**Ambas cosas coexisten, para propósitos distintos.** `admin-metrics` (lo que ve el panel de
Admin/Sede) hace consultas **en vivo** con `Promise.all` sobre `venues`, `classes`, `rooms`, `enrollments`,
`attendances` y `payment_sessions` en cada request — no lee de una tabla de caché. Aparte, existe
`snapshot_system_metrics()`, un cron que corre cada hora y graba contadores globales simples
(`total_users`, `active_classes`, `retained_total`, etc.) en la tabla `system_metrics`, con
`cleanup-old-metrics` purgando lo mayor a 90 días — esto sirve como serie de tiempo histórica para
tendencias, no como fuente de las métricas M1-M5 del panel en vivo. Es una decisión deliberada: las
métricas que un admin consulta activamente deben reflejar el estado real al segundo, mientras que el
historial de series de tiempo no necesita esa frescura y sería costoso recalcularlo en cada consulta.
→ `supabase/functions/admin-metrics/index.ts`, `supabase/migrations/20260619000500_cron_functions.sql` líneas 106-136

---

## 8. Preguntas trampa / de profundidad

### 32. ¿Qué endpoint sería el primer cuello de botella a 10x tráfico?
`admin-metrics`: hace `Promise.all` de 6 queries con `.limit(20000)`/`.limit(50000)` y agrega los
resultados **en memoria** (JavaScript, no SQL agregado). A 10x el volumen de datos, esas queries empiezan a
acercarse al límite y el cálculo en memoria (mapear/reducir miles de filas en el runtime de Deno) se vuelve
notablemente más lento que si la agregación se hiciera con `GROUP BY`/vistas materializadas en PostgreSQL.
Es una respuesta honesta y defendible: reconoce una limitación real de diseño, no es una debilidad oculta.
→ `supabase/functions/admin-metrics/index.ts`

### 33. Si `mercadopago-webhook` recibe la misma notificación dos veces, ¿es idempotente?
Sí, explícitamente: después de verificar la firma HMAC, la función busca la `payment_session` por
`external_reference` y si `session.status === 'APPROVED'` retorna `200 "ok"` de inmediato sin volver a
insertar `enrollments` ni `payments` — el código lo marca con el comentario `// idempotente`. Es
importante también porque MercadoPago reintenta activamente si no recibe `200` a tiempo, así que la función
además siempre responde `200` incluso en su `catch` genérico (`return new Response("ok", { status: 200 })`)
para no gatillar reintentos indefinidos por errores internos no relacionados con la firma.
→ `supabase/functions/mercadopago-webhook/index.ts` líneas 178-183, 267-272

### 34. ¿Por qué Deno para las Edge Functions y no Node.js, si el frontend usa Node?
Deno es el runtime nativo de Supabase Edge Functions (no es una elección independiente del equipo, es la
plataforma la que lo define) — ofrece TypeScript nativo sin transpilación adicional, permisos explícitos
(seguridad por defecto: sin acceso a filesystem/red salvo que se declare), y arranque en frío más rápido
que un runtime Node tradicional en un entorno serverless, lo cual importa para funciones que se invocan
esporádicamente (como los webhooks). El frontend usa Node/npm porque el ecosistema de Vue/Vite está
construido sobre esa cadena de herramientas — son dos entornos de ejecución distintos con propósitos
distintos (build de assets estáticos vs. funciones serverless), no hay necesidad de que compartan runtime.

### 35. Si el `service_role` key de Supabase se filtrara, ¿qué acceso tendría un atacante que RLS no puede bloquear?
El `service_role` key **bypassa RLS por diseño** — es la clave que usan las Edge Functions para actuar como
"administrador de base de datos" sin las restricciones por fila que aplican a `anon`/`authenticated`. Si se
filtrara, un atacante tendría lectura y escritura completa sobre las 32 tablas del esquema, sin ninguna
política RLS de por medio — podría leer todos los pagos, cambiar estados de clase, modificar roles de
usuario, etc. Es exactamente la razón por la que `CLAUDE.md` y la arquitectura documentada insisten en que
"nunca se expone la clave de servicio al frontend" — vive solo en las variables de entorno server-side de
las Edge Functions, nunca en el bundle de Vue ni en `VITE_*`.
→ `CLAUDE.md` sección "Edge Functions", `Documentación/08-Justificacion-Tecnica.md` sección 2.3

---

## Cómo usar esta guía

No la memorices palabra por palabra — practica explicarla con tus propias palabras citando el archivo
cuando corresponda ("eso está en `mercadopago-webhook`, línea..."). Si te preguntan algo que no está aquí
literalmente, usa el mismo patrón de razonamiento: qué problema de negocio resuelve, por qué se decidió así
y no de otra forma, y qué archivo lo implementa. La pregunta 28 muestra el patrón correcto para cuando
genuinamente no hay una regla definida: reconocerlo como laguna documentada es mejor que inventar una
respuesta en la defensa.
