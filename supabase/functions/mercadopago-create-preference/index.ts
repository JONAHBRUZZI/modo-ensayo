import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

const BodySchema = z.object({
  items: z.array(z.object({
    classId: z.string().uuid(),
    classTitle: z.string().min(1),
    discipline: z.string(),
    level: z.string(),
    price: z.number().positive(),
    beneficiaryType: z.string().optional(),
    beneficiaryId: z.string().uuid().nullish(),
  })),
});

export default {
  fetch: withSupabase({ auth: "user" }, async (req, ctx) => {
    try {
      const { supabase, userClaims } = ctx;
      const userId = userClaims!.id;
      const body = BodySchema.parse(await req.json());
      const externalRef = crypto.randomUUID();
      // FUNCTIONS_URL no es auto-provisto; derivar del SUPABASE_URL (sí lo es).
      const functionsBase = Deno.env.get("FUNCTIONS_URL") ??
        `${Deno.env.get("SUPABASE_URL")}/functions/v1`;
      const mpToken = Deno.env.get("MERCADOPAGO_ACCESS_TOKEN")!;
      const frontendUrl = Deno.env.get("APP_FRONTEND_URL")!;

      for (const item of body.items) {
        const { data: cls, error: clsErr } = await supabase
          .from("classes").select("id,status,capacity").eq("id", item.classId).single();
        if (clsErr || !cls || cls.status !== "PUBLISHED") {
          return Response.json({ error: `Clase ${item.classId} no disponible` }, { status: 400 });
        }
        const { count } = await supabase
          .from("enrollments").select("*", { count: "exact", head: true })
          .eq("class_id", item.classId).eq("status", "ACTIVE");
        if (count && count >= cls.capacity) {
          return Response.json({ error: `Sin cupos: ${item.classTitle}` }, { status: 400 });
        }
      }

      await supabase.from("payment_sessions").insert({
        owner_id: userId, external_reference: externalRef,
        cart_snapshot: body, status: "PENDING",
      });

      // MercadoPago solo acepta auto_return con back_urls https. En local (http)
      // se omite para no ser rechazado; el usuario vuelve manualmente y el webhook
      // (URL pública de Supabase) crea la inscripción igual.
      const isHttps = frontendUrl.startsWith("https://");
      const preferenceBody: Record<string, unknown> = {
        items: body.items.map((i) => ({
          id: i.classId, title: i.classTitle,
          description: `${i.discipline} - ${i.level}`, quantity: 1,
          currency_id: "CLP", unit_price: i.price,
        })),
        external_reference: externalRef,
        back_urls: {
          success: `${frontendUrl}/payment/success`,
          failure: `${frontendUrl}/payment/failure`,
          pending: `${frontendUrl}/payment/pending`,
        },
        notification_url: `${functionsBase}/mercadopago-webhook`,
      };
      if (isHttps) preferenceBody.auto_return = "approved";

      const mpResp = await fetch("https://api.mercadopago.com/checkout/preferences", {
        method: "POST",
        headers: { Authorization: `Bearer ${mpToken}`, "Content-Type": "application/json" },
        body: JSON.stringify(preferenceBody),
      });
      const preference = await mpResp.json();
      if (!mpResp.ok || !preference.init_point) {
        logError("mp_preference_rejected", new Error(mpResp.statusText), {
          status: mpResp.status, mpError: preference,
        });
        await supabase.from("payment_sessions").update({ status: "FAILED" })
          .eq("external_reference", externalRef);
        return Response.json({
          error: "MercadoPago rechazó la preferencia",
          detail: preference?.message ?? preference?.error ?? null,
          cause: preference?.cause ?? null,
        }, { status: 502 });
      }

      await supabase.from("payment_sessions").update({ preference_id: preference.id })
        .eq("external_reference", externalRef);

      logInfo("preference_created", { userId, preferenceId: preference.id, items: body.items.length });
      return Response.json({
        preferenceId: preference.id,
        initPoint: preference.init_point,
        sandboxInitPoint: preference.sandbox_init_point,
      });

    } catch (err) {
      logError("create_preference_error", err);
      if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400 });
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
