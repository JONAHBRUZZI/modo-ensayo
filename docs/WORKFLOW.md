# Modo Ensayo — Workflow de equipo

## Equipo

| Desarrollador | Rama base | Responsabilidad |
|--------------|-----------|----------------|
| **Jonathan** | `main` | Revisor de PRs, estabilidad, merge a main |
| **Victor** | `frontend/*` | Vue 3, Vite, Tailwind, componentes, páginas |
| **Darllete** | `backend/*` | Spring Boot, JPA, API REST, seguridad, BD |

---

## Stack

- **Frontend**: Vue 3 + Vite + Tailwind CSS (puerto 3000)
- **Backend**: Spring Boot 3.2 + Java 21 + PostgreSQL + JWT (puerto 8080)
- **Infra**: Docker Compose (postgres, pgadmin)
- **Pagos**: MercadoPago Checkout Pro (sandbox)

---

## Setup inicial

### Requisitos

- Git + GitHub SSH
- Docker Desktop
- Java 21+ (backend)
- Node.js 20+ (frontend)

### Todos los devs

```powershell
git clone https://github.com/JONAHBRUZZI/modo-ensayo.git
cd modo-ensayo
copy .env.example .env
docker compose up -d postgres
```

### Backend (Darllete)

```powershell
cd backend
$env:MERCADOPAGO_ACCESS_TOKEN="TEST-xxxxxxxxxxxxxxxxxxxx"
./mvnw spring-boot:run
```

### Frontend (Victor)

```powershell
cd frontend
npm install
npm run dev
```

### URLs locales

| Servicio | URL |
|----------|-----|
| Frontend | `http://localhost:3000` |
| Backend | `http://localhost:8080` |
| pgAdmin | `http://localhost:5050` |

---

## Git workflow

### Regla de oro

**Nadie pushea a `main`.** Todo entra por PR, y Jonathan lo revisa y mergea.

### Config inicial (cada dev una vez)

```powershell
git config --global user.name "Tu Nombre"
git config --global user.email "tu@email.com"
```

### Rama por tarea

```powershell
git checkout main
git pull origin main
git checkout -b feature/descripcion-corta
```

### Prefijos de rama

| Prefijo | Uso |
|---------|-----|
| `feature/` | Nueva funcionalidad |
| `fix/` | Correccion de bug |
| `chore/` | Mantenimiento, build, deps |
| `docs/` | Documentacion |

### Commits

```powershell
git add archivos-especificos
git commit -m "tipo: descripcion clara"
```

Tipos: `feat`, `fix`, `chore`, `docs`, `test`, `refactor`

Ejemplos:
```
feat: agregar filtro por disciplina en clases
fix: corregir validacion de email en login
test: agregar tests para AuthService
docs: actualizar endpoints en WORKFLOW
```

### Antes de pushear

```powershell
git pull --rebase origin main
```

Si hay conflictos, resolvelos localmente. Las secciones con `<<<<<<<`, `=======`, `>>>>>>>` indican conflicto — edita el archivo, decidi que version mantener, luego:

```powershell
git add .
git rebase --continue   # si estas en rebase
```

### Pull Request

1. `git push -u origin feature/mi-rama`
2. Crear PR en GitHub hacia `main`
3. **Jonathan revisa y mergea** (Victor/Darllete no mergean sus propios PRs)

---

## Responsabilidades

### Jonathan (estabilidad)

- Revisar y mergear PRs a `main`
- Verificar que `main` compile (`./mvnw test` y `npm run build`)
- Mantener `.gitignore` (nunca se commitea `.env`, `node_modules/`, `target/`, `dist/`)
- Apagar incendios y desbloquear al equipo

### Victor (frontend)

- Trabajar en `frontend/src/` (pages, components, services, router, hooks)
- Probar con `npm run build` antes de commitear
- No tocar `backend/` sin coordinar

### Darllete (backend)

