<template>
  <div class="mx-auto max-w-3xl space-y-6">
    <div>
      <router-link to="/teacher/classes" class="text-sm text-indigo-600 hover:underline">&larr; Volver a Mis Clases</router-link>
      <h1 class="mt-2 text-2xl font-bold text-gray-900">Marcar Asistencia</h1>
      <p v-if="classInfo" class="text-sm text-gray-600">{{ classInfo.title }} - {{ classInfo.discipline }}</p>
    </div>

    <div v-if="loading" class="rounded-xl border bg-white p-6 text-sm text-gray-500">Cargando...</div>

    <div v-else class="rounded-xl border bg-white p-6">
      <p v-if="enrollments.length === 0" class="text-sm text-gray-500">No hay alumnos inscritos en esta clase.</p>

      <div v-else>
        <div class="mb-4 flex items-center justify-between">
          <p class="text-sm font-medium text-gray-700">{{ enrollments.length }} alumnos inscritos</p>
          <button @click="selectAll" class="text-sm text-indigo-600 hover:text-indigo-700">Seleccionar todos</button>
        </div>

        <ul class="space-y-2">
          <li v-for="enrollment in enrollments" :key="enrollment.id" class="flex items-center justify-between rounded-lg border bg-gray-50 p-3">
            <div>
              <p class="font-medium text-gray-900">{{ enrollment.beneficiaryName || enrollment.beneficiaryId }}</p>
              <p class="text-xs text-gray-500">{{ enrollment.beneficiaryType === 'USER' ? 'Titular' : 'Asociado' }}</p>
            </div>
            <label class="flex cursor-pointer items-center gap-2">
              <input type="checkbox" :checked="attendanceMap[enrollment.beneficiaryId]" @change="toggleAttendance(enrollment.beneficiaryId)" class="h-4 w-4 rounded border-gray-300 text-indigo-600" />
              <span class="text-sm text-gray-700">Presente</span>
            </label>
          </li>
        </ul>

        <button @click="submitAttendance" :disabled="submitting" class="mt-4 w-full rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50">
          {{ submitting ? 'Guardando...' : 'Guardar Asistencia' }}
        </button>
      </div>
    </div>

    <div v-if="successMessage" class="fixed bottom-4 right-4 rounded-lg bg-emerald-600 px-4 py-3 text-sm text-white shadow-lg">
      {{ successMessage }}
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { classService } from '../services/classService'
import api from '../services/api'

const route = useRoute()
const router = useRouter()
const classId = route.params.classId

const loading = ref(true)
const submitting = ref(false)
const classInfo = ref(null)
const enrollments = ref([])
const attendanceMap = ref({})
const successMessage = ref('')

onMounted(async () => {
  await loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    const myClasses = await classService.getMyClasses()
    classInfo.value = myClasses.find(c => c.id === classId)

    const res = await api.get(`/payments/enrollments/class/${classId}`)
    enrollments.value = res.data

    for (const e of enrollments.value) {
      attendanceMap.value[e.beneficiaryId] = true
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const toggleAttendance = (beneficiaryId) => {
  attendanceMap.value[beneficiaryId] = !attendanceMap.value[beneficiaryId]
}

const selectAll = () => {
  for (const e of enrollments.value) {
    attendanceMap.value[e.beneficiaryId] = true
  }
}

const submitAttendance = async () => {
  submitting.value = true
  try {
    const attendees = enrollments.value.map(e => ({
      beneficiaryId: e.beneficiaryId,
      beneficiaryType: e.beneficiaryType || 'USER',
      present: !!attendanceMap.value[e.beneficiaryId]
    }))

    await classService.markAttendance({
      classId,
      markedBy: classId,
      attendees
    })

    successMessage.value = 'Asistencia guardada exitosamente!'
    setTimeout(() => { router.push('/teacher/classes') }, 1500)
  } catch (e) {
    alert(e.response?.data?.message || 'Error al guardar asistencia.')
  } finally {
    submitting.value = false
  }
}
</script>
