# Modo Ensayo — Workflow de equipo

## Equipo

| Desarrollador | Rama | Responsabilidad |
|--------------|------|----------------|
| **Jonathan** | `main` | Revisor de PRs, estabilidad general, merge a main |
| **Victor** | `frontend/*` | Vue 3, Vite, Tailwind, componentes, páginas, servicios |
| **Darllete** | `backend/*` | Spring Boot, JPA, API REST, seguridad, BD |

---

## Stack

- **Frontend**: Vue 3 + Vite + Tailwind CSS (puerto 3000)
- **Backend**: Spring Boot 3.2 + Java 21 + PostgreSQL + JWT (puerto 8080)
- **Infra**: Docker Compose (postgres, pgadmin)
- **Pagos**: MercadoPago Checkout Pro (sandbox)

---

## Setup inicial (cada dev lo hace una vez)

```powershell
git clone https://github.com/JONAHBRUZZI/modo-ensayo.git
cd modo-ensayo
git checkout main
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

---

## Git Workflow

### Regla de oro
**Nadie pushea directo a `main`.** Todo entra por PR.

### Rama por tarea

```powershell
git checkout main
git pull origin main
git checkout -b feature/descripcion-corta
```

### Convencion de prefijos

| Prefijo | Uso |
|---------|-----|
| `feature/` | Nueva funcionalidad |
| `fix/` | Correccion de bug |
| `chore/` | Tareas de mantenimiento |
| `docs/` | Documentacion |

### Commits

```powershell
git add archivos-especificos
git commit -m "tipo: descripcion clara en espanol"
```

Ejemplos: `feat: agregar login con JWT`, `fix: corregir proxy de vite`, `chore: actualizar dependencias`

### Antes de pushear

```powershell
git pull --rebase origin main
```

Si hay conflictos, resolverlos localmente. Si no estas seguro, consultar a Jonathan.

### Pull Request

1. Pushear la rama: `git push -u origin feature/mi-rama`
2. Crear PR en GitHub hacia `main`
3. **Jonathan revisa y mergea**

---

## Responsabilidades diarias

### Jonathan (estabilidad)
- Revisar PRs entrantes y hacer merge a `main`
- Verificar que `main` siempre compile y levante
- Aprobar solo PRs que pasen `./mvnw test` (backend) y `npm run build` (frontend)
- Mantener limpio el `.gitignore` (nunca se commitea `.env`, `node_modules/`, `target/`, `dist/`)

### Victor (frontend)
- Trabajar en `frontend/src/`
- Usar la carpeta `features/` para nuevos modulos
- Probar con `npm run build` antes de commitear
- No tocar `backend/` sin coordinar

### Darllete (backend)
- Trabajar en `backend/src/main/java/com/modoensayo/`
- Cada dominio tiene su paquete: `auth/`, `users/`, `classes/`, `payments/`, `venues/`
- Usar DTOs, nunca exponer entidades JPA directamente
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

| Endpoint | Auth | Quien |
|----------|------|-------|
| `POST /api/auth/register` | Public | — |
| `POST /api/auth/login` | Public | — |
| `GET /api/users/me` | JWT | — |
| `GET /api/classes` | Public | — |
| `POST /api/classes` | TEACHER/ADMIN | Darllete |
| `POST /api/payments/checkout` | JWT | — |
| `GET /api/admin/users` | ADMIN | Darllete |

---

## Estados clave del negocio

**Clase**: `SCHEDULED` → `IN_PROGRESS` → `COMPLETED`

**Pago**: `RETAINED` → `RELEASED` (al completar clase)

**Verificacion/Sede**: `PENDING` → `APPROVED` o `REJECTED`

---

## Troubleshooting

- **Puerto ocupado**: cerrar el proceso o usar otro puerto con `$env:SERVER_PORT=9090`
- **MercadoPago error**: definir `$env:MERCADOPAGO_ACCESS_TOKEN` antes de levantar backend
- **Docker no levanta**: verificar que Docker Desktop este corriendo
- **`.env` expuesto**: ya esta en `.gitignore`, nunca commitearlo. Si aparece, `git rm --cached .env`

---

## Integracion con IA (Claude Code, Copilot, etc.)

1. Da contexto al iniciar sesion: pega este documento o su resumen.
2. Se especifico con los paths: `backend/src/main/java/com/modoensayo/auth/...`
3. Pide que siga las mismas convenciones de paquetes y DTOs.
4. Para cambios en BD, recuerdale que use JPA/Hibernate (no SQL directo).
5. Antes de aceptar codigo generado por IA, verifica que compile y pase tests.
6. Siempre inclui `@PreAuthorize` en endpoints nuevos que requieran autenticacion.
