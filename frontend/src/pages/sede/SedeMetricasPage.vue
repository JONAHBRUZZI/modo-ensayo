<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-white">Métricas de Sede</h1>
      <p class="text-gray-400 text-sm mt-0.5">Rendimiento y ocupación de tus instalaciones</p>
    </div>

    <!-- KPIs -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
      <div v-for="kpi in kpis" :key="kpi.label"
        class="bg-[#161824] rounded-xl border border-white/10 p-4">
        <div :class="kpi.iconBg" class="w-9 h-9 rounded-lg flex items-center justify-center mb-3">
          <svg :class="kpi.iconColor" class="w-4.5 h-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="kpi.icon" />
          </svg>
        </div>
        <p class="text-2xl font-bold text-white">{{ kpi.value }}</p>
        <p class="text-xs text-gray-500 mt-0.5">{{ kpi.label }}</p>
        <p v-if="kpi.delta" class="text-xs mt-1.5" :class="kpi.deltaPositive ? 'text-emerald-400' : 'text-red-400'">
          {{ kpi.deltaPositive ? '▲' : '▼' }} {{ kpi.delta }}
        </p>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- Ocupación por sala -->
      <div class="bg-[#161824] rounded-xl border border-white/10">
        <div class="px-5 py-4 border-b border-white/5">
          <h2 class="font-semibold text-white text-sm">Ocupación por Sala</h2>
          <p class="text-xs text-gray-500 mt-0.5">Promedio del último mes</p>
        </div>
        <div class="p-5 space-y-4">
          <div v-for="sala in ocupacion" :key="sala.name">
            <div class="flex items-center justify-between mb-1.5">
              <span class="text-sm text-gray-300">{{ sala.name }}</span>
              <span class="text-sm font-semibold text-white">{{ sala.pct }}%</span>
            </div>
            <div class="w-full bg-white/5 rounded-full h-2">
              <div class="h-2 rounded-full transition-all duration-700"
                :class="sala.pct >= 80 ? 'bg-emerald-500' : sala.pct >= 50 ? 'bg-indigo-500' : 'bg-amber-500'"
                :style="{ width: `${sala.pct}%` }" />
            </div>
          </div>
        </div>
      </div>

      <!-- Horarios más usados -->
      <div class="bg-[#161824] rounded-xl border border-white/10">
        <div class="px-5 py-4 border-b border-white/5">
          <h2 class="font-semibold text-white text-sm">Horarios con más clases</h2>
          <p class="text-xs text-gray-500 mt-0.5">Este mes</p>
        </div>
        <div class="divide-y divide-white/5">
          <div v-for="h in horarios" :key="h.hora" class="px-5 py-3 flex items-center gap-3">
            <div class="w-14 flex-shrink-0">
              <span class="text-sm font-semibold text-white">{{ h.hora }}</span>
            </div>
            <div class="flex-1 bg-white/5 rounded-full h-2">
              <div class="h-2 rounded-full bg-indigo-500/70"
                :style="{ width: `${(h.clases / maxClases) * 100}%` }" />
            </div>
            <span class="text-xs text-gray-500 w-16 text-right">{{ h.clases }} clases</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Disciplinas más populares -->
    <div class="bg-[#161824] rounded-xl border border-white/10">
      <div class="px-5 py-4 border-b border-white/5">
        <h2 class="font-semibold text-white text-sm">Disciplinas dictadas</h2>
      </div>
      <div class="p-5 flex flex-wrap gap-2">
        <span v-for="d in disciplinas" :key="d.nombre"
          class="flex items-center gap-2 px-3 py-1.5 rounded-full border border-white/10 text-sm text-gray-300">
          <span class="w-2 h-2 rounded-full" :style="{ background: d.color }"></span>
          {{ d.nombre }}
          <span class="text-gray-600 font-medium">{{ d.clases }}</span>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const kpis = [
  { label: 'Clases este mes', value: '48', delta: 'vs mes anterior', deltaPositive: true, icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z', iconBg: 'bg-indigo-500/20', iconColor: 'text-indigo-400' },
  { label: 'Clases realizadas', value: '44', delta: '92% tasa de realización', deltaPositive: true, icon: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z', iconBg: 'bg-emerald-500/20', iconColor: 'text-emerald-400' },
  { label: 'Ocupación promedio', value: '73%', icon: 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z', iconBg: 'bg-purple-500/20', iconColor: 'text-purple-400' },
  { label: 'Ingresos estimados', value: '$1.1M', delta: 'mes actual', deltaPositive: true, icon: 'M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z', iconBg: 'bg-amber-500/20', iconColor: 'text-amber-400' },
]

const ocupacion = [
  { name: 'Sala Principal', pct: 87 },
  { name: 'Sala Pequeña', pct: 64 },
  { name: 'Sala Multiuso', pct: 52 },
  { name: 'Sala Exterior', pct: 31 },
]

const horarios = [
  { hora: '09:00', clases: 12 },
  { hora: '11:00', clases: 18 },
  { hora: '15:00', clases: 22 },
  { hora: '17:00', clases: 20 },
  { hora: '19:00', clases: 16 },
  { hora: '21:00', clases: 8 },
]

const maxClases = computed(() => Math.max(...horarios.map(h => h.clases)))

const disciplinas = [
  { nombre: 'Danza', clases: 18, color: '#a855f7' },
  { nombre: 'Yoga', clases: 10, color: '#6366f1' },
  { nombre: 'Teatro', clases: 8, color: '#ec4899' },
  { nombre: 'Música', clases: 7, color: '#f59e0b' },
  { nombre: 'Acrobacia', clases: 5, color: '#10b981' },
]
</script>
