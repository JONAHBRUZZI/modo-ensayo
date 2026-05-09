<template>
  <div class="space-y-6">

    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-white">Cobros de Clases</h1>
        <p class="text-gray-400 text-sm mt-0.5">Historial de ingresos por tus clases</p>
      </div>
    </div>

    <!-- Stats resumen -->
    <div class="grid grid-cols-2 lg:grid-cols-3 gap-4">
      <div v-for="s in stats" :key="s.label" class="bg-[#161824] rounded-xl border border-white/10 p-4">
        <div :class="s.iconBg" class="w-9 h-9 rounded-lg flex items-center justify-center mb-3">
          <svg :class="s.iconColor" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="s.icon" />
          </svg>
        </div>
        <p class="text-2xl font-bold text-white">{{ s.value }}</p>
        <p class="text-xs text-gray-500 mt-0.5">{{ s.label }}</p>
      </div>
    </div>

    <!-- Tabla de cobros -->
    <div class="bg-[#161824] rounded-xl border border-white/10">
      <div class="px-5 py-4 border-b border-white/5">
        <div class="flex items-center gap-2">
          <div class="w-2 h-2 rounded-full bg-purple-400"></div>
          <h2 class="font-semibold text-white text-sm">Historial de Cobros</h2>
        </div>
      </div>

      <div class="px-5 py-12 text-center">
        <div class="inline-flex items-center justify-center w-12 h-12 bg-purple-500/10 rounded-xl mb-4">
          <svg class="w-6 h-6 text-purple-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
              d="M9 14l6-6m-5.5.5h.01m4.99 5h.01M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16l3.5-2 3.5 2 3.5-2 3.5 2z" />
          </svg>
        </div>
        <p class="text-white font-medium text-sm">Módulo de cobros en desarrollo</p>
        <p class="text-gray-500 text-xs mt-1 max-w-xs mx-auto">
          Aquí verás el historial de pagos recibidos por tus clases, liquidaciones y estado de transferencias.
        </p>
      </div>
    </div>

  </div>
</template>

<script setup>
const stats = [
  {
    label: 'Ingresos del mes', value: '$0',
    icon: 'M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z',
    iconBg: 'bg-emerald-500/20', iconColor: 'text-emerald-400',
  },
  {
    label: 'Clases cobradas', value: '0',
    icon: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2',
    iconBg: 'bg-purple-500/20', iconColor: 'text-purple-400',
  },
  {
    label: 'Pendientes', value: '0',
    icon: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z',
    iconBg: 'bg-amber-500/20', iconColor: 'text-amber-400',
  },
]
</script>
