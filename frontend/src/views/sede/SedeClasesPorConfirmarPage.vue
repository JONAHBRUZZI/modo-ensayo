<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-8">Clases por Confirmar</h1>
    <div v-if="feedback.tipo" :class="['mb-4 px-4 py-3 rounded-xl text-sm', feedback.tipo === 'success' ? 'bg-green-500/10 border border-green-500/20 text-green-400' : 'bg-red-500/10 border border-red-500/20 text-red-400']">{{ feedback.mensaje }}</div>
    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>
    <div v-else-if="clases.length === 0" class="card text-center py-16">
      <div class="w-14 h-14 bg-[var(--bg-elevated)] rounded-2xl flex items-center justify-center mx-auto mb-4">
        <svg class="w-7 h-7 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
      </div>
      <p class="text-gray-400 font-medium">Todo al día</p>
      <p class='text-gray-600 text-sm mt-1'>No hay clases pendientes de confirmación.</p>
    </div>
    <div v-else class="space-y-4">
      <!-- Tarjeta clickeable: abre el detalle. Los botones detienen la propagación. -->
      <div v-for="c in clases" :key="c.id"
           @click="abrirDetalle(c)"
           class="card flex items-center justify-between gap-4 flex-wrap cursor-pointer hover:border-primary/50 transition-colors">
        <div class="min-w-0">
          <div class="flex items-center gap-2 flex-wrap">
            <h3 class="text-white font-medium">{{ c.title }}</h3>
            <span :class="['text-[11px] px-2 py-0.5 rounded-full', tipoInfo(c.tipoClase).clase]">{{ tipoInfo(c.tipoClase).label }}</span>
          </div>
          <p class="text-gray-400 text-sm">{{ c.discipline }} · {{ c.roomName }}</p>
          <p class="text-gray-500 text-xs">{{ formatDate(c.startTime) }}<span v-if="c.teacherName"> · {{ c.teacherName }}</span></p>
          <p class="text-gray-600 text-xs mt-1">Asistencia: {{ c.attendanceCount }} / {{ c.capacity }} · Precio: ${{ c.price?.toLocaleString() }}</p>
        </div>
        <div class="flex space-x-2 flex-shrink-0">
          <button @click.stop="abrirConfirmacion(c, true)" class="btn-primary text-sm bg-green-600 hover:bg-green-700">Realizada</button>
          <button @click.stop="abrirConfirmacion(c, false)" class="btn-danger text-sm">No Realizada</button>
        </div>
      </div>
    </div>

    <!-- Modal de DETALLE de la clase -->
    <BottomSheet v-model="detalle.abierto">
      <div v-if="detalle.cargando" class="text-center text-gray-500 py-10">
        <div class="inline-block w-5 h-5 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-2"></div>
        <p class="text-sm">Cargando detalle...</p>
      </div>
      <template v-else-if="detalle.clase">
        <div class="flex items-start justify-between gap-3 mb-1">
          <h3 class="text-xl font-bold text-white">{{ detalle.clase.title }}</h3>
          <span :class="['text-[11px] px-2 py-0.5 rounded-full whitespace-nowrap', tipoInfo(detalle.clase.tipoClase).clase]">{{ tipoInfo(detalle.clase.tipoClase).label }}</span>
        </div>
        <p class="text-gray-400 text-sm mb-4">{{ detalle.clase.discipline }} · {{ detalle.clase.level }}</p>

        <p v-if="detalle.clase.description" class="text-gray-300 text-sm mb-4">{{ detalle.clase.description }}</p>

        <!-- Origen de la clase -->
        <div class="rounded-lg bg-[var(--bg-elevated)] border border-white/5 p-3 mb-4 text-sm">
          <p class="text-gray-300">
            <span class="text-gray-500">{{ detalle.clase.tipoClase === 'ASIGNADA' ? 'Maestro de la sede:' : 'Profesor independiente:' }}</span>
            {{ detalle.clase.teacherName || detalle.clase.teacherEmail || 'Sin nombre' }}
          </p>
          <p class="text-gray-500 text-xs mt-0.5">
            {{ detalle.clase.tipoClase === 'ASIGNADA'
                ? 'Clase creada por tu sede y asignada a un maestro dependiente.'
                : 'Clase creada por un profesor independiente que arrendó tu sala.' }}
          </p>
        </div>

        <!-- Datos operativos -->
        <div class="grid grid-cols-2 gap-3 mb-4 text-sm">
          <div><p class="text-gray-500 text-xs">Sala</p><p class="text-gray-200">{{ detalle.clase.roomName }}</p></div>
          <div><p class="text-gray-500 text-xs">Fecha</p><p class="text-gray-200">{{ formatDate(detalle.clase.startTime) }}</p></div>
          <div><p class="text-gray-500 text-xs">Duración</p><p class="text-gray-200">{{ detalle.clase.duration }} min</p></div>
          <div><p class="text-gray-500 text-xs">Cupos</p><p class="text-gray-200">{{ detalle.clase.enrolledCount }} / {{ detalle.clase.capacity }}</p></div>
          <div><p class="text-gray-500 text-xs">Precio alumno</p><p class="text-gray-200">${{ Number(detalle.clase.price || 0).toLocaleString('es-CL') }}</p></div>
          <div v-if="detalle.clase.tipoClase === 'ASIGNADA' && detalle.clase.honorario != null">
            <p class="text-gray-500 text-xs">Honorario profe</p><p class="text-gray-200">${{ Number(detalle.clase.honorario).toLocaleString('es-CL') }}</p>
          </div>
        </div>

        <!-- Alumnos inscritos -->
        <div class="mb-6">
          <p class="text-gray-300 text-sm font-medium mb-2">Alumnos inscritos ({{ detalle.alumnos.length }})</p>
          <div v-if="detalle.alumnos.length === 0" class="text-gray-500 text-sm">Sin inscritos.</div>
          <div v-else class="space-y-1 max-h-48 overflow-y-auto">
            <div v-for="a in detalle.alumnos" :key="a.enrollmentId" class="flex items-center justify-between text-sm border-t border-white/5 py-1.5 first:border-0">
              <span class="text-gray-300 truncate">{{ a.attendeeName }}</span>
              <span v-if="a.present === true" class="text-green-400 text-xs">Presente</span>
              <span v-else-if="a.present === false" class="text-red-400 text-xs">Ausente</span>
              <span v-else class="text-gray-600 text-xs">Sin marcar</span>
            </div>
          </div>
        </div>

        <!-- Acciones -->
        <div class="flex gap-3">
          <button @click="abrirConfirmacion(detalle.clase, true)" class="flex-1 px-4 py-2 rounded-lg bg-green-600 hover:bg-green-500 text-white font-medium">Realizada</button>
          <button @click="abrirConfirmacion(detalle.clase, false)" class="flex-1 px-4 py-2 rounded-lg bg-red-600 hover:bg-red-500 text-white font-medium">No Realizada</button>
        </div>
      </template>
    </BottomSheet>

    <!-- Modal de CONFIRMACIÓN -->
    <BottomSheet v-model="modal.abierto">
      <h3 class="text-lg font-semibold text-white mb-2">{{ modal.titulo }}</h3>
      <p class="text-gray-400 text-sm mb-6">{{ modal.mensaje }}</p>
      <div class="flex space-x-3 justify-end">
        <button @click="modal.abierto = false" class="px-4 py-2 rounded-xl border border-white/10 text-gray-300 hover:bg-white/5 text-sm">Cancelar</button>
        <button @click="ejecutarConfirmacion" :disabled="modal.enviando" class="px-4 py-2 rounded-xl text-white text-sm font-medium" :class="modal.confirmandoRealizada ? 'bg-green-600 hover:bg-green-500' : 'bg-red-600 hover:bg-red-500'">
          {{ modal.enviando ? 'Procesando...' : 'Si, confirmar' }}
        </button>
      </div>
    </BottomSheet>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import venueService from '@/services/venueService'
