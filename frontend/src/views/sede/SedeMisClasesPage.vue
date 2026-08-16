<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-8">Clases de mi Sede</h1>
    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>
    <div v-else-if="clases.length === 0" class="card text-center py-16">
      <div class="w-14 h-14 bg-[var(--bg-elevated)] rounded-2xl flex items-center justify-center mx-auto mb-4">
        <svg class="w-7 h-7 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
        </svg>
      </div>
      <p class="text-gray-400 font-medium">Sin clases programadas</p>
      <p class='text-gray-600 text-sm mt-1'>Crea una clase para que aparezca aquí.</p>
      <router-link to="/sede/crear-clase" class="btn-primary inline-flex mt-4">Crear clase</router-link>
    </div>
    <div v-else class="space-y-4">
      <button
        v-for="c in clases"
        :key="c.id"
        type="button"
        @click="abrirDetalle(c)"
        class="card w-full text-left flex items-center justify-between hover:border-primary/50 transition-colors"
      >
        <div>
          <h3 class="text-white font-medium">{{ c.title }}</h3>
          <p class="text-gray-400 text-sm">{{ c.discipline }} — {{ c.roomName }}</p>
          <p class="text-gray-500 text-xs">{{ formatDate(c.startTime) }}</p>
        </div>
        <div class="flex items-center gap-3">
          <EstadoBadge :status="c.status" />
          <svg class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
        </div>
      </button>
    </div>

    <!-- Detalle de la clase -->
    <BottomSheet v-model="modalAbierto">
      <div v-if="detalleLoading" class="text-gray-500 text-sm py-6 text-center">Cargando detalle...</div>
      <div v-else-if="detalle">
        <div class="flex items-start justify-between gap-3 mb-4">
          <div>
            <h3 class="text-xl font-semibold text-white">{{ detalle.title }}</h3>
            <p class="text-gray-400 text-sm">{{ detalle.discipline }} · {{ detalle.level }}</p>
          </div>
          <EstadoBadge :status="detalle.status" />
        </div>

        <!-- Datos de la clase -->
        <div class="grid grid-cols-2 gap-x-4 gap-y-3 text-sm mb-5">
          <div><span class="text-gray-500">Sala:</span> <span class="text-white">{{ detalle.roomName }}</span></div>
          <div><span class="text-gray-500">Sede:</span> <span class="text-white">{{ detalle.venueName }}</span></div>
          <div><span class="text-gray-500">Fecha:</span> <span class="text-white">{{ detalle.startTime ? formatDate(detalle.startTime) : '—' }}</span></div>
          <div><span class="text-gray-500">Hora:</span> <span class="text-white">{{ detalle.startTime ? formatTime(detalle.startTime) : '—' }}<span v-if="detalle.endTime"> – {{ formatTime(detalle.endTime) }}</span></span></div>
          <div><span class="text-gray-500">Duración:</span> <span class="text-white">{{ detalle.duration || 0 }} min</span></div>
          <div><span class="text-gray-500">Precio alumno:</span> <span class="text-white">${{ Number(detalle.price || 0).toLocaleString('es-CL') }}</span></div>
          <div v-if="detalle.tipoClase === 'ASIGNADA'"><span class="text-gray-500">Honorario profe:</span> <span class="text-white">${{ Number(detalle.honorario || 0).toLocaleString('es-CL') }}</span></div>
          <div><span class="text-gray-500">Cupo:</span> <span class="text-white">{{ detalle.enrolledCount || 0 }} / {{ detalle.capacity }}</span></div>
        </div>

        <div v-if="detalle.description" class="mb-5">
          <p class="text-gray-500 text-xs uppercase tracking-wider mb-1">Descripción</p>
          <p class="text-gray-300 text-sm">{{ detalle.description }}</p>
        </div>

        <!-- Profesor -->
        <div class="mb-5 rounded-xl bg-[var(--bg-elevated)] p-3">
          <p class="text-gray-500 text-xs uppercase tracking-wider mb-1">Profesor que realiza la clase</p>
          <p class="text-white text-sm font-medium">{{ detalle.teacherName || 'Sin asignar' }}</p>
          <p v-if="detalle.teacherEmail" class="text-gray-400 text-xs">{{ detalle.teacherEmail }}</p>
        </div>

        <!-- Alumnos -->
        <div>
          <p class="text-gray-500 text-xs uppercase tracking-wider mb-2">
            Alumnos ({{ alumnos.length }})
          </p>
          <div v-if="alumnos.length === 0" class="text-gray-500 text-sm py-2">Aún no hay alumnos inscritos.</div>
          <div v-else class="space-y-2">
            <div v-for="al in alumnos" :key="al.enrollmentId" class="flex items-center justify-between border-t border-white/5 pt-2 first:border-0 first:pt-0">
              <div>
                <p class="text-white text-sm">{{ al.attendeeName }}</p>
                <p class="text-gray-500 text-xs">
                  {{ al.studentEmail }}
                  <span v-if="al.beneficiaryType && al.beneficiaryType !== 'SELF'"> · para {{ al.beneficiaryType }}</span>
                </p>
              </div>
              <span
                :class="[
                  'text-xs px-2 py-0.5 rounded-full',
                  al.present === true ? 'bg-green-500/20 text-green-400'
                  : al.present === false ? 'bg-red-500/20 text-red-400'
                  : 'bg-white/5 text-gray-400'
                ]"
              >
                {{ al.present === true ? 'Presente' : al.present === false ? 'Ausente' : 'Inscrito' }}
              </span>
            </div>
          </div>
        </div>
        <!-- R19: cancelar arriendo pagado por el profesor (no aplica a clases ASIGNADA, esas no tienen pago de sala) -->
        <div v-if="detalle.tipoClase === 'PROPIA' && !['CANCELLED', 'COMPLETED', 'SUSPENDED'].includes(detalle.status)" class="mt-6 pt-4 border-t border-white/5">
          <button
            :disabled="cancelando"
            class="text-red-400 hover:text-red-300 text-sm transition-colors"
            @click="cancelarArriendo"
          >
            {{ cancelando ? 'Procesando...' : 'Cancelar arriendo y reembolsar' }}
          </button>
          <p class="text-gray-600 text-xs mt-1">Reembolso total. Solo hasta 24h antes del horario y sin alumnos inscritos.</p>
        </div>
      </div>
      <div v-else class="text-gray-500 text-sm py-6 text-center">No se pudo cargar el detalle.</div>
    </BottomSheet>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import venueService from '@/services/venueService'
