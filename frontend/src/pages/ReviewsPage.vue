<template>
  <div class="mx-auto max-w-4xl space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">Evaluaciones pendientes</h1>
      <p class="text-sm text-gray-600">Solo puedes evaluar clases ya realizadas.</p>
    </div>

    <div v-if="loading" class="rounded-xl border bg-white p-6 text-sm text-gray-500">Cargando evaluaciones...</div>
    <div v-else-if="error" class="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{{ error }}</div>
    <div v-else-if="items.length === 0" class="rounded-xl border bg-white p-6 text-sm text-gray-500">No tienes evaluaciones pendientes.</div>

    <div v-else class="space-y-3">
      <div v-for="item in items" :key="item.classId + item.targetType + item.targetId" class="rounded-xl border bg-white p-4">
        <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <p class="font-semibold text-gray-900">{{ item.classTitle }}</p>
            <p class="text-sm text-gray-600">Objetivo: {{ targetLabel(item) }}</p>
          </div>
          <div class="flex flex-wrap gap-2">
            <button v-for="n in 5" :key="n" @click="setScore(itemKey(item), n)" :class="scoreClass(itemKey(item), n)">{{ n }}</button>
          </div>
        </div>
        <textarea v-model="comments[itemKey(item)]" rows="2" placeholder="Comentario (opcional)" class="mt-3 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm" />
        <div class="mt-3 flex flex-col gap-2 sm:flex-row">
          <button @click="submit(item)" :disabled="saving[itemKey(item)] || !scores[itemKey(item)]" class="w-full rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50 sm:w-auto">
            {{ saving[itemKey(item)] ? 'Enviando...' : 'Enviar evaluacion' }}
          </button>
          <p v-if="success[itemKey(item)]" class="self-center text-sm text-emerald-700">Evaluacion enviada.</p>
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
  await loadItems()
})

const loadItems = async () => {
  loading.value = true
  error.value = ''
  try {
    const user = JSON.parse(localStorage.getItem('user') || 'null')
    const isTeacher = user?.roles?.includes('TEACHER')
    const { data } = isTeacher
      ? await reviewService.getTeacherEligible()
      : await reviewService.getStudentEligible()
    items.value = data
  } catch (e) {
    error.value = 'No se pudieron cargar las evaluaciones pendientes.'
  } finally {
    loading.value = false
  }
}

const itemKey = (item) => `${item.classId}-${item.targetType}-${item.targetId}`
const setScore = (key, value) => { scores.value[key] = value }
const scoreClass = (key, n) => (
  scores.value[key] === n
    ? 'h-8 w-8 rounded-full bg-indigo-600 text-sm text-white'
    : 'h-8 w-8 rounded-full bg-gray-100 text-sm text-gray-700 hover:bg-gray-200'
)

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
      classId: item.classId,
      targetId: item.targetId,
      score: scores.value[key],
      comment: comments.value[key] || null,
    }

    if (item.targetType === 'CLASS') {
      await reviewService.createStudentClassReview(payload)
    } else if (item.targetType === 'VENUE') {
      await reviewService.createTeacherVenueReview(payload)
    } else {
      await reviewService.createTeacherStudentReview(payload)
    }

    success.value[key] = true
    items.value = items.value.filter((i) => itemKey(i) !== key)
  } catch (e) {
    error.value = e?.response?.data?.message || 'No se pudo enviar la evaluacion.'
  } finally {
    saving.value[key] = false
  }
}
</script>
