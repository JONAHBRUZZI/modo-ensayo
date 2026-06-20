import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

const BodySchema = z.object({
  classId: z.string().uuid(),
  realized: z.boolean(),
});

export default {
  fetch: withSupabase({ auth: "user" }, async (req, ctx) => {
    try {
      const { supabaseAdmin: admin, userClaims } = ctx;
      const userId = userClaims!.id;
      const roles: string[] = (userClaims!.appMetadata?.roles as string[]) ?? [];
      if (!roles.includes("VENUE_ADMIN") && !roles.includes("ADMIN")) {
        return Response.json({ error: "Forbidden" }, { status: 403 });
      }

      const body = BodySchema.parse(await req.json());

      const { data: cls } = await admin.from("classes")
        .select("id, status, room:rooms(venue:venues(admin_id))").eq("id", body.classId).single();
      if (!cls) return Response.json({ error: "Clase no encontrada" }, { status: 404 });

      const venueAdminId = (cls as any).room?.venue?.admin_id;
      if (venueAdminId !== userId && !roles.includes("ADMIN")) {
        return Response.json({ error: "No autorizado para esta sede" }, { status: 403 });
      }

      if (body.realized) {
        const { data: enrollments } = await admin.from("enrollments")
          .select("id").eq("class_id", body.classId).eq("status", "ACTIVE");
        if (enrollments) {
          for (const e of enrollments) {
            await admin.from("payments").update({ status: "RELEASED" })
              .eq("enrollment_id", e.id).eq("status", "RETAINED");
          }
        }
      }

      await admin.from("classes").update({
        status: body.realized ? "COMPLETED" : "SUSPENDED",
      }).eq("id", body.classId);

      await admin.from("audit_logs").insert({
        actor_id: userId,
        action: body.realized ? "class.confirmed_realized" : "class.confirmed_not_realized",
        resource_type: "class",
        resource_id: body.classId,
      });

      logInfo(body.realized ? "class_confirmed" : "class_not_realized", { classId: body.classId });
      return Response.json({ status: body.realized ? "COMPLETED" : "SUSPENDED" });

    } catch (err) {
      logError("confirm_class_error", err);
      if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400 });
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
