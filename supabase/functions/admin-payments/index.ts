import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

// Panel de admin de pagos: giros pendientes a profesores (teacher_payouts),
// reembolsos fallidos (payments FAILED) y finanzas del ciclo de corte (costo real
// de MercadoPago vs. comisión cobrada = margen). Todo requiere service_role:
// leer emails de auth.users y cerrar giros/reembolsos que el cliente no puede tocar.
const BodySchema = z.object({
  action: z.enum(["list", "finance", "markPayoutPaid", "retryRefund", "markRefundResolved"]),
  payoutId: z.string().uuid().optional(),
  paymentId: z.string().uuid().optional(),
  reference: z.string().max(200).optional(),
  month: z.string().regex(/^\d{4}-\d{2}$/).optional(), // 'YYYY-MM'
});

// deno-lint-ignore no-explicit-any
type Admin = any;

// Ventana del ciclo de corte para un mes 'YYYY-MM': (corte del mes previo, corte
// de este mes]. Ej. cutoffDay=24, month=2026-07 → (2026-06-24 23:59:59, 2026-07-24 23:59:59].
// Se trabaja en UTC (suficiente para el panel; el giro real lo hace el admin).
function cicloDeCorte(month: string, cutoffDay: number) {
  const [y, m] = month.split("-").map(Number); // m: 1-12
  const finDia = Math.min(cutoffDay, 28); // evita desbordes en meses cortos
  const end = new Date(Date.UTC(y, m - 1, finDia, 23, 59, 59, 999));
  const start = new Date(Date.UTC(y, m - 2, finDia, 23, 59, 59, 999));
  return { start: start.toISOString(), end: end.toISOString() };
}

async function getCutoffDay(admin: Admin): Promise<number> {
  const { data } = await admin.from("app_settings")
    .select("value").eq("key", "payout_cutoff_day").maybeSingle();
  const n = Number(data?.value);
  return Number.isFinite(n) && n >= 1 && n <= 28 ? n : 24;
}

// Empareja emails de auth.users a un set de ids (la Admin API no filtra por id,
// así que se pagina y se indexa localmente). Sólo para conjuntos moderados.
async function emailsById(admin: Admin, ids: Set<string>): Promise<Map<string, string>> {
  const out = new Map<string, string>();
  if (ids.size === 0) return out;
  let page = 1;
  const perPage = 1000;
  while (true) {
    const { data: list, error } = await admin.auth.admin.listUsers({ page, perPage });
    if (error) break;
    for (const u of list.users) if (ids.has(u.id)) out.set(u.id, u.email ?? "");
    if (list.users.length < perPage) break;
    page++;
  }
  return out;
}

