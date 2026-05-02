<template>
  <div class="max-w-2xl mx-auto space-y-8">
    <div>
      <h1 class="text-3xl font-bold text-gray-900 mb-2">Registrar Sala</h1>
      <p class="text-gray-600">Agrega una sala con sus caracteristicas artisticas</p>
    </div>

    <form @submit.prevent="submitRoom" class="bg-white rounded-xl shadow-md p-6 space-y-4">
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Sede *</label>
        <select v-model="form.venueId" required class="w-full px-3 py-2 border border-gray-300 rounded-lg">
          <option value="">Selecciona una sede</option>
          <option v-for="v in venues" :key="v.id" :value="v.id">{{ v.name }}</option>
        </select>
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Nombre de la Sala *</label>
        <input v-model="form.name" required class="w-full px-3 py-2 border border-gray-300 rounded-lg" />
      </div>
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Capacidad *</label>
          <input v-model.number="form.capacity" type="number" required class="w-full px-3 py-2 border border-gray-300 rounded-lg" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Tipo de Piso</label>
          <select v-model="form.floorType" class="w-full px-3 py-2 border border-gray-300 rounded-lg">
            <option value="">Seleccionar</option>
            <option>Madera flotante</option><option>Parquet</option><option>Linoleo</option><option>Alfombra</option><option>Harlequin</option>
          </select>
        </div>
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">Caracteristicas Artisticas</label>
        <div class="grid grid-cols-2 gap-3">
          <label class="flex items-center space-x-2"><input v-model="form.hasMirrors" type="checkbox" class="rounded" /> Espejos</label>
          <label class="flex items-center space-x-2"><input v-model="form.hasSound" type="checkbox" class="rounded" /> Sonido</label>
          <label class="flex items-center space-x-2"><input v-model="form.hasBalletBar" type="checkbox" class="rounded" /> Barra de Ballet</label>
          <label class="flex items-center space-x-2"><input v-model="form.hasAirConditioning" type="checkbox" class="rounded" /> Aire Acondicionado</label>
          <label class="flex items-center space-x-2"><input v-model="form.hasNaturalLight" type="checkbox" class="rounded" /> Luz Natural</label>
        </div>
      </div>
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Iluminacion</label>
          <input v-model="form.lighting" class="w-full px-3 py-2 border rounded-lg" placeholder="LED, natural, spotlights" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Color de Paredes</label>
          <input v-model="form.wallColor" class="w-full px-3 py-2 border rounded-lg" />
        </div>
      </div>
      <button type="submit" :disabled="isSubmitting" class="w-full py-3 bg-indigo-600 text-white rounded-lg font-semibold hover:bg-indigo-700 disabled:opacity-50">
        {{ isSubmitting ? 'Registrando...' : 'Registrar Sala' }}
      </button>
    </form>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { venueService } from '../services/venueService'

const venues = ref([])
const isSubmitting = ref(false)
const form = ref({
  venueId: '', name: '', capacity: 10, floorType: '',
  hasMirrors: false, hasSound: false, hasBalletBar: false,
  hasAirConditioning: false, hasNaturalLight: false,
  lighting: '', wallColor: '', imageUrl: ''
})

onMounted(async () => {
  try { venues.value = await venueService.list() } catch (e) { console.error(e) }
})

const submitRoom = async () => {
  isSubmitting.value = true
  try {
    await venueService.createRoom(form.value)
    alert('Sala registrada exitosamente')
  } catch (e) {
    alert('Error al registrar')
  } finally {
    isSubmitting.value = false
  }
}
</script>