<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <div class="flex items-center justify-between mb-8 flex-wrap gap-4">
      <h1 class="text-3xl font-bold text-white">Salas</h1>
      <router-link v-if="!loading && salas.length > 0" to="/sede/sala-registro" class="btn-primary text-sm inline-flex items-center gap-2">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/></svg>
        Agregar Sala
      </router-link>
    </div>
    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>
    <div v-else-if="salas.length === 0" class="card text-center py-12"><p class="text-gray-400">No tienes salas registradas.</p><router-link to="/sede/sala-registro" class="btn-primary mt-4 inline-block">Registrar Sala</router-link></div>
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="sala in salas" :key="sala.id" class="card">
        <h3 class="text-lg font-semibold text-white mb-2">{{ sala.name }}</h3>
        <p class="text-gray-400 text-sm">Capacidad: {{ sala.capacity }} | Tipo: {{ sala.type }}</p>
        <div class="mt-4 flex items-center gap-2">
          <router-link :to="{ name: 'SedeCalendario', query: { sala: sala.id } }" class="btn-primary text-sm">Agenda</router-link>
          <button
            @click="pedirEliminar(sala)"
            :disabled="verificandoId === sala.id"
            class="text-sm px-3 py-2 rounded-lg border border-red-500/40 text-red-400 hover:bg-red-500/10 transition-colors disabled:opacity-50"
          >
            {{ verificandoId === sala.id ? 'Verificando...' : 'Eliminar' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Confirmación de eliminación -->
    <BottomSheet v-model="modalAbierto">
      <h3 class="text-lg font-semibold text-white mb-2">Eliminar sala</h3>
      <p class="text-sm text-gray-400">
        ¿Eliminar la sala <span class="text-white font-medium">{{ salaAEliminar?.name }}</span>?
        Esta acción no se puede deshacer.
      </p>
      <div class="flex justify-end gap-3 mt-5">
        <button @click="modalAbierto = false" class="text-sm text-gray-400 hover:text-white">Cancelar</button>
        <button @click="confirmarEliminar" :disabled="eliminando" class="text-sm px-4 py-2 rounded-lg bg-red-500 text-white hover:bg-red-600 transition-colors disabled:opacity-50">
          {{ eliminando ? 'Eliminando...' : 'Eliminar' }}
        </button>
      </div>
    </BottomSheet>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import venueService from '@/services/venueService'
import { useToast } from '@/composables/useToast'
import BottomSheet from '@/components/BottomSheet.vue'

const toast = useToast()

const salas = ref([])
const loading = ref(true)

const modalAbierto = ref(false)
const salaAEliminar = ref(null)
const verificandoId = ref(null)
const eliminando = ref(false)

onMounted(async () => {
  await cargarSalas()
})

async function cargarSalas() {
  loading.value = true
  try {
    const venues = await venueService.getMyVenues()
    const vArr = Array.isArray(venues) ? venues : venues.content || []
    const sede = vArr.find(v => v.status === 'APROBADA') || vArr[0]
    if (sede) {
      salas.value = await venueService.getVenueRooms(sede.id)
    }
  } catch { salas.value = [] }
  loading.value = false
}

async function pedirEliminar(sala) {
  verificandoId.value = sala.id
  try {
    const reservas = await venueService.countReservasActivasSala(sala.id)
    if (reservas > 0) {
      toast.error('Esta sala tiene reservas activas y no puede eliminarse.')
      return
    }
    salaAEliminar.value = sala
    modalAbierto.value = true
  } catch {
    toast.error('No se pudo verificar las reservas de la sala.')
  } finally {
    verificandoId.value = null
  }
}

async function confirmarEliminar() {
  if (!salaAEliminar.value) return
  eliminando.value = true
  try {
    await venueService.deleteRoom(salaAEliminar.value.id)
    salas.value = salas.value.filter(s => s.id !== salaAEliminar.value.id)
    modalAbierto.value = false
    toast.success('Sala eliminada correctamente.')
  } catch (e) {
    toast.error(e?.response?.data?.message || 'Error al eliminar la sala.')
  } finally {
    eliminando.value = false
  }
}
</script>
