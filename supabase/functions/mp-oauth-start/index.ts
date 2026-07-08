import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";

/**
 * Edge Function: mp-oauth-start
 *
 * Inicia el flujo OAuth de MercadoPago para conectar la cuenta del profesor.
 * Genera un `state` anti-CSRF (persistido en mp_oauth_states ligado al usuario)
 * y devuelve la URL de autorización a la que el frontend debe redirigir.
 *
 * verify_jwt = true: solo un usuario autenticado puede iniciar la conexión.
 */
export default {
  fetch: withSupabase({ auth: "user" }, async (_req, ctx) => {
    try {
      const { supabaseAdmin: admin, userClaims } = ctx;
      const userId = userClaims!.id;

      const clientId = Deno.env.get("MERCADOPAGO_CLIENT_ID");
      if (!clientId) {
        logError("mp_oauth_start_no_client_id", new Error("MERCADOPAGO_CLIENT_ID missing"));
        return Response.json({ error: "OAuth no configurado" }, { status: 500 });
      }

      const functionsBase = Deno.env.get("FUNCTIONS_URL") ??
        `${Deno.env.get("SUPABASE_URL")}/functions/v1`;
      const redirectUri = `${functionsBase}/mp-oauth-callback`;

      const state = crypto.randomUUID();
      const { error: stErr } = await admin.from("mp_oauth_states").insert({
        state, user_id: userId,
      });
      if (stErr) {
        logError("mp_oauth_state_insert_failed", new Error(stErr.message), { stErr });
        return Response.json({ error: "No se pudo iniciar la conexión" }, { status: 500 });
      }

      const authUrl = "https://auth.mercadopago.cl/authorization?" + new URLSearchParams({
        client_id: clientId,
        response_type: "code",
        platform_id: "mp",
        state,
        redirect_uri: redirectUri,
      }).toString();

      logInfo("mp_oauth_start", { userId });
      return Response.json({ authUrl });

    } catch (err) {
      logError("mp_oauth_start_error", err);
      return Response.json({ error: "Internal error" }, { status: 500 });
    }
  }),
};
