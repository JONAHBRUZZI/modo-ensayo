<template>
  <div class="space-y-6">

    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-white">Hola, {{ displayName }} 🎓</h1>
        <p class="text-gray-400 text-sm mt-0.5">Panel de gestión docente</p>
      </div>
      <router-link to="/profesor/clases-propias"
        class="hidden sm:flex items-center gap-2 px-4 py-2 bg-purple-600 hover:bg-purple-500 text-white rounded-lg text-sm font-medium transition-colors">
        + Nueva Clase
      </router-link>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
      <div v-for="s in stats" :key="s.label" class="bg-[#161824] rounded-xl border border-white/10 p-4">
        <div :class="s.iconBg" class="w-9 h-9 rounded-lg flex items-center justify-center mb-3">
          <svg :class="s.iconColor" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="s.icon" />
          </svg>
        </div>
        <p class="text-2xl font-bold text-white">{{ loading ? '—' : s.value }}</p>
        <p class="text-xs text-gray-500 mt-0.5">{{ s.label }}</p>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">

      <div class="lg:col-span-2 space-y-5">

        <!-- Mis clases propias -->
        <div class="bg-[#161824] rounded-xl border border-white/10">
          <div class="px-5 py-4 border-b border-white/5 flex items-center justify-between">
            <div class="flex items-center gap-2">
              <div class="w-2 h-2 rounded-full bg-purple-400"></div>
              <h2 class="font-semibold text-white text-sm">Mis Clases Propias</h2>
            </div>
            <router-link to="/profesor/clases-propias" class="text-xs text-purple-400 hover:text-purple-300 transition-colors">Ver todas</router-link>
          </div>

          <div v-if="loading" class="p-5 space-y-3">
            <div v-for="i in 3" :key="i" class="h-14 bg-white/5 rounded-lg animate-pulse"></div>
          </div>
          <div v-else-if="clasesPropias.length" class="divide-y divide-white/5">
            <div v-for="c in clasesPropias" :key="c.id"
              class="px-5 py-3.5 flex items-center gap-4 hover:bg-white/3 transition-colors">
              <div class="flex-shrink-0 text-center w-10">
                <p class="text-xs font-bold text-purple-400 uppercase">{{ fmt(c.startTime, 'ddd') }}</p>
                <p class="text-xl font-bold text-white leading-none">{{ fmt(c.startTime, 'D') }}</p>
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium text-white truncate">{{ c.title }}</p>
                <p class="text-xs text-gray-500">{{ fmt(c.startTime, 'HH:mm') }} · {{ c.venueName }}</p>
              </div>
              <router-link :to="`/profesor/asistencia/${c.id}`"
                class="text-xs text-purple-400 hover:text-purple-300 transition-colors flex-shrink-0">
                Asistencia
              </router-link>
            </div>
          </div>
          <div v-else class="px-5 py-8 text-center">
            <p class="text-gray-500 text-sm">No tienes clases propias programadas</p>
            <router-link to="/profesor/clases-propias"
              class="text-purple-400 text-xs hover:underline mt-1 inline-block">
              Crear una clase
            </router-link>
          </div>
        </div>

        <!-- Clases asignadas por sede -->
        <div class="bg-[#161824] rounded-xl border border-white/10">
          <div class="px-5 py-4 border-b border-white/5 flex items-center justify-between">
            <div class="flex items-center gap-2">
              <div class="w-2 h-2 rounded-full bg-indigo-400"></div>
              <h2 class="font-semibold text-white text-sm">Clases Asignadas por Sede</h2>
            </div>
            <router-link v-if="tieneAsignacionesActivas" to="/profesor/clases-asignadas"
              class="text-xs text-indigo-400 hover:text-indigo-300 transition-colors">
              Ver todas
            </router-link>
          </div>

          <div v-if="loading" class="p-5 space-y-3">
            <div v-for="i in 2" :key="i" class="h-14 bg-white/5 rounded-lg animate-pulse"></div>
          </div>
          <div v-else-if="!tieneAsignacionesActivas" class="px-5 py-8 text-center">
            <div class="inline-flex items-center justify-center w-10 h-10 bg-white/5 rounded-full mb-3">
              <svg class="w-5 h-5 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1" />
              </svg>
            </div>
            <p class="text-gray-500 text-sm">Nada asignado</p>
            <p class="text-gray-600 text-xs mt-1">Ninguna sede te ha asignado clases aún</p>
          </div>
          <div v-else-if="clasesAsignadas.length" class="divide-y divide-white/5">
            <div v-for="c in clasesAsignadas" :key="c.id"
              class="px-5 py-3.5 flex items-center gap-4 hover:bg-white/3 transition-colors">
              <div class="flex-shrink-0 text-center w-10">
                <p class="text-xs font-bold text-indigo-400 uppercase">{{ fmt(c.startTime, 'ddd') }}</p>
                <p class="text-xl font-bold text-white leading-none">{{ fmt(c.startTime, 'D') }}</p>
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium text-white truncate">{{ c.title }}</p>
                <p class="text-xs text-gray-500">{{ fmt(c.startTime, 'HH:mm') }} · {{ c.venueName }}</p>
              </div>
              <span class="text-[10px] bg-indigo-500/15 text-indigo-400 border border-indigo-500/25 px-2 py-0.5 rounded-full flex-shrink-0">
                Sede
              </span>
            </div>
          </div>
          <div v-else class="px-5 py-8 text-center">
            <p class="text-gray-500 text-sm">Nada asignado</p>
            <p class="text-gray-600 text-xs mt-1">Ninguna sede te ha asignado clases aún</p>
          </div>
        </div>

      </div>

      <!-- Panel lateral -->
      <div class="space-y-3">
        <router-link v-for="link in quickLinks" :key="link.to" :to="link.to"
          class="flex items-center gap-3 p-4 bg-[#161824] border border-white/10 hover:border-white/20 rounded-xl transition-all group">
          <div :class="link.iconBg" class="w-9 h-9 rounded-lg flex items-center justify-center flex-shrink-0">
            <svg :class="link.iconColor" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="link.icon" />
            </svg>
          </div>
          <div class="min-w-0">
            <p class="text-sm font-medium text-white group-hover:text-purple-300 transition-colors">{{ link.label }}</p>
            <p class="text-xs text-gray-500">{{ link.desc }}</p>
          </div>
        </router-link>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuth } from '../../hooks/useAuth'
