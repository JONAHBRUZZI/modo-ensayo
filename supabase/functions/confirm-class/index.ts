import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

const BodySchema = z.object({
  classId: z.string().uuid(),
  realized: z.boolean(),
});

export default {
  fetch: withSupabase({ auth: "user" }, async (req, ctx) => {
    try {
      const { supabaseAdmin: admin, userClaims } = ctx;
      const userId = userClaims!.id;
      const roles: string[] = (userClaims!.appMetadata?.roles as string[]) ?? [];
      if (!roles.includes("VENUE_ADMIN") && !roles.includes("ADMIN")) {
        return Response.json({ error: "Forbidden" }, { status: 403 });
      }

      const body = BodySchema.parse(await req.json());

      const { data: cls } = await admin.from("classes")
        .select("id, status, teacher_id, room:rooms(venue:venues(admin_id))").eq("id", body.classId).single();
      if (!cls) return Response.json({ error: "Clase no encontrada" }, { status: 404 });

      const venueAdminId = (cls as any).room?.venue?.admin_id;
      if (venueAdminId !== userId && !roles.includes("ADMIN")) {
        return Response.json({ error: "No autorizado para esta sede" }, { status: 403 });
      }

      // Fetch enrollments with student_id for both branches
      const { data: enrollments } = await admin.from("enrollments")
        .select("id, student_id").eq("class_id", body.classId).eq("status", "ACTIVE");

      let refundPendingCount = 0;
      let payoutsCreated = 0;

      if (body.realized) {
        // Pagos RETAINED → RELEASED + crear liquidación al profesor (Marketplace).
        // La comisión de la plataforma se descuenta del bruto; el neto se le paga
        // al profesor vía process-payouts. UNIQUE(payment_id) hace idempotente el payout.
        const { data: setting } = await admin.from("app_settings")
          .select("value").eq("key", "marketplace_commission_pct").single();
        const commissionPct = Number(setting?.value ?? 10);
        const teacherId = (cls as any).teacher_id;

        if (enrollments) {
          for (const e of enrollments) {
            const { data: released } = await admin.from("payments")
              .update({ status: "RELEASED" })
              .eq("enrollment_id", e.id).eq("status", "RETAINED")
              .select("id, amount");

            for (const p of released ?? []) {
              const gross = Number(p.amount);
              const commission = Math.round(gross * commissionPct) / 100;
              const net = gross - commission;
              const { error: poErr } = await admin.from("teacher_payouts").upsert({
                payment_id: p.id,
                teacher_id: teacherId,
                class_id: body.classId,
                gross_amount: gross,
                commission_amount: commission,
                net_amount: net,
                status: "PENDING",
              }, { onConflict: "payment_id", ignoreDuplicates: true });
              if (!poErr) payoutsCreated++;
            }
          }
        }
      } else {
        // Pagos RETAINED → REFUND_PENDING (corrección G-07)
        if (enrollments) {
          for (const e of enrollments) {
            const { data: updated } = await admin.from("payments").update({ status: "REFUND_PENDING" })
              .eq("enrollment_id", e.id).eq("status", "RETAINED").select("id");
            refundPendingCount += updated?.length ?? 0;

            // Notificación al alumno
            await admin.from("notifications").insert({
              user_id: e.student_id,
              title: "Clase suspendida",
              message: "La clase fue suspendida. Se procesará tu reembolso.",
              type: "CLASS_SUSPENDED",
            });
          }
        }
      }

      await admin.from("classes").update({
        status: body.realized ? "COMPLETED" : "SUSPENDED",
      }).eq("id", body.classId);

      await admin.from("audit_logs").insert({
        actor_id: userId,
        action: body.realized ? "class.confirmed_realized" : "class.confirmed_not_realized",
        resource_type: "class",
        resource_id: body.classId,
        ...(body.realized
          ? { metadata: { payouts_created: payoutsCreated } }
          : { metadata: { payments_refund_pending: refundPendingCount } }),
      });

      logInfo(body.realized ? "class_confirmed" : "class_not_realized", { classId: body.classId });
      return Response.json({ status: body.realized ? "COMPLETED" : "SUSPENDED" });

    } catch (err) {
      logError("confirm_class_error", err);
      if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400 });
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
