import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

const BodySchema = z.object({
  classId: z.string().uuid(),
  roomId: z.string().uuid(),
  startTime: z.string().datetime(),
  duration: z.number().int().positive(),
});

export default {
  fetch: withSupabase({ auth: "user" }, async (req, ctx) => {
    try {
      const { supabaseAdmin: admin, userClaims } = ctx;
      const userId = userClaims!.id;
      const roles: string[] = (userClaims!.appMetadata?.roles as string[]) ?? [];
      const body = BodySchema.parse(await req.json());

      const { data: draft, error: draftErr } = await admin.from("classes")
        .select("*").eq("id", body.classId).eq("teacher_id", userId).eq("status", "DRAFT").single();
      if (draftErr || !draft) return Response.json({ error: "Borrador no encontrado" }, { status: 404 });

      const { data: room } = await admin.from("rooms")
        .select("*, venue:venues(*)").eq("id", body.roomId).single();
      if (!room || (room as any).venue?.status !== "APROBADA") {
        return Response.json({ error: "Sala no disponible" }, { status: 400 });
      }

      const start = new Date(body.startTime);
      const end = new Date(start.getTime() + body.duration * 60000);
      const { data: conflicts } = await admin.from("classes")
        .select("id").eq("room_id", body.roomId)
        .neq("status", "CANCELLED").neq("status", "SUSPENDED")
        .lt("start_time", end.toISOString()).gt("end_time", start.toISOString());
      if (conflicts && conflicts.length > 0) {
        return Response.json({ error: "Conflicto de horario en la sala" }, { status: 409 });
      }

      // Publicar el draft existente en vez de crear una fila nueva. La capacidad
      // de la clase la define la sala (su tope), no el borrador.
      const { data: cls, error: clsErr } = await admin.from("classes").update({
        duration: body.duration,
        start_time: start.toISOString(),
        end_time: end.toISOString(),
        room_id: body.roomId,
        capacity: (room as any).capacity,
        status: "PUBLISHED",
      }).eq("id", body.classId).eq("teacher_id", userId).eq("status", "DRAFT").select("*").single();
      if (clsErr || !cls) throw clsErr ?? new Error("Error publicando el borrador");

      await admin.from("room_schedule_blocks")
        .update({ status: "OCCUPIED", class_id: cls.id })
        .eq("room_id", body.roomId)
        .lte("start_time", start.toISOString())
        .gte("end_time", end.toISOString())
        .eq("status", "AVAILABLE");

      let atributosActualizados = false;
      if (!roles.includes("TEACHER")) {
        await admin.auth.admin.updateUserById(userId, {
          app_metadata: { roles: [...roles, "TEACHER"] },
        });
        atributosActualizados = true;
      }

      await admin.from("audit_logs").insert({
        actor_id: userId,
        action: "class.reserva_asignada",
        resource_type: "class",
        resource_id: cls.id,
        metadata: { draftId: body.classId, roomId: body.roomId },
      });

      logInfo("reserva_asignada", { classId: body.classId, teacherId: userId, roomId: body.roomId });
      return Response.json({ ...cls, ...(atributosActualizados ? { atributosActualizados: true } : {}) });

    } catch (err) {
      logError("assign_reserva_error", err);
      if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400 });
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
