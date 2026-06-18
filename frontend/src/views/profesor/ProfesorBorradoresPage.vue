<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <div class="flex items-center justify-between mb-8">
      <div>
        <h1 class="text-3xl font-bold text-white">Mis Borradores</h1>
        <p class="text-gray-400 text-sm mt-1">Clases en borrador: sin sala o pendientes de publicar.</p>
      </div>
      <div class="flex gap-3">
        <router-link to="/profesor/crear-borrador" class="btn-secondary text-sm">Crear Borrador</router-link>
        <router-link to="/profesor/buscar-salas" class="btn-primary text-sm">Reservar Sala</router-link>
      </div>
    </div>

    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>

    <div v-else-if="borradores.length === 0" class="card text-center py-16">
      <div class="w-14 h-14 bg-[#1a1d2e] rounded-2xl flex items-center justify-center mx-auto mb-4">
        <svg class="w-7 h-7 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
        </svg>
      </div>
      <p class="text-gray-400 font-medium">Sin borradores</p>
      <p class='text-gray-600 text-sm mt-1'>Crea un borrador para preparar tu próxima clase.</p>
      <router-link to="/profesor/borradores/nuevo" class="btn-primary inline-flex mt-4">Nuevo borrador</router-link>
    </div>


    <div v-else class="space-y-4">
      <div v-for="c in borradores" :key="c.id" class="card">
        <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <!-- Info -->
          <div class="flex-1">
            <div class="flex items-center flex-wrap gap-2 mb-2">
              <!-- Badge según si tiene sala o no -->
              <span v-if="c.roomId" class="bg-yellow-500/20 text-yellow-400 text-xs font-medium px-2 py-1 rounded-full">
                Sala Reservada
              </span>
              <span v-else class="bg-gray-500/20 text-gray-400 text-xs font-medium px-2 py-1 rounded-full">
                Sin Sala
              </span>
              <span v-if="c.roomName" class="text-gray-400 text-xs">{{ c.roomName }}</span>
              <span v-if="c.venueName" class="text-gray-500 text-xs">· {{ c.venueName }}</span>
            </div>
            <h3 class="text-white font-semibold text-lg">
              {{ c.title || 'Sin título aún' }}
            </h3>
            <div class="flex flex-wrap gap-3 mt-1">
              <span v-if="c.discipline" class="text-gray-300 text-sm">{{ c.discipline }}</span>
              <span v-if="c.level" class="text-gray-500 text-sm">· {{ c.level }}</span>
            </div>
            <div class="flex flex-wrap gap-4 mt-2 text-sm">
              <span class="text-gray-400">
                <span class="text-gray-500">Fecha:</span> {{ c.startTime ? formatDate(c.startTime) : 'Sin asignar' }}
              </span>
              <span v-if="c.duration" class="text-gray-400">
                <span class="text-gray-500">Duracion:</span> {{ c.duration }} min
              </span>
              <span v-if="c.price != null" class="text-primary font-medium">
                ${{ c.price?.toLocaleString('es-CL') }}
              </span>
            </div>
          </div>

          <!-- Acciones -->
          <div class="flex items-center flex-wrap gap-3">
            <!-- Con sala: Completar y Publicar -->
            <router-link
              v-if="c.roomId"
              :to="'/profesor/crear-clase?edit=' + c.id"
              class="btn-primary text-sm"
            >
              Completar y Publicar
            </router-link>
            <!-- Sin sala: Asignar sala -->
            <router-link
              v-else
              :to="'/profesor/buscar-salas?borradorId=' + c.id"
              class="btn-primary text-sm"
            >
              Asignar Sala
            </router-link>
            <button
              @click="confirmarEliminar(c)"
              class="text-red-400 hover:text-red-300 text-sm transition-colors"
            >
              Eliminar
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal confirmacion eliminar -->
    <div v-if="borrandoId" class="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
      <div class="bg-[#161824] border border-gray-700 rounded-xl p-6 max-w-sm w-full mx-4">
        <h3 class="text-white font-semibold mb-2">Eliminar borrador</h3>
        <p class="text-gray-400 text-sm mb-6">
          ¿Estas seguro de eliminar este borrador?
          <span v-if="borrandoConSala" class="text-yellow-400"> Se liberara la sala reservada.</span>
          Esta accion no se puede deshacer.
        </p>
        <div class="flex space-x-3">
          <button @click="eliminarBorrador" :disabled="eliminando"
            class="btn-primary flex-1 bg-red-600 hover:bg-red-700 border-red-600">
            {{ eliminando ? 'Eliminando...' : 'Si, eliminar' }}
          </button>
          <button @click="borrandoId = null"
            class="flex-1 px-4 py-2 rounded-lg border border-gray-700 text-gray-300 hover:bg-[#1a1d2e]">
            Cancelar
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import classService from '@/services/classService'
import api from '@/services/api'
import { formatDate } from '@/utils/dateFormatter'

const borradores = ref([])
const loading = ref(true)
const borrandoId = ref(null)
const borrandoConSala = ref(false)
const eliminando = ref(false)

onMounted(async () => {
  await cargar()
})

async function cargar() {
  loading.value = true
  try {
    const todos = await classService.getTeacherDrafts()
    // Solo borradores SIN sala — los que tienen sala van a "Clases por Asignar"
    borradores.value = (Array.isArray(todos) ? todos : []).filter(c => !c.roomId)
  } catch {
    borradores.value = []
  }
  loading.value = false
}

function confirmarEliminar(clase) {
  borrandoId.value = clase.id
  borrandoConSala.value = !!clase.roomId
}

async function eliminarBorrador() {
  if (!borrandoId.value) return
  eliminando.value = true
  try {
    await api.delete('/classes/' + borrandoId.value)
    borradores.value = borradores.value.filter(c => c.id !== borrandoId.value)
    borrandoId.value = null
  } catch {
    borrandoId.value = null
  }
  eliminando.value = false
}


</script>
