<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Clases Agendadas</h1>
    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>
    <div v-else-if="clases.length === 0" class="card text-center py-12"><p class="text-gray-400">No has creado clases.</p></div>
    <div v-else class="space-y-4">
      <div v-for="c in clases" :key="c.id" class="card flex items-center justify-between">
        <div><h3 class="text-white font-medium">{{ c.title }}</h3><p class="text-gray-400 text-sm">{{ c.discipline }} - {{ c.level }}</p><p class="text-gray-500 text-xs">{{ formatDate(c.startTime) }}</p></div>
        <div class="flex items-center space-x-4">
          <span class="text-primary font-semibold">${{ c.price?.toLocaleString() }}</span>
          <router-link v-if="c.status === 'DRAFT'" :to="'/alumno/crear-clase?edit=' + c.id + '&roomId=' + c.roomId + '&venueId=' + c.venueId" class="btn-primary text-xs !py-1.5 !px-3">Crear Clase</router-link>
          <span v-else class="badge badge-green text-xs">Clase Creada</span>
          <router-link :to="'/profesor/asistencia/' + c.id" class="btn-primary text-xs !py-1.5 !px-3">Asistencia</router-link>
          <EstadoBadge :status="c.status" />
        </div>
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
  try { clases.value = await classService.getTeacherPropias() } catch { clases.value = [] }
  loading.value = false
})

function formatDate(d) { return d ? new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : '' }
</script>
