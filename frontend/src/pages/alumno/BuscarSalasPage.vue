<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-white">Buscar Salas</h1>
      <p class="text-gray-400 text-sm mt-0.5">Encuentra el espacio perfecto para tu arte</p>
    </div>

    <!-- Filtros -->
    <div class="bg-[#161824] rounded-xl border border-white/10 p-4">
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <input v-model="filters.q" placeholder="Ciudad o nombre de sede..."
          class="px-3 py-2 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all" />
        <select v-model="filters.disciplina"
          class="px-3 py-2 bg-[#0d0f1a] border border-white/15 rounded-lg text-sm text-white [color-scheme:dark] focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all">
          <option value="">Todas las disciplinas</option>
          <option>Danza</option>
          <option>Teatro</option>
          <option>Música</option>
          <option>Artes visuales</option>
          <option>Yoga</option>
        </select>
        <select v-model="filters.capacidad"
          class="px-3 py-2 bg-[#0d0f1a] border border-white/15 rounded-lg text-sm text-white [color-scheme:dark] focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all">
          <option value="">Cualquier capacidad</option>
          <option value="10">Hasta 10 personas</option>
          <option value="30">Hasta 30 personas</option>
          <option value="60">Hasta 60 personas</option>
        </select>
      </div>
    </div>

    <!-- Skeleton -->
    <div v-if="loading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
      <div v-for="i in 6" :key="i" class="h-52 bg-[#161824] rounded-xl border border-white/10 animate-pulse"></div>
    </div>

    <!-- Grid de sedes -->
    <div v-else-if="filteredVenues.length" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
      <div v-for="v in filteredVenues" :key="v.id"
        class="bg-[#161824] rounded-xl border border-white/10 hover:border-white/20 overflow-hidden group transition-all hover:shadow-xl hover:shadow-black/20">
        <div class="h-32 bg-gradient-to-br relative overflow-hidden"
          :class="gradients[v.id % gradients.length]">
          <div class="absolute inset-0 bg-black/20"></div>
          <div class="absolute bottom-3 left-4">
            <p class="text-white font-bold text-sm">{{ v.name }}</p>
            <p class="text-white/70 text-xs">{{ v.address || v.city }}</p>
          </div>
        </div>
        <div class="p-4">
          <div class="flex items-center gap-2 flex-wrap mb-3">
            <span v-for="disc in (v.disciplines || []).slice(0, 3)" :key="disc"
              class="px-2 py-0.5 bg-indigo-500/15 text-indigo-400 border border-indigo-500/20 rounded-full text-xs">
              {{ disc }}
            </span>
          </div>
          <div class="flex items-center justify-between text-xs text-gray-500 mb-4">
            <span class="flex items-center gap-1">
              <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
              </svg>
              {{ v.roomCount || '?' }} sala{{ v.roomCount !== 1 ? 's' : '' }}
            </span>
            <span class="flex items-center gap-1">
              <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              Cap. {{ v.maxCapacity || '?' }}
            </span>
          </div>
          <button @click="selectVenue(v)"
            class="w-full py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-sm font-medium transition-colors">
            Ver disponibilidad
          </button>
        </div>
      </div>
    </div>

    <!-- Empty -->
    <div v-else class="text-center py-16 bg-[#161824] rounded-xl border border-white/10">
      <p class="text-gray-400 text-sm">No se encontraron sedes con esos criterios</p>
    </div>

    <!-- Modal disponibilidad -->
    <div v-if="selectedVenue" class="fixed inset-0 z-50 flex items-center justify-center bg-black/70" @click.self="selectedVenue = null">
      <div class="bg-[#161824] rounded-2xl border border-white/10 w-full max-w-lg mx-4 shadow-2xl overflow-hidden">
        <div class="px-6 py-4 border-b border-white/5 flex items-center justify-between">
          <h3 class="font-semibold text-white">{{ selectedVenue.name }}</h3>
          <button @click="selectedVenue = null" class="text-gray-500 hover:text-white transition-colors">
            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div class="p-6 space-y-3">
          <p class="text-sm text-gray-400 mb-4">Salas disponibles en este momento</p>
          <div v-for="room in (selectedVenue.rooms || mockRooms)" :key="room.id"
            class="flex items-center justify-between p-3 bg-white/3 rounded-xl border border-white/5">
            <div>
              <p class="text-sm font-medium text-white">{{ room.name }}</p>
              <p class="text-xs text-gray-500">Cap. {{ room.capacity }} · {{ room.type }}</p>
            </div>
            <EstadoBadge :estado="room.available ? 'ACTIVA' : 'EN_CURSO'" />
          </div>
        </div>
        <div class="px-6 pb-5">
          <router-link to="/profesor/clases-propias"
            class="w-full flex justify-center py-2.5 bg-purple-600 hover:bg-purple-500 text-white rounded-lg text-sm font-medium transition-colors">
            Crear clase aquí
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { venueService } from '../../services/venueService'
import EstadoBadge from '../../components/EstadoBadge.vue'

const loading = ref(true)
const venues = ref([])
const selectedVenue = ref(null)
const filters = ref({ q: '', disciplina: '', capacidad: '' })

const gradients = [
  'from-indigo-700 to-purple-700',
  'from-purple-700 to-pink-700',
  'from-cyan-700 to-indigo-700',
  'from-amber-700 to-orange-700',
  'from-emerald-700 to-cyan-700',
  'from-pink-700 to-rose-700',
]

const mockRooms = [
  { id: 1, name: 'Sala Principal', capacity: 25, type: 'Danza/Teatro', available: true },
  { id: 2, name: 'Sala Pequeña', capacity: 10, type: 'Música', available: false },
]

const filteredVenues = computed(() => venues.value.filter(v => {
  if (filters.value.q && !v.name?.toLowerCase().includes(filters.value.q.toLowerCase()) &&
      !v.city?.toLowerCase().includes(filters.value.q.toLowerCase())) return false
  if (filters.value.disciplina && !(v.disciplines || []).includes(filters.value.disciplina)) return false
  if (filters.value.capacidad && (v.maxCapacity || 0) > Number(filters.value.capacidad)) return false
  return true
}))

onMounted(async () => {
  try { venues.value = await venueService.listApproved() } catch { /* ignore */ }
  finally { loading.value = false }
})

const selectVenue = (v) => { selectedVenue.value = v }
</script>
