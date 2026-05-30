# Documentación · Modo Ensayo

Toda la documentación técnica, funcional y de planificación del proyecto.

## Índice

### Documentos principales

| Archivo | Contenido |
|---|---|
| [`00-PRD.md`](./00-PRD.md) | Problema, solución, alcance MVP |
| [`01-Metodologia.md`](./01-Metodologia.md) | Scrum aplicado al proyecto |
| [`02-Reglas-de-Negocio.md`](./02-Reglas-de-Negocio.md) | Las 18 reglas de negocio del sistema |
| [`03-Historias-de-Usuario.md`](./03-Historias-de-Usuario.md) | Historias de usuario formales con criterios de aceptación |
| [`04-Arquitectura.md`](./04-Arquitectura.md) | Arquitectura de la solución y patrones |
| [`05-Modelo-de-Datos.md`](./05-Modelo-de-Datos.md) | MER y esquema de la base de datos |
| [`06-API-Endpoints.md`](./06-API-Endpoints.md) | Catálogo de endpoints REST |
| [`07-Plan-de-Pruebas.md`](./07-Plan-de-Pruebas.md) | Plan preliminar de pruebas y cobertura |
| [`08-Justificacion-Tecnica.md`](./08-Justificacion-Tecnica.md) | Por qué cada decisión técnica |

### Anexos

| Archivo | Contenido |
|---|---|
| [`A1-Despliegue-AWS.md`](./A1-Despliegue-AWS.md) | Despliegue con Terraform en AWS |
| [`A2-Setup-Local.md`](./A2-Setup-Local.md) | Configuración del ambiente local |
| [`A3-Flujo-Usuario-Sin-Validar.md`](./A3-Flujo-Usuario-Sin-Validar.md) | Detalle del flujo de usuario nuevo |

### Documentos Word (entregables formales)

| Archivo | Descripción |
|---|---|
| [`word/10-Carta-Gantt-30may2026.docx`](./word/10-Carta-Gantt-30may2026.docx) | Carta Gantt actualizada al cierre del Sprint 5 |

Los Word son generados a partir de Markdown más estructura usando docx-js. El script reproducible está en `word/_generate_gantt.js`.

---

## Convenciones

- Los archivos están numerados para mantener un orden de lectura coherente.
- Cada documento es autocontenido pero contiene cross-references cuando aplica.
- Los diagramas se incluyen como Mermaid embebido y/o imágenes en `assets/`.
