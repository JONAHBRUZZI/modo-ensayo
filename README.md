# Modo Ensayo

## Descripcion
Plataforma para gestion de clases artisticas con pagos condicionados.

## Stack
- React
- Spring Boot
- PostgreSQL
- Docker

## Como correr el proyecto

```bash
docker compose up -d --build
```

### Levantar solo base de datos local

```bash
docker compose up -d postgres pgadmin
```

El contenedor de PostgreSQL ejecuta automaticamente:
- `infra/postgres/init/01_schema.sql` (DDL)
- `infra/postgres/init/02_seed.sql` (datos semilla)

Si ya habias levantado PostgreSQL antes y quieres re-ejecutar init scripts:

```bash
docker compose down -v
docker compose up -d postgres pgadmin
```

pgAdmin queda disponible en `http://localhost:5050`.

Credenciales por defecto (archivo `.env.example`):
- Email: `admin@modoensayo.com`
- Password: `admin123`

Datos para registrar el server de PostgreSQL dentro de pgAdmin:
- Host: `postgres`
- Port: `5432`
- Database: `modoensayo`
- Username: `modoensayo`
- Password: `modoensayo`

## Integracion Mercado Pago

La plataforma utiliza **Mercado Pago Checkout Pro** para procesar pagos.

### Flujo de Pago
1. Usuario agrega clases al carrito
2. Al hacer clic en "Pagar con Mercado Pago", el backend crea una **preferencia de pago**
3. Usuario es redirigido a Mercado Pago (modo sandbox en desarrollo)
4. Despues del pago, Mercado Pago redirige a:
   - `/payment/success` - Pago exitoso
   - `/payment/failure` - Pago cancelado/rechazado
   - `/payment/pending` - Pago en proceso (efectivo, transferencia)

### Configuracion
Para desarrollo, configurar las siguientes variables de entorno:
```bash
MERCADOPAGO_PUBLIC_KEY=TEST-xxxxxxxxxxxxxxxxxxxx
MERCADOPAGO_ACCESS_TOKEN=TEST-xxxxxxxxxxxxxxxxxxxx
APP_FRONTEND_URL=http://localhost:5173
APP_BACKEND_URL=https://tu-subdominio.ngrok-free.app
```

> Para recibir webhooks en local, levanta un tunel con ngrok:
> `ngrok http 8080`
> y usa la URL HTTPS en `APP_BACKEND_URL`.

Para produccion, usar credenciales de produccion:
```bash
MERCADOPAGO_ACCESS_TOKEN=APP_USR-...
```

## Despliegue AWS con Terraform

### Requisitos
- AWS CLI configurado (`aws configure`)
- Terraform >= 1.0 instalado
- Bucket S3 para estado remoto (`modoensayo-terraform-state`)

### Variables requeridas
Crear archivo `infra/terraform/terraform.tfvars`:
```hcl
db_username = "modoensayo"
db_password = "tu-password-seguro"
```

### Comandos
```bash
# Inicializar Terraform
cd infra/terraform
terraform init

# Planificar
terraform plan -var="environment=dev"

# Aplicar
terraform apply -var="environment=dev"

# Ver outputs (URLs, endpoints)
terraform output
```

### Arquitectura desplegada
- **VPC** con subnets publicas/privadas en 2 AZs
- **ECS Fargate** para backend Spring Boot
- **RDS PostgreSQL** en subnet privada
- **ALB** para routing y health checks
- **S3 + CloudFront** para frontend React
- **ECR** para imagenes Docker del backend

### Destruir infraestructura
```bash
terraform destroy -var="environment=dev"
```

## Estructura
Arquitectura modular por dominio.

## Estado
MVP en desarrollo.

## Estrategia de ramas
- `main`
- `develop`
- `feature/*`

Ejemplos:
- `feature/auth-login`
- `feature/payment-checkout`
- `feature/reschedule-flow`

## Buenas practicas
1. Un modulo = un dominio real. No mezclar pagos con clases.
2. Nada de logica en controllers. Todo en `service`.
3. DTOs obligatorios. Nunca exponer entidades directamente.
4. Enums para estados (ejemplo: `RETAINED`, `RELEASED`, `REFUND_PENDING`).
5. Logs de estados desde el inicio.
