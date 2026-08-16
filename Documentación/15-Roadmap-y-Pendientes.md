# Roadmap y Pendientes · Modo Ensayo

> **Versión:** 1.0 — 16-ago-2026
> Compilado a partir de una auditoría completa del proyecto (Supabase + código + documentación)
> hecha el 16-ago-2026. Reemplaza cualquier lista de pendientes dispersa en otros documentos como
> fuente única de seguimiento. Cada ítem indica **qué**, **por qué importa** y **dónde está**.

---

## 🔴 Crítico / bugs conocidos

### 1. R05 — Identidad duplicada: solo aviso en frontend, sin constraint en BD
La documentación (`02-Reglas-de-Negocio.md`, regla R05, y `07-Plan-de-Pruebas.md`, escenario
"Borde 1 — Identidad duplicada") dice que un documento de identidad ya aprobado en otra cuenta se
rechaza. El **11-jul** se agregó una mitigación parcial (RPC `rut_ya_registrado`,
`SECURITY DEFINER`) que avisa en el formulario de verificación si el RUT ya está registrado
(`PENDING`/`APPROVED` en `identity_verifications` o en `profiles.rut` de otra cuenta) y bloquea el
envío. Pero **sigue sin haber un constraint único en la base de datos**: `adminService.reviewIdentity()`
(`frontend/src/services/adminService.js:115-129`) aprueba sin reverificar, y una aprobación vía API
directa (bypaseando el frontend) no queda bloqueada.

- **Riesgo:** suplantación de identidad / una persona con múltiples cuentas validadas, si no pasa
  por el formulario normal.
- **Era una regresión de la migración**: el backend Spring Boot original lo tenía como constraint
  duro (`IdentityVerificationRepository.existsByDocumentNumberAndStatusAndUserIdNot()`).
- **Fix sugerido:** constraint único parcial en Postgres —
  `CREATE UNIQUE INDEX ON identity_verifications (document_number) WHERE status = 'APPROVED'` — o
  chequeo explícito en `reviewIdentity()` antes de aprobar, para cerrar el gap que el aviso del
  frontend no cubre.

### 2. Confirmar despliegue de los fixes del webhook de pago (11-jul)
Dos commits del 2026-07-11 corrigen bugs de correctitud en `mercadopago-webhook` (ver
`11-Mejoras-Incorporadas.md`, sección 4.7): aceptar inscripción aunque la clase cambió de estado, y
tratar el HMAC como defensa en profundidad en vez de bloqueo. Ambos están marcados "requiere
redeploy" en el commit, pero **no se pudo confirmar en la auditoría del 16-ago si la función
desplegada en producción ya incluye estos cambios** (el CLI perdió el contexto de sesión antes de
poder chequear `supabase functions list`).

- **Acción:** entrar al Dashboard de Supabase → Edge Functions → `mercadopago-webhook` → revisar
  fecha/versión del último deploy y compararla contra el commit `337f3ff`/`a4dd59d` (11-jul, ya en
  `main`). Si es anterior, `supabase functions deploy mercadopago-webhook`.

### 3. Verificar deploy de la migración `20260710000000_full_delete_cascade.sql`
Commit `32f366c fix(admin): cascada de borrado completa para eliminar admins de sede`, mergeado a
`main` después del corte de esta auditoría (no estaba en el schema comparado el 16-ago). Confirmar
con `supabase migration list` / `supabase db push` que quedó aplicada en remoto antes de asumir que
el borrado de admins de sede funciona en producción.

### ✅ Cerrado en esta misma auditoría (16-ago-2026)
- **Trigger `enforce_teacher_mp_connected` faltante en producción** (migración `20260622000400`,
  aplicada desde el 22-jun pero nunca desplegada) — aplicado y verificado.
- **Drift de tracking de migraciones** (22 migraciones locales marcadas "no aplicadas" cuando 21 ya
  lo estaban) — reconciliado con `migration repair`. Ver detalle en `11-Mejoras-Incorporadas.md` §13.

---

## 🟡 Deuda técnica

