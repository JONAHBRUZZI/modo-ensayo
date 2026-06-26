import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";

export default {
  fetch: withSupabase({ auth: "user" }, async (_req, ctx) => {
    try {
      const { supabaseAdmin: admin, userClaims } = ctx;
      const roles: string[] = (userClaims!.appMetadata?.roles as string[]) ?? [];
      if (!roles.includes("VENUE_ADMIN") && !roles.includes("ADMIN")) {
        return Response.json({ error: "Forbidden" }, { status: 403 });
      }

      const { error } = await admin.rpc("regenerate_schedule_blocks");
      if (error) throw error;

      logInfo("blocks_regenerated", {});
      return Response.json({ status: "ok" });

    } catch (err) {
      logError("generate_blocks_error", err);
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
