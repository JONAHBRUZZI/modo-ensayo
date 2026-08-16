import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError, logWarn } from "../_shared/logger.ts";
import { FintocPayoutProvider } from "../_shared/fintocPayoutProvider.ts";
import type { PayoutProvider } from "../_shared/payoutProvider.ts";

const payoutProvider: PayoutProvider = new FintocPayoutProvider();

/**
 * Edge Function: process-payouts
 *
 * Procesador batch de liquidaciones a profesores (Marketplace). Invocado por
 * pg_cron + pg_net con service_role key. Selecciona teacher_payouts PENDING,
 * resuelve los datos bancarios del profesor (`refund_methods`) y ejecuta el
 * desembolso de `net_amount` vía `PayoutProvider` (Fintoc), transicionando
 * idempotentemente a PAID.
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
  // 1. Resolver los datos bancarios del profesor (trg_enforce_payout_method
  // ya garantiza que existan si publicó una clase, pero se valida igual).
  const { data: rm, error: rmErr } = await admin
    .from("refund_methods")
    .select("bank, account_type, account_number, account_holder, rut")
    .eq("user_id", payout.teacher_id)
    .limit(1).maybeSingle();
  if (rmErr || !rm) {
    throw new Error(`Profesor ${payout.teacher_id} no tiene datos bancarios cargados`);
  }

  // 2. Ejecutar el desembolso del neto al profesor.
  const { providerReference, status } = await payoutProvider.sendPayout({
    recipient: {
      holderName: rm.account_holder,
      holderRut: rm.rut,
      bank: rm.bank,
      accountType: rm.account_type,
      accountNumber: rm.account_number,
    },
    amount: payout.net_amount,
    externalReference: payout.id,
  });

  if (status === "FAILED") {
    // Estado terminal no exitoso (ej. 'rejected'/'returned' en Fintoc) — no tiene
    // sentido reintentar, se marca FAILED para revisión manual.
    await admin.from("teacher_payouts")
      .update({
        status: "FAILED", mp_reference: providerReference, provider: payoutProvider.name,
        error_detail: `Payout rechazado por ${payoutProvider.name}`,
      })
      .eq("id", payout.id).eq("status", "PENDING");
    logError("payout_failed", new Error(`Payout ${payout.id} FAILED en ${payoutProvider.name}`), { payoutId: payout.id, providerReference });
    return;
  }
  if (status !== "PAID") {
    // El proveedor aceptó la transferencia pero sigue en proceso (ej. 'pending'
    // en Fintoc) — se deja PENDING para que la próxima pasada del cron lo revise.
    // Seguro reintentar: sendPayout reusa el mismo Idempotency-Key por payout.
    throw new Error(`Payout ${payout.id} en estado ${status} del lado de ${payoutProvider.name}, no confirmado todavía`);
  }

  // 3. Cierre idempotente: solo si sigue PENDING.
  const { data: updated, error: updErr } = await admin
    .from("teacher_payouts")
    .update({
      status: "PAID", mp_reference: providerReference, provider: payoutProvider.name,
      paid_at: new Date().toISOString(), error_detail: null,
    })
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
    metadata: { teacher_id: payout.teacher_id, amount: payout.net_amount, reference: providerReference, provider: payoutProvider.name },
  });

  logInfo("payout_paid", { payoutId: payout.id, amount: payout.net_amount, provider: payoutProvider.name });
}
