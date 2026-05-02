<script setup>
import { ref } from 'vue'
import rescheduleService from '../../services/rescheduleService'

const props = defineProps({
  classItem: { type: Object, required: true },
})

const emit = defineEmits(['done'])

const proposedDate = ref('')
const reason = ref('')
const loading = ref(false)
const error = ref(null)
const success = ref(null)

async function handlePropose() {
  if (!proposedDate.value) return
  loading.value = true
  error.value = null
  try {
    const isoDate = new Date(proposedDate.value).toISOString()
    const { data } = await rescheduleService.propose(
      props.classItem.id,
      isoDate,
      reason.value || null
    )
    success.value = 'Reagendamiento propuesto. Revisa el estado para confirmar.'
    emit('done', data)
  } catch (e) {
    error.value = e?.response?.data?.message || 'Error al proponer reagendamiento'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="p-4 border rounded-lg bg-yellow-50">
    <h3 class="font-semibold mb-2">Reagendar Clase</h3>
    <p class="text-sm text-gray-600 mb-3">Clase: {{ classItem.title }}</p>

    <div v-if="error" class="mb-3 p-2 bg-red-100 text-red-700 rounded text-sm">{{ error }}</div>
    <div v-if="success" class="mb-3 p-2 bg-green-100 text-green-700 rounded text-sm">{{ success }}</div>

    <div class="mb-3">
      <label class="block text-sm font-medium mb-1">Nueva fecha y hora</label>
      <input
        v-model="proposedDate"
        type="datetime-local"
        class="w-full px-3 py-2 border rounded"
        :min="new Date().toISOString().slice(0, 16)"
      />
    </div>

    <div class="mb-3">
      <label class="block text-sm font-medium mb-1">Motivo (opcional)</label>
      <input
        v-model="reason"
        type="text"
        placeholder="Ej: Profesor enfermo, sala no disponible..."
        class="w-full px-3 py-2 border rounded"
      />
    </div>

    <button
      @click="handlePropose"
      :disabled="!proposedDate || loading"
      class="w-full py-2 px-4 bg-yellow-600 text-white rounded hover:bg-yellow-700 disabled:opacity-50"
    >
      {{ loading ? 'Enviando...' : 'Proponer Reagendamiento' }}
    </button>
  </div>
</template>
