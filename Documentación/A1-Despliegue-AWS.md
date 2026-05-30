# Guia de Despliegue AWS con Terraform

## Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                          VPC                                 │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Subnets Publicas (2 AZs)                              │   │
│  │  - ALB (Application Load Balancer)                    │   │
│  │  - NAT Gateway                                        │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Subnets Privadas (2 AZs)                              │   │
│  │  - ECS Fargate (Backend Spring Boot)                  │   │
│  │  - RDS PostgreSQL 16                                  │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│ CloudFront → S3 (Frontend React)                            │
└─────────────────────────────────────────────────────────────┘
```

## Recursos Creados

| Recurso | Descripcion |
|---------|-------------|
| VPC | Red virtual con CIDR 10.0.0.0/16 |
| Subnets | 2 publicas + 2 privadas en AZs distintas |
| Internet Gateway | Acceso a internet para subnets publicas |
| NAT Gateway | Salida a internet para subnets privadas |
| ECS Cluster | Cluster Fargate para backend |
| ECS Task | Definicion con 512 CPU, 1GB RAM |
| ECS Service | 1 tarea deseada, auto-recovery |
| ECR | Repositorio para imagen backend |
| RDS | PostgreSQL 16, db.t3.micro, 20GB |
| ALB | Load balancer publico con health check |
| Security Groups | ALB, Backend, RDS (least privilege) |
| S3 | Bucket para archivos estaticos frontend |
| CloudFront | CDN para frontend con HTTPS |
| CloudWatch | Logs para tareas ECS |

## Flujo de Despliegue

### 1. Configurar credenciales AWS
```bash
aws configure
# O usar variables de entorno:
export AWS_ACCESS_KEY_ID="..."
export AWS_SECRET_ACCESS_KEY="..."
export AWS_DEFAULT_REGION="us-east-1"
```

### 2. Crear bucket para estado Terraform
```bash
aws s3 mb s3://modoensayo-terraform-state --region us-east-1
```

### 3. Configurar variables sensibles
```bash
# Crear infra/terraform/terraform.tfvars
cat > infra/terraform/terraform.tfvars <<EOF
db_username = "modoensayo"
db_password = "password-seguro-aqui"
EOF
```

### 4. Ejecutar Terraform
```bash
cd infra/terraform
terraform init
terraform plan -var="environment=dev"
terraform apply -var="environment=dev"
```

### 5. Obtener endpoints
```bash
terraform output
```

Outputs esperados:
- `alb_dns_name` → URL del backend API
- `backend_ecr_repository` → URL ECR para push de imagenes
- `rds_endpoint` → Endpoint de PostgreSQL
- `frontend_cloudfront_url` → URL del frontend
- `frontend_s3_bucket` → Nombre del bucket S3

### 6. Push de imagen backend a ECR
```bash
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account>.dkr.ecr.us-east-1.amazonaws.com

docker build -t modoensayo-backend -f infra/docker/backend.Dockerfile .
docker tag modoensayo-backend:latest <ecr-url>:latest
docker push <ecr-url>:latest
```

### 7. Deploy del frontend a S3
```bash
cd frontend
npm run build
aws s3 sync build/ s3://modoensayo-frontend-dev --delete
```

## Costos Estimados (MVP)

| Servicio | Costo Mensual Aprox |
|----------|---------------------|
| ECS Fargate (512 CPU, 1GB) | ~$10-15 |
| RDS db.t3.micro | ~$15-20 |
| ALB | ~$18 |
| CloudFront | ~$0.08/GB |
| S3 | ~$0.023/GB |
| NAT Gateway | ~$32 |
| **Total** | **~$75-85/mes** |

## Destruir Infraestructura
```bash
cd infra/terraform
terraform destroy -var="environment=dev"
```

## Variables Disponibles

| Variable | Default | Descripcion |
|----------|---------|-------------|
| aws_region | us-east-1 | Region AWS |
| environment | dev | Nombre del ambiente |
| project_name | modoensayo | Prefijo de recursos |
| db_username | - | Usuario master DB (requerido) |
| db_password | - | Password DB (requerido) |
| backend_image_tag | latest | Tag de imagen backend |
| vpc_cidr | 10.0.0.0/16 | CIDR de la VPC |
| allowed_cidrs | 0.0.0.0/0 | CIDRs permitidos al ALB |

## Troubleshooting

### Error: Bucket already exists
El bucket de estado Terraform debe ser unico globalmente. Cambiar nombre en `providers.tf`.

### ECS task no arranca
- Verificar logs en CloudWatch: `/ecs/modoensayo-backend`
- Revisar security groups permiten trafico ALB → Backend → RDS
- Validar credenciales de DB en variables de entorno

### RDS no accesible desde ECS
- Security group de RDS debe permitir puerto 5432 desde SG del backend
- Ambos deben estar en la misma VPC
