<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Verificacion de Identidad</h1>
    <div class="space-y-6">
      <div v-if="verification" class="card">
        <div class="flex items-center space-x-2">
          <span class="text-gray-400">Estado:</span>
          <EstadoBadge :status="verification.status" />
          <button v-if="verification.status === 'APPROVED' || verification.status === 'REJECTED'" @click="deleteDocument" class="text-xs text-red-400 hover:text-red-300 underline ml-4">Eliminar documento</button>
        </div>
        <div v-if="verification.status === 'REJECTED'" class="mt-3 text-sm text-red-400">Tu verificacion fue rechazada. Corrige los datos y vuelve a intentarlo.</div>
        <div v-if="verification.status === 'PENDING'" class="mt-3 text-sm text-yellow-400">Tu documento esta en revision. Espera la aprobacion del Administrador General.</div>
      </div>

      <form v-if="!verification || verification.status === 'REJECTED'" @submit.prevent="upload" class="card space-y-4">
        <h3 class="text-white font-medium">Datos del documento</h3>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Tipo de documento *</label>
          <select v-model="form.documentType" required class="input-field">
            <option value="">Seleccionar</option>
            <option value="RUT">Cedula de Identidad (RUT)</option>
            <option value="PASSPORT">Pasaporte</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Numero de documento *</label>
          <input v-model="form.documentNumber" required class="input-field" :placeholder="form.documentType === 'RUT' ? '12.345.678-9' : 'Numero de pasaporte'" @input="formatearRut" @blur="validarRut" />
          <p v-if="rutError" class="text-xs text-red-400 mt-1">{{ rutError }}</p>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Nombre completo (segun documento) *</label>
          <input v-model="form.fullName" required class="input-field" placeholder="Como aparece en tu documento"
            :class="fullNameError ? 'border-red-500/60' : ''" @input="fullNameError = ''" />
          <p v-if="fullNameError" class="text-xs text-red-400 mt-1">{{ fullNameError }}</p>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Fecha de nacimiento *</label>
          <input type="date" v-model="form.birthDate" required class="input-field"
            :max="maxBirthDate" :min="minBirthDate" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Documento (JPG, PNG o PDF, max 5MB) *</label>
          <div class="border-2 border-dashed border-gray-700 rounded-xl p-6 text-center">
            <input type="file" ref="fileInput" accept="image/*,.pdf" @change="handleFile" class="hidden" />
            <div v-if="!file" @click="$refs.fileInput.click()" class="cursor-pointer">
              <svg class="w-8 h-8 text-gray-500 mx-auto mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/></svg>
              <p class="text-gray-400 text-sm">Click para subir documento</p>
            </div>
            <div v-else>
              <p class="text-green-400 text-sm mb-2">{{ file.name }}</p>
              <button type="button" @click="file = null" class="text-xs text-red-400 underline">Cambiar archivo</button>
            </div>
          </div>
        </div>
        <!-- Mensaje de exito -->
        <p v-if="msg && msgType === 'success'" class="text-sm text-green-400">{{ msg }}</p>

        <!-- Mensaje de error: alert prominente con icono cuando hay conflicto de documento -->
        <div v-if="msg && msgType === 'error'"
             class="rounded-xl border border-red-500/40 bg-red-500/10 p-4 flex items-start gap-3">
          <svg class="w-5 h-5 text-red-400 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
          </svg>
          <div class="flex-1 min-w-0">
            <p class="text-red-300 font-semibold text-sm">No se pudo validar el documento</p>
            <p class="text-red-200/90 text-sm mt-1 whitespace-pre-line">{{ msg }}</p>
          </div>
        </div>

        <button type="submit" :disabled="uploading || (msg && msgType === 'error')"
                class="btn-primary w-full disabled:opacity-50 disabled:cursor-not-allowed">
          {{ uploading ? 'Enviando...' : (msg && msgType === 'error' ? 'Corrige los datos y vuelve a enviar' : 'Enviar para validacion') }}
        </button>
      </form>

      <div v-if="verification?.status === 'APPROVED'" class="card">
        <div class="flex items-center space-x-2 mb-2">
          <svg class="w-5 h-5 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
          <span class="text-green-400 font-medium">Identidad verificada</span>
        </div>
        <div v-if="verification.documentType" class="text-gray-400 text-sm">Tipo: {{ verification.documentType }} | Numero: {{ verification.documentNumber }}</div>
        <div v-if="verification.fullName" class="text-gray-400 text-sm">Nombre: {{ verification.fullName }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import userService from '@/services/userService'
import uploadService from '@/services/uploadService'
import EstadoBadge from '@/components/EstadoBadge.vue'

const verification = ref(null)
const file = ref(null)
const uploading = ref(false)
const msg = ref('')
const msgType = ref('')
const form = reactive({ documentType: '', documentNumber: '', fullName: '', birthDate: '' })
const rutError = ref('')
const fullNameError = ref('')

const maxBirthDate = computed(() => {
  const d = new Date()
  d.setFullYear(d.getFullYear() - 14)
  return d.toISOString().split('T')[0]
})

const minBirthDate = computed(() => {
  const d = new Date()
  d.setFullYear(d.getFullYear() - 120)
  return d.toISOString().split('T')[0]
})

onMounted(async () => {
  try { verification.value = await userService.getIdentityVerification() } catch {}
})

function handleFile(e) { file.value = e.target.files[0] }

function formatearRut() {
  // Cualquier edicion del RUT limpia el error de duplicado para permitir reintentar
  if (msgType.value === 'error') { msg.value = ''; msgType.value = '' }
  if (form.documentType !== 'RUT' || !form.documentNumber) return
  let valor = form.documentNumber.replace(/[^0-9kK]/g, '')
  if (valor.length < 2) { form.documentNumber = valor; return }
  const dv = valor.slice(-1)
  let cuerpo = valor.slice(0, -1)
  cuerpo = cuerpo.replace(/\B(?=(\d{3})+(?!\d))/g, '.')
  form.documentNumber = cuerpo + '-' + dv
}

function validarRut() {
  rutError.value = ''
  if (form.documentType !== 'RUT' || !form.documentNumber) return
  const rut = form.documentNumber.replace(/[^0-9kK]/g, '')
  if (rut.length < 2) { rutError.value = 'RUT invalido'; return }
  const dv = rut.slice(-1).toUpperCase()
  const cuerpo = rut.slice(0, -1)
  let suma = 0
  let multiplo = 2
  for (let i = cuerpo.length - 1; i >= 0; i--) {
    suma += parseInt(cuerpo[i]) * multiplo
    multiplo = multiplo < 7 ? multiplo + 1 : 2
  }
  const dvEsperado = 11 - (suma % 11)
  const dvCalculado = dvEsperado === 11 ? '0' : dvEsperado === 10 ? 'K' : dvEsperado.toString()
  if (dv !== dvCalculado) rutError.value = 'RUT invalido: el digito verificador no coincide'
}

async function upload() {
  if (!file.value) { msg.value = 'Debes subir un documento'; msgType.value = 'error'; return }
  if (file.value.size > 5 * 1024 * 1024) { msg.value = 'El archivo no debe superar los 5MB'; msgType.value = 'error'; return }
  if (rutError.value) { msg.value = 'Corrige el RUT antes de enviar'; msgType.value = 'error'; return }

  const fullName = form.fullName.trim()
  if (fullName.split(/\s+/).length < 2) {
    fullNameError.value = 'Ingresa tu nombre completo (nombre y apellido)'
    msg.value = ''; msgType.value = ''
    return
  }
  fullNameError.value = ''

  if (form.documentType === 'PASSPORT' && form.documentNumber.trim().length < 3) {
    msg.value = 'El numero de pasaporte debe tener al menos 3 caracteres'; msgType.value = 'error'; return
  }

  if (form.birthDate) {
    const birth = new Date(form.birthDate)
    if (birth > new Date()) { msg.value = 'La fecha de nacimiento no puede ser futura'; msgType.value = 'error'; return }
    const age = (new Date() - birth) / (365.25 * 24 * 60 * 60 * 1000)
    if (age < 14) { msg.value = 'Debes ser mayor de 14 anos para verificar tu identidad'; msgType.value = 'error'; return }
  }
  uploading.value = true
  msg.value = ''
  try {
    const data = await uploadService.uploadFile(file.value, 'documents')
    await userService.uploadIdentityDocument(data.url || data.fileUrl, form)
    msg.value = 'Documento y datos enviados para revision'
    msgType.value = 'success'
    verification.value = await userService.getIdentityVerification()
    file.value = null
  } catch (e) {
    msg.value = e.response?.data?.message || 'Error al enviar'
    msgType.value = 'error'
  } finally {
    uploading.value = false
  }
}

async function deleteDocument() {
  if (!confirm('Esto eliminara tu documento de identidad del sistema. La validacion previa se mantiene registrada. Continuar?')) return
  try {
    await userService.deleteIdentityDocument()
    msg.value = 'Documento eliminado.'
    msgType.value = 'success'
    verification.value = await userService.getIdentityVerification()
  } catch (e) {
    msg.value = e.response?.data?.message || 'Error al eliminar'
    msgType.value = 'error'
  }
}
</script>
