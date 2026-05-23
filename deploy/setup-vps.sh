#!/bin/bash
set -e

echo "=== Modo Ensayo - Instalacion de dependencias en VPS ==="

# Update system
sudo apt update && sudo apt upgrade -y

# Java 21
echo "Instalando Java 21..."
sudo apt install -y openjdk-21-jdk

# PostgreSQL
echo "Instalando PostgreSQL..."
sudo apt install -y postgresql postgresql-contrib
sudo systemctl enable postgresql
sudo systemctl start postgresql

# Create database and user
echo "Configurando base de datos..."
sudo -u postgres psql <<SQL
CREATE DATABASE modoensayo;
CREATE USER modoensayo WITH PASSWORD 'modoensayo';
GRANT ALL PRIVILEGES ON DATABASE modoensayo TO modoensayo;
\c modoensayo
GRANT ALL ON SCHEMA public TO modoensayo;
SQL

# Nginx
echo "Instalando nginx..."
sudo apt install -y nginx
sudo systemctl enable nginx
sudo systemctl start nginx

# Git and Maven
echo "Instalando git y maven..."
sudo apt install -y git maven

# Firewall
echo "Configurando firewall..."
sudo ufw allow 22
sudo ufw allow 80
sudo ufw allow 443
sudo ufw allow 8080
sudo ufw --force enable

echo ""
echo "=== Dependencias instaladas ==="
echo "Java:    $(java -version 2>&1 | head -1)"
echo "PSQL:    $(psql --version)"
echo "Nginx:   $(nginx -v 2>&1)"
echo "Maven:   $(mvn --version 2>&1 | head -1)"
echo ""
echo "Ahora ejecuta: bash deploy/deploy.sh"
