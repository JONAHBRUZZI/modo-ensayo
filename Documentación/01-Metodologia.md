# Metodología de Desarrollo · Modo Ensayo

> **Versión:** 1.0 — 30-may-2026
> **Marco:** Scrum adaptado a equipo de 3 personas con sprints semanales

## 1. Marco de trabajo

Adoptamos **Scrum con sprints semanales** según la recomendación del docente. La elección se basa en:

- **Iteraciones cortas (1 semana)** permiten detectar y corregir rumbo rápidamente, dado el plazo total de 11 semanas de desarrollo.
- **Ceremonias livianas** apropiadas para un equipo de 3 personas sin saturar agenda.
- **Definition of Done explícito** por sprint para asegurar que cada cierre sea evaluable.
- **Demo del sábado** como hito tangible que obliga a integrar y mostrar funcionalidad real, no avances aislados.

## 2. Equipo y roles Scrum

| Rol Scrum | Asignación |
|---|---|
| Product Owner | Compartido (decisiones de alcance del MVP consensuadas) |
| Scrum Master | Rotativo por sprint (lidera el Planning y Retro) |
| Equipo de desarrollo | Darlette + Jonathan + Victor (3 integrantes full stack por área) |

## 3. Ceremonias semanales

| Día | Ceremonia | Duración | Foco |
|---|---|---|---|
| Lunes | **Sprint Planning** | 60–90 min | Definir el alcance del sprint, dividir tareas por integrante con criterios de aceptación |
| Miércoles | **Daily / Check** | 30 min | Revisar avance, identificar bloqueos, ajustar si es necesario |
| Sábado | **Sprint Demo** | 60 min | Cada integrante muestra lo que terminó. Validación interna del DoD |
| Domingo | **Sprint Retro** | 30 min | Qué funcionó, qué mejorar, ajustes para el siguiente sprint |

Cualquier ceremonia se hace presencial en biblioteca de Duoc o sincrónica vía Discord/Meet según disponibilidad.

## 4. Artefactos

### Product Backlog
Documentado en [`03-Historias-de-Usuario.md`](./03-Historias-de-Usuario.md). Lista de 22 Historias de Usuario con criterios de aceptación.

### Sprint Backlog
Cada sprint extrae HU del Product Backlog. Se desglosan en tareas técnicas asignadas por integrante. Documentado en la [Carta Gantt](./word/10-Carta-Gantt-30may2026.docx).

### Incremento
Software funcional desplegado al cierre de cada sprint. Verificable en URL pública.

## 5. Definition of Done (DoD) general

Para que una tarea se considere "Done":

1. ✅ Funcionalidad implementada y desplegada en cloud
2. ✅ Test unitario o de integración (cuando aplica)
3. ✅ Documentación actualizada (si afecta API, BD o flujo)
4. ✅ PR mergeado a `develop` con revisión de al menos otro integrante
5. ✅ Verificada manualmente desde el ambiente desplegado por al menos otro integrante
6. ✅ Sin errores en consola/logs en flujo normal

Para que un sprint se considere cerrado:

1. ✅ Todos los DoD individuales del sprint cumplidos
2. ✅ Demo del sábado exitosa
3. ✅ Sin bugs críticos pendientes
4. ✅ Documentación de gestión actualizada (Carta Gantt si aplica)

## 6. Asignación de trabajo

| Área | Responsable principal | Apoyo |
|---|---|---|
| Backend (Java, Spring Boot) | Jonathan | Darlette (BD), Victor (tests) |
| Frontend (Vue, UX) | Victor | Jonathan (integración API), Darlette (mockups) |
| Base de Datos (PostgreSQL) | Darlette | Jonathan (entidades JPA) |
| DevOps (Docker, Cloud) | Darlette | Jonathan (config Spring) |
| Documentación de diseño | Compartido | Sprint 0 |
| QA | Compartido | Sprint 9 dedicado |

Aplicamos **pair programming** en módulos críticos (pagos, reagendamiento) para que al menos 2 personas entiendan cada flujo y evitar single points of failure.

## 7. Herramientas

| Herramienta | Uso |
|---|---|
| **GitHub** | Repositorio único, Pull Requests con revisión |
| **GitHub Actions** | CI: build + tests + linter en cada PR |
| **GitHub Projects / Issues** | Tracking del Sprint Backlog |
| **Docker Compose** | Ambiente local idéntico para los 3 integrantes |
| **Discord** | Comunicación asíncrona y dailies remotas |
| **VS Code + IntelliJ** | IDEs según preferencia (frontend / backend) |
| **draw.io / PlantUML / Mermaid** | Diagramas |
| **Postman** | Testing manual de API |
| **pgAdmin** | Inspección de base de datos |

## 8. Flujo Git

Detalle completo en [`../Gestión/03-Git-Workflow.md`](../Gestión/03-Git-Workflow.md). Resumen:

```
main          ← producción, solo merge desde develop
  ↑
develop       ← integración, merge de feature branches
  ↑
feature/xxx   ← rama por feature/HU
victor/...    ← ramas personales para WIP
```

Cada PR requiere:
- Mensaje de commit en español, formato `type: descripción`
- Pasar CI (tests + linter)
- Aprobación de al menos un integrante
- Sin conflictos con la rama base

## 9. Manejo de riesgos del proceso

| Riesgo | Mitigación adoptada |
|---|---|
| Ausencia de un integrante en una semana | Pair programming previo + documentación de cada módulo en GitHub |
| Bugs introducidos en producción | Despliegue continuo con revisión sábado/lunes, no viernes |
| Cambio de alcance a mitad de sprint | Solo el Sprint Backlog próximo se puede modificar; el actual queda fijo |
| Conflictos de merge | PRs pequeños y frecuentes, rebase frecuente con `develop` |
| Estimaciones imprecisas | Se replanifican Sprints 6-11 al cierre de cada sprint según velocity real |

## 10. Métricas de velocity observada

| Sprint | HU planificadas | HU entregadas | Velocity |
|---|---|---|---|
| 0 (3 días) | 100% documentación | 100% | 100% |
| 1 | 4 HU | 4 HU | 100% |
| 2 | 5 HU | 5 HU | 100% |
| 3 | 6 HU | 6 HU + 1 extra | 116% |
| 4 | 3 HU + presentación | 3 HU + presentación + 2 extras | 130% |
| 5 (actual) | 4 HU | 4 HU + 5 extras | 200% estimado |

La aceleración en Sprints 3-5 se debe al equipo entrando en ritmo y a la reutilización efectiva de componentes/patrones establecidos en Sprints 1-2.

## 11. Comunicación con el docente

- Entrega formal de Sprint 0 vía canal acordado (25-abr ✓)
- Presentación Experiencia 2 (Sem 11-12, 35%) ✓
- Avances mensuales por correo con link a Carta Gantt actualizada
- Defensa final Evaluación Transversal (Sem 17-18, 40%) pendiente
