import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";

// Inicia el flujo OAuth de MercadoPago Connect para que un gestor de sede vincule
// su cuenta de MercadoPago (vendedor del marketplace). Genera un `state` anti-CSRF,
// lo guarda en mp_oauth_states y devuelve la URL de autorización de MercadoPago.
export default {
  fetch: withSupabase({ auth: "user" }, async (_req, ctx) => {
    try {
      const { supabaseAdmin: admin, userClaims } = ctx;
      const userId = userClaims!.id;
      const roles: string[] = (userClaims!.appMetadata?.roles as string[]) ?? [];
      if (!roles.includes("VENUE_ADMIN") && !roles.includes("ADMIN")) {
        return Response.json({ error: "Solo un gestor de sede puede conectar MercadoPago" }, { status: 403 });
      }

      const clientId = Deno.env.get("MERCADOPAGO_CLIENT_ID");
      if (!clientId) {
        logError("mp_connect_start_no_client_id", new Error("MERCADOPAGO_CLIENT_ID no configurado"));
        return Response.json({ error: "MercadoPago no está configurado en el servidor" }, { status: 500 });
      }
      const functionsBase = Deno.env.get("FUNCTIONS_URL") ??
        `${Deno.env.get("SUPABASE_URL")}/functions/v1`;
      const redirectUri = `${functionsBase}/mp-connect-callback`;

      // State anti-CSRF: se valida en el callback.
      const state = crypto.randomUUID();
      // Limpia states viejos del usuario y crea el nuevo.
      await admin.from("mp_oauth_states").delete().eq("user_id", userId);
      const { error: stErr } = await admin.from("mp_oauth_states")
        .insert({ state, user_id: userId });
      if (stErr) throw stErr;

      const authUrl = "https://auth.mercadopago.com/authorization?" + new URLSearchParams({
        client_id: clientId,
        response_type: "code",
        platform_id: "mp",
        state,
        redirect_uri: redirectUri,
      }).toString();

      logInfo("mp_connect_started", { userId });
      return Response.json({ authUrl });

    } catch (err) {
      logError("mp_connect_start_error", err);
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