- Trabajar en `backend/src/main/java/com/modoensayo/`
- Cada dominio en su paquete: `auth/`, `users/`, `classes/`, `payments/`, `venues/`
- Usar DTOs, **nunca exponer entidades JPA** directamente
- Probar con `./mvnw test` antes de commitear
- No tocar `frontend/` sin coordinar

---

## Usuarios de prueba

| Email | Password | Rol |
|-------|----------|-----|
| admin@test.com | admin123 | ADMIN |
| teacher@test.com | teacher123 | TEACHER |
| user@test.com | user123 | USER |
| venueadmin@test.com | venue123 | VENUE_ADMIN |

---

## API resumen

Base: `/api`

| Method | Endpoint | Auth |
|--------|----------|------|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| GET | `/api/users/me` | JWT |
| GET | `/api/classes` | Public |
| POST | `/api/classes` | TEACHER/ADMIN |
| GET | `/api/venues` | Public |
| POST | `/api/payments/cart` | JWT |
| POST | `/api/payments/checkout` | JWT |
| GET | `/api/admin/users` | ADMIN |

---

## Estados del negocio

**Clase**: `SCHEDULED` → `IN_PROGRESS` → `COMPLETED`

**Pago**: `RETAINED` → `RELEASED` (trigger automatico al completar clase)

**Verificacion/Sede**: `PENDING` → `APPROVED` o `REJECTED`

**Regla core**: el pago se retiene hasta que la clase se completa.

---

## Code review checklist

```
[ ] Sigue las convenciones del proyecto (DTOs, paquetes por dominio)
[ ] Tests incluidos (o explicacion de por que no)
[ ] Sin conflictos con main
[ ] Validaciones de entrada
[ ] Sin N+1 queries
[ ] Mensajes de error claros
```

---

## Problemas comunes

| Problema | Solucion |
|----------|----------|
| Puerto 8080 ocupado | `$env:SERVER_PORT=9090` antes de levantar backend |
| MercadoPago falla | `$env:MERCADOPAGO_ACCESS_TOKEN="TEST-..."` |
| Docker no levanta | Verificar que Docker Desktop este corriendo |
| `.env` aparece en git | `git rm --cached .env` y verificar `.gitignore` |
| Conflicto en rebase | Resolver, `git add .`, `git rebase --continue` |
| Rama atrasada | `git pull --rebase origin main` |

---

## Roadmap

```
Semanas 1-2:  Estabilizacion + tests + fixes
Semanas 3-4:  Features core (webhooks MP, notificaciones, dashboard)
Semanas 5-6:  Integraciones y filtros avanzados
Semanas 7-8:  Performance, seguridad, despliegue AWS
```

---

## Integracion con IA (Claude Code, Copilot, Cursor)

### Al iniciar sesion con IA

Pega este contexto para que la IA entienda el proyecto:

```
Proyecto Modo Ensayo — Spring Boot 3.2 (Java 21) + Vue 3 + PostgreSQL
Trabajo en [frontend/backend]. Solo modifico [frontend/src | backend/src].
Estructura backend: paquetes por dominio (auth, users, classes, payments, venues, shared).
Estructura frontend: pages, components, services, router, hooks, features.
Siempre usar DTOs, nunca exponer entidades JPA. Usar @PreAuthorize en endpoints protegidos.
Convencion de commits: "tipo: descripcion en espanol".
```

### Buenas practicas con IA

1. Da paths exactos: `backend/src/main/java/com/modoensayo/auth/controller/AuthController.java`
2. Pide que siga las convenciones existentes (mirar archivos vecinos como ejemplo)
3. Para BD: usar JPA/Hibernate, no SQL directo
4. Antes de aceptar codigo: verifica que compile y pase tests
5. Nunca compartas el `.env` ni tokens reales con la IA
6. Si la IA sugiere crear archivos nuevos, validar que sigan la estructura de paquetes del proyecto
