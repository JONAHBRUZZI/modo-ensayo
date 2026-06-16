<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-8">Mis Clases</h1>
    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>
    <div v-else-if="clases.length === 0" class="card text-center py-16">
      <div class="w-14 h-14 bg-[#1a1d2e] rounded-2xl flex items-center justify-center mx-auto mb-4">
        <svg class="w-7 h-7 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/>
        </svg>
      </div>
      <p class="text-gray-400 font-medium">Aún no tienes clases inscritas</p>
      <p class='text-gray-600 text-sm mt-1'>Explora el cronograma y reserva la primera.</p>
      <router-link to="/classes" class="btn-primary inline-flex mt-4">Buscar clases</router-link>
    </div>
    <div v-else class="space-y-8">
      <div v-for="(grupo, nombre) in clasesAgrupadas" :key="nombre" class="space-y-3">
        <h2 class="text-lg font-semibold text-gray-300 border-b border-white/10 pb-2">{{ nombre }}</h2>
        <div v-for="c in grupo" :key="c.enrollmentId || c.classId" class="card">
          <div class="flex items-center justify-between">
            <router-link :to="'/alumno/clases/' + c.classId" class="flex-1 min-w-0 mr-4">
              <h3 class="text-lg font-semibold text-white hover:text-primary transition-colors">{{ c.title }}</h3>
              <p class="text-gray-400 text-sm">{{ c.discipline }} {{ c.level ? '— ' + c.level : '' }}</p>
              <p class="text-gray-500 text-xs mt-1">{{ formatDate(c.startTime) }}</p>
            </router-link>
            <div class="flex items-center gap-3 flex-shrink-0">
              <span class="text-primary font-semibold text-sm">${{ c.price?.toLocaleString('es-CL') }}</span>
              <EstadoBadge :status="c.enrollmentStatus === 'CANCELLED' ? 'CANCELLED' : c.status" />
              <router-link
                v-if="c.status === 'COMPLETED' && c.enrollmentStatus !== 'CANCELLED'"
                to="/reviews"
                class="text-xs bg-yellow-400/10 text-yellow-400 border border-yellow-400/20 px-3 py-1 rounded-full hover:bg-yellow-400/20 transition-colors whitespace-nowrap"
              >
                Dejar reseña
              </router-link>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import paymentService from '@/services/paymentService'
import EstadoBadge from '@/components/EstadoBadge.vue'

const clases = ref([])
const loading = ref(true)

const clasesAgrupadas = computed(() => {
  const grupos = {}
  for (const c of clases.value) {
    const nombre = c.beneficiaryName || 'Yo'
    if (!grupos[nombre]) grupos[nombre] = []
    grupos[nombre].push(c)
  }
  return grupos
})

onMounted(async () => {
  await cargarClases()
})

async function cargarClases() {
  loading.value = true
  try {
    const data = await paymentService.getMyEnrollments()
    clases.value = Array.isArray(data) ? data : []
  } catch {
    clases.value = []
  } finally {
    loading.value = false
  }
}

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })
}
</script>
