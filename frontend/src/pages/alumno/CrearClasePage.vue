<template>
  <div class="max-w-2xl mx-auto space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-white">Crear Clase</h1>
      <p class="text-gray-400 text-sm mt-0.5">Propón una clase en una de nuestras sedes</p>
    </div>

    <div class="bg-[#161824] rounded-xl border border-white/10 p-6 space-y-5">

      <!-- Identidad requerida -->
      <div v-if="!identidadValidada" class="bg-amber-500/10 border border-amber-500/20 rounded-xl p-4 flex items-start gap-3">
        <svg class="w-5 h-5 text-amber-400 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
        <div>
          <p class="text-sm text-amber-400 font-medium">Identidad requerida</p>
          <p class="text-xs text-amber-400/70 mt-0.5">Debes verificar tu identidad antes de crear clases.</p>
          <router-link to="/profile/identity" class="text-xs text-indigo-400 hover:underline mt-1 inline-block">Verificar ahora →</router-link>
        </div>
      </div>

      <fieldset :disabled="!identidadValidada">

        <!-- Nombre -->
        <div class="mb-4">
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Nombre de la clase *</label>
          <input v-model="form.title" placeholder="Ej: Tango principiantes"
            class="campo" />
        </div>

        <!-- Disciplina + nivel -->
        <div class="grid grid-cols-2 gap-3 mb-4">
          <div>
            <label class="block text-xs font-medium text-gray-400 mb-1.5">Disciplina *</label>
            <select v-model="form.discipline" class="campo">
              <option value="">Selecciona</option>
              <option>Danza</option>
              <option>Teatro</option>
              <option>Música</option>
              <option>Artes visuales</option>
              <option>Yoga</option>
              <option>Acrobacia</option>
            </select>
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-400 mb-1.5">Nivel</label>
            <select v-model="form.level" class="campo">
              <option value="">Todos</option>
              <option>Principiante</option>
              <option>Intermedio</option>
              <option>Avanzado</option>
            </select>
          </div>
        </div>

        <!-- Descripción -->
        <div class="mb-4">
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Descripción</label>
          <textarea v-model="form.description" rows="3" placeholder="Describe qué aprenderán los alumnos..."
            class="campo resize-none"></textarea>
        </div>

        <!-- Rango de edad -->
        <div class="mb-4">
          <label class="block text-xs font-medium text-gray-400 mb-1.5">
            Rango de edad
            <span class="ml-1 text-gray-600 normal-case font-normal">(opcional)</span>
          </label>
          <div class="grid grid-cols-2 gap-3">
            <div class="relative">
              <input v-model.number="form.minAge" type="number" min="3" max="99" placeholder="Mín"
                class="campo pr-10" />
              <span class="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-gray-600">años</span>
            </div>
            <div class="relative">
              <input v-model.number="form.maxAge" type="number" min="3" max="99" placeholder="Máx"
                class="campo pr-10" />
              <span class="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-gray-600">años</span>
            </div>
          </div>
          <p v-if="ageError" class="text-xs text-red-400 mt-1">{{ ageError }}</p>
          <p v-else class="text-xs text-gray-600 mt-1">Deja vacío para abrir a todas las edades.</p>
        </div>

        <!-- Cupos + Precio (siempre visibles) -->
        <div class="grid grid-cols-2 gap-3 mb-5">
          <div>
            <label class="block text-xs font-medium text-gray-400 mb-1.5">Cupos *</label>
            <input v-model.number="form.capacity" type="number" min="1" placeholder="15"
              class="campo" />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-400 mb-1.5">Precio (CLP) *</label>
            <input v-model.number="form.price" type="number" min="0" placeholder="15000"
              class="campo" />
          </div>
        </div>

        <!-- Divisor: sección sede/sala/fecha -->
        <div class="border-t border-white/8 pt-5 mb-4">
          <div class="flex items-center justify-between mb-3">
            <p class="text-xs font-semibold text-gray-400 uppercase tracking-wider">Sede y horario</p>
            <span v-if="!form.venueId"
              class="text-xs px-2 py-0.5 rounded-full bg-amber-500/10 border border-amber-500/20 text-amber-400 font-medium">
              Opcional para borrador
            </span>
          </div>

          <!-- Aviso borrador -->
          <div v-if="!form.venueId"
            class="mb-4 p-3 bg-[#0d0f1a] border border-white/8 rounded-lg flex items-start gap-2.5">
            <svg class="w-4 h-4 text-gray-500 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
            <p class="text-xs text-gray-500 leading-relaxed">
              Si aún no tienes sede, puedes guardar la clase como borrador y asignarle una sede más adelante desde <span class="text-gray-400">Mis Clases</span>.
            </p>
          </div>

          <!-- Sede + Sala -->
          <div class="grid grid-cols-2 gap-3 mb-4">
            <div>
              <label class="block text-xs font-medium text-gray-400 mb-1.5">Sede</label>
              <select v-model="form.venueId" @change="loadRooms" class="campo">
                <option value="">Sin sede (borrador)</option>
                <option v-for="v in venues" :key="v.id" :value="v.id">{{ v.name }}</option>
              </select>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-400 mb-1.5">Sala</label>
              <select v-model="form.roomId" :disabled="!form.venueId" class="campo">
                <option value="">{{ form.venueId ? 'Selecciona sala' : 'Elige sede primero' }}</option>
                <option v-for="r in rooms" :key="r.id" :value="r.id">{{ r.name }}</option>
              </select>
            </div>
          </div>

          <!-- Fecha + Hora + Duración -->
          <div class="grid grid-cols-3 gap-3">
            <div class="col-span-2">
              <label class="block text-xs font-medium text-gray-400 mb-1.5">Fecha y hora</label>
              <input v-model="form.startTime" type="datetime-local" :disabled="!form.venueId"
                class="campo" />
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-400 mb-1.5">Duración (min)</label>
              <input v-model.number="form.duration" type="number" min="30" max="240" step="15" placeholder="60"
                :disabled="!form.venueId"
                class="campo" />
            </div>
          </div>
        </div>

        <!-- Error inline -->
        <div v-if="error" class="mb-4 flex items-center gap-2 px-4 py-3 bg-red-500/10 border border-red-500/20 rounded-xl text-sm text-red-400">
          <svg class="w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          {{ error }}
        </div>

        <!-- Botones -->
        <div class="flex gap-3">
          <button v-if="!form.venueId" @click="guardarBorrador"
            :disabled="!canSaveDraft || saving"
            class="flex-1 py-3 border border-amber-500/30 hover:border-amber-500/50 text-amber-400 hover:text-amber-300 disabled:opacity-40 disabled:cursor-not-allowed rounded-xl text-sm font-medium transition-colors">
            {{ saving === 'draft' ? 'Guardando...' : 'Guardar borrador' }}
          </button>

          <button @click="crear"
            :disabled="!canPublish || saving"
            class="flex-1 py-3 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-40 disabled:cursor-not-allowed text-white rounded-xl text-sm font-semibold transition-colors">
            {{ saving === 'publish' ? 'Creando clase...' : 'Crear Clase' }}
          </button>
        </div>

        <p v-if="!form.venueId && !canSaveDraft" class="text-xs text-gray-600 text-center mt-2">
          Completa nombre, disciplina, cupos y precio para guardar como borrador.
        </p>

      </fieldset>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { classService } from '../../services/classService'
