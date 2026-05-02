# Setup Local de Colaboracion (Frontend / Backend / Infra)

Este documento explica como dejar el proyecto actualizado y funcionando en local para que cada desarrollador pueda trabajar sin romper el flujo del equipo.

## 1) Estructura oficial del repo

La estructura correcta es:

```text
modo-ensayo/
  backend/
  frontend/
  infra/
  docs/
  docker-compose.yml
```

No debe existir estructura duplicada como `backend/backend` o `frontend/frontend`.

## 2) Requisitos

- Git
- Docker Desktop (para base de datos/infra)
- Java 21+
- Node.js 20+
- npm 10+

## 3) Clonar y actualizar rama principal

```powershell
git clone <URL_DEL_REPO>
cd modo-ensayo
git checkout main
git pull origin main
```

## 4) Configuracion de entorno

Crear archivo `.env` en la raiz tomando `.env.example` como base:

```powershell
copy .env.example .env
```

Valores minimos recomendados:

```env
POSTGRES_DB=modoensayo
POSTGRES_USER=modoensayo
POSTGRES_PASSWORD=modoensayo
POSTGRES_PORT=5432

PGADMIN_DEFAULT_EMAIL=admin@modoensayo.com
PGADMIN_DEFAULT_PASSWORD=admin123
PGADMIN_PORT=5050

SPRING_PROFILES_ACTIVE=dev
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/modoensayo
SPRING_DATASOURCE_USERNAME=modoensayo
SPRING_DATASOURCE_PASSWORD=modoensayo

BACKEND_PORT=8080
FRONTEND_PORT=5173
NGINX_PORT=80

MERCADOPAGO_ACCESS_TOKEN=TEST-xxxxxxxxxxxxxxxxxxxx
APP_BACKEND_URL=http://localhost:8080
APP_FRONTEND_URL=http://localhost:5173
```

## 5) Levantar solo infraestructura base (recomendado para desarrollo)

Desde la raiz:

```powershell
docker compose up -d postgres pgadmin
```

Ver estado:

```powershell
docker compose ps
```

## 6) Levantar backend local

```powershell
cd backend
.\mvnw.cmd clean spring-boot:run
```

Backend esperado en: `http://localhost:8080`

## 7) Levantar frontend local

En otra terminal:

```powershell
cd frontend
npm install
npm run dev
```

Frontend esperado en: `http://localhost:5173`

## 8) Levantar todo con Docker (opcional)

Si quieren correr stack completo por contenedores:

```powershell
docker compose up -d
```

Servicios esperados:

- Postgres: `localhost:5432`
- PgAdmin: `localhost:5050`
- Backend: `localhost:8080`
- Frontend container: `localhost:3000`
- Nginx: `localhost:80`

## 9) Reglas de trabajo por equipo

### Frontend developer
- Trabaja en `frontend/src/**`
- Antes de push:

```powershell
cd frontend
npm run build
```

### Backend developer
- Trabaja en `backend/src/**`
- Antes de push:

```powershell
cd backend
.\mvnw.cmd test
```

### Infra developer
- Trabaja en `infra/**`, `docker-compose.yml`, `.env.example`
- Validar con:

```powershell
docker compose config
```

## 10) Flujo Git recomendado

1. Crear rama por tarea:

```powershell
git checkout -b feature/nombre-corto
```

2. Commits pequenos y claros.
3. Antes de push:

```powershell
git pull --rebase origin main
```

4. Resolver conflictos localmente y luego:

```powershell
git push -u origin feature/nombre-corto
```

## 11) Evitar conflictos masivos

- No mover carpetas raiz (`backend`, `frontend`, `infra`) sin acuerdo del equipo.
- No commitear `node_modules`, `target`, `.idea`, `.vite`, `dist`.
- Si aparecen trackeados por error:

```powershell
git rm -r --cached frontend/node_modules backend/target frontend/dist
git commit -m "chore: remove generated artifacts from git"
```

## 12) Troubleshooting rapido

### "Committing is not possible because you have unmerged files"

```powershell
git status
```

Resolver archivos con conflicto (`<<<<<<<`, `=======`, `>>>>>>>`), luego:

```powershell
git add .
git commit
```

### Puerto 8080/5173 ocupado

- Cerrar proceso que use el puerto o cambiar `BACKEND_PORT` / `FRONTEND_PORT`.

### Docker no levanta Postgres

- Revisar que Docker Desktop este encendido.
- Revisar logs:

```powershell
docker compose logs postgres
```

---

Si cada developer sigue este archivo, todos deberian poder levantar el proyecto y trabajar en paralelo sin romper estructura ni flujos.
