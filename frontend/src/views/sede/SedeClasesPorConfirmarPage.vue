<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Clases por Confirmar</h1>
    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>
    <div v-else-if="clases.length === 0" class="card text-center py-12"><p class="text-gray-400">No hay clases pendientes de confirmacion.</p></div>
    <div v-else class="space-y-4">
      <div v-for="c in clases" :key="c.id" class="card flex items-center justify-between">
        <div><h3 class="text-white font-medium">{{ c.title }}</h3><p class="text-gray-400 text-sm">{{ c.discipline }} - {{ c.level }}</p><p class="text-gray-500 text-xs">{{ formatDate(c.startTime) }}</p></div>
        <div class="flex space-x-2">
          <button @click="confirmar(c.id, true)" class="btn-primary text-sm bg-green-600 hover:bg-green-700">Realizada</button>
          <button @click="confirmar(c.id, false)" class="btn-danger text-sm">No Realizada</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import venueService from '@/services/venueService'

const clases = ref([])
const loading = ref(true)

onMounted(async () => {
  try { clases.value = await venueService.getPendingClasses() } catch { clases.value = [] }
  loading.value = false
})

async function confirmar(id, realizada) {
  try {
    if (realizada) await venueService.confirmClassRealized(id)
    else await venueService.confirmClassNotRealized(id)
    clases.value = clases.value.filter(c => c.id !== id)
  } catch {}
}

function formatDate(d) { return d ? new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : '' }
</script>
