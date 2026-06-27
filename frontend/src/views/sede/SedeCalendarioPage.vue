<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <div class="flex items-center justify-between mb-8 flex-wrap gap-4">
      <h1 class="text-3xl font-bold text-white">Agenda de Salas</h1>
      <router-link to="/sede/salas" class="text-sm text-gray-400 hover:text-white">← Volver a salas</router-link>
    </div>

    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>

    <!-- CTA: sin horario -->
    <div v-else-if="!hayHorario" class="card text-center py-16 mb-6">
      <div class="w-16 h-16 bg-yellow-500/10 rounded-2xl flex items-center justify-center mx-auto mb-4">
        <svg class="w-8 h-8 text-yellow-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
      </div>
      <h2 class="text-xl font-bold text-white mb-2">Sin horarios disponibles</h2>
      <p class="text-gray-400 text-sm mb-6 max-w-md mx-auto">
        Tu sede aún no tiene un horario laboral configurado. Defínelo en
        <strong>Configuración</strong> para que el sistema genere automáticamente los bloques
        de disponibilidad de tus salas.
      </p>
      <router-link to="/sede/configuracion" class="btn-primary text-base px-8 py-3 inline-block">
        Configurar horario laboral
      </router-link>
    </div>

    <template v-else>
      <!-- Acceso a la configuración del horario (la edición vive en Configuración) -->
      <div class="flex items-center justify-between gap-3 mb-6 p-4 card">
        <p class="text-sm text-gray-400">
          El horario laboral y la duración de los bloques se configuran en
          <strong class="text-gray-200">Configuración de la sede</strong>.
        </p>
        <router-link to="/sede/configuracion" class="text-sm text-primary hover:underline whitespace-nowrap">
          Editar horario →
        </router-link>
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

      <!-- Horario ocupado: info de solo lectura (gestión = fase futura) -->
      <div v-if="occupiedInfo" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
        <div class="card max-w-md w-full mx-4 space-y-4">
          <h4 class="text-lg font-semibold text-white">Horario ocupado</h4>
          <p class="text-sm text-gray-400">
            {{ occupiedInfo.date }} · {{ occupiedInfo.label }} — este horario está ocupado por una reserva.
          </p>
          <p class="text-xs text-gray-500">
            La gestión de horarios ocupados (liberar el cupo o cambiarlo a mantención) estará disponible próximamente.
          </p>
          <div class="flex justify-end">
            <button @click="occupiedInfo = null" class="btn-primary text-sm">Cerrar</button>
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
                    <span v-if="cellStatus(day, block) !== 'OUT'" class="text-[10px]" :class="cellLabelClass(day, block)">{{ cellStatusLabel(day, block) }}</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Leyenda clickeable: actúa como filtro de estado -->
        <div class="flex items-center gap-2 mt-6 pt-4 border-t border-dark-border flex-wrap">
          <button
            v-for="f in filtros" :key="f.value"
            type="button"
            @click="toggleFiltro(f.value)"
            :class="[
              'flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-lg border transition-colors',
              filtroEstado === f.value
                ? 'border-white/30 bg-[var(--bg-elevated)] text-white'
                : 'border-white/10 text-gray-400 hover:text-white'
            ]"
          >
            <span class="w-3 h-3 rounded-sm" :class="f.swatch"></span> {{ f.label }}
          </button>
          <span v-if="filtroEstado" class="text-[11px] text-gray-500 ml-1">
            Mostrando solo <strong class="text-gray-300">{{ filtroLabel }}</strong> ·
            <button type="button" @click="filtroEstado = null" class="text-primary hover:underline">ver todo</button>
          </span>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import venueService from '@/services/venueService'
import scheduleService from '@/services/scheduleService'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const toast = useToast()

const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
const dayLabelsShort = { MONDAY: 'Lun', TUESDAY: 'Mar', WEDNESDAY: 'Mié', THURSDAY: 'Jue', FRIDAY: 'Vie', SATURDAY: 'Sáb', SUNDAY: 'Dom' }

const venue = ref(null)
const rooms = ref([])
const loading = ref(true)
const hayHorario = ref(false)

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

// Info de solo lectura al pinchar un horario ocupado (gestión = fase futura).
const occupiedInfo = ref(null)

// Leyenda clickeable como filtro de estado. null = mostrar todo.
const filtroEstado = ref(null)
const filtros = [
  { value: 'AVAILABLE', label: 'Disponible', swatch: 'bg-green-500/40 border border-green-500/50' },
  { value: 'OCCUPIED', label: 'Ocupado', swatch: 'bg-red-500/40 border border-red-500/50' },
  { value: 'MAINTENANCE', label: 'Mantención', swatch: 'bg-yellow-500/40 border border-yellow-500/50' }
]
const filtroLabel = computed(() => filtros.find(f => f.value === filtroEstado.value)?.label || '')
function toggleFiltro(v) {
  filtroEstado.value = filtroEstado.value === v ? null : v
}

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

