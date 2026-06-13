<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Panel de Administracion</h1>

    <!-- Stats cards -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-6 mb-10">
      <router-link to="/admin/usuarios" class="card hover:border-primary/50 transition-colors group cursor-pointer">
        <h3 class="text-gray-400 text-sm mb-1">Usuarios</h3>
        <p class="text-3xl font-bold text-white group-hover:text-primary">{{ stats.usuarios || 0 }}</p>
      </router-link>
      <router-link to="/admin/sedes" class="card hover:border-primary/50 transition-colors group cursor-pointer">
        <h3 class="text-gray-400 text-sm mb-1">Sedes</h3>
        <p class="text-3xl font-bold text-primary">{{ stats.sedes || 0 }}</p>
      </router-link>
      <router-link to="/admin/roles" class="card hover:border-primary/50 transition-colors group cursor-pointer">
        <h3 class="text-gray-400 text-sm mb-1">Verif. Pendientes</h3>
        <p class="text-3xl font-bold text-yellow-400">{{ stats.pendientes || 0 }}</p>
      </router-link>
      <router-link to="/admin/sedes" class="card hover:border-primary/50 transition-colors group cursor-pointer">
        <h3 class="text-gray-400 text-sm mb-1">Sedes Pend.</h3>
        <p class="text-3xl font-bold text-red-400">{{ stats.sedesPendientes || 0 }}</p>
      </router-link>
    </div>

    <!-- Charts grid -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-10">
      <!-- Usuarios por rol (Pie) -->
      <div class="card flex flex-col items-center">
        <h3 class="text-white font-medium mb-4 self-start">Usuarios por Rol</h3>
        <div class="w-64 h-64">
          <Pie :data="chartUsuariosRol" :options="pieOptions" />
        </div>
      </div>

      <!-- Sedes por estado (Bar) -->
      <div class="card flex flex-col items-center">
        <h3 class="text-white font-medium mb-4 self-start">Sedes por Estado</h3>
        <div class="w-full h-64">
          <Bar :data="chartSedesEstado" :options="barOptions" />
        </div>
      </div>

      <!-- Clases (Bar) -->
      <div class="card flex flex-col items-center">
        <h3 class="text-white font-medium mb-4 self-start">Clases</h3>
        <div class="w-full h-64">
          <Bar :data="chartClases" :options="clasesOptions" />
        </div>
      </div>

      <!-- Ingresos mensuales (Line) -->
      <div class="card flex flex-col items-center">
        <h3 class="text-white font-medium mb-4 self-start">Ingresos Mensuales</h3>
        <div v-if="chartIngresos.labels.length > 0" class="w-full h-64">
          <Line :data="chartIngresos" :options="lineOptions" />
        </div>
        <p v-else class="text-gray-500 text-sm text-center py-16">Sin datos de ingresos aun</p>
      </div>
    </div>

    <!-- Métricas del docente -->
    <h2 class="text-lg font-semibold text-white mb-4 mt-6">Metricas de Rendimiento</h2>
    <div class="grid grid-cols-2 md:grid-cols-5 gap-4 mb-6">

      <div class="card text-center">
        <h3 class="text-gray-400 text-xs mb-1">M1 · Tasa Ocupacion</h3>
        <p class="text-3xl font-bold mt-1"
           :class="(stats.tasaOcupacion||0) >= 80 ? 'text-green-400' : 'text-yellow-400'">
          {{ stats.tasaOcupacion || 0 }}%
        </p>
        <p class="text-xs text-gray-500 mt-1">objetivo &gt; 80%</p>
      </div>

      <div class="card text-center">
        <h3 class="text-gray-400 text-xs mb-1">M2 · Conversion a Pago</h3>
        <p class="text-3xl font-bold mt-1"
           :class="(stats.conversionPago||0) >= 70 ? 'text-green-400' : 'text-red-400'">
          {{ stats.conversionPago || 0 }}%
        </p>
        <p class="text-xs text-gray-500 mt-1">objetivo &gt; 70%</p>
      </div>

      <div class="card text-center">
        <h3 class="text-gray-400 text-xs mb-1">M3 · Tasa Asistencia</h3>
        <p class="text-3xl font-bold mt-1"
           :class="(stats.tasaAsistencia||0) >= 90 ? 'text-green-400' : 'text-yellow-400'">
          {{ stats.tasaAsistencia || 0 }}%
        </p>
        <p class="text-xs text-gray-500 mt-1">objetivo &gt; 90%</p>
      </div>

      <div class="card text-center">
        <h3 class="text-gray-400 text-xs mb-1">M4 · Disponibilidad</h3>
        <a href="https://uptimerobot.com/dashboard"
           target="_blank"
           class="text-xl font-bold text-green-400 hover:underline block mt-1">
          Ver reporte ↗
        </a>
        <p class="text-xs text-gray-500 mt-1">objetivo &gt; 95%</p>
      </div>

      <div class="card text-center">
        <h3 class="text-gray-400 text-xs mb-1">M5 · Pagos Exitosos</h3>
        <p class="text-3xl font-bold mt-1"
           :class="(stats.tasaPagosExitosos||100) >= 98 ? 'text-green-400' : 'text-yellow-400'">
          {{ stats.tasaPagosExitosos || 100 }}%
        </p>
        <p class="text-xs text-gray-500 mt-1">objetivo &gt; 98%</p>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import adminService from '@/services/adminService'
