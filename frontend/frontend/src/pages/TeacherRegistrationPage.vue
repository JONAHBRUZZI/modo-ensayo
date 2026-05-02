<template>
  <div class="mx-auto max-w-lg space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">Registro como Profesor</h1>
      <p class="text-sm text-gray-600">Solicita acceso para crear y gestionar clases.</p>
    </div>

    <div v-if="isTeacher" class="rounded-xl border border-emerald-200 bg-emerald-50 p-6 text-center">
      <svg class="mx-auto h-12 w-12 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      <h3 class="mt-3 text-lg font-semibold text-emerald-900">Ya eres profesor</h3>
      <p class="mt-1 text-sm text-emerald-700">Puedes crear y gestionar clases.</p>
      <router-link to="/teacher/classes" class="mt-4 inline-block rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700">Ir a Mis Clases</router-link>
    </div>

    <div v-else class="rounded-xl border bg-white p-6">
      <div class="space-y-4">
        <div class="rounded-lg bg-amber-50 p-4 text-sm text-amber-700">
          <p class="font-medium">Requisitos para ser profesor:</p>
          <ul class="mt-2 list-disc pl-5 space-y-1">
            <li>Haber subido tu documento de identidad</li>
            <li>Esperar validacion del administrador</li>
          </ul>
        </div>

        <button @click="requestTeacher" :disabled="requesting" class="w-full rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50">
          {{ requesting ? 'Solicitando...' : 'Solicitar rol de Profesor' }}
        </button>

        <router-link to="/profile/identity" class="block text-center text-sm text-indigo-600 hover:underline">
          Subir documento de identidad primero
        </router-link>
      </div>
    </div>

    <div v-if="successMessage" class="fixed bottom-4 right-4 rounded-lg bg-emerald-600 px-4 py-3 text-sm text-white shadow-lg">
      {{ successMessage }}
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../hooks/useAuth'
import api from '../services/api'

const router = useRouter()
const { user, refreshUser } = useAuth()
const requesting = ref(false)
const successMessage = ref('')
const isTeacher = ref(false)

onMounted(() => {
  isTeacher.value = user.value?.roles?.includes('TEACHER') || false
})

const requestTeacher = async () => {
  requesting.value = true
  try {
    const { data } = await api.post('/auth/request-teacher')
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify({ email: data.email, fullName: data.fullName, roles: data.roles }))
    await refreshUser()
    isTeacher.value = true
    successMessage.value = 'Rol de profesor activado exitosamente!'
    setTimeout(() => { router.push('/teacher/classes') }, 1500)
  } catch (e) {
    alert(e.response?.data?.message || 'Error al solicitar rol de profesor.')
  } finally {
    requesting.value = false
  }
}
</script>
