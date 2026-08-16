<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-8">Pagos Recibidos</h1>

    <!-- Resumen financiero -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Retenido (pendiente)</h3>
        <p class="text-3xl font-bold text-yellow-400">${{ resumen.totalRetenido?.toLocaleString('es-CL') || 0 }}</p>
        <p class="text-gray-500 text-xs mt-1">Se libera al confirmar la clase</p>
      </div>
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Liberado este mes</h3>
        <p class="text-3xl font-bold text-green-400">${{ resumen.totalLiberadoMes?.toLocaleString('es-CL') || 0 }}</p>
      </div>
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Liberado acumulado</h3>
        <p class="text-3xl font-bold text-primary">${{ resumen.totalLiberadoAcumulado?.toLocaleString('es-CL') || 0 }}</p>
      </div>
    </div>

    <!-- Lista de pagos -->
    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>
    <div v-else-if="pagos.length === 0" class="card text-center py-12">
      <p class="text-gray-400">Aun no hay pagos registrados.</p>
      <p class="text-gray-500 text-sm mt-2">Los pagos aparecen cuando alumnos se inscriben en tus clases.</p>
    </div>
    <div v-else class="space-y-3">
      <div v-for="p in pagos" :key="p.id" class="card flex items-center justify-between">
        <div>
          <h3 class="text-white font-medium">{{ p.classTitle || 'Clase' }}</h3>
          <p class="text-gray-400 text-sm">{{ formatDate(p.date) }}</p>
        </div>
        <div class="text-right flex items-center space-x-4">
          <p class="text-green-400 font-semibold">${{ p.amount?.toLocaleString('es-CL') }}</p>
          <span :class="statusClass(p.status)" class="text-xs font-medium px-2 py-1 rounded-full">
            {{ statusLabel(p.status) }}
          </span>
        </div>
      </div>
    </div>

    <!-- Liquidaciones a tu cuenta bancaria -->
    <h2 class="text-xl font-bold text-white mt-12 mb-4">Liquidaciones</h2>
    <div v-if="payouts.length === 0" class="card text-center py-10">
      <p class="text-gray-400">Aún no hay liquidaciones.</p>
      <p class="text-gray-500 text-sm mt-2">Se generan cuando se confirma una clase realizada y se pagan a tu cuenta bancaria.</p>
    </div>
    <div v-else class="space-y-3">
      <div v-for="po in payouts" :key="po.id" class="card flex items-center justify-between">
        <div>
          <h3 class="text-white font-medium">{{ po.class?.title || 'Clase' }}</h3>
          <p class="text-gray-500 text-xs mt-1">
            Bruto ${{ Number(po.grossAmount).toLocaleString('es-CL') }} ·
            Comisión ${{ Number(po.commissionAmount).toLocaleString('es-CL') }}
          </p>
        </div>
        <div class="text-right flex items-center space-x-4">
          <p class="text-green-400 font-semibold">${{ Number(po.netAmount).toLocaleString('es-CL') }}</p>
          <span :class="payoutStatusClass(po.status)" class="text-xs font-medium px-2 py-1 rounded-full">
            {{ payoutStatusLabel(po.status) }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import classService from '@/services/classService'
import sellerService from '@/services/sellerService'
import { formatDate } from '@/utils/dateFormatter'

const pagos = ref([])
const payouts = ref([])
const resumen = ref({ totalRetenido: 0, totalLiberadoMes: 0, totalLiberadoAcumulado: 0 })
const loading = ref(true)

onMounted(async () => {
  try {
    const data = await classService.getTeacherEarnings()
    pagos.value = data?.pagos || []
    resumen.value = data?.resumen || { totalRetenido: 0, totalLiberadoMes: 0, totalLiberadoAcumulado: 0 }
  } catch (e) {
    pagos.value = []
  }
  try {
    payouts.value = await sellerService.getPayouts()
  } catch (e) {
    payouts.value = []
  }
  loading.value = false
})

function payoutStatusLabel(status) {
  const labels = { PENDING: 'Pendiente', PAID: 'Pagado', FAILED: 'Fallido' }
  return labels[status] || status
}

function payoutStatusClass(status) {
  const classes = {
    PENDING: 'bg-yellow-500/20 text-yellow-400',
    PAID: 'bg-green-500/20 text-green-400',
    FAILED: 'bg-red-500/20 text-red-400'
  }
  return classes[status] || 'bg-gray-500/20 text-gray-400'
}

function statusLabel(status) {
  const labels = { RETAINED: 'Retenido', RELEASED: 'Liberado', REFUND_PENDING: 'En devolucion', REFUNDED: 'Devuelto', FAILED: 'Fallido' }
  return labels[status] || status
}

function statusClass(status) {
  const classes = {
    RETAINED: 'bg-yellow-500/20 text-yellow-400',
    RELEASED: 'bg-green-500/20 text-green-400',
    REFUND_PENDING: 'bg-orange-500/20 text-orange-400',
    REFUNDED: 'bg-gray-500/20 text-gray-400',
    FAILED: 'bg-red-500/20 text-red-400'
  }
  return classes[status] || 'bg-gray-500/20 text-gray-400'
}
</script>