| Ítem | Detalle | Dónde |
|---|---|---|
| Desembolso real a profesores | `process-payouts` → `disburseToSeller` es un stub de Fase 0; los `teacher_payouts` quedan `PENDING` pero el dinero no se gira automáticamente (money-out de MercadoPago Chile pendiente de habilitar) | `supabase/functions/process-payouts/` |
| 2 tests fallando por timing | `CartPage.test.js` — el componente queda en "Cargando..." antes de que resuelva el mock; no es un bug de producto | `frontend/src/views/CartPage.test.js` |
| `features/` sin usar | Carpetas `auth`, `cart`, `classes`, `payments`, `reschedules` bajo `frontend/src/features/` están vacías (solo `.gitkeep`); la lógica real vive en `services/` + `views/`. Corregido en `CLAUDE.md` en esta auditoría — decidir si se elimina la carpeta o se usa de verdad | `frontend/src/features/` |
| CLI de Supabase desactualizado | v2.78.1 instalada, v2.114.0 disponible | — |
| Advisors reales de Supabase nunca ejecutados formalmente | En esta auditoría se replicaron a mano los checks más importantes (RLS habilitado, `search_path` en funciones `SECURITY DEFINER`) por no tener el conector MCP disponible, pero nunca se corrió el motor real de Advisors del Dashboard | [Dashboard → Advisors](https://supabase.com/dashboard/project/remznaanexwgzeeupctv/advisors/security) |
| Reembolso de arriendos de sala | No hay flujo para anular/devolver un `ROOM_RESERVATION` una vez pagado | — |
| Validaciones de perfil server-side | Hoy solo en frontend; faltan triggers/constraints equivalentes en BD | — |
| Rendimiento de vistas calientes | Cascadas de llamadas secuenciales en algunas vistas podrían paralelizarse | — |

---

## 🟢 Roadmap de producto

- **Money-out real de MercadoPago Chile** para que `process-payouts` gire el dinero automáticamente
  a los profesores (hoy el admin gira manualmente desde el panel de pagos).
- **Secrets de `ga-metrics`** (`GA_PROPERTY_ID`, `GA_SERVICE_ACCOUNT`): confirmar que están seteados
  en producción; sin ellos la función responde `{ configured:false }` y el panel de comportamiento
  del admin queda vacío.
- **Reglas de plazo 7d/72h para cobro parcial del reagendamiento** — mencionado explícitamente como
  "fuera de alcance (futuro)" en R16.1 de `02-Reglas-de-Negocio.md`.
- **Revisión de Historias de Usuario pendientes**: `03-Historias-de-Usuario.md` no se auditó a fondo
  en esta pasada (es un documento de producto, no técnico) — revisar si todas las 22 HU siguen
  vigentes o si alguna quedó parcialmente implementada tras la migración a Supabase.

---

## 📋 Higiene de repositorio

- `Documentación/13-Respaldo-Rubrica.md` y `Documentación/14-Preguntas-Tecnicas-Jonathan.md` existen
  en disco pero están **sin commitear** (untracked) — decidir si se commitean o se descartan.
- `Documentación/A1-Despliegue.md` tenía cambios locales sin commitear desde antes de esta auditoría
  (además de los agregados hoy) — revisar y commitear el paquete completo de documentación.
- Rotar el Supabase Personal Access Token que se pegó en el chat de esta sesión (`sbp_ef9c...`) —
  quedó expuesto en texto plano y debe tratarse como comprometido.

---

## Cómo se generó este documento

Auditoría del 16-ago-2026: verificación de cuenta/login de Supabase CLI, reconciliación de
migraciones locales vs. remotas (schema real comparado objeto por objeto vía `supabase db dump`),
chequeo manual de seguridad (RLS por tabla, `search_path` en funciones `SECURITY DEFINER`) y
performance (`supabase inspect db`), más una revisión cruzada de `Documentación/` contra el código
real (`frontend/src/`, `supabase/migrations/`, `supabase/functions/`) y el historial de git desde el
último corte de documentación (2026-07-11).
