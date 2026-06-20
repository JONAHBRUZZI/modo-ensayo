<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <div class="flex items-center justify-between mb-8 flex-wrap gap-4">
      <h1 class="text-3xl font-bold text-white">Mi Calendario</h1>
      <div class="flex gap-2">
        <button @click="vista = 'calendario'" :class="['btn-primary text-sm', vista === 'calendario' ? '' : '!bg-transparent border border-white/10 text-gray-300 hover:bg-white/5']">
          Calendario
        </button>
        <button @click="vista = 'lista'" :class="['btn-primary text-sm', vista === 'lista' ? '' : '!bg-transparent border border-white/10 text-gray-300 hover:bg-white/5']">
          Lista
        </button>
      </div>
    </div>

    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando clases...</div>
    <div v-else-if="classes.length === 0" class="card text-center py-12">
      <p class="text-gray-400">No tienes clases asignadas.</p>
    </div>

    <!-- Vista Calendario -->
    <template v-else-if="vista === 'calendario'">
      <div class="card">
        <div class="flex items-center justify-between mb-6">
          <button @click="prevWeek" class="text-sm text-gray-400 hover:text-white px-3 py-1 rounded hover:bg-dark-border">← Anterior</button>
          <h3 class="text-white font-medium">{{ weekLabel }}</h3>
          <button @click="nextWeek" class="text-sm text-gray-400 hover:text-white px-3 py-1 rounded hover:bg-dark-border">Siguiente →</button>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full border-collapse">
            <thead>
              <tr>
                <th class="w-24 p-2 text-left text-xs text-gray-500 font-medium"></th>
                <th v-for="day in weekDays" :key="day.date" class="p-2 text-center text-xs font-medium min-w-[130px]" :class="day.enabled ? 'text-gray-300' : 'text-gray-600'">
                  {{ day.label }}<br /><span class="font-normal opacity-60">{{ day.dateFormatted }}</span>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="block in timeBlocks" :key="block.key">
                <td class="p-2 text-xs text-gray-500 align-top whitespace-nowrap">{{ block.label }}</td>
                <td v-for="day in weekDays" :key="day.date" class="p-1 align-top" :class="day.enabled ? '' : 'bg-dark-bg/30'">
                  <div v-for="c in getClassesForCell(day.date, block)" :key="c.id" class="bg-primary/20 border border-primary/30 rounded p-1.5 mb-0.5 cursor-pointer hover:bg-primary/30 transition-colors" @click="selectClass(c)">
                    <p class="text-white text-[11px] font-medium truncate">{{ c.title }}</p>
                    <p class="text-gray-400 text-[10px] truncate">{{ c.roomName || c.room?.name }}</p>
                    <p v-if="c.enrollmentCount != null" class="text-primary text-[10px]">{{ c.enrollmentCount }} alumnos</p>
                  </div>
                  <div v-if="!getClassesForCell(day.date, block).length" class="min-h-[36px]"></div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- Vista Lista -->
    <div v-else class="space-y-4">
      <div v-for="c in sortedClasses" :key="c.id" class="card cursor-pointer hover:bg-[var(--bg-elevated)]" @click="selectClass(c)">
        <div class="flex items-center justify-between">
          <div>
            <h3 class="text-lg font-semibold text-white">{{ c.title }}</h3>
            <p class="text-gray-400 text-sm">{{ formatDate(c.startTime) }} · {{ formatTime(c.startTime) }} - {{ formatTime(c.endTime) }}</p>
            <p class="text-gray-500 text-xs mt-0.5">{{ c.roomName || c.room?.name }} · {{ c.venueName || '' }}</p>
            <p v-if="c.enrollmentCount != null" class="text-primary text-xs mt-0.5">{{ c.enrollmentCount }} alumnos inscritos</p>
          </div>
          <svg class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
        </div>
      </div>
    </div>

    <!-- Modal accion rapida -->
    <div v-if="selectedClass" class="fixed inset-0 bg-black/60 flex items-center justify-center z-50" @click.self="selectedClass = null">
      <div class="bg-[var(--bg-elevated)] rounded-2xl border border-white/10 p-6 max-w-md w-full mx-4">
        <h3 class="text-lg font-semibold text-white mb-2">{{ selectedClass.title }}</h3>
        <div class="text-gray-400 text-sm space-y-2 mb-6">
          <p><span class="text-gray-500">Sala:</span> {{ selectedClass.roomName || selectedClass.room?.name }}</p>
          <p><span class="text-gray-500">Fecha:</span> {{ formatDate(selectedClass.startTime) }}</p>
          <p><span class="text-gray-500">Horario:</span> {{ formatTime(selectedClass.startTime) }} - {{ formatTime(selectedClass.endTime) }}</p>
          <p v-if="selectedClass.enrollmentCount != null"><span class="text-gray-500">Alumnos:</span> {{ selectedClass.enrollmentCount }}</p>
        </div>
        <div class="flex gap-3">
          <router-link :to="'/profesor/asistencia/' + selectedClass.id" class="flex-1 text-center px-4 py-2 rounded-xl bg-primary text-white text-sm font-medium hover:bg-primary/80">Marcar asistencia</router-link>
          <router-link :to="'/alumno/clases/' + selectedClass.id" class="flex-1 text-center px-4 py-2 rounded-xl border border-white/10 text-gray-300 text-sm hover:bg-white/5">Ver alumnos</router-link>
        </div>
        <div class="mt-3 text-right">
          <button @click="selectedClass = null" class="text-xs text-gray-500 hover:text-gray-300">Cerrar</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import classService from '@/services/classService'
