# Usuarios de prueba y acceso a base de datos

## 1) Usuarios de prueba (demo)

- `admin@test.com` / `admin123` (ADMIN)
- `teacher@test.com` / `teacher123` (TEACHER)
- `user@test.com` / `user123` (USER)
- `venueadmin@test.com` / `venue123` (VENUE_ADMIN)

Estos usuarios se crean/actualizan desde `backend/src/main/java/com/modoensayo/shared/config/DataInitializer.java`.

## 2) Acceso a PostgreSQL por Docker

Desde la raiz del proyecto:

```powershell
docker compose up -d postgres pgadmin
```

Verifica que levante:

```powershell
docker compose ps
```

## 3) Conexion directa a PostgreSQL (cliente SQL)

Usa estos datos (segun `.env` / `.env.example`):

- Host: `localhost`
- Puerto: `5432`
- Database: `modoensayo`
- Usuario: `modoensayo`
- Password: `modoensayo`

Si tu `.env` tiene otros valores, usa esos.

## 4) Acceso con pgAdmin (web)

- URL: `http://localhost:5050`
- Email: `admin@modoensayo.com`
- Password: `admin123`

Al entrar en pgAdmin, crea un server con:

- Name: `modoensayo-local`
- Host name/address: `postgres` (si pgAdmin corre en Docker)
- Port: `5432`
- Maintenance DB: `modoensayo`
- Username: `modoensayo`
- Password: `modoensayo`

Si pgAdmin esta fuera de Docker, usa `localhost` como host.

## 5) Consultas utiles

Listar usuarios:

```sql
select id, email, full_name, created_at
from users
order by created_at desc;
```

Ver roles por usuario:

```sql
select u.email, r.name as role
from users u
join user_roles ur on ur.user_id = u.id
join roles r on r.id = ur.role_id
order by u.email, r.name;
```

Ver clases:

```sql
select id, title, discipline, status, start_time, end_time
from classes
order by created_at desc;
```

Ver pagos:

```sql
select p.id, p.amount, p.status, e.class_id, e.beneficiary_type, e.beneficiary_id
from payments p
join enrollments e on e.id = p.enrollment_id
order by p.created_at desc;
```

## 6) Reiniciar base de datos limpia

Esto elimina data local y vuelve a ejecutar scripts de `infra/postgres/init`:

```powershell
docker compose down -v
docker compose up -d postgres pgadmin
```
