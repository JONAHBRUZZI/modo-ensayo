# Guía de Despliegue (Vercel + Supabase)

Tras la migración, el despliegue tiene dos piezas:

- **Frontend (Vue SPA)** → Vercel
- **Backend (base de datos + lógica)** → Supabase (proyecto hosteado)

> La guía anterior de despliegue en AWS (ECS Fargate + RDS + ALB + Terraform)
> quedó obsoleta al eliminar el backend Spring Boot.

## Arquitectura de despliegue

```
Usuario
  │
  ├──► Vercel (frontend Vue, build estático servido por CDN)
  │       │
  │       └──► Supabase (mismas llamadas que en local)
  │
  └──► Supabase
          ├─ PostgreSQL + PostgREST (datos, RLS)
          ├─ Auth (login / JWT)
          ├─ Storage (archivos)
          ├─ Edge Functions (Deno) ──► MercadoPago API
          └─ pg_cron (tareas programadas)
```

## 1. Backend: Supabase

El proyecto hosteado ya existe (`modoensayo`). Para aplicar cambios:

```bash
# Vincular el workspace al proyecto
supabase login
supabase link --project-ref <project-ref>

# Aplicar migraciones de schema
supabase db push

# Desplegar Edge Functions
supabase functions deploy            # todas
supabase functions deploy <name>     # una sola

# Configurar secretos de las Edge Functions
supabase secrets set \
  MERCADOPAGO_ACCESS_TOKEN=APP_USR-... \
  MERCADOPAGO_WEBHOOK_SECRET=... \
  APP_FRONTEND_URL=https://<tu-dominio-vercel>
```

> Las claves `SUPABASE_URL` y `SUPABASE_SERVICE_ROLE_KEY` se auto-provisionan en
> la plataforma Supabase para las Edge Functions; no hay que configurarlas a mano.

### Autenticación (Google OAuth)

En Supabase Dashboard → Authentication → Providers, habilitar Google y registrar
el client ID/secret. En URL Configuration, agregar el dominio de Vercel a las
**Redirect URLs** y **Site URL**.

### Respaldo de la base de datos y réplica del ambiente de producción en pruebas

Procedimientos que garantizan que el ambiente de pruebas refleje fielmente el de producción
(ver también `Documentación/13-Respaldo-Rubrica.md`, punto 12 y `Documentación/07-Plan-de-Pruebas.md`
sección 3):

**1. Copia de seguridad de la base de datos de producción en el entorno de pruebas**

- Supabase genera respaldos automáticos (snapshots) diarios de la base de datos de producción,
  sin código adicional.
- Para llevar datos de producción al entorno de pruebas: volcado con `pg_dump` de la base de
  producción y restauración (`pg_restore` / `psql`) en el proyecto de desarrollo; alternativamente,
  branching de Supabase (`supabase branches create`).
- El esquema se reproduce de forma idéntica aplicando las migraciones versionadas de
  `supabase/migrations/` con `supabase db push`, evitando divergencias entre ambientes.

**2. Configuración del servidor (cloud) para que refleje producción**

```bash
supabase link --project-ref <project-ref-desarrollo>
supabase functions deploy            # mismas Edge Functions que producción
supabase secrets set ...             # mismos secretos (valores de sandbox)
```

El frontend se publica en Vercel con las mismas variables de entorno (`VITE_SUPABASE_URL`,
`VITE_SUPABASE_ANON_KEY`) apuntando al proyecto de desarrollo correspondiente.

**3. Instalación de lenguajes, bibliotecas y herramientas del servidor de producción**

- Lenguajes / runtimes: Node.js 22 y npm (frontend), Deno (Edge Functions), PostgreSQL 16
  (gestionado por Supabase).
- Herramientas: Supabase CLI para gestionar esquema y funciones; Git para control de versiones.
- Bibliotecas: se restauran de forma reproducible con `npm install` a partir de
  `package-lock.json`, garantizando las mismas versiones exactas en cualquier máquina.

## 2. Frontend: Vercel

El repo ya trae `vercel.json` en la raíz:

```json
{
  "buildCommand": "cd frontend && npm run build",
  "installCommand": "cd frontend && npm install",
  "outputDirectory": "frontend/dist"
}
```

### Pasos

1. Importar el repositorio en Vercel (o `vercel` / `vercel --prod` con la CLI).
2. Configurar las variables de entorno del proyecto en Vercel (Settings →
   Environment Variables):

   | Variable | Valor |
   |---|---|
   | `VITE_SUPABASE_URL` | `https://<project-ref>.supabase.co` |
   | `VITE_SUPABASE_ANON_KEY` | clave anon/publishable del proyecto |

3. Deploy. Vercel ejecuta el build y publica `frontend/dist` en su CDN.

> Estas variables son públicas (el control de acceso real lo hace RLS), por eso
> pueden vivir en el cliente.

## 3. Checklist de despliegue

- [ ] Migraciones aplicadas (`supabase db push`) sin drift (`supabase db diff --linked`)
- [ ] Edge Functions desplegadas y con secretos configurados
- [ ] Advisors de seguridad/performance revisados (sin tablas sin RLS)
- [ ] Google OAuth configurado con la URL de Vercel
- [ ] Variables `VITE_*` configuradas en Vercel
- [ ] Build de Vercel en verde y app accesible
- [ ] Webhook de MercadoPago apuntando a la Edge Function `mercadopago-webhook`

## 4. Costos

- **Supabase**: plan Free para desarrollo; Pro (~US$25/mes) para producción.
- **Vercel**: plan Hobby gratuito para proyectos personales; Pro según necesidad.

Mucho más simple y económico que el esquema AWS anterior (ECS + RDS + ALB +
NAT Gateway, ~US$75-85/mes).

## 5. Troubleshooting

- **`Could not find the '<columna>' column ... in schema cache`**: el schema en
  producción no coincide con el código; aplica migraciones y regenera tipos.
- **CORS / redirect en login**: revisa Site URL y Redirect URLs en Supabase Auth.
- **Webhook de pago no llega**: verifica que `mercadopago-webhook` tenga
  `verify_jwt = false` en `config.toml` y la URL registrada en MercadoPago.
- **Variables faltantes en build de Vercel**: confirma `VITE_SUPABASE_URL` y
  `VITE_SUPABASE_ANON_KEY` en el entorno del proyecto.
- **`supabase migration list` muestra migraciones locales como no aplicadas en
  remoto (o timestamps en remoto sin archivo local)**: es **drift de tracking**,
  no necesariamente un schema desincronizado — pasa cuando algún cambio se aplicó
  vía el conector MCP (`apply_migration`) en vez de `supabase db push`. Antes de
  reparar nada:
  1. Comparar contra el schema real con `supabase db dump --schema public` (buscar
     los objetos — tablas/funciones/triggers — que crea cada migración "pendiente").
  2. Si el objeto ya existe en el dump, reparar el tracking como aplicado:
     `supabase migration repair --status applied <timestamp>`.
  3. Si un timestamp remoto no tiene archivo local equivalente, marcarlo
     `--status reverted` (asumiendo que su contenido real ya está cubierto por
     otra migración local — verificar antes).
  4. Recién ahí `supabase db push` para aplicar lo que realmente falte.
  Ver el incidente documentado en `11-Mejoras-Incorporadas.md`, sección 13.
