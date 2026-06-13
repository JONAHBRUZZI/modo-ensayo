<template>
  <div class="min-h-[80vh] flex items-center justify-center px-4">
    <div class="card max-w-md w-full">
      <h1 class="text-2xl font-bold text-white text-center mb-8">Iniciar Sesion</h1>
      <form @submit.prevent="handleLogin" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Email</label>
          <input type="email" v-model="email" required class="input-field" placeholder="tu@email.com" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Contrasena</label>
          <input type="password" v-model="password" required class="input-field" placeholder="********" />
        </div>
        <p v-if="error" class="text-red-400 text-sm">{{ error }}</p>
        <button type="submit" :disabled="loading" class="btn-primary w-full">
          {{ loading ? 'Iniciando sesion...' : 'Iniciar Sesion' }}
        </button>
      </form>

      <GoogleLoginButton />

      <p class="text-center text-gray-400 text-sm mt-6">
        No tienes cuenta?
        <router-link to="/register" class="text-primary hover:underline">Registrate</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'
import GoogleLoginButton from '@/components/GoogleLoginButton.vue'

const router = useRouter()
const { login } = useAuth()

const email = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    await login(email.value, password.value)
    router.push('/alumno/dashboard')
  } catch (e) {
    error.value = e.response?.data?.message || 'Error al iniciar sesion'
  } finally {
    loading.value = false
  }
}
</script>
