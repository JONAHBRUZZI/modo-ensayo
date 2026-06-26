<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-8">Verificaciones de Identidad</h1>
    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>
    <div v-else-if="verifications.length === 0" class="card text-center py-16">
      <div class="w-14 h-14 bg-[var(--bg-elevated)] rounded-2xl flex items-center justify-center mx-auto mb-4">
        <svg class="w-7 h-7 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12l2 2 4-4M7.835 4.697a3.42 3.42 0 001.946-.806 3.42 3.42 0 014.438 0 3.42 3.42 0 001.946.806 3.42 3.42 0 013.138 3.138 3.42 3.42 0 00.806 1.946 3.42 3.42 0 010 4.438 3.42 3.42 0 00-.806 1.946 3.42 3.42 0 01-3.138 3.138 3.42 3.42 0 00-1.946.806 3.42 3.42 0 01-4.438 0 3.42 3.42 0 00-1.946-.806 3.42 3.42 0 01-3.138-3.138 3.42 3.42 0 00-.806-1.946 3.42 3.42 0 010-4.438 3.42 3.42 0 00.806-1.946 3.42 3.42 0 013.138-3.138z"/>
        </svg>
      </div>
      <p class="text-gray-400 font-medium">Sin verificaciones pendientes</p>
      <p class='text-gray-600 text-sm mt-1'>Cuando un usuario suba su identidad, aparecerá aquí.</p>
    </div>
    <div v-else class="space-y-4">
      <div v-for="v in verifications" :key="v.id" class="card">
        <div class="flex items-start justify-between">
          <div class="flex-1 space-y-2">
            <div class="flex items-center space-x-3">
              <div class="w-10 h-10 bg-primary/20 rounded-full flex items-center justify-center text-primary font-bold">{{ (v.fullName || v.userName || v.userEmail || 'U').charAt(0).toUpperCase() }}</div>
              <div>
                <h3 class="text-white font-medium">{{ v.fullName || v.userName || v.userEmail }}</h3>
                <p class="text-gray-400 text-sm">{{ v.documentType || 'RUT' }}: {{ v.documentNumber || v.userEmail || 'Ver documento adjunto' }}</p>
                <p v-if="v.birthDate" class="text-gray-500 text-xs">Nacimiento: {{ v.birthDate }}</p>
              </div>
            </div>
            <div class="flex items-center space-x-3 pl-13">
              <a v-if="v.documentUrl" @click.prevent="verDocumento(v.documentUrl)" href="#" class="text-xs text-indigo-400 hover:text-indigo-300 underline cursor-pointer">Ver documento adjunto</a>
              <EstadoBadge :status="v.status" />
            </div>
          </div>
          <div class="flex space-x-2 ml-4">
            <button @click="abrirModal(v, 'identity-approve')" class="text-green-400 hover:text-green-300 text-sm font-medium">Aprobar</button>
            <button @click="abrirModal(v, 'identity-reject')" class="text-red-400 hover:text-red-300 text-sm font-medium">Rechazar</button>
          </div>
        </div>
      </div>
    </div>

    <h2 class="text-2xl font-bold text-white mt-12 mb-6">Sedes Pendientes de Aprobacion</h2>
    <div v-if="pendingVenues.length === 0" class="card text-center py-12">
      <div class="w-14 h-14 bg-[var(--bg-elevated)] rounded-2xl flex items-center justify-center mx-auto mb-4">
        <svg class="w-7 h-7 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/>
        </svg>
      </div>
      <p class="text-gray-400 font-medium">Sin sedes pendientes</p>
    </div>
    <div v-else class="space-y-4">
      <div v-for="v in pendingVenues" :key="v.id" class="card flex items-center justify-between">
        <div><h3 class="text-white font-medium">{{ v.name }}</h3><p class="text-gray-400 text-sm">{{ v.city }}, {{ v.address }}</p></div>
        <div class="flex space-x-2">
          <button @click="abrirModal(v, 'venue-approve')" class="btn-primary text-sm bg-green-600 hover:bg-green-700">Aprobar</button>
          <button @click="abrirModal(v, 'venue-reject')" class="btn-danger text-sm">Rechazar</button>
        </div>
      </div>
    </div>

    <BottomSheet v-model="modal.abierto">
      <h3 class="text-lg font-semibold text-white mb-2">{{ modal.titulo }}</h3>
      <p class="text-gray-400 text-sm mb-4">{{ modal.mensaje }}</p>
      <div v-if="modal.accion === 'venue-reject' || modal.accion === 'identity-reject'" class="mb-4">
        <label class="block text-sm text-gray-400 mb-1">Motivo (requerido):</label>
        <textarea v-model="modal.motivo" rows="2" class="input-field" placeholder="Explica el motivo del rechazo..."></textarea>
      </div>
      <div class="flex space-x-3 justify-end">
        <button @click="modal.abierto = false" class="px-4 py-2 rounded-xl border border-white/10 text-gray-300 hover:bg-white/5 text-sm">Cancelar</button>
        <button @click="ejecutarAccion" :disabled="modal.enviando" class="px-4 py-2 rounded-xl text-white text-sm font-medium" :class="modal.esAprobacion ? 'bg-green-600 hover:bg-green-500' : 'bg-red-600 hover:bg-red-500'">
          {{ modal.enviando ? 'Procesando...' : 'Si, confirmar' }}
        </button>
      </div>
    </BottomSheet>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import adminService from '@/services/adminService'