import EstadoBadge from '@/components/EstadoBadge.vue'
import BottomSheet from '@/components/BottomSheet.vue'
import { formatDate, formatTime } from '@/utils/dateFormatter'
import { useToast } from '@/composables/useToast'

const toast = useToast()
const clases = ref([])
const loading = ref(true)

const modalAbierto = ref(false)
const detalle = ref(null)
const alumnos = ref([])
const detalleLoading = ref(false)
const cancelando = ref(false)

onMounted(async () => {
  try {
    const data = await venueService.getVenueClasses()
    clases.value = Array.isArray(data) ? data : []
  } catch { clases.value = [] }
  loading.value = false
})

async function abrirDetalle(c) {
  modalAbierto.value = true
  detalleLoading.value = true
  detalle.value = null
  alumnos.value = []
  try {
    const [d, als] = await Promise.all([
      venueService.getVenueClassDetail(c.id),
      venueService.getVenueClassStudents(c.id)
    ])
    detalle.value = d
    alumnos.value = Array.isArray(als) ? als : []
  } catch {
    detalle.value = null
  }
  detalleLoading.value = false
}

async function cancelarArriendo() {
  if (!detalle.value) return
  cancelando.value = true
  try {
    await venueService.cancelRoomReservation(detalle.value.id)
    toast.success('Arriendo cancelado. El reembolso ya fue procesado.')
    clases.value = clases.value.filter(c => c.id !== detalle.value.id)
    modalAbierto.value = false
  } catch (e) {
    toast.error(e?.response?.data?.error || e?.message || 'No se pudo cancelar el arriendo')
  }
  cancelando.value = false
}
</script>
