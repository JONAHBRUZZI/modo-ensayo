# Setup Local

Cómo dejar el proyecto funcionando en local. Tras la migración a Supabase, ya no
hay backend Java ni base de datos en Docker: el frontend Vue habla directamente
con un proyecto Supabase hosteado.

## 1) Estructura del repo

```text
modo-ensayo/
  frontend/         # Vue 3 SPA (lo que se corre en local)
  supabase/         # migraciones SQL + Edge Functions (se gestionan con la CLI)
  Documentación/    # documentación del proyecto
  Producto/         # artefactos del producto
  Gestión/          # gestión del equipo
```

## 2) Requisitos

- Git
- Node.js 22+ y npm 10+
- [Supabase CLI](https://supabase.com/docs/guides/cli) (solo si vas a tocar schema o Edge Functions)
- Una cuenta de Supabase con acceso al proyecto `modoensayo`

## 3) Clonar y actualizar

```powershell
git clone <URL_DEL_REPO>
cd modo-ensayo
git checkout main
git pull origin main
```

## 4) Configurar el frontend

Crear `frontend/.env` a partir del ejemplo:

```powershell
copy frontend\.env.example frontend\.env
```

Completar con los valores públicos del proyecto (Supabase Dashboard → Project
Settings → API):

```env
VITE_SUPABASE_URL=https://<project-ref>.supabase.co
VITE_SUPABASE_ANON_KEY=sb_publishable_xxxxxxxxxxxx
VITE_API_BASE_URL=
```

> La anon/publishable key es pública: el acceso real lo controla Row Level
> Security en la base de datos.

## 5) Correr el frontend

```powershell
cd frontend
npm install
npm run dev
```

Frontend en: `http://localhost:3001`

## 6) (Opcional) Trabajar con la base de datos / Edge Functions

Solo si vas a modificar el schema o las funciones:

```powershell
# Iniciar sesión y vincular el proyecto
supabase login
supabase link --project-ref <project-ref>

# Aplicar migraciones locales al proyecto vinculado
supabase db push

# Traer el historial remoto de migraciones a local
supabase migration fetch --yes

# Desplegar Edge Functions
supabase functions deploy

# Generar tipos TypeScript desde el schema
supabase gen types --linked > frontend/src/types/database.ts
```

## 7) Reglas de trabajo

### Frontend
- Trabaja en `frontend/src/**`
- Antes de push:

```powershell
cd frontend
npm run lint
npm run test
npm run build
```

### Base de datos / Edge Functions
- Cambios de schema → nueva migración en `supabase/migrations/`
- Tras un cambio DDL, revisar los advisors de seguridad y performance
- La base hosteada es la fuente de verdad del historial de migraciones

## 8) Flujo Git recomendado

```powershell
git checkout -b feature/nombre-corto
# ...commits pequeños y claros...
git pull --rebase origin main
git push -u origin feature/nombre-corto
```

No commitear `node_modules`, `dist`, ni archivos `.env` con secretos.

## 9) Troubleshooting

- **`Faltan VITE_SUPABASE_URL...`**: no creaste/completaste `frontend/.env`.
- **Puerto 3001 ocupado**: cierra el proceso o cambia el puerto en `vite.config.js`.
- **`Could not find the '<columna>' column ... in the schema cache`**: el código
  quedó desalineado con el schema; regenera tipos y revisa la migración.
- **Queries colgadas en "Cargando..."**: revisa que no haya múltiples instancias
  del cliente Supabase (debe usarse el singleton de `services/supabase.js`).
