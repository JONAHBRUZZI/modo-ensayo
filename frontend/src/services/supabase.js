import { createClient } from '@supabase/supabase-js'
import { decodeJwt } from '@/utils/jwt'

const SUPABASE_URL = import.meta.env.VITE_SUPABASE_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

if (!SUPABASE_URL || !SUPABASE_ANON_KEY) {
  // Falla rápido y claro: sin estas variables el frontend no puede hablar con Supabase.
  throw new Error(
    'Faltan VITE_SUPABASE_URL y/o VITE_SUPABASE_ANON_KEY. Copia frontend/.env.example a frontend/.env y complétalas.'
  )
}

// Singleton real: en dev (HMR) y con imports vía distintos specifiers
// (`./supabase`, `@/services/supabase`) este módulo puede evaluarse más de una
// vez y crear varios GoTrueClient bajo la misma storage key → "Multiple
// GoTrueClient instances detected" y queries colgadas en "Cargando...".
// Cacheando el cliente en globalThis garantizamos UNA sola instancia.
function createSupabaseClient() {
  return createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
    auth: {
      persistSession: true,
      autoRefreshToken: true,
      detectSessionInUrl: true,
      flowType: 'pkce'
      // Lock por defecto (navigator.locks): serializa el refresh de token entre
      // llamadas concurrentes. NO usamos pass-through: sin serialización, las
      // queries concurrentes en el mount disparaban refresh en paralelo →
      // race de rotación del refresh token → queries colgadas en "Cargando...".
      // El deadlock que motivó el pass-through lo causaban las instancias
      // múltiples (ya resuelto con el singleton de globalThis arriba).
    }
  })
}

export const supabase = globalThis.__modoEnsayoSupabase__ ??
  (globalThis.__modoEnsayoSupabase__ = createSupabaseClient())

const SNAKE_RE = /_([a-z0-9])/g
const toCamelKey = (k) => k.replace(SNAKE_RE, (_, c) => c.toUpperCase())

/**
 * Convierte recursivamente las claves snake_case → camelCase de objetos/arrays
 * devueltos por PostgREST/Edge Functions, para que las vistas (escritas para el
 * backend Spring en camelCase) sigan funcionando sin cambios.
 * No toca strings/números/Date ni las claves ya camelCase.
 * @template T
 * @param {T} value
 * @returns {T}
 */
export function camelize(value) {
  if (Array.isArray(value)) return value.map(camelize)
  if (value && typeof value === 'object' && !(value instanceof Date)) {
    const out = {}
    for (const [k, v] of Object.entries(value)) out[toCamelKey(k)] = camelize(v)
    return out
  }
  return value
}

/**
 * Id del usuario autenticado. Lee el claim `sub` del JWT en localStorage
 * (instantáneo, sin tocar getSession/locks); cae a getSession si hiciera falta.
 * @returns {Promise<string|null>}
 */
export async function currentUserId() {
  const token = localStorage.getItem('auth_token')
  const sub = token ? decodeJwt(token)?.sub : null
  if (sub) return sub
  const { data } = await supabase.auth.getSession()
  return data.session?.user?.id ?? null
}

/**
 * Invoca una Edge Function y normaliza el error al shape estilo-axios
 * (`error.response = { status, data }`) que las vistas ya esperan.
 * El JWT de Supabase se adjunta automáticamente por el cliente.
 *
 * @param {string} name  Nombre de la Edge Function.
 * @param {{ method?: string, body?: unknown }} [options]
 * @returns {Promise<unknown>} El cuerpo JSON de la respuesta.
 */
export async function invokeFunction(name, options = {}) {
  const { data, error } = await supabase.functions.invoke(name, {
    method: options.method || 'POST',
    body: options.body
  })

  if (error) {
    let status = 500
    let payload = { error: error.message }
    // FunctionsHttpError expone la Response original en error.context.
    if (error.context instanceof Response) {
      status = error.context.status
      payload = await error.context.json().catch(() => payload)
    }
    throw { response: { status, data: payload }, message: payload?.error || error.message }
  }

  return camelize(data)
}

export default supabase
