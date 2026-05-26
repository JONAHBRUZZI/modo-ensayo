<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Clases por Confirmar</h1>
    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>
    <div v-else-if="clases.length === 0" class="card text-center py-12"><p class="text-gray-400">No hay clases pendientes de confirmacion.</p></div>
    <div v-else class="space-y-4">
      <div v-for="c in clases" :key="c.id" class="card flex items-center justify-between">
        <div>
          <h3 class="text-white font-medium">{{ c.title }}</h3>
          <p class="text-gray-400 text-sm">{{ c.discipline }} - {{ c.roomName }}</p>
          <p class="text-gray-500 text-xs">{{ formatDate(c.startTime) }}</p>
          <p class="text-gray-600 text-xs mt-1">Asistencia: {{ c.attendanceCount }} / {{ c.capacity }} | Precio: ${{ c.price?.toLocaleString() }}</p>
        </div>
        <div class="flex space-x-2">
          <button @click="abrirConfirmacion(c, true)" class="btn-primary text-sm bg-green-600 hover:bg-green-700">Realizada</button>
          <button @click="abrirConfirmacion(c, false)" class="btn-danger text-sm">No Realizada</button>
        </div>
      </div>
    </div>

    <div v-if="modal.abierto" class="fixed inset-0 bg-black/60 flex items-center justify-center z-50" @click.self="modal.abierto = false">
      <div class="bg-[#1a1d2e] rounded-2xl border border-white/10 p-6 max-w-md w-full mx-4">
        <h3 class="text-lg font-semibold text-white mb-2">{{ modal.titulo }}</h3>
        <p class="text-gray-400 text-sm mb-6">{{ modal.mensaje }}</p>
        <div class="flex space-x-3 justify-end">
          <button @click="modal.abierto = false" class="px-4 py-2 rounded-xl border border-white/10 text-gray-300 hover:bg-white/5 text-sm">Cancelar</button>
          <button @click="ejecutarConfirmacion" :disabled="modal.enviando" class="px-4 py-2 rounded-xl text-white text-sm font-medium" :class="modal.confirmandoRealizada ? 'bg-green-600 hover:bg-green-500' : 'bg-red-600 hover:bg-red-500'">
            {{ modal.enviando ? 'Procesando...' : 'Si, confirmar' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import venueService from '@/services/venueService'

const clases = ref([])
const loading = ref(true)
const modal = reactive({ abierto: false, titulo: '', mensaje: '', claseId: null, confirmandoRealizada: false, enviando: false })

onMounted(async () => {
  try { clases.value = await venueService.getPendingClasses() } catch { clases.value = [] }
  loading.value = false
})

function abrirConfirmacion(c, realizada) {
  modal.claseId = c.id
  modal.confirmandoRealizada = realizada
  if (realizada) {
    modal.titulo = 'Confirmar clase realizada'
    modal.mensaje = `¿Confirma su respuesta? La clase "${c.title}" sera marcada como REALIZADA y los pagos de ${c.price ? '$'+c.price.toLocaleString() : ''} seran LIBERADOS al profesor. Esta accion es irreversible.`
  } else {
    modal.titulo = 'Confirmar clase NO realizada'
    modal.mensaje = `¿Confirma su respuesta? La clase "${c.title}" sera marcada como NO REALIZADA. Se notificara al profesor para reagendar y los pagos podrian pasar a devolucion. Esta accion es irreversible.`
  }
  modal.abierto = true
}

async function ejecutarConfirmacion() {
  modal.enviando = true
  try {
    if (modal.confirmandoRealizada) await venueService.confirmClassRealized(modal.claseId)
    else await venueService.confirmClassNotRealized(modal.claseId)
    clases.value = clases.value.filter(c => c.id !== modal.claseId)
    modal.abierto = false
  } catch {}
  modal.enviando = false
}

function formatDate(d) { return d ? new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : '' }
</script>
