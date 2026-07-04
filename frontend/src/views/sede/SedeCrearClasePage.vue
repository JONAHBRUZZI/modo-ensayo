<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Crear Clase en Sede</h1>
    <form @submit.prevent="handleCreate" class="card space-y-4">
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Titulo</label><input v-model="form.title" required class="input-field" /></div>
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
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Descripcion</label><textarea v-model="form.description" rows="3" class="input-field"></textarea></div>
      <div class="grid grid-cols-3 gap-4">
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Capacidad</label><input type="number" v-model.number="form.capacity" min="1" required class="input-field" /></div>
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Duracion (min)</label><input type="number" v-model.number="form.duration" min="30" required class="input-field" /></div>
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Precio</label><input type="number" v-model.number="form.price" min="0" required class="input-field" /></div>
      </div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Fecha y Hora</label><input type="datetime-local" v-model="form.startTime" required class="input-field" /></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Sala</label><select v-model="form.roomId" required class="input-field"><option value="">Seleccionar sala</option><option v-for="r in rooms" :key="r.id" :value="r.id">{{ r.name }}</option></select></div>
      <div class="grid grid-cols-2 gap-4">
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Maestro Dependiente</label><select v-model="form.teacherId" class="input-field"><option :value="null">Sin asignar (clase sin profesor)</option><option v-for="t in teachers" :key="t.id" :value="t.teacherId">{{ t.fullName || t.email }}</option></select></div>
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Honorario del profe ($)</label><input type="number" v-model.number="form.honorario" min="0" class="input-field" placeholder="Ej: 70000" /></div>
      </div>
      <p class="text-xs text-gray-500 -mt-2">La sede cobra a los alumnos y le paga al profe el honorario fijo; la diferencia es el ingreso de la sede.</p>
      <p v-if="error" class="text-red-400 text-sm">{{ error }}</p>
      <button type="submit" :disabled="creating" class="btn-primary w-full">{{ creating ? 'Creando...' : 'Crear Clase' }}</button>
    </form>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import classService from '@/services/classService'
import venueService from '@/services/venueService'

const router = useRouter()
const form = ref({ title: '', discipline: '', level: '', description: '', capacity: 10, duration: 60, price: 0, startTime: '', roomId: '', teacherId: null, honorario: 0, tipoClase: 'ASIGNADA' })
const rooms = ref([])
const teachers = ref([])
const error = ref('')
const creating = ref(false)

// Disciplinas base + las que agregue la sede vía "Otro". disciplinaSel es el valor
// del select ('__OTRO__' abre el campo para escribir una nueva).
const disciplinas = ref(['Danza', 'Guitarra', 'Bateria', 'Bajo', 'Canto', 'Piano', 'Violin'])
const disciplinaSel = ref('')
const nuevaDisciplina = ref('')

// El select controla form.discipline salvo cuando está en "Otro" (aún sin definir).
watch(disciplinaSel, (val) => {
  if (val !== '__OTRO__') form.value.discipline = val
})

// Normaliza a "Title Case" con espacios colapsados (ej: "  KARATE " -> "Karate").
function normalizarDisciplina(txt) {
  return (txt || '').trim().replace(/\s+/g, ' ').toLowerCase()
    .split(' ').filter(Boolean)
    .map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' ')
}

// Clave de comparación: sin acentos ni mayúsculas, para deduplicar
// ("Karate" == "karate" == "KÁRATE"). No fusiona grafías distintas (karate ≠ carate).
function claveComparacion(txt) {
  return normalizarDisciplina(txt).toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '')
}

function agregarDisciplina() {
  const canon = normalizarDisciplina(nuevaDisciplina.value)
  if (!canon) { error.value = 'Escribe la disciplina.'; return }
  error.value = ''
  const clave = claveComparacion(canon)
  const existente = disciplinas.value.find(d => claveComparacion(d) === clave)
  if (existente) {
    disciplinaSel.value = existente          // ya existía: la reutiliza
  } else {
    disciplinas.value.push(canon)            // nueva: al final de las opciones
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
  // "Otro" elegido pero sin agregar la disciplina: no dejar enviar el centinela.
  if (disciplinaSel.value === '__OTRO__' || !form.value.discipline) {
    error.value = 'Selecciona o agrega una disciplina.'
    return
  }
  creating.value = true
  try {
    await classService.createClass(form.value)
    router.push('/sede/dashboard')
  } catch (e) { error.value = e.response?.data?.message || 'Error al crear clase' }
  creating.value = false
}
</script>
