<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-2">Panel de Maestro</h1>
    <div class="flex items-center gap-3 mb-8">
      <p class="text-gray-400">Bienvenido, {{ displayName }}</p>
      <EstadoProfesorBadge :estado="estadoProfesor" />
      <span v-if="averageRating" class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-yellow-500/15 text-yellow-400 text-xs font-medium">
        ★ {{ averageRating?.toFixed(1) }}
      </span>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-2 md:grid-cols-5 gap-6 mb-10">
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Clases Propias</h3>
        <p class="text-3xl font-bold text-white">{{ stats.propias || 0 }}</p>
      </div>
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Asignadas</h3>
        <p class="text-3xl font-bold text-primary">{{ stats.asignadas || 0 }}</p>
      </div>
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Total Alumnos</h3>
        <p class="text-3xl font-bold text-green-400">{{ stats.alumnos || 0 }}</p>
      </div>
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Retenido</h3>
        <p class="text-3xl font-bold text-yellow-400">${{ stats.totalRetenido?.toLocaleString('es-CL') || 0 }}</p>
      </div>
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Liberado</h3>
        <p class="text-3xl font-bold text-green-400">${{ stats.totalLiberado?.toLocaleString('es-CL') || 0 }}</p>
      </div>
    </div>

    <!-- Seccion: Clases por Asignar (salas reservadas sin clase configurada) -->
    <div v-if="reservasSinClase" class="mb-8">
      <div class="bg-yellow-500/10 border border-yellow-500/30 rounded-xl p-5 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div class="flex items-start gap-3">
          <div class="w-10 h-10 bg-yellow-500/20 rounded-lg flex items-center justify-center flex-shrink-0 mt-0.5">
            <svg class="w-5 h-5 text-yellow-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
            </svg>
          </div>
          <div>
            <h2 class="text-white font-semibold">
              Clases por Asignar
              <span class="ml-2 bg-yellow-500 text-black text-xs font-bold px-2 py-0.5 rounded-full">{{ reservasSinClaseCount }}</span>
            </h2>
            <p class="text-gray-400 text-sm mt-0.5">
              Tienes {{ reservasSinClaseCount === 1 ? 'una sala reservada' : reservasSinClaseCount + ' salas reservadas' }}
              esperando que configures la clase.
            </p>
          </div>
        </div>
        <router-link to="/profesor/clases-por-asignar" class="btn-primary text-sm flex-shrink-0">
          Configurar Clases
        </router-link>
      </div>
    </div>

    <!-- Seccion A: Clases propias activas -->
    <div v-if="tieneReservasActivas" class="mb-10">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-xl font-semibold text-white">Mis Clases Próximas</h2>
        <router-link to="/profesor/clases-propias" class="text-primary text-sm hover:underline">Ver todas</router-link>
      </div>
      <div v-if="loadingPropias" class="text-gray-400 text-sm">Cargando...</div>
      <div v-else-if="propiasFuturas.length === 0" class="card text-center py-6">
        <p class="text-gray-400 text-sm">No hay clases próximas.</p>
      </div>
      <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div v-for="c in propiasFuturas.slice(0, 4)" :key="c.id" class="card">
          <div class="flex items-start justify-between">
            <div>
              <h3 class="text-white font-medium">{{ c.title }}</h3>
              <p class="text-gray-400 text-sm">{{ c.discipline }} · {{ c.level }}</p>
              <p class="text-gray-500 text-xs mt-1">{{ formatDate(c.startTime) }}</p>
            </div>
            <span class="text-primary font-semibold text-sm">${{ c.price?.toLocaleString('es-CL') }}</span>
          </div>
          <div class="flex items-center justify-between mt-3">
            <span class="text-gray-400 text-xs">{{ c.enrolledCount || 0 }}/{{ c.capacity }} alumnos</span>
            <router-link :to="'/profesor/asistencia/' + c.id" class="btn-primary text-xs !py-1 !px-3">Asistencia</router-link>
          </div>
        </div>
      </div>
    </div>

    <!-- Seccion B: Clases asignadas activas -->
    <div v-if="tieneAsignacionesActivas" class="mb-10">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-xl font-semibold text-white">Clases Asignadas</h2>
        <router-link to="/profesor/clases-asignadas" class="text-primary text-sm hover:underline">Ver todas</router-link>
      </div>
      <div v-if="loadingAsignadas" class="text-gray-400 text-sm">Cargando...</div>
      <div v-else-if="asignadasActivas.length === 0" class="card text-center py-6">
        <p class="text-gray-400 text-sm">No hay clases asignadas activas.</p>
      </div>
      <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div v-for="c in asignadasActivas.slice(0, 4)" :key="c.id" class="card">
          <div class="flex items-start justify-between">
            <div>
              <h3 class="text-white font-medium">{{ c.title }}</h3>
              <p class="text-gray-400 text-sm">{{ c.discipline }} · {{ c.level }}</p>
              <p class="text-gray-500 text-xs mt-1">{{ formatDate(c.startTime) }}</p>
            </div>
            <span class="badge badge-blue text-xs">Asignada</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Estado vacio: sin actividad activa -->
    <div v-if="!tieneReservasActivas && !tieneAsignacionesActivas" class="card text-center py-12 mb-10">
      <div class="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
        <svg class="w-8 h-8 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"/>
        </svg>
      </div>
      <h3 class="text-white font-semibold mb-2">Crea tu primera clase</h3>
      <p class="text-gray-400 text-sm mb-4">Reserva una sala y publica tu clase para comenzar a recibir alumnos.</p>
      <router-link to="/profesor/buscar-salas" class="btn-primary">Buscar Sala</router-link>
    </div>

    <!-- Accesos rapidos -->
    <h2 class="text-lg font-semibold text-white mb-4">Accesos rapidos</h2>
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <router-link to="/profesor/perfil-profesional" class="card hover:border-primary/50 transition-colors group border-primary/30">
        <h3 class="text-lg font-semibold text-white group-hover:text-primary transition-colors">Perfil Profesional</h3>
        <p class="text-gray-400 text-sm mt-2">Especialidad, experiencia, formacion y redes sociales.</p>
      </router-link>
      <router-link to="/profesor/buscar-salas" class="card hover:border-primary/50 transition-colors group">
        <h3 class="text-lg font-semibold text-white group-hover:text-primary">Agendar Sala</h3>
        <p class="text-gray-400 text-sm mt-2">Reserva una sala en una sede para tu proxima clase.</p>
      </router-link>
      <router-link to="/profesor/metricas" class="card hover:border-primary/50 transition-colors group">
        <h3 class="text-lg font-semibold text-white group-hover:text-primary">Métricas</h3>
        <p class="text-gray-400 text-sm mt-2">Estadisticas de rendimiento y asistencia.</p>
      </router-link>
      <router-link to="/profesor/pagos" class="card hover:border-primary/50 transition-colors group">
        <h3 class="text-lg font-semibold text-white group-hover:text-primary">Pagos</h3>
        <p class="text-gray-400 text-sm mt-2">Historial de pagos recibidos por tus clases.</p>
      </router-link>
      <router-link to="/profesor/clases-propias" class="card hover:border-primary/50 transition-colors group">
        <h3 class="text-lg font-semibold text-white group-hover:text-primary">Clases Agendadas</h3>
        <p class="text-gray-400 text-sm mt-2">Salas reservadas. Completa los datos de tu clase.</p>
      </router-link>
      <router-link to="/profesor/clases-asignadas" class="card hover:border-primary/50 transition-colors group">
        <h3 class="text-lg font-semibold text-white group-hover:text-primary">Clases Asignadas</h3>
        <p class="text-gray-400 text-sm mt-2">Clases donde fuiste asignado como profesor.</p>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuth } from '@/stores/auth'
