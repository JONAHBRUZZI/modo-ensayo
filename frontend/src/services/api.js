import axios from 'axios'

const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

const api = axios.create({
  baseURL: `${API_BASE}/api`,
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('auth_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => {
    // Fix #8: si el backend indica que los atributos del usuario cambiaron,
    // sincronizamos automáticamente para reflejar nuevos roles/estado.
    if (response.data?.atributosActualizados === true) {
      // Importación dinámica para evitar dependencia circular
      import('@/stores/auth').then(({ useAuth }) => {
        try {
          const auth = useAuth()
          auth.syncAtributos()
        } catch { /* silencioso */ }
      })
    }
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('auth_token')
      localStorage.removeItem('auth_user')
      localStorage.removeItem('auth_refresh_token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