export default {
  fetch: withSupabase({ auth: "user" }, async (req, ctx) => {
    try {
      const { supabaseAdmin: admin, userClaims } = ctx;
      const roles: string[] = (userClaims!.appMetadata?.roles as string[]) ?? [];
      if (!roles.includes("ADMIN")) return Response.json({ error: "Forbidden" }, { status: 403 });

      const body = BodySchema.parse(await req.json());

      // ── list: giros PENDING enriquecidos + reembolsos fallidos ──
      if (body.action === "list") {
        const { data: payouts } = await admin
          .from("teacher_payouts")
          .select("id, teacher_id, class_id, gross_amount, commission_amount, net_amount, status, created_at")
          .eq("status", "PENDING")
          .order("created_at", { ascending: true });

        const classIds = [...new Set((payouts ?? []).map((p: any) => p.class_id).filter(Boolean))];
        const teacherIds = new Set<string>((payouts ?? []).map((p: any) => p.teacher_id));

        // Clases → título, tipo y sala (para llegar a la sede).
        const { data: classes } = classIds.length
          ? await admin.from("classes").select("id, title, tipo_clase, room_id").in("id", classIds)
          : { data: [] as any[] };
        const classById = new Map((classes ?? []).map((c: any) => [c.id, c]));

        const roomIds = [...new Set((classes ?? []).map((c: any) => c.room_id).filter(Boolean))];
        const { data: rooms } = roomIds.length
          ? await admin.from("rooms").select("id, venue_id").in("id", roomIds)
          : { data: [] as any[] };
        const roomById = new Map((rooms ?? []).map((r: any) => [r.id, r]));

        const venueIds = [...new Set((rooms ?? []).map((r: any) => r.venue_id).filter(Boolean))];
        const { data: venues } = venueIds.length
          ? await admin.from("venues").select("id, name").in("id", venueIds)
          : { data: [] as any[] };
        const venueById = new Map((venues ?? []).map((v: any) => [v.id, v]));

        // Profesores → nombre (profiles) + email (auth.users).
        const { data: profiles } = teacherIds.size
          ? await admin.from("profiles").select("id, full_name").in("id", [...teacherIds])
          : { data: [] as any[] };
        const nameById = new Map((profiles ?? []).map((p: any) => [p.id, p.full_name]));
        const emailMap = await emailsById(admin, teacherIds);

        const nombreSede = (classId: string | null) => {
          const cls = classId ? classById.get(classId) : null;
          const room = cls?.room_id ? roomById.get(cls.room_id) : null;
          return room?.venue_id ? (venueById.get(room.venue_id)?.name ?? null) : null;
        };

        const payoutsOut = (payouts ?? []).map((p: any) => ({
          id: p.id,
          teacherId: p.teacher_id,
          teacherName: nameById.get(p.teacher_id) ?? null,
          teacherEmail: emailMap.get(p.teacher_id) ?? null,
          classId: p.class_id,
          classTitle: p.class_id ? (classById.get(p.class_id)?.title ?? null) : null,
          tipoClase: p.class_id ? (classById.get(p.class_id)?.tipo_clase ?? null) : null,
          venueName: nombreSede(p.class_id),
          grossAmount: Number(p.gross_amount),
          commissionAmount: Number(p.commission_amount),
          netAmount: Number(p.net_amount),
          createdAt: p.created_at,
        }));

        // Reembolsos fallidos: payments FAILED + alumno/clase (vía enrollment) +
        // error (desde audit_logs 'payment.refund_failed').
        const { data: failed } = await admin
          .from("payments")
          .select("id, enrollment_id, amount, status, updated_at")
          .eq("status", "FAILED")
          .order("updated_at", { ascending: false });

        const enrIds = [...new Set((failed ?? []).map((p: any) => p.enrollment_id).filter(Boolean))];
        const { data: enrolls } = enrIds.length
          ? await admin.from("enrollments").select("id, student_id, class_id").in("id", enrIds)
          : { data: [] as any[] };
        const enrById = new Map((enrolls ?? []).map((e: any) => [e.id, e]));

        const failStudentIds = new Set<string>((enrolls ?? []).map((e: any) => e.student_id));
        const { data: failProfiles } = failStudentIds.size
          ? await admin.from("profiles").select("id, full_name").in("id", [...failStudentIds])
          : { data: [] as any[] };
        const failNameById = new Map((failProfiles ?? []).map((p: any) => [p.id, p.full_name]));

        const failClassIds = [...new Set((enrolls ?? []).map((e: any) => e.class_id).filter(Boolean))];
        const { data: failClasses } = failClassIds.length
          ? await admin.from("classes").select("id, title").in("id", failClassIds)
          : { data: [] as any[] };
        const failClassById = new Map((failClasses ?? []).map((c: any) => [c.id, c.title]));

        const failIds = (failed ?? []).map((p: any) => p.id);
        const { data: logs } = failIds.length
          ? await admin.from("audit_logs")
            .select("resource_id, metadata, created_at")
            .eq("action", "payment.refund_failed").eq("resource_type", "payment")
            .in("resource_id", failIds).order("created_at", { ascending: false })
          : { data: [] as any[] };
        const errorById = new Map<string, string>();
        for (const l of logs ?? []) {
          if (!errorById.has(l.resource_id)) errorById.set(l.resource_id, l.metadata?.error ?? "");
        }

        const failedRefunds = (failed ?? []).map((p: any) => {
          const enr = enrById.get(p.enrollment_id);
          return {
            paymentId: p.id,
            amount: Number(p.amount),
            studentId: enr?.student_id ?? null,
            studentName: enr?.student_id ? (failNameById.get(enr.student_id) ?? null) : null,
            classTitle: enr?.class_id ? (failClassById.get(enr.class_id) ?? null) : null,
            error: errorById.get(p.id) ?? null,
            updatedAt: p.updated_at,
          };
        });

        return Response.json({ payouts: payoutsOut, failedRefunds });
      }

      // ── finance: costo real MP vs. comisión cobrada (margen) del ciclo ──
      if (body.action === "finance") {
        const cutoffDay = await getCutoffDay(admin);
        const now = new Date();
        const month = body.month ??
          `${now.getUTCFullYear()}-${String(now.getUTCMonth() + 1).padStart(2, "0")}`;
        const { start, end } = cicloDeCorte(month, cutoffDay);

        // payment_sessions aprobadas dentro del ciclo: costo MP, neto e ingresos,
        // y comisión de arriendos (marketplace_fee del cart_snapshot).
        const { data: sessions } = await admin
          .from("payment_sessions")
          .select("cart_snapshot, mp_fee_amount, net_received_amount, processed_at")
          .eq("status", "APPROVED").gt("processed_at", start).lte("processed_at", end);

        let costoMp = 0, netoRecibido = 0, ingresos = 0, comisionArriendos = 0;
        for (const s of sessions ?? []) {
          costoMp += Number(s.mp_fee_amount ?? 0);
          netoRecibido += Number(s.net_received_amount ?? 0);
          const snap = s.cart_snapshot ?? {};
          if (snap.type === "ROOM_RESERVATION") {
            ingresos += Number(snap.amount ?? 0);
            comisionArriendos += Number(snap.marketplaceFee ?? 0);
          } else if (Array.isArray(snap.items)) {
            ingresos += snap.items.reduce((a: number, it: { price?: number }) => a + Number(it.price ?? 0), 0);
          }
        }

        // Comisión de clases: teacher_payouts creados en el ciclo.
        const { data: payoutRows } = await admin
          .from("teacher_payouts")
          .select("commission_amount, created_at")
          .gt("created_at", start).lte("created_at", end);
        const comisionClases = (payoutRows ?? [])
          .reduce((a: number, r: any) => a + Number(r.commission_amount ?? 0), 0);

        const comisionCobrada = comisionArriendos + comisionClases;
        return Response.json({
          month, cutoffDay, cicloInicio: start, cicloFin: end,
          costoMp, netoRecibido, ingresos,
          comisionArriendos, comisionClases, comisionCobrada,
          margen: comisionCobrada - costoMp,
        });
      }

      // ── markPayoutPaid: cierra un giro manualmente (PENDING → PAID) ──
      if (body.action === "markPayoutPaid") {
        if (!body.payoutId) return Response.json({ error: "payoutId requerido" }, { status: 400 });
        const { data: updated, error } = await admin
          .from("teacher_payouts")
          .update({ status: "PAID", paid_at: new Date().toISOString(), mp_reference: body.reference ?? null })
          .eq("id", body.payoutId).eq("status", "PENDING")
          .select("id");
        if (error) throw error;
        if (!updated || updated.length === 0) {
          return Response.json({ error: "El giro no está pendiente" }, { status: 409 });
        }
        await admin.from("audit_logs").insert({
          actor_id: userClaims!.id, action: "payout.paid_manual",
          resource_type: "teacher_payout", resource_id: body.payoutId,
          metadata: { reference: body.reference ?? null },
        });
        logInfo("payout_paid_manual", { payoutId: body.payoutId });
        return Response.json({ status: "ok" });
      }

      // ── retryRefund: reencola un reembolso fallido (FAILED → REFUND_PENDING) ──
      if (body.action === "retryRefund") {
        if (!body.paymentId) return Response.json({ error: "paymentId requerido" }, { status: 400 });
        const { data: updated, error } = await admin
          .from("payments").update({ status: "REFUND_PENDING" })
          .eq("id", body.paymentId).eq("status", "FAILED")
          .select("id");
        if (error) throw error;
        if (!updated || updated.length === 0) {
          return Response.json({ error: "El pago no está en estado fallido" }, { status: 409 });
        }
        await admin.from("audit_logs").insert({
          actor_id: userClaims!.id, action: "payment.refund_retry_manual",
          resource_type: "payment", resource_id: body.paymentId, metadata: {},
        });
        logInfo("refund_retry_manual", { paymentId: body.paymentId });
        return Response.json({ status: "ok" });
      }

      // ── markRefundResolved: marca resuelto un reembolso fallido (FAILED → REFUNDED) ──
      if (body.action === "markRefundResolved") {
        if (!body.paymentId) return Response.json({ error: "paymentId requerido" }, { status: 400 });
        const { data: updated, error } = await admin
          .from("payments").update({ status: "REFUNDED" })
          .eq("id", body.paymentId).eq("status", "FAILED")
          .select("id");
        if (error) throw error;
        if (!updated || updated.length === 0) {
          return Response.json({ error: "El pago no está en estado fallido" }, { status: 409 });
        }
        await admin.from("audit_logs").insert({
          actor_id: userClaims!.id, action: "payment.refund_resolved_manual",
          resource_type: "payment", resource_id: body.paymentId, metadata: {},
        });
        logInfo("refund_resolved_manual", { paymentId: body.paymentId });
        return Response.json({ status: "ok" });
      }

      return Response.json({ error: "Acción no soportada" }, { status: 400 });
    } catch (err) {
      logError("admin_payments_error", err);
      if (err instanceof z.ZodError) {
        return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400 });
      }
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
