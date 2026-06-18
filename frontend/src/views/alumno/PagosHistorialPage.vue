<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-8">Historial de Pagos</h1>
    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>
    <div v-else-if="pagos.length === 0" class="card text-center py-16">
      <div class="w-14 h-14 bg-[#1a1d2e] rounded-2xl flex items-center justify-center mx-auto mb-4">
        <svg class="w-7 h-7 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z"/>
        </svg>
      </div>
      <p class="text-gray-400 font-medium">Sin pagos registrados</p>
      <p class='text-gray-600 text-sm mt-1'>Tus compras y transacciones aparecerán aquí.</p>
    </div>
    <div v-else class="space-y-4">
      <div v-for="p in pagos" :key="p.id" class="card flex items-center justify-between">
        <div>
          <h3 class="text-white font-medium">{{ p.description || p.classTitle || 'Pago' }}</h3>
          <p class="text-gray-400 text-sm">{{ formatDate(p.createdAt || p.date) }}</p>
        </div>
        <div class="text-right">
          <p class="text-primary font-semibold">${{ p.amount?.toLocaleString() }}</p>
          <EstadoBadge :status="p.status" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import paymentService from '@/services/paymentService'
import EstadoBadge from '@/components/EstadoBadge.vue'
import { formatDate } from '@/utils/dateFormatter'

const pagos = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const data = await paymentService.getMyPaymentHistory()
    pagos.value = Array.isArray(data) ? data : []
  } catch {
    pagos.value = []
  } finally {
    loading.value = false
  }
})

</script>
