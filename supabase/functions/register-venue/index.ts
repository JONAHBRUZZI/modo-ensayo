import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

const DocumentPathSchema = z.object({
  path: z.string().min(1),
  tipo: z.string().min(1),
  nombre: z.string().optional(),
  tipoArchivo: z.string().optional(),
});

const BodySchema = z.object({
  name: z.string().min(1),
  city: z.string().optional(),
  region: z.string().optional(),
  comuna: z.string().optional(),
  address: z.string().optional(),
  description: z.string().optional(),
  phone: z.string().optional(),
  email: z.string().email().optional(),
  tipo: z.enum(["SEDE","HOME_STUDIO"]).default("SEDE"),
  instagram: z.string().optional(),
  youtube: z.string().optional(),
  sitioWeb: z.string().optional(),
  facebook: z.string().optional(),
  documentPaths: z.array(DocumentPathSchema).optional().default([]),
});

export default {
  fetch: withSupabase({ auth: "user" }, async (req, ctx) => {
    try {
      const { supabaseAdmin: admin, userClaims } = ctx;
      const userId = userClaims!.id;
      const body = BodySchema.parse(await req.json());

      const { data: profile } = await admin.from("profiles")
        .select("identidad_validada").eq("id", userId).single();
      if (!profile?.identidad_validada) {
        return Response.json({ error: "Identidad no validada. Sube tu documento primero." }, { status: 403 });
      }

      const { sitioWeb, documentPaths, ...rest } = body;
      const venueData = { ...rest, sitio_web: sitioWeb ?? null };

      const { data: existing } = await admin.from("venues")
        .select("id").eq("admin_id", userId).eq("status", "RECHAZADA").maybeSingle();

      let venue;
      if (existing) {
        const { data: updated, error: updateErr } = await admin.from("venues").update({
          ...venueData, status: "PENDIENTE_APROBACION",
        }).eq("id", existing.id).select("*").single();
        if (updateErr || !updated) throw updateErr ?? new Error("Error actualizando la sede");
        venue = updated;
      } else {
        const { data: created, error: createErr } = await admin.from("venues").insert({
          ...venueData, admin_id: userId, status: "PENDIENTE_APROBACION",
        }).select("*").single();
        if (createErr || !created) throw createErr ?? new Error("Error registrando la sede");
        venue = created;
      }

      await admin.from("notifications").insert({
        user_id: userId,
        title: "Sede registrada",
        message: "Tu sede está pendiente de aprobación.",
        type: "VENUE_REGISTERED",
      });

      await admin.from("audit_logs").insert({
        actor_id: userId,
        action: "venue.registered",
        resource_type: "venue",
        resource_id: venue.id,
      });

      if (body.documentPaths.length > 0) {
        const docs = body.documentPaths.map((d) => ({
          venue_id: venue.id,
          file_url: d.path,
          tipo: d.tipo,
          nombre: d.nombre ?? null,
          tipo_archivo: d.tipoArchivo ?? null,
          estado: "PENDIENTE",
        }));
        const { error: docErr } = await admin.from("venue_documents").insert(docs);
        if (docErr) {
          logError("venue_documents_insert_failed", new Error(docErr.message), { docErr });
        }
      }

      logInfo("venue_registered", { venueId: venue.id, adminId: userId });
      return Response.json(venue);

    } catch (err) {
      logError("register_venue_error", err);
      if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400 });
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
