# Architecture

## Stack

- **Frontend**: Vue 3 (Composition API) + Vite + Tailwind CSS
- **Backend**: Spring Boot 3.2 (Java 21) + JPA/Hibernate + Spring Security + JWT
- **Base de datos**: PostgreSQL 16
- **Infra local**: Docker Compose (postgres, pgAdmin, backend, frontend, nginx)
- **Infra cloud**: AWS con Terraform (ECS Fargate, RDS, ALB, S3 + CloudFront)
- **Pagos**: MercadoPago SDK Java (Checkout Pro)

## Estructura del Proyecto

```
modo-ensayo/
  backend/          # Spring Boot (Java 21 + Maven)
    src/main/java/com/modoensayo/
      admin/        # Panel de administracion
      associates/   # Gestión de asociados/familiares
      auth/         # Autenticacion (login, register, JWT)
      classes/      # Clases y horarios
      payments/     # Carrito, pagos, MercadoPago
      users/        # Perfil, roles, verificaciones, reembolsos
      venues/       # Sedes y salas artisticas
      shared/       # Seguridad, excepciones, utilidades
  frontend/         # Vue 3 SPA
    src/
      components/   # Componentes reutilizables
      features/     # Modulos por dominio (feature-sliced)
      hooks/        # Composables Vue 3
      layouts/      # Layout de la aplicacion
      pages/        # Vistas/route components
      router/       # Vue Router con guards
      services/     # Axios API services
  infra/            # Infraestructura como codigo
    docker/         # Dockerfiles
    nginx/          # Config nginx reverse proxy
    postgres/init/  # Scripts SQL de inicializacion
    terraform/      # IaC para AWS
    scripts/        # Scripts de despliegue
  docs/             # Documentacion
```

## Principios de Diseno

1. **Package by feature** en backend: cada dominio tiene su propio modulo con controller, service, domain, dto, repository
2. **DTOs obligatorios**: nunca exponer entidades JPA directamente en la API
3. **Logica en service layer**: controladores solo orquestan, no contienen logica de negocio
4. **Enums para estados**: todos los estados del sistema usan enums Java y CHECK constraints en BD
5. **Triggers de negocio en BD**: reglas criticas (liberacion de pagos, control de capacidad) implementadas en la base de datos como ultima linea de defensa

## Flujo de Datos

```
Usuario -> Nginx (/:3000) -> Frontend (Vue SPA)
                                |
Usuario -> Nginx (/api/ -> :8080) -> Backend (Spring Boot)
                                           |
                                      PostgreSQL
                                           |
                                      MercadoPago API
```

## Patrones

- **Singleton** para estado compartido en frontend (useAuth composable)
- **Repository pattern** con Spring Data JPA
- **JWT stateless** para autenticacion
- **Circuit breaker implicito** en MercadoPago SDK
