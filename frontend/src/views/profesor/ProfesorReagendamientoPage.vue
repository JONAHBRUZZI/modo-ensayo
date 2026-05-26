<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Reagendar Clase</h1>
    <form @submit.prevent="submit" class="card space-y-4">
      <p class="text-gray-400">Clase ID: {{ $route.params.claseId }}</p>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Nueva Fecha y Hora</label><input type="datetime-local" v-model="newDate" required class="input-field" /></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Motivo</label><textarea v-model="reason" rows="3" required class="input-field" placeholder="Explica el motivo del reagendamiento"></textarea></div>
      <p v-if="msg" class="text-green-400 text-sm">{{ msg }}</p>
      <button type="submit" :disabled="sending" class="btn-primary w-full">{{ sending ? 'Enviando...' : 'Solicitar Reagendamiento' }}</button>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import rescheduleService from '@/services/rescheduleService'

const route = useRoute()
const newDate = ref('')
const reason = ref('')
const sending = ref(false)
const msg = ref('')

async function submit() {
  sending.value = true
  try {
    await rescheduleService.propose(route.params.claseId, new Date(newDate.value).toISOString(), reason.value)
    msg.value = 'Reagendamiento solicitado correctamente. Los alumnos seran notificados.'
  } catch (e) {
    msg.value = e.response?.data?.message || 'Error al solicitar reagendamiento'
  }
  sending.value = false
}
</script>
