<template>
  <div class="max-w-2xl mx-auto space-y-6">
    <div>
      <router-link to="/profesor/clases-propias" class="text-sm text-indigo-400 hover:text-indigo-300 transition-colors">
        ← Volver a Mis Clases
      </router-link>
      <h1 class="text-2xl font-bold text-white mt-2">Marcar Asistencia</h1>
      <p v-if="classInfo" class="text-sm text-gray-400 mt-0.5">{{ classInfo.title }} · {{ classInfo.discipline }}</p>
    </div>

    <div v-if="loading" class="bg-[#161824] rounded-xl border border-white/10 p-8">
      <div class="space-y-3">
        <div v-for="i in 4" :key="i" class="h-14 bg-white/5 rounded-lg animate-pulse"></div>
      </div>
    </div>

    <div v-else class="bg-[#161824] rounded-xl border border-white/10 overflow-hidden">
      <div class="px-5 py-4 border-b border-white/5 flex items-center justify-between">
        <p class="text-sm font-medium text-white">{{ enrollments.length }} alumnos inscritos</p>
        <button @click="selectAll" class="text-xs text-indigo-400 hover:text-indigo-300 transition-colors">
          Seleccionar todos
        </button>
      </div>

      <div v-if="enrollments.length === 0" class="px-5 py-10 text-center">
        <p class="text-sm text-gray-500">No hay alumnos inscritos en esta clase.</p>
      </div>

      <ul v-else class="divide-y divide-white/5">
        <li v-for="enrollment in enrollments" :key="enrollment.id"
          class="flex items-center justify-between px-5 py-3.5 hover:bg-white/3 transition-colors">
          <div class="flex items-center gap-3">
            <div class="w-8 h-8 bg-gradient-to-br from-indigo-600 to-purple-600 rounded-full flex items-center justify-center flex-shrink-0">
              <span class="text-xs font-bold text-white">
                {{ (enrollment.beneficiaryName || enrollment.beneficiaryId)?.charAt(0)?.toUpperCase() }}
              </span>
            </div>
            <div>
              <p class="text-sm font-medium text-white">{{ enrollment.beneficiaryName || enrollment.beneficiaryId }}</p>
              <p class="text-xs text-gray-500">{{ enrollment.beneficiaryType === 'USER' ? 'Titular' : 'Asociado' }}</p>
            </div>
          </div>
          <label class="flex items-center gap-2 cursor-pointer">
            <div class="relative">
              <input type="checkbox" :checked="attendanceMap[enrollment.beneficiaryId]"
                @change="toggleAttendance(enrollment.beneficiaryId)" class="sr-only" />
              <div :class="attendanceMap[enrollment.beneficiaryId] ? 'bg-emerald-600 border-emerald-600' : 'bg-[#0d0f1a] border-white/15'"
                class="w-5 h-5 rounded border-2 transition-colors flex items-center justify-center">
                <svg v-if="attendanceMap[enrollment.beneficiaryId]" class="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                </svg>
              </div>
            </div>
            <span class="text-xs text-gray-400">Presente</span>
          </label>
        </li>
      </ul>

      <div class="px-5 py-4 border-t border-white/5" v-if="enrollments.length > 0">
        <button @click="submitAttendance" :disabled="submitting"
          class="w-full py-2.5 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white rounded-lg text-sm font-semibold transition-colors">
          <span v-if="submitting" class="flex items-center justify-center gap-2">
            <svg class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
            </svg>
            Guardando...
          </span>
          <span v-else>Guardar Asistencia</span>
        </button>
      </div>
    </div>

    <!-- Toast -->
    <Transition enter-active-class="transition-all duration-300" enter-from-class="opacity-0 translate-y-2" leave-active-class="transition-all duration-300" leave-to-class="opacity-0 translate-y-2">
      <div v-if="successMessage" class="fixed bottom-6 right-6 bg-emerald-600 text-white text-sm font-medium px-4 py-3 rounded-xl shadow-lg z-50">
        {{ successMessage }}
      </div>
    </Transition>
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
  loading.value = true
  try {
    const myClasses = await classService.getMyClasses()
    classInfo.value = myClasses.find(c => String(c.id) === String(classId))
    const res = await api.get(`/payments/enrollments/class/${classId}`)
    enrollments.value = res.data
    for (const e of enrollments.value) {
      attendanceMap.value[e.beneficiaryId] = true
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
})

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
      present: !!attendanceMap.value[e.beneficiaryId],
    }))
    await classService.markAttendance({ classId, markedBy: classId, attendees })
    successMessage.value = 'Asistencia guardada exitosamente.'
    setTimeout(() => { router.push('/profesor/clases-propias') }, 1500)
  } catch (e) {
    alert(e.response?.data?.message || 'Error al guardar asistencia.')
  } finally {
    submitting.value = false
  }
}
</script>
