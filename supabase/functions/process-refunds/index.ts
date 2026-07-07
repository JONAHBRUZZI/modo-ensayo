import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError, logWarn } from "../_shared/logger.ts";

/**
 * Edge Function: process-refunds
 *
 * Procesador batch de reembolsos. Invocado por pg_cron + pg_net con service_role key.
 * Selecciona todos los pagos en REFUND_PENDING, resuelve el canal de devolución
 * (bancario o API MercadoPago) y transiciona idempotentemente a REFUNDED.
 *
 * Idempotencia: el UPDATE final condiciona status = 'REFUND_PENDING';
 * una segunda pasada sobre el mismo pago afecta 0 filas.
 */
export default {
  fetch: withSupabase({ auth: "service_role" }, async (_req, ctx) => {
    const admin = ctx.supabaseAdmin;
    const results: { processed: number; failed: number; errors: string[] } = {
      processed: 0,
      failed: 0,
      errors: [],
    };

    try {
      // 1. Selección idempotente: pagos pendientes de reembolso
      const { data: pendingPayments, error: fetchErr } = await admin
        .from("payments")
        .select("id, enrollment_id, amount")
        .eq("status", "REFUND_PENDING");

      if (fetchErr) {
        logError("process_refunds_fetch_error", fetchErr);
        return Response.json({ error: "Error fetching pending payments" }, { status: 500 });
      }

      if (!pendingPayments || pendingPayments.length === 0) {
        logInfo("process_refunds_noop", { message: "No payments in REFUND_PENDING" });
        return Response.json({ processed: 0, failed: 0 });
      }

      logInfo("process_refunds_start", { count: pendingPayments.length });

      // 2. Procesar cada pago individualmente
      for (const payment of pendingPayments) {
        try {
          await processPaymentRefund(admin, payment);
          results.processed++;
        } catch (err) {
          results.failed++;
          const errorMsg = err instanceof Error ? err.message : String(err);
          results.errors.push(`payment ${payment.id}: ${errorMsg}`);
          logError("process_refund_payment_error", err, { paymentId: payment.id });
          // No avanzar a REFUNDED — queda en REFUND_PENDING para reintento
        }
      }

      logInfo("process_refunds_complete", {
        processed: results.processed,
        failed: results.failed,
      });

      return Response.json({
        processed: results.processed,
        failed: results.failed,
      });

    } catch (err) {
      logError("process_refunds_error", err);
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};

// ---------- Helpers ----------

interface PaymentRow {
  id: string;
  enrollment_id: string;
  amount: number;
}

async function processPaymentRefund(admin: any, payment: PaymentRow): Promise<void> {
  // 2a. Resolver alumno vía enrollment
  const { data: enrollment, error: enrollErr } = await admin
    .from("enrollments")
    .select("student_id, class_id")
    .eq("id", payment.enrollment_id)
    .single();

  if (enrollErr || !enrollment) {
    throw new Error(`Enrollment not found for payment ${payment.id}`);
  }

  const studentId: string = enrollment.student_id;
  const classId: string = enrollment.class_id;

  // 2b. Siempre intentar reembolso por MercadoPago (el pago original fue por MP)
  const mpPaymentId = await resolveMercadoPagoPaymentId(admin, studentId, classId);

  if (!mpPaymentId) {
    throw new Error(
      `Cannot resolve MercadoPago payment ID for student ${studentId}, class ${classId}`
    );
  }

  const mpToken = Deno.env.get("MERCADOPAGO_ACCESS_TOKEN");
  if (!mpToken) {
    throw new Error("MERCADOPAGO_ACCESS_TOKEN not configured");
  }

  // Reembolso PARCIAL por el monto exacto de esta inscripción (payment.amount =
  // lo que el alumno pagó por esta clase). Un mismo pago de MP puede cubrir varias
  // inscripciones del carrito; sin el monto, MP haría un reembolso TOTAL del pago.
  // La X-Idempotency-Key (estable por payment.id) evita doble reembolso si el cron
  // reintenta la misma fila.
  const mpResp = await fetch(
    `https://api.mercadopago.com/v1/payments/${mpPaymentId}/refunds`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${mpToken}`,
        "Content-Type": "application/json",
        "X-Idempotency-Key": `refund:${payment.id}`,
      },
      body: JSON.stringify({ amount: Number(payment.amount) }),
    }
  );

  if (!mpResp.ok) {
    const errorBody = await mpResp.text();
    // 5xx / 429: error transitorio de MP → se reintenta en la próxima pasada del cron.
    if (mpResp.status >= 500 || mpResp.status === 429) {
      throw new Error(`MercadoPago refund failed (${mpResp.status}): ${errorBody}`);
    }
    // 4xx: error permanente (pago no reembolsable, ya reembolsado, etc.). No tiene
    // sentido reintentar indefinidamente: se marca FAILED para atención manual.
    await marcarRefundFallido(admin, payment, studentId, mpPaymentId, mpResp.status, errorBody);
    return;
  }

  const mpRefund = await mpResp.json();
  const refundReference = `mp_refund:${mpRefund.id ?? mpPaymentId}`;
  logInfo("refund_mercadopago_success", {
    paymentId: payment.id,
    mpPaymentId,
    mpRefundId: mpRefund.id,
  });

  // 3. Cierre idempotente: solo si sigue en REFUND_PENDING
  const { data: updated, error: updateErr } = await admin
    .from("payments")
    .update({ status: "REFUNDED" })
    .eq("id", payment.id)
    .eq("status", "REFUND_PENDING")
    .select("id");

  if (updateErr) {
    throw new Error(`Failed to update payment ${payment.id}: ${updateErr.message}`);
  }

  // Si 0 filas afectadas, el pago ya fue procesado (idempotencia)
  if (!updated || updated.length === 0) {
    logWarn("refund_already_processed", "Payment was already transitioned", {
      paymentId: payment.id,
    });
    return;
  }

  // 4. Auditoría
  await admin.from("audit_logs").insert({
    actor_id: null, // Acción de sistema (pg_cron)
    action: "payment.refunded",
    resource_type: "payment",
    resource_id: payment.id,
    metadata: {
      channel: "mercadopago",
      reference: refundReference,
      amount: payment.amount,
      student_id: studentId,
    },
  });
}

/**
 * Marca un pago como FAILED (reembolso permanentemente fallido) para atención
 * manual, sin reintentos. Idempotente: solo actúa si sigue en REFUND_PENDING.
 */
async function marcarRefundFallido(
  admin: any,
  payment: PaymentRow,
  studentId: string,
  mpPaymentId: string,
  mpStatus: number,
  errorBody: string,
): Promise<void> {
  const { data: updated } = await admin
    .from("payments")
    .update({ status: "FAILED" })
    .eq("id", payment.id)
    .eq("status", "REFUND_PENDING")
    .select("id");

  if (!updated || updated.length === 0) return; // ya procesado por otra pasada

  await admin.from("audit_logs").insert({
    actor_id: null,
    action: "payment.refund_failed",
    resource_type: "payment",
    resource_id: payment.id,
    metadata: {
      student_id: studentId,
      mp_payment_id: mpPaymentId,
      mp_status: mpStatus,
      error: errorBody?.slice(0, 500),
      amount: payment.amount,
    },
  });

  logWarn(
    "refund_failed_permanent",
    "MercadoPago rechazó el reembolso; requiere atención manual",
    { paymentId: payment.id, mpStatus },
  );
}

/**
 * Resuelve el mercado_pago_payment_id a partir de payment_sessions.
 * Busca sesiones del alumno cuyo cart_snapshot contenga la clase del pago.
 */
async function resolveMercadoPagoPaymentId(
  admin: any,
  studentId: string,
  classId: string
): Promise<string | null> {
  // Buscar payment_sessions del alumno que estén aprobadas
  const { data: sessions, error } = await admin
    .from("payment_sessions")
    .select("id, mercado_pago_payment_id, cart_snapshot")
    .eq("owner_id", studentId)
    .eq("status", "APPROVED")
    .not("mercado_pago_payment_id", "is", null)
    .order("created_at", { ascending: false });

  if (error || !sessions || sessions.length === 0) return null;

  // Buscar la sesión cuyo cart_snapshot contenga un item con la classId
  for (const session of sessions) {
    const cart = session.cart_snapshot as {
      items?: Array<{ classId: string }>;
    };
    const items = cart?.items ?? [];
    if (items.some((item: { classId: string }) => item.classId === classId)) {
      return session.mercado_pago_payment_id;
    }
  }

  return null;
}