import { formatDate } from '@/utils/dateFormatter'
import BottomSheet from '@/components/BottomSheet.vue'

const clases = ref([])
const loading = ref(true)
const modal = reactive({ abierto: false, titulo: '', mensaje: '', claseId: null, confirmandoRealizada: false, enviando: false })
const detalle = reactive({ abierto: false, cargando: false, clase: null, alumnos: [] })
const feedback = ref({ tipo: '', mensaje: '' })

onMounted(async () => {
  try { clases.value = await venueService.getPendingClasses() } catch { clases.value = [] }
  loading.value = false
})

// Distingue clase de la propia sede (ASIGNADA) vs. profesor independiente (PROPIA).
function tipoInfo(tipoClase) {
  if (tipoClase === 'ASIGNADA') return { label: 'De mi sede', clase: 'bg-blue-500/20 text-blue-300' }
  return { label: 'Profesor independiente', clase: 'bg-purple-500/20 text-purple-300' }
}

async function abrirDetalle(c) {
  detalle.abierto = true
  detalle.cargando = true
  detalle.clase = null
  detalle.alumnos = []
  try {
    const [info, alumnos] = await Promise.all([
      venueService.getVenueClassDetail(c.id),
      venueService.getVenueClassStudents(c.id)
    ])
    detalle.clase = info
    detalle.alumnos = alumnos
  } catch (e) {
    feedback.value = { tipo: 'error', mensaje: e?.response?.data?.message || 'No se pudo cargar el detalle de la clase.' }
    detalle.abierto = false
  }
  detalle.cargando = false
}

function abrirConfirmacion(c, realizada) {
  feedback.value = { tipo: '', mensaje: '' }
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
    detalle.abierto = false
    feedback.value = { tipo: 'success', mensaje: modal.confirmandoRealizada ? 'Clase confirmada como realizada.' : 'Clase marcada como no realizada.' }
    setTimeout(() => feedback.value = { tipo: '', mensaje: '' }, 4000)
  } catch (e) {
    feedback.value = { tipo: 'error', mensaje: e?.response?.data?.message || 'Error al confirmar la clase.' }
  }
  modal.enviando = false
}
</script>
