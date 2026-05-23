<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Metricas</h1>
    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>
    <div v-else class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div class="card"><h3 class="text-gray-400 text-sm mb-1">Total Clases</h3><p class="text-3xl font-bold text-white">{{ metrics.totalClases || 0 }}</p></div>
      <div class="card"><h3 class="text-gray-400 text-sm mb-1">Asistencia Promedio</h3><p class="text-3xl font-bold text-primary">{{ metrics.asistenciaPromedio || 0 }}%</p></div>
      <div class="card"><h3 class="text-gray-400 text-sm mb-1">Rating</h3><p class="text-3xl font-bold text-yellow-400">{{ metrics.rating || 0 }}/5</p></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import classService from '@/services/classService'

const metrics = ref({})
const loading = ref(true)

onMounted(async () => {
  try { metrics.value = await classService.getTeacherMetrics() } catch { metrics.value = {} }
  loading.value = false
})
</script>
