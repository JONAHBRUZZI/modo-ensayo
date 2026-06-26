import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

const BodySchema = z.object({
  venueId: z.string().uuid(),
  action: z.enum(["approve","reject"]),
  reason: z.string().optional(),
});

export default {
  fetch: withSupabase({ auth: "user" }, async (req, ctx) => {
    try {
      const { supabaseAdmin: admin, userClaims } = ctx;
      const userId = userClaims!.id;
      const roles: string[] = (userClaims!.appMetadata?.roles as string[]) ?? [];
      if (!roles.includes("ADMIN")) return Response.json({ error: "Forbidden" }, { status: 403 });

      const body = BodySchema.parse(await req.json());

      const { data: venue } = await admin.from("venues")
        .select("*").eq("id", body.venueId).single();
      if (!venue) return Response.json({ error: "Sede no encontrada" }, { status: 404 });

      if (body.action === "approve") {
        await admin.from("venues").update({
          status: "APROBADA", rejection_reason: null,
        }).eq("id", body.venueId);

        await admin.from("profiles").update({ tiene_sede_aprobada: true }).eq("id", venue.admin_id);

        const { data: adminUser } = await admin.auth.admin.getUserById(venue.admin_id);
        const existingRoles: string[] = adminUser.user?.app_metadata?.roles ?? [];
        if (!existingRoles.includes("VENUE_ADMIN")) {
          await admin.auth.admin.updateUserById(venue.admin_id, {
            app_metadata: { roles: [...existingRoles, "VENUE_ADMIN"] },
          });
        }

        await admin.from("notifications").insert({
          user_id: venue.admin_id,
          title: "Sede aprobada",
          message: "Tu sede ha sido aprobada. Ya puedes gestionar salas.",
          type: "VENUE_APPROVED",
        });
      } else {
        await admin.from("venues").update({
          status: "RECHAZADA", rejection_reason: body.reason,
        }).eq("id", body.venueId);

        await admin.from("notifications").insert({
          user_id: venue.admin_id,
          title: "Sede rechazada",
          message: `Motivo: ${body.reason}`,
          type: "VENUE_REJECTED",
        });
      }

      await admin.from("audit_logs").insert({
        actor_id: userId,
        action: `venue.${body.action}d`,
        resource_type: "venue",
        resource_id: body.venueId,
        metadata: { reason: body.reason },
      });

      logInfo(`venue_${body.action}d`, { venueId: body.venueId, adminId: userId });
      return Response.json({ status: "ok" });

    } catch (err) {
      logError("approve_venue_error", err);
      if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400 });
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
