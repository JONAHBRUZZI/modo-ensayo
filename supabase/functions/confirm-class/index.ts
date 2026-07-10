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
        .select("id, status, teacher_id, tipo_clase, honorario, room:rooms(venue:venues(admin_id))")
        .eq("id", body.classId).single();
      if (!cls) return Response.json({ error: "Clase no encontrada" }, { status: 404 });

      const venueAdminId = (cls as any).room?.venue?.admin_id;
      if (venueAdminId !== userId && !roles.includes("ADMIN")) {
        return Response.json({ error: "No autorizado para esta sede" }, { status: 403 });
      }

      // Fetch enrollments with student_id for both branches
      const { data: enrollments } = await admin.from("enrollments")
        .select("id, student_id").eq("class_id", body.classId).eq("status", "ACTIVE");

      let payoutsCreated = 0;
      let rescheduleDeadline: string | null = null;

      if (body.realized) {
        const { data: setting } = await admin.from("app_settings")
          .select("value").eq("key", "marketplace_commission_pct").single();
        const commissionPct = Number(setting?.value ?? 10);
        const teacherId = (cls as any).teacher_id;
        const tipoClase = (cls as any).tipo_clase;
        const honorario = Number((cls as any).honorario ?? 0);
        const esAsignada = tipoClase === "ASIGNADA";

        // Liberar todos los pagos retenidos de la clase (RETAINED → RELEASED).
        const releasedPayments: { id: string; amount: number }[] = [];
        if (enrollments) {
          for (const e of enrollments) {
            const { data: released } = await admin.from("payments")
              .update({ status: "RELEASED" })
              .eq("enrollment_id", e.id).eq("status", "RETAINED")
              .select("id, amount");
            for (const p of released ?? []) releasedPayments.push({ id: p.id, amount: Number(p.amount) });
          }
        }

        if (esAsignada && honorario > 0) {
          const first = releasedPayments[0];
          if (first) {
            const grossTotal = releasedPayments.reduce((s, p) => s + p.amount, 0);
            const { error: poErr } = await admin.from("teacher_payouts").upsert({
              payment_id: first.id,
              teacher_id: teacherId,
              class_id: body.classId,
              gross_amount: grossTotal,
              commission_amount: 0,
              net_amount: honorario,
              status: "PENDING",
            }, { onConflict: "payment_id", ignoreDuplicates: true });
            if (!poErr) payoutsCreated++;
          }
        } else {
          for (const p of releasedPayments) {
            const commission = Math.round(p.amount * commissionPct) / 100;
            const { error: poErr } = await admin.from("teacher_payouts").upsert({
              payment_id: p.id,
              teacher_id: teacherId,
              class_id: body.classId,
              gross_amount: p.amount,
              commission_amount: commission,
              net_amount: p.amount - commission,
              status: "PENDING",
            }, { onConflict: "payment_id", ignoreDuplicates: true });
            if (!poErr) payoutsCreated++;
          }
        }

        // Liberar bloques OCCUPIED (la clase ya pasó, el horario queda disponible)
        await admin.from("room_schedule_blocks")
          .update({ status: "AVAILABLE", class_id: null })
          .eq("class_id", body.classId).eq("status", "OCCUPIED");
      } else {
        // No realizada: NO se reembolsa de inmediato. Se abre una ventana de 24h para
        // reagendar; los pagos quedan RETAINED y las inscripciones ACTIVE. Si nadie
        // reagenda en el plazo, el cron process_class_reschedule_timeouts hace el refund.
        rescheduleDeadline = new Date(Date.now() + 24 * 3600 * 1000).toISOString();

        // Libera los bloques del horario viejo (la clase no ocurrió ahí).
        await admin.from("room_schedule_blocks")
          .update({ status: "AVAILABLE", class_id: null })
          .eq("class_id", body.classId).eq("status", "OCCUPIED");

        // Notifica al responsable de reagendar según el tipo de clase.
        const tipoClase = (cls as any).tipo_clase;
        if (tipoClase !== "ASIGNADA") {
          // PROPIA: el profesor independiente decide si reagenda.
          await admin.from("notifications").insert({
            user_id: (cls as any).teacher_id,
            title: "Clase marcada como no realizada",
            message: "Tienes 24 horas para reagendarla; si no, se reembolsará a los alumnos.",
            type: "CLASS_RESCHEDULE_OFFERED",
          });
        }
        // ASIGNADA: la sede (que la marcó) gestiona el reagendamiento desde su UI.
      }

      await admin.from("classes").update({
        status: body.realized ? "COMPLETED" : "SUSPENDED",
        reschedule_deadline: rescheduleDeadline,
      }).eq("id", body.classId);

      await admin.from("audit_logs").insert({
        actor_id: userId,
        action: body.realized ? "class.confirmed_realized" : "class.confirmed_not_realized",
        resource_type: "class",
        resource_id: body.classId,
        ...(body.realized
          ? { metadata: { payouts_created: payoutsCreated } }
          : { metadata: { reschedule_deadline: rescheduleDeadline } }),
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
