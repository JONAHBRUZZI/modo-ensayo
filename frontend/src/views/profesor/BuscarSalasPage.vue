<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Buscar Salas</h1>
    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>
    <div v-else-if="venues.length === 0" class="card text-center py-12"><p class="text-gray-400">No hay sedes disponibles.</p></div>
    <div v-else class="space-y-6">
      <div v-for="venue in venues" :key="venue.id" class="card">
        <h3 class="text-lg font-semibold text-white mb-2">{{ venue.name }}</h3>
        <p class="text-gray-400 text-sm mb-4">{{ venue.address }}, {{ venue.city }}</p>
        <div v-if="venue.rooms?.length" class="space-y-3">
          <div v-for="room in venue.rooms" :key="room.id" class="bg-[#1a1d2e] rounded-xl p-4 flex items-center justify-between">
            <div><p class="text-white font-medium">{{ room.name }}</p><p class="text-gray-400 text-sm">Capacidad: {{ room.capacity }} | {{ room.equipment || '' }}</p></div>
            <span class="badge badge-green">Disponible</span>
          </div>
        </div>
        <p v-else class="text-gray-500 text-sm">No hay salas registradas</p>
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
</script>
