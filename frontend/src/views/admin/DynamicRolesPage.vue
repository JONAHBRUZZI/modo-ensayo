<template>
  <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Verificaciones de Identidad</h1>
    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>
    <div v-else-if="verifications.length === 0" class="card text-center py-12"><p class="text-gray-400">No hay verificaciones pendientes.</p></div>
    <div v-else class="space-y-4">
      <div v-for="v in verifications" :key="v.id" class="card">
        <div class="flex items-start justify-between">
          <div class="flex-1 space-y-2">
            <div class="flex items-center space-x-3">
              <div class="w-10 h-10 bg-primary/20 rounded-full flex items-center justify-center text-primary font-bold">{{ (v.fullName || v.userName || v.userEmail || 'U').charAt(0).toUpperCase() }}</div>
              <div>
                <h3 class="text-white font-medium">{{ v.fullName || v.userName || v.userEmail }}</h3>
                <p class="text-gray-400 text-sm">{{ v.documentType || 'RUT' }}: {{ v.documentNumber || 'No especificado' }}</p>
                <p v-if="v.birthDate" class="text-gray-500 text-xs">Nacimiento: {{ v.birthDate }}</p>
              </div>
            </div>
            <div class="flex items-center space-x-3 pl-13">
              <a v-if="v.documentUrl" :href="v.documentUrl" target="_blank" class="text-xs text-indigo-400 hover:text-indigo-300 underline">Ver documento adjunto</a>
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
    <div v-if="pendingVenues.length === 0" class="card text-center py-8"><p class="text-gray-400">No hay sedes pendientes.</p></div>
    <div v-else class="space-y-4">
      <div v-for="v in pendingVenues" :key="v.id" class="card flex items-center justify-between">
        <div><h3 class="text-white font-medium">{{ v.name }}</h3><p class="text-gray-400 text-sm">{{ v.city }}, {{ v.address }}</p></div>
        <div class="flex space-x-2">
          <button @click="abrirModal(v, 'venue-approve')" class="btn-primary text-sm bg-green-600 hover:bg-green-700">Aprobar</button>
          <button @click="abrirModal(v, 'venue-reject')" class="btn-danger text-sm">Rechazar</button>
        </div>
      </div>
    </div>

    <div v-if="modal.abierto" class="fixed inset-0 bg-black/60 flex items-center justify-center z-50" @click.self="modal.abierto = false">
      <div class="bg-[#1a1d2e] rounded-2xl border border-white/10 p-6 max-w-md w-full mx-4">
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
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import adminService from '@/services/adminService'
import EstadoBadge from '@/components/EstadoBadge.vue'

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

function abrirModal(item, accion) {
  modal.item = item
  modal.accion = accion
  modal.motivo = ''
  modal.enviando = false
  modal.esAprobacion = accion.endsWith('approve')
  if (accion === 'identity-approve') {
    modal.titulo = 'Aprobar identidad'
    modal.mensaje = `¿Confirma su respuesta? Se aprobara la identidad de ${item.userName || item.userEmail}. El usuario recibira el rol solicitado.`
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
    alert(e?.response?.data?.message || 'Error al procesar la accion')
  }
  modal.enviando = false
}
</script>
