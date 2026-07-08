import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

const BodySchema = z.object({
  rescheduleId: z.string().uuid(),
  accepted: z.boolean(),
});

export default {
  fetch: withSupabase({ auth: "user" }, async (req, ctx) => {
    try {
      const { supabaseAdmin: admin, userClaims } = ctx;
      const userId = userClaims!.id;
      const body = BodySchema.parse(await req.json());

      // Verificar que el reagendamiento existe y obtener la clase asociada
      const { data: resched, error: reschedErr } = await admin.from("reschedules")
        .select("class_id").eq("id", body.rescheduleId).single();
      if (reschedErr || !resched) return Response.json({ error: "Reagendamiento no encontrado" }, { status: 404 });

      // Verificar que el alumno está inscrito en esa clase
      const { data: enrollment } = await admin.from("enrollments")
        .select("id").eq("class_id", resched.class_id).eq("student_id", userId).eq("status", "ACTIVE").maybeSingle();
      if (!enrollment) return Response.json({ error: "No estás inscrito en esta clase" }, { status: 403 });

      const { data: resp, error } = await admin.from("reschedule_responses")
        .select("*").eq("reschedule_id", body.rescheduleId).eq("user_id", userId)
        .is("response_type", null).single();
      if (error || !resp) return Response.json({ error: "Respuesta no pendiente" }, { status: 404 });

      const responseType = body.accepted ? "ACCEPTED" : "REJECTED";
      await admin.from("reschedule_responses").update({
        response_type: responseType,
        responded_at: new Date().toISOString(),
      }).eq("id", resp.id);

      if (!body.accepted) {
        const { data: enrollments } = await admin.from("enrollments")
          .select("id").eq("class_id", resched.class_id).eq("student_id", userId).eq("status", "ACTIVE");
        if (enrollments) {
          for (const e of enrollments) {
            await admin.from("payments").update({ status: "REFUND_PENDING" })
              .eq("enrollment_id", e.id).eq("status", "RETAINED");
          }
        }
      }

      const { count } = await admin.from("reschedule_responses")
        .select("*", { count: "exact", head: true })
        .eq("reschedule_id", body.rescheduleId).is("response_type", null);
      if (count === 0) {
        await admin.from("reschedules").update({ status: "COMPLETED" }).eq("id", body.rescheduleId);
      }

      logInfo("student_decision", { rescheduleId: body.rescheduleId, accepted: body.accepted, userId });
      return Response.json({ status: "ok" });

    } catch (err) {
      logError("student_decision_error", err);
      if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400 });
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
