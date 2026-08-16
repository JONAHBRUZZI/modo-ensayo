import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";
import { getValidSellerToken } from "../_shared/mpAuth.ts";
import { z } from "npm:zod@3";

// R19 (02-Reglas-de-Negocio.md): el profesor que pagó el arriendo o la sede
// dueña de la sala pueden cancelarlo hasta 24h antes del horario reservado,
// con reembolso TOTAL. El cobro se hizo con el access_token de la SEDE
// (split), así que el reembolso también se hace con ese token, no el de la
// plataforma.
//
// Guarda de seguridad (no pedida explícitamente en R19, agregada por
// integridad de datos): se rechaza si la clase ya tiene inscripciones
// ACTIVE — ese caso lo cubre el flujo existente de "clase no realizada"
// (R13/R16.1), que ya sabe reembolsar a los alumnos.
const BodySchema = z.object({
  classId: z.string().uuid(),
});

const VEINTICUATRO_HORAS_MS = 24 * 60 * 60 * 1000;

export default {
  fetch: withSupabase({ auth: "user" }, async (req, ctx) => {
    try {
      const { supabaseAdmin: admin, userClaims } = ctx;
      const userId = userClaims!.id;
      const body = BodySchema.parse(await req.json());

      const { data: cls, error: clsErr } = await admin
        .from("classes")
        .select("id,status,teacher_id,room_id,start_time,payment_session_id")
        .eq("id", body.classId).single();
      if (clsErr || !cls) {
        return Response.json({ error: "Clase no encontrada" }, { status: 404 });
      }
      if (!cls.payment_session_id) {
        return Response.json({
          error: "Esta clase no corresponde a un arriendo de sala pagado",
        }, { status: 404 });
      }
      if (["CANCELLED", "COMPLETED", "SUSPENDED"].includes(cls.status)) {
        return Response.json({ error: `La clase ya está en estado ${cls.status}` }, { status: 409 });
      }

      const { count: inscritos } = await admin
        .from("enrollments")
        .select("*", { count: "exact", head: true })
        .eq("class_id", cls.id).eq("status", "ACTIVE");
      if ((inscritos ?? 0) > 0) {
        return Response.json({
          error: "Esta clase ya tiene alumnos inscritos. Usa el flujo de 'clase no realizada' para reembolsarlos.",
        }, { status: 409 });
      }

      if (Date.now() > new Date(cls.start_time).getTime() - VEINTICUATRO_HORAS_MS) {
        return Response.json({
          error: "Solo se puede cancelar hasta 24 horas antes del horario reservado",
        }, { status: 409 });
      }

      // Autorización: el profesor dueño de la clase, o el admin de la sede
      // dueña de la sala.
      const { data: room } = await admin
        .from("rooms").select("id,venue:venues(admin_id)").eq("id", cls.room_id).single();
      const venueAdminId = (room?.venue as { admin_id: string } | null)?.admin_id ?? null;
      const esProfesor = cls.teacher_id === userId;
      const esSede = venueAdminId === userId;
      if (!esProfesor && !esSede) {
        return Response.json({ error: "No autorizado" }, { status: 403 });
      }

      const { data: session, error: sessErr } = await admin
        .from("payment_sessions")
        .select("id,status,mercado_pago_payment_id,cart_snapshot")
        .eq("id", cls.payment_session_id).single();
      if (sessErr || !session || session.status !== "APPROVED" || !session.mercado_pago_payment_id) {
        return Response.json({ error: "El pago de esta reserva no está en un estado reembolsable" }, { status: 409 });
      }

      const cart = session.cart_snapshot as { sellerUserId?: string; amount?: number };
      if (!cart?.sellerUserId) {
        return Response.json({ error: "No se pudo determinar la cuenta vendedora del arriendo" }, { status: 500 });
      }

      const seller = await getValidSellerToken(admin, cart.sellerUserId);

      // Reembolso TOTAL (sin `amount`) — a diferencia de process-refunds, acá
      // el pago corresponde 1:1 a esta reserva, no a un carrito compartido.
      const mpResp = await fetch(
        `https://api.mercadopago.com/v1/payments/${session.mercado_pago_payment_id}/refunds`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${seller.accessToken}`,
            "Content-Type": "application/json",
            "X-Idempotency-Key": `room_refund:${session.id}`,
          },
        },
      );
      if (!mpResp.ok) {
        const detail = await mpResp.text();
        logError("cancel_room_reservation_mp_refund_failed", new Error(detail), {
          classId: cls.id, sessionId: session.id, status: mpResp.status,
        });
        return Response.json({
          error: "MercadoPago no pudo procesar el reembolso. Intenta de nuevo en unos minutos.",
          detail,
        }, { status: 502 });
      }

      await admin.from("payment_sessions").update({ status: "REFUNDED" }).eq("id", session.id);
      await admin.from("room_schedule_blocks")
        .update({ status: "AVAILABLE", class_id: null, held_until: null, held_by: null })
        .eq("class_id", cls.id);
      await admin.from("classes").update({ status: "CANCELLED" }).eq("id", cls.id);

      await admin.from("audit_logs").insert({
        actor_id: userId,
        action: "room_reservation.cancelled",
        resource_type: "class",
        resource_id: cls.id,
        metadata: { paymentSessionId: session.id, amount: cart.amount ?? null, canceladoPor: esProfesor ? "PROFESOR" : "SEDE" },
      });

      // Notifica a la otra parte.
      const notifyUserId = esProfesor ? venueAdminId : cls.teacher_id;
      if (notifyUserId) {
        await admin.from("notifications").insert({
          user_id: notifyUserId,
          title: "Arriendo de sala cancelado",
          message: esProfesor
            ? "El profesor canceló un arriendo de sala. Se reembolsó el pago y el horario quedó disponible."
            : "La sede canceló tu arriendo de sala. Se reembolsó el pago.",
          type: "ROOM_RESERVATION_CANCELLED",
        });
      }

      logInfo("room_reservation_cancelled", { classId: cls.id, sessionId: session.id, canceladoPor: esProfesor ? "PROFESOR" : "SEDE" });
      return Response.json({ status: "REFUNDED" });

    } catch (err) {
      logError("cancel_room_reservation_error", err);
      if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400 });
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
