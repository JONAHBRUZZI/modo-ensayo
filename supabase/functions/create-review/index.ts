import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

const BodySchema = z.object({
  classId: z.string().uuid(),
  targetId: z.string().uuid(),
  targetType: z.enum(["CLASS","VENUE","STUDENT"]),
  score: z.number().int().min(1).max(5),
  comment: z.string().max(1000).optional(),
});

export default {
  fetch: withSupabase({ auth: "user" }, async (req, ctx) => {
    try {
      const { supabaseAdmin: admin, userClaims } = ctx;
      const userId = userClaims!.id;
      const body = BodySchema.parse(await req.json());

      const { data: enrollment } = await admin.from("enrollments")
        .select("id").eq("class_id", body.classId).eq("student_id", userId).eq("status", "ACTIVE").maybeSingle();
      const { data: cls } = await admin.from("classes")
        .select("teacher_id").eq("id", body.classId).eq("teacher_id", userId).maybeSingle();

      if (!enrollment && !cls) {
        return Response.json({ error: "Debes haber participado en la clase para reseñar" }, { status: 403 });
      }

      const { data: review, error } = await admin.from("reviews").insert({
        class_id: body.classId,
        reviewer_id: userId,
        target_type: body.targetType,
        target_id: body.targetId,
        score: body.score,
        comment: body.comment ?? null,
      }).select("*").single();

      if (error) throw error;

      logInfo("review_created", { reviewId: review.id, classId: body.classId });
      return Response.json(review);

    } catch (err) {
      logError("create_review_error", err);
      if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400 });
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