import { classService } from '../../services/classService'

const { user, tieneAsignacionesActivas, displayName } = useAuth()
const loading = ref(true)
const todasLasClases = ref([])
const clasesAsignadas = ref([])

const ahora = new Date()
const clasesPropias = computed(() =>
  todasLasClases.value
    .filter(c => c.startTime && new Date(c.startTime) > ahora)
    .slice(0, 5)
)

const stats = computed(() => [
  {
    label: 'Clases propias', value: todasLasClases.value.length,
    icon: 'M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253',
    iconBg: 'bg-purple-500/20', iconColor: 'text-purple-400',
  },
  {
    label: 'Asignadas', value: tieneAsignacionesActivas.value ? clasesAsignadas.value.length : '—',
    icon: 'M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1',
    iconBg: 'bg-indigo-500/20', iconColor: 'text-indigo-400',
  },
  {
    label: 'Total alumnos', value: todasLasClases.value.reduce((s, c) => s + (c.enrolled || 0), 0),
    icon: 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z',
    iconBg: 'bg-cyan-500/20', iconColor: 'text-cyan-400',
  },
  {
    label: 'Asistencia prom.', value: '87%',
    icon: 'M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z',
    iconBg: 'bg-emerald-500/20', iconColor: 'text-emerald-400',
  },
])

const quickLinks = [
  { to: '/profesor/clases-propias', label: 'Mis Clases', desc: 'Clases que gestiono', icon: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2', iconBg: 'bg-purple-500/20', iconColor: 'text-purple-400' },
  { to: '/profesor/clases-asignadas', label: 'Asignaciones Sede', desc: 'Clases de terceros', icon: 'M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1', iconBg: 'bg-indigo-500/20', iconColor: 'text-indigo-400' },
  { to: '/profesor/metricas', label: 'Mis Métricas', desc: 'Estadísticas y rendimiento', icon: 'M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z', iconBg: 'bg-emerald-500/20', iconColor: 'text-emerald-400' },
  { to: '/profesor/buscar-salas', label: 'Buscar Salas', desc: 'Reservar espacio para clase', icon: 'M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z', iconBg: 'bg-cyan-500/20', iconColor: 'text-cyan-400' },
]

onMounted(async () => {
  try {
    todasLasClases.value = await classService.getMyClasses()
    if (tieneAsignacionesActivas.value) {
      try { clasesAsignadas.value = await classService.getAssignedClasses() } catch { /* ignore */ }
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
})

const fmt = (d, type) => {
  if (!d) return '—'
  const date = new Date(d)
  if (type === 'ddd') return date.toLocaleDateString('es-CL', { weekday: 'short' }).toUpperCase()
  if (type === 'D') return date.getDate()
  if (type === 'HH:mm') return date.toLocaleTimeString('es-CL', { hour: '2-digit', minute: '2-digit' })
  return d
}
</script>
