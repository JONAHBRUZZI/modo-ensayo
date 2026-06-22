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
