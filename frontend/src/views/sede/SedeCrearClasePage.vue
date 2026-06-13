<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Crear Clase en Sede</h1>
    <form @submit.prevent="handleCreate" class="card space-y-4">
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Titulo</label><input v-model="form.title" required class="input-field" /></div>
      <div class="grid grid-cols-2 gap-4">
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Disciplina</label><select v-model="form.discipline" required class="input-field"><option value="">Seleccionar</option><option>Guitarra</option><option>Bateria</option><option>Bajo</option><option>Canto</option><option>Piano</option><option>Violin</option><option>Otro</option></select></div>
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Nivel</label><select v-model="form.level" required class="input-field"><option value="">Seleccionar</option><option value="BASICO">Básico</option><option value="INTERMEDIO">Intermedio</option><option value="AVANZADO">Avanzado</option></select></div>
      </div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Descripcion</label><textarea v-model="form.description" rows="3" class="input-field"></textarea></div>
      <div class="grid grid-cols-3 gap-4">
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Capacidad</label><input type="number" v-model.number="form.capacity" min="1" required class="input-field" /></div>
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Duracion (min)</label><input type="number" v-model.number="form.duration" min="30" required class="input-field" /></div>
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Precio</label><input type="number" v-model.number="form.price" min="0" required class="input-field" /></div>
      </div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Fecha y Hora</label><input type="datetime-local" v-model="form.startTime" required class="input-field" /></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Sala</label><select v-model="form.roomId" required class="input-field"><option value="">Seleccionar sala</option><option v-for="r in rooms" :key="r.id" :value="r.id">{{ r.name }}</option></select></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Maestro Dependiente</label><select v-model="form.teacherId" class="input-field"><option :value="null">Sin asignar (clase sin profesor)</option><option v-for="t in teachers" :key="t.id" :value="t.id">{{ t.name || t.email }}</option></select></div>
      <p v-if="error" class="text-red-400 text-sm">{{ error }}</p>
      <button type="submit" :disabled="creating" class="btn-primary w-full">{{ creating ? 'Creando...' : 'Crear Clase' }}</button>
    </form>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import classService from '@/services/classService'
import venueService from '@/services/venueService'

const router = useRouter()
const form = ref({ title: '', discipline: '', level: '', description: '', capacity: 10, duration: 60, price: 0, startTime: '', roomId: '', teacherId: null, tipoClase: 'ASIGNADA' })
const rooms = ref([])
const teachers = ref([])
const error = ref('')
const creating = ref(false)

onMounted(async () => {
  try {
    const venues = await venueService.getMyVenues()
    const vArr = Array.isArray(venues) ? venues : venues.content || []
    if (vArr.length > 0) rooms.value = await venueService.getVenueRooms(vArr[0].id)
  } catch { rooms.value = [] }
  try { teachers.value = await venueService.getVenueProfessors() } catch { teachers.value = [] }
})

async function handleCreate() {
  error.value = ''
  creating.value = true
  try {
    await classService.createClass(form.value)
    router.push('/sede/dashboard')
  } catch (e) { error.value = e.response?.data?.message || 'Error al crear clase' }
  creating.value = false
}
</script>
