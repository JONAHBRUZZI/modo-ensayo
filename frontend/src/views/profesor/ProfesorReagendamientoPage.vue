<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-2">Reagendar Clase</h1>
    <p class="text-gray-400 text-sm mb-8">Elige uno de los horarios disponibles de la sede y explica el motivo. Los alumnos serán notificados para aceptar o rechazar.</p>

    <div v-if="loading" class="card text-center py-10 text-gray-400">Cargando horarios disponibles...</div>

    <div v-else-if="slots.length === 0" class="card text-center py-10">
      <p class="text-gray-300 font-medium">No hay horarios disponibles</p>
      <p class="text-gray-500 text-sm mt-1">La sede no tiene bloques horarios libres para reagendar esta clase. Contacta a la sede para que habilite disponibilidad.</p>
    </div>

    <form v-else @submit.prevent="submit" class="card space-y-5">
      <div>
        <label class="block text-sm font-medium text-gray-300 mb-2">Nuevo horario *</label>
        <div class="space-y-2">
          <label v-for="s in slots" :key="s.id"
                 class="flex items-center gap-3 p-3 rounded-lg border cursor-pointer transition-colors"
                 :class="slotSel === s.id ? 'border-primary bg-primary/10' : 'border-white/10 hover:border-white/20'">
            <input type="radio" :value="s.id" v-model="slotSel" class="text-primary" />
            <div>
              <p class="text-white text-sm">{{ formatSlot(s.startTime, s.endTime) }}</p>
              <p class="text-gray-500 text-xs">{{ s.roomName }}</p>
            </div>
          </label>
        </div>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-300 mb-1">Motivo *</label>
        <textarea v-model="reason" rows="3" required class="input-field" placeholder="Explica el motivo del reagendamiento"></textarea>
      </div>

      <p v-if="msg" :class="msgType === 'error' ? 'text-red-400' : 'text-green-400'" class="text-sm">{{ msg }}</p>

      <button type="submit" :disabled="sending || !slotSel || !reason" class="btn-primary w-full">
        {{ sending ? 'Enviando...' : 'Solicitar Reagendamiento' }}
      </button>
    </form>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import rescheduleService from '@/services/rescheduleService'

const route = useRoute()
const router = useRouter()
const slots = ref([])
const slotSel = ref(null)
const reason = ref('')
const loading = ref(true)
const sending = ref(false)
const msg = ref('')
const msgType = ref('')

onMounted(async () => {
  try {
    const res = await rescheduleService.getAvailableSlots(route.params.claseId)
    slots.value = Array.isArray(res.data) ? res.data : []
  } catch {
    slots.value = []
  } finally {
    loading.value = false
  }
})

async function submit() {
  const slot = slots.value.find(s => s.id === slotSel.value)
  if (!slot) return
  sending.value = true
  msg.value = ''
  try {
    await rescheduleService.propose(route.params.claseId, slot.startTime, reason.value)
    msg.value = 'Reagendamiento solicitado. Los alumnos serán notificados para confirmar.'
    msgType.value = 'success'
    setTimeout(() => router.push('/profesor/clases-propias'), 1800)
  } catch (e) {
    msg.value = e.response?.data?.message || 'Error al solicitar reagendamiento'
    msgType.value = 'error'
  } finally {
    sending.value = false
  }
}

function formatSlot(start, end) {
  if (!start) return ''
  const s = new Date(start)
  const e = end ? new Date(end) : null
  const fecha = s.toLocaleDateString('es-CL', { weekday: 'long', day: 'numeric', month: 'long' })
  const horaIni = s.toLocaleTimeString('es-CL', { hour: '2-digit', minute: '2-digit' })
  const horaFin = e ? e.toLocaleTimeString('es-CL', { hour: '2-digit', minute: '2-digit' }) : ''
  return `${fecha}, ${horaIni}${horaFin ? ' - ' + horaFin : ''}`
}
</script>
