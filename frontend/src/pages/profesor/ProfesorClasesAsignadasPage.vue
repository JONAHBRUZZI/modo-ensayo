<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-white">Clases Asignadas</h1>
      <p class="text-gray-400 text-sm mt-0.5">Clases de sedes donde has sido asignado como instructor</p>
    </div>

    <div v-if="loading" class="space-y-3">
      <div v-for="i in 3" :key="i" class="h-24 bg-[#161824] rounded-xl border border-white/10 animate-pulse"></div>
    </div>

    <div v-else-if="clases.length" class="space-y-3">
      <div v-for="c in clases" :key="c.id"
        class="bg-[#161824] rounded-xl border border-white/10 hover:border-white/20 p-5 transition-all">
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0">
            <h3 class="font-semibold text-white">{{ c.title }}</h3>
            <p class="text-xs text-gray-500 mt-1">{{ formatFecha(c.startTime) }} · {{ c.venueName }}</p>
            <div class="flex items-center gap-2 mt-1">
              <span class="text-xs text-indigo-400">Asignado por sede</span>
            </div>
          </div>
          <EstadoBadge :estado="c.estado || 'ACTIVA'" />
        </div>
        <div class="flex items-center gap-3 mt-3 pt-3 border-t border-white/5">
          <router-link :to="`/profesor/asistencia/${c.id}`" class="text-xs text-purple-400 hover:text-purple-300 transition-colors">Tomar asistencia</router-link>
          <span class="text-gray-700 text-xs">·</span>
          <router-link :to="`/profesor/clases/${c.id}/reagendamiento`" class="text-xs text-amber-400 hover:text-amber-300 transition-colors">Solicitar reagendamiento</router-link>
        </div>
      </div>
    </div>

    <div v-else class="text-center py-16 bg-[#161824] rounded-xl border border-white/10">
      <p class="text-gray-400 text-sm">No tienes clases asignadas por sedes</p>
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
  try { clases.value = await classService.getMyClasses() } catch { /* ignore */ }
  finally { loading.value = false }
})

const formatFecha = (d) => d ? new Date(d).toLocaleDateString('es-CL', { weekday: 'short', day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : '—'
</script>
