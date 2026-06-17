<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Reagendar Clase</h1>
    <form @submit.prevent="submit" class="card space-y-4">
      <p class="text-gray-400">Clase ID: {{ $route.params.claseId }}</p>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Nueva Fecha</label><input type="datetime-local" v-model="newDate" required class="input-field" /></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Nueva Sala</label><select v-model="roomId" required class="input-field"><option value="">Seleccionar</option><option v-for="r in rooms" :key="r.id" :value="r.id">{{ r.name }}</option></select></div>
      <p v-if="msg" class="text-green-400 text-sm">{{ msg }}</p>
      <button type="submit" :disabled="sending" class="btn-primary w-full">{{ sending ? 'Enviando...' : 'Reagendar' }}</button>
    </form>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import venueService from '@/services/venueService'
import rescheduleService from '@/services/rescheduleService'

const route = useRoute()
const newDate = ref('')
const roomId = ref('')
const reason = ref('')
const rooms = ref([])
const sending = ref(false)
const msg = ref('')

onMounted(async () => {
  try {
    const venues = await venueService.getMyVenues()
    const vArr = Array.isArray(venues) ? venues : venues.content || []
    const sede = vArr.find(v => v.status === 'APROBADA') || vArr[0]
    if (sede) rooms.value = await venueService.getVenueRooms(sede.id)
  } catch { rooms.value = [] }
})

async function submit() {
  sending.value = true
  try {
    await rescheduleService.propose(route.params.claseId, new Date(newDate.value).toISOString(), reason.value || 'Reagendamiento solicitado por administración de sede')
    msg.value = 'Reagendamiento solicitado. Los alumnos seran notificados.'
  } catch (e) {
    msg.value = e.response?.data?.message || 'Error al solicitar reagendamiento'
  }
  sending.value = false
}
</script>
