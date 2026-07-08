import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

// Guarda la asistencia de una clase de una sola vez ("pasar lista"). El front
// manda una marca por alumno inscrito (present true/false); acá se resuelve el
// beneficiario real de cada inscripción y se hace upsert idempotente. Requiere
// service role porque el profesor no tiene policy de UPDATE sobre attendances.
const BodySchema = z.object({
  classId: z.string().uuid(),
  marks: z.array(z.object({
    enrollmentId: z.string().uuid(),
    present: z.boolean(),
  })).min(1).max(500),
});

export default {
  fetch: withSupabase({ auth: "user" }, async (req, ctx) => {
    try {
      const { supabaseAdmin: admin, userClaims } = ctx;
      const userId = userClaims!.id;
      const roles: string[] = (userClaims!.appMetadata?.roles as string[]) ?? [];

      const body = BodySchema.parse(await req.json());

      // Autorización: el profesor de la clase, el admin de la sede o un ADMIN.
      const { data: cls } = await admin.from("classes")
        .select("id, teacher_id, room_id").eq("id", body.classId).single();
      if (!cls) return Response.json({ error: "Clase no encontrada" }, { status: 404 });

      let venueAdminId: string | null = null;
      if (cls.room_id) {
        const { data: room } = await admin.from("rooms")
          .select("venue_id").eq("id", cls.room_id).maybeSingle();
        if (room?.venue_id) {
          const { data: venue } = await admin.from("venues")
            .select("admin_id").eq("id", room.venue_id).maybeSingle();
          venueAdminId = venue?.admin_id ?? null;
        }
      }

      const autorizado = cls.teacher_id === userId || venueAdminId === userId || roles.includes("ADMIN");
      if (!autorizado) return Response.json({ error: "Forbidden" }, { status: 403 });

      // Resuelve el beneficiario real de cada inscripción (asociado o titular).
      const enrollmentIds = body.marks.map((m) => m.enrollmentId);
      const { data: enrolls } = await admin.from("enrollments")
        .select("id, student_id, beneficiary_id, beneficiary_type")
        .eq("class_id", body.classId).in("id", enrollmentIds);
      const enrById = new Map((enrolls ?? []).map((e: any) => [e.id, e]));

      const rows = [];
      for (const m of body.marks) {
        const e = enrById.get(m.enrollmentId);
        if (!e) continue; // la inscripción no pertenece a esta clase → se ignora
        rows.push({
          class_id: body.classId,
          beneficiary_id: e.beneficiary_id ?? e.student_id,
          beneficiary_type: e.beneficiary_type ?? "SELF",
          present: m.present,
          marked_by: userId,
        });
      }
      if (rows.length === 0) return Response.json({ error: "Sin marcas válidas" }, { status: 400 });

      const { error } = await admin.from("attendances")
        .upsert(rows, { onConflict: "class_id,beneficiary_id" });
      if (error) throw error;

      logInfo("attendance_saved", { classId: body.classId, count: rows.length });
      return Response.json({ status: "ok", saved: rows.length });
    } catch (err) {
      logError("save_attendance_error", err);
      if (err instanceof z.ZodError) {
        return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400 });
      }
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
