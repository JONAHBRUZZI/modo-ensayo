<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-white">Clases de mi Sede</h1>
        <p class="text-gray-400 text-sm mt-0.5">Todas las clases programadas en tus instalaciones</p>
      </div>
      <router-link to="/sede/crear-clase"
        class="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-sm font-medium transition-colors">
        + Nueva Clase
      </router-link>
    </div>

    <div v-if="loading" class="space-y-3">
      <div v-for="i in 4" :key="i" class="h-20 bg-[#161824] rounded-xl border border-white/10 animate-pulse"></div>
    </div>

    <div v-else-if="clases.length" class="space-y-3">
      <div v-for="c in clases" :key="c.id"
        class="bg-[#161824] rounded-xl border border-white/10 hover:border-white/20 p-4 flex items-center gap-4 transition-all">
        <div class="flex-shrink-0 w-12 text-center">
          <p class="text-xs font-bold text-indigo-400 uppercase">{{ fmt(c.startTime, 'ddd') }}</p>
          <p class="text-xl font-bold text-white leading-none">{{ fmt(c.startTime, 'D') }}</p>
        </div>
        <div class="flex-1 min-w-0">
          <h3 class="text-sm font-semibold text-white">{{ c.title }}</h3>
          <p class="text-xs text-gray-500 mt-0.5">{{ fmt(c.startTime, 'HH:mm') }} · {{ c.capacity }} cupos · {{ c.discipline }}</p>
        </div>
        <div class="flex items-center gap-2 flex-shrink-0">
          <EstadoBadge :estado="c.estado || 'ACTIVA'" />
          <router-link :to="`/sede/clases/${c.id}/reagendamiento`"
            class="text-xs text-amber-400 hover:text-amber-300 transition-colors hidden sm:block">
            Reagendar
          </router-link>
        </div>
      </div>
    </div>

    <div v-else class="text-center py-16 bg-[#161824] rounded-xl border border-white/10">
      <p class="text-gray-400 text-sm">No hay clases programadas</p>
      <router-link to="/sede/crear-clase" class="inline-block mt-3 text-sm text-indigo-400 hover:text-indigo-300 transition-colors">
        Crear primera clase
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { classService } from '../../services/classService'
import EstadoBadge from '../../components/EstadoBadge.vue'

const loading = ref(true)
const clases = ref([])

onMounted(async () => {
  try { clases.value = await classService.listPublished() } catch { /* ignore */ }
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
