<template>
  <div class="min-h-[80vh] flex items-center justify-center py-12 px-4">
    <div class="max-w-sm w-full space-y-6">
      <div class="text-center">
        <div class="inline-flex items-center justify-center w-12 h-12 bg-yellow-400/10 rounded-full border border-yellow-400/20 mb-4">
          <svg class="w-6 h-6 text-yellow-400" fill="currentColor" viewBox="0 0 24 24">
            <path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/>
          </svg>
        </div>
        <h1 class="text-2xl font-bold text-white">Bienvenido de vuelta</h1>
        <p class="text-gray-400 text-sm mt-1">Inicia sesión en tu cuenta</p>
      </div>

      <div class="bg-[#161824] rounded-2xl border border-white/10 p-6 shadow-2xl space-y-4">
        <div v-if="error" class="bg-red-500/15 border border-red-500/30 text-red-400 text-sm rounded-lg p-3">
          {{ error }}
        </div>

        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Email</label>
          <input v-model="email" type="email" placeholder="tu@email.com" @keyup.enter="handleLogin"
                 class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent text-sm transition-all" />
        </div>

        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Contraseña</label>
          <div class="relative">
            <input v-model="password" :type="showPassword ? 'text' : 'password'" placeholder="••••••••" @keyup.enter="handleLogin"
                   class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent text-sm pr-10 transition-all" />
            <button type="button" @click="showPassword = !showPassword"
                    class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-300 transition-colors">
              <svg v-if="!showPassword" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0zM2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
              <svg v-else class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
              </svg>
            </button>
          </div>
          <div class="text-right mt-1.5">
            <a href="#" class="text-xs text-indigo-400 hover:text-indigo-300 transition-colors">¿Olvidaste tu contraseña?</a>
          </div>
        </div>

        <button @click="handleLogin" :disabled="loading || !email || !password"
                class="w-full py-2.5 bg-purple-600 hover:bg-purple-500 disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg text-sm font-semibold transition-colors shadow-lg shadow-purple-500/25">
          <span v-if="loading" class="flex items-center justify-center gap-2">
            <svg class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
            </svg>
            Iniciando sesión...
          </span>
          <span v-else>Iniciar Sesión</span>
        </button>
      </div>

      <p class="text-center text-sm text-gray-500">
        ¿No tienes cuenta?
        <router-link to="/register" class="text-indigo-400 hover:text-indigo-300 font-medium transition-colors">Regístrate gratis</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '../hooks/useAuth'

const { login } = useAuth()
const router = useRouter()
const route = useRoute()

const email = ref('')
const password = ref('')
const showPassword = ref(false)
const loading = ref(false)
const error = ref('')

const handleLogin = async () => {
  if (loading.value || !email.value || !password.value) return
  loading.value = true
  error.value = ''
  try {
    await login(email.value, password.value)
    const redirect = route.query.redirect || '/alumno/dashboard'
    router.push(redirect)
  } catch (e) {
    error.value = e?.response?.data?.message || 'Credenciales incorrectas. Intenta nuevamente.'
  } finally {
    loading.value = false
  }
}
</script>
