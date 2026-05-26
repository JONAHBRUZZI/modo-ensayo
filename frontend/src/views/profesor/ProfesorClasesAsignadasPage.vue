<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Clases Asignadas</h1>
    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>
    <div v-else-if="clases.length === 0" class="card text-center py-12"><p class="text-gray-400">No tienes clases asignadas.</p></div>
    <div v-else class="space-y-4">
      <div v-for="c in clases" :key="c.id" class="card flex items-center justify-between">
        <div><h3 class="text-white font-medium">{{ c.title }}</h3><p class="text-gray-400 text-sm">{{ c.discipline }} - {{ c.level }}</p><p class="text-gray-500 text-xs">{{ formatDate(c.startTime) }}</p></div>
        <router-link :to="'/profesor/asistencia/' + c.id" class="btn-primary text-sm">Asistencia</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import classService from '@/services/classService'
import EstadoBadge from '@/components/EstadoBadge.vue'

const clases = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const data = await classService.getTeacherAsignadas()
    clases.value = Array.isArray(data) ? data : data?.content || []
  } catch { clases.value = [] }
  loading.value = false
})

function formatDate(d) {
  return d ? new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : ''
}
</script>
