-- ============================================================
-- Cron: procesadores batch de reembolsos y liquidaciones (Marketplace)
-- Agenda process-refunds y process-payouts vía pg_cron + pg_net (http_post a
-- las Edge Functions con la service_role key).
--
-- PRERREQUISITOS antes de aplicar esta migración:
--   1. Edge Functions `process-refunds` y `process-payouts` desplegadas.
--   2. Secret en Vault con la service_role key:
--        select vault.create_secret('<SERVICE_ROLE_KEY>', 'service_role_key');
-- (Sin el secret, los jobs corren pero fallan al construir el header Authorization.)
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pg_net WITH SCHEMA extensions;

-- Reembolsos: cada 10 minutos
SELECT cron.schedule(
  'process-refunds',
  '*/10 * * * *',
  $$
    SELECT net.http_post(
      url := 'https://remznaanexwgzeeupctv.supabase.co/functions/v1/process-refunds',
      headers := jsonb_build_object(
        'Content-Type', 'application/json',
        'Authorization', 'Bearer ' || (SELECT decrypted_secret FROM vault.decrypted_secrets WHERE name = 'service_role_key')
      ),
      body := '{}'::jsonb,
      timeout_milliseconds := 30000
    );
  $$
);

-- Liquidaciones a profesores: cada 15 minutos
SELECT cron.schedule(
  'process-payouts',
  '*/15 * * * *',
  $$
    SELECT net.http_post(
      url := 'https://remznaanexwgzeeupctv.supabase.co/functions/v1/process-payouts',
      headers := jsonb_build_object(
        'Content-Type', 'application/json',
        'Authorization', 'Bearer ' || (SELECT decrypted_secret FROM vault.decrypted_secrets WHERE name = 'service_role_key')
      ),
      body := '{}'::jsonb,
      timeout_milliseconds := 30000
    );
  $$
);
