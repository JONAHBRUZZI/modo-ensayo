<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-2">Panel de Sede</h1>
    <p class="text-gray-400 mb-8">Gestiona tu sede de ensayo</p>

    <!-- Stats operativos -->
    <div class="grid grid-cols-2 md:grid-cols-3 gap-6 mb-8">
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Salas</h3>
        <p class="text-3xl font-bold text-white">{{ stats.salas || 0 }}</p>
      </div>
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Por Confirmar</h3>
        <p class="text-3xl font-bold text-yellow-400">{{ stats.porConfirmar || 0 }}</p>
      </div>
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Ingreso Total</h3>
        <p class="text-3xl font-bold text-green-400">${{ ingresoTotal.toLocaleString('es-CL') }}</p>
        <p class="text-[10px] text-gray-500 mt-1">Arriendo + clases (antes de pagar profes)</p>
      </div>
    </div>

    <!-- Ingresos y egresos -->
    <div class="flex items-center justify-between mb-4 flex-wrap gap-3">
      <h2 class="text-lg font-semibold text-white">Ingresos y egresos</h2>
      <div class="inline-flex rounded-lg border border-white/10 overflow-hidden text-sm">
        <button
          @click="setGranularidad('month')"
          :class="granularidad === 'month' ? 'bg-primary text-white' : 'text-gray-400 hover:text-white'"
          class="px-4 py-1.5 transition-colors"
        >Mensual</button>
        <button
          @click="setGranularidad('year')"
          :class="granularidad === 'year' ? 'bg-primary text-white' : 'text-gray-400 hover:text-white'"
          class="px-4 py-1.5 transition-colors"
        >Anual</button>
      </div>
    </div>

    <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Ingreso Arriendo</h3>
        <p class="text-2xl font-bold text-blue-400">${{ totales.arriendo.toLocaleString('es-CL') }}</p>
        <p class="text-xs text-gray-500 mt-1">Reservas de sala pagadas</p>
      </div>
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Ingreso Clases</h3>
        <p class="text-2xl font-bold text-purple-400">${{ totales.clases.toLocaleString('es-CL') }}</p>
        <p class="text-xs text-gray-500 mt-1">Recaudación de alumnos</p>
      </div>
      <!-- Egreso profesores: clickeable, despliega el detalle por profesor -->
      <button
        type="button"
        @click="mostrarEgreso = !mostrarEgreso"
        class="card text-left hover:border-primary/50 transition-colors"
      >
        <div class="flex items-center justify-between">
          <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Egreso Profesores</h3>
          <svg :class="['w-4 h-4 text-gray-400 transition-transform', mostrarEgreso && 'rotate-180']" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
        </div>
        <p class="text-2xl font-bold text-red-400">${{ totales.egreso.toLocaleString('es-CL') }}</p>
        <p class="text-xs text-gray-500 mt-1">Honorarios a profes asociados · ver detalle</p>
      </button>
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Resultado</h3>
        <p class="text-2xl font-bold" :class="neto >= 0 ? 'text-green-400' : 'text-red-400'">${{ neto.toLocaleString('es-CL') }}</p>
        <p class="text-xs text-gray-500 mt-1">Ingresos − egresos</p>
      </div>
    </div>

    <!-- Detalle de egreso por profesor -->
    <div v-if="mostrarEgreso" class="card mb-6">
      <h3 class="text-white font-medium mb-3">Pago a profesores asociados</h3>
      <div v-if="payoutsLoading" class="text-gray-500 text-sm py-3">Cargando...</div>
      <div v-else-if="payouts.length === 0" class="text-gray-500 text-sm py-3">Aún no hay honorarios comprometidos.</div>
      <div v-else class="space-y-2">
        <div v-for="p in payouts" :key="p.teacherId" class="flex items-center justify-between border-t border-white/5 pt-2 first:border-0 first:pt-0">
          <div>
            <p class="text-white text-sm font-medium">{{ p.fullName || p.email || 'Profesor' }}</p>
            <p class="text-gray-500 text-xs">{{ p.clases }} clase{{ Number(p.clases) > 1 ? 's' : '' }}</p>
          </div>
          <p class="text-red-400 font-semibold">${{ Number(p.totalHonorario).toLocaleString('es-CL') }}</p>
        </div>
        <div class="flex items-center justify-between border-t border-white/10 pt-3 mt-1">
          <p class="text-gray-300 text-sm font-medium">Total a pagar</p>
          <p class="text-red-400 font-bold">${{ totales.egreso.toLocaleString('es-CL') }}</p>
        </div>
      </div>
    </div>

    <!-- Detalle por período -->
    <div v-if="metricasLoading" class="text-gray-500 text-sm py-6 text-center">Cargando...</div>
    <div v-else-if="metricas.length" class="card overflow-x-auto mb-8">
      <table class="w-full text-sm">
        <thead>
          <tr class="text-gray-500 text-xs uppercase">
            <th class="text-left py-2 px-2">{{ granularidad === 'year' ? 'Año' : 'Mes' }}</th>
            <th class="text-right py-2 px-2">Arriendo</th>
            <th class="text-right py-2 px-2">Clases</th>
            <th class="text-right py-2 px-2">Egreso profes</th>
            <th class="text-right py-2 px-2">Resultado</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in metricas" :key="m.periodo" class="border-t border-white/5">
            <td class="py-2 px-2 text-gray-300">{{ m.periodo }}</td>
            <td class="py-2 px-2 text-right text-blue-400">${{ Number(m.ingresoArriendo).toLocaleString('es-CL') }}</td>
            <td class="py-2 px-2 text-right text-purple-400">${{ Number(m.ingresoClases).toLocaleString('es-CL') }}</td>
            <td class="py-2 px-2 text-right text-red-400">${{ Number(m.egresoProfes).toLocaleString('es-CL') }}</td>
            <td class="py-2 px-2 text-right font-medium" :class="Number(m.total) >= 0 ? 'text-green-400' : 'text-red-400'">${{ Number(m.total).toLocaleString('es-CL') }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-else class="card text-center py-8 mb-8">
      <p class="text-gray-500 text-sm">Aún no hay movimientos registrados.</p>
    </div>

    <!-- Alerta urgente: clases pendientes de confirmación -->
    <div v-if="stats.porConfirmar > 0" class="mb-8 rounded-xl bg-yellow-500/10 border border-yellow-500/30 px-5 py-4 flex items-center justify-between gap-4 flex-wrap">
      <div class="flex items-center gap-3">
        <svg class="w-5 h-5 text-yellow-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
        </svg>
        <span class="text-yellow-200 text-sm font-medium">
          {{ stats.porConfirmar === 1 ? 'Hay 1 clase esperando tu confirmación.' : `Hay ${stats.porConfirmar} clases esperando tu confirmación.` }}
        </span>
      </div>
      <router-link to="/sede/clases-por-confirmar" class="px-4 py-2 rounded-lg bg-yellow-500 text-black text-sm font-semibold hover:bg-yellow-400 transition-colors whitespace-nowrap flex-shrink-0">
        Confirmar ahora
      </router-link>
    </div>

    <!-- Acciones rápidas -->
    <div class="flex flex-wrap gap-3">
      <router-link to="/sede/crear-clase" class="btn-primary inline-flex items-center gap-2">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        Crear Clase
      </router-link>
      <router-link to="/sede/calendario" class="inline-flex items-center gap-2 px-4 py-2 rounded-lg border border-[var(--border-subtle)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:border-primary/50 transition-colors text-sm">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
        </svg>
        Ver Calendario
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import venueService from '@/services/venueService'

const stats = ref({ salas: 0, porConfirmar: 0 })
const metricas = ref([])
const metricasLoading = ref(true)
const granularidad = ref('month')

// Egreso a profesores: detalle desplegable.
const mostrarEgreso = ref(false)
const payouts = ref([])
const payoutsLoading = ref(true)

// Totales sumando todos los períodos.
const totales = computed(() => {
  return metricas.value.reduce((acc, m) => {
    acc.arriendo += Number(m.ingresoArriendo || 0)
    acc.clases += Number(m.ingresoClases || 0)
    acc.egreso += Number(m.egresoProfes || 0)
    return acc
  }, { arriendo: 0, clases: 0, egreso: 0 })
})
// Ingreso = arriendo + recaudación de clases (antes de pagar profes).
const ingresoTotal = computed(() => totales.value.arriendo + totales.value.clases)
// Resultado neto = ingresos − egresos.
const neto = computed(() => ingresoTotal.value - totales.value.egreso)

onMounted(async () => {
  await cargarMetricas()
  try { payouts.value = await venueService.getVenueTeacherPayouts() } catch { payouts.value = [] }
  payoutsLoading.value = false

  try {
    const pendientes = await venueService.getPendingClasses()
    stats.value.porConfirmar = Array.isArray(pendientes) ? pendientes.length : 0
  } catch { stats.value.porConfirmar = 0 }

  try {
    const myVenues = await venueService.getMyVenues()
    const vArr = Array.isArray(myVenues) ? myVenues : myVenues?.content || []
    let totalSalas = 0
    for (const v of vArr) {
      try {
        const rooms = await venueService.getVenueRooms(v.id)
        totalSalas += Array.isArray(rooms) ? rooms.length : 0
      } catch (err) {
        console.error('Error al cargar salas de la sede', err)
      }
    }
    stats.value.salas = totalSalas
  } catch (err) {
    console.error('Error al cargar salas de la sede', err)
  }
})

async function cargarMetricas() {
  metricasLoading.value = true
  try {
    metricas.value = await venueService.getVenueMetrics(granularidad.value)
  } catch (err) {
    console.error('Error al cargar métricas de la sede', err)
    metricas.value = []
  }
  metricasLoading.value = false
}

function setGranularidad(g) {
  if (granularidad.value === g) return
  granularidad.value = g
  cargarMetricas()
}
</script>
