<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-2">Pasar lista</h1>
    <p class="text-gray-500 text-sm mb-8">
      Todos parten como <span class="text-green-400">Presente</span>. Desmarca solo a quienes faltaron y guarda.
    </p>

    <div v-if="loading" class="text-center text-gray-500 py-16">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>

    <div v-else-if="students.length === 0" class="card text-center py-12">
      <p class="text-gray-400">No hay alumnos inscritos en esta clase.</p>
    </div>

    <template v-else>
      <!-- Resumen + acción rápida -->
      <div class="flex items-center justify-between mb-4 flex-wrap gap-2">
        <p class="text-sm text-gray-400">
          <span class="text-green-400 font-medium">{{ presentesCount }}</span> presentes ·
          <span class="text-red-400 font-medium">{{ students.length - presentesCount }}</span> ausentes
          <span class="text-gray-600">de {{ students.length }}</span>
        </p>
        <button @click="marcarTodosPresentes" class="text-xs text-primary hover:underline">Marcar todos presentes</button>
      </div>

      <div class="space-y-3">
        <label
          v-for="s in students"
          :key="s.enrollmentId"
          class="card flex items-center justify-between cursor-pointer transition-colors"
          :class="s.present ? '' : 'border-red-500/30 bg-red-500/5'"
        >
          <div class="min-w-0">
            <p class="text-white font-medium truncate">{{ s.attendeeName || 'Alumno' }}</p>
            <p class="text-gray-500 text-sm truncate">
              {{ s.studentEmail }}<span v-if="s.beneficiaryType && s.beneficiaryType !== 'SELF'"> · {{ s.beneficiaryType }}</span>
            </p>
          </div>
          <div class="flex items-center gap-2 flex-shrink-0">
            <span class="text-sm" :class="s.present ? 'text-green-400' : 'text-red-400'">
              {{ s.present ? 'Presente' : 'Ausente' }}
            </span>
            <input type="checkbox" v-model="s.present" class="w-5 h-5 rounded accent-primary cursor-pointer" />
          </div>
        </label>
      </div>

      <div class="mt-8 flex justify-end">
        <button @click="guardar" :disabled="guardando" class="btn-primary disabled:opacity-50">
          {{ guardando ? 'Guardando...' : 'Guardar asistencia' }}
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import classService from '@/services/classService'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const students = ref([])
const loading = ref(true)
const guardando = ref(false)

const presentesCount = computed(() => students.value.filter((s) => s.present).length)

onMounted(async () => {
  try {
    const data = await classService.getClassStudents(route.params.classId)
    students.value = (data || [])
      .filter((s) => s.status === 'ACTIVE')
      // Default: presente. Solo si ya fue marcado ausente antes se respeta (false).
      .map((s) => ({ ...s, present: s.present === false ? false : true }))
  } catch {
    students.value = []
  }
  loading.value = false
})

function marcarTodosPresentes() {
  students.value.forEach((s) => { s.present = true })
}

async function guardar() {
  guardando.value = true
  try {
    const marks = students.value.map((s) => ({ enrollmentId: s.enrollmentId, present: s.present }))
    await classService.saveAttendance(route.params.classId, marks)
    toast.success('Asistencia guardada.')
    router.back()
  } catch (e) {
    toast.error(e?.response?.data?.error || e?.message || 'No se pudo guardar la asistencia.')
  } finally {
    guardando.value = false
  }
}
</script>
