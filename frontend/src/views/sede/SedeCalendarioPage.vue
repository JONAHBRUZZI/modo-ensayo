<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <div class="flex items-center justify-between mb-8 flex-wrap gap-4">
      <h1 class="text-3xl font-bold text-white">Agenda de Salas</h1>
      <router-link to="/sede/salas" class="text-sm text-gray-400 hover:text-white">← Volver a salas</router-link>
    </div>

    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>

    <!-- CTA: sin horario -->
    <div v-else-if="schedules.length === 0" class="card text-center py-16 mb-6">
      <div class="w-16 h-16 bg-yellow-500/10 rounded-2xl flex items-center justify-center mx-auto mb-4">
        <svg class="w-8 h-8 text-yellow-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
      </div>
      <h2 class="text-xl font-bold text-white mb-2">Sin horarios disponibles</h2>
      <p class="text-gray-400 text-sm mb-6 max-w-md mx-auto">
        Tu sede aún no tiene un horario laboral configurado. Defínelo para que el sistema
        genere automáticamente los bloques de disponibilidad de tus salas.
      </p>
      <button @click="showConfig = true" class="btn-primary text-base px-8 py-3">
        Crear horario laboral
      </button>
    </div>

    <template v-else>
      <!-- Config section (collapsible) -->
      <div class="card mb-6">
        <button
          @click="showConfig = !showConfig"
          class="flex items-center justify-between w-full text-left"
        >
          <h3 class="text-white font-medium">Horario laboral de la sede</h3>
          <svg
            :class="{ 'rotate-180': showConfig }"
            class="w-5 h-5 text-gray-400 transition-transform duration-200"
            fill="none" stroke="currentColor" viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
          </svg>
        </button>

        <div v-if="showConfig" class="mt-4 space-y-4">
          <div class="space-y-3">
            <div
              v-for="day in days"
              :key="day"
              class="flex items-center gap-4 flex-wrap"
            >
              <label class="flex items-center gap-2 w-28">
                <input
                  type="checkbox"
                  v-model="scheduleDays[day].enabled"
                  class="w-4 h-4 rounded border-dark-border bg-dark-bg text-primary focus:ring-primary/50"
                />
                <span class="text-sm text-gray-300">{{ dayLabels[day] }}</span>
              </label>
              <template v-if="scheduleDays[day].enabled">
                <input
                  type="time"
                  v-model="scheduleDays[day].openTime"
                  class="input-field w-32"
                />
                <span class="text-gray-400 text-sm">a</span>
                <input
                  type="time"
                  v-model="scheduleDays[day].closeTime"
                  class="input-field w-32"
                />
              </template>
            </div>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2 border-t border-dark-border">
            <div>
              <label class="block text-xs text-gray-400 mb-1">Duración del bloque (min)</label>
              <input
                type="number"
                v-model.number="blockCfg.duration"
                class="input-field"
                min="15"
                step="5"
              />
            </div>
            <div>
              <label class="block text-xs text-gray-400 mb-1">Brecha entre bloques (min)</label>
              <input
                type="number"
                v-model.number="blockCfg.gap"
                class="input-field"
                min="0"
                step="5"
              />
            </div>
          </div>

          <p v-if="configMsg" :class="configMsgType === 'error' ? 'text-red-400' : 'text-green-400'" class="text-sm">{{ configMsg }}</p>

          <button
            @click="confirmSaveConfig = true"
            :disabled="savingConfig"
            class="btn-primary text-sm"
          >
            {{ savingConfig ? 'Guardando...' : 'Guardar configuración' }}
          </button>
        </div>
      </div>

      <!-- Confirmation modal -->
      <div v-if="confirmSaveConfig" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
        <div class="card max-w-md w-full mx-4 space-y-4">
          <h4 class="text-lg font-semibold text-white">Confirmar cambios</h4>
          <p class="text-sm text-gray-400">
            ⚠️ Este cambio es TOTAL. Se regenerarán todos los bloques. Las clases en horarios que ya no existan serán afectadas. ¿Confirmas?
          </p>
          <div class="flex justify-end gap-3">
            <button @click="confirmSaveConfig = false" class="text-sm text-gray-400 hover:text-white">Cancelar</button>
            <button @click="saveAllConfig" class="btn-primary text-sm">Confirmar</button>
          </div>
        </div>
      </div>

      <!-- Maintenance confirmation modal -->
      <div v-if="maintenanceConfirm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
        <div class="card max-w-md w-full mx-4 space-y-4">
          <h4 class="text-lg font-semibold text-white">{{ maintenanceConfirm.action === 'mark' ? 'Marcar mantención' : 'Liberar mantención' }}</h4>
          <p class="text-sm text-gray-400">{{ maintenanceConfirm.message }}</p>
          <div class="flex justify-end gap-3">
            <button @click="maintenanceConfirm = null" class="text-sm text-gray-400 hover:text-white">Cancelar</button>
            <button @click="doMaintenanceAction" class="btn-primary text-sm">
              {{ maintenanceConfirm.action === 'mark' ? 'Marcar' : 'Liberar' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Calendar section -->
      <div class="card">
        <!-- Week selector -->
        <div class="flex items-center justify-between mb-6">
          <button
            @click="prevWeek"
            class="text-sm text-gray-400 hover:text-white px-3 py-1 rounded hover:bg-dark-border transition-colors"
          >
            ← Anterior
          </button>
          <h3 class="text-white font-medium text-center">{{ weekLabel }}</h3>
          <button
            @click="nextWeek"
            class="text-sm text-gray-400 hover:text-white px-3 py-1 rounded hover:bg-dark-border transition-colors"
          >
            Siguiente →
          </button>
        </div>

        <!-- Room selector -->
        <div class="mb-4 max-w-xs">
          <label class="block text-xs text-gray-400 mb-1">Filtrar por sala</label>
          <select v-model="selectedRoomId" class="input-field" @change="onRoomChange">
            <option :value="null">Todas las salas</option>
            <option v-for="room in rooms" :key="room.id" :value="room.id">{{ room.name }}</option>
          </select>
        </div>

        <!-- Calendar grid -->
        <div class="overflow-x-auto">
          <table class="w-full border-collapse">
            <thead>
              <tr>
                <th class="w-24 p-2 text-left text-xs text-gray-500 font-medium"></th>
                <th
                  v-for="day in weekDays"
                  :key="day.date"
                  class="p-2 text-center text-xs font-medium min-w-[100px]"
                  :class="scheduleDays[day.dayOfWeek]?.enabled ? 'text-gray-300' : 'text-gray-600'"
                >
                  {{ day.label }}<br /><span class="font-normal opacity-60">{{ day.dateFormatted }}</span>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="block in timeBlocks" :key="block.key">
                <td class="p-2 text-xs text-gray-500 align-top whitespace-nowrap">{{ block.label }}</td>
                <td
                  v-for="day in weekDays"
                  :key="day.date"
                  class="p-1 align-top"
                  :class="getCellClass(day, block)"
                  @click="onCellClick(day, block)"
                >
                  <div class="text-xs text-center min-h-[40px] flex flex-col items-center justify-center gap-0.5 py-1">
                    <span v-if="getCellClass(day, block).includes('cursor-pointer')" class="text-[10px] opacity-60">{{ getCellStatusLabel(day, block) }}</span>
                    <span v-if="getCellClass(day, block) === 'bg-red-500/20 border border-red-500/30 rounded p-1'" class="text-[10px] text-red-300 truncate max-w-[90px]">{{ getCellTitle(day, block) }}</span>
                    <span v-if="getCellClass(day, block) === 'bg-yellow-500/20 border border-yellow-500/30 rounded p-1 cursor-pointer hover:bg-yellow-500/30'" class="text-[10px] text-yellow-300">Mantención</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Legend -->
        <div class="flex items-center gap-6 mt-6 pt-4 border-t border-dark-border flex-wrap">
          <span class="flex items-center gap-1.5 text-xs text-gray-400">
            <span class="w-3 h-3 rounded-sm bg-green-500/40 border border-green-500/50"></span> Disponible
          </span>
          <span class="flex items-center gap-1.5 text-xs text-gray-400">
            <span class="w-3 h-3 rounded-sm bg-red-500/40 border border-red-500/50"></span> Ocupado
          </span>
          <span class="flex items-center gap-1.5 text-xs text-gray-400">
            <span class="w-3 h-3 rounded-sm bg-yellow-500/40 border border-yellow-500/50"></span> Mantención
          </span>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import venueService from '@/services/venueService'
import scheduleService from '@/services/scheduleService'

const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
const dayLabels = {
  MONDAY: 'Lunes', TUESDAY: 'Martes', WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves', FRIDAY: 'Viernes', SATURDAY: 'Sábado', SUNDAY: 'Domingo'
}
const dayLabelsShort = { MONDAY: 'Lun', TUESDAY: 'Mar', WEDNESDAY: 'Mié', THURSDAY: 'Jue', FRIDAY: 'Vie', SATURDAY: 'Sáb', SUNDAY: 'Dom' }

const venue = ref(null)
const rooms = ref([])
const loading = ref(true)

const showConfig = ref(false)
const savingConfig = ref(false)
const confirmSaveConfig = ref(false)
const configMsg = ref('')
const configMsgType = ref('')

const scheduleDays = reactive({
  MONDAY: { enabled: true, openTime: '08:00', closeTime: '18:00' },
  TUESDAY: { enabled: true, openTime: '08:00', closeTime: '18:00' },
  WEDNESDAY: { enabled: true, openTime: '08:00', closeTime: '18:00' },
  THURSDAY: { enabled: true, openTime: '08:00', closeTime: '18:00' },
  FRIDAY: { enabled: true, openTime: '08:00', closeTime: '18:00' },
  SATURDAY: { enabled: false, openTime: '08:00', closeTime: '18:00' },
  SUNDAY: { enabled: false, openTime: '08:00', closeTime: '18:00' }
})

const blockCfg = reactive({ duration: 60, gap: 15 })

const currentWeekStart = ref(getMonday(new Date()))
const selectedRoomId = ref(null)
const scheduleData = ref({}) // key: `${roomId}|${date}|${HH}:${MM}` -> schedule entry

const maintenanceConfirm = ref(null)
const maintenanceLoading = ref(false)

function getMonday(d) {
  const date = new Date(d)
  const day = date.getDay() || 7
  date.setDate(date.getDate() - day + 1)
  date.setHours(0, 0, 0, 0)
  return date
}

function pad2(n) {
  return String(n).padStart(2, '0')
}

function timeToMinutes(t) {
  if (!t) return 0
  const [h, m] = t.split(':').map(Number)
  return h * 60 + (m || 0)
}

function minutesToTime(m) {
  return `${pad2(Math.floor(m / 60))}:${pad2(m % 60)}`
}

const weekLabel = computed(() => {
  const monday = new Date(currentWeekStart.value)
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)
  const opts = { day: 'numeric', month: 'long' }
  return `Semana del ${monday.toLocaleDateString('es-CL', opts)} al ${sunday.toLocaleDateString('es-CL', opts)}`
})

const weekDays = computed(() => {
  const result = []
  const monday = new Date(currentWeekStart.value)
  const weekDaysKeys = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
  for (let i = 0; i < 7; i++) {
    const d = new Date(monday)
    d.setDate(monday.getDate() + i)
    result.push({
      date: d.toISOString().slice(0, 10),
      dateFormatted: d.toLocaleDateString('es-CL', { day: 'numeric', month: 'short' }),
      label: dayLabelsShort[weekDaysKeys[i]],
      dayOfWeek: weekDaysKeys[i]
    })
  }
  return result
})

const timeBlocks = computed(() => {
  const blocks = []
  let minOpen = Infinity
  let maxClose = -Infinity

  for (const day of days) {
    if (scheduleDays[day].enabled) {
      minOpen = Math.min(minOpen, timeToMinutes(scheduleDays[day].openTime))
      maxClose = Math.max(maxClose, timeToMinutes(scheduleDays[day].closeTime))
    }
  }

  if (minOpen === Infinity || maxClose <= 0) return blocks

  const duration = blockCfg.duration || 60
  const gap = blockCfg.gap || 0

  let current = minOpen
  while (current + duration <= maxClose) {
    blocks.push({
      key: minutesToTime(current),
      label: `${minutesToTime(current)} - ${minutesToTime(current + duration)}`,
      start: current,
      end: current + duration
    })
    current = current + duration + gap
  }
  return blocks
})

function buildLookupKey(roomId, date, timeStr) {
  return `${roomId}|${date}|${timeStr}`
}

function getScheduleForCell(dateStr, blockStartMin, roomId) {
  const timeStr = minutesToTime(blockStartMin)
  const key = buildLookupKey(roomId, dateStr, timeStr)
  return scheduleData.value[key] || null
}

function getCellClass(day, block) {
  const dow = day.dayOfWeek
  if (!scheduleDays[dow]?.enabled) {
    return 'bg-dark-bg/50 rounded'
  }

  const dayClose = timeToMinutes(scheduleDays[dow]?.closeTime)
  if (block.start >= dayClose) {
    return 'bg-dark-bg/50 rounded'
  }

  const dayOpen = timeToMinutes(scheduleDays[dow]?.openTime)
  if (block.start < dayOpen) {
    return 'bg-dark-bg/50 rounded'
  }

  if (selectedRoomId.value) {
    const entry = getScheduleForCell(day.date, block.start, selectedRoomId.value)
    if (entry?.status === 'OCCUPIED') return 'bg-red-500/20 border border-red-500/30 rounded p-1'
    if (entry?.status === 'MAINTENANCE') return 'bg-yellow-500/20 border border-yellow-500/30 rounded p-1 cursor-pointer hover:bg-yellow-500/30'
    return 'bg-green-500/20 border border-green-500/30 rounded p-1 cursor-pointer hover:bg-green-500/30'
  }

  // Aggregate for all rooms
  let hasOccupied = false
  let hasMaintenance = false
  let hasAvailable = false

  for (const room of rooms.value) {
    const entry = getScheduleForCell(day.date, block.start, room.id)
    if (entry?.status === 'OCCUPIED') hasOccupied = true
    else if (entry?.status === 'MAINTENANCE') hasMaintenance = true
    else hasAvailable = true
  }

  if (hasOccupied) return 'bg-red-500/20 border border-red-500/30 rounded p-1'
  if (hasMaintenance) return 'bg-yellow-500/20 border border-yellow-500/30 rounded p-1'
  return 'bg-green-500/20 border border-green-500/30 rounded p-1'
}

function getCellStatusLabel(day, block) {
  const dow = day.dayOfWeek
  if (!scheduleDays[dow]?.enabled) return ''

  if (selectedRoomId.value) {
    const entry = getScheduleForCell(day.date, block.start, selectedRoomId.value)
    if (entry?.status === 'OCCUPIED') return 'Ocupado'
    if (entry?.status === 'MAINTENANCE') return ''
    return 'Disponible'
  }

  let hasOccupied = false
  let hasMaintenance = false
  for (const room of rooms.value) {
    const entry = getScheduleForCell(day.date, block.start, room.id)
    if (entry?.status === 'OCCUPIED') hasOccupied = true
    else if (entry?.status === 'MAINTENANCE') hasMaintenance = true
  }
  if (hasOccupied) return 'Ocupado'
  if (hasMaintenance) return 'Mant.'
  return 'Disp.'
}

function getCellTitle(day, block) {
  if (!selectedRoomId.value) return ''
  const entry = getScheduleForCell(day.date, block.start, selectedRoomId.value)
  return entry?.classTitle || ''
}

function onCellClick(day, block) {
  const dow = day.dayOfWeek
  if (!scheduleDays[dow]?.enabled) return
  if (!selectedRoomId.value) return

  const entry = getScheduleForCell(day.date, block.start, selectedRoomId.value)
  const status = entry?.status || 'AVAILABLE'

  if (status === 'OCCUPIED') return

  if (status === 'MAINTENANCE') {
    maintenanceConfirm.value = {
      action: 'release',
      message: '¿Liberar este horario de mantención?',
      blockId: entry.blockId,
      roomId: selectedRoomId.value
    }
    return
  }

  maintenanceConfirm.value = {
    action: 'mark',
    message: '¿Marcar este horario como mantención?',
    blockId: entry?.blockId,
    roomId: selectedRoomId.value,
    date: day.date,
    startMin: block.start
  }
}

async function doMaintenanceAction() {
  if (!maintenanceConfirm.value) return
  const { action, blockId, roomId, date, startMin } = maintenanceConfirm.value

  maintenanceLoading.value = true
  try {
    if (action === 'release') {
      await scheduleService.releaseMaintenance(blockId)
    } else if (action === 'mark') {
      await scheduleService.markMaintenance(roomId, blockId, 'Mantención programada')
    }
    maintenanceConfirm.value = null
    await loadAllSchedules()
  } catch (e) {
    configMsg.value = e?.response?.data?.message || 'Error al actualizar mantención'
    configMsgType.value = 'error'
  }
  maintenanceLoading.value = false
}

function onRoomChange() {
  loadAllSchedules()
}

async function saveAllConfig() {
  confirmSaveConfig.value = false
  savingConfig.value = true
  configMsg.value = ''
  try {
    const schedules = days
      .filter(d => scheduleDays[d].enabled)
      .map(d => ({
        dayOfWeek: d,
        openTime: scheduleDays[d].openTime,
        closeTime: scheduleDays[d].closeTime
      }))

    const cfg = {
      blockDurationMin: blockCfg.duration,
      gapBetweenBlocksMin: blockCfg.gap
    }

    await scheduleService.saveSchedule(venue.value.id, schedules)
    await scheduleService.saveBlockConfig(venue.value.id, cfg)
    await scheduleService.generateBlocks(venue.value.id)

    configMsg.value = 'Configuración guardada y bloques regenerados correctamente.'
    configMsgType.value = 'success'
    await loadAllSchedules()
  } catch (e) {
    configMsg.value = e?.response?.data?.message || 'Error al guardar la configuración'
    configMsgType.value = 'error'
  }
  savingConfig.value = false
}

async function loadAllSchedules() {
  if (!venue.value) return

  const monday = new Date(currentWeekStart.value)
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)
  sunday.setHours(23, 59, 59, 999)

  const from = monday.toISOString()
  const to = sunday.toISOString()

  const map = {}

  const roomList = selectedRoomId.value
    ? rooms.value.filter(r => r.id === selectedRoomId.value)
    : rooms.value

  for (const room of roomList) {
    try {
      const entries = await scheduleService.getRoomSchedule(room.id, from, to)
      if (Array.isArray(entries)) {
        for (const entry of entries) {
          const dateStr = entry.startTime?.slice(0, 10)
          const timeStr = entry.startTime?.slice(11, 16)
          if (dateStr && timeStr) {
            const key = buildLookupKey(room.id, dateStr, timeStr)
            map[key] = { ...entry, roomId: room.id }
          }
        }
      }
    } catch {
      // ignore errors for individual rooms
    }
  }

  scheduleData.value = map
}

