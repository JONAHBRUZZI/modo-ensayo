import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { authService } from '../services/authService'

const token = ref(localStorage.getItem('token') || null)
const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

export function useAuth() {
  const router = useRouter()

  const isAuthenticated = computed(() => !!token.value)

  const login = async (email, password) => {
    const response = await authService.login({ email, password })
    token.value = response.token
    user.value = { email: response.email, fullName: response.fullName, roles: response.roles }
    localStorage.setItem('token', response.token)
    localStorage.setItem('user', JSON.stringify(user.value))
    return response
  }

  const register = async (fullName, email, password, phone) => {
    const response = await authService.register({ fullName, email, password, phone })
    token.value = response.token
    user.value = { email: response.email, fullName: response.fullName, roles: response.roles }
    localStorage.setItem('token', response.token)
    localStorage.setItem('user', JSON.stringify(user.value))
    return response
  }

  const logout = () => {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    router.push({ name: 'Login' })
  }

  return {
    token,
    user,
    isAuthenticated,
    login,
    register,
    logout,
  }
}
