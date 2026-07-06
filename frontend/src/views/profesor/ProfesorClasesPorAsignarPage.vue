<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <div class="flex items-center justify-between mb-8">
      <div>
        <h1 class="text-3xl font-bold text-white">Clases por Asignar</h1>
        <p class="text-gray-400 text-sm mt-1">Salas reservadas esperando que configures tu clase.</p>
      </div>
      <router-link to="/profesor/buscar-salas" class="btn-primary text-sm">Reservar Sala</router-link>
    </div>

    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>

    <div v-else-if="reservas.length === 0" class="card text-center py-16">
      <div class="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
        <svg class="w-8 h-8 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
        </svg>
      </div>
      <h3 class="text-white font-semibold mb-2">Sin salas reservadas</h3>
      <p class="text-gray-400 text-sm mb-6">Reserva una sala para luego crear tu clase en ese espacio.</p>
      <router-link to="/profesor/buscar-salas" class="btn-primary">Buscar Sala</router-link>
    </div>

    <div v-else class="space-y-4">
      <div v-for="reserva in reservas" :key="reserva.id" class="card">
        <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <!-- Info de la reserva -->
          <div class="flex-1">
            <div class="flex items-center gap-2 mb-2">
              <span class="bg-yellow-500/20 text-yellow-400 text-xs font-medium px-2 py-1 rounded-full">
                Sala Reservada
              </span>
            </div>
            <h3 class="text-white font-semibold text-lg">{{ reserva.roomName || 'Sala' }}</h3>
            <p class="text-gray-400 text-sm">{{ reserva.venueName || 'Sede' }}</p>
            <div class="flex flex-wrap gap-4 mt-2 text-sm">
              <span class="text-gray-400">
                <span class="text-gray-500">Fecha:</span>
                {{ reserva.startTime ? formatDate(reserva.startTime) : 'Sin fecha' }}
              </span>
              <span v-if="reserva.duration" class="text-gray-400">
                <span class="text-gray-500">Duracion:</span> {{ reserva.duration }} min
              </span>
              <span v-if="reserva.price != null" class="text-primary font-medium">
                ${{ reserva.price?.toLocaleString('es-CL') }}
              </span>
            </div>
          </div>

          <!-- Acciones -->
          <div class="flex items-center flex-wrap gap-3 flex-shrink-0">
            <!-- Crear clase nueva para esta sala -->
            <router-link
              :to="'/profesor/crear-clase?edit=' + reserva.id"
              class="btn-primary text-sm"
            >
              Crear Clase
            </router-link>
            <!-- Asignar borrador existente a esta sala -->
            <button @click="abrirModalBorrador(reserva)" class="btn-secondary text-sm">
              Usar Borrador
            </button>
            <button @click="confirmarEliminar(reserva)" class="text-red-400 hover:text-red-300 text-sm transition-colors">
              Liberar
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal: Usar borrador existente -->
    <BottomSheet v-model="modalBorrador.abierto">
      <h3 class="text-white font-semibold text-lg mb-1">Seleccionar Borrador</h3>
      <p class="text-gray-400 text-sm mb-4">
        Asigna la sala reservada a uno de tus borradores y publícalo.
      </p>

      <div v-if="loadingBorradores" class="text-gray-400 text-sm py-4 text-center">
        Cargando borradores...
      </div>
      <div v-else-if="borradores.length === 0" class="text-center py-4">
        <p class="text-gray-500 text-sm">No tienes borradores sin sala disponibles.</p>
        <router-link to="/profesor/crear-borrador" class="text-primary text-sm underline mt-2 inline-block">
          Crear un borrador
        </router-link>
      </div>
      <div v-else class="space-y-2 max-h-64 overflow-y-auto mb-4">
        <button
          v-for="b in borradores"
          :key="b.id"
          @click="borradoresSeleccionado = b"
          :class="[
            'w-full text-left p-3 rounded-xl border transition-colors',
            borradoresSeleccionado?.id === b.id
              ? 'border-primary bg-primary/10'
              : 'border-white/10 hover:border-white/20 bg-[var(--bg-base)]'
          ]"
        >
          <p class="text-white text-sm font-medium">{{ b.title }}</p>
          <p class="text-gray-400 text-xs mt-0.5">
            {{ b.discipline || 'Sin disciplina' }}
            <span v-if="b.level"> · {{ b.level }}</span>
            <span v-if="b.price != null"> · ${{ b.price?.toLocaleString('es-CL') }}</span>
          </p>
        </button>
      </div>

      <p v-if="modalBorrador.error" class="text-red-400 text-sm mb-3">{{ modalBorrador.error }}</p>

      <div class="flex gap-3">
        <button
          @click="asignarBorrador"
          :disabled="!borradoresSeleccionado || modalBorrador.procesando"
          class="btn-primary flex-1 text-sm"
        >
          {{ modalBorrador.procesando ? 'Publicando...' : 'Asignar y Publicar' }}
        </button>
        <button @click="modalBorrador.abierto = false" class="btn-secondary text-sm px-4">
          Cancelar
        </button>
      </div>
    </BottomSheet>

    <!-- Modal confirmacion liberar sala -->
    <BottomSheet :model-value="!!eliminandoId" @update:model-value="$event || (eliminandoId = null)">
      <h3 class="text-white font-semibold mb-2">Liberar sala reservada</h3>
      <p class="text-gray-400 text-sm mb-6">
        ¿Confirmas cancelar esta reserva? La sala quedara disponible para otros profesores.
      </p>
      <div class="flex gap-3">
        <button @click="eliminarReserva" :disabled="eliminando"
          class="btn-primary flex-1 bg-red-600 hover:bg-red-700 border-red-600">
          {{ eliminando ? 'Liberando...' : 'Si, liberar sala' }}
        </button>
        <button @click="eliminandoId = null"
          class="flex-1 px-4 py-2 rounded-lg border border-gray-700 text-gray-300 hover:bg-[var(--bg-elevated)]">
          Cancelar
        </button>
      </div>
    </BottomSheet>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import classService from '@/services/classService'
