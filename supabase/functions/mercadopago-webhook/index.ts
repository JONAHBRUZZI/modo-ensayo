import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";

// deno-lint-ignore no-explicit-any
type Admin = any;

// Materializa una reserva de sala pagada: asigna/crea la clase, ocupa los bloques
// (HELD -> OCCUPIED) y asegura el rol TEACHER del profesor. El split del dinero ya
// lo hizo MercadoPago (el cobro se generó con el token de la sede + marketplace_fee).
async function materializeRoomReservation(admin: Admin, session: { id: string; owner_id: string }, cart: {
  roomId: string; roomName: string; blockIds: string[]; borradorId?: string | null; amount: number;
}) {
  const blockIds: string[] = cart.blockIds || [];
  const ownerId = session.owner_id;
  if (blockIds.length === 0) return;

  const { data: blocks } = await admin
    .from("room_schedule_blocks")
    .select("id,start_time,end_time")
    .in("id", blockIds).order("start_time", { ascending: true });
  if (!blocks || blocks.length === 0) return;

  const firstStart = blocks[0].start_time;
  const lastEnd = blocks[blocks.length - 1].end_time;
  const durationMin = blockIds.length * 60;

  let classId: string | null = cart.borradorId ?? null;
  if (classId) {
    // Asigna sala/horario al borrador existente y lo publica.
    await admin.from("classes").update({
      room_id: cart.roomId, start_time: firstStart, end_time: lastEnd, status: "PUBLISHED",
    }).eq("id", classId).eq("teacher_id", ownerId);
  } else {
    // Crea un borrador de reserva (el profesor lo completa luego).
    const { data: nueva } = await admin.from("classes").insert({
      title: `Reserva - ${cart.roomName}`,
      level: "BASICO", capacity: 1, duration: durationMin, price: cart.amount,
      start_time: firstStart, end_time: lastEnd,
      room_id: cart.roomId, teacher_id: ownerId,
      status: "DRAFT", tipo_clase: "PROPIA",
    }).select("id").single();
    classId = nueva?.id ?? null;
  }

  // Ocupa los bloques reservados y limpia el hold temporal.
  await admin.from("room_schedule_blocks")
    .update({ status: "OCCUPIED", class_id: classId, held_until: null, held_by: null })
    .in("id", blockIds);

  // Asegura el rol TEACHER del profesor (no bloquear la reserva si falla).
  try {
    const { data: u } = await admin.auth.admin.getUserById(ownerId);
    const roles: string[] = (u?.user?.app_metadata?.roles as string[]) ?? [];
    if (!roles.includes("TEACHER")) {
      await admin.auth.admin.updateUserById(ownerId, { app_metadata: { roles: [...roles, "TEACHER"] } });
    }
  } catch (_e) { /* best-effort */ }
}

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

      const admin = ctx.supabaseAdmin;

      // En pagos marketplace (arriendo de sala) la preferencia se crea con el
      // token del VENDEDOR, por lo que el pago vive en SU cuenta y el token de la
      // plataforma no puede consultarlo. La notification_url incluye ?seller=<id>
      // para saber con qué token consultar el pago.
      let mpToken = Deno.env.get("MERCADOPAGO_ACCESS_TOKEN")!;
      const sellerId = new URL(req.url).searchParams.get("seller");
      if (sellerId) {
        const { data: sellerAcc } = await admin
          .from("mp_seller_accounts").select("access_token")
          .eq("user_id", sellerId).maybeSingle();
        if (sellerAcc?.access_token) mpToken = sellerAcc.access_token;
      }

      const mpResp = await fetch(`https://api.mercadopago.com/v1/payments/${paymentId}`, {
        headers: { Authorization: `Bearer ${mpToken}` },
      });
      const payment = await mpResp.json();
      if (payment.status !== "approved") return new Response("ok", { status: 200 });

      const { data: session, error: sessErr } = await admin
        .from("payment_sessions").select("*")
        .eq("external_reference", payment.external_reference).single();
      if (sessErr || !session || session.status === "APPROVED") {
        return new Response("ok", { status: 200 }); // idempotente
      }

      // deno-lint-ignore no-explicit-any
      const cart = session.cart_snapshot as any;

      if (cart?.type === "ROOM_RESERVATION") {
        // Arriendo de sala: materializa la reserva (clase + bloques OCCUPIED).
        await materializeRoomReservation(admin, session, cart);
      } else {
        // Inscripción a clases (flujo original): crea enrollments + payments.
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
      }

      await admin.from("payment_sessions").update({
        status: "APPROVED",
        mercado_pago_payment_id: String(paymentId),
        processed_at: new Date().toISOString(),
      }).eq("id", session.id);

      await admin.from("audit_logs").insert({
        actor_id: session.owner_id,
        action: cart?.type === "ROOM_RESERVATION" ? "room_reservation.paid" : "payment.approved",
        resource_type: "payment_session",
        resource_id: session.id,
        metadata: {
          payment_id: paymentId,
          amount: cart?.type === "ROOM_RESERVATION"
            ? cart.amount
            : cart.items.reduce((a: number, b: { price: number }) => a + b.price, 0),
          ...(cart?.type === "ROOM_RESERVATION" ? { marketplace_fee: cart.marketplaceFee } : {}),
        },
      });

      logInfo("payment_processed", {
        sessionId: session.id, paymentId,
        type: cart?.type ?? "CLASS_ENROLLMENT",
        ...(cart?.type === "ROOM_RESERVATION"
          ? { blocks: cart.blockIds?.length ?? 0 }
          : { items: cart.items?.length ?? 0 }),
      });
      return new Response("ok", { status: 200 });

    } catch (err) {
      logError("webhook_error", err);
      return new Response("ok", { status: 200 }); // Siempre 200 para MercadoPago
    }
  }),
};
