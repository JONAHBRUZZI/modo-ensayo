#!/bin/bash
set -e

echo "=== Modo Ensayo - Deploy Backend ==="
echo ""

# Config
APP_DIR="/opt/modoensayo"
REPO="https://github.com/JONAHBRUZZI/modo-ensayo.git"
BRANCH="main"

# 1. Create user if not exists
if ! id modoensayo &>/dev/null; then
    echo "Creando usuario modoensayo..."
    sudo useradd -r -s /bin/false modoensayo
fi

# 2. Create directories
sudo mkdir -p $APP_DIR/uploads
sudo chown -R modoensayo:modoensayo $APP_DIR

# 3. Clone / pull repo
if [ -d "$APP_DIR/repo" ]; then
    echo "Actualizando repositorio..."
    cd $APP_DIR/repo
    git pull origin $BRANCH
else
    echo "Clonando repositorio..."
    git clone -b $BRANCH $REPO $APP_DIR/repo
fi

# 4. Build backend
echo "Compilando backend..."
cd $APP_DIR/repo/backend
chmod +x mvnw 2>/dev/null || true
./mvnw clean package -DskipTests

# 5. Deploy JAR
echo "Desplegando JAR..."
sudo cp target/modoensayo-*.jar $APP_DIR/modoensayo.jar
sudo chown modoensayo:modoensayo $APP_DIR/modoensayo.jar

# 6. Setup nginx
if ! [ -f /etc/nginx/sites-enabled/modoensayo ]; then
    echo "Configurando nginx..."
    sudo cp $APP_DIR/repo/deploy/nginx.conf /etc/nginx/sites-available/modoensayo
    sudo ln -sf /etc/nginx/sites-available/modoensayo /etc/nginx/sites-enabled/
    sudo rm -f /etc/nginx/sites-enabled/default
    sudo nginx -t && sudo systemctl reload nginx
fi

# 7. Setup systemd service
echo "Configurando servicio systemd..."
sudo cp $APP_DIR/repo/deploy/modoensayo.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable modoensayo
sudo systemctl restart modoensayo

# 8. Status
echo ""
echo "=== Deploy completado ==="
sudo systemctl status modoensayo --no-pager -l
echo ""
echo "Logs: sudo journalctl -u modoensayo -f"
echo "API:  http://$(hostname -I | awk '{print $1}'):8080"
echo ""
echo "Para activar SSL (HTTPS) con Let's Encrypt:"
echo "  1. Asegurate de tener un dominio apuntando a este servidor"
echo "  2. sudo apt install -y certbot python3-certbot-nginx"
echo "  3. Actualiza server_name en /etc/nginx/sites-available/modoensayo"
echo "  4. sudo certbot --nginx -d TU_DOMINIO"
