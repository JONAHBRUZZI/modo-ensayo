<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Verificacion de Identidad</h1>
    <div class="card">
      <p class="text-gray-300 mb-6">Para acceder a funcionalidades avanzadas, necesitas verificar tu identidad subiendo una foto de tu documento.</p>
      <div v-if="verification" class="mb-6">
        <div class="flex items-center space-x-2">
          <span class="text-gray-400">Estado:</span>
          <EstadoBadge :status="verification.status" />
          <button v-if="verification.status === 'APPROVED' || verification.status === 'REJECTED'" @click="deleteDocument" class="text-xs text-red-400 hover:text-red-300 underline ml-4">Eliminar documento</button>
        </div>
      </div>
      <div class="border-2 border-dashed border-gray-700 rounded-xl p-8 text-center">
        <input type="file" ref="fileInput" accept="image/*" @change="handleFile" class="hidden" />
        <div v-if="!file" @click="$refs.fileInput.click()" class="cursor-pointer">
          <svg class="w-10 h-10 text-gray-500 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/></svg>
          <p class="text-gray-400">Click para subir documento</p>
        </div>
        <div v-else>
          <p class="text-green-400 mb-3">{{ file.name }}</p>
          <button @click="upload" :disabled="uploading" class="btn-primary">{{ uploading ? 'Subiendo...' : 'Enviar Documento' }}</button>
        </div>
      </div>
      <p v-if="msg" :class="msgType === 'success' ? 'text-green-400' : 'text-red-400'" class="text-sm mt-4">{{ msg }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import userService from '@/services/userService'
import uploadService from '@/services/uploadService'
import EstadoBadge from '@/components/EstadoBadge.vue'

const verification = ref(null)
const file = ref(null)
const uploading = ref(false)
const msg = ref('')
const msgType = ref('')

onMounted(async () => {
  try { verification.value = await userService.getIdentityVerification() } catch {}
})

function handleFile(e) { file.value = e.target.files[0] }

async function upload() {
  uploading.value = true
  msg.value = ''
  try {
    const data = await uploadService.uploadFile(file.value, 'documents')
    await userService.uploadIdentityDocument(data.url || data.fileUrl)
    msg.value = 'Documento enviado para revision'
    msgType.value = 'success'
    verification.value = await userService.getIdentityVerification()
    file.value = null
  } catch (e) {
    msg.value = e.response?.data?.message || 'Error al subir'
    msgType.value = 'error'
  } finally {
    uploading.value = false
  }
}

async function deleteDocument() {
  if (!confirm('Esto eliminara tu documento de identidad del sistema. La validacion previa se mantiene registrada. Continuar?')) return
  try {
    await userService.deleteIdentityDocument()
    msg.value = 'Documento eliminado. La validacion previa se mantiene.'
    msgType.value = 'success'
    verification.value = await userService.getIdentityVerification()
  } catch (e) {
    msg.value = e.response?.data?.message || 'Error al eliminar'
    msgType.value = 'error'
  }
}
</script>
