<template>
  <div class="max-w-3xl mx-auto space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-white">Evaluaciones Pendientes</h1>
      <p class="text-gray-400 text-sm mt-0.5">Solo puedes evaluar clases ya realizadas.</p>
    </div>

    <div v-if="loading" class="space-y-3">
      <div v-for="i in 3" :key="i" class="h-32 bg-[#161824] rounded-xl border border-white/10 animate-pulse"></div>
    </div>

    <div v-else-if="error" class="bg-red-500/10 border border-red-500/20 rounded-xl p-4">
      <p class="text-sm text-red-400">{{ error }}</p>
    </div>

    <div v-else-if="items.length === 0" class="text-center py-14 bg-[#161824] rounded-xl border border-white/10">
      <div class="inline-flex items-center justify-center w-14 h-14 bg-white/5 rounded-full border border-white/10 mb-4">
        <svg class="w-7 h-7 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
        </svg>
      </div>
      <p class="text-gray-400 text-sm">No tienes evaluaciones pendientes.</p>
    </div>

    <div v-else class="space-y-3">
      <div v-for="item in items" :key="itemKey(item)"
        class="bg-[#161824] rounded-xl border border-white/10 p-5 space-y-4">
        <div class="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3">
          <div>
            <p class="font-semibold text-white">{{ item.classTitle }}</p>
            <p class="text-xs text-gray-500 mt-0.5">{{ targetLabel(item) }}</p>
          </div>
          <!-- Estrellas -->
          <div class="flex gap-1.5">
            <button v-for="n in 5" :key="n" @click="setScore(itemKey(item), n)"
              :class="scores[itemKey(item)] >= n ? 'text-amber-400' : 'text-gray-600'"
              class="transition-colors hover:text-amber-300 text-xl leading-none">★</button>
          </div>
        </div>

        <textarea v-model="comments[itemKey(item)]" rows="2"
          placeholder="Comentario (opcional)"
          class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all resize-none" />

        <div class="flex items-center gap-3">
          <button @click="submit(item)" :disabled="saving[itemKey(item)] || !scores[itemKey(item)]"
            class="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white rounded-lg text-sm font-medium transition-colors">
            {{ saving[itemKey(item)] ? 'Enviando...' : 'Enviar evaluación' }}
          </button>
          <p v-if="success[itemKey(item)]" class="text-xs text-emerald-400">¡Evaluación enviada!</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { reviewService } from '../services/reviewService'

const loading = ref(true)
const error = ref('')
const items = ref([])
const scores = ref({})
const comments = ref({})
const saving = ref({})
const success = ref({})

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    const user = JSON.parse(localStorage.getItem('user') || 'null')
    const isTeacher = user?.roles?.includes('TEACHER')
    const { data } = isTeacher
      ? await reviewService.getTeacherEligible()
      : await reviewService.getStudentEligible()
    items.value = data
  } catch {
    error.value = 'No se pudieron cargar las evaluaciones pendientes.'
  } finally {
    loading.value = false
  }
})

const itemKey = (item) => `${item.classId}-${item.targetType}-${item.targetId}`
const setScore = (key, value) => { scores.value[key] = value }

const targetLabel = (item) => {
  if (item.targetType === 'VENUE') return `Sede: ${item.targetLabel}`
  if (item.targetType === 'STUDENT') return `Alumno: ${item.targetLabel}`
  return 'Clase'
}

const submit = async (item) => {
  const key = itemKey(item)
  success.value[key] = false
  saving.value[key] = true
  try {
    const payload = {
      classId: item.classId, targetId: item.targetId,
      score: scores.value[key], comment: comments.value[key] || null,
    }
    if (item.targetType === 'CLASS') await reviewService.createStudentClassReview(payload)
    else if (item.targetType === 'VENUE') await reviewService.createTeacherVenueReview(payload)
    else await reviewService.createTeacherStudentReview(payload)
    success.value[key] = true
    items.value = items.value.filter(i => itemKey(i) !== key)
  } catch (e) {
    error.value = e?.response?.data?.message || 'No se pudo enviar la evaluación.'
  } finally {
    saving.value[key] = false
  }
}
</script>
