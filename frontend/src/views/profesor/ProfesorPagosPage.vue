<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Pagos Recibidos</h1>
    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>
    <div v-else-if="pagos.length === 0" class="card text-center py-12"><p class="text-gray-400">No has recibido pagos.</p></div>
    <div v-else class="space-y-4">
      <div v-for="p in pagos" :key="p.id" class="card flex items-center justify-between">
        <div><h3 class="text-white font-medium">{{ p.classTitle || 'Clase' }}</h3><p class="text-gray-400 text-sm">{{ formatDate(p.date) }}</p></div>
        <div class="text-right"><p class="text-green-400 font-semibold">${{ p.amount?.toLocaleString() }}</p><EstadoBadge :status="p.status" /></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import classService from '@/services/classService'
import EstadoBadge from '@/components/EstadoBadge.vue'

const pagos = ref([])
const loading = ref(true)

onMounted(async () => {
  try { pagos.value = await classService.getTeacherEarnings() } catch { pagos.value = [] }
  loading.value = false
})

function formatDate(d) { return d ? new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'short', year: 'numeric' }) : '' }
</script>
