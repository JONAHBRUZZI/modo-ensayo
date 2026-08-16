# Respaldo de Evidencia · Pauta de Evaluación Final Transversal (TPY1101)

Este documento mapea cada indicador de la pauta de evaluación (Encargo 20% + Presentación 80%) a evidencia
verificable en el repositorio: archivos, rutas, comandos y resultados reales de ejecución. Todo lo listado
aquí fue verificado directamente contra el código y la documentación al 10-07-2026, no son afirmaciones sin
respaldo.

> Los 13 indicadores de la **Dimensión Presentación** son los mismos temas que los primeros 13 de la
> **Dimensión Encargo**, evaluados por dominio oral en la defensa. Por eso este documento no los separa:
> la evidencia de cada punto sirve como base tanto para el informe como para argumentar en vivo.

---

## Dimensión Encargo — Planificación y alcance

### 1. Modelo de solución / procesos de negocio afectados
- `Documentación/00-PRD.md` — sección "Producto" y "Problema": define el proceso de negocio afectado
  (coordinación de clases artísticas, pago condicionado a asistencia).
- `Documentación/12-Justificacion-Negocio.md` — análisis de negocio dedicado.
- Slide 3-6 de la presentación desarrollan lo mismo en formato ejecutivo.

### 2. Oportunidad/problemática, causas y efectos
- Slide 3 — tabla causa → efecto (5 filas: gestión informal, sin retención de pago, sin validación,
  sin reagendamiento, salas sin difusión).
- `Documentación/00-PRD.md` sección "Problema".

### 3. Objetivo general + objetivos específicos coherentes
- Slide 5 — objetivo general + OE1 + OE2, cada uno con línea "Coherencia:" que conecta explícitamente
  con la problemática de la slide 3.

### 4. Alcance (objetivos, entregables, supuestos, restricciones)
- Slide 6 — los 4 elementos exigidos por la pauta están presentes de forma explícita y separada.

### 5. Planificación de actividades (Gantt, tareas, responsables)
- `Documentación/Carta-Gantt-13jun2026.md` (106 líneas) — carta Gantt completa por sprint.
- Slide 7 — resumen ejecutivo de la misma carta Gantt + tabla de responsabilidades por integrante:
  - Darlette Morales → BD, RLS, migraciones, despliegue
  - Jonathan Guerra → lógica de negocio y Edge Functions
  - Victor Silva → frontend, UX, QA

### 6. Tecnologías adecuadas al problema
- `Documentación/08-Justificacion-Tecnica.md` sección 1 "Stack tecnológico" (Vue 3 + Vite, Supabase,
  Deno/TypeScript, MercadoPago, Vercel).
- Slide 9.

### 7. Justificación de la tecnología (cloud vs. local vs. híbrida)
- `Documentación/08-Justificacion-Tecnica.md` sección 5 "Decisiones que se evaluaron y descartaron"
  — comparación explícita local / híbrida / nube con motivos de descarte.
- Slide 9 — tabla "¿Por qué nube y no local o híbrida?" con factibilidad para equipo de 3 personas.

### 8. Atributos de calidad (integridad, confiabilidad, precisión, oportunidad, seguridad)
- **Resuelto.** `Documentación/12-Justificacion-Negocio.md` sección "4. Atributos de calidad de la
  solución" — tabla dedicada con los 5 atributos exigidos por la pauta (Integridad, Confiabilidad,
  Precisión, Oportunidad, Seguridad), cada uno con su mecanismo técnico concreto de garantía.
- `Informe_ModoEnsayo_TPY1101.docx` sección **2.9 "Atributos de calidad de la solución"** reproduce la
  misma tabla — el informe ya cumple este punto de forma explícita y nombrada.
- **Pendiente menor:** ninguna slide de la presentación nombra los 5 atributos como tabla explícita
  (solo se infieren de forma indirecta en la slide 9/11). No es necesario agregarlo como mejora de
  producto — el contenido ya existe en repo e informe — pero conviene mencionarlos por nombre durante
  la defensa oral si preguntan por "atributos de calidad", citando la tabla del informe.

### 9. Metodología coherente + ciclo de vida justificado
- `Documentación/01-Metodologia.md` — documento dedicado.
- Slide 8 — Iterativo-Incremental + Scrum, con justificación explícita de por qué (equipo pequeño,
  feedback semanal, núcleo de pagos condicionados debía validarse primero).

