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
      const roles: string[] = (userClaims!.appMetadata?.roles as string[]) ?? [];
      const body = BodySchema.parse(await req.json());

      const { data: resched, error: rErr } = await admin.from("reschedules")
        .select("*, class:classes(*)").eq("id", body.rescheduleId).eq("status", "PROPOSED").single();
      if (rErr || !resched) {
        return Response.json({ error: "Reagendamiento no encontrado" }, { status: 404 });
      }

      const isTeacher = (resched as any).class?.teacher_id === userId;
      const isVenueAdmin = roles.includes("VENUE_ADMIN");
      if (!isTeacher && !isVenueAdmin && !roles.includes("ADMIN")) {
        return Response.json({ error: "No autorizado" }, { status: 403 });
      }

      if (body.accepted) {
        const deadline = new Date(Date.now() + 48 * 3600 * 1000).toISOString();
        await admin.from("reschedules").update({
          status: "TEACHER_ACCEPTED",
          response_deadline: deadline,
        }).eq("id", body.rescheduleId);

        const newEndTime = new Date(
          new Date(resched.proposed_time).getTime() + ((resched as any).class?.duration ?? 60) * 60000
        ).toISOString();
        await admin.from("classes").update({
          start_time: resched.proposed_time,
          end_time: newEndTime,
        }).eq("id", resched.class_id);

        const { data: enrolled } = await admin.from("enrollments")
          .select("student_id").eq("class_id", resched.class_id).eq("status", "ACTIVE");
        if (enrolled) {
          for (const e of enrolled) {
            await admin.from("reschedule_responses").insert({
              reschedule_id: body.rescheduleId,
              user_id: e.student_id,
              response_type: null,
            });
            await admin.from("notifications").insert({
              user_id: e.student_id,
              title: "Confirma reagendamiento",
              message: `Tu clase fue reagendada. Tienes 48h para confirmar.`,
              type: "RESCHEDULE_PENDING",
            });
          }
        }
      } else {
        await admin.from("reschedules").update({ status: "TEACHER_REJECTED" }).eq("id", body.rescheduleId);

        const { data: enrolled } = await admin.from("enrollments")
          .select("id, student_id").eq("class_id", resched.class_id).eq("status", "ACTIVE");
        if (enrolled) {
          for (const e of enrolled) {
            await admin.from("payments").update({ status: "REFUND_PENDING" })
              .eq("enrollment_id", e.id).eq("status", "RETAINED");
            await admin.from("notifications").insert({
              user_id: e.student_id,
              title: "Clase reagendada cancelada",
              message: "El profesor rechazó el reagendamiento. Se procesará tu reembolso.",
              type: "RESCHEDULE_REJECTED",
            });
          }
        }
      }

      logInfo("teacher_decision", { rescheduleId: body.rescheduleId, accepted: body.accepted });
      return Response.json({ status: body.accepted ? "TEACHER_ACCEPTED" : "TEACHER_REJECTED" });

    } catch (err) {
      logError("teacher_decision_error", err);
      if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400 });
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