const ZONA_SEDE = 'America/Santiago'

// Convierte un instante ISO (UTC) a su fecha (YYYY-MM-DD) y hora (HH:MM) en la
// zona horaria de la sede, para que coincida con la grilla del horario laboral.
function instantToLocalParts(iso) {
  if (!iso) return { date: '', time: '' }
  const p = new Intl.DateTimeFormat('en-CA', {
    timeZone: ZONA_SEDE, year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hour12: false
  }).formatToParts(new Date(iso)).reduce((a, x) => { a[x.type] = x.value; return a }, {})
  const hour = p.hour === '24' ? '00' : p.hour
  return { date: `${p.year}-${p.month}-${p.day}`, time: `${hour}:${p.minute}` }
}

// Offset (ms) de la zona de la sede respecto a UTC en un instante dado.
function sedeOffsetMs(date) {
  const p = new Intl.DateTimeFormat('en-US', {
    timeZone: ZONA_SEDE, hour12: false,
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  }).formatToParts(date).reduce((a, x) => { a[x.type] = x.value; return a }, {})
  const asUTC = Date.UTC(+p.year, +p.month - 1, +p.day, +p.hour, +p.minute, +p.second)
  return asUTC - date.getTime()
}

// Convierte una fecha (YYYY-MM-DD) + minutos desde medianoche en hora de la sede
// al instante UTC (ISO) correspondiente. Inverso de instantToLocalParts.
function localSedeToInstant(dateStr, minutes) {
  const [y, mo, d] = dateStr.split('-').map(Number)
  const hh = Math.floor(minutes / 60)
  const mm = minutes % 60
  const naiveUTC = Date.UTC(y, mo - 1, d, hh, mm)
  const off = sedeOffsetMs(new Date(naiveUTC))
  return new Date(naiveUTC - off).toISOString()
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

// Estado efectivo de una celda: 'OUT' (fuera de horario) | 'AVAILABLE' | 'OCCUPIED' | 'MAINTENANCE'.
// Cuando no hay sala seleccionada agrega todas las salas con prioridad ocupado > mantención > disponible.
function cellStatus(day, block) {
  const dow = day.dayOfWeek
  if (!scheduleDays[dow]?.enabled) return 'OUT'
  if (block.start >= timeToMinutes(scheduleDays[dow]?.closeTime)) return 'OUT'
  if (block.start < timeToMinutes(scheduleDays[dow]?.openTime)) return 'OUT'

  if (selectedRoomId.value) {
    const entry = getScheduleForCell(day.date, block.start, selectedRoomId.value)
    return entry?.status || 'AVAILABLE'
  }

  let hasOccupied = false
  let hasMaintenance = false
  for (const room of rooms.value) {
    const entry = getScheduleForCell(day.date, block.start, room.id)
    if (entry?.status === 'OCCUPIED') hasOccupied = true
    else if (entry?.status === 'MAINTENANCE') hasMaintenance = true
  }
  if (hasOccupied) return 'OCCUPIED'
  if (hasMaintenance) return 'MAINTENANCE'
  return 'AVAILABLE'
}

// Una celda se atenúa (y deja de ser interactiva) si está fuera de horario o si hay un
// filtro activo y su estado no coincide.
function cellDimmed(day, block) {
  const st = cellStatus(day, block)
  if (st === 'OUT') return true
  return !!filtroEstado.value && st !== filtroEstado.value
}

function getCellClass(day, block) {
  const st = cellStatus(day, block)
  if (st === 'OUT') return 'bg-dark-bg/50 rounded'

  const dimmed = !!filtroEstado.value && st !== filtroEstado.value
  const colors = {
    AVAILABLE: dimmed ? 'bg-green-500/10 border-green-500/15' : 'bg-green-500/20 border-green-500/30',
    OCCUPIED: dimmed ? 'bg-red-500/10 border-red-500/15' : 'bg-red-500/20 border-red-500/30',
    MAINTENANCE: dimmed ? 'bg-yellow-500/10 border-yellow-500/15' : 'bg-yellow-500/20 border-yellow-500/30'
  }[st]

  let cls = `${colors} border rounded p-1`
  if (dimmed) {
    cls += ' opacity-40'
  } else if (selectedRoomId.value) {
    const hover = { AVAILABLE: 'hover:bg-green-500/30', OCCUPIED: 'hover:bg-red-500/30', MAINTENANCE: 'hover:bg-yellow-500/30' }[st]
    cls += ` cursor-pointer ${hover}`
  }
  return cls
}

function cellStatusLabel(day, block) {
  const st = cellStatus(day, block)
  if (st === 'OUT') return ''
  const agg = !selectedRoomId.value
  if (st === 'OCCUPIED') return 'Ocupado'
  if (st === 'MAINTENANCE') return agg ? 'Mant.' : 'Mantención'
  return agg ? 'Disp.' : 'Disponible'
}

function cellLabelClass(day, block) {
  const st = cellStatus(day, block)
  const color = { AVAILABLE: 'text-green-200', OCCUPIED: 'text-red-200', MAINTENANCE: 'text-yellow-200' }[st] || 'text-gray-300'
  return cellDimmed(day, block) ? `${color} opacity-50` : `${color} opacity-80`
}

function onCellClick(day, block) {
  // Sin sala seleccionada (vista agregada) o celda atenuada por horario/filtro: no se actúa.
  if (!selectedRoomId.value) return
  if (cellDimmed(day, block)) return

  const st = cellStatus(day, block)
  const entry = getScheduleForCell(day.date, block.start, selectedRoomId.value)

  if (st === 'OCCUPIED') {
    occupiedInfo.value = { date: day.dateFormatted, label: block.label }
    return
  }

  if (st === 'MAINTENANCE') {
    maintenanceConfirm.value = {
      action: 'release',
      message: '¿Liberar este horario y dejarlo disponible nuevamente?',
      // entry puede traer el id de la mantención y/o el id del bloque (si existe).
      maintenanceId: entry?.maintenanceId || null,
      blockId: entry?.id || null,
      roomId: selectedRoomId.value
    }
    return
  }

  // AVAILABLE — calcular el tramo desde la celda (sirve aunque no exista bloque).
  maintenanceConfirm.value = {
    action: 'mark',
    message: '¿Marcar este horario como mantención?',
    blockId: entry?.id || null,
    startTime: entry?.startTime || localSedeToInstant(day.date, block.start),
    endTime: entry?.endTime || localSedeToInstant(day.date, block.end),
    roomId: selectedRoomId.value
  }
}

async function doMaintenanceAction() {
  if (!maintenanceConfirm.value) return
  const { action, blockId, maintenanceId, roomId, startTime, endTime } = maintenanceConfirm.value

  maintenanceLoading.value = true
  try {
    if (action === 'release') {
      await scheduleService.releaseMaintenance(maintenanceId, blockId)
    } else if (action === 'mark') {
      await scheduleService.markMaintenance(roomId, startTime, endTime, blockId, 'Mantención programada')
    }
    maintenanceConfirm.value = null
    await loadAllSchedules()
  } catch (e) {
    toast.error(e?.response?.data?.message || 'Error al actualizar mantención')
  }
  maintenanceLoading.value = false
}

function onRoomChange() {
  loadAllSchedules()
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
          // El bloque viene en UTC; la grilla usa la hora local de la sede.
          const { date: dateStr, time: timeStr } = instantToLocalParts(entry.startTime)
          if (dateStr && timeStr) {
            const key = buildLookupKey(room.id, dateStr, timeStr)
            map[key] = { ...entry, roomId: room.id }
          }
        }
      }
    } catch {
      // ignore errors for individual rooms
    }

    // Sobreponer mantenciones: son la fuente de verdad y pueden existir aunque la
    // sala no tenga bloques de horario generados.
    try {
      const maints = await scheduleService.getRoomMaintenances(room.id, from, to)
      if (Array.isArray(maints)) {
        for (const m of maints) {
          const { date: dateStr, time: timeStr } = instantToLocalParts(m.startTime)
          if (dateStr && timeStr) {
            const key = buildLookupKey(room.id, dateStr, timeStr)
            const prev = map[key] || {}
            map[key] = { ...prev, roomId: room.id, status: 'MAINTENANCE', maintenanceId: m.id, startTime: m.startTime, endTime: m.endTime }
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

    // Pre-seleccionar la sala si viene en la URL (?sala=:id), p.ej. al pulsar
    // "Agenda" desde la lista de salas.
    const salaParam = route.query.sala
    if (salaParam && rooms.value.some(r => r.id === salaParam)) {
      selectedRoomId.value = salaParam
    }

    if (scheduleRes.status === 'fulfilled' && Array.isArray(scheduleRes.value)) {
      hayHorario.value = scheduleRes.value.length > 0
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
