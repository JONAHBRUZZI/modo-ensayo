<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Mis Clases</h1>
    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>
    <div v-else-if="clases.length === 0" class="card text-center py-12">
      <p class="text-gray-400">No tienes clases inscritas.</p>
      <router-link to="/classes" class="btn-primary mt-4 inline-block">Buscar Clases</router-link>
    </div>
    <div v-else class="space-y-4">
      <router-link v-for="c in clases" :key="c.classId" :to="'/alumno/clases/' + c.classId" class="card block hover:border-primary/50 transition-colors">
        <div class="flex items-center justify-between">
          <div>
            <h3 class="text-lg font-semibold text-white group-hover:text-primary">{{ c.title }}</h3>
            <p class="text-gray-400 text-sm">{{ c.discipline }} {{ c.level ? '— ' + c.level : '' }}</p>
            <p class="text-gray-500 text-xs mt-1">{{ formatDate(c.startTime) }}</p>
          </div>
          <div class="flex items-center gap-3">
            <span class="text-primary font-semibold text-sm">${{ c.price?.toLocaleString() }}</span>
            <EstadoBadge :status="c.status" />
          </div>
        </div>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import paymentService from '@/services/paymentService'
import EstadoBadge from '@/components/EstadoBadge.vue'

const clases = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const data = await paymentService.getMyEnrollments()
    clases.value = Array.isArray(data) ? data : []
  } catch {
    clases.value = []
  } finally {
    loading.value = false
  }
})

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })
}
</script>
