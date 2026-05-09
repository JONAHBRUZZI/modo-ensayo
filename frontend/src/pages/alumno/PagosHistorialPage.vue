<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-white">Historial de Pagos</h1>
        <p class="text-gray-400 text-sm mt-0.5">Registro de todas tus transacciones</p>
      </div>
    </div>

    <!-- Resumen -->
    <div class="grid grid-cols-3 gap-4">
      <div class="bg-[#161824] rounded-xl border border-white/10 p-4">
        <p class="text-xs text-gray-500 mb-1">Total pagado</p>
        <p class="text-2xl font-bold text-white">${{ totalPagado.toLocaleString('es-CL') }}</p>
      </div>
      <div class="bg-[#161824] rounded-xl border border-white/10 p-4">
        <p class="text-xs text-gray-500 mb-1">Transacciones</p>
        <p class="text-2xl font-bold text-white">{{ pagos.length }}</p>
      </div>
      <div class="bg-[#161824] rounded-xl border border-white/10 p-4">
        <p class="text-xs text-gray-500 mb-1">Pendientes</p>
        <p class="text-2xl font-bold text-amber-400">{{ pagosPendientes }}</p>
      </div>
    </div>

    <!-- Filtros -->
    <div class="flex gap-2 flex-wrap">
      <button v-for="f in ['Todos', 'Pagados', 'Pendientes', 'Cancelados']" :key="f"
        @click="filtroEstado = f"
        :class="filtroEstado === f ? 'bg-indigo-600 text-white' : 'bg-[#161824] border border-white/10 text-gray-400 hover:text-white'"
        class="px-3 py-1.5 rounded-lg text-xs font-medium transition-colors">
        {{ f }}
      </button>
    </div>

    <!-- Skeleton -->
    <div v-if="loading" class="space-y-3">
      <div v-for="i in 5" :key="i" class="h-20 bg-[#161824] rounded-xl border border-white/10 animate-pulse"></div>
    </div>

    <!-- Lista -->
    <div v-else-if="filteredPagos.length" class="space-y-3">
      <div v-for="p in filteredPagos" :key="p.id"
        class="bg-[#161824] rounded-xl border border-white/10 p-4 flex items-center gap-4">
        <div class="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0"
          :class="p.status === 'PAGADO' ? 'bg-emerald-500/20' : p.status === 'PENDIENTE' ? 'bg-amber-500/20' : 'bg-red-500/20'">
          <svg class="w-4.5 h-4.5" :class="p.status === 'PAGADO' ? 'text-emerald-400' : p.status === 'PENDIENTE' ? 'text-amber-400' : 'text-red-400'"
            fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path v-if="p.status === 'PAGADO'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            <path v-else-if="p.status === 'PENDIENTE'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </div>
        <div class="flex-1 min-w-0">
          <p class="text-sm font-medium text-white truncate">{{ p.className || 'Clase' }}</p>
          <p class="text-xs text-gray-500 mt-0.5">{{ formatFecha(p.createdAt) }} · Ref: {{ p.reference || p.id }}</p>
        </div>
        <div class="text-right flex-shrink-0">
          <p class="text-base font-bold text-white">${{ (p.amount || 0).toLocaleString('es-CL') }}</p>
          <EstadoBadge :estado="p.status || 'PENDIENTE'" />
        </div>
      </div>
    </div>

    <!-- Empty -->
    <div v-else class="text-center py-14 bg-[#161824] rounded-xl border border-white/10">
      <p class="text-gray-400 text-sm">No hay transacciones para mostrar</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { cartService } from '../../services/cartService'
import EstadoBadge from '../../components/EstadoBadge.vue'

const loading = ref(true)
const pagos = ref([])
const filtroEstado = ref('Todos')

const filteredPagos = computed(() => {
  if (filtroEstado.value === 'Todos') return pagos.value
  const map = { Pagados: 'PAGADO', Pendientes: 'PENDIENTE', Cancelados: 'CANCELADO' }
  return pagos.value.filter(p => p.status === map[filtroEstado.value])
})

const totalPagado = computed(() => pagos.value.filter(p => p.status === 'PAGADO').reduce((s, p) => s + (p.amount || 0), 0))
const pagosPendientes = computed(() => pagos.value.filter(p => p.status === 'PENDIENTE').length)

onMounted(async () => {
  try {
    const data = await cartService.getPaymentHistory?.()
    pagos.value = Array.isArray(data) ? data : []
  } catch { pagos.value = [] }
  finally { loading.value = false }
})

const formatFecha = (d) => d ? new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'short', year: 'numeric' }) : '—'
</script>
