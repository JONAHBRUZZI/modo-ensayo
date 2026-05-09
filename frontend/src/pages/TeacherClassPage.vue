<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-white">Mis Clases</h1>
      <p class="text-gray-400 text-sm mt-0.5">Crea y gestiona tus clases de baile.</p>
    </div>

    <!-- Tabs -->
    <div class="flex gap-1 bg-[#161824] rounded-xl border border-white/10 p-1 w-fit">
      <button @click="activeTab = 'create'" :class="activeTab === 'create' ? 'bg-indigo-600 text-white' : 'text-gray-400 hover:text-white'"
        class="px-4 py-1.5 rounded-lg text-sm font-medium transition-colors">Crear Clase</button>
      <button @click="activeTab = 'my-classes'; loadMyClasses()"
        :class="activeTab === 'my-classes' ? 'bg-indigo-600 text-white' : 'text-gray-400 hover:text-white'"
        class="px-4 py-1.5 rounded-lg text-sm font-medium transition-colors">Mis Clases</button>
    </div>

    <!-- Formulario crear clase -->
    <div v-if="activeTab === 'create'" class="bg-[#161824] rounded-xl border border-white/10 p-6">
      <form @submit.prevent="submitClass" class="space-y-4">
        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Título *</label>
          <input v-model="form.title" required placeholder="Ej: Clase de Cueca para principiantes"
            class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-sm text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all" />
        </div>

        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Disciplina *</label>
          <select v-model="form.discipline" required
            class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-sm text-white [color-scheme:dark] focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all">
            <option value="">Seleccionar...</option>
            <option v-for="d in disciplines" :key="d">{{ d }}</option>
          </select>
        </div>

        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Sede *</label>
          <select v-model="form.venueId" required @change="loadRooms"
            class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-sm text-white [color-scheme:dark] focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all">
            <option value="">Seleccionar sede...</option>
            <option v-for="venue in venues" :key="venue.id" :value="venue.id">{{ venue.name }}</option>
          </select>
        </div>

        <div v-if="form.venueId">
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Sala *</label>
          <select v-model="form.roomId" required @change="loadAvailability"
            class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-sm text-white [color-scheme:dark] focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all">
            <option value="">Seleccionar sala...</option>
            <option v-for="room in rooms" :key="room.id" :value="room.id">{{ room.name }} (Cap: {{ room.capacity }})</option>
          </select>
        </div>

        <div v-if="form.roomId">
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Horario disponible</label>
          <select v-model="form.availabilityId" @change="fillFromAvailability"
            class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-sm text-white [color-scheme:dark] focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all">
            <option value="">Seleccionar bloque o ingresar manual...</option>
            <option v-for="avail in availabilities" :key="avail.id" :value="avail.id">
              {{ formatDateTime(avail.startTime) }} — {{ formatDateTime(avail.endTime) }}
            </option>
          </select>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-xs font-medium text-gray-400 mb-1.5">Inicio *</label>
            <input v-model="form.startTime" type="datetime-local" required
              class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-sm text-white [color-scheme:dark] focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all" />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-400 mb-1.5">Fin *</label>
            <input v-model="form.endTime" type="datetime-local" required
              class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-sm text-white [color-scheme:dark] focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all" />
          </div>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-xs font-medium text-gray-400 mb-1.5">Cupo *</label>
            <input v-model.number="form.capacity" type="number" min="1" required
              class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all" />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-400 mb-1.5">Precio (CLP) *</label>
            <input v-model.number="form.price" type="number" min="0" required
              class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all" />
          </div>
        </div>

        <button type="submit" :disabled="submitting"
          class="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white rounded-lg text-sm font-semibold transition-colors">
          <span v-if="submitting" class="flex items-center justify-center gap-2">
            <svg class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
            </svg>
            Creando...
          </span>
          <span v-else>Crear Clase</span>
        </button>
      </form>
    </div>

    <!-- Lista de mis clases -->
    <div v-if="activeTab === 'my-classes'">
      <div v-if="myClassesLoading" class="space-y-3">
        <div v-for="i in 4" :key="i" class="h-24 bg-[#161824] rounded-xl border border-white/10 animate-pulse"></div>
      </div>
      <div v-else-if="myClasses.length === 0" class="text-center py-14 bg-[#161824] rounded-xl border border-white/10">
        <p class="text-gray-400 text-sm">No tienes clases creadas aún.</p>
      </div>
      <div v-else class="space-y-3">
        <div v-for="cls in myClasses" :key="cls.id"
          class="bg-[#161824] rounded-xl border border-white/10 hover:border-white/20 p-5 flex items-start gap-4 transition-all">
          <div class="flex-1 min-w-0">
            <div class="flex items-start justify-between gap-2">
              <div>
                <h3 class="font-semibold text-white">{{ cls.title }}</h3>
                <p class="text-xs text-gray-500 mt-1">{{ cls.discipline }} · {{ cls.venueName }}</p>
                <p class="text-xs text-gray-500">{{ formatDateTime(cls.startTime) }} · Cupo: {{ cls.capacity }} · ${{ cls.price?.toLocaleString('es-CL') }}</p>
              </div>
              <EstadoBadge :estado="cls.status || 'ACTIVA'" />
            </div>
            <div v-if="cls.status === 'PUBLISHED'" class="flex gap-3 mt-3">
              <router-link :to="`/profesor/asistencia/${cls.id}`" class="text-xs text-purple-400 hover:text-purple-300 transition-colors">
                Tomar asistencia
              </router-link>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <Transition enter-active-class="transition-all duration-300" enter-from-class="opacity-0 translate-y-2" leave-active-class="transition-all duration-300" leave-to-class="opacity-0 translate-y-2">
      <div v-if="successMessage" class="fixed bottom-6 right-6 bg-emerald-600 text-white text-sm font-medium px-4 py-3 rounded-xl shadow-lg z-50">
        {{ successMessage }}
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { classService } from '../services/classService'
import EstadoBadge from '../components/EstadoBadge.vue'