import classService from '@/services/classService'
import EstadoProfesorBadge from '@/components/EstadoProfesorBadge.vue'
import api from '@/services/api'

const { displayName, tieneReservasActivas, tieneAsignacionesActivas, reservasSinClase, reservasSinClaseCount, estadoProfesor } = useAuth()

const stats = ref({ propias: 0, asignadas: 0, alumnos: 0, totalRetenido: 0, totalLiberado: 0 })
const averageRating = ref(null)
const propiasFuturas = ref([])
const asignadasActivas = ref([])
const loadingPropias = ref(false)
const loadingAsignadas = ref(false)

const now = new Date()

onMounted(async () => {
  // Stats generales
  const [propias, asignadas, earnings] = await Promise.allSettled([
    classService.getTeacherPropias(),
    classService.getTeacherAsignadas(),
    classService.getTeacherEarnings()
  ])

  const propiasData = propias.status === 'fulfilled' && Array.isArray(propias.value) ? propias.value : []
  const asignadasData = asignadas.status === 'fulfilled' && Array.isArray(asignadas.value) ? asignadas.value : []

  stats.value.propias = propiasData.length
  stats.value.asignadas = asignadasData.length
  stats.value.alumnos = [...propiasData, ...asignadasData].reduce((s, c) => s + (c.enrolledCount || 0), 0)
  if (earnings.status === 'fulfilled') {
    stats.value.totalRetenido = earnings.value?.resumen?.totalRetenido || 0
    stats.value.totalLiberado = earnings.value?.resumen?.totalLiberado || 0
  }

  // Seccion A: propias futuras
  if (tieneReservasActivas.value) {
    loadingPropias.value = true
    propiasFuturas.value = propiasData
      .filter(c => c.startTime && new Date(c.startTime) > now)
      .sort((a, b) => new Date(a.startTime) - new Date(b.startTime))
    loadingPropias.value = false
  }

  // Seccion B: asignadas activas
  if (tieneAsignacionesActivas.value) {
    loadingAsignadas.value = true
    asignadasActivas.value = asignadasData
      .filter(c => c.startTime && new Date(c.startTime) > now)
      .sort((a, b) => new Date(a.startTime) - new Date(b.startTime))
    loadingAsignadas.value = false
  }

  // Cargar rating promedio
  try {
    const profile = await api.get('/users/me/professional-profile')
    if (profile.data?.averageRating) averageRating.value = profile.data.averageRating
  } catch (err) {
    console.error('Error al cargar rating del profesor', err)
  }
})

function formatDate(d) {
  return d ? new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : ''
}
</script>
