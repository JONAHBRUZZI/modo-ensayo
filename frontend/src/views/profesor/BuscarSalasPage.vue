<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-4">Agendar Sala</h1>
    <p class="text-gray-400 mb-6">Encuentra la sala perfecta para tu clase.</p>

    <!-- Filtros -->
    <div class="card mb-6 grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
      <select v-model="filtros.comuna" class="input-field text-sm py-2" @change="buscar">
        <option value="">TODAS las comunas</option>
        <option v-for="c in comunas" :key="c" :value="c">{{ c }}</option>
      </select>
      <select v-model="filtros.tipo" class="input-field text-sm py-2" @change="buscar">
        <option value="">TODOS los tipos</option>
        <option value="DANZA">Danza</option>
        <option value="MUSICA">Musica</option>
      </select>
      <div>
        <label class="text-xs text-gray-500 mb-0.5 block">Desde</label>
        <input v-model="filtros.fechaDesde" type="date" class="input-field text-sm py-2" @change="buscar" />
      </div>
      <div>
        <label class="text-xs text-gray-500 mb-0.5 block">Hasta</label>
        <input v-model="filtros.fechaHasta" type="date" class="input-field text-sm py-2" @change="buscar" />
      </div>
      <button @click="buscar" class="btn-primary text-sm">Buscar Salas</button>
    </div>

    <!-- Resultados -->
    <div v-if="loading" class="text-center text-gray-400 py-20">Buscando salas disponibles...</div>
    <div v-else-if="venues.length === 0" class="card text-center py-12"><p class="text-gray-400">No se encontraron salas con esos filtros.</p></div>
    <div v-else class="space-y-4">
      <div v-for="venue in venues" :key="venue.id" class="card">
        <div @click="toggleVenue(venue.id)" class="cursor-pointer flex items-center justify-between">
          <div>
            <h3 class="text-lg font-semibold text-white">{{ venue.name }}</h3>
            <p class="text-gray-400 text-sm">{{ venue.address }}, {{ venue.city }}</p>
          </div>
          <svg :class="['w-5 h-5 text-gray-400 transition-transform', expandedVenue === venue.id && 'rotate-180']" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
        </div>

        <div v-if="expandedVenue === venue.id" class="mt-4 space-y-3">
          <div v-if="venue.rooms?.length">
            <div v-for="room in venue.rooms" :key="room.id" class="bg-[#1a1d2e] rounded-xl p-4">
              <div @click="toggleRoom(room.id)" class="cursor-pointer flex items-center justify-between">
                <div class="flex-1">
                  <p class="text-white font-medium">{{ room.name }}</p>
                  <p class="text-gray-400 text-sm">Cap: {{ room.capacity }} | Piso: {{ room.floorType || 'N/A' }}</p>
                  <p v-if="room.pricePerHour" class="text-primary text-sm font-medium mt-0.5">${{ room.pricePerHour?.toLocaleString() }} / hora</p>
                  <div class="flex flex-wrap gap-1 mt-1">
                    <span v-if="room.hasMirrors" class="text-xs text-gray-500 bg-white/5 px-2 py-0.5 rounded">Espejos</span>
                    <span v-if="room.hasSound" class="text-xs text-gray-500 bg-white/5 px-2 py-0.5 rounded">Sonido</span>
                  </div>
                </div>
                <svg :class="['w-4 h-4 text-gray-500 ml-3 transition-transform', expandedRoom === room.id && 'rotate-180']" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
              </div>

              <div v-if="expandedRoom === room.id" class="mt-3">
                <div v-if="loadingSlots === room.id" class="text-gray-500 text-sm py-2">Cargando horarios...</div>
                <div v-else-if="roomSlots[room.id]?.length" class="space-y-2">
                  <p class="text-xs text-gray-500 mb-2">Horarios disponibles:</p>
                  <div v-for="slot in filteredSlots(room.id)" :key="slot.id" class="flex items-center justify-between bg-[#0d0f1a] rounded-lg p-3">
                    <div>
                      <p class="text-white text-sm">{{ formatDate(slot.startTime) }}</p>
                      <p class="text-gray-400 text-xs">{{ formatTime(slot.startTime) }} - {{ formatTime(slot.endTime) }}</p>
                    </div>
                    <router-link :to="'/alumno/crear-clase?roomId=' + room.id + '&venueId=' + venue.id + '&startTime=' + slot.startTime"
                      class="btn-primary text-xs !py-1.5 !px-3">Agendar</router-link>
                  </div>
                </div>
                <div v-else class="text-gray-500 text-sm py-2">Sin horarios disponibles en este rango</div>
              </div>
            </div>
          </div>
          <p v-else class="text-gray-500 text-sm">No hay salas en esta sede</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import classService from '@/services/classService'
import venueService from '@/services/venueService'

const venues = ref([])
const loading = ref(true)
const expandedVenue = ref(null)
const expandedRoom = ref(null)
const roomSlots = ref({})
const loadingSlots = ref(null)
const comunas = ref([])
const filtros = ref({ comuna: '', tipo: '', fechaDesde: '', fechaHasta: '' })

onMounted(async () => {
  await cargarComunas()
  await buscar()
})

async function cargarComunas() {
  try {
    const data = await classService.getVenues()
    const list = Array.isArray(data) ? data : data?.content || []
    comunas.value = [...new Set(list.map(v => v.city).filter(Boolean))].sort()
  } catch { comunas.value = [] }
}

async function buscar() {
  loading.value = true
  expandedVenue.value = null
  expandedRoom.value = null
  roomSlots.value = {}
  try {
    const data = await classService.getVenues()
    let list = Array.isArray(data) ? data : data?.content || []
    if (filtros.value.comuna) list = list.filter(v => v.city === filtros.value.comuna)
    const venuesWithRooms = await Promise.all(list.map(async (v) => {
      try {
        const rooms = await venueService.getRooms(v.id)
        return { ...v, rooms: Array.isArray(rooms) ? rooms : [] }
      } catch {
        return { ...v, rooms: [] }
      }
    }))
    venues.value = venuesWithRooms
  } catch { venues.value = [] }
  loading.value = false
}

function toggleVenue(id) {
  expandedVenue.value = expandedVenue.value === id ? null : id
  expandedRoom.value = null
}

async function toggleRoom(roomId) {
  if (expandedRoom.value === roomId) { expandedRoom.value = null; return }
  expandedRoom.value = roomId
  if (!roomSlots.value[roomId]) {
    loadingSlots.value = roomId
    try {
      const slots = await venueService.getPublicRoomAvailability(roomId)
      roomSlots.value[roomId] = Array.isArray(slots) ? slots : []
    } catch {
      roomSlots.value[roomId] = []
    }
    loadingSlots.value = null
  }
}

function filteredSlots(roomId) {
  const slots = roomSlots.value[roomId] || []
  if (!filtros.value.fechaDesde && !filtros.value.fechaHasta) return slots
  return slots.filter(s => {
    const d = new Date(s.startTime).toISOString().split('T')[0]
    if (filtros.value.fechaDesde && d < filtros.value.fechaDesde) return false
    if (filtros.value.fechaHasta && d > filtros.value.fechaHasta) return false
    return true
  })
}

function formatDate(d) {
  return d ? new Date(d).toLocaleDateString('es-CL', { weekday: 'short', day: 'numeric', month: 'short' }) : ''
}
function formatTime(d) {
  return d ? new Date(d).toLocaleTimeString('es-CL', { hour: '2-digit', minute: '2-digit' }) : ''
}
</script>
