# Despliegue del marketplace de pagos (arriendo de salas con MercadoPago Connect)

Checklist para poner en marcha el split de pagos. El **código ya está en la rama
`victor`** (Fases A–D). Falta desplegarlo. Mientras no se complete, NO mergear
`victor` → `main`, porque el frontend mostraría botones que aún no funcionan.

## 0. Prerrequisitos
- [ ] **App de MercadoPago Connect** creada en https://www.mercadopago.cl/developers/panel/app
      → anotar **Client ID** y **Client Secret**.
- [ ] **CLI de Supabase** instalado, `supabase login` y
      `supabase link --project-ref remznaanexwgzeeupctv` hechos.

## 1. Aplicar migraciones (SQL Editor del dashboard o `supabase db push`)
Aplicar **en este orden** (la 001 va sola y antes de la 002, por el `ALTER TYPE`):
- [ ] `supabase/migrations/20260624000000_mp_connect_seller_accounts.sql`
- [ ] `supabase/migrations/20260624000001_block_status_held.sql`
- [ ] `supabase/migrations/20260624000002_room_reservation_holds.sql`

## 2. Configurar secretos de las Edge Functions
```
supabase secrets set MERCADOPAGO_CLIENT_ID=TU_CLIENT_ID
supabase secrets set MERCADOPAGO_CLIENT_SECRET=TU_CLIENT_SECRET
```
(`MERCADOPAGO_ACCESS_TOKEN`, `MERCADOPAGO_WEBHOOK_SECRET` y `APP_FRONTEND_URL` ya
están configurados desde el flujo de pago de clases.)

## 3. Desplegar las Edge Functions
```
supabase functions deploy mp-connect-start
supabase functions deploy mp-connect-callback
supabase functions deploy reserve-room-preference
supabase functions deploy mercadopago-webhook
```

## 4. Configurar el Redirect URI en la app de MercadoPago
En la app de MercadoPago → sección de redirección / Redirect URLs, agregar EXACTO:
```
https://remznaanexwgzeeupctv.supabase.co/functions/v1/mp-connect-callback
```

## 5. (Opcional) Definir la comisión de la plataforma
Por defecto es 0 %. Para cobrar, p. ej., 10 %:
```sql
UPDATE public.app_settings SET value = '10'::jsonb
WHERE key = 'room_reservation_commission_pct';
```

## 6. Publicar el frontend
- [ ] Mergear `victor` → `main` (Vercel despliega el frontend con la sección
      "Conectar MercadoPago" y el pago real de reservas).

---

## Verificación end-to-end (sandbox)
1. Crear cuentas de prueba en MercadoPago (vendedor = sede, comprador = profesor).
2. Como sede: **Configuración → Conectar MercadoPago** → autorizar → vuelve con
   "Cuenta vinculada". Verificar fila en `mp_seller_accounts`.
3. Como profesor: Buscar Salas → elegir bloques → **Pagar con MercadoPago** →
   pagar con la cuenta compradora de prueba.
4. Tras aprobar: los bloques quedan `OCCUPIED`, se crea el borrador/clase, y el
   dinero se reparte (sede recibe el neto, plataforma la comisión).
5. Si NO se paga: el cron `release-expired-holds` devuelve los bloques `HELD` a
   `AVAILABLE` a los ~15 min.
6. Sede sin MercadoPago vinculado → la reserva responde error claro (409).

## Notas
- Los tokens del vendedor expiran; queda pendiente (mejora futura) refrescarlos con
  el `refresh_token` guardado en `mp_seller_accounts`.
- El mismo mecanismo Connect podría reutilizarse para pagar a profesores por clases.
