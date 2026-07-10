import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";
import { z } from "npm:zod@3";

// Gestión de usuarios por parte del ADMIN. Requiere la Admin API
// (listar usuarios de auth, editar app_metadata.roles, ban/unban, borrar),
// imposible desde el cliente con publishable key — de ahí esta Edge Function.
const BodySchema = z.object({
  action: z.enum(["list", "assignRole", "revokeRole", "toggleUser", "deleteUser"]),
  userId: z.string().uuid().optional(),
  role: z.string().min(1).optional(),
});

export default {
  fetch: withSupabase({ auth: "user" }, async (req, ctx) => {
    try {
      const { supabaseAdmin: admin, userClaims } = ctx;
      const roles: string[] = (userClaims!.appMetadata?.roles as string[]) ?? [];
      if (!roles.includes("ADMIN")) return Response.json({ error: "Forbidden" }, { status: 403 });

      const body = BodySchema.parse(await req.json());

      if (body.action === "list") {
        // Colectar todos los usuarios paginando de a 1000 para no truncar silenciosamente
        const allAuthUsers: any[] = [];
        let page = 1;
        const perPage = 1000;
        while (true) {
          const { data: list, error } = await admin.auth.admin.listUsers({ page, perPage });
          if (error) throw error;
          allAuthUsers.push(...list.users);
          if (list.users.length < perPage) break;
          page++;
        }
        const ids = allAuthUsers.map((u) => u.id);
        const { data: profiles } = await admin.from("profiles")
          .select("id, full_name, social_name, phone, identidad_estado, tiene_sede_aprobada")
          .in("id", ids.length ? ids : ["00000000-0000-0000-0000-000000000000"]);
        const byId = new Map((profiles ?? []).map((p) => [p.id, p]));
        const users = allAuthUsers.map((u: any) => ({
          id: u.id,
          email: u.email,
          roles: (u.app_metadata?.roles as string[]) ?? [],
          enabled: !u.banned_until || new Date(u.banned_until) <= new Date(),
          createdAt: u.created_at,
          ...(byId.get(u.id) ?? {}),
        }));
        return Response.json(users);
      }

      if (!body.userId) return Response.json({ error: "userId requerido" }, { status: 400 });

      if (body.action === "deleteUser") {
        const { error } = await admin.auth.admin.deleteUser(body.userId);
        if (error) throw error;
        logInfo("user_deleted", { userId: body.userId });
        return Response.json({ status: "ok" });
      }

      if (body.action === "toggleUser") {
        const { data: target } = await admin.auth.admin.getUserById(body.userId);
        const isBanned = target.user?.banned_until && new Date(target.user.banned_until) > new Date();
        const { error } = await admin.auth.admin.updateUserById(body.userId, {
          ban_duration: isBanned ? "none" : "876000h",
        });
        if (error) throw error;
        logInfo("user_toggled", { userId: body.userId, banned: !isBanned });
        return Response.json({ status: "ok", enabled: isBanned });
      }

      // assignRole / revokeRole
      if (!body.role) return Response.json({ error: "role requerido" }, { status: 400 });
      const { data: target } = await admin.auth.admin.getUserById(body.userId);
      const current: string[] = (target.user?.app_metadata?.roles as string[]) ?? [];
      const next = body.action === "assignRole"
        ? Array.from(new Set([...current, body.role]))
        : current.filter((r) => r !== body.role);
      const { error } = await admin.auth.admin.updateUserById(body.userId, {
        app_metadata: { roles: next },
      });
      if (error) throw error;
      logInfo("user_roles_updated", { userId: body.userId, roles: next });
      return Response.json({ status: "ok", roles: next });

    } catch (err) {
      logError("admin_users_error", err);
      if (err instanceof z.ZodError) return Response.json({ error: "Invalid input", details: err.flatten() }, { status: 400 });
      // Se expone el mensaje real (función solo para ADMIN) para que el panel
      // muestre la causa concreta en vez de un "Error interno" opaco.
      const message = err instanceof Error ? err.message : "Error interno";
      return Response.json({ error: message, message }, { status: 500 });
    }
  }),
};
