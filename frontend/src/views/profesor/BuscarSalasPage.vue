<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Agendar Sala</h1>
    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>
    <div v-else-if="venues.length === 0" class="card text-center py-12"><p class="text-gray-400">No hay sedes disponibles.</p></div>
    <div v-else class="space-y-4">
      <!-- Venue card -->
      <div v-for="venue in venues" :key="venue.id" class="card">
        <div @click="toggleVenue(venue.id)" class="cursor-pointer flex items-center justify-between">
          <div>
            <h3 class="text-lg font-semibold text-white">{{ venue.name }}</h3>
            <p class="text-gray-400 text-sm">{{ venue.address }}, {{ venue.city }}</p>
          </div>
          <svg :class="['w-5 h-5 text-gray-400 transition-transform', expandedVenue === venue.id && 'rotate-180']" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
        </div>

        <!-- Rooms (expandido) -->
        <div v-if="expandedVenue === venue.id" class="mt-4 space-y-3">
          <div v-if="venue.rooms?.length" class="space-y-3">
            <div v-for="room in venue.rooms" :key="room.id" class="bg-[#1a1d2e] rounded-xl p-4">
              <div @click="toggleRoom(room.id)" class="cursor-pointer flex items-center justify-between">
                <div class="flex-1">
                  <p class="text-white font-medium">{{ room.name }}</p>
                  <p class="text-gray-400 text-sm">Capacidad: {{ room.capacity }} | Piso: {{ room.floorType || 'N/A' }} | {{ room.equipment || '' }}</p>
                  <div class="flex flex-wrap gap-1 mt-1">
                    <span v-if="room.hasMirrors" class="text-xs text-gray-500 bg-white/5 px-2 py-0.5 rounded">Espejos</span>
                    <span v-if="room.hasSound" class="text-xs text-gray-500 bg-white/5 px-2 py-0.5 rounded">Sonido</span>
                  </div>
                </div>
                <svg :class="['w-4 h-4 text-gray-500 ml-3 transition-transform', expandedRoom === room.id && 'rotate-180']" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
              </div>

              <!-- Horarios (expandido) -->
              <div v-if="expandedRoom === room.id" class="mt-3">
                <div v-if="loadingSlots === room.id" class="text-gray-500 text-sm py-2">Cargando horarios...</div>
                <div v-else-if="roomSlots[room.id]?.length" class="space-y-2">
                  <p class="text-xs text-gray-500 mb-2">Horarios disponibles:</p>
                  <div v-for="slot in roomSlots[room.id]" :key="slot.id" class="flex items-center justify-between bg-[#0d0f1a] rounded-lg p-3">
                    <div>
                      <p class="text-white text-sm">{{ formatSlot(slot.startTime) }} - {{ formatSlot(slot.endTime) }}</p>
                    </div>
                    <router-link :to="'/alumno/crear-clase?roomId=' + room.id + '&venueId=' + venue.id + '&startTime=' + slot.startTime"
                      class="btn-primary text-xs !py-1.5 !px-3">Agendar</router-link>
                  </div>
                </div>
                <div v-else class="text-gray-500 text-sm py-2">Sin horarios disponibles</div>
              </div>
            </div>
          </div>
          <p v-else class="text-gray-500 text-sm">No hay salas registradas en esta sede</p>
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

onMounted(async () => {
  try {
    const data = await classService.getVenues()
    const list = Array.isArray(data) ? data : data?.content || []
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
})

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

function formatSlot(d) {
  return d ? new Date(d).toLocaleTimeString('es-CL', { hour: '2-digit', minute: '2-digit' }) : ''
}
</script>
