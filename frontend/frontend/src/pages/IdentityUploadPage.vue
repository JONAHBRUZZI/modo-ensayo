<template>
  <div class="max-w-2xl mx-auto space-y-8">
    <div>
      <h1 class="text-3xl font-bold text-gray-900 mb-2">Verificacion de Identidad</h1>
      <p class="text-gray-600">Sube tu documento de identidad para validar tu cuenta</p>
    </div>

    <div class="bg-white rounded-xl shadow-md p-6">
      <div v-if="verification" class="mb-6">
        <div :class="['p-4 rounded-lg', statusClass]">
          <p class="font-semibold">Estado: {{ statusText }}</p>
          <p v-if="verification.documentUrl" class="text-sm mt-1">Documento: {{ verification.documentUrl }}</p>
        </div>
      </div>

      <div class="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center">
        <svg class="mx-auto h-12 w-12 text-gray-400 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
        </svg>
        <p class="text-gray-500 mb-2">Arrastra y suelta tu documento o haz clic para seleccionar</p>
        <p class="text-xs text-gray-400 mb-4">Formatos aceptados: JPG, PNG, PDF (max 10MB)</p>
        <label class="inline-flex items-center px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 cursor-pointer">
          <input type="file" @change="handleFileUpload" accept=".jpg,.jpeg,.png,.pdf" class="hidden" />
          <span>{{ isUploading ? 'Subiendo...' : 'Seleccionar Documento' }}</span>
        </label>
      </div>

      <div v-if="previewUrl" class="mt-4">
        <img :src="previewUrl" alt="Preview" class="max-h-64 rounded-lg mx-auto" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { uploadService } from '../services/uploadService'
import { userService } from '../services/userService'

const verification = ref(null)
const isUploading = ref(false)
const previewUrl = ref(null)

const statusClass = computed(() => {
  const s = verification.value?.status
  if (s === 'APPROVED') return 'bg-green-50 text-green-700'
  if (s === 'REJECTED') return 'bg-red-50 text-red-700'
  return 'bg-yellow-50 text-yellow-700'
})

const statusText = computed(() => {
  const s = verification.value?.status
  if (s === 'APPROVED') return 'Verificado'
  if (s === 'REJECTED') return 'Rechazado'
  return 'Pendiente de revision'
})

onMounted(async () => {
  try { verification.value = await userService.getIdentityVerification() } catch (e) { /* no verification yet */ }
})

const handleFileUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  previewUrl.value = URL.createObjectURL(file)
  isUploading.value = true

  try {
    const { url } = await uploadService.upload(file, 'identity')
    verification.value = await userService.uploadIdentity(url)
  } catch (e) {
    alert('Error al subir documento')
  } finally {
    isUploading.value = false
  }
}
</script>