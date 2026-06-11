import { ref, computed } from 'vue'
import api from '@/services/api'

const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

// Auth store class
class AuthStore {
  constructor() {
    this.token = ref(localStorage.getItem('auth_token') || null)
    this.user = ref(parseUser())
    this.modoActual = ref(localStorage.getItem('modoActual') || 'alumno')
  }

  setToken(token, refreshToken = null) {
    this.token.value = token
    localStorage.setItem('auth_token', token)
    if (refreshToken) {
      localStorage.setItem('auth_refresh_token', refreshToken)
    }
  }

  getToken() {
    return this.token.value
  }

  getRefreshToken() {
    return localStorage.getItem('auth_refresh_token')
  }

  setUser(user) {
    this.user.value = user
    localStorage.setItem('auth_user', JSON.stringify(user))
  }

  getUser() {
    return this.user.value
  }

  isAuthenticated() {
    if (!this.token.value) return false
    try {
      const payload = jwtDecode(this.token.value)
      return payload.exp * 1000 > Date.now()
    } catch {
      return false
    }
  }

  hasRole(roles) {
    if (!this.user.value?.roles) return false
    return roles.some((r) => this.user.value.roles.includes(r))
  }

  clearAuth() {
    this.token.value = null
    this.user.value = null
    localStorage.removeItem('auth_token')
    localStorage.removeItem('auth_user')
    localStorage.removeItem('auth_refresh_token')
  }

  logout() {
    this.clearAuth()
  }
}

function jwtDecode(token) {
  try {
    return JSON.parse(atob(token.split('.')[1]))
  } catch {
    return {}
  }
}

function parseUser() {
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
      atributosActivos: u.atributosActivos || {
        identidadValidada: false,
        identidadEnRevision: false,
        tieneReservasActivas: false,
        tieneAsignacionesActivas: false,
        hasRoleTeacher: false,
        estadoProfesor: 'INACTIVO'
      }
    }
  } catch {
    return null
  }
}

