import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

const BodySchema = z.object({
  title: z.string().min(1).max(200),
  discipline: z.string().min(1).nullish(),
  disciplineCategory: z.string().min(1).nullish(),
  level: z.enum(["BASICO","INTERMEDIO","AVANZADO"]).nullish(),
  description: z.string().max(2000).optional(),
  capacity: z.number().int().positive().max(500).optional(),
  duration: z.number().int().positive(),
  price: z.number().positive(),
  minAge: z.number().int().min(0).optional(),
  maxAge: z.number().int().max(120).optional(),
  startTime: z.string().datetime().optional(),
  roomId: z.string().uuid().optional(),
  draft: z.boolean().default(false),
  tipoClase: z.enum(["PROPIA", "ASIGNADA"]).default("PROPIA"),
  teacherId: z.string().uuid().nullish(),
  honorario: z.number().positive().nullish(),
});

export default {
  fetch: withSupabase({ auth: "user" }, async (req, ctx) => {
    try {
      const { supabaseAdmin: admin, userClaims } = ctx;
      const userId = userClaims!.id;
      const roles: string[] = (userClaims!.appMetadata?.roles as string[]) ?? [];
      const body = BodySchema.parse(await req.json());

      // ASIGNADA = clase creada por la sede (profe dependiente + honorario fijo).
      // El creador es el gestor de sede, no el profesor: cambia validación y ruteo.
      const tipoClase = body.tipoClase ?? "PROPIA";
      const esAsignada = tipoClase === "ASIGNADA";
      let teacherId = userId;
      let honorario: number | null = null;

      if (esAsignada) {
        if (!roles.includes("VENUE_ADMIN") && !roles.includes("ADMIN")) {
          return Response.json({ error: "Solo el admin de sede puede crear clases ASIGNADA" }, { status: 403 });
        }
        if (!body.teacherId) {
          return Response.json({ error: "Debe seleccionar un profesor dependiente" }, { status: 400 });
        }
        if (!body.roomId) {
          return Response.json({ error: "Debe seleccionar una sala" }, { status: 400 });
        }
        // La sala debe pertenecer a una sede del gestor.
        const { data: room } = await admin.from("rooms")
          .select("venue_id, venues!inner(admin_id)").eq("id", body.roomId).single();
        const venueId = (room as any)?.venue_id;
        if (!room || (room as any).venues?.admin_id !== userId) {
          return Response.json({ error: "La sala no pertenece a tu sede" }, { status: 403 });
        }
        // El profe debe ser dependiente ACTIVO de ESA misma sede (no de otra).
        const { data: vt } = await admin.from("venue_teachers")
          .select("id").eq("teacher_id", body.teacherId).eq("venue_id", venueId)
          .eq("status", "ACTIVE").maybeSingle();
        if (!vt) {
          return Response.json({ error: "El profesor no está vinculado a esta sede" }, { status: 400 });
        }
        teacherId = body.teacherId;
        honorario = body.honorario ?? null;
      }

      // Identidad: solo se exige al profesor que publica su propia clase (PROPIA).
      if (!esAsignada && roles.includes("TEACHER")) {
        const { data: profile } = await admin.from("profiles")
          .select("identidad_validada").eq("id", userId).single();
        if (!profile?.identidad_validada) {
          return Response.json({ error: "Identidad no validada" }, { status: 403 });
        }
      }

      // Guard Marketplace: para PUBLICAR una clase PROPIA (no borrador) el profesor
      // debe tener MercadoPago conectado (se le liquida el pago). Las ASIGNADA no lo
      // requieren: el honorario del profe dependiente se registra como payout (tracked).
      if (!body.draft && !esAsignada) {
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

      // La capacidad de la clase la define la sala (su tope): si hay sala, se toma
      // rooms.capacity e ignora lo que venga del formulario. Sin sala (borrador),
      // se usa un placeholder que se corrige al asignar la sala.
      let capacity = body.capacity ?? 1;
      if (body.roomId) {
        const { data: roomCap } = await admin.from("rooms")
          .select("capacity").eq("id", body.roomId).single();
        if (roomCap?.capacity) capacity = roomCap.capacity;
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
        duration: body.duration,
        price: body.price,
        capacity,
        min_age: body.minAge ?? null,
        max_age: body.maxAge ?? null,
        start_time: body.startTime ?? null,
        end_time: endTime,
        room_id: body.roomId ?? null,
        teacher_id: teacherId,
        status,
        tipo_clase: tipoClase,
        honorario,
      }).select("*").single();

      if (error) throw error;

      let atributosActualizados = false;
      if (!body.draft && !esAsignada && !roles.includes("TEACHER")) {
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
