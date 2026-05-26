#!/bin/bash
set -e

echo "=== Modo Ensayo - Docker Deploy ==="

# Check docker
if ! command -v docker &>/dev/null; then
    echo "Instalando Docker..."
    curl -fsSL https://get.docker.com | sudo bash
    sudo usermod -aG docker $USER
    echo "Docker instalado. Cierra sesion y vuelve a entrar, luego ejecuta este script de nuevo."
    exit 0
fi

# Clone repo if needed
if [ ! -f docker-compose.yml ]; then
    echo "Clonando repositorio..."
    git clone https://github.com/JONAHBRUZZI/modo-ensayo.git .
fi

# Pull latest
git pull origin main

# Build and start
echo "Construyendo y desplegando..."
docker compose down --remove-orphans 2>/dev/null || true
docker compose build backend
docker compose up -d

# Wait for healthy
echo "Esperando que los servicios esten listos..."
sleep 10

# Status
echo ""
echo "=== Servicios ==="
docker compose ps
echo ""
echo "Logs: docker compose logs -f backend"
echo "API:  http://$(hostname -I | awk '{print $1}')"
echo ""
echo "Para SSL con Let's Encrypt:"
echo "  1. Apunta tu dominio a este servidor"
echo "  2. docker compose run --rm certbot certonly --webroot -w /var/www/certbot -d TU_DOMINIO"
