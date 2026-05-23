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
          <label class="block text-sm font-medium text-gray-300 mb-1">Telefono</label>
          <input type="tel" v-model="phone" required class="input-field" placeholder="+56912345678" />
        </div>
        <DocumentoIdentidad v-model="documento" />
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Contrasena</label>
          <input type="password" v-model="password" required class="input-field" placeholder="Minimo 8 caracteres" />
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
import DocumentoIdentidad from '@/components/DocumentoIdentidad.vue'

const router = useRouter()
const { register } = useAuth()

const fullName = ref('')
const email = ref('')
const phone = ref('')
const documento = ref({ tipo: 'RUT', numero: '' })
const password = ref('')
const error = ref('')
const loading = ref(false)

async function handleRegister() {
  error.value = ''
  loading.value = true
  try {
    await register(
      fullName.value,
      email.value,
      password.value,
      phone.value,
      documento.value.numero
    )
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.message || 'Error al crear cuenta'
  } finally {
    loading.value = false
  }
}
</script>
