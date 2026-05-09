<template>
  <div class="max-w-2xl mx-auto space-y-6">
    <div>
      <h1 class="text-3xl font-bold text-white mb-2">Verificación de Identidad</h1>
      <p class="text-gray-400">Sube tu documento para validar tu cuenta y desbloquear todas las funciones</p>
    </div>

    <!-- Estado actual -->
    <div v-if="verification" class="bg-[#161824] rounded-xl border p-4"
         :class="statusConfig.border">
      <div class="flex items-start gap-3">
        <div :class="statusConfig.iconBg" class="w-9 h-9 rounded-full flex items-center justify-center flex-shrink-0">
          <svg class="w-4 h-4" :class="statusConfig.iconColor" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path v-if="verification.status === 'APPROVED'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            <path v-else-if="verification.status === 'REJECTED'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <div class="flex-1">
          <p class="font-semibold text-white text-sm">{{ statusConfig.title }}</p>
          <p class="text-xs text-gray-400 mt-0.5">{{ statusConfig.description }}</p>
          <div v-if="verification.status === 'REJECTED' && verification.rejectionReason"
               class="mt-2 bg-red-500/10 border border-red-500/20 rounded-lg px-3 py-2">
            <p class="text-xs text-red-400"><span class="font-medium">Motivo:</span> {{ verification.rejectionReason }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Upload area (solo si no está aprobado) -->
    <div v-if="!verification || verification.status !== 'APPROVED'" class="bg-[#161824] rounded-xl border border-white/10 p-6 space-y-4">
      <h2 class="font-semibold text-white text-sm">
        {{ verification?.status === 'REJECTED' ? 'Volver a enviar documento' : 'Subir documento' }}
      </h2>

      <div class="bg-indigo-500/10 border border-indigo-500/20 rounded-lg px-3 py-2">
        <p class="text-xs text-indigo-300">
          <span class="font-medium">Documentos aceptados:</span> Cédula de identidad (ambos lados), pasaporte o licencia de conducir vigente.
        </p>
      </div>

      <FileUpload @update:file="selectedFile = $event" hint="JPG, PNG o PDF — máx. 10MB" accept=".jpg,.jpeg,.png,.pdf" />

      <button @click="handleUpload" :disabled="!selectedFile || isUploading"
              class="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg text-sm font-semibold transition-colors">
        <span v-if="isUploading" class="flex items-center justify-center gap-2">
          <svg class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
          </svg>
          Subiendo documento...
        </span>
        <span v-else>Enviar para Verificación</span>
      </button>
    </div>

    <!-- Ya aprobado -->
    <div v-if="verification?.status === 'APPROVED'" class="bg-[#161824] rounded-xl border border-white/10 p-6 text-center">
      <div class="text-4xl mb-3">✓</div>
      <p class="text-white font-semibold">Tu identidad ha sido verificada</p>
      <p class="text-gray-400 text-sm mt-1">Puedes acceder a todas las funciones de Modo Ensayo</p>
      <router-link to="/alumno/dashboard"
                   class="inline-flex items-center gap-2 mt-4 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm rounded-lg transition-colors">
        Ir a mi panel
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { uploadService } from '../services/uploadService'
import { userService } from '../services/userService'
import FileUpload from '../components/FileUpload.vue'
import { useAuth } from '../hooks/useAuth'

const { updateUserAttributes } = useAuth()

const verification = ref(null)
const isUploading = ref(false)
const selectedFile = ref(null)

const statusConfig = computed(() => {
  const s = verification.value?.status
  if (s === 'APPROVED') return {
    title: 'Identidad verificada', description: 'Tu cuenta está completamente habilitada.',
    border: 'border-emerald-500/30', iconBg: 'bg-emerald-500/20', iconColor: 'text-emerald-400',
  }
  if (s === 'REJECTED') return {
    title: 'Verificación rechazada', description: 'Por favor sube un nuevo documento.',
    border: 'border-red-500/30', iconBg: 'bg-red-500/20', iconColor: 'text-red-400',
  }
  return {
    title: 'En revisión', description: 'Estamos revisando tu documento. Esto puede tomar hasta 24 horas.',
    border: 'border-blue-500/30', iconBg: 'bg-blue-500/20', iconColor: 'text-blue-400',
  }
})

onMounted(async () => {
  try { verification.value = await userService.getIdentityVerification() } catch { /* sin verificación */ }
})

const handleUpload = async () => {
  if (!selectedFile.value) return
  isUploading.value = true
  try {
    const { url } = await uploadService.upload(selectedFile.value, 'identity')
    verification.value = await userService.uploadIdentity(url)
    updateUserAttributes({ identidadEnRevision: true, identidadValidada: false })
  } catch {
    alert('Error al subir el documento. Intenta nuevamente.')
  } finally {
    isUploading.value = false
  }
}
</script>
