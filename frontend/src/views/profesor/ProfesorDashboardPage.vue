<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-2">Panel de Profesor</h1>
    <p class="text-gray-400 mb-8">Bienvenido, {{ displayName }}</p>
    <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-10">
      <div class="card"><h3 class="text-gray-400 text-sm mb-1">Clases Propias</h3><p class="text-3xl font-bold text-white">{{ stats.propias || 0 }}</p></div>
      <div class="card"><h3 class="text-gray-400 text-sm mb-1">Asignadas</h3><p class="text-3xl font-bold text-primary">{{ stats.asignadas || 0 }}</p></div>
      <div class="card"><h3 class="text-gray-400 text-sm mb-1">Alumnos</h3><p class="text-3xl font-bold text-green-400">{{ stats.alumnos || 0 }}</p></div>
      <div class="card"><h3 class="text-gray-400 text-sm mb-1">Ganancias</h3><p class="text-3xl font-bold text-yellow-400">${{ stats.ganancias?.toLocaleString() || 0 }}</p></div>
    </div>
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <router-link to="/profesor/perfil-profesional" class="card hover:border-primary/50 transition-colors group border-primary/30">
        <h3 class="text-lg font-semibold text-white group-hover:text-primary transition-colors">Perfil Profesional</h3>
        <p class="text-gray-400 text-sm mt-2">Completa tu perfil de maestro: especialidad, experiencia, formacion y redes.</p>
      </router-link>
      <router-link to="/profesor/clases-propias" class="card hover:border-primary/50 transition-colors group"><h3 class="text-lg font-semibold text-white group-hover:text-primary">Mis Clases</h3><p class="text-gray-400 text-sm mt-2">Gestiona las clases que tu creaste.</p></router-link>
      <router-link to="/profesor/clases-asignadas" class="card hover:border-primary/50 transition-colors group"><h3 class="text-lg font-semibold text-white group-hover:text-primary">Clases Asignadas</h3><p class="text-gray-400 text-sm mt-2">Clases donde fuiste asignado como profesor.</p></router-link>
      <router-link to="/profesor/buscar-salas" class="card hover:border-primary/50 transition-colors group"><h3 class="text-lg font-semibold text-white group-hover:text-primary">Agendar Sala</h3><p class="text-gray-400 text-sm mt-2">Reserva una sala en una sede para dictar tu clase.</p></router-link>
      <router-link to="/profesor/metricas" class="card hover:border-primary/50 transition-colors group"><h3 class="text-lg font-semibold text-white group-hover:text-primary">Metricas</h3><p class="text-gray-400 text-sm mt-2">Estadisticas de rendimiento y asistencia.</p></router-link>
      <router-link to="/profesor/pagos" class="card hover:border-primary/50 transition-colors group"><h3 class="text-lg font-semibold text-white group-hover:text-primary">Pagos</h3><p class="text-gray-400 text-sm mt-2">Historial de pagos recibidos.</p></router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuth } from '@/stores/auth'
import classService from '@/services/classService'

const { displayName } = useAuth()
const stats = ref({ propias: 0, asignadas: 0, alumnos: 0, ganancias: 0 })

onMounted(async () => {
  try {
    const [propias, asignadas, todas] = await Promise.allSettled([
      classService.getTeacherPropias(),
      classService.getTeacherAsignadas(),
      classService.getTeacherClasses()
    ])
    if (propias.status === 'fulfilled') stats.value.propias = Array.isArray(propias.value) ? propias.value.length : 0
    if (asignadas.status === 'fulfilled') stats.value.asignadas = Array.isArray(asignadas.value) ? asignadas.value.length : 0
    if (todas.status === 'fulfilled' && Array.isArray(todas.value)) {
      stats.value.alumnos = todas.value.reduce((s, c) => s + (c.enrolledCount || 0), 0)
      stats.value.ganancias = todas.value.reduce((s, c) => s + ((c.price || 0) * (c.enrolledCount || 0)), 0)
    }
  } catch {}
})
</script>
