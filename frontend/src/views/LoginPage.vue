<template>
  <div class="min-h-[80vh] flex items-center justify-center px-4">
    <div
      v-motion
      :initial="{ opacity: 0, y: 24, scale: 0.98 }"
      :enter="{ opacity: 1, y: 0, scale: 1, transition: { duration: 400 } }"
      class="card max-w-md w-full"
    >
      <h1 class="text-2xl font-bold text-white text-center mb-8">Iniciar sesión</h1>

      <div v-if="aviso" class="bg-yellow-500/10 border border-yellow-500/30 rounded-lg p-3 mb-6">
        <p class="text-yellow-300 text-sm">{{ aviso }}</p>
      </div>

      <form @submit.prevent="handleLogin" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Email</label>
          <input type="email" v-model="email" required class="input-field" placeholder="tu@email.com" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Contraseña</label>
          <input type="password" v-model="password" required class="input-field" placeholder="********" />
        </div>
        <p v-if="error" class="text-red-400 text-sm">{{ error }}</p>
        <button type="submit" :disabled="loading" class="btn-primary w-full">
          {{ loading ? 'Iniciando sesion...' : 'Iniciar sesión' }}
        </button>
      </form>

      <GoogleLoginButton />

      <p class="text-center text-gray-400 text-sm mt-6">
        ¿No tienes cuenta?
        <router-link to="/register" class="text-primary hover:underline">Registrate</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '@/stores/auth'
import GoogleLoginButton from '@/components/GoogleLoginButton.vue'

const router = useRouter()
const route = useRoute()
const { login } = useAuth()

const email = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

// Aviso mostrado cuando el usuario llega redirigido por una sesión cortada
// (eliminado/suspendido por el admin, o token expirado en pleno uso).
const aviso = computed(() =>
  route.query.motivo === 'sesion'
    ? 'Tu sesión finalizó. Si tu cuenta fue suspendida, contacta al administrador. Vuelve a iniciar sesión para continuar.'
    : ''
)

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    await login(email.value, password.value)
    router.push('/alumno/dashboard')
  } catch (e) {
    error.value = e.response?.data?.message || 'Error al iniciar sesión'
  } finally {
    loading.value = false
  }
}
</script>
