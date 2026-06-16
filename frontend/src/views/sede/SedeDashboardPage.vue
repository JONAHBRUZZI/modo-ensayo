<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-2">Panel de Sede</h1>
    <p class="text-gray-400 mb-8">Gestióna tu sede de ensayo</p>
    <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-10">
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
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <router-link to="/sede/salas" class="card hover:border-primary/50 transition-colors group"><h3 class="text-lg font-semibold text-white group-hover:text-primary">Salas</h3><p class="text-gray-400 text-sm mt-2">Gestióna salas y disponibilidad.</p></router-link>
      <router-link to="/sede/clases-por-confirmar" class="card hover:border-primary/50 transition-colors group"><h3 class="text-lg font-semibold text-white group-hover:text-primary">Clases por Confirmar</h3><p class="text-gray-400 text-sm mt-2">Confirma clases realizadas.</p></router-link>
      <router-link to="/sede/mis-clases" class="card hover:border-primary/50 transition-colors group"><h3 class="text-lg font-semibold text-white group-hover:text-primary">Mis Clases</h3><p class="text-gray-400 text-sm mt-2">Todas las clases de tu sede.</p></router-link>
      <router-link to="/sede/profesores" class="card hover:border-primary/50 transition-colors group"><h3 class="text-lg font-semibold text-white group-hover:text-primary">Profesores</h3><p class="text-gray-400 text-sm mt-2">Profesores de tu sede.</p></router-link>
      <router-link to="/sede/métricas" class="card hover:border-primary/50 transition-colors group"><h3 class="text-lg font-semibold text-white group-hover:text-primary">Métricas</h3><p class="text-gray-400 text-sm mt-2">Rendimiento de tu sede.</p></router-link>
      <router-link to="/sede/configuración" class="card hover:border-primary/50 transition-colors group"><h3 class="text-lg font-semibold text-white group-hover:text-primary">Configuración</h3><p class="text-gray-400 text-sm mt-2">Datos de tu sede.</p></router-link>
      <router-link to="/sede/crear-clase" class="card hover:border-primary/50 transition-colors group"><h3 class="text-lg font-semibold text-white group-hover:text-primary">Crear Clase</h3><p class="text-gray-400 text-sm mt-2">Crea una clase en tu sede.</p></router-link>
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
      } catch {}
    }
    stats.value.salas = totalSalas
  } catch {}
})
</script>