import { formatDate, formatTime } from '@/utils/dateFormatter'

const vista = ref('calendario')
const loading = ref(true)
const classes = ref([])
const currentWeekStart = ref(getMonday(new Date()))
const selectedClass = ref(null)

function getMonday(d) {
  const date = new Date(d)
  const day = date.getDay() || 7
  date.setDate(date.getDate() - day + 1)
  date.setHours(0, 0, 0, 0)
  return date
}

const weekDays = computed(() => {
  const result = []
  const monday = new Date(currentWeekStart.value)
  const dowLabels = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']
  for (let i = 0; i < 7; i++) {
    const d = new Date(monday)
    d.setDate(monday.getDate() + i)
    const dateStr = d.toISOString().slice(0, 10)
    const hasClasses = classes.value.some(c => c.startTime?.slice(0, 10) === dateStr)
    result.push({
      date: dateStr,
      dateFormatted: d.toLocaleDateString('es-CL', { day: 'numeric', month: 'short' }),
      label: dowLabels[i],
      enabled: hasClasses || (i < 5)
    })
  }
  return result
})

const weekLabel = computed(() => {
  const monday = new Date(currentWeekStart.value)
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)
  return `Semana del ${monday.toLocaleDateString('es-CL', { day: 'numeric', month: 'long' })} al ${sunday.toLocaleDateString('es-CL', { day: 'numeric', month: 'long' })}`
})

const timeBlocks = computed(() => {
  const blocks = []
  for (let h = 7; h <= 22; h++) {
    const start = `${String(h).padStart(2, '0')}:00`
    blocks.push({ key: start, label: start, start })
  }
  return blocks
})

function getClassesForCell(dateStr, block) {
  return classes.value.filter(c => {
    const classDate = c.startTime?.slice(0, 10)
    const classHour = c.startTime?.slice(11, 13) + ':00'
    return classDate === dateStr && classHour === block.start
  })
}

const sortedClasses = computed(() => {
  return [...classes.value].sort((a, b) => new Date(a.startTime) - new Date(b.startTime))
})

function selectClass(c) {
  selectedClass.value = c
}

function prevWeek() {
  const d = new Date(currentWeekStart.value)
  d.setDate(d.getDate() - 7)
  currentWeekStart.value = d
}

function nextWeek() {
  const d = new Date(currentWeekStart.value)
  d.setDate(d.getDate() + 7)
  currentWeekStart.value = d
}

onMounted(async () => {
  try {
    const data = await classService.getTeacherClasses()
    classes.value = Array.isArray(data) ? data : data?.content || []
  } catch {
    classes.value = []
  }
  loading.value = false
})
</script>
