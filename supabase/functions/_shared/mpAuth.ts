// ============================================================
// Helpers OAuth de MercadoPago (Marketplace).
// Intercambio de code→tokens, refresco de token, y obtención de un
// access_token válido del vendedor (profesor) para liquidarle pagos.
//
// Requiere los secrets MERCADOPAGO_CLIENT_ID y MERCADOPAGO_CLIENT_SECRET
// (credenciales de la aplicación marketplace en el panel de MercadoPago).
// ============================================================

const MP_TOKEN_URL = "https://api.mercadopago.com/oauth/token";

export interface MpTokenResponse {
  access_token: string;
  refresh_token: string;
  user_id: number | string;
  public_key?: string;
  expires_in: number; // segundos
  scope?: string;
  token_type?: string;
}

function clientCreds(): { clientId: string; clientSecret: string } {
  const clientId = Deno.env.get("MERCADOPAGO_CLIENT_ID");
  const clientSecret = Deno.env.get("MERCADOPAGO_CLIENT_SECRET");
  if (!clientId || !clientSecret) {
    throw new Error("MERCADOPAGO_CLIENT_ID / MERCADOPAGO_CLIENT_SECRET no configurados");
  }
  return { clientId, clientSecret };
}

/** Intercambia el `code` del callback OAuth por tokens del vendedor. */
export async function exchangeCodeForTokens(
  code: string,
  redirectUri: string,
): Promise<MpTokenResponse> {
  const { clientId, clientSecret } = clientCreds();
  const resp = await fetch(MP_TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify({
      client_id: clientId,
      client_secret: clientSecret,
      grant_type: "authorization_code",
      code,
      redirect_uri: redirectUri,
    }),
  });
  const data = await resp.json();
  if (!resp.ok || !data.access_token) {
    throw new Error(`MP token exchange failed (${resp.status}): ${JSON.stringify(data)}`);
  }
  return data as MpTokenResponse;
}

/** Refresca el access_token de un vendedor usando su refresh_token. */
export async function refreshToken(refreshTokenValue: string): Promise<MpTokenResponse> {
  const { clientId, clientSecret } = clientCreds();
  const resp = await fetch(MP_TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify({
      client_id: clientId,
      client_secret: clientSecret,
      grant_type: "refresh_token",
      refresh_token: refreshTokenValue,
    }),
  });
  const data = await resp.json();
  if (!resp.ok || !data.access_token) {
    throw new Error(`MP token refresh failed (${resp.status}): ${JSON.stringify(data)}`);
  }
  return data as MpTokenResponse;
}

/**
 * Devuelve un access_token válido del vendedor (profesor), refrescándolo y
 * persistiéndolo si está vencido o a punto de vencer. Lanza si no está conectado.
 * `admin` = cliente service_role (ctx.supabaseAdmin).
 */
export async function getValidSellerToken(
  admin: any,
  teacherId: string,
): Promise<{ accessToken: string; mpUserId: string }> {
  const { data: acc, error } = await admin
    .from("mp_seller_accounts")
    .select("access_token, refresh_token, token_expires_at, mp_user_id, status")
    .eq("user_id", teacherId)
    .single();

  if (error || !acc || acc.status !== "CONNECTED") {
    throw new Error(`Profesor ${teacherId} no tiene MercadoPago conectado`);
  }

  const expiresAt = acc.token_expires_at ? new Date(acc.token_expires_at).getTime() : 0;
  const skewMs = 5 * 60 * 1000; // refrescar 5 min antes de expirar
  if (Date.now() < expiresAt - skewMs) {
    return { accessToken: acc.access_token, mpUserId: String(acc.mp_user_id) };
  }

  const refreshed = await refreshToken(acc.refresh_token);
  await admin
    .from("mp_seller_accounts")
    .update({
      access_token: refreshed.access_token,
      refresh_token: refreshed.refresh_token,
      token_expires_at: new Date(Date.now() + refreshed.expires_in * 1000).toISOString(),
      updated_at: new Date().toISOString(),
    })
    .eq("user_id", teacherId);

  return { accessToken: refreshed.access_token, mpUserId: String(acc.mp_user_id) };
}
