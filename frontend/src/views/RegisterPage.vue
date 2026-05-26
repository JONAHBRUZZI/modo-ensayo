<template>
  <div class="min-h-[80vh] flex items-center justify-center px-4 py-10">
    <div class="card max-w-md w-full">
      <h1 class="text-2xl font-bold text-white text-center mb-8">Crear Cuenta</h1>
      <form @submit.prevent="handleRegister" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Nombre Completo</label>
          <input type="text" v-model="fullName" required class="input-field" placeholder="Juan Perez" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Email</label>
          <input type="email" v-model="email" required class="input-field" placeholder="tu@email.com" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Telefono <span class="text-gray-500">(opcional)</span></label>
          <input type="tel" v-model="phone" class="input-field" placeholder="+56912345678" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Contrasena</label>
          <input type="password" v-model="password" required class="input-field" placeholder="Min 8 caracteres, mayuscula, minuscula, numero" />
          <p class="text-xs text-gray-500 mt-1">Minimo 8 caracteres, al menos una mayuscula, una minuscula y un numero.</p>
        </div>
        <div class="flex items-start space-x-2">
          <input type="checkbox" v-model="aceptoTerminos" id="terminos" class="mt-1 text-primary" />
          <label for="terminos" class="text-xs text-gray-400">Acepto los terminos y condiciones de Modo Ensayo y autorizo el tratamiento de mis datos personales conforme a la Ley 19.628.</label>
        </div>
        <p v-if="error" class="text-red-400 text-sm">{{ error }}</p>
        <button type="submit" :disabled="loading" class="btn-primary w-full">
          {{ loading ? 'Creando cuenta...' : 'Crear Cuenta' }}
        </button>
      </form>
      <p class="text-center text-gray-400 text-sm mt-6">
        Ya tienes cuenta?
        <router-link to="/login" class="text-primary hover:underline">Inicia sesion</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'

const router = useRouter()
const { register } = useAuth()

const fullName = ref('')
const email = ref('')
const phone = ref('')
const password = ref('')
const aceptoTerminos = ref(false)
const error = ref('')
const loading = ref(false)

async function handleRegister() {
  error.value = ''
  if (!aceptoTerminos.value) {
    error.value = 'Debes aceptar los terminos y condiciones para registrarte.'
    return
  }
  const pwRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/
  if (!pwRegex.test(password.value)) {
    error.value = 'La contrasena debe tener al menos 8 caracteres, una mayuscula, una minuscula y un numero.'
    return
  }
  loading.value = true
  try {
    await register(
      fullName.value,
      email.value,
      password.value,
      phone.value,
      null
    )
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.message || 'Error al crear cuenta'
  } finally {
    loading.value = false
  }
}
</script>
