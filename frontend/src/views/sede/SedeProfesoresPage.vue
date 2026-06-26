<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-8">Profesores de mi Sede</h1>
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
      <p class="text-gray-400 font-medium">Sin maestros asociados</p>
      <p class='text-gray-600 text-sm mt-1'>Los profesores que dicten clases en tu sede aparecerán aquí.</p>
    </div>
    <div v-else class="space-y-4">
      <div v-for="p in profesores" :key="p.id" class="card flex items-center justify-between">
        <div class="flex items-center space-x-4">
          <div class="w-10 h-10 bg-primary/20 rounded-full flex items-center justify-center text-primary font-bold">{{ (p.name || p.fullName || 'P').charAt(0) }}</div>
          <div><h3 class="text-white font-medium">{{ p.name || p.fullName }}</h3><p class="text-gray-400 text-sm">{{ p.email }}</p></div>
        </div>
        <EstadoBadge :status="p.status || 'ACTIVE'" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import venueService from '@/services/venueService'
import EstadoBadge from '@/components/EstadoBadge.vue'

const profesores = ref([])
const loading = ref(true)

onMounted(async () => {
  try { profesores.value = await venueService.getVenueProfessors() } catch { profesores.value = [] }
  loading.value = false
})
</script>
