<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Agendar Sala y Crear Clase</h1>
    <form @submit.prevent="handleCreate" class="card space-y-4">
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Titulo</label><input v-model="form.title" required class="input-field" /></div>
      <div class="grid grid-cols-2 gap-4">
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Disciplina</label><select v-model="form.discipline" required class="input-field"><option value="">Seleccionar</option><option>CUECA</option><option>BALLET</option><option>DANZA</option><option>TEATRO</option><option>CANTO</option><option>GUITARRA</option><option>BATERIA</option><option>BAJO</option><option>PIANO</option><option>VIOLIN</option><option>SAXOFON</option><option>OTRO</option></select></div>
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Nivel</label><select v-model="form.level" required class="input-field"><option value="">Seleccionar</option><option>BASICO</option><option>INTERMEDIO</option><option>AVANZADO</option></select></div>
      </div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Descripcion</label><textarea v-model="form.description" rows="3" class="input-field"></textarea></div>
      <div class="grid grid-cols-3 gap-4">
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Capacidad</label><input type="number" v-model.number="form.capacity" min="1" required class="input-field" /></div>
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Duracion (min)</label><input type="number" v-model.number="form.duration" min="30" required class="input-field" /></div>
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Precio</label><input type="number" v-model.number="form.price" min="0" required class="input-field" /></div>
      </div>
      <div class="grid grid-cols-2 gap-4">
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Edad Minima</label><input type="number" v-model.number="form.minAge" min="0" class="input-field" /></div>
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Edad Maxima</label><input type="number" v-model.number="form.maxAge" min="0" class="input-field" /></div>
      </div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Fecha y Hora</label><input type="datetime-local" v-model="form.startTime" required class="input-field" /></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Sede</label><select v-model="form.venueId" required class="input-field"><option value="">Seleccionar sede</option><option v-for="v in venues" :key="v.id" :value="v.id">{{ v.name }}</option></select></div>
      <div v-if="form.venueId"><label class="block text-sm font-medium text-gray-300 mb-1">Sala</label><select v-model="form.roomId" required class="input-field"><option value="">Seleccionar sala</option><option v-for="r in rooms" :key="r.id" :value="r.id">{{ r.name }} (cap: {{ r.capacity }})</option></select></div>
      <p v-if="error" class="text-red-400 text-sm">{{ error }}</p>
      <button type="submit" :disabled="creating" class="btn-primary w-full">{{ creating ? 'Creando...' : 'Agendar Sala y Crear Clase' }}</button>
    </form>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import classService from '@/services/classService'
import { useAuth } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const { syncActividadMaestro, refreshProfile } = useAuth()
const form = ref({ title: '', discipline: '', level: '', description: '', capacity: 10, duration: 60, price: 0, minAge: 0, maxAge: 99, startTime: '', venueId: '', roomId: '' })
const venues = ref([])
const rooms = ref([])
const error = ref('')
const creating = ref(false)

onMounted(async () => {
  try { venues.value = await classService.getVenues() } catch { venues.value = [] }
  if (route.query.roomId) form.value.roomId = route.query.roomId
  if (route.query.venueId) {
    form.value.venueId = route.query.venueId
    try { rooms.value = await classService.getVenueRooms(route.query.venueId) } catch { rooms.value = [] }
  }
  if (route.query.startTime) form.value.startTime = route.query.startTime
})

watch(() => form.value.venueId, async (id) => {
  if (!id) { rooms.value = []; return }
  try { rooms.value = await classService.getVenueRooms(id) } catch { rooms.value = [] }
})

async function handleCreate() {
  error.value = ''
  creating.value = true
  try {
    await classService.createClass(form.value)
    await refreshProfile()
    await syncActividadMaestro()
    router.push('/profesor/clases-propias')
  } catch (e) {
    error.value = e.response?.data?.message || 'Error al crear clase'
  } finally {
    creating.value = false
  }
}
</script>
