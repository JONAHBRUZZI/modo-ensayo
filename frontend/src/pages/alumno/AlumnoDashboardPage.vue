<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-white">Hola, {{ displayName }} 👋</h1>
        <p class="text-gray-400 text-sm mt-0.5">Aquí está tu resumen artístico</p>
      </div>
      <router-link to="/classes"
                   class="hidden sm:flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-sm font-medium transition-colors">
        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        Buscar Clases
      </router-link>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
      <div v-for="stat in stats" :key="stat.label" class="bg-[#161824] rounded-xl border border-white/10 p-4">
        <div :class="stat.iconBg" class="w-9 h-9 rounded-lg flex items-center justify-center mb-3">
          <svg :class="stat.iconColor" class="w-4.5 h-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="stat.icon" />
          </svg>
        </div>
        <p class="text-2xl font-bold text-white">{{ loading ? '—' : stat.value }}</p>
        <p class="text-xs text-gray-500 mt-0.5">{{ stat.label }}</p>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Próximas clases -->
      <div class="lg:col-span-2 bg-[#161824] rounded-xl border border-white/10">
        <div class="px-5 py-4 border-b border-white/5 flex items-center justify-between">
          <h2 class="font-semibold text-white text-sm">Próximas Clases</h2>
          <router-link to="/alumno/mis-clases" class="text-xs text-indigo-400 hover:text-indigo-300 transition-colors">Ver todas</router-link>
        </div>
        <div v-if="loading" class="p-5 space-y-3">
          <div v-for="i in 3" :key="i" class="h-16 bg-white/5 rounded-lg animate-pulse"></div>
        </div>
        <div v-else-if="proximasClases.length > 0" class="divide-y divide-white/5">
          <div v-for="c in proximasClases" :key="c.id" class="px-5 py-3.5 flex items-center gap-4 hover:bg-white/3 transition-colors">
            <div class="flex-shrink-0 text-center w-10">
              <p class="text-xs font-bold text-indigo-400 uppercase">{{ formatDia(c.startTime) }}</p>
              <p class="text-xl font-bold text-white leading-none">{{ formatNumDia(c.startTime) }}</p>
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium text-white truncate">{{ c.title }}</p>
              <p class="text-xs text-gray-500">{{ formatHora(c.startTime) }} · {{ c.venueName }}</p>
            </div>
            <EstadoBadge :estado="c.estado || 'PROXIMA'" />
          </div>
        </div>
        <div v-else class="px-5 py-10 text-center">
          <p class="text-gray-500 text-sm">No tienes clases próximas</p>
          <router-link to="/classes" class="text-indigo-400 text-sm hover:underline mt-1 inline-block">Explorar clases</router-link>
        </div>
      </div>

      <!-- Panel lateral -->
      <div class="space-y-4">
        <!-- Estado de identidad -->
        <div class="bg-[#161824] rounded-xl border p-4"
             :class="identidadValidada ? 'border-emerald-500/20' : 'border-amber-500/20'">
          <div class="flex items-center gap-3">
            <div :class="identidadValidada ? 'bg-emerald-500/20' : 'bg-amber-500/20'" class="w-9 h-9 rounded-lg flex items-center justify-center">
              <svg :class="identidadValidada ? 'text-emerald-400' : 'text-amber-400'" class="w-4.5 h-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V8a2 2 0 00-2-2h-5m-4 0V5a2 2 0 114 0v1m-4 0a2 2 0 104 0m-5 8a2 2 0 100-4 2 2 0 000 4zm0 0c1.306 0 2.417.835 2.83 2M9 14a3.001 3.001 0 00-2.83 2M15 11h3m-3 4h2" />
              </svg>
            </div>
            <div class="flex-1">
              <p class="text-sm font-medium text-white">{{ identidadValidada ? 'Identidad verificada' : 'Sin verificar' }}</p>
              <p class="text-xs text-gray-500">{{ identidadValidada ? 'Acceso completo' : 'Sube tu documento' }}</p>
            </div>
          </div>
          <router-link v-if="!identidadValidada" to="/profile/identity"
                       class="mt-3 w-full flex justify-center py-2 bg-amber-500/15 hover:bg-amber-500/25 border border-amber-500/30 text-amber-400 rounded-lg text-xs font-medium transition-colors">
            Verificar ahora
          </router-link>
        </div>

        <!-- Accesos rápidos -->
        <div class="bg-[#161824] rounded-xl border border-white/10 p-4">
          <h3 class="text-sm font-semibold text-white mb-3">Accesos rápidos</h3>
          <div class="space-y-1">
            <router-link v-for="link in quickLinks" :key="link.to" :to="link.to"
                         class="flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm text-gray-400 hover:text-white hover:bg-white/5 transition-colors group">
              <svg class="w-4 h-4 flex-shrink-0 text-gray-600 group-hover:text-indigo-400 transition-colors" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="link.icon" />
              </svg>
              {{ link.label }}
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuth } from '../../hooks/useAuth'
import { classService } from '../../services/classService'
import EstadoBadge from '../../components/EstadoBadge.vue'

const { user, identidadValidada, displayName } = useAuth()
const loading = ref(true)
const proximasClases = ref([])
const misClases = ref([])

const stats = computed(() => [
  { label: 'Clases inscritas', value: misClases.value.length, icon: 'M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253', iconBg: 'bg-indigo-500/20', iconColor: 'text-indigo-400' },
  { label: 'Esta semana', value: proximasClases.value.length, icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z', iconBg: 'bg-purple-500/20', iconColor: 'text-purple-400' },
  { label: 'Disciplinas', value: new Set(misClases.value.map(c => c.discipline)).size, icon: 'M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01', iconBg: 'bg-cyan-500/20', iconColor: 'text-cyan-400' },
  { label: 'Familiares', value: 0, icon: 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z', iconBg: 'bg-pink-500/20', iconColor: 'text-pink-400' },
])

const quickLinks = [
  { to: '/classes', label: 'Explorar clases', icon: 'M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z' },
  { to: '/alumno/mis-clases', label: 'Mis clases', icon: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2' },
  { to: '/alumno/pagos', label: 'Historial de pagos', icon: 'M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z' },
  { to: '/alumno/asociados', label: 'Mis familiares', icon: 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z' },
  { to: '/cart', label: 'Mi carrito', icon: 'M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z' },
]

onMounted(async () => {
  try {
    misClases.value = await classService.getMyClasses()
    const ahora = Date.now()
    const semana = ahora + 7 * 24 * 3600 * 1000
    proximasClases.value = misClases.value
      .filter(c => c.startTime && new Date(c.startTime).getTime() > ahora && new Date(c.startTime).getTime() < semana)
      .slice(0, 5)
  } catch { /* ignore */ }
  finally { loading.value = false }
})

const formatDia = (d) => new Date(d).toLocaleDateString('es-CL', { weekday: 'short' }).toUpperCase()
const formatNumDia = (d) => new Date(d).getDate()
const formatHora = (d) => new Date(d).toLocaleTimeString('es-CL', { hour: '2-digit', minute: '2-digit' })
</script>
