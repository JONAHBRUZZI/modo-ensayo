import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

const BodySchema = z.object({
  title: z.string().min(1).max(200),
  // nullable/opcional: el flujo "reservar sala" crea drafts sin disciplina
  discipline: z.string().min(1).nullish(),
  disciplineCategory: z.string().min(1).nullish(),
  level: z.enum(["BASICO","INTERMEDIO","AVANZADO"]).nullish(),
  description: z.string().max(2000).optional(),
  capacity: z.number().int().positive().max(500),
  duration: z.number().int().positive(),
  price: z.number().positive(),
  minAge: z.number().int().min(0).optional(),
  maxAge: z.number().int().max(120).optional(),
  startTime: z.string().datetime().optional(),
  roomId: z.string().uuid().optional(),
  draft: z.boolean().default(false),
});

export default {
  fetch: withSupabase({ auth: "user" }, async (req, ctx) => {
    try {
      const { supabaseAdmin: admin, userClaims } = ctx;
      const userId = userClaims!.id;
      const roles: string[] = (userClaims!.appMetadata?.roles as string[]) ?? [];
      const body = BodySchema.parse(await req.json());

      if (roles.includes("TEACHER")) {
        const { data: profile } = await admin.from("profiles")
          .select("identidad_validada").eq("id", userId).single();
        if (!profile?.identidad_validada) {
          return Response.json({ error: "Identidad no validada" }, { status: 403 });
        }
      }

      // Guard Marketplace: para PUBLICAR (no borrador) el profesor debe tener
      // MercadoPago conectado — al confirmarse la clase se le liquida el pago.
      if (!body.draft) {
        const { data: seller } = await admin.from("mp_seller_accounts")
          .select("status").eq("user_id", userId).maybeSingle();
        if (!seller || seller.status !== "CONNECTED") {
          return Response.json({
            error: "Debes conectar tu cuenta de MercadoPago antes de publicar una clase",
            code: "MP_NOT_CONNECTED",
          }, { status: 409 });
        }
      }

      if (body.roomId && body.startTime) {
        const start = new Date(body.startTime);
        const end = new Date(start.getTime() + body.duration * 60000);
        const { data: conflicts } = await admin.from("classes")
          .select("id").eq("room_id", body.roomId)
          .neq("status", "CANCELLED").neq("status", "SUSPENDED")
          .lt("start_time", end.toISOString()).gt("end_time", start.toISOString());
        if (conflicts && conflicts.length > 0) {
          return Response.json({ error: "Conflicto de horario en la sala" }, { status: 409 });
        }
      }

      const status = body.draft ? "DRAFT" : "PUBLISHED";
      const endTime = body.startTime
        ? new Date(new Date(body.startTime).getTime() + body.duration * 60000).toISOString()
        : null;

      const { data: cls, error } = await admin.from("classes").insert({
        title: body.title,
        discipline: body.discipline ?? null,
        discipline_category: body.disciplineCategory ?? null,
        level: body.level ?? null,
        description: body.description ?? null,
        capacity: body.capacity,
        duration: body.duration,
        price: body.price,
        min_age: body.minAge ?? null,
        max_age: body.maxAge ?? null,
        start_time: body.startTime ?? null,
        end_time: endTime,
        room_id: body.roomId ?? null,
        teacher_id: userId,
        status,
        tipo_clase: "PROPIA",
      }).select("*").single();

      if (error) throw error;

      let atributosActualizados = false;
      if (!body.draft && !roles.includes("TEACHER")) {
        await admin.auth.admin.updateUserById(userId, {
          app_metadata: { roles: [...roles, "TEACHER"] },
        });
        await admin.from("notifications").insert({
          user_id: userId,
          title: "Rol Profesor asignado",
          message: "Has sido asignado como Profesor al publicar tu primera clase.",
          type: "ROLE_CHANGE",
        });
        atributosActualizados = true;
      }

      logInfo("class_created", { classId: cls.id, teacherId: userId, draft: body.draft });
      return Response.json({ ...cls, ...(atributosActualizados ? { atributosActualizados: true } : {}) });

    } catch (err) {
      logError("create_class_error", err);
      if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400 });
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
