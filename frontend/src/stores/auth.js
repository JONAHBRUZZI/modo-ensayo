import { ref, computed } from 'vue'
import { supabase } from '@/services/supabase'
import { decodeJwt } from '@/utils/jwt'

// ============================================================================
// Auth store — respaldado por Supabase Auth.
// Mantiene el mismo contrato de localStorage que la versión Spring Boot
// (`auth_token` = access_token JWT, `auth_user` = objeto usuario, `modoActual`)
// para que el router y el resto de la app sigan funcionando sin cambios.
// ============================================================================

const DEFAULT_ATRIBUTOS = {
  identidadValidada: false,
  identidadEnRevision: false,
  identidadRechazada: false,
  tieneReservasActivas: false,
  tieneAsignacionesActivas: false,
  reservasSinClase: false,
  reservasSinClaseCount: 0,
  perfilProfesionalCompleto: false,
  hasRoleTeacher: false,
  tieneSedeAprobada: false,
  estadoSolicitudSede: null,
  motivoRechazoSede: null,
  estadoProfesor: 'INACTIVO'
}

class AuthStore {
  constructor() {
    this.token = ref(localStorage.getItem('auth_token') || null)
    this.user = ref(parseStoredUser())
    this.modoActual = ref(localStorage.getItem('modoActual') || 'alumno')
  }

  setToken(token, refreshToken = null) {
    this.token.value = token
    if (token) localStorage.setItem('auth_token', token)
    else localStorage.removeItem('auth_token')
    if (refreshToken) localStorage.setItem('auth_refresh_token', refreshToken)
  }

  setUser(user) {
    this.user.value = user
    if (user) localStorage.setItem('auth_user', JSON.stringify(user))
    else localStorage.removeItem('auth_user')
  }

  getToken() {
    return this.token.value
  }

  isAuthenticated() {
    if (!this.token.value) return false
    const payload = decodeJwt(this.token.value)
    if (!payload) return false
    return payload.exp * 1000 > Date.now()
  }

  clearAuth() {
    this.token.value = null
    this.user.value = null
    localStorage.removeItem('auth_token')
    localStorage.removeItem('auth_user')
    localStorage.removeItem('auth_refresh_token')
  }
}

function parseStoredUser() {
  try {
    const raw = localStorage.getItem('auth_user')
    if (!raw) return null
    const u = JSON.parse(raw)
    return {
      id: u.id,
      email: u.email,
      fullName: u.fullName || u.full_name,
      socialName: u.socialName || u.social_name,
      phone: u.phone,
      roles: u.roles || ['USER'],
      enabled: u.enabled !== false,
      atributosActivos: { ...DEFAULT_ATRIBUTOS, ...(u.atributosActivos || {}) }
    }
  } catch {
    return null
  }
}

function rolesFromSession(session) {
  const roles = session?.user?.app_metadata?.roles
  return Array.isArray(roles) && roles.length > 0 ? roles : ['USER']
}

const store = new AuthStore()

/**
 * Construye el objeto de usuario a partir de la sesión Supabase:
 * roles desde app_metadata (JWT), datos desde `profiles`, derivados desde la RPC.
 * @param {import('@supabase/supabase-js').Session} session
 */
async function buildUserFromSession(session) {
  if (!session?.user) return null
  const authUser = session.user
  const roles = rolesFromSession(session)

  const [{ data: profile }, { data: attrs }] = await Promise.all([
    supabase.from('profiles').select('full_name, social_name, phone').eq('id', authUser.id).maybeSingle(),
    supabase.rpc('get_my_attributes')
  ])

  return {
    id: authUser.id,
    email: authUser.email,
    fullName: profile?.full_name || authUser.user_metadata?.full_name || authUser.email,
    socialName: profile?.social_name || null,
    phone: profile?.phone || null,
    roles,
    enabled: true,
    atributosActivos: mapAtributos(attrs)
  }
}

/** Normaliza la respuesta de get_my_attributes() al shape `atributosActivos`. */
function mapAtributos(d) {
  if (!d || d.error) return { ...DEFAULT_ATRIBUTOS }
  const estado = d.identidadEstado
  return {
    identidadValidada: d.identidadValidada === true,
    identidadEnRevision: estado === 'PENDIENTE' || estado === 'PENDING',
    identidadRechazada: estado === 'RECHAZADO' || estado === 'REJECTED',
    tieneReservasActivas: d.tieneReservasActivas === true,
    tieneAsignacionesActivas: d.tieneAsignacionesActivas === true,
    reservasSinClase: d.reservasSinClase === true,
    reservasSinClaseCount: d.reservasSinClaseCount || 0,
    perfilProfesionalCompleto: d.perfilProfesionalCompleto === true,
    hasRoleTeacher: d.hasRoleTeacher === true,
    tieneSedeAprobada: d.tieneSedeAprobada === true,
    estadoSolicitudSede: d.estadoSolicitudSede || null,
    motivoRechazoSede: d.motivoRechazoSede || null,
    estadoProfesor: d.estadoProfesor || 'INACTIVO'
  }
}

