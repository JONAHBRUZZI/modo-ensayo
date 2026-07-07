<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-2">Home</h1>
    <div class="flex items-center gap-3 mb-8">
      <p class="text-gray-400">Bienvenido, {{ displayName }}</p>
      <EstadoProfesorBadge :estado="estadoProfesor" />
      <span v-if="averageRating" class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-yellow-500/15 text-yellow-400 text-xs font-medium">
        ★ {{ averageRating?.toFixed(1) }}
      </span>
    </div>

    <!-- Stats (pulsables → detalle) -->
    <div class="grid grid-cols-2 md:grid-cols-5 gap-6 mb-10">
      <router-link to="/profesor/clases-propias" class="card group hover:border-primary/50 transition-colors">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2 group-hover:text-gray-300 transition-colors">Clases Propias</h3>
        <p class="text-3xl font-bold text-white">{{ stats.propias || 0 }}</p>
      </router-link>
      <router-link to="/profesor/clases-asignadas" class="card group hover:border-primary/50 transition-colors">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2 group-hover:text-gray-300 transition-colors">Asignadas</h3>
        <p class="text-3xl font-bold text-primary">{{ stats.asignadas || 0 }}</p>
      </router-link>
      <router-link to="/profesor/metricas" class="card group hover:border-primary/50 transition-colors">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2 group-hover:text-gray-300 transition-colors">Total Alumnos</h3>
        <p class="text-3xl font-bold text-green-400">{{ stats.alumnos || 0 }}</p>
      </router-link>
      <router-link to="/profesor/pagos" class="card group hover:border-primary/50 transition-colors">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2 group-hover:text-gray-300 transition-colors">Retenido</h3>
        <p class="text-3xl font-bold text-yellow-400">${{ stats.totalRetenido?.toLocaleString('es-CL') || 0 }}</p>
      </router-link>
      <router-link to="/profesor/pagos" class="card group hover:border-primary/50 transition-colors">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2 group-hover:text-gray-300 transition-colors">Liberado</h3>
        <p class="text-3xl font-bold text-green-400">${{ stats.totalLiberado?.toLocaleString('es-CL') || 0 }}</p>
      </router-link>
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

    <!-- Estado vacio: sin actividad activa -->
    <div v-if="!tienePropias && !tieneReservasActivas && !tieneAsignacionesActivas" class="card text-center py-12 mb-10">
      <div class="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
        <svg class="w-8 h-8 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"/>
        </svg>
      </div>
      <h3 class="text-white font-semibold mb-2">Crea tu primera clase</h3>
      <p class="text-gray-400 text-sm mb-4">Reserva una sala y publícala, o prepara un borrador para asignarlo después.</p>
      <div class="flex items-center justify-center gap-3 flex-wrap">
        <router-link to="/profesor/buscar-salas" class="btn-primary">Buscar Sala</router-link>
        <router-link to="/profesor/crear-borrador" class="btn-secondary">Crear Borrador</router-link>
      </div>
    </div>

    <!-- Accesos rapidos -->
    <h2 class="text-lg font-semibold text-white mb-4">Accesos rapidos</h2>
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <router-link to="/profesor/crear-borrador" class="card hover:border-primary/50 transition-colors group border-primary/30">
        <h3 class="text-lg font-semibold text-white group-hover:text-primary transition-colors">Crear Borrador</h3>
        <p class="text-gray-400 text-sm mt-2">Prepara una clase como plantilla y asígnala luego a una sala.</p>
      </router-link>
      <router-link to="/profesor/perfil-profesional" class="card hover:border-primary/50 transition-colors group border-primary/30">
        <h3 class="text-lg font-semibold text-white group-hover:text-primary transition-colors">Perfil Profesional</h3>
        <p class="text-gray-400 text-sm mt-2">Especialidad, experiencia, formacion y redes sociales.</p>
      </router-link>
      <router-link to="/profesor/buscar-salas" class="card hover:border-primary/50 transition-colors group">
        <h3 class="text-lg font-semibold text-white group-hover:text-primary">Agendar Sala</h3>
        <p class="text-gray-400 text-sm mt-2">Reserva una sala en una sede para tu proxima clase.</p>
      </router-link>
      <router-link to="/profesor/clases-por-asignar" class="card hover:border-primary/50 transition-colors group">
        <h3 class="text-lg font-semibold text-white group-hover:text-primary">Clases por Asignar</h3>
        <p class="text-gray-400 text-sm mt-2">Salas reservadas. Completa los datos de tu clase.</p>
      </router-link>
      <router-link to="/profesor/calendario" class="card hover:border-primary/50 transition-colors group">
        <h3 class="text-lg font-semibold text-white group-hover:text-primary">Calendario</h3>
        <p class="text-gray-400 text-sm mt-2">Vista semanal de todas tus clases y reservas.</p>
      </router-link>
      <router-link to="/profesor/borradores" class="card hover:border-primary/50 transition-colors group">
        <h3 class="text-lg font-semibold text-white group-hover:text-primary">Borradores</h3>
        <p class="text-gray-400 text-sm mt-2">Clases en borrador pendientes de publicar.</p>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuth } from '@/stores/auth'
import classService from '@/services/classService'
import EstadoProfesorBadge from '@/components/EstadoProfesorBadge.vue'
import professionalProfileService from '@/services/professionalProfileService'

const { displayName, tieneReservasActivas, tieneAsignacionesActivas, reservasSinClase, reservasSinClaseCount, estadoProfesor } = useAuth()

const stats = ref({ propias: 0, asignadas: 0, alumnos: 0, totalRetenido: 0, totalLiberado: 0 })
const averageRating = ref(null)

// El estado vacío de onboarding se muestra si el profesor no tiene clases propias
// (el flag de reservas puede estar en falso aunque existan clases).
const tienePropias = computed(() => stats.value.propias > 0)

onMounted(async () => {
  // Stats generales (los listados de clases se ven desde las tarjetas pulsables)
  const [propias, asignadas, earnings] = await Promise.allSettled([
    classService.getTeacherPropias(),
    classService.getTeacherAsignadas(),
    classService.getTeacherEarnings()
  ])

  const propiasData = propias.status === 'fulfilled' && Array.isArray(propias.value) ? propias.value : []
  const asignadasData = asignadas.status === 'fulfilled' && Array.isArray(asignadas.value) ? asignadas.value : []

  // Cuenta solo clases propias activas (no borradores), para coincidir con la
  // página "Clases Propias" que filtra los DRAFT (viven en "Borradores").
  const propiasPublicadas = propiasData.filter(c => c.status !== 'DRAFT')
  stats.value.propias = propiasPublicadas.length
  stats.value.asignadas = asignadasData.length
  stats.value.alumnos = [...propiasData, ...asignadasData].reduce((s, c) => s + (c.enrolledCount || 0), 0)
  if (earnings.status === 'fulfilled') {
    stats.value.totalRetenido = earnings.value?.resumen?.totalRetenido || 0
    stats.value.totalLiberado = earnings.value?.resumen?.totalLiberado || 0
  }

  // Cargar rating promedio
  try {
    const profile = await professionalProfileService.getMine()
    if (profile?.averageRating) averageRating.value = profile.averageRating
  } catch (err) {
    console.error('Error al cargar rating del profesor', err)
  }
})


</script>
