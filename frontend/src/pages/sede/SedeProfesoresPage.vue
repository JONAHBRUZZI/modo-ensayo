<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-white">Profesores Asociados</h1>
        <p class="text-gray-400 text-sm mt-0.5">Instructores que dictan clases en tu sede</p>
      </div>
    </div>

    <!-- Skeleton -->
    <div v-if="loading" class="space-y-3">
      <div v-for="i in 4" :key="i" class="h-20 bg-[#161824] rounded-xl border border-white/10 animate-pulse"></div>
    </div>

    <!-- Lista -->
    <div v-else-if="profesores.length" class="space-y-3">
      <div v-for="p in profesores" :key="p.id"
        class="bg-[#161824] rounded-xl border border-white/10 hover:border-white/20 p-5 flex items-center gap-4 transition-all">
        <!-- Avatar -->
        <div class="w-10 h-10 rounded-full bg-gradient-to-br from-purple-600 to-indigo-700 flex items-center justify-center flex-shrink-0">
          <span class="text-sm font-bold text-white">{{ iniciales(p.fullName) }}</span>
        </div>

        <!-- Info -->
        <div class="flex-1 min-w-0">
          <h3 class="text-sm font-semibold text-white">{{ p.socialName || p.fullName }}</h3>
          <p class="text-xs text-gray-500 mt-0.5">{{ p.email }}</p>
        </div>

        <!-- Stats -->
        <div class="hidden sm:flex items-center gap-6 text-center flex-shrink-0">
          <div>
            <p class="text-lg font-bold text-white">{{ p.clasesActivas ?? '—' }}</p>
            <p class="text-xs text-gray-600">clases activas</p>
          </div>
          <div>
            <p class="text-lg font-bold text-white">{{ p.totalClases ?? '—' }}</p>
            <p class="text-xs text-gray-600">total dictadas</p>
          </div>
        </div>

        <!-- Estado -->
        <span class="flex-shrink-0 text-xs px-2.5 py-1 rounded-full font-medium"
          :class="p.activo !== false
            ? 'bg-emerald-500/10 border border-emerald-500/20 text-emerald-400'
            : 'bg-white/5 border border-white/10 text-gray-500'">
          {{ p.activo !== false ? 'Activo' : 'Inactivo' }}
        </span>
      </div>
    </div>

    <!-- Vacío -->
    <div v-else class="text-center py-16 bg-[#161824] rounded-xl border border-white/10">
      <div class="w-14 h-14 rounded-full bg-purple-500/10 flex items-center justify-center mx-auto mb-3">
        <svg class="w-7 h-7 text-purple-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
      </div>
      <p class="text-white font-medium text-sm">Sin profesores asociados</p>
      <p class="text-gray-500 text-sm mt-1">Los profesores aparecerán aquí cuando tengan clases asignadas en tu sede</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const loading = ref(true)
const profesores = ref([])

onMounted(async () => {
  // TODO: conectar con endpoint GET /venue-admin/profesores cuando esté disponible
  await new Promise(r => setTimeout(r, 600))
  profesores.value = []
  loading.value = false
})

const iniciales = (nombre) => {
  if (!nombre) return '?'
  const partes = nombre.trim().split(' ')
  return (partes[0][0] + (partes[partes.length - 1]?.[0] || '')).toUpperCase()
}
</script>