import EstadoBadge from '@/components/EstadoBadge.vue'
import { useToast } from '@/composables/useToast'
import uploadService from '@/services/uploadService'
import BottomSheet from '@/components/BottomSheet.vue'

const toast = useToast()

const verifications = ref([])
const pendingVenues = ref([])
const loading = ref(true)
const modal = reactive({
  abierto: false, titulo: '', mensaje: '', accion: '', item: null,
  motivo: '', esAprobacion: false, enviando: false
})

onMounted(async () => {
  try { verifications.value = await adminService.getIdentityVerifications() } catch { verifications.value = [] }
  try { pendingVenues.value = await adminService.getPendingVenues() } catch { pendingVenues.value = [] }
  loading.value = false
})

async function verDocumento(url) {
  try {
    const viewUrl = await uploadService.resolveDocUrl(url)
    if (!viewUrl) throw new Error('sin url')
    window.open(viewUrl, '_blank')
  } catch {
    toast.error('No se pudo cargar el documento')
  }
}

function abrirModal(item, accion) {
  modal.item = item
  modal.accion = accion
  modal.motivo = ''
  modal.enviando = false
  modal.esAprobacion = accion.endsWith('approve')
  if (accion === 'identity-approve') {
    modal.titulo = 'Aprobar identidad'
    modal.mensaje = `¿Confirma su respuesta? Se aprobara la identidad de ${item.userName || item.userEmail}. El usuario habilitara capacidades de Maestro y registro de sede.`
  } else if (accion === 'identity-reject') {
    modal.titulo = 'Rechazar identidad'
    modal.mensaje = `¿Confirma su respuesta? Se rechazara la identidad de ${item.userName || item.userEmail}. El usuario podra reenviar un nuevo documento.`
  } else if (accion === 'venue-approve') {
    modal.titulo = 'Aprobar sede'
    modal.mensaje = `¿Confirma su respuesta? La sede "${item.name}" sera APROBADA y podra comenzar a operar con sus salas visibles.`
  } else {
    modal.titulo = 'Rechazar sede'
    modal.mensaje = `¿Confirma su respuesta? La sede "${item.name}" sera RECHAZADA. El administrador podra corregir y reenviar.`
  }
  modal.abierto = true
}

async function ejecutarAccion() {
  modal.enviando = true
  try {
    const { accion, item, motivo } = modal
    if (accion === 'identity-approve') await adminService.reviewIdentity(item.id, 'approve')
    else if (accion === 'identity-reject') await adminService.reviewIdentity(item.id, 'reject')
    else if (accion === 'venue-approve') await adminService.approveVenue(item.id)
    else if (accion === 'venue-reject') await adminService.rejectVenue(item.id, motivo)
    verifications.value = verifications.value.filter(v => v.id !== item.id)
    pendingVenues.value = pendingVenues.value.filter(v => v.id !== item.id)
    modal.abierto = false
  } catch (e) {
    toast.error(e?.response?.data?.message || 'Error al procesar la accion')
  }
  modal.enviando = false
}
</script>
