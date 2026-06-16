<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <div class="flex items-center justify-between mb-8">
      <h1 class="text-3xl font-bold text-white">Clases Agendadas</h1>
      <router-link to="/profesor/buscar-salas" class="btn-primary text-sm">Reservar Sala</router-link>
    </div>

    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>
    <div v-else-if="clases.length === 0" class="card text-center py-16">
      <div class="w-14 h-14 bg-[#1a1d2e] rounded-2xl flex items-center justify-center mx-auto mb-4">
        <svg class="w-7 h-7 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
        </svg>
      </div>
      <p class="text-gray-400 font-medium">Aún no tienes clases agendadas</p>
      <p class="text-gray-600 text-sm mt-1">Reserva una sala para crear tu primera clase.</p>
      <router-link to="/profesor/buscar-salas" class="btn-primary inline-flex mt-4">Buscar sala</router-link>
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
            <div v-if="c.status === 'DRAFT'" class="flex items-center gap-2">
              <router-link
                :to="'/profesor/crear-clase?edit=' + c.id"
                class="btn-primary text-xs !py-1.5 !px-3"
              >
                Completar Clase
              </router-link>
              <button
                @click="abrirBorradorSelector(c)"
                class="btn-secondary text-xs !py-1.5 !px-3"
              >
                Asignar Clase
              </button>
            </div>
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

    <BorradorSelector
      :abierto="borradorAbierto"
      :reservation-id="reservaSeleccionada?.id || null"
      :room-id="reservaSeleccionada?.roomId || null"
      :start-time="reservaSeleccionada?.startTime || null"
      :duration="reservaSeleccionada?.duration || 60"
      @close="borradorAbierto = false"
      @applied="cargar" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import classService from '@/services/classService'
import EstadoBadge from '@/components/EstadoBadge.vue'
import BorradorSelector from '@/components/BorradorSelector.vue'

const clases = ref([])
const loading = ref(true)

const borradorAbierto = ref(false)
const reservaSeleccionada = ref(null)

function abrirBorradorSelector(c) {
  reservaSeleccionada.value = c
  borradorAbierto.value = true
}

async function cargar() {
  loading.value = true
  try { clases.value = await classService.getTeacherPropias() } catch { clases.value = [] }
  loading.value = false
}

onMounted(() => { cargar() })

function formatDate(d) {
  return d ? new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : ''
}
</script>
