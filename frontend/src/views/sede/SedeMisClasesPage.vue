<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-8">Clases de mi Sede</h1>
    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>
    <div v-else-if="clases.length === 0" class="card text-center py-16">
      <div class="w-14 h-14 bg-[var(--bg-elevated)] rounded-2xl flex items-center justify-center mx-auto mb-4">
        <svg class="w-7 h-7 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
        </svg>
      </div>
      <p class="text-gray-400 font-medium">Sin clases programadas</p>
      <p class='text-gray-600 text-sm mt-1'>Crea una clase para que aparezca aquí.</p>
      <router-link to="/sede/crear-clase" class="btn-primary inline-flex mt-4">Crear clase</router-link>
    </div>
    <div v-else class="space-y-4">
      <div v-for="c in clases" :key="c.id" class="card flex items-center justify-between">
        <div><h3 class="text-white font-medium">{{ c.title }}</h3><p class="text-gray-400 text-sm">{{ c.discipline }} — {{ c.roomName }}</p><p class="text-gray-500 text-xs">{{ formatDate(c.startTime) }}</p></div>
        <EstadoBadge :status="c.status" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import venueService from '@/services/venueService'
import EstadoBadge from '@/components/EstadoBadge.vue'
import { formatDate } from '@/utils/dateFormatter'

const clases = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const data = await venueService.getVenueClasses()
    clases.value = Array.isArray(data) ? data : []
  } catch (e) { clases.value = [] }
  loading.value = false
})
</script>
