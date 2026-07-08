import { withSupabase } from "npm:@supabase/server";
import { logError } from "../_shared/logger.ts";

// Métricas de rendimiento del admin, globales y desglosadas por sede. Requiere
// service role: cruza clases/salas/sedes, inscripciones, asistencias y sesiones
// de pago (attendances no tiene policy de lectura para admin). Solo ADMIN.
//
//   M1 Ocupación     = inscripciones ACTIVE / cupos ofrecidos (clases abiertas)
//   M2 Conversión    = sesiones aprobadas / sesiones iniciadas
//   M3 Asistencia    = asistencias presentes / asistencias marcadas
//   M5 Pagos exitosos = aprobadas / (aprobadas + fallidas)   [excluye pendientes]
//   M4 Disponibilidad = uptime externo (no se calcula acá)

// deno-lint-ignore no-explicit-any
type Any = any;

const pct = (num: number, den: number): number | null =>
  den > 0 ? Math.round((num / den) * 1000) / 10 : null;

export default {
  fetch: withSupabase({ auth: "user" }, async (_req, ctx) => {
    try {
      const { supabaseAdmin: admin, userClaims } = ctx;
      const roles: string[] = (userClaims!.appMetadata?.roles as string[]) ?? [];
      if (!roles.includes("ADMIN")) return Response.json({ error: "Forbidden" }, { status: 403 });

      const [venuesR, classesR, roomsR, enrollsR, attsR, sessionsR] = await Promise.all([
        admin.from("venues").select("id, name"),
        admin.from("classes").select("id, capacity, status, room_id").limit(20000),
        admin.from("rooms").select("id, venue_id, capacity").limit(20000),
        admin.from("enrollments").select("class_id, status").limit(50000),
        admin.from("attendances").select("class_id, present").limit(50000),
        admin.from("payment_sessions").select("cart_snapshot, status").limit(50000),
      ]);

      const venues: Any[] = venuesR.data ?? [];
      const roomToVenue = new Map<string, string>((roomsR.data ?? []).map((r: Any) => [r.id, r.venue_id]));
      const roomCapById = new Map<string, number>((roomsR.data ?? []).map((r: Any) => [r.id, Number(r.capacity ?? 0)]));
      const classes: Any[] = classesR.data ?? [];
      const classToVenue = new Map<string, string | null>();
      const classInfo = new Map<string, Any>();
      for (const c of classes) {
        const venueId = c.room_id ? (roomToVenue.get(c.room_id) ?? null) : null;
        classToVenue.set(c.id, venueId);
        classInfo.set(c.id, c);
      }

      // Contadores por sede + global.
      type Acc = {
        ocupActive: number; ocupCap: number;
        asistPres: number; asistTot: number;
        convApp: number; convTot: number;
        pagoApp: number; pagoFail: number;
      };
      const nuevo = (): Acc => ({
        ocupActive: 0, ocupCap: 0, asistPres: 0, asistTot: 0,
        convApp: 0, convTot: 0, pagoApp: 0, pagoFail: 0,
      });
      const global = nuevo();
      const porSede = new Map<string, Acc>();
      const accSede = (venueId: string | null): Acc | null => {
        if (!venueId) return null;
        if (!porSede.has(venueId)) porSede.set(venueId, nuevo());
        return porSede.get(venueId)!;
      };

      // ── M1 Ocupación: inscripciones activas vs CAPACIDAD DE LA SALA ──
      // Por cada clase abierta (no borrador/cancelada) con sala, el denominador es
      // el aforo físico de la sala (rooms.capacity), no el cupo de la clase.
      const activasPorClase = new Map<string, number>();
      for (const e of enrollsR.data ?? []) {
        if (e.status !== "ACTIVE") continue;
        activasPorClase.set(e.class_id, (activasPorClase.get(e.class_id) ?? 0) + 1);
      }
      const ABIERTAS = new Set(["PUBLISHED", "IN_PROGRESS", "FULL", "COMPLETED", "SUSPENDED", "POR_VALIDAR"]);
      for (const c of classes) {
        if (!ABIERTAS.has(c.status)) continue;
        const salaCap = c.room_id ? (roomCapById.get(c.room_id) ?? 0) : 0;
        if (!(salaCap > 0)) continue; // sin sala/aforo no se puede medir ocupación
        const activos = activasPorClase.get(c.id) ?? 0;
        global.ocupActive += activos; global.ocupCap += salaCap;
        const s = accSede(classToVenue.get(c.id) ?? null);
        if (s) { s.ocupActive += activos; s.ocupCap += salaCap; }
      }

      // ── M3 Asistencia: presentes vs total de marcas ──
      for (const a of attsR.data ?? []) {
        global.asistTot += 1; if (a.present) global.asistPres += 1;
        const s = accSede(classToVenue.get(a.class_id) ?? null);
        if (s) { s.asistTot += 1; if (a.present) s.asistPres += 1; }
      }

      // ── M2 Conversión + M5 Pagos: por sesión, atribuida a la(s) sede(s) que toca ──
      for (const ps of sessionsR.data ?? []) {
        const snap = ps.cart_snapshot ?? {};
        const status = ps.status;
        // Sedes que toca esta sesión.
        const sedes = new Set<string>();
        if (snap.type === "ROOM_RESERVATION") {
          if (snap.venueId) sedes.add(snap.venueId);
        } else if (Array.isArray(snap.items)) {
          for (const it of snap.items) {
            const v = it.classId ? classToVenue.get(it.classId) : null;
            if (v) sedes.add(v);
          }
        }

        // Global.
        global.convTot += 1;
        if (status === "APPROVED") { global.convApp += 1; global.pagoApp += 1; }
        else if (status === "FAILED") { global.pagoFail += 1; }

        // Por sede.
        for (const v of sedes) {
          const s = accSede(v)!;
          s.convTot += 1;
          if (status === "APPROVED") { s.convApp += 1; s.pagoApp += 1; }
          else if (status === "FAILED") { s.pagoFail += 1; }
        }
      }

      const metrics = (a: Acc) => ({
        ocupacion: pct(a.ocupActive, a.ocupCap),
        conversion: pct(a.convApp, a.convTot),
        asistencia: pct(a.asistPres, a.asistTot),
        pagosExitosos: pct(a.pagoApp, a.pagoApp + a.pagoFail),
      });

      const venueName = new Map<string, string>(venues.map((v: Any) => [v.id, v.name]));
      const porSedeOut = [...porSede.entries()]
        .map(([venueId, acc]) => ({
          venueId,
          venueName: venueName.get(venueId) ?? "Sede",
          ...metrics(acc),
        }))
        .sort((x, y) => x.venueName.localeCompare(y.venueName));

      // ── M4 Disponibilidad: latidos registrados vs esperados (ventana 7 días) ──
      // Se toma como inicio el primer latido (si el monitoreo empezó hace poco) para
      // no penalizar el ramp-up; esperados = minutos de la ventana / 5.
      const sieteDias = 7 * 24 * 60 * 60 * 1000;
      const [{ data: primer }, { count: observados }] = await Promise.all([
        admin.from("uptime_checks").select("checked_at").order("checked_at", { ascending: true }).limit(1).maybeSingle(),
        admin.from("uptime_checks").select("*", { count: "exact", head: true })
          .gte("checked_at", new Date(Date.now() - sieteDias).toISOString()),
      ]);
      let disponibilidad: number | null = null;
      if (primer?.checked_at) {
        const inicio = Math.max(new Date(primer.checked_at).getTime(), Date.now() - sieteDias);
        const esperados = Math.max(1, Math.floor((Date.now() - inicio) / (5 * 60 * 1000)));
        disponibilidad = Math.min(100, Math.round(((observados ?? 0) / esperados) * 1000) / 10);
      }

      return Response.json({
        global: { ...metrics(global), disponibilidad },
        porSede: porSedeOut,
      });
    } catch (err) {
      logError("admin_metrics_error", err);
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
