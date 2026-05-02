<script setup>
import { ref, onMounted, computed } from 'vue'
import rescheduleService from '../../services/rescheduleService'

const props = defineProps({
  reschedule: { type: Object, required: true },
})

const emit = defineEmits(['updated'])

const loading = ref(false)
const error = ref(null)
const userDecision = ref(null)
const deadlineReached = computed(() => {
  if (!props.reschedule.responseDeadline) return false
  return new Date(props.reschedule.responseDeadline) < new Date()
})
const timeLeft = ref('')

function updateTimeLeft() {
  if (!props.reschedule.responseDeadline) return
  const now = new Date()
  const deadline = new Date(props.reschedule.responseDeadline)
  const diff = deadline - now
  if (diff <= 0) {
    timeLeft.value = 'Plazo vencido'
    return
  }
  const hours = Math.floor(diff / 3600000)
  const minutes = Math.floor((diff % 3600000) / 60000)
  timeLeft.value = `${hours}h ${minutes}m restantes`
}

onMounted(() => {
  updateTimeLeft()
  const interval = setInterval(updateTimeLeft, 60000)
  return () => clearInterval(interval)
})

async function decide(accepted) {
  loading.value = true
  error.value = null
  try {
    const { data } = await rescheduleService.studentDecision(props.reschedule.id, accepted)
    userDecision.value = accepted ? 'ACCEPTED' : 'REJECTED'
    emit('updated', data)
  } catch (e) {
    error.value = e?.response?.data?.message || 'Error al enviar decision'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="p-4 border rounded-lg bg-white shadow-sm">
    <div class="flex justify-between items-start mb-3">
      <div>
        <h3 class="font-semibold">{{ reschedule.classTitle }}</h3>
        <p class="text-sm text-gray-500">
          Nueva fecha: {{ new Date(reschedule.proposedTime).toLocaleString() }}
        </p>
        <p v-if="reschedule.reason" class="text-sm text-gray-400 italic">
          Motivo: {{ reschedule.reason }}
        </p>
      </div>
      <span
        class="text-xs px-2 py-1 rounded"
        :class="{
          'bg-yellow-100 text-yellow-700': reschedule.status === 'TEACHER_ACCEPTED',
          'bg-green-100 text-green-700': reschedule.status === 'COMPLETED',
          'bg-red-100 text-red-700': reschedule.status === 'TEACHER_REJECTED',
        }"
      >
        {{ reschedule.status === 'TEACHER_ACCEPTED' ? 'Pendiente' : reschedule.status }}
      </span>
    </div>

    <div v-if="error" class="mb-2 p-2 bg-red-100 text-red-700 rounded text-sm">{{ error }}</div>

    <div
      v-if="reschedule.status === 'TEACHER_ACCEPTED' && !userDecision"
      class="bg-blue-50 p-2 rounded mb-3 text-sm text-blue-700"
    >
      {{ timeLeft }}
    </div>

    <div
      v-if="reschedule.status === 'TEACHER_ACCEPTED' && !userDecision && !deadlineReached"
      class="flex gap-2"
    >
      <button
        @click="decide(true)"
        :disabled="loading"
        class="flex-1 py-2 px-4 bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50 text-sm"
      >
        {{ loading ? '...' : 'Confirmar Asistencia' }}
      </button>
      <button
        @click="decide(false)"
        :disabled="loading"
        class="flex-1 py-2 px-4 bg-red-600 text-white rounded hover:bg-red-700 disabled:opacity-50 text-sm"
      >
        {{ loading ? '...' : 'Rechazar (Devolucion)' }}
      </button>
    </div>

    <div v-if="userDecision === 'ACCEPTED'" class="text-green-700 font-medium text-sm">
      Has confirmado tu asistencia
    </div>
    <div v-if="userDecision === 'REJECTED'" class="text-red-700 font-medium text-sm">
      Has rechazado. Se procesara tu devolucion.
    </div>

    <!-- Student responses summary -->
    <div v-if="reschedule.responses?.length" class="mt-3 pt-3 border-t">
      <p class="text-xs font-medium text-gray-500 mb-2">Respuestas:</p>
      <div
        v-for="r in reschedule.responses"
        :key="r.id"
        class="flex justify-between text-xs py-1"
      >
        <span>{{ r.userFullName || r.userEmail || r.userId }}</span>
        <span
          :class="{
            'text-green-600': r.responseType === 'ACCEPTED',
            'text-red-600': r.responseType === 'REJECTED',
            'text-yellow-600': r.responseType === 'TIMEOUT',
            'text-gray-400': !r.responseType,
          }"
        >
          {{ r.responseType || 'Pendiente' }}
        </span>
      </div>
    </div>
  </div>
</template>
