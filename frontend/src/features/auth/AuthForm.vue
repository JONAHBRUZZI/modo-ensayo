<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../../hooks/useAuth'

const props = defineProps({
  mode: { type: String, default: 'login' },
})

const router = useRouter()
const { login, register } = useAuth()

const loading = ref(false)
const error = ref(null)

const form = ref({
  email: '',
  password: '',
  fullName: '',
  phone: '',
})

async function handleSubmit() {
  loading.value = true
  error.value = null
  try {
    if (props.mode === 'login') {
      await login(form.value.email, form.value.password)
    } else {
      await register(form.value.fullName, form.value.email, form.value.password, form.value.phone || null)
    }
    router.push({ name: 'Home' })
  } catch (e) {
    error.value = e?.response?.data?.message || e?.message || 'Error inesperado'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <form @submit.prevent="handleSubmit" class="max-w-md mx-auto p-6 bg-white rounded-lg shadow-md">
    <h2 class="text-2xl font-bold mb-6 text-center">
      {{ mode === 'login' ? 'Iniciar sesión' : 'Crear cuenta' }}
    </h2>

    <div v-if="error" class="mb-4 p-3 bg-red-100 text-red-700 rounded">
      {{ error }}
    </div>

    <div class="mb-4">
      <label class="block text-sm font-medium mb-1">Email</label>
      <input
        v-model="form.email"
        type="email"
        required
        class="w-full px-3 py-2 border rounded focus:ring-2 focus:ring-indigo-500"
      />
    </div>

    <div class="mb-4">
      <label class="block text-sm font-medium mb-1">Contraseña</label>
      <input
        v-model="form.password"
        type="password"
        required
        minlength="6"
        class="w-full px-3 py-2 border rounded focus:ring-2 focus:ring-indigo-500"
      />
    </div>

    <template v-if="mode === 'register'">
      <div class="mb-4">
        <label class="block text-sm font-medium mb-1">Nombre Completo</label>
        <input
          v-model="form.fullName"
          type="text"
          required
          class="w-full px-3 py-2 border rounded focus:ring-2 focus:ring-indigo-500"
        />
      </div>
      <div class="mb-4">
        <label class="block text-sm font-medium mb-1">Teléfono (opcional)</label>
        <input
          v-model="form.phone"
          type="text"
          class="w-full px-3 py-2 border rounded focus:ring-2 focus:ring-indigo-500"
        />
      </div>
    </template>

    <button
      type="submit"
      :disabled="loading"
      class="w-full py-2 px-4 bg-indigo-600 text-white rounded hover:bg-indigo-700 disabled:opacity-50"
    >
      {{ loading ? 'Cargando...' : mode === 'login' ? 'Ingresar' : 'Registrarse' }}
    </button>
  </form>
</template>
