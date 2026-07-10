// deno-lint-ignore-file no-explicit-any
import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";
import { triggerStudentReschedule } from "../_shared/reschedule.ts";
import { z } from "npm:zod@3";

// Reagendamiento de una clase caída (no realizada) por parte de la SEDE, para
// clases ASIGNADA (creadas por la sede). La sede usa su propia sala, así que NO
// hay pago. Ocupa los bloques nuevos, republica la clase en ese horario, cierra
// la ventana de reagendamiento y dispara la decisión de los alumnos. Notifica al
// profesor dependiente (solo aviso).
const BodySchema = z.object({
  classId: z.string().uuid(),
  roomId: z.string().uuid(),
  blockIds: z.array(z.string().uuid()).min(1).max(24),
  reason: z.string().min(1).max(500),
});

export default {
  fetch: withSupabase({ auth: "user" }, async (req: Request, ctx: any) => {
    try {
      const { supabaseAdmin: admin, userClaims } = ctx;
      const userId = userClaims!.id;
      const roles: string[] = (userClaims!.appMetadata?.roles as string[]) ?? [];
      if (!roles.includes("VENUE_ADMIN") && !roles.includes("ADMIN")) {
        return Response.json({ error: "Forbidden" }, { status: 403 });
      }
      const body = BodySchema.parse(await req.json());

      // La clase debe estar SUSPENDED y dentro de la ventana de reagendamiento.
      const { data: cls } = await admin.from("classes")
        .select("id, status, teacher_id, reschedule_deadline")
        .eq("id", body.classId).single();
      if (!cls) return Response.json({ error: "Clase no encontrada" }, { status: 404 });
      if (cls.status !== "SUSPENDED" || !cls.reschedule_deadline) {
        return Response.json({ error: "La clase no está en ventana de reagendamiento" }, { status: 409 });
      }

      // La nueva sala debe pertenecer a una sede que administra el usuario.
      const { data: room } = await admin.from("rooms")
        .select("id, capacity, venue:venues(admin_id)").eq("id", body.roomId).single();
      const venueAdminId = (room as any)?.venue?.admin_id;
      if (!room || (venueAdminId !== userId && !roles.includes("ADMIN"))) {
        return Response.json({ error: "La sala no pertenece a tu sede" }, { status: 403 });
      }

      // Bloques válidos, de esa sala, disponibles y futuros.
      const { data: blocks } = await admin.from("room_schedule_blocks")
        .select("id, start_time, end_time, status")
        .eq("room_id", body.roomId).in("id", body.blockIds);
      if (!blocks || blocks.length !== body.blockIds.length ||
          blocks.some((b: any) => b.status !== "AVAILABLE")) {
        return Response.json({ error: "Algún horario ya no está disponible" }, { status: 409 });
      }
      if (blocks.some((b: any) => new Date(b.start_time).getTime() <= Date.now())) {
        return Response.json({ error: "No se puede reagendar a un horario que ya pasó" }, { status: 409 });
      }
      const ordenados = blocks.slice().sort((a: any, b: any) =>
        new Date(a.start_time).getTime() - new Date(b.start_time).getTime());
      const startIso = ordenados[0].start_time;
      const endIso = ordenados[ordenados.length - 1].end_time;
      const durationMin = Math.round((new Date(endIso).getTime() - new Date(startIso).getTime()) / 60000);

      // Ocupa los bloques nuevos (guard atómico contra carrera).
      const { data: held } = await admin.from("room_schedule_blocks")
        .update({ status: "OCCUPIED", class_id: body.classId })
        .in("id", body.blockIds).eq("status", "AVAILABLE").select("id");
      if (!held || held.length !== body.blockIds.length) {
        await admin.from("room_schedule_blocks")
          .update({ status: "AVAILABLE", class_id: null })
          .in("id", body.blockIds).eq("class_id", body.classId).eq("status", "OCCUPIED");
        return Response.json({ error: "Algún horario ya no está disponible" }, { status: 409 });
      }

      // Republica la clase en el nuevo horario y cierra la ventana.
      const patch: Record<string, unknown> = {
        room_id: body.roomId, start_time: startIso, end_time: endIso,
        duration: durationMin, status: "PUBLISHED", reschedule_deadline: null,
      };
      if ((room as any).capacity != null) patch.capacity = (room as any).capacity;
      await admin.from("classes").update(patch).eq("id", body.classId);

      // Dispara la decisión de los alumnos (aceptar/rechazar la nueva fecha).
      await triggerStudentReschedule(admin, {
        classId: body.classId, teacherId: cls.teacher_id, newStartTime: startIso, reason: body.reason,
      });

      // Aviso (no accionable) al profesor dependiente.
      if (cls.teacher_id) {
        await admin.from("notifications").insert({
          user_id: cls.teacher_id,
          title: "Clase reagendada por la sede",
          message: "La sede reagendó una de tus clases a un nuevo horario.",
          type: "CLASS_RESCHEDULED",
        });
      }

      await admin.from("audit_logs").insert({
        actor_id: userId, action: "class.sede_rescheduled",
        resource_type: "class", resource_id: body.classId,
        metadata: { room_id: body.roomId, start_time: startIso },
      });

      logInfo("sede_reschedule_class", { classId: body.classId, startTime: startIso });
      return Response.json({ status: "ok", startTime: startIso });

    } catch (err) {
      logError("sede_reschedule_error", err);
      if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400 });
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
