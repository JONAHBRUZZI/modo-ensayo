import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

// Equivalente a POST /rooms/{roomId}/book del backend: un profesor autenticado
// marca un bloque puntual como OCCUPIED y lo vincula a una clase.
// Usa supabaseAdmin para saltar la RLS rsb_update_admin (que solo permite al
// admin de sede). Soporta la selección múltiple de bloques (se llama por bloque).
const BodySchema = z.object({
  blockId: z.string().uuid(),
  classId: z.string().uuid().nullish(),
});

export default {
  fetch: withSupabase({ auth: "user" }, async (req, ctx) => {
    try {
      const admin = ctx.supabaseAdmin;
      const body = BodySchema.parse(await req.json());

      // El bloque debe existir y estar disponible (evita doble reserva)
      const { data: block, error: blkErr } = await admin
        .from("room_schedule_blocks")
        .select("id,status")
        .eq("id", body.blockId)
        .single();
      if (blkErr || !block) {
        return Response.json({ error: "Bloque no encontrado" }, { status: 404 });
      }
      if (block.status !== "AVAILABLE") {
        return Response.json({ error: "El bloque ya no está disponible" }, { status: 409 });
      }

      const { data: updated, error } = await admin
        .from("room_schedule_blocks")
        .update({ status: "OCCUPIED", class_id: body.classId ?? null })
        .eq("id", body.blockId)
        .eq("status", "AVAILABLE") // guard atómico contra carrera
        .select("*")
        .single();
      if (error) throw error;
      if (!updated) {
        return Response.json({ error: "El bloque ya no está disponible" }, { status: 409 });
      }

      logInfo("slot_booked", { blockId: body.blockId, classId: body.classId ?? null });
      return Response.json(updated);

    } catch (err) {
      logError("book_slot_error", err);
      if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400 });
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