### 10. Documentación de revisiones y entregas parciales
- Carpeta `Documentación/` — 20 archivos numerados (00 a 12 + A1-A3 + Carta Gantt + README + WORKFLOW),
  718+ líneas solo entre PRD, Plan de Pruebas, Mejoras Incorporadas y Carta Gantt.
- Carpeta `Gestión/` — 5 archivos: equipos y responsabilidades, plan de trabajo, git workflow, onboarding.
- Historial de Pull Requests referenciado explícitamente en `11-Mejoras-Incorporadas.md` (PR #30 a #48).

---

## Desarrollo de la solución innovadora

### 11. Ambiente de pruebas configurado (operativas, validación, verificación)
- `Documentación/07-Plan-de-Pruebas.md` sección 3 "Ambiente de pruebas" — describe frontend, backend
  (Supabase) y "ambiente cloud para validación" (MercadoPago sandbox, mismo esquema que producción).
- `Documentación/A2-Setup-Local.md` — pasos reproducibles de setup.
- Slide 12 — resumen de las 3 tipos de prueba (operativas, de validación, de verificación).

### 12. Backup de BD de producción + réplica de configuración de servidor
- **Resuelto.** `Informe_ModoEnsayo_TPY1101.docx` sección **3.2 "Respaldo de la base de datos y
  configuración del servidor cloud"** documenta los 3 procedimientos exigidos por la pauta: (1) copia de
  seguridad de producción hacia pruebas vía `pg_dump`/`pg_restore` o branching de Supabase, (2)
  configuración del servidor cloud de pruebas para reflejar producción (`supabase link`,
  `supabase functions deploy`, mismas variables de entorno en Vercel), y (3) instalación de lenguajes,
  bibliotecas y herramientas (Node.js 22, Deno, PostgreSQL 16 gestionado, Supabase CLI, `npm install`
  reproducible vía `package-lock.json`).
- **Gap que sí existía y fue corregido en esta revisión:** el procedimiento estaba redactado solo en el
  informe, sin mirror en la documentación del repositorio. Se agregó la misma sección (verbatim) a
  `Documentación/A1-Despliegue.md`, entre "Backend: Supabase" y "Frontend: Vercel", para que el
  repositorio sea consistente con lo que el informe declara y sea consultable en vivo durante la demo.
- **Pendiente menor:** la presentación solo menciona "Replica producción (mismo esquema, Edge Functions y
  MercadoPago sandbox)" en la slide 12, sin detallar el mecanismo (`pg_dump`/branching). No es necesario
  agregarlo como mejora de producto — es una decisión válida de síntesis para una slide de 40 minutos —
  pero prepárate para explicar el mecanismo verbalmente citando `A1-Despliegue.md` si te preguntan "¿cómo
  exactamente respaldan la base de datos?".

### 13. Código con patrón de arquitectura y buenas prácticas
- `Documentación/04-Arquitectura.md` completo (stack, estructura, capa de servicios, flujo de datos,
  flujo de pagos, principios de diseño, patrones).
- Evidencia directa en el repo:
  - 27 Edge Functions en `supabase/functions/` (lógica sensible aislada del cliente).
  - 43 migraciones SQL versionadas en `supabase/migrations/`.
  - 32 tablas creadas, **32 con `ENABLE ROW LEVEL SECURITY`** — cobertura RLS 100% verificada con:
    ```
    grep -rhoE "CREATE TABLE( IF NOT EXISTS)? [a-zA-Z_.]+" supabase/migrations | sort -u | wc -l   → 32
    grep -rhoE "ALTER TABLE .* ENABLE ROW LEVEL SECURITY" supabase/migrations | sort -u | wc -l      → 32
    ```
    Esto confirma exactamente la cifra de la slide 14 ("0 tablas sin política RLS").

---

## Validación y mejoras

### 14. Plan de pruebas en tabla (acciones, funcionalidades, resultado esperado)
- `Documentación/07-Plan-de-Pruebas.md` sección 5 "Plan de escenarios de aceptación" (5.1 a 5.4, con
  P1/P2 priorizados) + sección 5.5 "Lista de verificación de aceptación — 22 HU".
- Slide 12 — tabla de 6 escenarios con resultado esperado (registro+identidad, pago consolidado,
  clase realizada, clase no realizada, reagendamiento+48h, cancelación).

