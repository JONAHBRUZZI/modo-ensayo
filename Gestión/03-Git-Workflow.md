# 🔧 CONFIGURACIÓN GIT PARA EL EQUIPO

## 1. Configuración Global (Cada dev debe ejecutar)

```bash
# Configurar nombre y email
git config --global user.name "Tu Nombre"
git config --global user.email "tu@email.com"

# Configurar editor por defecto
git config --global core.editor "code"

# Configurar alias útiles
git config --global alias.st status
git config --global alias.co checkout
git config --global alias.br branch
git config --global alias.ci commit
git config --global alias.unstage 'restore --staged'
git config --global alias.last 'log -1 HEAD'
```

## 2. Setup del Repositorio (Común)

```bash
# Clonar repo
git clone https://github.com/JONAHBRUZZI/modo-ensayo.git
cd modo-ensayo

# Configurar tracking de ramas remotas
git fetch origin
git branch -u origin/dev dev
git branch -u origin/main main

# Ver configuración
git config --local -l
```

## 3. Flujo de Trabajo Diario

### Inicio del día
```bash
# 1. Ir a dev
git checkout dev

# 2. Actualizar desde remoto
git pull origin dev

# 3. Crear rama local para feature
git checkout -b feature/mi-feature
```

### Durante desarrollo
```bash
# Commits frecuentes
git add .
git commit -m "feat(scope): descripción breve"

# Ver estado
git status

# Ver cambios sin stagear
git diff

# Ver cambios stagebados
git diff --cached
```

### Antes de hacer push
```bash
# Verificar cambios
git log --oneline origin/dev..HEAD

# Actualizar desde dev (por si hay cambios)
git fetch origin
git rebase origin/dev

# Si hay conflictos
# 1. Resolver en editor
# 2. git add .
# 3. git rebase --continue
```

### Push y PR
```bash
# Hacer push
git push origin feature/mi-feature

# Crear PR desde GitHub UI o CLI:
gh pr create --base dev --head feature/mi-feature --title "Tu título" --body "Descripción"
```

## 4. Manejo de Conflictos

### Conflicto durante merge/rebase
```bash
# Ver archivos con conflicto
git status

# Abrir editor (VS Code)
code .

# Después de resolver
git add .
git rebase --continue  # si estás en rebase
# o
git merge --continue   # si estás en merge
```

## 5. Comandos Útiles

### Ver historial
```bash
# Último commit
git log -1

# Últimos 5 commits con gráfico
git log --oneline --graph -5

# Commits de alguien
git log --author="Developer Name"

# Entre dos ramas
git log dev..feature/mi-feature
```

### Cambios locales
```bash
# Descartar cambios en archivo
git checkout -- archivo.ts

# Descartar todos los cambios
git reset --hard

# Crear stash (guardar cambios temporalmente)
git stash
git stash pop
```

### Limpieza
```bash
# Eliminar rama local
git branch -d feature/mi-feature

# Eliminar rama local (forzado)
git branch -D feature/mi-feature

# Eliminar rama remota
git push origin --delete feature/mi-feature

# Limpiar referencias muertas
git remote prune origin
```

## 6. Guardianes de Rama (Branch Protection)

### main
```
✓ Requiere 2 review aprobadas
✓ Requiere que PRs estén up-to-date
✓ Requiere que todos checks pasen
✗ No allow force push
```

### dev
```
✓ Requiere 1 review aprobada
✓ Requiere que PRs estén up-to-date
✓ Requiere que todos checks pasen
✗ No allow force push
```

## 7. CI/CD (Implementar después)

```yaml
# .github/workflows/test.yml
name: Tests
on: [push, pull_request]
jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run tests
        run: cd backend && mvn test
        
  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run tests
        run: cd frontend && npm test
```

## 8. Gitignore (Debe incluir)

```
# OS
.DS_Store
Thumbs.db

# IDE
.vscode/
.idea/
*.swp
*.swo

# Dependencias
node_modules/
target/

# Archivos locales
.env.local
.env.*.local

# Construcción
dist/
build/

# Logs
*.log

# Imágenes/uploads
uploads/
docs/*.png

# Temporal
.claude/
```

## 9. Convención de Mensajes (Importante)

### ❌ MAL
```
git commit -m "fix bug"
git commit -m "updated"
git commit -m "asd"
```

### ✅ BIEN
```
git commit -m "fix(auth): corregir validación de email en login"
git commit -m "feat(classes): agregar filtro por nivel de dificultad"
git commit -m "refactor(services): mejorar manejo de errores API"
git commit -m "test(components): agregar tests para Button component"
git commit -m "docs: actualizar README con instrucciones Docker"
```

## 10. Problemas Comunes y Soluciones

### Problema: "Your branch is ahead of origin/main by 5 commits"
```bash
# Solución 1: Push tus cambios
git push origin main

# Solución 2: Si no quieres los cambios
git reset --hard origin/main
```

### Problema: "Conflict (add/add)"
```bash
# El archivo fue agregado en ambas ramas
# 1. Editar el archivo y decidir qué versión mantener
# 2. git add archivo
# 3. git rebase --continue
```

### Problema: "Accidental push al main"
```bash
# Reviertir el último commit (sin perder cambios)
git reset --soft HEAD~1

# Hacer commit en rama correcta
git checkout -b hotfix/accidental-fix
git commit -m "fix: corrección rápida"
git push origin hotfix/accidental-fix
# Crear PR para revisión
```

### Problema: "Necesito cambios de otra rama"
```bash
# Cherry-pick un commit específico
git cherry-pick abc1234

# Si quieres varios commits
git cherry-pick abc1234 def5678 ghi9012
```

---

**Importante:** Todos los devs deben leer y aplicar estas convenciones para mantener el repo limpio y profesional.
