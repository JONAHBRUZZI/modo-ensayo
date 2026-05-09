# 🚀 ONBOARDING CHECKLIST - MODO ENSAYO

Use este checklist cuando un nuevo dev se una al equipo.

---

## DÍA 1: Setup Técnico

### Requisitos Previos
- [ ] Acceso a GitHub (invite enviado)
- [ ] Git instalado (`git --version`)
- [ ] VS Code instalado
- [ ] Node.js v22+ instalado (`node --version`)
- [ ] npm v10+ (`npm --version`)
- [ ] Docker Desktop instalado
- [ ] PostgreSQL client instalado (optional)

### Git Configuration
```bash
# 1. Configurar identidad
git config --global user.name "Tu Nombre"
git config --global user.email "tu@email.com"

# 2. Generar SSH key (si no tienes)
ssh-keygen -t ed25519 -C "tu@email.com"

# 3. Agregar SSH key a GitHub
# Copiar contenido de ~/.ssh/id_ed25519.pub
# Ir a: https://github.com/settings/keys

# 4. Verificar conexión SSH
ssh -T git@github.com
```

### Clonar Repositorio
```bash
# 1. Clonar repo
git clone git@github.com:JONAHBRUZZI/modo-ensayo.git
cd modo-ensayo

# 2. Verificar ramas
git branch -a

# 3. Checkout dev
git checkout dev
git pull origin dev
```

### Setup Backend (si vas a trabajar con backend)
```bash
cd backend

# 1. Descargar dependencias
./mvnw clean install

# 2. Verificar Maven
./mvnw --version

# 3. Construir proyecto
./mvnw clean package

# 4. Si pasa, backend ✓
```

### Setup Frontend (si vas a trabajar con frontend)
```bash
cd frontend

# 1. Instalar dependencias
npm install

# 2. Verificar scripts
npm run dev    # Debe abrir http://localhost:5173

# 3. Si funciona, frontend ✓
```

### Setup Docker (compartido)
```bash
cd ..

# 1. Iniciar servicios
docker compose up -d postgres pgadmin

# 2. Verificar
docker ps

# 3. Acceder pgAdmin
# URL: http://localhost:5050
# Email: admin@modoensayo.com
# Password: admin123
```

### Verificación Final
```bash
# Todos los comandos deben funcionar:
git --version
node --version
npm --version
docker --version
java -version
mvn --version        # Si tienes Maven
```

- [ ] Git configurado
- [ ] Repo clonado
- [ ] Backend builds correctamente
- [ ] Frontend inicia localmente
- [ ] Docker conecta con DB

---

## DÍA 2: Conocimiento del Proyecto

### Documentación
- [ ] Leer `README.md` (10 min)
- [ ] Revisar `docs/architecture.md` (20 min)
- [ ] Revisar `docs/database-schema.md` (15 min)
- [ ] Revisar `docs/api-endpoints.md` (30 min)
- [ ] Revisar `docs/business-rules.md` (20 min)

### Proyecto vs Archivo Actual
- [ ] Revisar `PLAN_TRABAJO_EQUIPOS.md` (30 min)
- [ ] Revisar `EQUIPOS_RESPONSABILIDADES.md` (20 min)
- [ ] Revisar `GIT_WORKFLOW.md` (20 min)

### Tour del Código
```
Frontend (15 min)
├─ pages/       → donde están las pantallas
├─ components/  → componentes reutilizables
├─ services/    → llamadas API
├─ router/      → rutas
└─ hooks/       → estado personalizado

Backend (20 min)
├─ auth/        → autenticación
├─ users/       → gestión usuarios
├─ classes/     → gestión clases
├─ payments/    → pagos
├─ venues/      → sedes
└─ shared/      → config global
```

### Stack Details
```
Frontend
├─ Framework:   Vue 3
├─ Build:       Vite
├─ Styling:     Tailwind CSS
├─ HTTP:        Axios
├─ Testing:     Vitest
└─ Linting:     ESLint + Prettier

Backend
├─ Framework:   Spring Boot
├─ ORM:         JPA/Hibernate
├─ DB:          PostgreSQL
├─ Auth:        JWT
├─ Payments:    Mercado Pago
└─ Testing:     JUnit 5
```

- [ ] Documentación leída
- [ ] Archivos del proyecto explorados
- [ ] Stack técnico entendido

---

## DÍA 3: Asignación de Tareas

### Según tu Rol

#### Si eres Backend Dev
- [ ] Leer `EQUIPOS_RESPONSABILIDADES.md` sección "Dev2"
- [ ] Clonar rama `feature/backend-setup`
- [ ] Crear primera rama: `feature/tu-nombre-hello-world`
- [ ] Tarea: Crear endpoint simple GET `/api/test` que retorna `{"message": "Hello"}` 
- [ ] Hacer commit: `feat(api): agregar endpoint de prueba`
- [ ] Crear PR para code review

#### Si eres Frontend Dev
- [ ] Leer `EQUIPOS_RESPONSABILIDADES.md` sección "Dev3"
- [ ] Clonar rama `feature/frontend-setup`
- [ ] Crear primera rama: `feature/tu-nombre-hello-world`
- [ ] Tarea: Crear componente `<HelloWorld />` simple
- [ ] Hacer commit: `feat(components): agregar componente HelloWorld`
- [ ] Crear PR para code review

#### Si eres Full Stack Dev
- [ ] Leer `EQUIPOS_RESPONSABILIDADES.md` sección "Dev1"
- [ ] Hacer review de código existente
- [ ] Crear rama: `feature/tu-nombre-infra`
- [ ] Tarea: Mejorar documentación de setup
- [ ] Hacer PR

