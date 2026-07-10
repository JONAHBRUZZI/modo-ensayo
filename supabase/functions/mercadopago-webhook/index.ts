import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";
import { triggerStudentReschedule } from "../_shared/reschedule.ts";

// deno-lint-ignore no-explicit-any
type Admin = any;

// Materializa una reserva de sala pagada: asigna/crea la clase, ocupa los bloques
// (HELD -> OCCUPIED) y asegura el rol TEACHER del profesor. El split del dinero ya
// lo hizo MercadoPago (el cobro se generó con el token de la sede + marketplace_fee).
async function materializeRoomReservation(admin: Admin, session: { id: string; owner_id: string }, cart: {
  roomId: string; roomName: string; blockIds: string[]; borradorId?: string | null; amount: number;
  rescheduleReason?: string | null;
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

  // La capacidad de la clase la define la sala (su tope).
  const { data: roomRow } = await admin.from("rooms").select("capacity").eq("id", cart.roomId).single();
  const roomCapacity = (roomRow?.capacity as number) ?? 1;

  let classId: string | null = cart.borradorId ?? null;
  let reagendabaDeadline: string | null = null;
  if (classId) {
    // Si el "borrador" es en realidad una clase caída (no realizada) que está en
    // ventana de reagendamiento, esto la republica en el nuevo horario y luego
    // dispara la decisión de los alumnos.
    const { data: prev } = await admin.from("classes")
      .select("reschedule_deadline").eq("id", classId).single();
    reagendabaDeadline = (prev?.reschedule_deadline as string | null) ?? null;

    // Asigna sala/horario al borrador existente y lo publica. La duración la
    // define el horario reservado (bloques), no lo que traía el borrador.
    // Si venía de reagendamiento, se cierra la ventana (reschedule_deadline=null).
    await admin.from("classes").update({
      room_id: cart.roomId, start_time: firstStart, end_time: lastEnd,
      duration: durationMin, capacity: roomCapacity, status: "PUBLISHED",
      reschedule_deadline: null,
    }).eq("id", classId).eq("teacher_id", ownerId);
  } else {
    // Crea un borrador de reserva (el profesor lo completa luego).
    const { data: nueva } = await admin.from("classes").insert({
      title: `Reserva - ${cart.roomName}`,
      level: "BASICO", capacity: roomCapacity, duration: durationMin, price: cart.amount,
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

  // Si esta reserva completó un reagendamiento de una clase caída, notifica a los
  // alumnos para que acepten/rechacen la nueva fecha (reusa student-decision).
  if (classId && reagendabaDeadline) {
    await triggerStudentReschedule(admin, {
      classId,
      teacherId: ownerId,
      newStartTime: firstStart,
      reason: cart.rescheduleReason || "Reagendamiento del profesor",
    });
  }
}

// Consulta un pago en la API de MercadoPago con un access_token dado.
// Devuelve el JSON del pago, o null si el token no puede leerlo (404/401).
async function fetchMpPayment(paymentId: string, token: string) {
  const resp = await fetch(`https://api.mercadopago.com/v1/payments/${paymentId}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!resp.ok) return null;
  return await resp.json();
}

// auth: "none" — MercadoPago no envía JWT. Verificamos via HMAC SHA-256.
// Recordar: verify_jwt = false para esta función en supabase/config.toml.
export default {
  fetch: withSupabase({ auth: "none" }, async (req, ctx) => {
    try {
      const url = new URL(req.url);

      // Solo procesamos notificaciones de pago. merchant_order y otros topics se
      // ignoran con 200 (no hacemos nada con ellos) para no spamear reintentos
      // ni ensuciar los logs con 403 de firma que no aplican.
      const topic = url.searchParams.get("topic") ?? url.searchParams.get("type");
      if (topic && topic !== "payment") return new Response("ok", { status: 200 });

      const body = await req.text();

      const secret = Deno.env.get("MERCADOPAGO_WEBHOOK_SECRET");
      if (!secret) {
        logError("webhook_secret_missing", new Error("MERCADOPAGO_WEBHOOK_SECRET not configured"));
        return new Response("Internal Server Error", { status: 500 });
      }

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

      // El manifest de MP usa data.id en minúsculas (relevante para ids alfanuméricos).
      const dataId = (url.searchParams.get("data.id") ?? JSON.parse(body).data?.id ?? "").toString().toLowerCase();
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

      const payload = JSON.parse(body);
      const paymentId = payload.data?.id ?? url.searchParams.get("data.id");
      if (!paymentId) return new Response("ok", { status: 200 });

      const admin = ctx.supabaseAdmin;

      // El pago de una inscripción a clase vive en la cuenta de la plataforma,
      // pero el de una reserva de sala vive en la cuenta del VENDEDOR (la sede,
      // que creó la preferencia con su propio token). Probamos primero el token
      // de la plataforma y, si no puede leer el pago, los de las sedes conectadas.
      const platformToken = Deno.env.get("MERCADOPAGO_ACCESS_TOKEN")!;
      let payment = await fetchMpPayment(paymentId, platformToken);
      if (!payment?.external_reference) {
        const { data: sellers } = await admin
          .from("mp_seller_accounts")
          .select("access_token").eq("status", "CONNECTED");
        for (const s of sellers ?? []) {
          if (!s.access_token) continue;
          const sellerPayment = await fetchMpPayment(paymentId, s.access_token);
          if (sellerPayment?.external_reference) { payment = sellerPayment; break; }
        }
      }
      if (!payment || payment.status !== "approved") return new Response("ok", { status: 200 });

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
        // agregadosPorClase cuenta las inscripciones ya creadas a cada clase EN ESTE
        // carrito: el count de la BD no las ve hasta confirmarse cada insert, así que
        // sin esto un carrito con varias inscripciones a la misma clase podría pasar
        // el cupo (todas leen el mismo count inicial).
        const agregadosPorClase: Record<string, number> = {};
        for (const item of cart.items) {
          const { data: cls } = await admin.from("classes")
            .select("id,status,capacity").eq("id", item.classId).single();
          if (!cls || cls.status !== "PUBLISHED") continue;

          const { count } = await admin.from("enrollments")
            .select("*", { count: "exact", head: true })
            .eq("class_id", item.classId).eq("status", "ACTIVE");
          const yaAgregados = agregadosPorClase[item.classId] ?? 0;
          if ((count ?? 0) + yaAgregados >= cls.capacity) continue;

          const { data: enrollment } = await admin.from("enrollments").insert({
            class_id: item.classId,
            student_id: session.owner_id,
            beneficiary_type: item.beneficiaryType || "SELF",
            beneficiary_id: item.beneficiaryId ?? session.owner_id,
            status: "ACTIVE",
          }).select("id").single();

          if (enrollment) {
            agregadosPorClase[item.classId] = yaAgregados + 1;
            await admin.from("payments").insert({
              enrollment_id: enrollment.id,
              amount: item.price,
              status: "RETAINED",
            });
          }
        }
        await admin.from("cart_items").delete().eq("owner_id", session.owner_id);
      }

      // Costo real que cobró MercadoPago y neto recibido (para el margen del
      // admin). fee_details suma todas las comisiones del pago; net_received_amount
      // es lo que efectivamente llegó a la cuenta. Pagos viejos no traen fee.
      const mpFeeAmount = Array.isArray(payment.fee_details)
        ? payment.fee_details.reduce(
          (acc: number, f: { amount?: number }) => acc + Number(f.amount ?? 0), 0)
        : null;
      const netReceivedAmount = payment.transaction_details?.net_received_amount ?? null;

      await admin.from("payment_sessions").update({
        status: "APPROVED",
        mercado_pago_payment_id: String(paymentId),
        processed_at: new Date().toISOString(),
        mp_fee_amount: mpFeeAmount,
        net_received_amount: netReceivedAmount,
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
