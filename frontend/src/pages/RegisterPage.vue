<template>
  <div class="min-h-screen flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full space-y-8">
      <div class="text-center">
        <div class="flex items-center justify-center gap-2 mb-4">
          <svg class="w-6 h-6 text-yellow-400" fill="currentColor" viewBox="0 0 24 24">
            <path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/>
          </svg>
          <span class="text-xl font-bold text-white">Modo Ensayo</span>
        </div>
        <h2 class="text-3xl font-extrabold text-white">Crear Cuenta</h2>
        <p class="mt-2 text-sm text-gray-400">Completa tus datos para registrarte</p>
      </div>

      <div class="bg-[#161824] p-8 rounded-2xl border border-white/10">
        <form class="space-y-5" @submit.prevent="handleRegister">
          <div v-if="error" class="bg-red-500/15 border border-red-500/30 text-red-400 px-4 py-3 rounded-lg text-sm">
            {{ error }}
          </div>

          <div>
            <label for="fullName" class="block text-sm font-medium text-gray-300 mb-1">Nombre Completo</label>
            <input
              id="fullName"
              v-model="form.fullName"
              type="text"
              required
              class="block w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
              placeholder="Juan Pérez"
            />
          </div>

          <div>
            <label for="email" class="block text-sm font-medium text-gray-300 mb-1">Email</label>
            <input
              id="email"
              v-model="form.email"
              type="email"
              required
              autocomplete="email"
              class="block w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
              placeholder="tu@email.com"
            />
          </div>

          <div>
            <label for="phone" class="block text-sm font-medium text-gray-300 mb-1">Teléfono <span class="text-gray-500">(opcional)</span></label>
            <input
              id="phone"
              v-model="form.phone"
              type="tel"
              class="block w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
              placeholder="+56912345678"
            />
          </div>

          <div>
            <label for="password" class="block text-sm font-medium text-gray-300 mb-1">Contraseña</label>
            <input
              id="password"
              v-model="form.password"
              type="password"
              required
              autocomplete="new-password"
              minlength="6"
              class="block w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
              placeholder="Mínimo 6 caracteres"
            />
          </div>

          <div>
            <label for="confirmPassword" class="block text-sm font-medium text-gray-300 mb-1">Confirmar Contraseña</label>
            <input
              id="confirmPassword"
              v-model="form.confirmPassword"
              type="password"
              required
              autocomplete="new-password"
              minlength="6"
              class="block w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
              placeholder="Repite tu contraseña"
            />
          </div>

          <button
            type="submit"
            :disabled="isLoading || !isFormValid"
            class="w-full flex justify-center py-3 px-4 rounded-lg text-sm font-semibold text-white bg-purple-600 hover:bg-purple-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors shadow-lg shadow-purple-500/25 mt-2"
          >
            <span v-if="isLoading" class="flex items-center gap-2">
              <svg class="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              Creando cuenta...
            </span>
            <span v-else>Registrarse</span>
          </button>
        </form>

        <div class="mt-6 text-center text-sm">
          <span class="text-gray-500">¿Ya tienes cuenta?</span>
          <router-link to="/login" class="ml-1 font-medium text-purple-400 hover:text-purple-300 transition-colors">
            Inicia sesión aquí
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '../hooks/useAuth'

const router = useRouter()
const route = useRoute()
const { register } = useAuth()

const form = ref({ fullName: '', email: '', phone: '', password: '', confirmPassword: '' })
const error = ref('')
const isLoading = ref(false)

const isFormValid = computed(() =>
  form.value.fullName &&
  form.value.email &&
  form.value.password.length >= 6 &&
  form.value.password === form.value.confirmPassword
)

const handleRegister = async () => {
  error.value = ''
  if (form.value.password !== form.value.confirmPassword) {
    error.value = 'Las contraseñas no coinciden'
    return
  }
  isLoading.value = true
  try {
    await register(form.value.fullName, form.value.email, form.value.password, form.value.phone)
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (err) {
    error.value = err.response?.data?.message || 'Error al crear la cuenta. Intenta con otro email.'
  } finally {
    isLoading.value = false
  }
}
</script>
