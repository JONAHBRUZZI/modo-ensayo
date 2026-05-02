$ErrorActionPreference = "Stop"

# Variables de entorno para desarrollo local
# Reemplaza el token por uno sandbox valido de Mercado Pago.
$env:MERCADOPAGO_ACCESS_TOKEN = "TEST-xxxxxxxxxxxxxxxxxxxx"
$env:APP_BACKEND_URL = "http://localhost:8080"
$env:APP_FRONTEND_URL = "http://localhost:5173"

Set-Location "$PSScriptRoot\..\backend"
Write-Host "Iniciando backend con variables locales..." -ForegroundColor Cyan
.\mvnw.cmd spring-boot:run
