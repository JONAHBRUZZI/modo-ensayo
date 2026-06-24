<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-2">Panel de Sede</h1>
    <p class="text-gray-400 mb-8">Gestióna tu sede de ensayo</p>

    <!-- Stats -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Salas</h3>
        <p class="text-3xl font-bold text-white">{{ stats.salas || 0 }}</p>
      </div>
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Clases Hoy</h3>
        <p class="text-3xl font-bold text-primary">{{ stats.clasesHoy || 0 }}</p>
      </div>
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Por Confirmar</h3>
        <p class="text-3xl font-bold text-yellow-400">{{ stats.porConfirmar || 0 }}</p>
      </div>
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Ingresos</h3>
        <p class="text-3xl font-bold text-green-400">${{ stats.ingresos?.toLocaleString() || 0 }}</p>
      </div>
    </div>

    <!-- Alerta urgente: clases pendientes de confirmación -->
    <div v-if="stats.porConfirmar > 0" class="mb-8 rounded-xl bg-yellow-500/10 border border-yellow-500/30 px-5 py-4 flex items-center justify-between gap-4 flex-wrap">
      <div class="flex items-center gap-3">
        <svg class="w-5 h-5 text-yellow-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
        </svg>
        <span class="text-yellow-200 text-sm font-medium">
          {{ stats.porConfirmar === 1 ? 'Hay 1 clase esperando tu confirmación.' : `Hay ${stats.porConfirmar} clases esperando tu confirmación.` }}
        </span>
      </div>
      <router-link to="/sede/clases-por-confirmar" class="px-4 py-2 rounded-lg bg-yellow-500 text-black text-sm font-semibold hover:bg-yellow-400 transition-colors whitespace-nowrap flex-shrink-0">
        Confirmar ahora
      </router-link>
    </div>

    <!-- Acciones rápidas -->
    <div class="flex flex-wrap gap-3">
      <router-link to="/sede/crear-clase" class="btn-primary inline-flex items-center gap-2">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        Crear Clase
      </router-link>
      <router-link to="/sede/calendario" class="inline-flex items-center gap-2 px-4 py-2 rounded-lg border border-[var(--border-subtle)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:border-primary/50 transition-colors text-sm">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
        </svg>
        Ver Calendario
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import venueService from '@/services/venueService'

const stats = ref({ salas: 0, clasesHoy: 0, porConfirmar: 0, ingresos: 0 })

onMounted(async () => {
  try {
    const [metrics, pendingProm] = await Promise.allSettled([
      venueService.getVenueMetrics(),
      venueService.getPendingClasses()
    ])
    if (metrics.status === 'fulfilled' && metrics.value) {
      stats.value.ingresos = metrics.value.ingresos || 0
      stats.value.clasesHoy = metrics.value.totalClases || 0
    }
    if (pendingProm.status === 'fulfilled' && pendingProm.value) {
      stats.value.porConfirmar = pendingProm.value.length || 0
    }

    const myVenues = await venueService.getMyVenues()
    const vArr = Array.isArray(myVenues) ? myVenues : myVenues?.content || []
    let totalSalas = 0
    for (const v of vArr) {
      try {
        const rooms = await venueService.getVenueRooms(v.id)
        totalSalas += Array.isArray(rooms) ? rooms.length : 0
      } catch (err) {
        console.error('Error al cargar salas de la sede', err)
      }
    }
    stats.value.salas = totalSalas
  } catch (err) {
    console.error('Error al cargar estadísticas de la sede', err)
  }
})
</script>
