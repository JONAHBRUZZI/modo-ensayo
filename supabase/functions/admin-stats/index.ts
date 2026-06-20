import { withSupabase } from "npm:@supabase/server";
import { logError } from "../_shared/logger.ts";

export default {
  fetch: withSupabase({ auth: "user" }, async (_req, ctx) => {
    try {
      const { supabaseAdmin: admin, userClaims } = ctx;
      const roles: string[] = (userClaims!.appMetadata?.roles as string[]) ?? [];
      if (!roles.includes("ADMIN")) return Response.json({ error: "Forbidden" }, { status: 403 });

      const [users, classes, venues, payments, identity, reviews, sessions] = await Promise.all([
        admin.from("profiles").select("*", { count: "exact", head: true }),
        admin.from("classes").select("status, price, capacity"),
        admin.from("venues").select("status"),
        admin.from("payments").select("status, amount"),
        admin.from("identity_verifications").select("status"),
        admin.from("reviews").select("score"),
        admin.from("payment_sessions").select("status"),
      ]);

      const classesData = classes.data ?? [];
      const paymentsData = payments.data ?? [];
      const venuesData = venues.data ?? [];
      const identityData = identity.data ?? [];
      const reviewsData = reviews.data ?? [];
      const sessionsData = sessions.data ?? [];

      const totalRevenue = paymentsData
        .filter((p) => p.status === "RELEASED")
        .reduce((s, p) => s + (p.amount || 0), 0);

      const retainedTotal = paymentsData
        .filter((p) => p.status === "RETAINED")
        .reduce((s, p) => s + (p.amount || 0), 0);

      const avgRating = reviewsData.length > 0
        ? (reviewsData.reduce((s, r) => s + (r.score || 0), 0) / reviewsData.length).toFixed(2)
        : "0";

      const conversionRate = sessionsData.length > 0
        ? (sessionsData.filter((s) => s.status === "APPROVED").length / sessionsData.length * 100).toFixed(1)
        : "0";

      return Response.json({
        totalUsers: users.count ?? 0,
        activeClasses: classesData.filter((c) => c.status === "PUBLISHED").length,
        completedClasses: classesData.filter((c) => c.status === "COMPLETED").length,
        pendingVenues: venuesData.filter((v) => v.status === "PENDIENTE_APROBACION").length,
        pendingIdentity: identityData.filter((i) => i.status === "PENDING").length,
        totalRevenue,
        retainedTotal,
        avgRating: parseFloat(avgRating),
        conversionRate: parseFloat(conversionRate),
      });

    } catch (err) {
      logError("admin_stats_error", err);
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
