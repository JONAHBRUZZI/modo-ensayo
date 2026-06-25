import { withSupabase } from "npm:@supabase/server";
import { logInfo, logError } from "../_shared/logger.ts";
import { exchangeCodeForTokens } from "../_shared/mpAuth.ts";

/**
 * Edge Function: mp-oauth-callback
 *
 * Recibe el redirect de MercadoPago tras la autorización del profesor.
 * Valida el `state`, intercambia el `code` por tokens y los persiste en
 * mp_seller_accounts. Redirige de vuelta al perfil del profesor en el frontend.
 *
 * verify_jwt = false (en config.toml): MercadoPago redirige aquí sin JWT.
 * La identidad del usuario se resuelve desde el `state` (mp_oauth_states).
 */
const STATE_TTL_MS = 10 * 60 * 1000; // 10 minutos

export default {
  fetch: withSupabase({ auth: "none" }, async (req, ctx) => {
    const admin = ctx.supabaseAdmin;
    const frontendUrl = Deno.env.get("APP_FRONTEND_URL") ?? "";
    const dest = (result: string) =>
      Response.redirect(`${frontendUrl}/profesor/perfil-profesional?mp=${result}`, 302);

    try {
      const url = new URL(req.url);
      const code = url.searchParams.get("code");
      const state = url.searchParams.get("state");
      if (!code || !state) return dest("error");

      // Validar y consumir el state
      const { data: st } = await admin
        .from("mp_oauth_states")
        .select("user_id, created_at")
        .eq("state", state)
        .single();
      await admin.from("mp_oauth_states").delete().eq("state", state);

      if (!st) {
        logError("mp_oauth_callback_invalid_state", new Error("state not found"));
        return dest("error");
      }
      if (Date.now() - new Date(st.created_at).getTime() > STATE_TTL_MS) {
        logError("mp_oauth_callback_expired_state", new Error("state expired"));
        return dest("expired");
      }

      const functionsBase = Deno.env.get("FUNCTIONS_URL") ??
        `${Deno.env.get("SUPABASE_URL")}/functions/v1`;
      const redirectUri = `${functionsBase}/mp-oauth-callback`;

      const tokens = await exchangeCodeForTokens(code, redirectUri);

      const now = new Date().toISOString();
      const { error: upErr } = await admin.from("mp_seller_accounts").upsert({
        user_id: st.user_id,
        mp_user_id: String(tokens.user_id),
        access_token: tokens.access_token,
        refresh_token: tokens.refresh_token,
        token_expires_at: new Date(Date.now() + tokens.expires_in * 1000).toISOString(),
        scope: tokens.scope ?? null,
        public_key: tokens.public_key ?? null,
        status: "CONNECTED",
        connected_at: now,
        updated_at: now,
      });
      if (upErr) {
        logError("mp_seller_upsert_failed", new Error(upErr.message), { upErr });
        return dest("error");
      }

      logInfo("mp_oauth_connected", { userId: st.user_id, mpUserId: tokens.user_id });
      return dest("connected");

    } catch (err) {
      logError("mp_oauth_callback_error", err);
      return dest("error");
    }
  }),
};
