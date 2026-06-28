import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

// Crea una preferencia de MercadoPago para arrendar una sala, con SPLIT: el cobro
// se hace con el access_token de la SEDE (vendedor) y un marketplace_fee que se
// queda la plataforma. Marca los bloques como HELD (reserva temporal) mientras el
// profesor paga; el webhook materializa la reserva al aprobarse el pago.
const BodySchema = z.object({
  roomId: z.string().uuid(),
  blockIds: z.array(z.string().uuid()).min(1).max(24),
  borradorId: z.string().uuid().nullish(),
});

export default {
  fetch: withSupabase({ auth: "user" }, async (req, ctx) => {
    try {
      const { supabaseAdmin: admin, userClaims } = ctx;
      const userId = userClaims!.id;
      const body = BodySchema.parse(await req.json());

      // 1. La sala y su sede (debe estar aprobada).
      const { data: room, error: roomErr } = await admin
        .from("rooms")
        .select("id,name,price_per_hour,venue_id, venue:venues(id,name,admin_id,status)")
        .eq("id", body.roomId).single();
      if (roomErr || !room || !room.venue || room.venue.status !== "APROBADA") {
        return Response.json({ error: "Sala no disponible" }, { status: 400 });
      }
      const venue = room.venue as { id: string; name: string; admin_id: string; status: string };

      // 2. Los bloques deben existir, ser de esa sala y estar disponibles.
      const { data: blocks, error: blkErr } = await admin
        .from("room_schedule_blocks")
        .select("id,start_time,end_time,status")
        .eq("room_id", body.roomId).in("id", body.blockIds);
      if (blkErr) throw blkErr;
      if (!blocks || blocks.length !== body.blockIds.length ||
          blocks.some((b) => b.status !== "AVAILABLE")) {
        return Response.json({ error: "Algún horario ya no está disponible" }, { status: 409 });
      }

      // 2.1 No se puede reservar un horario que ya pasó. Se rechaza si algún
      // bloque tiene inicio en el pasado (regla de negocio: solo fechas futuras).
      const ahora = Date.now();
      if (blocks.some((b) => new Date(b.start_time).getTime() <= ahora)) {
        return Response.json(
          { error: "No se puede reservar un horario que ya pasó" },
          { status: 409 },
        );
      }

      // 3. La sede debe tener su cuenta de MercadoPago vinculada.
      const { data: seller } = await admin
        .from("mp_seller_accounts")
        .select("access_token,status").eq("user_id", venue.admin_id).maybeSingle();
      if (!seller?.access_token || seller.status !== "CONNECTED") {
        return Response.json({
          error: "Esta sede aún no puede recibir pagos online (MercadoPago no vinculado)",
        }, { status: 409 });
      }

      // 4. Monto y comisión configurable.
      const amount = body.blockIds.length * Number(room.price_per_hour || 0);
      if (amount <= 0) return Response.json({ error: "Precio de sala inválido" }, { status: 400 });
      const { data: cfg } = await admin.from("app_settings")
        .select("value").eq("key", "room_reservation_commission_pct").maybeSingle();
      const pct = Number(cfg?.value ?? 0) || 0;
      const marketplaceFee = Math.round(amount * pct / 100);

      // 5. Hold temporal (15 min). Guard atómico contra carrera.
      const heldUntil = new Date(Date.now() + 15 * 60000).toISOString();
      const { data: held, error: holdErr } = await admin
        .from("room_schedule_blocks")
        .update({ status: "HELD", held_until: heldUntil, held_by: userId })
        .in("id", body.blockIds).eq("status", "AVAILABLE")
        .select("id");
      if (holdErr) throw holdErr;
      if (!held || held.length !== body.blockIds.length) {
        // Alguien tomó un bloque entremedio: revertir lo que alcanzamos a marcar.
        await admin.from("room_schedule_blocks")
          .update({ status: "AVAILABLE", held_until: null, held_by: null })
          .in("id", body.blockIds).eq("held_by", userId).eq("status", "HELD");
        return Response.json({ error: "Algún horario ya no está disponible" }, { status: 409 });
      }

      // 6. Sesión de pago.
      const externalRef = crypto.randomUUID();
      await admin.from("payment_sessions").insert({
        owner_id: userId, external_reference: externalRef, status: "PENDING",
        cart_snapshot: {
          type: "ROOM_RESERVATION",
          roomId: body.roomId, roomName: room.name, venueId: venue.id,
          sellerUserId: venue.admin_id, blockIds: body.blockIds,
          borradorId: body.borradorId ?? null, amount, marketplaceFee,
        },
      });

      // 7. Preferencia MP con el token del VENDEDOR (split).
      const frontendUrl = Deno.env.get("APP_FRONTEND_URL")!;
      const functionsBase = Deno.env.get("FUNCTIONS_URL") ??
        `${Deno.env.get("SUPABASE_URL")}/functions/v1`;
      const isHttps = frontendUrl.startsWith("https://");
      const preferenceBody: Record<string, unknown> = {
        items: [{
          id: body.roomId,
          title: `Arriendo ${room.name} (${body.blockIds.length}h)`,
          description: `Reserva de sala en ${venue.name}`,
          quantity: 1, currency_id: "CLP", unit_price: amount,
        }],
        external_reference: externalRef,
        back_urls: {
          success: `${frontendUrl}/payment/success`,
          failure: `${frontendUrl}/payment/failure`,
          pending: `${frontendUrl}/payment/pending`,
        },
        notification_url: `${functionsBase}/mercadopago-webhook`,
      };
      if (marketplaceFee > 0) preferenceBody.marketplace_fee = marketplaceFee;
      if (isHttps) preferenceBody.auto_return = "approved";

      const mpResp = await fetch("https://api.mercadopago.com/checkout/preferences", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${seller.access_token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(preferenceBody),
      });
      const preference = await mpResp.json();
      if (!mpResp.ok || !preference.init_point) {
        logError("reserve_preference_rejected", new Error(mpResp.statusText), { mpError: preference });
        // Revertir hold y marcar sesión fallida.
        await admin.from("room_schedule_blocks")
          .update({ status: "AVAILABLE", held_until: null, held_by: null })
          .in("id", body.blockIds).eq("held_by", userId);
        await admin.from("payment_sessions").update({ status: "FAILED" })
          .eq("external_reference", externalRef);
        return Response.json({
          error: "MercadoPago rechazó la preferencia",
          detail: preference?.message ?? null,
        }, { status: 502 });
      }

      await admin.from("payment_sessions").update({ preference_id: preference.id })
        .eq("external_reference", externalRef);

      logInfo("room_reservation_preference_created", {
        userId, roomId: body.roomId, blocks: body.blockIds.length, amount, marketplaceFee,
      });
      return Response.json({
        preferenceId: preference.id,
        initPoint: preference.init_point,
        sandboxInitPoint: preference.sandbox_init_point,
      });

    } catch (err) {
      logError("reserve_room_preference_error", err);
      if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400 });
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
