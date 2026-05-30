# Modo Ensayo

> Plataforma de gestión de clases artísticas con pagos condicionados a la realización de la clase.

**Equipo:** Darlette Morales · Jonathan Guerra · Victor Silva
**Asignatura:** Taller Aplicado de Programación — TPY1101
**Profesor:** Felipe Arturo Castillo Ducaud
**Período:** Abril – Julio 2026

---

## Organización del repositorio

Este repositorio está estructurado en tres áreas, según los lineamientos de la asignatura:

| Carpeta | Contenido |
|---|---|
| [`Documentación/`](./Documentación/) | Toda la documentación del proyecto: PRD, Reglas de Negocio, Historias de Usuario, Arquitectura, Modelo de Datos, API, Plan de Pruebas, Carta Gantt |
| [`Producto/`](./Producto/) | Artefactos del producto: scripts SQL, procedimientos almacenados, credenciales de prueba, capturas del sistema, referencias al código fuente |
| [`Gestión/`](./Gestión/) | Gestión del equipo: responsabilidades, plan de trabajo, Git workflow, onboarding |

El **código fuente** vive en `backend/`, `frontend/` e `infra/` en la raíz del repo y está documentado desde `Producto/README.md`.

---

## Documentos clave para evaluación

- [Carta Gantt actualizada (Word)](./Documentación/word/10-Carta-Gantt-30may2026.docx)
- [PRD — Problema y Solución](./Documentación/00-PRD.md)
- [Metodología Scrum aplicada](./Documentación/01-Metodologia.md)
- [Reglas de Negocio (18 reglas)](./Documentación/02-Reglas-de-Negocio.md)
- [Historias de Usuario](./Documentación/03-Historias-de-Usuario.md)
- [Arquitectura de la solución](./Documentación/04-Arquitectura.md)
- [Modelo de Datos](./Documentación/05-Modelo-de-Datos.md)
- [API Endpoints](./Documentación/06-API-Endpoints.md)
- [Plan de Pruebas](./Documentación/07-Plan-de-Pruebas.md)
- [Justificación Técnica](./Documentación/08-Justificacion-Tecnica.md)

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Frontend | Vue 3 (Composition API) + Vite + Tailwind CSS |
| Backend | Spring Boot 3.2 + Java 21 + Spring Security + JWT |
| Base de Datos | PostgreSQL 16 + Triggers + Procedimientos Almacenados |
| Pagos | MercadoPago Checkout Pro (SDK Java) |
| Infraestructura | Docker Compose (local) + AWS ECS Fargate + RDS + ALB (cloud) |
| CI/CD | GitHub Actions |

---

## Cómo correr el proyecto

```bash
# Levantar todo el stack
docker compose up -d --build

# Solo base de datos local (para desarrollo)
docker compose up -d postgres pgadmin
```

Detalles en [`Documentación/A2-Setup-Local.md`](./Documentación/A2-Setup-Local.md).

---

## Estado del proyecto al 30-may-2026

- **Sprint actual:** Sprint 5 (Semana 12) en cierre
- **Sprints completados:** 5 de 12
- **Hitos cumplidos:** 4 de 8 (incluye Experiencia 2 con 35%)
- **MVP completado:** ~70%
- **Funcionalidades extra implementadas (fuera del plan):** 27

Detalle en la [Carta Gantt actualizada](./Documentación/word/10-Carta-Gantt-30may2026.docx).
