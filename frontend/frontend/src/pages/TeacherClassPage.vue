<template>
  <div class="mx-auto max-w-4xl space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">Mis Clases</h1>
      <p class="text-sm text-gray-600">Crea y gestiona tus clases de baile.</p>
    </div>

    <div class="flex gap-2 border-b">
      <button @click="activeTab = 'create'" :class="activeTab === 'create' ? 'border-indigo-600 text-indigo-600' : 'border-transparent text-gray-500'" class="border-b-2 px-4 py-2 text-sm font-medium">Crear Clase</button>
      <button @click="activeTab = 'my-classes'; loadMyClasses()" :class="activeTab === 'my-classes' ? 'border-indigo-600 text-indigo-600' : 'border-transparent text-gray-500'" class="border-b-2 px-4 py-2 text-sm font-medium">Mis Clases</button>
    </div>

    <div v-if="activeTab === 'create'" class="rounded-xl border bg-white p-6">
      <form @submit.prevent="submitClass" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700">Titulo</label>
          <input v-model="form.title" required class="mt-1 w-full rounded-lg border px-3 py-2 text-sm" placeholder="Ej: Clase de Cueca para principiantes" />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700">Disciplina</label>
          <select v-model="form.discipline" required class="mt-1 w-full rounded-lg border px-3 py-2 text-sm">
            <option value="">Seleccionar...</option>
            <option value="Cueca">Cueca</option>
            <option value="Ballet">Ballet</option>
            <option value="Folclor">Folclor</option>
            <option value="Salsa">Salsa</option>
            <option value="Tango">Tango</option>
            <option value="Contemporaneo">Contemporaneo</option>
            <option value="Jazz">Jazz</option>
            <option value="Hip Hop">Hip Hop</option>
          </select>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700">Sede</label>
          <select v-model="form.venueId" required @change="loadRooms" class="mt-1 w-full rounded-lg border px-3 py-2 text-sm">
            <option value="">Seleccionar sede...</option>
            <option v-for="venue in venues" :key="venue.id" :value="venue.id">{{ venue.name }}</option>
          </select>
        </div>

        <div v-if="form.venueId">
          <label class="block text-sm font-medium text-gray-700">Sala</label>
          <select v-model="form.roomId" required @change="loadAvailability" class="mt-1 w-full rounded-lg border px-3 py-2 text-sm">
            <option value="">Seleccionar sala...</option>
            <option v-for="room in rooms" :key="room.id" :value="room.id">{{ room.name }} (Cap: {{ room.capacity }})</option>
          </select>
        </div>

        <div v-if="form.roomId">
          <label class="block text-sm font-medium text-gray-700">Horario disponible</label>
          <select v-model="form.availabilityId" @change="fillFromAvailability" class="mt-1 w-full rounded-lg border px-3 py-2 text-sm">
            <option value="">Seleccionar bloque o ingresar manual...</option>
            <option v-for="avail in availabilities" :key="avail.id" :value="avail.id">{{ formatDateTime(avail.startTime) }} - {{ formatDateTime(avail.endTime) }}</option>
          </select>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700">Inicio</label>
            <input v-model="form.startTime" type="datetime-local" required class="mt-1 w-full rounded-lg border px-3 py-2 text-sm" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700">Fin</label>
            <input v-model="form.endTime" type="datetime-local" required class="mt-1 w-full rounded-lg border px-3 py-2 text-sm" />
          </div>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700">Cupo</label>
            <input v-model.number="form.capacity" type="number" min="1" required class="mt-1 w-full rounded-lg border px-3 py-2 text-sm" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700">Precio (CLP)</label>
            <input v-model.number="form.price" type="number" min="0" required class="mt-1 w-full rounded-lg border px-3 py-2 text-sm" />
          </div>
        </div>

        <button type="submit" :disabled="submitting" class="w-full rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50">
          {{ submitting ? 'Creando...' : 'Crear Clase' }}
        </button>
      </form>
    </div>

    <div v-if="activeTab === 'my-classes'">
      <div v-if="myClassesLoading" class="rounded-xl border bg-white p-6 text-sm text-gray-500">Cargando clases...</div>
      <div v-else-if="myClasses.length === 0" class="rounded-xl border bg-white p-6 text-center text-sm text-gray-500">
        No tienes clases creadas.
      </div>
      <div v-else class="space-y-4">
        <div v-for="cls in myClasses" :key="cls.id" class="rounded-xl border bg-white p-5">
          <div class="flex items-start justify-between">
            <div>
              <h3 class="text-lg font-semibold text-gray-900">{{ cls.title }}</h3>
              <p class="text-sm text-gray-600">{{ cls.discipline }}</p>
              <p class="text-sm text-gray-500">{{ cls.venueName }} - {{ formatDateTime(cls.startTime) }}</p>
              <p class="text-sm text-gray-500">Cupo: {{ cls.capacity }} - Precio: ${{ cls.price?.toLocaleString('es-CL') }}</p>
            </div>
            <span :class="classStatusBadge(cls.status)" class="rounded-full px-2 py-0.5 text-xs font-medium">{{ cls.status }}</span>
          </div>
          <div v-if="cls.status === 'PUBLISHED'" class="mt-3 flex gap-2">
            <router-link :to="`/teacher/attendance/${cls.id}`" class="rounded-md bg-emerald-600 px-3 py-1.5 text-sm text-white hover:bg-emerald-700">Marcar Asistencia</router-link>
          </div>
        </div>
      </div>
    </div>

    <div v-if="successMessage" class="fixed bottom-4 right-4 rounded-lg bg-emerald-600 px-4 py-3 text-sm text-white shadow-lg">
      {{ successMessage }}
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { classService } from '../services/classService'