function prevWeek() {
  const d = new Date(currentWeekStart.value)
  d.setDate(d.getDate() - 7)
  currentWeekStart.value = d
  loadAllSchedules()
}

function nextWeek() {
  const d = new Date(currentWeekStart.value)
  d.setDate(d.getDate() + 7)
  currentWeekStart.value = d
  loadAllSchedules()
}

onMounted(async () => {
  try {
    const venues = await venueService.getMyVenues()
    const vArr = Array.isArray(venues) ? venues : venues?.content || []
    const sede = vArr.find(v => v.status === 'APROBADA') || vArr[0]
    if (!sede) {
      loading.value = false
      return
    }

    venue.value = sede

    const [roomsRes, scheduleRes, blockConfigRes] = await Promise.allSettled([
      venueService.getVenueRooms(sede.id),
      scheduleService.getSchedule(sede.id),
      scheduleService.getBlockConfig(sede.id)
    ])

    if (roomsRes.status === 'fulfilled') {
      rooms.value = Array.isArray(roomsRes.value) ? roomsRes.value : []
    }

    if (scheduleRes.status === 'fulfilled' && Array.isArray(scheduleRes.value)) {
      for (const s of scheduleRes.value) {
        if (scheduleDays[s.dayOfWeek]) {
          scheduleDays[s.dayOfWeek].enabled = true
          scheduleDays[s.dayOfWeek].openTime = s.openTime?.slice(0, 5) || '08:00'
          scheduleDays[s.dayOfWeek].closeTime = s.closeTime?.slice(0, 5) || '18:00'
        }
      }
    }

    if (blockConfigRes.status === 'fulfilled' && blockConfigRes.value) {
      blockCfg.duration = blockConfigRes.value.blockDurationMin || 60
      blockCfg.gap = blockConfigRes.value.gapBetweenBlocksMin || 15
    }

    await loadAllSchedules()
  } catch (err) {
    console.error('Error al cargar datos de la sede', err)
  }
  loading.value = false
})
</script>