- [ ] Rol definido
- [ ] Primera rama creada
- [ ] Primera tarea asignada
- [ ] PR creada para review

---

## SEMANA 1: Integración

### Lunes: Daily Standup
```
09:00 AM - Primer standup
├─ ¿Qué hice?: Setup y exploración
├─ ¿Qué hago?: Primera tarea
└─ ¿Bloqueantes?: Preguntar todo lo necesario
```
- [ ] Asistir a standup
- [ ] Presentarse al equipo

### Miércoles: Code Review
- [ ] Tu PR tiene feedback
- [ ] Aplica cambios sugeridos
- [ ] Mejora el código
- [ ] Espera aprobación
- [ ] Mergea a dev

### Viernes: Weekly Review
- [ ] Presente en weekly review
- [ ] Ve demo de tu feature
- [ ] Feedback del equipo
- [ ] Plan para próxima semana

- [ ] Asistencia a standups 100%
- [ ] Primer PR mergeado
- [ ] Feedback recibido y aplicado
- [ ] Próxima tarea clara

---

## SEMANA 2-3: Productividad

### Tareas
- [ ] Complea 2-3 features pequeñas
- [ ] Entiende flujo de trabajo
- [ ] Contribuye a code review
- [ ] Documenta dudas que surjan
- [ ] Propone mejoras

### Conocimiento
- [ ] Puedes iniciar proyecto solo
- [ ] Conoces convenciones del código
- [ ] Entiendes arquitectura
- [ ] Sabes cómo debuggear
- [ ] Conoces stack del equipo

### Colaboración
- [ ] Participas en standups
- [ ] Comunicación clara
- [ ] Pides ayuda cuando necesita
- [ ] Ayudas a otros compañeros

- [ ] Mínimo 3 PRs mergeadas
- [ ] 0 PRs rechazadas sin rebase
- [ ] Participación activa en meetings
- [ ] Comenzó mentoría con Dev1

---

## PRIMER MES: Evaluación

### Código
- [ ] Code style consistente
- [ ] Tests incluidos (>80% coverage)
- [ ] Commits con buen mensaje
- [ ] PRs bien documentadas
- [ ] Sin warnings en build

### Conocimiento
- [ ] Puede resolver tasks sin ayuda
- [ ] Entiende arquitectura completa
- [ ] Hace decisiones técnicas OK
- [ ] Documenta su código
- [ ] Propone mejoras

### Colaboración
- [ ] Comunicación fluida
- [ ] Respeta convenciones
- [ ] Code review constructivo
- [ ] Responsive en Slack/Teams
- [ ] Integrado al equipo

### Evaluación Formal
```
Criterio                   | Peso | Score (1-5)
---------------------------|------|----
Código Quality             | 30%  | ___
Productividad              | 25%  | ___
Colaboración               | 25%  | ___
Aprendizaje                | 20%  | ___
                           |      | TOTAL: ___/5
```

**Target:** 4.0+ para pasar onboarding

---

## RECURSOS IMPORTANTES

### Documentación
```
Interna:
├─ README.md                    → Overview
├─ PLAN_TRABAJO_EQUIPOS.md     → Plan maestro
├─ EQUIPOS_RESPONSABILIDADES.md → Tu rol
├─ GIT_WORKFLOW.md              → Cómo usar git
└─ docs/                        → Técnica
```

### Contactos
```
Dev1 (Lead):    @dev1   dev1@email.com
Dev2 (Backend): @dev2   dev2@email.com
Dev3 (Frontend):@dev3   dev3@email.com
```

### Herramientas
```
GitHub:      https://github.com/JONAHBRUZZI/modo-ensayo
pgAdmin:     http://localhost:5050
Frontend:    http://localhost:5173
Backend:     http://localhost:8080
Docs:        /docs folder
```

### Comandos Frecuentes
```bash
# General
git status
git checkout dev
git pull origin dev

# Crear rama
git checkout -b feature/tu-feature

# Cambios
git add .
git commit -m "feat(scope): mensaje"
git push origin feature/tu-feature

# Cleanup
git fetch --prune
git branch -d feature/completada
```

---

## PROBLEMAS FRECUENTES EN ONBOARDING

### "No puedo clonar el repo"
✓ Solución: Generar SSH key y agregar a GitHub

### "Backend no compila"
✓ Solución: `./mvnw clean install` luego `./mvnw compile`

### "Frontend no corre"
✓ Solución: `npm install` luego `npm run dev`

### "No puedo conectar a PostgreSQL"
✓ Solución: `docker compose up -d postgres` y esperar 30s

### "Conflicto de ramas"
✓ Solución: Contactar a Dev1 para help

### "No entiendo el flujo de trabajo"
✓ Solución: Leer `GIT_WORKFLOW.md` y preguntar en standup

---

## CHECKLIST FINAL

Antes de marcar onboarding completo:

- [ ] Ambiente local 100% funcional
- [ ] Primer PR mergeado
- [ ] Puedes hacer tareas sin ayuda
- [ ] Entiendes arquitectura
- [ ] Sabes dónde buscar información
- [ ] Puedes resolver bugs simples
- [ ] Comunicación fluida con equipo
- [ ] Contribuciones de calidad
- [ ] Evaluación 4.0+

---

## FEEDBACK Y MEJORA CONTINUA

Después del primer mes:
- [ ] Feedback formal de Dev1
- [ ] Áreas de mejora identificadas
- [ ] Mentorship plan para mes 2
- [ ] Goals para próximo mes
- [ ] 1-on-1 con Dev1 (30 min)

---

**Bienvenido al equipo Modo Ensayo! 🎉**

Si tienes preguntas, preúntale a Dev1 o en el standup diario.

Last updated: Mayo 9, 2026