const activeTab = ref('create')
const venues = ref([])
const rooms = ref([])
const availabilities = ref([])
const myClasses = ref([])
const myClassesLoading = ref(false)
const submitting = ref(false)
const successMessage = ref('')

const form = ref({
  title: '',
  discipline: '',
  venueId: '',
  roomId: '',
  availabilityId: '',
  startTime: '',
  endTime: '',
  capacity: 20,
  price: 15000
})

onMounted(async () => {
  venues.value = await classService.getVenues()
})

const loadRooms = async () => {
  rooms.value = []
  availabilities.value = []
  form.value.roomId = ''
  form.value.availabilityId = ''
  if (!form.value.venueId) return
  rooms.value = await classService.getVenueRooms(form.value.venueId)
}

const loadAvailability = async () => {
  availabilities.value = []
  form.value.availabilityId = ''
  if (!form.value.roomId) return
  availabilities.value = await classService.getRoomAvailability(form.value.roomId)
}

const fillFromAvailability = () => {
  const avail = availabilities.value.find(a => a.id === form.value.availabilityId)
  if (avail) {
    form.value.startTime = toLocalDateTime(avail.startTime)
    form.value.endTime = toLocalDateTime(avail.endTime)
  }
}

const submitClass = async () => {
  submitting.value = true
  try {
    await classService.createClass({
      title: form.value.title,
      discipline: form.value.discipline,
      roomId: form.value.roomId,
      startTime: new Date(form.value.startTime).toISOString(),
      endTime: new Date(form.value.endTime).toISOString(),
      capacity: form.value.capacity,
      price: form.value.price
    })
    successMessage.value = 'Clase creada exitosamente!'
    form.value = { title: '', discipline: '', venueId: '', roomId: '', availabilityId: '', startTime: '', endTime: '', capacity: 20, price: 15000 }
    rooms.value = []
    availabilities.value = []
    setTimeout(() => { successMessage.value = '' }, 3000)
  } catch (e) {
    alert(e.response?.data?.message || 'Error al crear la clase.')
  } finally {
    submitting.value = false
  }
}

const loadMyClasses = async () => {
  myClassesLoading.value = true
  try {
    myClasses.value = await classService.getMyClasses()
  } catch (e) {
    myClasses.value = []
  } finally {
    myClassesLoading.value = false
  }
}

const formatDateTime = (instant) => {
  if (!instant) return ''
  return new Date(instant).toLocaleDateString('es-CL', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

const toLocalDateTime = (instant) => {
  const d = new Date(instant)
  const offset = d.getTimezoneOffset()
  const local = new Date(d.getTime() - offset * 60000)
  return local.toISOString().slice(0, 16)
}

const classStatusBadge = (status) => {
  const map = {
    PUBLISHED: 'bg-emerald-100 text-emerald-700',
    POR_VALIDAR: 'bg-amber-100 text-amber-700',
    COMPLETED: 'bg-blue-100 text-blue-700',
    SUSPENDED: 'bg-rose-100 text-rose-700',
    CANCELLED: 'bg-gray-100 text-gray-700',
    FULL: 'bg-purple-100 text-purple-700'
  }
  return map[status] || 'bg-gray-100 text-gray-700'
}
</script>
