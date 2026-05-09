<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-white">Mis Clases</h1>
        <p class="text-gray-400 text-sm mt-0.5">Tus inscripciones activas</p>
      </div>
      <router-link to="/classes"
        class="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-sm font-medium transition-colors">
        + Inscribirme
      </router-link>
    </div>

    <!-- Tabs -->
    <div class="flex gap-1 bg-[#161824] rounded-xl border border-white/10 p-1 w-fit">
      <button v-for="tab in tabs" :key="tab.key" @click="activeTab = tab.key"
        :class="activeTab === tab.key ? 'bg-indigo-600 text-white' : 'text-gray-400 hover:text-white'"
        class="px-4 py-1.5 rounded-lg text-sm font-medium transition-colors">
        {{ tab.label }}
        <span v-if="tab.count" class="ml-1.5 text-xs opacity-70">({{ tab.count }})</span>
      </button>
    </div>

    <!-- Skeleton -->
    <div v-if="loading" class="space-y-3">
      <div v-for="i in 4" :key="i" class="h-24 bg-[#161824] rounded-xl border border-white/10 animate-pulse"></div>
    </div>

    <!-- Lista -->
    <div v-else-if="filteredClases.length" class="space-y-3">
      <div v-for="c in filteredClases" :key="c.id"
        class="bg-[#161824] rounded-xl border border-white/10 hover:border-white/20 p-5 flex items-start gap-4 transition-all">
        <!-- Fecha -->
        <div class="flex-shrink-0 w-12 text-center">
          <p class="text-xs font-bold text-indigo-400 uppercase">{{ formatDia(c.startTime) }}</p>
          <p class="text-2xl font-bold text-white leading-none">{{ formatNumDia(c.startTime) }}</p>
          <p class="text-xs text-gray-500">{{ formatMes(c.startTime) }}</p>
        </div>

        <div class="flex-1 min-w-0">
          <div class="flex items-start justify-between gap-2">
            <div class="min-w-0">
              <h3 class="font-semibold text-white truncate">{{ c.title }}</h3>
              <div class="flex items-center gap-3 text-xs text-gray-500 mt-1">
                <span>{{ formatHora(c.startTime) }}</span>
                <span class="text-gray-700">·</span>
                <span>{{ c.venueName || 'Sede por confirmar' }}</span>
                <span v-if="c.beneficiaryName" class="text-gray-700">·</span>
                <span v-if="c.beneficiaryName" class="text-indigo-400">{{ c.beneficiaryName }}</span>
              </div>
            </div>
            <EstadoBadge :estado="c.estado || 'ACTIVA'" />
          </div>

          <!-- Acciones -->
          <div class="flex items-center gap-2 mt-3">
            <router-link :to="`/alumno/clases/${c.id}`"
              class="text-xs text-indigo-400 hover:text-indigo-300 transition-colors">
              Ver detalle
            </router-link>
            <span class="text-gray-700 text-xs">·</span>
            <button v-if="puedeReagendar(c)" @click="solicitarReagendamiento(c)"
              class="text-xs text-amber-400 hover:text-amber-300 transition-colors">
              Reagendar
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty -->
    <div v-else class="text-center py-16 bg-[#161824] rounded-xl border border-white/10">
      <div class="inline-flex items-center justify-center w-14 h-14 bg-white/5 rounded-full border border-white/10 mb-4">
        <svg class="w-7 h-7 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
        </svg>
      </div>
      <p class="text-gray-400 text-sm">No tienes clases en esta categoría</p>
      <router-link to="/classes" class="inline-flex items-center gap-1.5 mt-3 text-sm text-indigo-400 hover:text-indigo-300 transition-colors">
        Explorar clases disponibles
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { classService } from '../../services/classService'
import EstadoBadge from '../../components/EstadoBadge.vue'

const router = useRouter()
const loading = ref(true)
const clases = ref([])
const activeTab = ref('proximas')

const tabs = computed(() => [
  { key: 'proximas', label: 'Próximas', count: clases.value.filter(c => esFutura(c)).length },
  { key: 'pasadas', label: 'Pasadas', count: clases.value.filter(c => !esFutura(c)).length },
  { key: 'todas', label: 'Todas', count: clases.value.length },
])

const filteredClases = computed(() => {
  if (activeTab.value === 'proximas') return clases.value.filter(c => esFutura(c))
  if (activeTab.value === 'pasadas') return clases.value.filter(c => !esFutura(c))
  return clases.value
})

const esFutura = (c) => c.startTime && new Date(c.startTime) > new Date()
const puedeReagendar = (c) => {
  if (!c.startTime) return false
  const diff = new Date(c.startTime) - Date.now()
  return diff > 0 && diff < 72 * 3600 * 1000
}

onMounted(async () => {
  try { clases.value = await classService.getMyClasses() } catch { /* ignore */ }
  finally { loading.value = false }
})

const solicitarReagendamiento = (c) => {
  router.push(`/alumno/clases/${c.id}?reagendar=1`)
}

const formatDia = (d) => d ? new Date(d).toLocaleDateString('es-CL', { weekday: 'short' }).toUpperCase() : '—'
const formatNumDia = (d) => d ? new Date(d).getDate() : '—'
const formatMes = (d) => d ? new Date(d).toLocaleDateString('es-CL', { month: 'short' }) : '—'
const formatHora = (d) => d ? new Date(d).toLocaleTimeString('es-CL', { hour: '2-digit', minute: '2-digit' }) : '—'
</script>
