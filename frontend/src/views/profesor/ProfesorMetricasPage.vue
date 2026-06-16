<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-8">Métricas</h1>
    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Total Clases</h3>
        <p class="text-3xl font-bold text-white">{{ metrics.totalClases || 0 }}</p>
      </div>
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Clases este mes</h3>
        <p class="text-3xl font-bold text-primary">{{ metrics.clasesEsteMes || 0 }}</p>
      </div>
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Total Alumnos</h3>
        <p class="text-3xl font-bold text-green-400">{{ metrics.totalAlumnos || 0 }}</p>
      </div>
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Clases Activas</h3>
        <p class="text-3xl font-bold text-yellow-400">{{ metrics.clasesActivas || 0 }}</p>
      </div>
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