import { venueService } from '../../services/venueService'
import { useAuth } from '../../hooks/useAuth'

const router = useRouter()
const { identidadValidada } = useAuth()

const form = ref({
  title: '', discipline: '', level: '', description: '',
  minAge: null, maxAge: null,
  venueId: '', roomId: '',
  startTime: '', duration: 60,
  capacity: '', price: '',
})
const venues = ref([])
const rooms = ref([])
const saving = ref(null) // null | 'draft' | 'publish'
const error = ref('')

const ageError = computed(() => {
  const { minAge, maxAge } = form.value
  if (minAge && maxAge && Number(minAge) >= Number(maxAge)) return 'La edad mínima debe ser menor que la máxima.'
  return ''
})

const canSaveDraft = computed(() =>
  identidadValidada.value &&
  form.value.title.trim() && form.value.discipline &&
  form.value.capacity && form.value.price &&
  !ageError.value
)

const canPublish = computed(() =>
  canSaveDraft.value &&
  form.value.venueId && form.value.roomId && form.value.startTime
)

onMounted(async () => {
  try { venues.value = await venueService.listApproved() } catch { /* ignore */ }
})

const loadRooms = async () => {
  form.value.roomId = ''
  rooms.value = []
  if (!form.value.venueId) return
  try { rooms.value = await venueService.getRooms(form.value.venueId) } catch { /* ignore */ }
}

const buildPayload = (draft) => {
  const base = {
    title: form.value.title.trim(),
    discipline: form.value.discipline,
    level: form.value.level || null,
    description: form.value.description || null,
    minAge: form.value.minAge || null,
    maxAge: form.value.maxAge || null,
    capacity: form.value.capacity,
    price: form.value.price,
  }
  if (draft) return base
  const end = new Date(new Date(form.value.startTime).getTime() + (form.value.duration || 60) * 60000)
  return {
    ...base,
    roomId: form.value.roomId,
    startTime: new Date(form.value.startTime).toISOString(),
    endTime: end.toISOString(),
  }
}

const guardarBorrador = async () => {
  if (!canSaveDraft.value) return
  saving.value = 'draft'
  error.value = ''
  try {
    await classService.createClass(buildPayload(true))
    router.push('/profesor/clases-propias')
  } catch (e) {
    error.value = e?.response?.data?.message || 'Error al guardar el borrador'
  } finally {
    saving.value = null
  }
}

const crear = async () => {
  if (!canPublish.value) return
  saving.value = 'publish'
  error.value = ''
  try {
    await classService.createClass(buildPayload(false))
    router.push('/profesor/clases-propias')
  } catch (e) {
    error.value = e?.response?.data?.message || 'Error al crear la clase'
  } finally {
    saving.value = null
  }
}
</script>

<style scoped>
.campo {
  @apply w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white
         placeholder-gray-500 text-sm [color-scheme:dark]
         focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all
         disabled:opacity-40 disabled:cursor-not-allowed;
}
</style>
