import { withSupabase } from "npm:@supabase/server";
import { logError } from "../_shared/logger.ts";

export default {
  fetch: withSupabase({ auth: "user" }, async (_req, ctx) => {
    try {
      const { supabaseAdmin: admin, userClaims } = ctx;
      const roles: string[] = (userClaims!.appMetadata?.roles as string[]) ?? [];
      if (!roles.includes("ADMIN")) return Response.json({ error: "Forbidden" }, { status: 403 });

      const [
        users, usersWithoutIdentity,
        totalClases, clasesActivas, clasesCompletadas,
        totalSedes, sedesPendientes,
        identity,
        payments, reviews,
        approvedSessions, totalSessions,
      ] = await Promise.all([
        admin.from("profiles").select("*", { count: "exact", head: true }),
        admin.from("profiles").select("*", { count: "exact", head: true }).eq("identidad_validada", false),
        admin.from("classes").select("*", { count: "exact", head: true }),
        admin.from("classes").select("*", { count: "exact", head: true }).eq("status", "PUBLISHED"),
        admin.from("classes").select("*", { count: "exact", head: true }).eq("status", "COMPLETED"),
        admin.from("venues").select("*", { count: "exact", head: true }),
        admin.from("venues").select("*", { count: "exact", head: true }).eq("status", "PENDIENTE_APROBACION"),
        admin.from("identity_verifications").select("*", { count: "exact", head: true }).eq("status", "PENDING"),
        // Límite de seguridad: estas columnas son las únicas necesarias para los agregados
        admin.from("payments").select("status, amount").limit(10000),
        admin.from("reviews").select("score").limit(10000),
        admin.from("payment_sessions").select("*", { count: "exact", head: true }).eq("status", "APPROVED"),
        admin.from("payment_sessions").select("*", { count: "exact", head: true }),
      ]);

      if (users.error) logError("users_query_error", users.error);
      if (usersWithoutIdentity.error) logError("users_without_identity_error", usersWithoutIdentity.error);
      if (identity.error) logError("identity_query_error", identity.error);
      if (sedesPendientes.error) logError("venues_query_error", sedesPendientes.error);

      const paymentsData = payments.data ?? [];
      const reviewsData = reviews.data ?? [];

      const totalRevenue = paymentsData
        .filter((p) => p.status === "RELEASED")
        .reduce((s, p) => s + (p.amount || 0), 0);

      const retainedTotal = paymentsData
        .filter((p) => p.status === "RETAINED")
        .reduce((s, p) => s + (p.amount || 0), 0);

      const avgRating = reviewsData.length > 0
        ? (reviewsData.reduce((s, r) => s + (r.score || 0), 0) / reviewsData.length).toFixed(2)
        : "0";

      const totalSessionsCount = totalSessions.count ?? 0;
      const conversionRate = totalSessionsCount > 0
        ? ((approvedSessions.count ?? 0) / totalSessionsCount * 100).toFixed(1)
        : "0";

      return Response.json({
        usuarios: users.count ?? 0,
        usuariosPendientes: usersWithoutIdentity.count ?? 0,
        sedes: totalSedes.count ?? 0,
        sedesPendientes: sedesPendientes.count ?? 0,
        pendientes: identity.count ?? 0,
        clases: totalClases.count ?? 0,
        clasesActivas: clasesActivas.count ?? 0,
        clasesCompletadas: clasesCompletadas.count ?? 0,
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
