// deno-lint-ignore-file no-explicit-any
import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";
import { triggerStudentReschedule } from "../_shared/reschedule.ts";
import { z } from "npm:zod@3";

// Reagendamiento de una clase PUBLICADA que todavía no ocurre, por parte del
// PROFESOR dueño de la clase. El profesor solo puede moverla a otro horario de
// la MISMA sala que ya arrienda, así que NO hay pago involucrado: se sueltan los
// bloques viejos y se toman los nuevos. Los alumnos inscritos deben aceptar o
// rechazar la nueva fecha (rechazo → reembolso), igual que en el reagendamiento
// de la sede.
//
// Distinto de `sede-reschedule-class`, que atiende clases ya caídas (SUSPENDED,
// dentro de la ventana de 24h) y donde los bloques viejos ya fueron liberados
// por `confirm-class`.
const BodySchema = z.object({
  classId: z.string().uuid(),
  blockIds: z.array(z.string().uuid()).min(1).max(24),
  reason: z.string().min(1).max(500),
});

// Una clase con cupos libres queda PUBLISHED; al llenarse pasa a FULL. Ambas
// están vigentes y son reagendables.
const REAGENDABLE = ["PUBLISHED", "FULL"];

export default {
  fetch: withSupabase({ auth: "user" }, async (req: Request, ctx: any) => {
    try {
      const { supabaseAdmin: admin, userClaims } = ctx;
      const userId = userClaims!.id;
      const roles: string[] = (userClaims!.appMetadata?.roles as string[]) ?? [];
      const body = BodySchema.parse(await req.json());

      const { data: cls } = await admin.from("classes")
        .select("id, status, teacher_id, room_id, start_time")
        .eq("id", body.classId).single();
      if (!cls) return Response.json({ error: "Clase no encontrada" }, { status: 404 });

      // Solo el profesor dueño de la clase (o un ADMIN) puede reagendarla.
      if (cls.teacher_id !== userId && !roles.includes("ADMIN")) {
        return Response.json({ error: "Forbidden" }, { status: 403 });
      }
      if (!REAGENDABLE.includes(cls.status)) {
        return Response.json({ error: "Solo se puede reagendar una clase publicada" }, { status: 409 });
      }
      if (new Date(cls.start_time).getTime() <= Date.now()) {
        return Response.json({ error: "La clase ya comenzó; no se puede reagendar" }, { status: 409 });
      }
      if (!cls.room_id) {
        return Response.json({ error: "La clase no tiene una sala asignada" }, { status: 409 });
      }

      // Bloques válidos: de la MISMA sala, disponibles y futuros.
      const { data: blocks } = await admin.from("room_schedule_blocks")
        .select("id, start_time, end_time, status")
        .eq("room_id", cls.room_id).in("id", body.blockIds);
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

      // Toma los bloques nuevos (guard atómico: solo los que sigan AVAILABLE).
      const { data: held } = await admin.from("room_schedule_blocks")
        .update({ status: "OCCUPIED", class_id: body.classId })
        .in("id", body.blockIds).eq("status", "AVAILABLE").select("id");
      if (!held || held.length !== body.blockIds.length) {
        await admin.from("room_schedule_blocks")
          .update({ status: "AVAILABLE", class_id: null })
          .in("id", body.blockIds).eq("class_id", body.classId).eq("status", "OCCUPIED");
        return Response.json({ error: "Algún horario ya no está disponible" }, { status: 409 });
      }

      // Recién ahora libera los bloques del horario viejo (los de esta clase que
      // no quedaron en la selección nueva). Si se hiciera antes, un fallo al
      // tomar los nuevos dejaría la clase sin horario.
      await admin.from("room_schedule_blocks")
        .update({ status: "AVAILABLE", class_id: null })
        .eq("class_id", body.classId).not("id", "in", `(${body.blockIds.join(",")})`);

      // La sala no cambia, así que el cupo tampoco.
      await admin.from("classes").update({
        start_time: startIso, end_time: endIso, duration: durationMin,
      }).eq("id", body.classId);

      // Dispara la decisión de los alumnos (aceptar/rechazar la nueva fecha).
      await triggerStudentReschedule(admin, {
        classId: body.classId, teacherId: cls.teacher_id, newStartTime: startIso, reason: body.reason,
      });

      await admin.from("audit_logs").insert({
        actor_id: userId, action: "class.teacher_rescheduled",
        resource_type: "class", resource_id: body.classId,
        metadata: { start_time: startIso, previous_start_time: cls.start_time },
      });

      logInfo("teacher_reschedule_class", { classId: body.classId, startTime: startIso });
      return Response.json({ status: "ok", startTime: startIso });

    } catch (err) {
      logError("teacher_reschedule_error", err);
      if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400 });
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
