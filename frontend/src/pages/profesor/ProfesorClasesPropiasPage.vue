<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-white">Mis Clases</h1>
        <p class="text-gray-400 text-sm mt-0.5">Clases que has creado como profesor</p>
      </div>
      <router-link to="/alumno/crear-clase"
        class="px-4 py-2 bg-purple-600 hover:bg-purple-500 text-white rounded-lg text-sm font-medium transition-colors">
        + Nueva Clase
      </router-link>
    </div>

    <!-- Borradores (si los hay) -->
    <div v-if="!loading && borradores.length" class="space-y-2">
      <div class="flex items-center gap-2 px-1">
        <div class="w-2 h-2 rounded-full bg-amber-400"></div>
        <p class="text-xs font-semibold text-gray-500 uppercase tracking-wider">Borradores sin sede</p>
        <span class="text-xs text-gray-600">({{ borradores.length }})</span>
      </div>
      <div v-for="c in borradores" :key="c.id"
        class="bg-[#161824] rounded-xl border border-amber-500/20 p-5 flex items-start gap-4">
        <div class="flex-shrink-0 w-12 flex flex-col items-center justify-center">
          <div class="w-9 h-9 rounded-lg bg-amber-500/10 flex items-center justify-center">
            <svg class="w-4.5 h-4.5 text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
          </div>
        </div>
        <div class="flex-1 min-w-0">
          <div class="flex items-start justify-between gap-2">
            <div>
              <h3 class="font-semibold text-white">{{ c.title }}</h3>
              <p class="text-xs text-gray-500 mt-1">{{ c.discipline }} · {{ c.capacity }} cupos · ${{ c.price?.toLocaleString('es-CL') }}</p>
            </div>
            <span class="flex-shrink-0 text-xs px-2 py-0.5 rounded-full bg-amber-500/10 border border-amber-500/20 text-amber-400 font-medium">
              Borrador
            </span>
          </div>
          <div class="flex items-center gap-3 mt-3">
            <router-link to="/alumno/crear-clase"
              class="text-xs text-amber-400 hover:text-amber-300 transition-colors font-medium">
              Asignar sede →
            </router-link>
          </div>
        </div>
      </div>
    </div>

    <!-- Clases publicadas -->
    <div v-if="!loading && publicadas.length" class="space-y-2">
      <div v-if="borradores.length" class="flex items-center gap-2 px-1 pt-2">
        <div class="w-2 h-2 rounded-full bg-purple-400"></div>
        <p class="text-xs font-semibold text-gray-500 uppercase tracking-wider">Publicadas</p>
      </div>

      <div v-if="loading" class="space-y-3">
        <div v-for="i in 4" :key="i" class="h-24 bg-[#161824] rounded-xl border border-white/10 animate-pulse"></div>
      </div>

      <div v-for="c in publicadas" :key="c.id"
        class="bg-[#161824] rounded-xl border border-white/10 hover:border-white/20 p-5 flex items-start gap-4 transition-all">
        <div class="flex-shrink-0 text-center w-12">
          <p class="text-xs font-bold text-purple-400 uppercase">{{ fmt(c.startTime, 'ddd') }}</p>
          <p class="text-2xl font-bold text-white leading-none">{{ fmt(c.startTime, 'D') }}</p>
        </div>
        <div class="flex-1 min-w-0">
          <div class="flex items-start justify-between gap-2">
            <div>
              <h3 class="font-semibold text-white">{{ c.title }}</h3>
              <p class="text-xs text-gray-500 mt-1">{{ fmt(c.startTime, 'HH:mm') }} · {{ c.venueName }} · {{ c.capacity }} cupos</p>
            </div>
            <EstadoBadge :estado="c.status || 'ACTIVA'" />
          </div>
          <div class="flex items-center gap-3 mt-3">
            <router-link :to="`/profesor/asistencia/${c.id}`" class="text-xs text-purple-400 hover:text-purple-300 transition-colors">Tomar asistencia</router-link>
            <span class="text-gray-700 text-xs">·</span>
            <router-link :to="`/profesor/clases/${c.id}/reagendamiento`" class="text-xs text-amber-400 hover:text-amber-300 transition-colors">Reagendar</router-link>
          </div>
        </div>
      </div>
    </div>

    <!-- Skeleton carga -->
    <div v-if="loading" class="space-y-3">
      <div v-for="i in 4" :key="i" class="h-24 bg-[#161824] rounded-xl border border-white/10 animate-pulse"></div>
    </div>

    <!-- Vacío -->
    <div v-if="!loading && !clases.length" class="text-center py-16 bg-[#161824] rounded-xl border border-white/10">
      <p class="text-gray-400 text-sm">No tienes clases creadas aún</p>
      <router-link to="/alumno/crear-clase" class="inline-block mt-3 text-sm text-purple-400 hover:text-purple-300 transition-colors">Crear primera clase</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { classService } from '../../services/classService'
import EstadoBadge from '../../components/EstadoBadge.vue'

const loading = ref(true)
const clases = ref([])

const borradores = computed(() => clases.value.filter(c => c.status === 'DRAFT'))
const publicadas = computed(() => clases.value.filter(c => c.status !== 'DRAFT'))

onMounted(async () => {
  try { clases.value = await classService.getMyClasses() } catch { /* ignore */ }
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