const disciplines = ['Cueca', 'Ballet', 'Folclor', 'Salsa', 'Tango', 'Contemporáneo', 'Jazz', 'Hip Hop', 'Otro']

const activeTab = ref('create')
const venues = ref([])
const rooms = ref([])
const availabilities = ref([])
const myClasses = ref([])
const myClassesLoading = ref(false)
const submitting = ref(false)
const successMessage = ref('')

const form = ref({
  title: '', discipline: '', venueId: '', roomId: '', availabilityId: '',
  startTime: '', endTime: '', capacity: 20, price: 15000,
})

onMounted(async () => {
  try { venues.value = await classService.getVenues() } catch { /* ignore */ }
})

const loadRooms = async () => {
  rooms.value = []; availabilities.value = []
  form.value.roomId = ''; form.value.availabilityId = ''
  if (!form.value.venueId) return
  try { rooms.value = await classService.getVenueRooms(form.value.venueId) } catch { /* ignore */ }
}

const loadAvailability = async () => {
  availabilities.value = []; form.value.availabilityId = ''
  if (!form.value.roomId) return
  try { availabilities.value = await classService.getRoomAvailability(form.value.roomId) } catch { /* ignore */ }
}

const fillFromAvailability = () => {
  const avail = availabilities.value.find(a => a.id === form.value.availabilityId)
  if (avail) { form.value.startTime = toLocalDT(avail.startTime); form.value.endTime = toLocalDT(avail.endTime) }
}

const submitClass = async () => {
  submitting.value = true
  try {
    await classService.createClass({
      title: form.value.title, discipline: form.value.discipline, roomId: form.value.roomId,
      startTime: new Date(form.value.startTime).toISOString(), endTime: new Date(form.value.endTime).toISOString(),
      capacity: form.value.capacity, price: form.value.price,
    })
    successMessage.value = '¡Clase creada exitosamente!'
    form.value = { title: '', discipline: '', venueId: '', roomId: '', availabilityId: '', startTime: '', endTime: '', capacity: 20, price: 15000 }
    rooms.value = []; availabilities.value = []
    setTimeout(() => { successMessage.value = '' }, 3000)
  } catch (e) {
    alert(e.response?.data?.message || 'Error al crear la clase.')
  } finally {
    submitting.value = false
  }
}

const loadMyClasses = async () => {
  myClassesLoading.value = true
  try { myClasses.value = await classService.getMyClasses() } catch { myClasses.value = [] }
  finally { myClassesLoading.value = false }
}

const formatDateTime = (d) => d ? new Date(d).toLocaleDateString('es-CL', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''
const toLocalDT = (d) => { const dt = new Date(d); return new Date(dt.getTime() - dt.getTimezoneOffset() * 60000).toISOString().slice(0, 16) }
</script>
