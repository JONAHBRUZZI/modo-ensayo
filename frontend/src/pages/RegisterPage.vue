<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full space-y-8">
      <div class="text-center">
        <h2 class="text-3xl font-extrabold text-gray-900">Crear Cuenta</h2>
        <p class="mt-2 text-sm text-gray-600">Completa tus datos para registrarte</p>
      </div>

      <div class="bg-white p-8 rounded-lg shadow-md">
        <form class="space-y-6" @submit.prevent="handleRegister">
          <div v-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md text-sm">
            {{ error }}
          </div>

          <div>
            <label for="fullName" class="block text-sm font-medium text-gray-700">Nombre Completo</label>
            <input
              id="fullName"
              v-model="form.fullName"
              type="text"
              required
              class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              placeholder="Juan Perez"
            />
          </div>

          <div>
            <label for="email" class="block text-sm font-medium text-gray-700">Email</label>
            <input
              id="email"
              v-model="form.email"
              type="email"
              required
              autocomplete="email"
              class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              placeholder="tu@email.com"
            />
          </div>

          <div>
            <label for="phone" class="block text-sm font-medium text-gray-700">Telefono (opcional)</label>
            <input
              id="phone"
              v-model="form.phone"
              type="tel"
              class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              placeholder="+56912345678"
            />
          </div>

          <div>
            <label for="password" class="block text-sm font-medium text-gray-700">Password</label>
            <input
              id="password"
              v-model="form.password"
              type="password"
              required
              autocomplete="new-password"
              minlength="6"
              class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              placeholder="Minimo 6 caracteres"
            />
          </div>

          <div>
            <label for="confirmPassword" class="block text-sm font-medium text-gray-700">Confirmar Password</label>
            <input
              id="confirmPassword"
              v-model="form.confirmPassword"
              type="password"
              required
              autocomplete="new-password"
              minlength="6"
              class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              placeholder="Repite tu password"
            />
          </div>

          <button
            type="submit"
            :disabled="isLoading || !isFormValid"
            class="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <span v-if="isLoading" class="flex items-center">
              <svg class="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              Creando cuenta...
            </span>
            <span v-else>Registrarse</span>
          </button>
        </form>

        <div class="mt-6 text-center text-sm">
          <span class="text-gray-600">Ya tienes cuenta?</span>
          <router-link to="/login" class="ml-1 font-medium text-indigo-600 hover:text-indigo-500">
            Inicia sesion aqui
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../hooks/useAuth'

const router = useRouter()
const { register } = useAuth()

const form = ref({
  fullName: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
})

const error = ref('')
const isLoading = ref(false)

const isFormValid = computed(() => {
  return form.value.fullName &&
         form.value.email &&
         form.value.password.length >= 6 &&
         form.value.password === form.value.confirmPassword
})

const handleRegister = async () => {
  error.value = ''

  if (form.value.password !== form.value.confirmPassword) {
    error.value = 'Las passwords no coinciden'
    return
  }

  isLoading.value = true

  try {
    await register(form.value.fullName, form.value.email, form.value.password, form.value.phone)
    router.push({ name: 'Home' })
  } catch (err) {
    error.value = err.response?.data?.message || 'Error al crear la cuenta. Intenta con otro email.'
  } finally {
    isLoading.value = false
  }
}
</script>
