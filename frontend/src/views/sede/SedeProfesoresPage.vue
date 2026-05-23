<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Profesores de mi Sede</h1>
    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>
    <div v-else-if="profesores.length === 0" class="card text-center py-12"><p class="text-gray-400">No hay profesores asociados.</p></div>
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
