<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-white">Clases por Confirmar</h1>
      <p class="text-gray-400 text-sm mt-0.5">Valida si las clases se realizaron o no para liberar pagos</p>
    </div>

    <!-- Info importante -->
    <div class="flex items-start gap-3 p-4 bg-emerald-500/8 border border-emerald-500/20 rounded-xl">
      <svg class="w-5 h-5 text-emerald-400 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      <div>
        <p class="text-sm font-medium text-emerald-300">Este módulo libera los pagos</p>
        <p class="text-xs text-emerald-400/70 mt-0.5">Al marcar una clase como realizada, los fondos se transfieren al profesor. Al marcarla como no realizada, se activa el proceso de devolución al alumno.</p>
      </div>
    </div>

    <!-- Skeleton -->
    <div v-if="loading" class="space-y-3">
      <div v-for="i in 3" :key="i" class="h-28 bg-[#161824] rounded-xl border border-white/10 animate-pulse"></div>
    </div>

    <!-- Lista -->
    <div v-else-if="clases.length" class="space-y-3">
      <div v-for="c in clases" :key="c.id"
        class="bg-[#161824] rounded-xl border border-white/10 p-5 transition-all"
        :class="{ 'opacity-60': procesando === c.id }">

        <div class="flex items-start gap-4">
          <!-- Fecha -->
          <div class="flex-shrink-0 text-center w-12">
            <p class="text-xs font-bold text-emerald-400 uppercase">{{ fmt(c.startTime, 'ddd') }}</p>
            <p class="text-2xl font-bold text-white leading-none">{{ fmt(c.startTime, 'D') }}</p>
          </div>

          <!-- Info clase -->
          <div class="flex-1 min-w-0">
            <div class="flex items-start justify-between gap-2 mb-1">
              <h3 class="font-semibold text-white">{{ c.title }}</h3>
              <span class="flex-shrink-0 text-xs px-2 py-0.5 rounded-full bg-amber-500/10 border border-amber-500/20 text-amber-400">
                Por confirmar
              </span>
            </div>
            <p class="text-xs text-gray-500">
              {{ fmt(c.startTime, 'HH:mm') }} · {{ c.venueName || 'Sala asignada' }} · {{ c.capacity }} cupos
            </p>
            <p v-if="c.discipline" class="text-xs text-gray-600 mt-0.5">{{ c.discipline }}</p>
          </div>
        </div>

        <!-- Acciones -->
        <div class="flex items-center gap-3 mt-4 pt-4 border-t border-white/5">
          <button
            @click="confirmarRealizada(c.id)"
            :disabled="!!procesando"
            class="flex-1 flex items-center justify-center gap-2 py-2.5 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg text-sm font-medium transition-colors">
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
            {{ procesando === c.id ? 'Confirmando...' : 'Sí se realizó' }}
          </button>
          <button
            @click="confirmarNoRealizada(c.id)"
            :disabled="!!procesando"
            class="flex-1 flex items-center justify-center gap-2 py-2.5 bg-red-600/20 hover:bg-red-600/30 border border-red-500/30 disabled:opacity-50 disabled:cursor-not-allowed text-red-400 rounded-lg text-sm font-medium transition-colors">
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
            No se realizó
          </button>
        </div>
      </div>
    </div>

    <!-- Vacío -->
    <div v-else class="text-center py-16 bg-[#161824] rounded-xl border border-white/10">
      <div class="w-14 h-14 rounded-full bg-emerald-500/10 flex items-center justify-center mx-auto mb-3">
        <svg class="w-7 h-7 text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      </div>
      <p class="text-white font-medium text-sm">Todo al día</p>
      <p class="text-gray-500 text-sm mt-1">No hay clases pendientes de confirmación</p>
    </div>

    <!-- Error -->
    <div v-if="error" class="flex items-center gap-2 px-4 py-3 bg-red-500/10 border border-red-500/20 rounded-xl text-sm text-red-400">
      <svg class="w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      {{ error }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { venueService } from '../../services/venueService'

const loading = ref(true)
const clases = ref([])
const procesando = ref(null)
const error = ref('')

onMounted(async () => {
  try { clases.value = await venueService.getClassesPendingConfirmation() } catch { /* ignore */ }
  finally { loading.value = false }
})

const confirmarRealizada = async (id) => {
  procesando.value = id
  error.value = ''
  try {
    await venueService.confirmClassRealized(id)
    clases.value = clases.value.filter(c => c.id !== id)
  } catch {
    error.value = 'No se pudo confirmar la clase. Intenta de nuevo.'
  } finally {
    procesando.value = null
  }
}

const confirmarNoRealizada = async (id) => {
  procesando.value = id
  error.value = ''
  try {
    await venueService.confirmClassNotRealized(id)
    clases.value = clases.value.filter(c => c.id !== id)
  } catch {
    error.value = 'No se pudo registrar la incidencia. Intenta de nuevo.'
  } finally {
    procesando.value = null
  }
}

const fmt = (d, type) => {
  if (!d) return '—'
  const date = new Date(d)
  if (type === 'ddd') return date.toLocaleDateString('es-CL', { weekday: 'short' }).toUpperCase()
  if (type === 'D') return date.getDate()
  if (type === 'HH:mm') return date.toLocaleTimeString('es-CL', { hour: '2-digit', minute: '2-digit' })
  return d
}
</script>
