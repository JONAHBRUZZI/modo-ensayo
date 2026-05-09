<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-white">Mis Métricas</h1>
      <p class="text-gray-400 text-sm mt-0.5">Desempeño y estadísticas de tus clases</p>
    </div>

    <!-- KPIs -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
      <div v-for="k in kpis" :key="k.label" class="bg-[#161824] rounded-xl border border-white/10 p-4">
        <p class="text-xs text-gray-500 mb-1">{{ k.label }}</p>
        <p class="text-2xl font-bold" :class="k.color">{{ k.value }}</p>
        <p v-if="k.sub" class="text-xs text-gray-600 mt-0.5">{{ k.sub }}</p>
      </div>
    </div>

    <!-- Gráfico de asistencia simulado -->
    <div class="bg-[#161824] rounded-xl border border-white/10 p-5">
      <h2 class="font-semibold text-white text-sm mb-4">Asistencia por clase (últimas 8 semanas)</h2>
      <div class="flex items-end gap-2 h-32">
        <div v-for="(bar, i) in asistenciaData" :key="i" class="flex-1 flex flex-col items-center gap-1">
          <div class="w-full bg-indigo-500/20 rounded-t-sm transition-all hover:bg-indigo-500/40"
               :style="`height: ${bar.pct}%`"></div>
          <span class="text-xs text-gray-600">{{ bar.label }}</span>
        </div>
      </div>
    </div>

    <!-- Calificaciones -->
    <div class="bg-[#161824] rounded-xl border border-white/10 p-5">
      <h2 class="font-semibold text-white text-sm mb-4">Calificaciones de alumnos</h2>
      <div class="space-y-3">
        <div v-for="r in ratings" :key="r.stars" class="flex items-center gap-3">
          <span class="text-xs text-gray-400 w-12">{{ r.stars }} ★</span>
          <div class="flex-1 h-2 bg-white/5 rounded-full overflow-hidden">
            <div :style="`width: ${r.pct}%`" class="h-full bg-amber-400 rounded-full"></div>
          </div>
          <span class="text-xs text-gray-500 w-8 text-right">{{ r.pct }}%</span>
        </div>
      </div>
      <p class="text-center mt-4 text-2xl font-bold text-white">4.7 <span class="text-sm text-gray-500 font-normal">/ 5.0</span></p>
    </div>
  </div>
</template>

<script setup>
const kpis = [
  { label: 'Total alumnos', value: '48', color: 'text-white' },
  { label: 'Asistencia promedio', value: '87%', color: 'text-emerald-400', sub: 'Últimas 4 semanas' },
  { label: 'Clases dictadas', value: '24', color: 'text-white' },
  { label: 'Calificación', value: '4.7★', color: 'text-amber-400' },
]

const asistenciaData = [
  { label: 'S1', pct: 72 }, { label: 'S2', pct: 85 }, { label: 'S3', pct: 78 },
  { label: 'S4', pct: 91 }, { label: 'S5', pct: 88 }, { label: 'S6', pct: 95 },
  { label: 'S7', pct: 82 }, { label: 'S8', pct: 89 },
]

const ratings = [
  { stars: 5, pct: 68 }, { stars: 4, pct: 22 }, { stars: 3, pct: 7 },
  { stars: 2, pct: 2 }, { stars: 1, pct: 1 },
]
</script>