import { useAuth } from '@/stores/auth'
import { formatDate } from '@/utils/dateFormatter'
import BottomSheet from '@/components/BottomSheet.vue'

const router = useRouter()
const { syncAtributos } = useAuth()

const reservas = ref([])
const loading = ref(true)

const borradores = ref([])
const loadingBorradores = ref(false)
const borradoresSeleccionado = ref(null)
const modalBorrador = ref({ abierto: false, reserva: null, procesando: false, error: '' })

const eliminandoId = ref(null)
const eliminando = ref(false)

onMounted(async () => {
  await cargar()
})

async function cargar() {
  loading.value = true
  try {
    const todos = await classService.getTeacherDrafts()
    // Solo los que tienen sala asignada = reservas pendientes de configurar
    reservas.value = (Array.isArray(todos) ? todos : []).filter(c => c.roomId)
  } catch {
    reservas.value = []
  }
  loading.value = false
}

async function abrirModalBorrador(reserva) {
  modalBorrador.value = { abierto: true, reserva, procesando: false, error: '' }
  borradoresSeleccionado.value = null
  loadingBorradores.value = true
  try {
    const todos = await classService.getTeacherDrafts()
    // Solo borradores SIN sala disponibles para asignar
    borradores.value = (Array.isArray(todos) ? todos : []).filter(b => !b.roomId)
  } catch {
    borradores.value = []
  }
  loadingBorradores.value = false
}

async function asignarBorrador() {
  if (!borradoresSeleccionado.value || !modalBorrador.value.reserva) return
  modalBorrador.value.procesando = true
  modalBorrador.value.error = ''
  const reserva = modalBorrador.value.reserva
  const borrador = borradoresSeleccionado.value
  try {
    // 1. Asignar la sala de la reserva al borrador seleccionado (lo publica)
    await classService.assignReserva(borrador.id, {
      roomId: reserva.roomId,
      startTime: new Date(reserva.startTime).toISOString(),
      duration: reserva.duration || 60,
      reservationId: reserva.id
    })
    // 2. Eliminar el draft de la reserva original
    await classService.deleteDraft(reserva.id)
    // 3. Actualizar atributos del usuario
    await syncAtributos()
    modalBorrador.value.abierto = false
    router.push('/profesor/clases-propias')
  } catch (e) {
    modalBorrador.value.error = e?.response?.data?.error || e?.response?.data?.message || 'Error al asignar el borrador'
  }
  modalBorrador.value.procesando = false
}

function confirmarEliminar(reserva) {
  eliminandoId.value = reserva.id
}

async function eliminarReserva() {
  if (!eliminandoId.value) return
  eliminando.value = true
  try {
    await classService.deleteDraft(eliminandoId.value)
    reservas.value = reservas.value.filter(r => r.id !== eliminandoId.value)
    await syncAtributos()
    eliminandoId.value = null
  } catch {
    eliminandoId.value = null
  }
  eliminando.value = false
}
</script>