// Mantiene token + usuario sincronizados con la sesión de Supabase
// (login, refresh automático del token, logout en otra pestaña, etc.).
supabase.auth.onAuthStateChange(async (event, session) => {
  if (event === 'SIGNED_OUT' || !session) {
    store.clearAuth()
    return
  }
  store.setToken(session.access_token, session.refresh_token)
  if (event === 'SIGNED_IN' || event === 'INITIAL_SESSION') {
    try {
      const u = await buildUserFromSession(session)
      if (u) store.setUser(u)
    } catch (err) {
      console.error('Error al construir el usuario de la sesión', err)
    }
  }
})

export function useAuth() {
  const token = store.token
  const user = store.user
  const modoActual = store.modoActual

  const isAuthenticated = computed(() => store.isAuthenticated())
  const identidadValidada = computed(() => user.value?.atributosActivos?.identidadValidada || false)
  const identidadEnRevision = computed(() => user.value?.atributosActivos?.identidadEnRevision || false)
  const identidadRechazada = computed(() => user.value?.atributosActivos?.identidadRechazada || false)
  const tieneReservasActivas = computed(() => user.value?.atributosActivos?.tieneReservasActivas || false)
  const tieneAsignacionesActivas = computed(() => user.value?.atributosActivos?.tieneAsignacionesActivas || false)
  const reservasSinClase = computed(() => user.value?.atributosActivos?.reservasSinClase || false)
  const reservasSinClaseCount = computed(() => user.value?.atributosActivos?.reservasSinClaseCount || 0)
  const perfilProfesionalCompleto = computed(() => user.value?.atributosActivos?.perfilProfesionalCompleto || false)
  const hasRoleTeacher = computed(() => user.value?.atributosActivos?.hasRoleTeacher || user.value?.roles?.includes('TEACHER') || false)
  const estadoProfesor = computed(() => user.value?.atributosActivos?.estadoProfesor || 'INACTIVO')

  const puedeAlternarModo = computed(() => {
    if (!user.value) return false
    const roles = user.value.roles || []
    return roles.includes('TEACHER') || roles.includes('VENUE_ADMIN') || roles.includes('ADMIN')
  })

  const puedeVerContextoProfesor = computed(() => user.value?.atributosActivos?.hasRoleTeacher || false)
  const puedeVerContextoSede = computed(() => user.value?.roles?.includes('VENUE_ADMIN') || false)

  const isAdmin = computed(() => user.value?.roles?.includes('ADMIN') || false)
  const isSede = computed(() => user.value?.roles?.includes('VENUE_ADMIN') || false)
  const isTeacher = computed(() => user.value?.roles?.includes('TEACHER') || false)

  const displayName = computed(() => {
    if (!user.value) return 'Usuario'
    return user.value.socialName || user.value.fullName?.split(' ')[0] || 'Usuario'
  })

  async function login(email, password) {
    const { data, error } = await supabase.auth.signInWithPassword({ email, password })
    if (error) throw normalizeAuthError(error)
    store.setToken(data.session.access_token, data.session.refresh_token)
    const u = await buildUserFromSession(data.session)
    store.setUser(u)
    setModo('alumno')
    return u
  }

  async function googleLogin(credential) {
    // `credential` = ID token de Google Identity Services (One Tap / botón GIS).
    const { data, error } = await supabase.auth.signInWithIdToken({ provider: 'google', token: credential })
    if (error) throw normalizeAuthError(error)
    store.setToken(data.session.access_token, data.session.refresh_token)
    const u = await buildUserFromSession(data.session)
    store.setUser(u)
    setModo('alumno')
    return u
  }

  async function register(fullName, email, password, phone, rut) {
    const { data, error } = await supabase.auth.signUp({
      email,
      password,
      options: { data: { full_name: fullName, phone: phone || '', rut: rut || '' } }
    })
    if (error) throw normalizeAuthError(error)
    // Si el proyecto exige confirmación de email, no habrá sesión todavía.
    if (!data.session) {
      return { requiresEmailConfirmation: true, email }
    }
    store.setToken(data.session.access_token, data.session.refresh_token)
    const u = await buildUserFromSession(data.session)
    store.setUser(u)
    setModo('alumno')
    return u
  }

  async function logout() {
    // 1. Invalidar sesión en Supabase (esto borra sb-*-auth-token de localStorage)
    try {
      await supabase.auth.signOut({ scope: 'local' })
    } catch (err) {
      console.error('Error al cerrar sesión en Supabase', err)
    }
    // 2. Limpiar nuestro estado personalizado
    store.clearAuth()
    // 3. Limpiar cualquier residuo de Supabase en localStorage por si signOut no lo hizo
    const keysToRemove = Object.keys(localStorage).filter(
      (k) => k.startsWith('sb-') && k.endsWith('-auth-token')
    )
    keysToRemove.forEach((k) => localStorage.removeItem(k))
    // 4. Redirigir (hard reload para limpiar estado in-memory)
    window.location.href = '/login'
  }

  function setModo(mode) {
    modoActual.value = mode
    localStorage.setItem('modoActual', mode)
  }

  function updateUserAttributes(attrs) {
    if (!user.value) return
    const merged = { ...user.value, atributosActivos: { ...user.value.atributosActivos, ...attrs } }
    store.setUser(merged)
  }

  async function syncAtributos() {
    try {
      const { data, error } = await supabase.rpc('get_my_attributes')
      if (error) throw error
      updateUserAttributes(mapAtributos(data))
      // Reflejar roles emergentes (TEACHER / VENUE_ADMIN) sin re-login.
      if (user.value) {
        const roles = user.value.roles || []
        const next = [...roles]
        if (data?.hasRoleTeacher && !next.includes('TEACHER')) next.push('TEACHER')
        if (data?.tieneSedeAprobada && !next.includes('VENUE_ADMIN')) next.push('VENUE_ADMIN')
        if (next.length !== roles.length) {
          store.setUser({ ...user.value, roles: next })
        }
      }
    } catch (err) {
      console.error('Error al sincronizar atributos de usuario', err)
    }
  }

  // Las versiones específicas delegan en el superset (una sola RPC, datos consistentes).
  async function syncIdentityStatus() {
    await syncAtributos()
  }

  async function syncActividadMaestro() {
    await syncAtributos()
  }

  async function refreshProfile() {
    try {
      const { data: { session } } = await supabase.auth.getSession()
      if (!session) return
      const u = await buildUserFromSession(session)
      if (u) store.setUser(u)
    } catch (err) {
      console.error('Error al refrescar el perfil', err)
    }
  }

  async function updateUserProfile(data) {
    const { data: { session } } = await supabase.auth.getSession()
    if (!session) throw new Error('No autenticado')
    const patch = {}
    if (data.fullName !== undefined) patch.full_name = data.fullName
    if (data.socialName !== undefined) patch.social_name = data.socialName
    if (data.phone !== undefined) patch.phone = data.phone
    const { error } = await supabase.from('profiles').update(patch).eq('id', session.user.id)
    if (error) throw error
    const u = await buildUserFromSession(session)
    store.setUser(u)
    return u
  }

  async function refreshToken() {
    try {
      const { data, error } = await supabase.auth.refreshSession()
      if (error || !data.session) return false
      store.setToken(data.session.access_token, data.session.refresh_token)
      return true
    } catch {
      return false
    }
  }

  return {
    token,
    user,
    modoActual,
    isAuthenticated,
    identidadValidada,
    identidadEnRevision,
    identidadRechazada,
    tieneReservasActivas,
    tieneAsignacionesActivas,
    reservasSinClase,
    reservasSinClaseCount,
    perfilProfesionalCompleto,
    hasRoleTeacher,
    estadoProfesor,
    tieneSedeAprobada: computed(() => user.value?.atributosActivos?.tieneSedeAprobada || false),
    estadoSolicitudSede: computed(() => user.value?.atributosActivos?.estadoSolicitudSede || null),
    motivoRechazoSede: computed(() => user.value?.atributosActivos?.motivoRechazoSede || null),
    puedeAlternarModo,
    puedeVerContextoProfesor,
    puedeVerContextoSede,
    isAdmin,
    isSede,
    isTeacher,
    displayName,
    login,
    googleLogin,
    register,
    logout,
    setModo,
    updateUserAttributes,
    updateUserProfile,
    refreshToken,
    syncIdentityStatus,
    syncAtributos,
    syncActividadMaestro,
    refreshProfile
  }
}

/** Convierte un AuthError de Supabase al shape estilo-axios que las vistas esperan. */
function normalizeAuthError(error) {
  return {
    response: { status: error.status || 400, data: { message: error.message } },
    message: error.message
  }
}