import { Pie, Bar, Line } from 'vue-chartjs'
import {
  Chart as ChartJS, ArcElement, Tooltip, Legend, CategoryScale,
  LinearScale, BarElement, PointElement, LineElement, Filler
} from 'chart.js'

ChartJS.register(ArcElement, Tooltip, Legend, CategoryScale, LinearScale, BarElement, PointElement, LineElement, Filler)

const stats = ref({})

const chartUsuariosRol = computed(() => ({
  labels: Object.keys(stats.value.usuariosPorRol || {}),
  datasets: [{
    data: Object.values(stats.value.usuariosPorRol || {}),
    backgroundColor: ['#ef4444', '#3b82f6', '#10b981', '#8b5cf6', '#f59e0b', '#ec4899']
  }]
}))

const chartSedesEstado = computed(() => ({
  labels: Object.keys(stats.value.sedesPorEstado || {}).map(s => s.replace(/_/g, ' ')),
  datasets: [{
    label: 'Sedes',
    data: Object.values(stats.value.sedesPorEstado || {}),
    backgroundColor: ['#10b981', '#f59e0b', '#ef4444', '#f97316']
  }]
}))

const chartClases = computed(() => ({
  labels: ['Total', 'Realizadas'],
  datasets: [{
    label: 'Clases',
    data: [stats.value.totalClases || 0, stats.value.clasesRealizadas || 0],
    backgroundColor: ['#8b5cf6', '#10b981']
  }]
}))

const chartIngresos = computed(() => ({
  labels: (stats.value.ingresosMensuales || []).map(i => i.mes?.substring(5) || ''),
  datasets: [{
    label: 'Ingresos $',
    data: (stats.value.ingresosMensuales || []).map(i => i.ingresos || 0),
    borderColor: '#8b5cf6',
    backgroundColor: 'rgba(139, 92, 246, 0.15)',
    fill: true,
    tension: 0.3
  }]
}))

const pieOptions = { plugins: { legend: { labels: { color: '#9ca3af' } } } }
const barOptions = { plugins: { legend: { display: false } }, scales: { x: { ticks: { color: '#9ca3af' } }, y: { ticks: { color: '#9ca3af' } } } }
const clasesOptions = { plugins: { legend: { display: false } }, scales: { x: { ticks: { color: '#9ca3af' } }, y: { ticks: { color: '#9ca3af' } } } }
const lineOptions = { plugins: { legend: { display: false } }, scales: { x: { ticks: { color: '#9ca3af' } }, y: { ticks: { color: '#9ca3af' } } } }

onMounted(async () => {
  try { stats.value = await adminService.getStats() } catch { stats.value = {} }
})
</script>
