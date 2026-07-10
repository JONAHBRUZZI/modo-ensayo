<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-2">Reagendamientos</h1>
    <p class="text-gray-500 text-sm mb-8">Clases marcadas como no realizadas. Reagéndalas antes de que venza el plazo o se reembolsará a los alumnos.</p>

    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>

    <div v-else-if="clases.length === 0" class="card text-center py-16">
      <p class="text-gray-400 font-medium">No tienes clases por reagendar.</p>
      <p class="text-gray-600 text-sm mt-1">Cuando una sede marque una clase tuya como no realizada, aparecerá aquí.</p>
    </div>

    <div v-else class="space-y-4">
      <div v-for="c in clases" :key="c.id" class="card">
        <div class="flex items-start justify-between flex-wrap gap-3">
          <div class="min-w-0">
            <h3 class="text-white font-semibold text-lg">{{ c.title }}</h3>
            <p class="text-gray-400 text-sm">{{ c.discipline }} {{ c.level ? '· ' + c.level : '' }}</p>
            <p class="text-gray-500 text-xs mt-1">Horario original: {{ formatDate(c.startTime) }}</p>
            <p v-if="c.room?.venue" class="text-gray-500 text-xs">{{ c.room.venue.name }}</p>
          </div>
          <div class="text-right">
            <p class="text-xs text-gray-500 uppercase tracking-wider">Plazo para reagendar</p>
            <p class="text-lg font-bold" :class="expirado(c) ? 'text-red-400' : 'text-yellow-400'">
              {{ deadlineTexto(c) }}
            </p>
          </div>
        </div>
        <div class="flex justify-end mt-4">
          <button v-if="!expirado(c)" @click="abrirReagendar(c)" class="btn-primary text-sm">Reagendar</button>
          <span v-else class="text-red-400 text-sm">Plazo vencido — se reembolsará a los alumnos.</span>
        </div>
      </div>
    </div>

    <!-- Modal: motivo obligatorio antes de ir a reservar la nueva sala -->
    <BottomSheet v-model="modalAbierto">
      <h3 class="text-lg font-semibold text-white mb-2">Reagendar "{{ claseSel?.title }}"</h3>
      <p class="text-sm text-gray-400 mb-4">
        A continuación reservarás y pagarás un nuevo horario de sala. Cuéntales a tus alumnos por qué se reagenda (obligatorio).
      </p>
      <label class="block text-sm font-medium text-gray-300 mb-1">Motivo del reagendamiento *</label>
      <textarea v-model="motivo" rows="3" maxlength="500" class="input-field w-full" placeholder="Ej. imprevisto de salud, corte de luz en la sala…"></textarea>
      <div class="flex justify-end gap-3 mt-4">
        <button @click="modalAbierto = false" class="text-sm text-gray-400 hover:text-white">Cancelar</button>
        <button @click="continuarAReserva" :disabled="!motivo.trim()" class="btn-primary text-sm disabled:opacity-50">
          Continuar a reservar sala
        </button>
      </div>
    </BottomSheet>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import classService from '@/services/classService'
import BottomSheet from '@/components/BottomSheet.vue'
import { useToast } from '@/composables/useToast'
import { formatDate } from '@/utils/dateFormatter'

const router = useRouter()
const toast = useToast()
const clases = ref([])
const loading = ref(true)
const ahora = ref(Date.now())
let timer = null

const modalAbierto = ref(false)
const claseSel = ref(null)
const motivo = ref('')

function expirado(c) {
  return !c.rescheduleDeadline || new Date(c.rescheduleDeadline).getTime() <= ahora.value
}

function deadlineTexto(c) {
  if (!c.rescheduleDeadline) return '—'
  const restante = new Date(c.rescheduleDeadline).getTime() - ahora.value
  if (restante <= 0) return 'Vencido'
  const h = Math.floor(restante / 3_600_000)
  const m = Math.floor((restante % 3_600_000) / 60_000)
  return `${h}h ${m}m`
}

function abrirReagendar(c) {
  claseSel.value = c
  motivo.value = ''
  modalAbierto.value = true
}

function continuarAReserva() {
  if (!motivo.value.trim()) return
  // Reusa el flujo de arriendo: la clase caída va como borradorId y el motivo
  // viaja para notificar a los alumnos cuando se materialice el pago.
  router.push({
    path: '/profesor/buscar-salas',
    query: { borradorId: claseSel.value.id, rescheduleReason: motivo.value.trim() }
  })
}

async function cargar() {
  loading.value = true
  try {
    const data = await classService.getMyRescheduleClasses()
    clases.value = Array.isArray(data) ? data : []
  } catch (err) {
    console.error('Error al cargar reagendamientos', err)
    toast.error('No se pudieron cargar los reagendamientos')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  cargar()
  timer = setInterval(() => { ahora.value = Date.now() }, 30_000)
})
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>
