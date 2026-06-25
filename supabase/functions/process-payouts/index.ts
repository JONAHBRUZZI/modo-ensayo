import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError, logWarn } from "../_shared/logger.ts";
import { getValidSellerToken } from "../_shared/mpAuth.ts";

/**
 * Edge Function: process-payouts
 *
 * Procesador batch de liquidaciones a profesores (Marketplace). Invocado por
 * pg_cron + pg_net con service_role key. Selecciona teacher_payouts PENDING,
 * resuelve la cuenta MP del profesor y ejecuta el desembolso de `net_amount`,
 * transicionando idempotentemente a PAID.
 *
 * Idempotencia: el UPDATE final condiciona status = 'PENDING'; una segunda
 * pasada sobre el mismo payout afecta 0 filas. Si el desembolso falla, el payout
 * queda PENDING (con error_detail) para reintento en la siguiente pasada.
 */
export default {
  fetch: withSupabase({ auth: "service_role" }, async (_req, ctx) => {
    const admin = ctx.supabaseAdmin;
    const results = { processed: 0, failed: 0 };

    try {
      const { data: pending, error: fetchErr } = await admin
        .from("teacher_payouts")
        .select("id, payment_id, teacher_id, class_id, net_amount")
        .eq("status", "PENDING");

      if (fetchErr) {
        logError("process_payouts_fetch_error", fetchErr);
        return Response.json({ error: "Error fetching pending payouts" }, { status: 500 });
      }
      if (!pending || pending.length === 0) {
        logInfo("process_payouts_noop", { message: "No payouts PENDING" });
        return Response.json({ processed: 0, failed: 0 });
      }

      logInfo("process_payouts_start", { count: pending.length });

      for (const payout of pending) {
        try {
          await processOnePayout(admin, payout);
          results.processed++;
        } catch (err) {
          results.failed++;
          const msg = err instanceof Error ? err.message : String(err);
          // Dejar PENDING para reintento; registrar el motivo.
          await admin.from("teacher_payouts")
            .update({ error_detail: msg })
            .eq("id", payout.id).eq("status", "PENDING");
          logError("process_payout_error", err, { payoutId: payout.id });
        }
      }

      logInfo("process_payouts_complete", results);
      return Response.json(results);

    } catch (err) {
      logError("process_payouts_error", err);
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};

// ---------- Helpers ----------

interface PayoutRow {
  id: string;
  payment_id: string;
  teacher_id: string;
  class_id: string | null;
  net_amount: number;
}

async function processOnePayout(admin: any, payout: PayoutRow): Promise<void> {
  // 1. Resolver token + cuenta MP del profesor (refresca si está vencido).
  const { accessToken, mpUserId } = await getValidSellerToken(admin, payout.teacher_id);

  // 2. Ejecutar el desembolso del neto al profesor.
  const reference = await disburseToSeller({
    accessToken,
    mpUserId,
    amount: payout.net_amount,
    externalReference: payout.id,
  });

  // 3. Cierre idempotente: solo si sigue PENDING.
  const { data: updated, error: updErr } = await admin
    .from("teacher_payouts")
    .update({ status: "PAID", mp_reference: reference, paid_at: new Date().toISOString(), error_detail: null })
    .eq("id", payout.id)
    .eq("status", "PENDING")
    .select("id");

  if (updErr) throw new Error(`Failed to update payout ${payout.id}: ${updErr.message}`);
  if (!updated || updated.length === 0) {
    logWarn("payout_already_processed", "Payout was already transitioned", { payoutId: payout.id });
    return;
  }

  // 4. Auditoría
  await admin.from("audit_logs").insert({
    actor_id: null, // acción de sistema (pg_cron)
    action: "payout.paid",
    resource_type: "teacher_payout",
    resource_id: payout.id,
    metadata: { teacher_id: payout.teacher_id, amount: payout.net_amount, reference },
  });

  logInfo("payout_paid", { payoutId: payout.id, mpUserId, amount: payout.net_amount });
}

/**
 * Ejecuta la transferencia del neto a la cuenta MercadoPago del profesor.
 *
 * ⚠️ PENDIENTE FASE 0 (spike): el mecanismo de desembolso depende de lo que
 * habilite la cuenta MP de la plataforma en Chile. Dos opciones:
 *   (ii) Money-out / transferencia entre cuentas MP (este punto).
 *   (i)  Split nativo con `release` del pago retenido (requiere cambiar también
 *        create-preference para crear el split con el token del profesor).
 *
 * Hasta confirmar el endpoint correcto, lanza para que el payout quede PENDING
 * (seguro y reintentable) en vez de marcar PAID sin mover dinero real.
 * Cuando se confirme el mecanismo, implementar aquí la llamada a la API de MP
 * usando `accessToken`/`mpUserId` y devolver la referencia de la transferencia.
 */
async function disburseToSeller(_args: {
  accessToken: string;
  mpUserId: string;
  amount: number;
  externalReference: string;
}): Promise<string> {
  const mode = Deno.env.get("MP_PAYOUT_MODE"); // "live" cuando el mecanismo esté confirmado
  if (mode !== "live") {
    throw new Error(
      "Desembolso no configurado: definir el mecanismo MP (Fase 0) y MP_PAYOUT_MODE=live",
    );
  }
  // TODO(Fase 0): implementar la llamada real de desembolso/transfer/release MP.
  throw new Error("disburseToSeller live no implementado: pendiente confirmación del endpoint MP");
}
