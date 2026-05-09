import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { authService } from '../services/authService'

const token = ref(localStorage.getItem('token') || null)

// Migrar usuario guardado: si los atributos son todos false pero tiene roles especiales, derivarlos
const _cargarUsuario = () => {
  const raw = localStorage.getItem('user')
  if (!raw) return null
  const u = JSON.parse(raw)
  const roles = u?.roles || []
  const a = u?.atributosActivos || {}
  const todosVacios = !a.identidadValidada && !a.tieneReservasActivas && !a.tieneAsignacionesActivas
  if (todosVacios && (roles.includes('TEACHER') || roles.includes('VENUE_ADMIN'))) {
    const esProfesor = roles.includes('TEACHER')
    const esSede = roles.includes('VENUE_ADMIN')
    u.atributosActivos = {
      identidadValidada: esProfesor || esSede,
      identidadEnRevision: false,
      tieneReservasActivas: esProfesor,
      tieneAsignacionesActivas: esProfesor,
    }
    localStorage.setItem('user', JSON.stringify(u))
  }
  return u
}

const user = ref(_cargarUsuario())
// Ref reactivo para el modo — evita leer localStorage dentro de computed()
const _modoGuardado = ref(localStorage.getItem('modoActual') || 'alumno')

export function useAuth() {
  const router = useRouter()

  const isAuthenticated = computed(() => !!token.value)

  // Computed de atributos dinámicos
  const identidadValidada = computed(() => user.value?.atributosActivos?.identidadValidada ?? false)
  const identidadEnRevision = computed(() => user.value?.atributosActivos?.identidadEnRevision ?? false)
  const tieneReservasActivas = computed(() => user.value?.atributosActivos?.tieneReservasActivas ?? false)
  const tieneAsignacionesActivas = computed(() => user.value?.atributosActivos?.tieneAsignacionesActivas ?? false)

  const isAdmin = computed(() => (user.value?.roles || []).includes('ADMIN'))
  const isSede = computed(() => (user.value?.roles || []).includes('VENUE_ADMIN'))

  // Contextos disponibles para este usuario
  const puedeVerContextoProfesor = computed(() =>
    isAuthenticated.value && (tieneReservasActivas.value || tieneAsignacionesActivas.value)
  )
  const puedeVerContextoSede = computed(() => isAuthenticated.value && isSede.value)

  // El interruptor aparece cuando hay al menos un contexto adicional al de alumno
  const puedeAlternarModo = computed(() =>
    puedeVerContextoProfesor.value || puedeVerContextoSede.value
  )

  // Modo activo: alumno | profesor | sede
  const modoActual = computed(() => {
    if (!isAuthenticated.value) return null
    const guardado = _modoGuardado.value
    if (guardado === 'sede' && puedeVerContextoSede.value) return 'sede'
    if (guardado === 'profesor' && puedeVerContextoProfesor.value) return 'profesor'
    return 'alumno'
  })

  const setModo = (modo) => {
    _modoGuardado.value = modo
    localStorage.setItem('modoActual', modo)
  }

  // Deriva atributos desde los roles cuando el backend no los envía
  const _derivarAtributos = (roles = [], atributosBackend = null) => {
    if (atributosBackend) return atributosBackend
    const esProfesor = roles.includes('TEACHER')
    const esSede = roles.includes('VENUE_ADMIN')
    return {
      identidadValidada: esProfesor || esSede,
      identidadEnRevision: false,
      tieneReservasActivas: esProfesor,
      tieneAsignacionesActivas: esProfesor,
    }
  }

  const login = async (email, password) => {
    const response = await authService.login({ email, password })
    token.value = response.token
    user.value = {
      email: response.email,
      fullName: response.fullName,
      roles: response.roles || [],
      atributosActivos: _derivarAtributos(response.roles, response.atributosActivos),
    }
    localStorage.setItem('token', response.token)
    localStorage.setItem('user', JSON.stringify(user.value))
    return response
  }

  const register = async (fullName, email, password, phone, rut) => {
    const response = await authService.register({ fullName, email, password, phone, rut })
    token.value = response.token
    user.value = {
      email: response.email,
      fullName: response.fullName,
      roles: response.roles || [],
      atributosActivos: _derivarAtributos(response.roles, response.atributosActivos),
    }
    localStorage.setItem('token', response.token)
    localStorage.setItem('user', JSON.stringify(user.value))
    return response
  }

  const updateUserAttributes = (atributos) => {
    user.value = { ...user.value, atributosActivos: { ...user.value?.atributosActivos, ...atributos } }
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  const updateUserProfile = (data) => {
    // Solo permite actualizar campos no sensibles (no fullName)
    const { socialName, phone } = data
    user.value = { ...user.value, ...(socialName !== undefined && { socialName }), ...(phone !== undefined && { phone }) }
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  // Nombre a mostrar: nombre social si existe, si no el primer nombre del nombre completo
  const displayName = computed(() =>
    user.value?.socialName?.trim() || user.value?.fullName?.split(' ')[0] || 'Usuario'
  )

  const logout = () => {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    localStorage.removeItem('modoActual')
    router.push({ name: 'Login' })
  }

  return {
    token,
    user,
    isAuthenticated,
    identidadValidada,
    identidadEnRevision,
    tieneReservasActivas,
    tieneAsignacionesActivas,
    puedeAlternarModo,
    puedeVerContextoProfesor,
    puedeVerContextoSede,
    modoActual,
    isAdmin,
    isSede,
    login,
    register,
    logout,
    setModo,
    updateUserAttributes,
    updateUserProfile,
    displayName,
  }
}
