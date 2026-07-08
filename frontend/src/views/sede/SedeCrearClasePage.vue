<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Crear Clase en Sede</h1>
    <form @submit.prevent="handleCreate" class="card space-y-4">
      <div>
        <label class="block text-sm font-medium text-gray-300 mb-1">Titulo</label>
        <input v-model="form.title" required class="input-field" />
      </div>

      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Disciplina</label>
          <select v-model="disciplinaSel" required class="input-field">
            <option value="">Seleccionar</option>
            <option v-for="d in disciplinas" :key="d" :value="d">{{ d }}</option>
            <option value="__OTRO__">Otro...</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Nivel</label>
          <select v-model="form.level" required class="input-field">
            <option value="">Seleccionar</option>
            <option value="BASICO">Básico</option>
            <option value="INTERMEDIO">Intermedio</option>
            <option value="AVANZADO">Avanzado</option>
          </select>
        </div>
      </div>
      <div v-if="disciplinaSel === '__OTRO__'" class="flex gap-3 items-end">
        <div class="flex-1">
          <label class="block text-sm font-medium text-gray-300 mb-1">¿Cuál disciplina?</label>
          <input v-model="nuevaDisciplina" class="input-field" placeholder="Ej: Karate, Ballet" @keyup.enter.prevent="agregarDisciplina" />
        </div>
        <button type="button" @click="agregarDisciplina" class="btn-primary text-sm whitespace-nowrap">Agregar</button>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-300 mb-1">Descripcion</label>
        <textarea v-model="form.description" rows="3" class="input-field"></textarea>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-300 mb-1">Precio por alumno ($)</label>
        <input type="number" v-model.number="form.price" min="0" required class="input-field" placeholder="Ej: 20000" />
      </div>

      <!-- Sala + capacidad visible -->
      <div>
        <label class="block text-sm font-medium text-gray-300 mb-1">Sala</label>
        <select v-model="form.roomId" required class="input-field">
          <option value="">Seleccionar sala</option>
          <option v-for="r in rooms" :key="r.id" :value="r.id">{{ r.name }} (cap: {{ r.capacity }})</option>
        </select>
        <p v-if="capacidadSala" class="text-xs text-primary mt-1">
          Capacidad de la sala: <span class="font-semibold">{{ capacidadSala }} personas</span> (define el cupo de la clase)
        </p>
      </div>

      <!-- Picker de bloques del horario de la sala -->
      <div v-if="form.roomId">
        <label class="block text-sm font-medium text-gray-300 mb-1">Horario de la clase (marca uno o más bloques)</label>
        <div v-if="bloquesLoading" class="text-gray-500 text-sm py-3">Cargando horarios de la sala...</div>
        <div v-else-if="bloquesPorDia.length === 0" class="text-gray-500 text-sm py-3">
          Esta sala no tiene horarios disponibles próximamente.
        </div>
        <div v-else class="space-y-3 max-h-72 overflow-y-auto pr-1">
          <div v-for="dia in bloquesPorDia" :key="dia.key">
            <p class="text-xs text-gray-400 mb-1 capitalize">{{ dia.label }}</p>
            <div class="flex flex-wrap gap-2">
              <button
                v-for="b in dia.bloques"
                :key="b.id"
                type="button"
                @click="toggleBloque(b.id)"
                :class="[
                  'px-3 py-1.5 rounded-lg border text-xs transition-colors',
                  seleccionados.includes(b.id)
                    ? 'bg-primary/25 border-primary text-primary'
                    : 'border-white/10 text-gray-300 hover:border-white/30 bg-[var(--bg-elevated)]'
                ]"
              >
                {{ formatHora(b.startTime) }}
              </button>
            </div>
          </div>
        </div>
        <p v-if="seleccionados.length" class="text-xs text-green-400 mt-2">
          {{ seleccionados.length }} bloque{{ seleccionados.length > 1 ? 's' : '' }} · {{ seleccionados.length }}h
        </p>
      </div>

      <!-- Maestro + honorario -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Maestro Dependiente</label>
          <select v-model="form.teacherId" required class="input-field">
            <option :value="null">Seleccionar profesor</option>
            <option v-for="t in teachers" :key="t.id" :value="t.teacherId">{{ t.fullName || t.email }}</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Honorario del profe ($)</label>
          <div class="relative">
            <input type="number" v-model.number="form.honorario" min="0" class="input-field pr-16" placeholder="Ej: 70000" />
            <span v-if="porcentajeHonorario !== null" class="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-semibold text-primary">
              {{ porcentajeHonorario }}%
            </span>
          </div>
        </div>
      </div>
      <p class="text-xs text-gray-500 -mt-2">
        <template v-if="baseRecaudacion > 0">
          Recaudación con sala llena: <span class="text-gray-300">${{ baseRecaudacion.toLocaleString('es-CL') }}</span>.
          El honorario es {{ porcentajeHonorario ?? 0 }}% de eso; la diferencia (${{ margenSede.toLocaleString('es-CL') }}) es el ingreso de la sede.
        </template>
        <template v-else>
          Elige sala y precio para ver el % que representa el honorario.
        </template>
      </p>

      <p v-if="error" class="text-red-400 text-sm">{{ error }}</p>
      <button type="submit" :disabled="creating" class="btn-primary w-full">{{ creating ? 'Creando...' : 'Crear Clase' }}</button>
    </form>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import classService from '@/services/classService'
