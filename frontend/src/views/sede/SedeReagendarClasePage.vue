<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-white">Reagendar clase</h1>
      <router-link to="/sede/clases-por-confirmar" class="text-sm text-gray-400 hover:text-white">← Volver</router-link>
    </div>

    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>

    <div v-else class="space-y-6">
      <div class="card">
        <p class="text-gray-500 text-xs uppercase tracking-wider mb-1">Clase</p>
        <p class="text-white font-semibold">{{ clase?.title || 'Clase' }}</p>
        <p v-if="clase?.startTime" class="text-gray-500 text-xs mt-1">Horario original: {{ formatDate(clase.startTime) }}</p>
      </div>

      <!-- Sala -->
      <div class="card">
        <label class="block text-sm font-medium text-gray-300 mb-2">Sala</label>
        <select v-model="roomId" @change="cargarBloques" class="input-field">
          <option value="">Selecciona una sala</option>
          <option v-for="r in rooms" :key="r.id" :value="r.id">{{ r.name }}</option>
        </select>
      </div>

      <!-- Horarios disponibles -->
      <div v-if="roomId" class="card">
        <label class="block text-sm font-medium text-gray-300 mb-3">Nuevo horario</label>
        <div v-if="cargandoBloques" class="text-gray-500 text-sm py-4">Cargando horarios...</div>
        <div v-else-if="bloquesPorDia.length === 0" class="text-gray-500 text-sm py-4">Esta sala no tiene horarios disponibles próximos.</div>
        <div v-else class="space-y-4 max-h-96 overflow-y-auto pr-1">
          <div v-for="dia in bloquesPorDia" :key="dia.fecha">
            <p class="text-gray-400 text-xs font-medium mb-2">{{ dia.label }}</p>
            <div class="flex flex-wrap gap-2">
              <button
                v-for="b in dia.bloques"
                :key="b.id"
                type="button"
                @click="toggleBloque(b.id)"
                :class="seleccionados.includes(b.id) ? 'bg-primary text-white border-primary' : 'text-gray-300 border-white/10 hover:border-primary/50'"
                class="text-xs px-3 py-1.5 rounded-lg border transition-colors"
              >
                {{ hora(b.startTime) }}–{{ hora(b.endTime) }}
              </button>
            </div>
          </div>
        </div>
        <p v-if="seleccionados.length" class="text-primary text-xs mt-3">{{ seleccionados.length }} bloque(s) seleccionado(s)</p>
      </div>

      <!-- Motivo -->
      <div class="card">
        <label class="block text-sm font-medium text-gray-300 mb-1">Motivo del reagendamiento *</label>
        <textarea v-model="motivo" rows="3" maxlength="500" class="input-field w-full" placeholder="Se notificará a los alumnos con este motivo."></textarea>
      </div>

      <button @click="pedirConfirmar" :disabled="!puedeConfirmar" class="btn-primary w-full disabled:opacity-50">
        Reagendar
      </button>
    </div>

    <BottomSheet v-model="confirmAbierto">
      <h3 class="text-lg font-semibold text-white mb-2">¿Seguro del horario?</h3>
      <p class="text-sm text-gray-400 mb-4">
        Se reagendará la clase al horario seleccionado y se notificará a los alumnos (aceptan o rechazan, con reembolso si rechazan) y al profesor.
      </p>
      <div class="flex justify-end gap-3">
        <button @click="confirmAbierto = false" class="text-sm text-gray-400 hover:text-white">Revisar nuevamente</button>
        <button @click="confirmar" :disabled="guardando" class="btn-primary text-sm disabled:opacity-50">
          {{ guardando ? 'Reagendando...' : 'Estoy seguro' }}
        </button>
      </div>
    </BottomSheet>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import venueService from '@/services/venueService'
import scheduleService from '@/services/scheduleService'
import BottomSheet from '@/components/BottomSheet.vue'
import { useToast } from '@/composables/useToast'
import { formatDate } from '@/utils/dateFormatter'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const loading = ref(true)
const clase = ref(null)
const rooms = ref([])
const roomId = ref('')
const bloques = ref([])
const cargandoBloques = ref(false)
const seleccionados = ref([])
const motivo = ref('')
const confirmAbierto = ref(false)
const guardando = ref(false)

const puedeConfirmar = computed(() => roomId.value && seleccionados.value.length > 0 && motivo.value.trim())

const bloquesPorDia = computed(() => {
  const map = {}
  for (const b of bloques.value) {
    const fecha = (b.startTime || '').slice(0, 10)
    if (!map[fecha]) map[fecha] = { fecha, label: etiquetaDia(b.startTime), bloques: [] }
    map[fecha].bloques.push(b)
  }
  return Object.values(map)
})

function hora(iso) {
  return new Date(iso).toLocaleTimeString('es-CL', { hour: '2-digit', minute: '2-digit' })
}
function etiquetaDia(iso) {
  return new Date(iso).toLocaleDateString('es-CL', { weekday: 'long', day: 'numeric', month: 'long' })
}
function toggleBloque(id) {
  const i = seleccionados.value.indexOf(id)
  if (i >= 0) seleccionados.value.splice(i, 1)
  else seleccionados.value.push(id)
}

async function cargarBloques() {
  seleccionados.value = []
  if (!roomId.value) return
  cargandoBloques.value = true
  try {
    const desde = new Date().toISOString()
    const hasta = new Date(Date.now() + 30 * 24 * 3600 * 1000).toISOString()
    const data = await scheduleService.getRoomSchedule(roomId.value, desde, hasta)
    const ahora = Date.now()
    bloques.value = (Array.isArray(data) ? data : [])
      .filter(b => b.status === 'AVAILABLE' && new Date(b.startTime).getTime() > ahora)
  } catch (err) {
    console.error('Error al cargar horarios', err)
    bloques.value = []
  } finally {
    cargandoBloques.value = false
  }
}

function pedirConfirmar() {
  if (puedeConfirmar.value) confirmAbierto.value = true
}

async function confirmar() {
  guardando.value = true
  try {
    await venueService.sedeRescheduleClass(route.params.classId, roomId.value, seleccionados.value, motivo.value.trim())
    toast.success('Clase reagendada. Se notificó a los alumnos y al profesor.')
    router.push('/sede/clases-por-confirmar')
  } catch (err) {
    toast.error(err?.response?.data?.error || 'No se pudo reagendar la clase')
    guardando.value = false
    confirmAbierto.value = false
  }
}

onMounted(async () => {
  try {
    // Detalle de la clase (título) + salas de la sede.
    try { clase.value = await venueService.getVenueClassDetail(route.params.classId) } catch { /* opcional */ }
    const venues = await venueService.getMyVenues()
    const aprobada = (Array.isArray(venues) ? venues : []).find(v => v.status === 'APROBADA') || venues?.[0]
    if (aprobada) {
      const rs = await venueService.getVenueRooms(aprobada.id)
      rooms.value = Array.isArray(rs) ? rs : []
    }
  } catch (err) {
    console.error('Error al iniciar reagendamiento de sede', err)
    toast.error('No se pudo cargar la información')
  } finally {
    loading.value = false
  }
})
</script>
