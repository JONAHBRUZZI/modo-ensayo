import { withSupabase } from "npm:@supabase/server";
import { logError } from "../_shared/logger.ts";

// Métricas de comportamiento desde Google Analytics 4 (Data API), para el
// dashboard admin. Solo ADMIN. Los secretos NO se exponen al frontend:
//   GA_PROPERTY_ID       → id numérico de la propiedad GA4 (ej. 123456789)
//   GA_SERVICE_ACCOUNT   → JSON de la cuenta de servicio (con acceso Viewer a GA4)
// Si faltan, responde { configured:false } y el dashboard muestra un aviso.

// deno-lint-ignore no-explicit-any
type Any = any;

function base64url(input: ArrayBuffer | string): string {
  const bytes = typeof input === "string" ? new TextEncoder().encode(input) : new Uint8Array(input);
  let bin = "";
  for (const b of bytes) bin += String.fromCharCode(b);
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function pemToPkcs8(pem: string): ArrayBuffer {
  const b64 = pem.replace(/-----BEGIN PRIVATE KEY-----/, "")
    .replace(/-----END PRIVATE KEY-----/, "").replace(/\s+/g, "");
  const bin = atob(b64);
  const buf = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) buf[i] = bin.charCodeAt(i);
  return buf.buffer;
}

// OAuth2 service-account flow: firma un JWT RS256 y lo canjea por un access_token.
async function getAccessToken(sa: Any): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  const header = { alg: "RS256", typ: "JWT" };
  const claims = {
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/analytics.readonly",
    aud: sa.token_uri,
    exp: now + 3600,
    iat: now,
  };
  const unsigned = `${base64url(JSON.stringify(header))}.${base64url(JSON.stringify(claims))}`;
  const key = await crypto.subtle.importKey(
    "pkcs8", pemToPkcs8(sa.private_key),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"],
  );
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(unsigned));
  const jwt = `${unsigned}.${base64url(sig)}`;

  const resp = await fetch(sa.token_uri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`,
  });
  const data = await resp.json();
  if (!data.access_token) throw new Error("token exchange failed: " + JSON.stringify(data));
  return data.access_token as string;
}

async function gaFetch(propertyId: string, token: string, method: string, body: Any): Promise<Any> {
  const resp = await fetch(
    `https://analyticsdata.googleapis.com/v1beta/properties/${propertyId}:${method}`,
    {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify(body),
    },
  );
  return await resp.json();
}

export default {
  fetch: withSupabase({ auth: "user" }, async (_req, ctx) => {
    try {
      const { userClaims } = ctx;
      const roles: string[] = (userClaims!.appMetadata?.roles as string[]) ?? [];
      if (!roles.includes("ADMIN")) return Response.json({ error: "Forbidden" }, { status: 403 });

      const propertyId = Deno.env.get("GA_PROPERTY_ID");
      const saRaw = Deno.env.get("GA_SERVICE_ACCOUNT");
      if (!propertyId || !saRaw) return Response.json({ configured: false });

      const sa = JSON.parse(saRaw);
      const token = await getAccessToken(sa);

      const [rt, rep, top] = await Promise.all([
        // Usuarios activos ahora mismo (últimos 30 min).
        gaFetch(propertyId, token, "runRealtimeReport", { metrics: [{ name: "activeUsers" }] }),
        // Últimos 7 días: usuarios, sesiones y vistas.
        gaFetch(propertyId, token, "runReport", {
          dateRanges: [{ startDate: "7daysAgo", endDate: "today" }],
          metrics: [{ name: "activeUsers" }, { name: "sessions" }, { name: "screenPageViews" }],
        }),
        // Top 5 páginas por vistas (7 días).
        gaFetch(propertyId, token, "runReport", {
          dateRanges: [{ startDate: "7daysAgo", endDate: "today" }],
          dimensions: [{ name: "pagePath" }],
          metrics: [{ name: "screenPageViews" }],
          orderBys: [{ metric: { metricName: "screenPageViews" }, desc: true }],
          limit: 5,
        }),
      ]);

      const m = rep.rows?.[0]?.metricValues ?? [];
      return Response.json({
        configured: true,
        activeNow: Number(rt.rows?.[0]?.metricValues?.[0]?.value ?? 0),
        usuarios7d: Number(m[0]?.value ?? 0),
        sesiones7d: Number(m[1]?.value ?? 0),
        vistas7d: Number(m[2]?.value ?? 0),
        topPages: (top.rows ?? []).map((r: Any) => ({
          path: r.dimensionValues?.[0]?.value ?? "",
          views: Number(r.metricValues?.[0]?.value ?? 0),
        })),
      });
    } catch (err) {
      logError("ga_metrics_error", err);
      return Response.json({ configured: false, error: "GA no disponible" }, { status: 200 });
    }
  }),
};