import venueService from '@/services/venueService'
import scheduleService from '@/services/scheduleService'
import { normalizarDisciplina, claveComparacion } from '@/utils/disciplina'

const router = useRouter()
const form = ref({ title: '', discipline: '', level: '', description: '', price: 0, roomId: '', teacherId: null, honorario: 0, tipoClase: 'ASIGNADA' })
const rooms = ref([])
const teachers = ref([])
const error = ref('')
const creating = ref(false)

// Bloques del horario de la sala seleccionada.
const bloques = ref([])
const bloquesLoading = ref(false)
const seleccionados = ref([])

// Disciplinas base + las que agregue la sede vía "Otro".
const disciplinas = ref(['Danza', 'Guitarra', 'Bateria', 'Bajo', 'Canto', 'Piano', 'Violin'])
const disciplinaSel = ref('')
const nuevaDisciplina = ref('')

watch(disciplinaSel, (val) => {
  if (val !== '__OTRO__') form.value.discipline = val
})

// Capacidad de la sala elegida (visible + base del cálculo del honorario).
const capacidadSala = computed(() => {
  const sala = rooms.value.find(r => r.id === form.value.roomId)
  return sala?.capacity ?? null
})
// Recaudación con la sala llena = capacidad × precio por alumno.
const baseRecaudacion = computed(() => {
  if (!capacidadSala.value || !form.value.price) return 0
  return capacidadSala.value * Number(form.value.price)
})
// Se ingresa el honorario en $ y se muestra el % que representa sobre esa base.
const porcentajeHonorario = computed(() => {
  if (!baseRecaudacion.value || !form.value.honorario) return null
  return Math.round((Number(form.value.honorario) / baseRecaudacion.value) * 100)
})
const margenSede = computed(() => Math.max(0, baseRecaudacion.value - Number(form.value.honorario || 0)))

// Al cambiar de sala, cargar sus bloques disponibles futuros y limpiar la selección.
watch(() => form.value.roomId, async (id) => {
  seleccionados.value = []
  bloques.value = []
  if (!id) return
  bloquesLoading.value = true
  try {
    const desde = new Date().toISOString()
    const hasta = new Date(Date.now() + 21 * 24 * 60 * 60 * 1000).toISOString()
    const data = await scheduleService.getRoomSchedule(id, desde, hasta)
    const ahora = Date.now()
    bloques.value = (Array.isArray(data) ? data : [])
      .filter(b => b.status === 'AVAILABLE' && new Date(b.startTime).getTime() > ahora)
  } catch { bloques.value = [] }
  bloquesLoading.value = false
})

// Bloques agrupados por día para el picker.
const bloquesPorDia = computed(() => {
  const map = {}
  for (const b of bloques.value) {
    const d = new Date(b.startTime)
    const key = d.toLocaleDateString('en-CA')
    if (!map[key]) {
      map[key] = { key, label: d.toLocaleDateString('es-CL', { weekday: 'short', day: 'numeric', month: 'short' }), bloques: [] }
    }
    map[key].bloques.push(b)
  }
  return Object.values(map).sort((a, b) => a.key.localeCompare(b.key))
})

function formatHora(iso) {
  return new Date(iso).toLocaleTimeString('es-CL', { hour: '2-digit', minute: '2-digit' })
}

function toggleBloque(id) {
  const i = seleccionados.value.indexOf(id)
  if (i >= 0) seleccionados.value.splice(i, 1)
  else seleccionados.value.push(id)
}

function agregarDisciplina() {
  const canon = normalizarDisciplina(nuevaDisciplina.value)
  if (!canon) { error.value = 'Escribe la disciplina.'; return }
  error.value = ''
  const clave = claveComparacion(canon)
  const existente = disciplinas.value.find(d => claveComparacion(d) === clave)
  if (existente) {
    disciplinaSel.value = existente
  } else {
    disciplinas.value.push(canon)
    disciplinaSel.value = canon
  }
  nuevaDisciplina.value = ''
}

onMounted(async () => {
  try {
    const venues = await venueService.getMyVenues()
    const vArr = Array.isArray(venues) ? venues : venues.content || []
    const sede = vArr.find(v => v.status === 'APROBADA') || vArr[0]
    if (sede) rooms.value = await venueService.getVenueRooms(sede.id)
  } catch { rooms.value = [] }
  try { teachers.value = await venueService.getVenueProfessors() } catch { teachers.value = [] }
})

async function handleCreate() {
  error.value = ''
  if (disciplinaSel.value === '__OTRO__' || !form.value.discipline) {
    error.value = 'Selecciona o agrega una disciplina.'
    return
  }
  if (!form.value.roomId) { error.value = 'Selecciona una sala.'; return }
  if (seleccionados.value.length === 0) { error.value = 'Marca al menos un bloque de horario.'; return }
  if (!form.value.teacherId) { error.value = 'Selecciona el maestro dependiente.'; return }
  creating.value = true
  try {
    await classService.createClass({ ...form.value, blockIds: seleccionados.value })
    router.push('/sede/dashboard')
  } catch (e) { error.value = e.response?.data?.message || 'Error al crear clase' }
  creating.value = false
}
</script>
