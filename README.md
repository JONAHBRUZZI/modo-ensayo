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

El **código fuente** vive en `frontend/` (Vue SPA) y `supabase/` (migraciones SQL y Edge Functions) en la raíz del repo.

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
| Backend | Supabase (PostgreSQL 16 + Auth + Storage + Realtime) |
| Lógica de servidor | Supabase Edge Functions (Deno + TypeScript) |
| Seguridad de datos | Row Level Security (RLS) en PostgreSQL |
| Autenticación | Supabase Auth (email/password + Google OAuth, JWT) |
| Pagos | MercadoPago Checkout Pro + Connect marketplace con split (vía Edge Functions) |
| Hosting frontend | Vercel |
| CI/CD | GitHub Actions |

> El proyecto migró desde un backend Spring Boot a Supabase. El frontend Vue
> habla directamente con Supabase (PostgREST + Auth + Storage) y delega la
> lógica de negocio sensible a Edge Functions.

---

## Cómo correr el proyecto

Requisitos: Node.js 22+, [Supabase CLI](https://supabase.com/docs/guides/cli) y una cuenta de Supabase.

```bash
# 1. Frontend
cd frontend
cp .env.example .env          # completar VITE_SUPABASE_URL y VITE_SUPABASE_ANON_KEY
npm install
npm run dev                   # servidor de desarrollo en http://localhost:3001

# 2. (Opcional) Sincronizar/aplicar cambios de base de datos
supabase link --project-ref <project-ref>
supabase db push              # aplica las migraciones de supabase/migrations/
supabase functions deploy     # despliega las Edge Functions de supabase/functions/
```

Detalles en [`Documentación/A2-Setup-Local.md`](./Documentación/A2-Setup-Local.md).

---

## Estado del proyecto al 08-jul-2026

- **MVP completado:** 100% (todo desplegado)
- **Pagos**: inscripción a clases + arriendo de sala con split MercadoPago Connect (marketplace), desplegado
- **Panel de admin de pagos**: giros manuales a profesores, reembolsos fallidos, día de corte mensual y **margen real de MercadoPago** (comisión cobrada vs. costo MP)
- **Métricas de rendimiento (M1–M5)**: reales y clicleables, con **desglose por sede** (M4 Disponibilidad con latido interno de uptime)
- **Comportamiento**: **Google Analytics 4** integrado en el dashboard admin (usuarios activos, sesiones, vistas)
- **Asistencia**: flujo de "pasar lista" corregido (todos presentes por defecto)
- **Funcionalidades implementadas (fuera del plan original):** 40+

Detalle completo en el [changelog de mejoras](./Documentación/11-Mejoras-Incorporadas.md) y la [Carta Gantt actualizada](./Documentación/Carta-Gantt-13jun2026.md).
