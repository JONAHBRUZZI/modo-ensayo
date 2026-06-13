<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-2">
      {{ isEditing ? 'Completar y Publicar Clase' : 'Agendar Sala y Crear Clase' }}
    </h1>
    <p class="text-gray-400 text-sm mb-8">
      {{ isEditing ? 'Completa los datos de tu clase. La sala y horario ya estan reservados.' : 'Reserva una sala y define los datos de tu clase.' }}
    </p>

    <!-- Info de sala reservada (solo lectura cuando se edita un borrador) -->
    <div v-if="isEditing && salaInfo" class="card mb-6 border-primary/30">
      <p class="text-xs text-primary font-medium mb-3 uppercase tracking-wider">Sala Reservada</p>
      <div class="grid grid-cols-2 gap-4 text-sm">
        <div>
          <span class="text-gray-500">Sala:</span>
          <span class="text-white ml-2">{{ salaInfo.roomName || form.roomId }}</span>
        </div>
        <div>
          <span class="text-gray-500">Sede:</span>
          <span class="text-white ml-2">{{ salaInfo.venueName || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-500">Fecha y hora:</span>
          <span class="text-white ml-2">{{ formatDatetime(form.startTime) }}</span>
        </div>
        <div v-if="salaInfo.duration">
          <span class="text-gray-500">Duracion:</span>
          <span class="text-white ml-2">{{ salaInfo.duration }} min</span>
        </div>
      </div>
    </div>

    <!-- Opciones para esta reserva: completar nueva o asignar borrador existente -->
    <div v-if="isEditing" class="mb-6">
      <p class="text-sm text-gray-300 mb-3">¿Qué deseas hacer con esta sala reservada?</p>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
        <!-- Opcion 1: Completar clase nueva (el formulario de abajo) -->
        <div class="rounded-xl border border-primary/40 bg-primary/5 p-4">
          <div class="flex items-center gap-2 mb-1">
            <span class="w-2 h-2 bg-primary rounded-full"></span>
            <p class="text-white font-medium text-sm">Completar Clase Nueva</p>
          </div>
          <p class="text-gray-400 text-xs">
            Configura la clase desde cero con el formulario de abajo (titulo, disciplina, precio, etc.).
          </p>
        </div>

        <!-- Opcion 2: Asignar un borrador existente -->
        <button
          type="button"
          @click="abrirModalBorrador"
          class="rounded-xl border border-white/10 bg-[#0d0f1a] hover:border-white/30 hover:bg-[#1a1d2e] p-4 text-left transition-colors"
        >
          <div class="flex items-center gap-2 mb-1">
            <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
            </svg>
            <p class="text-white font-medium text-sm">Asignar Borrador Existente</p>
          </div>
          <p class="text-gray-400 text-xs">
            Usa una clase que ya tienes guardada como borrador y asígnale esta sala/horario.
          </p>
        </button>
      </div>
    </div>

    <!-- Modal: seleccionar borrador existente -->
    <BorradorSelector
      :abierto="modalBorrador"
      :reservation-id="editingClassId"
      :room-id="form.roomId"
      :start-time="form.startTime"
      :duration="form.duration"
      @close="modalBorrador = false"
      @applied="onBorradorAplicado" />

    <form @submit.prevent="handleCreate" class="card space-y-4">
      <!-- Titulo -->
      <div>
        <label class="block text-sm font-medium text-gray-300 mb-1">Titulo *</label>
        <input v-model="form.title" required class="input-field" placeholder="Ej: Guitarra para principiantes" />
      </div>

      <!-- Disciplina y Nivel -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Disciplina *</label>
          <select v-model="form.discipline" required class="input-field">
            <option value="">Seleccionar</option>
            <option value="CUECA">Cueca</option><option value="BALLET">Ballet</option><option value="DANZA">Danza</option>
            <option>TEATRO</option><option>CANTO</option><option>GUITARRA</option>
            <option>BATERIA</option><option>BAJO</option><option>PIANO</option>
            <option>VIOLIN</option><option>SAXOFON</option><option>OTRO</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Nivel *</label>
          <select v-model="form.level" required class="input-field">
            <option value="">Seleccionar</option>
            <option value="BASICO">Básico</option><option value="INTERMEDIO">Intermedio</option><option value="AVANZADO">Avanzado</option>
          </select>
        </div>
      </div>

      <!-- Descripcion -->
      <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Descripción</label>
        <textarea v-model="form.description" rows="3" class="input-field" placeholder="Describe tu clase..."></textarea>
      </div>

      <!-- Capacidad, Duracion, Precio -->
      <div class="grid grid-cols-3 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Capacidad *</label>
          <input type="number" v-model.number="form.capacity" min="1" required class="input-field" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Duracion (min) *</label>
          <input type="number" v-model.number="form.duration" min="30" required class="input-field" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Precio ($) *</label>
          <input type="number" v-model.number="form.price" min="0" required class="input-field" />
        </div>
      </div>

      <!-- Edades -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Edad Minima</label>
          <input type="number" v-model.number="form.minAge" min="0" class="input-field" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Edad Maxima</label>
          <input type="number" v-model.number="form.maxAge" min="0" class="input-field" />
        </div>
      </div>

      <!-- Fecha/hora + Sede/Sala: solo visibles cuando NO se edita un borrador -->
      <template v-if="!isEditing">
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Fecha y Hora *</label>
          <input type="datetime-local" v-model="form.startTime" required class="input-field" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Sede *</label>
          <select v-model="form.venueId" required class="input-field">
            <option value="">Seleccionar sede</option>
            <option v-for="v in venues" :key="v.id" :value="v.id">{{ v.name }}</option>
          </select>
        </div>
        <div v-if="form.venueId">
          <label class="block text-sm font-medium text-gray-300 mb-1">Sala *</label>
          <select v-model="form.roomId" required class="input-field">
            <option value="">Seleccionar sala</option>
            <option v-for="r in rooms" :key="r.id" :value="r.id">{{ r.name }} (cap: {{ r.capacity }})</option>
          </select>
        </div>
      </template>

      <p v-if="error" class="text-red-400 text-sm">{{ error }}</p>

      <button type="submit" :disabled="creating" class="btn-primary w-full">
        {{ creating ? 'Guardando...' : isEditing ? 'Publicar Clase' : 'Agendar Sala y Crear Clase' }}
      </button>
    </form>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import classService from '@/services/classService'
import api from '@/services/api'
import { useAuth } from '@/stores/auth'
import BorradorSelector from '@/components/BorradorSelector.vue'

const router = useRouter()
const route = useRoute()
const { syncAtributos } = useAuth()

const form = ref({
  title: '', discipline: '', level: '', description: '',
  capacity: 10, duration: 60, price: 0, minAge: 0, maxAge: 99,
  startTime: '', venueId: '', roomId: ''
})
const salaInfo = ref(null)
const venues = ref([])
const rooms = ref([])
const error = ref('')
const creating = ref(false)
const isEditing = ref(false)
const editingClassId = ref(null)

const modalBorrador = ref(false)

onMounted(async () => {
  try { venues.value = await classService.getVenues() } catch { venues.value = [] }

  if (route.query.edit) {
    // Modo publicar borrador: sala y horario son fijos
    try {
      const cls = await classService.getClassById(route.query.edit)
      if (cls) {
        form.value.title = cls.title && !cls.title.startsWith('Reserva -') ? cls.title : ''
        form.value.discipline = cls.discipline || ''
        form.value.level = cls.level || ''
        form.value.description = cls.description || ''
        form.value.capacity = cls.capacity || 10
        form.value.duration = cls.duration || 60
        form.value.price = cls.price || 0
        form.value.minAge = cls.minAge || 0
        form.value.maxAge = cls.maxAge || 99
        form.value.startTime = cls.startTime || ''
        form.value.venueId = cls.venueId || ''
        form.value.roomId = cls.roomId || ''
        isEditing.value = true
        editingClassId.value = route.query.edit
        // Guardar info de sala para mostrar en solo lectura
        salaInfo.value = {
          roomName: cls.roomName || null,
          venueName: cls.venueName || null,
          duration: cls.duration || null
        }
      }
    } catch {}
  } else {
    // Modo crear directo (sin reserva previa)
    if (route.query.roomId) form.value.roomId = route.query.roomId
    if (route.query.venueId) {
      form.value.venueId = route.query.venueId
      try { rooms.value = await classService.getVenueRooms(route.query.venueId) } catch { rooms.value = [] }
    }
    if (route.query.startTime) form.value.startTime = route.query.startTime
  }
})

watch(() => form.value.venueId, async (id) => {
  if (isEditing.value) return  // no cambiar sala si es borrador
  if (!id) { rooms.value = []; return }
  try { rooms.value = await classService.getVenueRooms(id) } catch { rooms.value = [] }
})

async function handleCreate() {
  error.value = ''
  creating.value = true
  try {
    if (isEditing.value) {
      // Publicar borrador — sala y horario vienen del DRAFT original, no del formulario
      await api.put('/classes/' + editingClassId.value + '/publish', {
        title: form.value.title,
        discipline: form.value.discipline,
        level: form.value.level,
        description: form.value.description,
        capacity: form.value.capacity,
        duration: form.value.duration,
        price: form.value.price,
        minAge: form.value.minAge,
        maxAge: form.value.maxAge
        // roomId y startTime NO se envian: quedan los del DRAFT
      })
    } else {
      await classService.createClass(form.value)
    }
    await syncAtributos()
    router.push('/profesor/clases-propias')
  } catch (e) {
    error.value = e.response?.data?.message || 'Error al ' + (isEditing.value ? 'publicar' : 'crear') + ' clase'
  } finally {
    creating.value = false
  }
}

async function abrirModalBorrador() {
  modalBorrador.value = true
}

function onBorradorAplicado(draft) {
  router.push('/profesor/clases-propias')
}

function formatDatetime(d) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('es-CL', {
    weekday: 'short', day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit'
  })
}
</script>
