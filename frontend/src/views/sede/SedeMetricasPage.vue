<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-2">Estadísticas de Sede</h1>
    <p class="text-gray-400 mb-8">Datos de tus salas para tomar mejores decisiones.</p>

    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>

    <div v-else-if="sinDatos" class="card text-center py-16">
      <p class="text-gray-400">Aún no hay clases publicadas en tu sede para generar estadísticas.</p>
    </div>

    <div v-else class="space-y-10">
      <!-- KPIs -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-6">
        <div class="card">
          <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Total Clases</h3>
          <p class="text-3xl font-bold text-white">{{ kpis.totalClases || 0 }}</p>
        </div>
        <div class="card">
          <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Alumnos</h3>
          <p class="text-3xl font-bold text-green-400">{{ kpis.totalAlumnos || 0 }}</p>
        </div>
        <div class="card">
          <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Ocupación Promedio</h3>
          <p class="text-3xl font-bold text-primary">{{ kpis.ocupacionPromedio || 0 }}%</p>
        </div>
        <div class="card">
          <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Ingresos</h3>
          <p class="text-3xl font-bold text-yellow-400">${{ Number(kpis.ingresos || 0).toLocaleString('es-CL') }}</p>
        </div>
      </div>

      <!-- Disciplinas más demandadas -->
      <section>
        <h2 class="text-lg font-semibold text-white mb-1">Clases más cotizadas</h2>
        <p class="text-gray-500 text-sm mb-4">Disciplinas ordenadas por cantidad de alumnos inscritos.</p>
        <div class="card overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="text-gray-500 text-xs uppercase">
                <th class="text-left py-2 px-2">Disciplina</th>
                <th class="text-right py-2 px-2">Clases</th>
                <th class="text-right py-2 px-2">Alumnos</th>
                <th class="text-right py-2 px-2">Ocupación</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in disciplinas" :key="d.disciplina" class="border-t border-white/5">
                <td class="py-2 px-2 text-gray-200">{{ d.disciplina }}</td>
                <td class="py-2 px-2 text-right text-gray-400">{{ d.clases }}</td>
                <td class="py-2 px-2 text-right text-green-400 font-medium">{{ d.alumnos }}</td>
                <td class="py-2 px-2 text-right"><span :class="ocupacionColor(d.ocupacion)">{{ d.ocupacion }}%</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- Salas con menor uso -->
      <section>
        <h2 class="text-lg font-semibold text-white mb-1">Uso de tus salas</h2>
        <p class="text-gray-500 text-sm mb-4">Ordenadas de menor a mayor ocupación — las primeras necesitan más difusión.</p>
        <div class="card overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="text-gray-500 text-xs uppercase">
                <th class="text-left py-2 px-2">Sala</th>
                <th class="text-right py-2 px-2">Clases</th>
                <th class="text-right py-2 px-2">Alumnos</th>
                <th class="text-right py-2 px-2">Ocupación</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="s in salas" :key="s.sala" class="border-t border-white/5">
                <td class="py-2 px-2 text-gray-200">{{ s.sala }}</td>
                <td class="py-2 px-2 text-right text-gray-400">{{ s.clases }}</td>
                <td class="py-2 px-2 text-right text-gray-300">{{ s.alumnos }}</td>
                <td class="py-2 px-2 text-right"><span :class="ocupacionColor(s.ocupacion)">{{ s.ocupacion }}%</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- Franjas horarias -->
      <section>
        <h2 class="text-lg font-semibold text-white mb-1">Ocupación por franja horaria</h2>
        <p class="text-gray-500 text-sm mb-4">Identifica los horarios con baja ocupación para reforzar la oferta.</p>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div v-for="h in horarios" :key="h.franja" class="card">
            <h3 class="text-gray-300 text-sm font-medium mb-2">{{ h.franja }}</h3>
            <p class="text-2xl font-bold mb-1" :class="ocupacionColor(h.ocupacion)">{{ h.ocupacion }}%</p>
            <p class="text-xs text-gray-500">{{ h.clases }} clase{{ h.clases === 1 ? '' : 's' }} · {{ h.alumnos }} alumno{{ h.alumnos === 1 ? '' : 's' }}</p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import venueService from '@/services/venueService'

const stats = ref({ kpis: {}, salas: [], disciplinas: [], horarios: [] })
const loading = ref(true)

const kpis = computed(() => stats.value.kpis || {})
const salas = computed(() => stats.value.salas || [])
const disciplinas = computed(() => stats.value.disciplinas || [])
const horarios = computed(() => stats.value.horarios || [])
const sinDatos = computed(() => !kpis.value.totalClases)

// Verde (buena ocupación) → amarillo → rojo (baja ocupación, necesita atención).
function ocupacionColor(pct) {
  const n = Number(pct) || 0
  if (n >= 70) return 'text-green-400'
  if (n >= 40) return 'text-yellow-400'
  return 'text-red-400'
}

onMounted(async () => {
  try {
    stats.value = await venueService.getVenueStats()
  } catch (err) {
    console.error('Error al cargar estadísticas de la sede', err)
    stats.value = { kpis: {}, salas: [], disciplinas: [], horarios: [] }
  }
  loading.value = false
})
</script>
