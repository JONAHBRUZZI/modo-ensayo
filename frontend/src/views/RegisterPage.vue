<template>
  <div class="min-h-[80vh] flex items-center justify-center px-4 py-10">
    <div class="card max-w-md w-full">
      <h1 class="text-2xl font-bold text-white text-center mb-8">Crear Cuenta</h1>
      <form @submit.prevent="handleRegister" class="space-y-4">
    <div>
      <label class="block text-sm font-medium text-gray-300 mb-1">Nombre Completo <span class="text-red-400">*</span></label>
      <input type="text" v-model="fullName" required class="input-field" placeholder="Juan Perez"
        :class="errorCampo === 'fullName' ? 'border-red-500/60' : ''"
        @input="errorCampo = null" />
      <p v-if="errorCampo === 'fullName'" class="text-red-400 text-xs mt-1">{{ error }}</p>
    </div>
    <div>
      <label class="block text-sm font-medium text-gray-300 mb-1">Email <span class="text-red-400">*</span></label>
      <input type="email" v-model="email" required class="input-field" placeholder="tu@gmail.com"
        :class="errorCampo === 'email' ? 'border-red-500/60' : ''"
        @input="errorCampo = null" />
    </div>
    <div>
      <label class="block text-sm font-medium text-gray-300 mb-1">RUT <span class="text-gray-500">(opcional)</span></label>
      <input type="text" v-model="rut" class="input-field" placeholder="12345678-9"
        :class="errorCampo === 'rut' ? 'border-red-500/60' : ''"
        @input="errorCampo = null" />
    </div>
    <div>
      <label class="block text-sm font-medium text-gray-300 mb-1">Telefono <span class="text-gray-500">(opcional)</span></label>
      <input type="tel" v-model="phone" class="input-field" placeholder="+56912345678"
        :class="errorCampo === 'phone' ? 'border-red-500/60' : ''"
        @input="errorCampo = null" />
    </div>
    <div>
      <label class="block text-sm font-medium text-gray-300 mb-1">Contrasena <span class="text-red-400">*</span></label>
      <input type="password" v-model="password" required class="input-field" placeholder="Min 8 caracteres, mayuscula, minuscula, numero" />
      <p class="text-xs text-gray-500 mt-1">Minimo 8 caracteres, al menos una mayuscula, una minuscula y un numero.</p>
    </div>
        <div class="flex items-start space-x-2">
          <input type="checkbox" v-model="aceptoTerminos" id="terminos" class="mt-1 text-primary" />
          <label for="terminos" class="text-xs text-gray-400">Acepto los terminos y condiciones de Modo Ensayo y autorizo el tratamiento de mis datos personales conforme a la Ley 19.628.</label>
        </div>

        <!-- Error genérico -->
        <p v-if="error && !errorCampo" class="text-red-400 text-sm">{{ error }}</p>

        <!-- Error de campo específico (email o RUT duplicado) -->
        <div v-if="error && errorCampo"
          class="flex items-start gap-2 rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-2.5">
          <svg class="w-4 h-4 text-red-400 mt-0.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
          </svg>
          <div>
            <p class="text-red-300 text-sm font-medium">
              {{ errorCampo === 'rut' ? 'RUT ya registrado' : 'Correo ya registrado' }}
            </p>
            <p class="text-red-400 text-xs mt-0.5">{{ error }}</p>
          </div>
        </div>

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
const rut = ref('')
const aceptoTerminos = ref(false)
const error = ref('')
const errorCampo = ref(null)
const loading = ref(false)

async function handleRegister() {
  error.value = ''
  errorCampo.value = null
  if (!aceptoTerminos.value) {
    error.value = 'Debes aceptar los terminos y condiciones para registrarte.'
    return
  }

  const trimmed = fullName.value.trim()
  if (trimmed.split(/\s+/).length < 2) {
    error.value = 'Ingresa tu nombre completo (nombre y apellido).'
    errorCampo.value = 'fullName'
    return
  }

  if (!email.value.includes('@') || !email.value.includes('.')) {
    error.value = 'Ingresa un correo electronico valido.'
    errorCampo.value = 'email'
    return
  }

  if (!/@gmail\.com$/i.test(email.value.trim())) {
    error.value = 'Solo se permiten correos @gmail.com.'
    errorCampo.value = 'email'
    return
  }

  if (rut.value && !validateRut(rut.value)) {
    error.value = 'El RUT ingresado no es valido. Usa formato 12345678-9.'
    errorCampo.value = 'rut'
    return
  }

  if (phone.value && !/^\+?[0-9]{8,15}$/.test(phone.value.trim())) {
    error.value = 'El telefono debe tener entre 8 y 15 digitos numericos.'
    errorCampo.value = 'phone'
    return
  }

  const pwRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/
  if (!pwRegex.test(password.value)) {
    error.value = 'La contrasena debe tener al menos 8 caracteres, una mayuscula, una minuscula y un numero.'
    return
  }
  loading.value = true
  try {
    await register(fullName.value.trim(), email.value.trim(), password.value, phone.value.trim() || null, rut.value.trim() || null)
    router.push('/alumno/dashboard')
  } catch (e) {
    const msg = e.response?.data?.message || 'Error al crear cuenta'
    const status = e.response?.status
    if (status === 409) {
      error.value = msg
      errorCampo.value = msg.toLowerCase().includes('rut') ? 'rut' : 'email'
    } else {
      error.value = msg
    }
  } finally {
    loading.value = false
  }
}

function validateRut(rut) {
  const cleaned = rut.replace(/[^0-9kK]/g, '')
  if (cleaned.length < 2) return false
  const dv = cleaned.slice(-1).toUpperCase()
  const body = cleaned.slice(0, -1)
  let sum = 0
  let mul = 2
  for (let i = body.length - 1; i >= 0; i--) {
    sum += parseInt(body[i]) * mul
    mul = mul === 7 ? 2 : mul + 1
  }
  const expected = 11 - (sum % 11)
  const expectedDv = expected === 11 ? '0' : expected === 10 ? 'K' : String(expected)
  return dv === expectedDv
}
</script>
