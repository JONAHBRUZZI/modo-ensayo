<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <div class="flex items-center justify-between mb-2 flex-wrap gap-3">
      <h1 class="text-3xl font-bold text-white">Disponibilidad de Sala</h1>
      <router-link to="/sede/salas" class="text-sm text-gray-400 hover:text-white">← Volver a salas</router-link>
    </div>
    <p class="text-gray-400 text-sm mb-8">
      Define los bloques horarios en que esta sala está disponible. Los maestros solo pueden reagendar sus clases
      dentro de estos bloques.
    </p>

    <!-- Agregar bloque -->
    <form @submit.prevent="agregar" class="card space-y-4 mb-8">
      <h2 class="text-white font-medium">Agregar bloque disponible</h2>
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div>
          <label class="block text-xs text-gray-400 mb-1">Fecha</label>
          <input type="date" v-model="form.fecha" required class="input-field" />
        </div>
        <div>
          <label class="block text-xs text-gray-400 mb-1">Hora inicio</label>
          <input type="time" v-model="form.inicio" required class="input-field" />
        </div>
        <div>
          <label class="block text-xs text-gray-400 mb-1">Hora fin</label>
          <input type="time" v-model="form.fin" required class="input-field" />
        </div>
      </div>
      <p v-if="msg" :class="msgType === 'error' ? 'text-red-400' : 'text-green-400'" class="text-sm">{{ msg }}</p>
      <button type="submit" :disabled="guardando" class="btn-primary text-sm">
        {{ guardando ? 'Guardando...' : 'Agregar bloque' }}
      </button>
    </form>

    <!-- Bloques existentes -->
    <h2 class="text-white font-medium mb-3">Bloques disponibles</h2>
    <div v-if="loading" class="text-gray-400 text-sm">Cargando...</div>
    <div v-else-if="slots.length === 0" class="card text-center py-8">
      <p class="text-gray-400 text-sm">Esta sala aún no tiene bloques de disponibilidad.</p>
    </div>
    <div v-else class="space-y-3">
      <div v-for="s in slotsOrdenados" :key="s.id" class="card flex items-center justify-between">
        <div>
          <p class="text-white text-sm">{{ formatBloque(s.startTime, s.endTime) }}</p>
          <p class="text-gray-500 text-xs">{{ s.roomName }}</p>
        </div>
        <button @click="eliminar(s.id)" class="text-red-400 text-xs hover:underline">Eliminar</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import venueService from '@/services/venueService'

const route = useRoute()
const roomId = route.params.salaId
const slots = ref([])
const loading = ref(true)
const guardando = ref(false)
const msg = ref('')
const msgType = ref('')
const form = reactive({ fecha: '', inicio: '', fin: '' })

const slotsOrdenados = computed(() =>
  [...slots.value].sort((a, b) => new Date(a.startTime) - new Date(b.startTime))
)

async function cargar() {
  loading.value = true
  try {
    slots.value = await venueService.getRoomAvailability(roomId)
  } catch {
    slots.value = []
  } finally {
    loading.value = false
  }
}

onMounted(cargar)

async function agregar() {
  msg.value = ''
  const start = new Date(`${form.fecha}T${form.inicio}`)
  const end = new Date(`${form.fecha}T${form.fin}`)
  if (end <= start) {
    msg.value = 'La hora de fin debe ser posterior a la de inicio.'
    msgType.value = 'error'
    return
  }
  guardando.value = true
  try {
    await venueService.createRoomAvailability(roomId, {
      roomId,
      startTime: start.toISOString(),
      endTime: end.toISOString()
    })
    msg.value = 'Bloque agregado correctamente.'
    msgType.value = 'success'
    form.inicio = ''; form.fin = ''
    await cargar()
  } catch (e) {
    msg.value = e.response?.data?.message || 'Error al agregar el bloque.'
    msgType.value = 'error'
  } finally {
    guardando.value = false
  }
}

async function eliminar(slotId) {
  try {
    await venueService.deleteRoomAvailability(roomId, slotId)
    slots.value = slots.value.filter(s => s.id !== slotId)
  } catch (e) {
    msg.value = e.response?.data?.message || 'Error al eliminar el bloque.'
    msgType.value = 'error'
  }
}

function formatBloque(start, end) {
  if (!start) return ''
  const s = new Date(start)
  const e = end ? new Date(end) : null
  const fecha = s.toLocaleDateString('es-CL', { weekday: 'long', day: 'numeric', month: 'long' })
  const hi = s.toLocaleTimeString('es-CL', { hour: '2-digit', minute: '2-digit' })
  const hf = e ? e.toLocaleTimeString('es-CL', { hour: '2-digit', minute: '2-digit' }) : ''
  return `${fecha} · ${hi}${hf ? ' - ' + hf : ''}`
}
</script>