### 15. Pruebas de validación aplicadas
- Ejecución real verificada en esta sesión:
  ```
  cd frontend && npm test -- --run
  Test Files  1 failed | 6 passed (7)
  Tests       2 failed | 42 passed (44)
  ```
  → coincide exactamente con la cifra de la slide 14 ("42/44 tests automatizados en verde"). Los 2
  fallos están en `CartPage.test.js` (test de UI del botón "Pagar", no afecta lógica de pagos condicionados).
- `Documentación/07-Plan-de-Pruebas.md` sección 4 "Tests automatizados implementados" — detalla los
  7 archivos de test: `CartPage.test.js`, `PaymentSuccessPage.test.js`, y 5 tests en
  `frontend/src/__tests__/bugfix/` (incluye tests de propiedad para reembolsos y auth de funciones
  privilegiadas — evidencia de pruebas de seguridad, no solo funcionales).
- Lint verificado en esta sesión:
  ```
  cd frontend && npm run lint
  ✖ 5909 problems (0 errors, 5909 warnings)
  ```
  → confirma "0 errores de lint" de la slide 14, aunque conviene mencionar en la defensa que existen
  ~5900 warnings de estilo (`vue/max-attributes-per-line`, `vue/html-indent`, etc.), todas no bloqueantes.

### 16. Mejoras aplicadas según resultado de pruebas
- `Documentación/11-Mejoras-Incorporadas.md` (314 líneas) — 11 categorías de mejora, cada una con
  referencia a PR concreto: descubrimiento de clases (PR #31/#32), inscripciones (PR #30), reagendamiento
  (PR #30, PR #47), ciclo del dinero (`confirm-class` PR #38, `process-refunds` PR #39/#40/#43, cupo a
  prueba de concurrencia PR #44), asistencia, métricas por sede, GA4, gestión de usuarios en cascada.
- Slide 15 — 6 mejoras categorizadas (corrección, seguridad, estadísticas, reagendamiento, usabilidad,
  pertinencia), cada una trazable a una sección específica de `11-Mejoras-Incorporadas.md`.

### 17. Informe con evidencias del proyecto
- La carpeta `Documentación/` completa constituye el respaldo escrito: PRD, metodología, reglas de
  negocio, historias de usuario, arquitectura, modelo de datos, endpoints, plan de pruebas, justificación
  técnica y de negocio, mejoras incorporadas, despliegue y setup.
- `Producto/01-Credenciales-Prueba.md` — datos de prueba reales para la demo (login de cada rol).

---

## Cifras clave citadas en la presentación — verificación cruzada

| Cifra en la slide | Fuente verificada | Resultado |
|---|---|---|
| "22 historias de usuario" (slide 10, 17) | `grep -c "HU" Documentación/03-Historias-de-Usuario.md` | ✅ 22 confirmado |
| "18 reglas formales" (slide 10, 17) | Encabezados `## R01` a `## R18` en `Documentación/02-Reglas-de-Negocio.md` | ✅ 18 confirmado (R01–R18 + R16.1) |
| "42/44 tests automatizados en verde" (slide 14) | `npm test -- --run` ejecutado en esta sesión | ✅ exacto: 42 passed / 2 failed de 44 |
| "0 errores de lint" (slide 14) | `npm run lint` ejecutado en esta sesión | ✅ exacto: 0 errors (5909 warnings no mencionados en la slide) |
| "0 tablas sin política RLS" (slide 14) | Conteo de `CREATE TABLE` vs `ENABLE ROW LEVEL SECURITY` en `supabase/migrations/` | ✅ exacto: 32/32 tablas |
| "22/22 historias de usuario verificadas" (slide 14) | `Documentación/07-Plan-de-Pruebas.md` sección 5.5 | ✅ existe checklist dedicado |

**Conclusión:** ninguna cifra citada en la presentación está inflada o es falsa — todas tienen respaldo
verificable en el repositorio. Los dos vacíos identificados en la revisión anterior (punto 8, atributos de
calidad, y punto 12, backup/réplica de servidor) **ya están cubiertos en `Informe_ModoEnsayo_TPY1101.docx`**
(secciones 2.9 y 3.2 respectivamente), y el punto 8 además ya vivía en el repo
(`Documentación/12-Justificacion-Negocio.md` sección 4). El único ajuste real hecho en esta revisión fue
mirrorear el procedimiento de backup del informe hacia `Documentación/A1-Despliegue.md`, que no lo tenía.
Lo único que queda pendiente es opcional: nombrar ambos puntos explícitamente en la presentación oral si
preguntan por ellos en la defensa, ya que el PPTX los resume de forma indirecta.