const store = new AuthStore()

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

  // Botón Maestro: permanente una vez que el usuario tiene rol TEACHER (spec Sección 8)
  const puedeVerContextoProfesor = computed(() => {
    return user.value?.roles?.includes('TEACHER') || false
  })
  const puedeVerContextoSede = computed(() => user.value?.roles?.includes('VENUE_ADMIN') || false)

  const isAdmin = computed(() => user.value?.roles?.includes('ADMIN') || false)
  const isSede = computed(() => user.value?.roles?.includes('VENUE_ADMIN') || false)
  const isTeacher = computed(() => user.value?.roles?.includes('TEACHER') || false)

  const displayName = computed(() => {
    if (!user.value) return 'Usuario'
    return user.value.socialName || user.value.fullName?.split(' ')[0] || 'Usuario'
  })

  async function login(email, password) {
    const res = await api.post('/auth/login', { email, password })
    const { token: t, refreshToken: rt, user: u } = res.data
    store.setToken(t, rt)
    store.setUser(u)
    setModo('alumno')
    return u
  }

  async function googleLogin(credential) {
    const res = await api.post('/auth/google', { credential })
    const { token: t, refreshToken: rt, user: u } = res.data
    store.setToken(t, rt)
    store.setUser(u)
    setModo('alumno')
    return u
  }

  async function register(fullName, email, password, phone, rut) {
    const res = await api.post('/auth/register', {
      fullName,
      email,
      password,
      phone,
      rut
    })
    const { token: t, refreshToken: rt, user: u } = res.data
    store.setToken(t, rt)
    store.setUser(u)
    setModo('alumno')
    return u
  }

  async function logout() {
    try {
      await fetch(`${API_BASE}/api/auth/logout`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${store.getToken()}` }
      })
    } catch {
      // ignore
    }
    store.logout()
    window.location.href = '/login'
  }

  function setModo(mode) {
    modoActual.value = mode
    localStorage.setItem('modoActual', mode)
  }

  function updateUserAttributes(attrs) {
    if (user.value) {
      user.value.atributosActivos = { ...user.value.atributosActivos, ...attrs }
      const raw = JSON.parse(localStorage.getItem('auth_user') || '{}')
      raw.atributosActivos = { ...raw.atributosActivos, ...attrs }
      localStorage.setItem('auth_user', JSON.stringify(raw))
    }
  }

  async function syncActividadMaestro() {
    try {
      const res = await api.get('/users/me/actividad-maestro')
      if (res.data) {
        updateUserAttributes({
          tieneReservasActivas: res.data.tieneReservasActivas,
          tieneAsignacionesActivas: res.data.tieneAsignacionesActivas
        })
      }
    } catch {
      // silencioso — no crítico
    }
  }

  async function syncIdentityStatus() {
    try {
      const res = await api.get('/users/me/identity-verification')
      if (res.data) {
        const status = res.data.status
        updateUserAttributes({
          identidadValidada: status === 'APPROVED',
          identidadEnRevision: status === 'PENDING',
          identidadRechazada: status === 'REJECTED'
        })
      }
    } catch {
      updateUserAttributes({ identidadValidada: false, identidadEnRevision: false, identidadRechazada: false })
    }
  }

  async function syncAtributos() {
    try {
      const res = await api.get('/users/me/atributos')
      if (res.data) {
        updateUserAttributes({
          identidadValidada: res.data.identidadValidada,
          identidadEnRevision: res.data.identidadEstado === 'PENDING' || res.data.identidadEstado === 'PENDIENTE',
          identidadRechazada: res.data.identidadEstado === 'REJECTED' || res.data.identidadEstado === 'RECHAZADA',
          tieneReservasActivas: res.data.tieneReservasActivas,
          tieneAsignacionesActivas: res.data.tieneAsignacionesActivas,
          tieneSedeAprobada: res.data.tieneSedeAprobada,
          reservasSinClase: res.data.reservasSinClase,
          reservasSinClaseCount: res.data.reservasSinClaseCount || 0,
          perfilProfesionalCompleto: res.data.perfilProfesionalCompleto || false,
          hasRoleTeacher: res.data.hasRoleTeacher || false,
          estadoProfesor: res.data.estadoProfesor || 'INACTIVO'
        })
        if (res.data.hasRoleTeacher && user.value && !user.value.roles.includes('TEACHER')) {
          user.value.roles.push('TEACHER')
          localStorage.setItem('auth_user', JSON.stringify(user.value))
        }
        if (res.data.tieneSedeAprobada && user.value && !user.value.roles.includes('VENUE_ADMIN')) {
          user.value.roles.push('VENUE_ADMIN')
          localStorage.setItem('auth_user', JSON.stringify(user.value))
        }
      }
    } catch {}
  }

  async function refreshProfile() {
    try {
      const res = await api.get('/users/me')
      if (res.data) {
        const d = res.data
        const merged = {
          id: d.id,
          email: d.email,
          fullName: d.fullName,
          socialName: d.socialName,
          phone: d.phone,
          roles: d.roles || ['USER'],
          enabled: d.enabled !== false,
          atributosActivos: {
            ...(user.value?.atributosActivos || {}),
            identidadValidada: d.identidadValidada || false,
            identidadEnRevision: d.identidadEnRevision || false
          }
        }
        store.setUser(merged)
      }
    } catch { /* silencioso */ }
  }

  async function updateUserProfile(data) {
    const res = await api.put('/users/me', data)
    store.setUser(res.data)
    return res.data
  }

  async function refreshToken() {
    try {
      const rt = store.getRefreshToken()
      if (!rt) return false
      const res = await fetch(`${API_BASE}/api/auth/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${rt}`
        }
      })
      if (!res.ok) return false
      const data = await res.json()
      store.setToken(data.token, data.refreshToken)
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
