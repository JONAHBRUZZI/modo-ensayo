<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <div class="flex items-center justify-between mb-8 flex-wrap gap-4">
      <h1 class="text-3xl font-bold text-white">Calendario</h1>
      <router-link to="/alumno/mis-clases" class="text-sm text-gray-400 hover:text-white">&larr; Volver a Mis Clases</router-link>
    </div>

    <!-- Filtro: radio buttons -->
    <div class="card mb-6">
      <div class="flex flex-wrap items-center gap-6">
        <label v-for="f in filtros" :key="f.key" class="flex items-center gap-2 cursor-pointer">
          <input type="radio" v-model="filtroActivo" :value="f.key" class="w-4 h-4 text-primary focus:ring-primary/50" />
          <span class="text-sm text-gray-300">{{ f.label }}</span>
        </label>

        <!-- Dropdown de asociado -->
        <div v-if="filtroActivo === 'por-asociado'" class="flex items-center gap-2">
          <span class="text-xs text-gray-500">Asociado:</span>
          <select v-model="associateSelected" class="input-field text-sm py-1.5 w-48">
            <option value="">Todos</option>
            <option v-for="a in associates" :key="a.id" :value="a.id">{{ a.name || a.email }}</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Navegador mensual -->
    <div class="card mb-6">
      <div class="flex items-center justify-between">
        <button @click="prevMonth" class="text-sm text-gray-400 hover:text-white px-3 py-1 rounded hover:bg-dark-border">&larr; Anterior</button>
        <h2 class="text-lg font-semibold text-white">{{ monthLabel }}</h2>
        <button @click="nextMonth" class="text-sm text-gray-400 hover:text-white px-3 py-1 rounded hover:bg-dark-border">Siguiente &rarr;</button>
      </div>
    </div>

    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando clases...</div>

    <div v-else class="card overflow-x-auto">
      <table class="w-full border-collapse">
        <thead>
          <tr>
            <th v-for="d in dayHeaders" :key="d" class="p-2 text-center text-xs font-medium text-gray-400 border-b border-dark-border">{{ d }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(week, wi) in monthWeeks" :key="wi">
            <td
              v-for="(day, di) in week"
              :key="di"
              :class="['p-1 align-top border border-dark-border min-w-[100px]', day.isCurrentMonth ? 'bg-[var(--bg-base)]' : 'bg-dark-bg/30']"
              @click="day.isCurrentMonth && day.classes.length && expandDay(day)"
            >
              <div class="min-h-[70px]">
                <span :class="['text-xs mb-1 block', day.isCurrentMonth ? 'text-gray-300 font-medium' : 'text-gray-600']">{{ day.dayNumber }}</span>
                <div v-if="day.classes.length" class="flex flex-wrap gap-1">
                  <span v-if="day.propiasCount > 0" class="w-2 h-2 rounded-full bg-purple-500 inline-block" title="Mis clases"></span>
                  <span v-if="day.asociadasCount > 0" class="w-2 h-2 rounded-full bg-orange-500 inline-block" title="Clases de asociados"></span>
                </div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Leyenda -->
      <div class="flex items-center gap-6 mt-4 pt-4 border-t border-dark-border flex-wrap">
        <span class="flex items-center gap-1.5 text-xs text-gray-400">
          <span class="w-2.5 h-2.5 rounded-full bg-purple-500"></span> Mis clases
        </span>
        <span class="flex items-center gap-1.5 text-xs text-gray-400">
          <span class="w-2.5 h-2.5 rounded-full bg-orange-500"></span> Clases de asociados
        </span>
      </div>
    </div>

    <!-- Modal dia expandido -->
    <div v-if="selectedDay" class="fixed inset-0 bg-black/60 flex items-center justify-center z-50" @click.self="selectedDay = null">
      <div class="bg-[var(--bg-elevated)] rounded-2xl border border-white/10 p-6 max-w-lg w-full mx-4 max-h-[80vh] overflow-y-auto">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold text-white">{{ formatDateFull(selectedDay.date) }}</h3>
          <button @click="selectedDay = null" class="text-gray-400 hover:text-white">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
          </button>
        </div>
        <div v-if="dayDetail.length === 0" class="text-gray-400 text-sm py-4">Sin clases este dia.</div>
        <div v-else class="space-y-3">
          <div v-for="c in dayDetail" :key="c.classId || c.id" :class="['rounded-xl p-4 border', c.isOwner ? 'border-purple-500/30 bg-purple-500/5' : 'border-orange-500/30 bg-orange-500/5']">
            <div class="flex items-start justify-between">
              <div>
                <h4 class="text-white font-medium">{{ c.title }}</h4>
                <p class="text-gray-400 text-sm mt-0.5">{{ c.discipline }} {{ c.level ? '-- ' + c.level : '' }}</p>
                <p class="text-gray-500 text-xs mt-1">{{ formatTime(c.startTime) }} - {{ formatTime(c.endTime) }}</p>
                <p v-if="c.venueName" class="text-gray-500 text-xs">{{ c.venueName }}</p>
                <span :class="['text-xs px-2 py-0.5 rounded-full mt-1.5 inline-block', c.isOwner ? 'bg-purple-500/20 text-purple-300' : 'bg-orange-500/20 text-orange-300']">
                  {{ c.isOwner ? 'Mi clase' : 'Asociado' }}
                </span>
              </div>
              <router-link :to="'/alumno/clases/' + (c.classId || c.id)" class="text-primary text-sm hover:underline whitespace-nowrap ml-4">Ver detalle</router-link>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import scheduleService from '@/services/scheduleService'
import associateService from '@/services/associateService'
import { formatTime } from '@/utils/dateFormatter'

const loading = ref(true)
const enrollments = ref([])
const currentMonth = ref(new Date().getMonth())
const currentYear = ref(new Date().getFullYear())
const selectedDay = ref(null)

const filtroActivo = ref('mis-clases')
const filtros = [
  { key: 'mis-clases', label: 'Mis clases' },
  { key: 'por-asociado', label: 'Por asociado' },
  { key: 'mixta', label: 'Vista mixta' }
]

const associates = ref([])
const associateSelected = ref('')

const dayHeaders = ['Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab', 'Dom']

const monthLabel = computed(() => {
  const d = new Date(currentYear.value, currentMonth.value)
  return d.toLocaleDateString('es-CL', { month: 'long', year: 'numeric' })
})

const filteredEnrollments = computed(() => {
  if (filtroActivo.value === 'mis-clases') {
    return enrollments.value.filter(e => e.isOwner !== false)
  }
  if (filtroActivo.value === 'por-asociado') {
    let list = enrollments.value.filter(e => e.isOwner === false)
    if (associateSelected.value) {
      list = list.filter(e => e.beneficiaryId == associateSelected.value)
    }
    return list
  }
  // Vista mixta: todas
  return enrollments.value
})

const monthWeeks = computed(() => {
  const year = currentYear.value
  const month = currentMonth.value
  const firstDay = new Date(year, month, 1)
  let startDay = firstDay.getDay() || 7
  startDay -= 1

  const lastDate = new Date(year, month + 1, 0).getDate()

  const weeks = []
  let day = 1
  let currentWeek = []

  for (let i = 0; i < startDay; i++) {
    currentWeek.push({ dayNumber: '', isCurrentMonth: false, classes: [], propiasCount: 0, asociadasCount: 0, date: null })
  }

  while (day <= lastDate) {
    const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    const dayClasses = getClassesForDate(dateStr)
    const propias = dayClasses.filter(c => c.isOwner).length
    const asociadas = dayClasses.filter(c => !c.isOwner).length

    currentWeek.push({
      dayNumber: day,
      isCurrentMonth: true,
      classes: dayClasses,
      propiasCount: propias,
      asociadasCount: asociadas,
      date: dateStr
    })

    if (currentWeek.length === 7) {
      weeks.push(currentWeek)
      currentWeek = []
    }
    day++
  }

  if (currentWeek.length > 0) {
    while (currentWeek.length < 7) {
      currentWeek.push({ dayNumber: '', isCurrentMonth: false, classes: [], propiasCount: 0, asociadasCount: 0, date: null })
    }
    weeks.push(currentWeek)
  }

  return weeks
})

function getClassesForDate(dateStr) {
  return filteredEnrollments.value.filter(e => {
    if (!e.startTime) return false
    return e.startTime.slice(0, 10) === dateStr
  })
}

const dayDetail = computed(() => {
  if (!selectedDay.value) return []
  return getClassesForDate(selectedDay.value.date)
})

function expandDay(day) {
  selectedDay.value = day
}

function prevMonth() {
  if (currentMonth.value === 0) {
    currentMonth.value = 11
    currentYear.value--
  } else {
    currentMonth.value--
  }
  loadCalendar()
}

function nextMonth() {
  if (currentMonth.value === 11) {
    currentMonth.value = 0
    currentYear.value++
  } else {
    currentMonth.value++
  }
  loadCalendar()
}

function formatDateFull(dateStr) {
  if (!dateStr) return ''
  const [y, m, d] = dateStr.split('-')
  return new Date(+y, +m - 1, +d).toLocaleDateString('es-CL', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
}

async function loadCalendar() {
  loading.value = true
  try {
    // Traemos todas las inscripciones; el calendario filtra por mes/dia en el cliente.
    const data = await scheduleService.getUserCalendar()
    enrollments.value = Array.isArray(data) ? data : data?.content || []
  } catch {
    enrollments.value = []
  }
  loading.value = false
}

onMounted(async () => {
  try {
    const data = await associateService.getAssociates()
    associates.value = Array.isArray(data) ? data : []
  } catch {
    associates.value = []
  }
  await loadCalendar()
})

watch([currentMonth, currentYear], () => {
  loadCalendar()
})
</script>