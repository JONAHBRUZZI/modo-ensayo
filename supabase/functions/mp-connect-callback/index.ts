import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";

// Callback del OAuth de MercadoPago Connect. MercadoPago redirige aquí (GET) con
// ?code=...&state=... tras autorizar el gestor de sede. Validamos el state,
// canjeamos el code por los tokens del vendedor y los guardamos en
// mp_seller_accounts. Es un redirect del navegador: verify_jwt = false.
export default {
  fetch: withSupabase({ auth: "none" }, async (req, ctx) => {
    const frontendUrl = Deno.env.get("APP_FRONTEND_URL") ?? "";
    const redirectOk = `${frontendUrl}/sede/configuracion?mp=ok`;
    const redirectErr = `${frontendUrl}/sede/configuracion?mp=error`;

    try {
      const admin = ctx.supabaseAdmin;
      const url = new URL(req.url);
      const code = url.searchParams.get("code");
      const state = url.searchParams.get("state");
      if (!code || !state) {
        return Response.redirect(redirectErr, 302);
      }

      // Valida el state (anti-CSRF) y resuelve el usuario.
      const { data: stateRow, error: stErr } = await admin
        .from("mp_oauth_states").select("user_id").eq("state", state).single();
      if (stErr || !stateRow) {
        logError("mp_callback_invalid_state", new Error("state no encontrado"));
        return Response.redirect(redirectErr, 302);
      }
      const userId = stateRow.user_id as string;

      const clientId = Deno.env.get("MERCADOPAGO_CLIENT_ID")!;
      const clientSecret = Deno.env.get("MERCADOPAGO_CLIENT_SECRET")!;
      const functionsBase = Deno.env.get("FUNCTIONS_URL") ??
        `${Deno.env.get("SUPABASE_URL")}/functions/v1`;
      const redirectUri = `${functionsBase}/mp-connect-callback`;

      // Canje del code por tokens del vendedor.
      // MercadoPago OAuth requiere application/x-www-form-urlencoded.
      const tokenResp = await fetch("https://api.mercadopago.com/oauth/token", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded", "Accept": "application/json" },
        body: new URLSearchParams({
          client_id: clientId,
          client_secret: clientSecret,
          code,
          grant_type: "authorization_code",
          redirect_uri: redirectUri,
        }).toString(),
      });
      const token = await tokenResp.json();
      if (!tokenResp.ok || !token.access_token) {
        logError("mp_token_exchange_failed", new Error(tokenResp.statusText), { mpError: token });
        await admin.from("mp_oauth_states").delete().eq("state", state);
        return Response.redirect(redirectErr, 302);
      }

      const expiresAt = token.expires_in
        ? new Date(Date.now() + Number(token.expires_in) * 1000).toISOString()
        : null;

      // Upsert de la cuenta del vendedor.
      const { error: upErr } = await admin.from("mp_seller_accounts").upsert({
        user_id: userId,
        mp_user_id: token.user_id ? String(token.user_id) : null,
        access_token: token.access_token,
        refresh_token: token.refresh_token ?? null,
        token_expires_at: expiresAt,
        scope: token.scope ?? null,
        public_key: token.public_key ?? null,
        status: "CONNECTED",
        connected_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
      }, { onConflict: "user_id" });
      if (upErr) throw upErr;

      // Limpia el state usado.
      await admin.from("mp_oauth_states").delete().eq("state", state);

      logInfo("mp_connected", { userId, mpUserId: token.user_id });
      return Response.redirect(redirectOk, 302);

    } catch (err) {
      logError("mp_callback_error", err);
      return Response.redirect(redirectErr, 302);
    }
  }),
};
