import type {
  PayoutProvider,
  SendPayoutArgs,
  SendPayoutResult,
} from "./payoutProvider.ts";

// ============================================================
// FintocPayoutProvider — desembolso real a profesores vía Fintoc
// (open banking chileno, transferencia directa a cuenta bancaria).
// ------------------------------------------------------------
// ⚠️ SIN PROBAR CONTRA LA API REAL (16-ago-2026). Implementado contra la
// documentación pública de Fintoc (docs.fintoc.com) y el comportamiento
// del SDK oficial de Python (github.com/fintoc-com/fintoc-python), sin
// poder ejecutar una llamada real en esta sesión. Antes de usar en
// producción:
//   1. Generar el par de llaves JWS (aparte de la Secret/Public Key de la
//      cuenta): `openssl genrsa -out private_key.pem 2048` y
//      `openssl rsa -in private_key.pem -outform PEM -pubout -out public_key.pem`.
//   2. Subir `public_key.pem` en dashboard.fintoc.com → API Keys → JWS
//      Public Keys.
//   3. `supabase secrets set FINTOC_SECRET_KEY=... FINTOC_JWS_PRIVATE_KEY="$(cat private_key.pem)"`.
//   4. Probar con un payout de monto mínimo en modo test antes de confiar
//      en el flujo (mismo criterio que R19).
//
// Gaps conocidos, no resueltos en este pase:
//   - `BANK_TO_INSTITUTION`: mapeo de nombre de banco (texto libre en
//     `refund_methods.bank`) a `institution_id` de Fintoc. Solo cubre los
//     bancos chilenos más comunes; si no matchea, el envío falla con un
//     error claro en vez de adivinar. Alternativa mejor a futuro: cambiar
//     el formulario de `RefundMethodPage.vue` a un selector que consuma
//     `GET /v2/institutions` en vez de texto libre.
//   - `ACCOUNT_TYPE_MAP`: `refund_methods.account_type` es texto libre
//     (CORRIENTE/VISTA/AHORRO); se mapea a los valores que documenta
//     Fintoc para Chile (`checking`/`sight`) con una heurística razonable,
//     sin confirmar contra la API real.
//   - El esquema exacto del header `Authorization` (con o sin prefijo
//     `Bearer`) no se pudo confirmar con certeza en la documentación
//     pública — implementado como valor directo (patrón más común en
//     fintechs latinoamericanas); ajustar si la API rechaza con 401.
// ============================================================

const FINTOC_API_BASE = "https://api.fintoc.com/v2";

const BANK_TO_INSTITUTION: Record<string, string> = {
  "BANCO ESTADO": "cl_banco_estado",
  "BANCOESTADO": "cl_banco_estado",
  "BANCO DE CHILE": "cl_banco_de_chile",
  "BANCO BCI": "cl_banco_bci",
  "BCI": "cl_banco_bci",
  "BANCO BICE": "cl_banco_bice",
  "BANCO FALABELLA": "cl_banco_falabella",
  "BANCO ITAU": "cl_banco_itau",
  "BANCO ITAÚ": "cl_banco_itau",
  "BANCO SANTANDER": "cl_banco_santander",
  "SANTANDER": "cl_banco_santander",
  "SCOTIABANK": "cl_scotiabank",
  "BANCO SECURITY": "cl_banco_security",
  "BANCO CONSORCIO": "cl_banco_consorcio",
};

const ACCOUNT_TYPE_MAP: Record<string, string> = {
  CORRIENTE: "checking",
  VISTA: "sight",
  AHORRO: "sight",
};

function resolveInstitutionId(bankName: string): string {
  const key = bankName.trim().toUpperCase();
  const id = BANK_TO_INSTITUTION[key];
  if (!id) {
    throw new Error(
      `Banco "${bankName}" no reconocido en el mapeo de instituciones Fintoc. ` +
        `Agregar a BANK_TO_INSTITUTION o migrar el formulario a un selector de GET /v2/institutions.`,
    );
  }
  return id;
}

function resolveAccountType(accountType: string): string {
  return ACCOUNT_TYPE_MAP[accountType.trim().toUpperCase()] ?? "checking";
}

function base64url(input: string | Uint8Array): string {
  const bytes = typeof input === "string" ? new TextEncoder().encode(input) : input;
  let binary = "";
  for (const b of bytes) binary += String.fromCharCode(b);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

async function importPrivateKey(pem: string): Promise<CryptoKey> {
  const clean = pem
    .replace(/-----BEGIN PRIVATE KEY-----/, "")
    .replace(/-----END PRIVATE KEY-----/, "")
    .replace(/\s+/g, "");
  const der = Uint8Array.from(atob(clean), (c) => c.charCodeAt(0));
  return crypto.subtle.importKey(
    "pkcs8",
    der,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
}

/** Firma JWS (RS256) de un request de transferencia, según docs.fintoc.com/docs/setting-up-jws-keys. */
async function signJws(privateKeyPem: string, rawBody: string): Promise<string> {
  const nonce = crypto.randomUUID().replace(/-/g, "");
  const ts = Math.floor(Date.now() / 1000);
  const protectedHeader = { alg: "RS256", nonce, ts, crit: ["ts", "nonce"] };

  const encHeader = base64url(JSON.stringify(protectedHeader));
  const encBody = base64url(rawBody);
  const signingInput = `${encHeader}.${encBody}`;

  const key = await importPrivateKey(privateKeyPem);
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(signingInput),
  );

  return `${encHeader}.${encBody}.${base64url(new Uint8Array(signature))}`;
}

export class FintocPayoutProvider implements PayoutProvider {
  readonly name = "FINTOC";

  async sendPayout(args: SendPayoutArgs): Promise<SendPayoutResult> {
    const secretKey = Deno.env.get("FINTOC_SECRET_KEY");
    const jwsPrivateKey = Deno.env.get("FINTOC_JWS_PRIVATE_KEY");
    if (!secretKey || !jwsPrivateKey) {
      throw new Error("FINTOC_SECRET_KEY / FINTOC_JWS_PRIVATE_KEY no configurados");
    }

    const body = {
      amount: Math.round(args.amount),
      currency: "CLP",
      reference_id: args.externalReference,
      comment: "Honorario ModoEnsayo".slice(0, 40),
      counterparty: {
        holder_id: args.recipient.holderRut.replace(/[^0-9kK]/g, "").toUpperCase(),
        holder_name: args.recipient.holderName,
        account_number: args.recipient.accountNumber,
        account_type: resolveAccountType(args.recipient.accountType),
        institution_id: resolveInstitutionId(args.recipient.bank),
      },
    };
    const rawBody = JSON.stringify(body);
    const jws = await signJws(jwsPrivateKey, rawBody);

    const resp = await fetch(`${FINTOC_API_BASE}/transfers`, {
      method: "POST",
      headers: {
        Authorization: secretKey,
        "Content-Type": "application/json",
        "Fintoc-JWS-Signature": jws,
      },
      body: rawBody,
    });

    if (!resp.ok) {
      const detail = await resp.text();
      throw new Error(`Fintoc transfer failed (${resp.status}): ${detail}`);
    }

    const transfer = await resp.json();
    return {
      providerReference: transfer.id,
      status: transfer.status === "succeeded" ? "PAID" : "PENDING",
    };
  }
}
