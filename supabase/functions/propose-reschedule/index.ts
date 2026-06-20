import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

const BodySchema = z.object({
  classId: z.string().uuid(),
  proposedTime: z.string().datetime(),
  reason: z.string().max(500).optional(),
});

export default {
  fetch: withSupabase({ auth: "user" }, async (req, ctx) => {
    try {
      const admin = ctx.supabaseAdmin;
      const body = BodySchema.parse(await req.json());

      const { data: cls, error: clsErr } = await admin.from("classes")
        .select("id,status,teacher_id").eq("id", body.classId).eq("status", "PUBLISHED").single();
      if (clsErr || !cls) return Response.json({ error: "Clase no encontrada" }, { status: 404 });

      const { data: reschedule, error } = await admin.from("reschedules").insert({
        class_id: body.classId,
        teacher_id: cls.teacher_id,
        proposed_time: body.proposedTime,
        reason: body.reason ?? null,
        status: "PROPOSED",
      }).select("*").single();

      if (error) throw error;

      await admin.from("notifications").insert({
        user_id: cls.teacher_id,
        title: "Nueva propuesta de reagendamiento",
        message: `Se ha propuesto reagendar la clase para ${body.proposedTime}`,
        type: "RESCHEDULE_PROPOSED",
      });

      logInfo("reschedule_proposed", { rescheduleId: reschedule.id, classId: body.classId });
      return Response.json(reschedule);

    } catch (err) {
      logError("propose_reschedule_error", err);
      if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400 });
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
