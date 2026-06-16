<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Asistencia - Clase #{{ $route.params.classId }}</h1>
    <div v-if="students.length === 0" class="card text-center py-12"><p class="text-gray-400">No hay alumnos inscritos.</p></div>
    <div v-else class="space-y-3">
      <div v-for="s in students" :key="s.id" class="card flex items-center justify-between">
        <div><p class="text-white font-medium">{{ s.name || s.fullName }}</p><p class="text-gray-400 text-sm">{{ s.email }}</p></div>
        <div class="flex items-center space-x-2">
          <label class="flex items-center space-x-2 cursor-pointer">
            <input type="checkbox" v-model="s.present" class="rounded text-primary focus:ring-primary" @change="markAttendance(s)" />
            <span class="text-sm text-gray-300">Presente</span>
          </label>
        </div>
      </div>
    </div>
    <p v-if="success" class="text-green-400 text-sm mt-4 text-center">{{ success }}</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import classService from '@/services/classService'

const route = useRoute()
const students = ref([])
const success = ref('')

onMounted(async () => {
  try {
    const data = await classService.getClassAttendance(route.params.classId)
    students.value = (data || []).map(s => ({ ...s, present: s.present || false }))
  } catch { students.value = [] }
})

async function markAttendance(student) {
  try {
    await classService.markAttendance(route.params.classId, { userId: student.userId, present: student.present })
    success.value = 'Asistencia actualizada'
    setTimeout(() => success.value = '', 2000)
  } catch (err) {
    console.error('Error al actualizar asistencia', err)
  }
}
</script>
