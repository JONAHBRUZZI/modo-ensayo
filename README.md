# Modo Ensayo

Plataforma para gestion de clases artisticas (danza, teatro, musica) con **pagos condicionados**: el alumno paga al reservar, el profesor cobra al completar la clase.

---

## Stack

| Capa | Tecnologia |
|------|-----------|
| Frontend | Vue 3 + Vite + Tailwind CSS |
| Backend | Spring Boot 3.2 + Java 21 + JWT |
| Base de datos | PostgreSQL 16 |
| Infra | Docker Compose (local), AWS/Terraform (cloud) |
| Pagos | MercadoPago Checkout Pro (sandbox) |

---

## Levantar en local

### Requisitos

- Docker Desktop
- Java 21+ (backend)
- Node.js 20+ (frontend)

### 1. Clonar y configurar

```powershell
git clone https://github.com/JONAHBRUZZI/modo-ensayo.git
cd modo-ensayo
copy .env.example .env
```

### 2. Base de datos

```powershell
docker compose up -d postgres
```

PostgreSQL queda en `localhost:5432`. Credenciales: `modoensayo` / `modoensayo`.

### 3. Backend

```powershell
cd backend
$env:MERCADOPAGO_ACCESS_TOKEN="TEST-xxxxxxxxxxxxxxxxxxxx"
./mvnw spring-boot:run
```

Backend en `http://localhost:8080`.

### 4. Frontend

```powershell
cd frontend
npm install
npm run dev
```

Frontend en `http://localhost:3000`.

### URLs

| Servicio | URL |
|----------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080/api |
| pgAdmin | http://localhost:5050 |

---

## Usuarios de prueba

| Email | Password | Rol |
|-------|----------|-----|
| admin@test.com | admin123 | ADMIN |
| teacher@test.com | teacher123 | TEACHER |
| user@test.com | user123 | USER |
| venueadmin@test.com | venue123 | VENUE_ADMIN |

---

## Estructura del proyecto

```
modo-ensayo/
  backend/          # Spring Boot (paquetes por dominio)
    src/main/java/com/modoensayo/
      auth/         # Login, registro, JWT
      users/        # Perfiles, roles, verificaciones
      classes/      # Clases y horarios
      payments/     # Carrito, MercadoPago, pagos retenidos
      venues/       # Sedes y salas artisticas
      admin/        # Panel de administracion
      shared/       # Seguridad, excepciones, utilidades
  frontend/         # Vue 3 SPA
    src/
      pages/        # Vistas por rol
      components/   # Componentes reutilizables
      services/     # Llamadas API (Axios)
      router/       # Vue Router con guards JWT
      hooks/        # Composables (useAuth, etc.)
  infra/            # Dockerfiles, nginx, scripts DB, Terraform
  docs/             # Documentacion del equipo
```

---

## Flujo de pagos

1. Usuario agrega clases al carrito
2. Paga con MercadoPago Checkout Pro (sandbox en dev)
3. Pago queda en estado **RETAINED** (retenido)
4. Al completar la clase, trigger en BD libera el pago a **RELEASED**

Nunca se usa `retained` ni `released`. Estados reales: `RETAINED` → `RELEASED` (trigger automatico), o `RETAINED` → `REFUND_PENDING` → `REFUNDED` (cancelacion).

---

## MercadoPago (desarrollo)

```powershell
$env:MERCADOPAGO_ACCESS_TOKEN="TEST-xxxxxxxxxxxxxxxxxxxx"
```

Para recibir webhooks en local, usar ngrok:

```powershell
ngrok http 8080
# Usar la URL HTTPS como APP_BACKEND_URL
```

---

## Equipo y workflow

Ver [`docs/WORKFLOW.md`](docs/WORKFLOW.md) — roles, git workflow, convenciones, code review y tips de IA.

TL;DR:

| Dev | Rama | Responsabilidad |
|-----|------|----------------|
| Jonathan | `main` | Revisa PRs, estabilidad |
| Victor | `frontend/*` | Vue, componentes, paginas |
| Darllete | `backend/*` | API, seguridad, BD |

- **Nadie pushea directo a `main`**. Todo por PR.
- Ramas: `feature/`, `fix/`, `chore/`, `docs/`
- Commits: `tipo: descripcion en espanol`

---

## Despliegue AWS (opcional)

Infraestructura como codigo con Terraform en `infra/terraform/`. Despliega ECS Fargate + RDS + ALB + S3/CloudFront.

```powershell
cd infra/terraform
terraform init
terraform plan -var="environment=dev"
terraform apply -var="environment=dev"
```

Costo estimado MVP: ~$75-85/mes.
