import { withSupabase } from "npm:@supabase/server";
import { logError } from "../_shared/logger.ts";

export default {
  fetch: withSupabase({ auth: "user" }, async (_req, ctx) => {
    try {
      const { supabaseAdmin: admin, userClaims } = ctx;
      const roles: string[] = (userClaims!.appMetadata?.roles as string[]) ?? [];
      if (!roles.includes("ADMIN")) return Response.json({ error: "Forbidden" }, { status: 403 });

      const [users, usersWithoutIdentity, classes, venues, payments, identity, reviews, sessions] = await Promise.all([
        admin.from("profiles").select("*", { count: "exact", head: true }),
        admin.from("profiles").select("*", { count: "exact", head: true }).eq("identidad_validada", false),
        admin.from("classes").select("status, price, capacity"),
        admin.from("venues").select("status"),
        admin.from("payments").select("status, amount"),
        admin.from("identity_verifications").select("*", { count: "exact", head: true }).eq("status", "PENDING"),
        admin.from("reviews").select("score"),
        admin.from("payment_sessions").select("status"),
      ]);

      const classesData = classes.data ?? [];
      const paymentsData = payments.data ?? [];
      const venuesData = venues.data ?? [];
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
        usuarios: users.count ?? 0,
        usuariosPendientes: usersWithoutIdentity.count ?? 0,
        sedes: venuesData.length,
        sedesPendientes: venuesData.filter((v) => v.status === "PENDIENTE_APROBACION").length,
        pendientes: identity.count ?? 0,
        clases: classesData.length,
        clasesActivas: classesData.filter((c) => c.status === "PUBLISHED").length,
        clasesCompletadas: classesData.filter((c) => c.status === "COMPLETED").length,
        totalIngresos: totalRevenue,
        ingresoRetenido: retainedTotal,
        calificacionPromedio: parseFloat(avgRating),
        tasaConversion: parseFloat(conversionRate),
      });

    } catch (err) {
      logError("admin_stats_error", err);
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
