<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <div class="flex items-center justify-between mb-8">
      <h1 class="text-3xl font-bold text-white">Clases Agendadas</h1>
      <router-link to="/profesor/buscar-salas" class="btn-primary text-sm">Reservar Sala</router-link>
    </div>

    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>
    <div v-else-if="clases.length === 0" class="card text-center py-12">
      <p class="text-gray-400">No has creado clases propias aun.</p>
      <router-link to="/profesor/buscar-salas" class="btn-primary mt-4 inline-block">Buscar Sala</router-link>
    </div>
    <div v-else class="space-y-4">
      <div v-for="c in clases" :key="c.id" class="card">
        <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <div class="flex items-center space-x-2 mb-1">
              <EstadoBadge :status="c.status" />
              <span v-if="c.tipoClase === 'PROPIA'" class="text-xs text-gray-500">Clase propia</span>
            </div>
            <h3 class="text-white font-medium text-lg">{{ c.title || 'Sin titulo' }}</h3>
            <p class="text-gray-400 text-sm">{{ c.discipline }} · {{ c.level }}</p>
            <p class="text-gray-500 text-xs mt-1">{{ formatDate(c.startTime) }}</p>
          </div>
          <div class="flex flex-wrap items-center gap-3">
            <span class="text-primary font-semibold">${{ c.price?.toLocaleString('es-CL') }}</span>
            <span class="text-gray-400 text-sm">{{ c.enrolledCount || 0 }}/{{ c.capacity }} alumnos</span>
            <!-- DRAFT: pendiente de completar -->
            <router-link
              v-if="c.status === 'DRAFT'"
              :to="'/profesor/crear-clase?edit=' + c.id"
              class="btn-primary text-xs !py-1.5 !px-3"
            >
              Completar Clase
            </router-link>
            <!-- PUBLISHED: ir a asistencia -->
            <router-link
              v-else
              :to="'/profesor/asistencia/' + c.id"
              class="btn-primary text-xs !py-1.5 !px-3"
            >
              Asistencia
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import classService from '@/services/classService'
import EstadoBadge from '@/components/EstadoBadge.vue'

const clases = ref([])
const loading = ref(true)

onMounted(async () => {
  try { clases.value = await classService.getTeacherPropias() } catch { clases.value = [] }
  loading.value = false
})

function formatDate(d) {
  return d ? new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : ''
}
</script>
