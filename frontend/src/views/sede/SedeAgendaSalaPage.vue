<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Salas</h1>
    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>
    <div v-else-if="salas.length === 0" class="card text-center py-12"><p class="text-gray-400">No tienes salas registradas.</p><router-link to="/sede/sala-registro" class="btn-primary mt-4 inline-block">Registrar Sala</router-link></div>
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="sala in salas" :key="sala.id" class="card">
        <h3 class="text-lg font-semibold text-white mb-2">{{ sala.name }}</h3>
        <p class="text-gray-400 text-sm">Capacidad: {{ sala.capacity }} | Tipo: {{ sala.type }}</p>
        <div class="mt-4 flex space-x-2">
          <router-link :to="'/sede/salas/' + sala.id + '/agenda'" class="btn-primary text-sm">Agenda</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import venueService from '@/services/venueService'

const salas = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const venues = await venueService.getMyVenues()
    const vArr = Array.isArray(venues) ? venues : venues.content || []
    if (vArr.length > 0) {
      salas.value = await venueService.getVenueRooms(vArr[0].id)
    }
  } catch { salas.value = [] }
  loading.value = false
})
</script>
