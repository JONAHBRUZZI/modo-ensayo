import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";

// auth: "none" — MercadoPago no envía JWT. Verificamos via HMAC SHA-256.
// Recordar: verify_jwt = false para esta función en supabase/config.toml.
export default {
  fetch: withSupabase({ auth: "none" }, async (req, ctx) => {
    try {
      const body = await req.text();

      const secret = Deno.env.get("MERCADOPAGO_WEBHOOK_SECRET");
      if (secret) {
        const signature = req.headers.get("x-signature");
        const requestId = req.headers.get("x-request-id");
        if (!signature || !requestId) {
          logError("webhook_signature_missing", new Error("Missing signature headers"));
          return new Response("Forbidden", { status: 403 });
        }

        let ts: string | null = null;
        let v1: string | null = null;
        for (const part of signature.split(",")) {
          const [k, val] = part.split("=").map((s) => s.trim());
          if (k === "ts") ts = val;
          if (k === "v1") v1 = val;
        }
        if (!ts || !v1) return new Response("Forbidden", { status: 403 });

        const url = new URL(req.url);
        const dataId = url.searchParams.get("data.id") ?? JSON.parse(body).data?.id ?? "";
        const manifest = `id:${dataId};request-id:${requestId};ts:${ts};`;
        const encoder = new TextEncoder();
        const key = await crypto.subtle.importKey(
          "raw", encoder.encode(secret),
          { name: "HMAC", hash: "SHA-256" }, false, ["sign"]
        );
        const expected = Array.from(
          new Uint8Array(await crypto.subtle.sign("HMAC", key, encoder.encode(manifest)))
        ).map((b) => b.toString(16).padStart(2, "0")).join("");

        if (expected !== v1) {
          logError("webhook_signature_invalid", new Error("HMAC verification failed"));
          return new Response("Forbidden", { status: 403 });
        }
        logInfo("webhook_signature_verified", { requestId });
      }

      const payload = JSON.parse(body);
      const paymentId = payload.data?.id;
      if (!paymentId) return new Response("ok", { status: 200 });

      const mpToken = Deno.env.get("MERCADOPAGO_ACCESS_TOKEN")!;
      const mpResp = await fetch(`https://api.mercadopago.com/v1/payments/${paymentId}`, {
        headers: { Authorization: `Bearer ${mpToken}` },
      });
      const payment = await mpResp.json();
      if (payment.status !== "approved") return new Response("ok", { status: 200 });

      const admin = ctx.supabaseAdmin;

      const { data: session, error: sessErr } = await admin
        .from("payment_sessions").select("*")
        .eq("external_reference", payment.external_reference).single();
      if (sessErr || !session || session.status === "APPROVED") {
        return new Response("ok", { status: 200 }); // idempotente
      }

      const cart = session.cart_snapshot as {
        items: Array<{
          classId: string;
          price: number;
          beneficiaryType?: string;
          beneficiaryId?: string | null;
        }>
      };

      for (const item of cart.items) {
        const { data: cls } = await admin.from("classes")
          .select("id,status,capacity").eq("id", item.classId).single();
        if (!cls || cls.status !== "PUBLISHED") continue;

        const { count } = await admin.from("enrollments")
          .select("*", { count: "exact", head: true })
          .eq("class_id", item.classId).eq("status", "ACTIVE");
        if (count && count >= cls.capacity) continue;

        const { data: enrollment } = await admin.from("enrollments").insert({
          class_id: item.classId,
          student_id: session.owner_id,
          beneficiary_type: item.beneficiaryType || "SELF",
          beneficiary_id: item.beneficiaryId ?? session.owner_id,
          status: "ACTIVE",
        }).select("id").single();

        if (enrollment) {
          await admin.from("payments").insert({
            enrollment_id: enrollment.id,
            amount: item.price,
            status: "RETAINED",
          });
        }
      }

      await admin.from("cart_items").delete().eq("owner_id", session.owner_id);
      await admin.from("payment_sessions").update({
        status: "APPROVED",
        mercado_pago_payment_id: String(paymentId),
        processed_at: new Date().toISOString(),
      }).eq("id", session.id);

      await admin.from("audit_logs").insert({
        actor_id: session.owner_id,
        action: "payment.approved",
        resource_type: "payment_session",
        resource_id: session.id,
        metadata: {
          payment_id: paymentId,
          amount: cart.items.reduce((a, b) => a + b.price, 0),
        },
      });

      logInfo("payment_processed", { sessionId: session.id, paymentId, items: cart.items.length });
      return new Response("ok", { status: 200 });

    } catch (err) {
      logError("webhook_error", err);
      return new Response("ok", { status: 200 }); // Siempre 200 para MercadoPago
    }
  }),
};
