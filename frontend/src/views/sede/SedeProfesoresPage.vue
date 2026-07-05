<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-2">Profesores de mi Sede</h1>
    <p class="text-gray-400 mb-8">Profesores dependientes que puedes asignar a las clases creadas por la sede.</p>

    <!-- Agregar profesor por email -->
    <div class="card mb-6">
      <label class="block text-sm font-medium text-gray-300 mb-1">Agregar profesor por email</label>
      <div class="flex gap-3 flex-wrap">
        <input
          v-model="nuevoEmail"
          type="email"
          list="teacher-emails"
          autocomplete="off"
          placeholder="profesor@correo.cl"
          class="input-field flex-1 min-w-[220px]"
          @keyup.enter="agregar"
        />
        <datalist id="teacher-emails">
          <option v-for="c in candidatos" :key="c.email" :value="c.email">{{ c.fullName }}</option>
        </datalist>
        <button @click="agregar" :disabled="agregando || !nuevoEmail" class="btn-primary text-sm disabled:opacity-50">
          {{ agregando ? 'Agregando...' : 'Agregar' }}
        </button>
      </div>
      <p class="text-xs text-gray-500 mt-2">Escribe el correo (te sugerimos profesores registrados). Debe tener una cuenta en la plataforma.</p>
    </div>

    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>
    <div v-else-if="profesores.length === 0" class="card text-center py-16">
      <div class="w-14 h-14 bg-[var(--bg-elevated)] rounded-2xl flex items-center justify-center mx-auto mb-4">
        <svg class="w-7 h-7 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z"/>
        </svg>
      </div>
      <p class="text-gray-400 font-medium">Sin profesores dependientes</p>
      <p class="text-gray-600 text-sm mt-1">Agrega uno por email para poder asignarlo a las clases de tu sede.</p>
    </div>
    <div v-else class="space-y-4">
      <div v-for="p in profesores" :key="p.id" class="card flex items-center justify-between">
        <div class="flex items-center space-x-4">
          <div class="w-10 h-10 bg-primary/20 rounded-full flex items-center justify-center text-primary font-bold">{{ (p.fullName || p.email || 'P').charAt(0).toUpperCase() }}</div>
          <div><h3 class="text-white font-medium">{{ p.fullName || 'Profesor' }}</h3><p class="text-gray-400 text-sm">{{ p.email }}</p></div>
        </div>
        <button
          @click="quitar(p)"
          :disabled="quitandoId === p.id"
          class="text-sm px-3 py-2 rounded-lg border border-red-500/40 text-red-400 hover:bg-red-500/10 transition-colors disabled:opacity-50"
        >
          {{ quitandoId === p.id ? 'Quitando...' : 'Quitar' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import venueService from '@/services/venueService'
import { useToast } from '@/composables/useToast'

const toast = useToast()
const profesores = ref([])
const candidatos = ref([])
const loading = ref(true)
const venueId = ref(null)
const nuevoEmail = ref('')
const agregando = ref(false)
const quitandoId = ref(null)

onMounted(async () => {
  try {
    const venues = await venueService.getMyVenues()
    const vArr = Array.isArray(venues) ? venues : venues.content || []
    const sede = vArr.find(v => v.status === 'APROBADA') || vArr[0]
    venueId.value = sede?.id || null
  } catch { venueId.value = null }
  // Sugerencias de correo (profesores registrados) para el autocompletado.
  try { candidatos.value = await venueService.getTeacherCandidates() } catch { candidatos.value = [] }
  await cargar()
})

async function cargar() {
  loading.value = true
  try { profesores.value = await venueService.getVenueProfessors() } catch { profesores.value = [] }
  loading.value = false
}

async function agregar() {
  if (!nuevoEmail.value) return
  if (!venueId.value) { toast.error('No se encontró una sede para asociar el profesor.'); return }
  agregando.value = true
  try {
    await venueService.addVenueTeacher(venueId.value, nuevoEmail.value.trim())
    nuevoEmail.value = ''
    toast.success('Profesor agregado.')
    await cargar()
  } catch (e) {
    toast.error(e?.response?.data?.message || 'No se pudo agregar el profesor.')
  } finally {
    agregando.value = false
  }
}

async function quitar(p) {
  quitandoId.value = p.id
  try {
    await venueService.removeVenueTeacher(p.id)
    profesores.value = profesores.value.filter(x => x.id !== p.id)
    toast.success('Profesor quitado.')
  } catch {
    toast.error('No se pudo quitar el profesor.')
  } finally {
    quitandoId.value = null
  }
}
</script>
