# Roadmap y Pendientes · Modo Ensayo

> **Versión:** 1.0 — 16-ago-2026
> Compilado a partir de una auditoría completa del proyecto (Supabase + código + documentación)
> hecha el 16-ago-2026. Reemplaza cualquier lista de pendientes dispersa en otros documentos como
> fuente única de seguimiento. Cada ítem indica **qué**, **por qué importa** y **dónde está**.

---

## 🔴 Crítico / bugs conocidos

Sin ítems abiertos al cierre de esta auditoría (16-ago-2026) — el último (R05) se resolvió en la
misma sesión. Ver "Cerrado" abajo para el detalle de cada uno.

### ✅ Cerrado el 16-ago-2026 (sesión de la auditoría)
- **R05 — Identidad duplicada:** índice único parcial `identity_verifications_document_approved_unique`
  sobre `identity_verifications.document_number` (normalizado) para `status = 'APPROVED'`
  (migración `20260816000000_identity_document_unique.sql`, commit `590d269`), más manejo del error
  en `adminService.reviewIdentity()` para devolver un mensaje claro en vez del error crudo de
  Postgres. Aplicado en producción sin conflictos (no había documentos duplicados preexistentes).
  Cierra el gap que dejaba el aviso de RUT del frontend (11-jul), que no cubría aprobaciones vía API
  directa ni condiciones de carrera. Era una regresión de la migración desde Spring Boot
  (`IdentityVerificationRepository.existsByDocumentNumberAndStatusAndUserIdNot()`).
- **Trigger `enforce_teacher_mp_connected` faltante en producción** (migración `20260622000400`,
  aplicada desde el 22-jun pero nunca desplegada) — aplicado y verificado.
- **Drift de tracking de migraciones** (22 migraciones locales marcadas "no aplicadas" cuando 21 ya
  lo estaban) — reconciliado con `migration repair`. Ver detalle en `11-Mejoras-Incorporadas.md` §13.
- **Fixes del webhook de pago (11-jul)** — `mercadopago-webhook` redeployado (`supabase functions
  deploy mercadopago-webhook`, confirmado en terminal del usuario). Los dos fixes de correctitud
  (inscripción con clase en otro estado + HMAC como defensa en profundidad) ya están en producción.
- **Colisión de timestamp de migración**: `full_delete_cascade.sql` y `venue_stats.sql` compartían
  el mismo número de versión `20260710000000`, lo que confundía el tracking de
  `supabase migration list`. Renombrado a `20260710000002_full_delete_cascade.sql` (commit
  `638bf76`) y aplicado junto con `20260711000000_rut_exists_rpc.sql` vía `supabase db push` — ambos
  confirmados en remoto sin errores. Con esto, **la cascada de borrado completa de admins de sede
  (32f366c) y el aviso de RUT duplicado (R05, mitigación parcial) ya están en producción.**

---

## 🟡 Deuda técnica

| Ítem | Detalle | Dónde |
|---|---|---|
| Desembolso real a profesores | `process-payouts` → `disburseToSeller` es un stub de Fase 0; los `teacher_payouts` quedan `PENDING` pero el dinero no se gira automáticamente (money-out de MercadoPago Chile pendiente de habilitar). **Decisión de alcance del equipo, no deuda olvidada** — ver `02-Reglas-de-Negocio.md` R13 y `14-Preguntas-Tecnicas-Jonathan.md` | `supabase/functions/process-payouts/` |
| Advisors reales de Supabase nunca ejecutados formalmente | En esta auditoría se replicaron a mano los checks más importantes (RLS habilitado, `search_path` en funciones `SECURITY DEFINER`) por no tener el conector MCP disponible, pero nunca se corrió el motor real de Advisors del Dashboard. **Acción manual, no de código** | [Dashboard → Advisors](https://supabase.com/dashboard/project/remznaanexwgzeeupctv/advisors/security) |
| Reembolso de arriendos de sala | No hay flujo para anular/devolver un `ROOM_RESERVATION` una vez pagado. **Bloqueado en decisión de producto**: quién puede cancelar (profesor/sede/ambos), hasta cuándo, reembolso total o con penalidad — sin definir esto no se puede diseñar la migración/Edge Function | `supabase/functions/reserve-room-preference/`, `payment_sessions` |

**✅ Cerrado el 16-ago-2026 (segunda sesión, deuda técnica):**
- CLI de Supabase actualizada v2.78.1 → v2.114.0 (`scoop update supabase`).
- Los 2 tests de `CartPage.test.js` — causa real: `associateService.getAssociates()` (llamado en
  paralelo con `getCart()` en `onMounted`) no estaba mockeado y disparaba una llamada real a
  Supabase que nunca resolvía en el entorno de test. Se mockeó y se cambió el `setTimeout` fijo por
  `flushPromises()`. Suite completa: **44/44 tests pasan** (antes 42/44).
- `frontend/src/features/` (vacío, solo `.gitkeep`) — eliminado. Corregidas las referencias en
  `CLAUDE.md` y `Documentación/04-Arquitectura.md`.
- **Validaciones de perfil server-side**: nueva migración
  `20260816010000_profile_validation_constraints.sql` — función `rut_valido()` (mismo algoritmo que
  el frontend, verificado con 8 casos de prueba cruzados) + constraints en `profiles.rut`,
  `profiles.phone`, `refund_methods.rut` e `identity_verifications.document_number` (solo cuando
  `document_type = 'RUT'`, para no romper pasaportes). `userService.js` traduce el error de Postgres
  (23514) a un mensaje claro. **Pendiente aplicar en producción** (`supabase db push`).
- **Rendimiento de vistas calientes**: `AdminDashboardPage.vue` (7 llamadas secuenciales → 1
  `Promise.allSettled`), `SedeDashboardPage.vue` (4 secuenciales + loop de salas por sede → paralelo
  con `Promise.all`), `ProfesorDashboardPage.vue` (2 llamadas sueltas después del primer
  `Promise.allSettled` → sumadas al mismo). Verificado: lint sin errores nuevos, 44/44 tests, build
  de producción limpio.

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
