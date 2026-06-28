<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <div class="flex items-center justify-between mb-4 flex-wrap gap-4">
      <div>
        <h1 class="text-3xl font-bold text-white mb-1">Buscar Salas</h1>
        <p class="text-gray-400">Encuentra la sala perfecta para tu clase.</p>
      </div>
    </div>

    <div v-if="borradorId" class="bg-blue-500/10 border border-blue-500/30 rounded-xl px-4 py-3 mb-6 flex items-center gap-3">
      <svg class="w-5 h-5 text-blue-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
      </svg>
      <p class="text-blue-300 text-sm">Selecciona un horario para asignarlo a tu clase y publicarla.</p>
    </div>

    <!-- ====== STEP 1: Filtros ====== -->
    <div class="card mb-6">
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
        <div>
          <label class="text-xs text-gray-500 mb-0.5 block">Region</label>
          <select v-model="filtros.region" class="input-field text-sm py-2" @change="onRegionChange">
            <option value="">Todas las regiones</option>
            <option v-for="r in regiones" :key="r" :value="r">{{ r }}</option>
          </select>
        </div>
        <div>
          <label class="text-xs text-gray-500 mb-0.5 block">Comuna</label>
          <input v-model="filtros.comuna" type="text" class="input-field text-sm py-2" placeholder="Ej: Providencia" />
        </div>
        <div>
          <label class="text-xs text-gray-500 mb-0.5 block">Desde</label>
          <input v-model="filtros.fechaDesde" type="date" class="input-field text-sm py-2" />
        </div>
        <div>
          <label class="text-xs text-gray-500 mb-0.5 block">Hasta</label>
          <input v-model="filtros.fechaHasta" type="date" class="input-field text-sm py-2" />
        </div>
      </div>

      <div class="mb-4">
        <label class="text-xs text-gray-500 mb-1.5 block">Disciplina</label>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="d in disciplinas"
            :key="d"
            type="button"
            :class="[
              'px-4 py-1.5 rounded-full text-sm font-medium border transition-colors',
              filtros.disciplina === d
                ? 'bg-primary/20 border-primary text-primary'
                : 'border-white/10 text-gray-400 hover:border-white/30 hover:text-white bg-[var(--bg-elevated)]'
            ]"
            @click="toggleDisciplina(d)"
          >
            {{ d }}
          </button>
        </div>
      </div>

      <div v-if="filtros.disciplina" class="mb-4">
        <label class="text-xs text-gray-500 mb-1.5 block">Caracteristicas</label>
        <div class="flex flex-wrap gap-3">
          <label v-for="c in caracteristicasFiltradas" :key="c.key" class="flex items-center gap-1.5 cursor-pointer">
            <input type="checkbox" v-model="filtros.caracteristicas" :value="c.key" class="w-4 h-4 rounded border-dark-border bg-dark-bg text-primary focus:ring-primary/50" />
            <span class="text-sm text-gray-300">{{ c.label }}</span>
          </label>
        </div>
      </div>

      <button @click="buscar" class="btn-primary text-sm w-full sm:w-auto">Buscar</button>
    </div>

    <!-- ====== STEP 2: Venues ====== -->
    <div v-if="loading" class="text-center text-gray-400 py-20">Buscando salas disponibles...</div>
    <div v-else-if="venues.length === 0" class="card text-center py-12"><p class="text-gray-400">No se encontraron salas con esos filtros.</p></div>
    <div v-else class="space-y-4">
      <div v-for="venue in venues" :key="venue.id" class="card">
        <div class="flex items-start justify-between gap-3">
          <div class="flex-1">
            <h3 class="text-lg font-semibold text-white">{{ venue.name }}</h3>
            <p class="text-gray-400 text-sm">{{ venue.address }}</p>
            <p class="text-gray-500 text-xs mt-1">{{ venue.region || '' }}{{ venue.city ? ' . ' + venue.city : '' }}</p>
            <p class="text-primary text-xs mt-0.5">{{ venue.rooms?.length || 0 }} sala(s) disponible(s)</p>
          </div>
          <button
            @click="toggleVenue(venue.id)"
            class="flex-shrink-0 inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-primary/40 text-primary text-sm font-medium hover:bg-primary/10 transition-colors whitespace-nowrap"
          >
            <span>{{ expandedVenue === venue.id ? 'Ocultar salas' : 'Ver salas disponibles' }}</span>
            <svg :class="['w-4 h-4 transition-transform', expandedVenue === venue.id && 'rotate-180']" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
          </button>
        </div>

        <!-- ====== STEP 3: Rooms ====== -->
        <div v-if="expandedVenue === venue.id" class="mt-4 space-y-3">
          <div v-if="venue.rooms?.length">
            <div v-for="room in venue.rooms" :key="room.id" class="bg-[var(--bg-elevated)] rounded-xl p-4">
              <div class="flex items-start justify-between gap-3">
                <div class="flex-1">
                  <p class="text-white font-medium">{{ room.name }}</p>
                  <p class="text-gray-400 text-sm">
                    Cap: {{ room.capacity }} pers.
                    <span v-if="room.tamanoM2"> . {{ room.tamanoM2 }} m2</span>
                    <span v-if="room.floorType"> . Piso {{ room.floorType }}</span>
                  </p>
                  <p class="text-primary text-sm font-medium mt-0.5">${{ room.pricePerHour?.toLocaleString() }} / hora</p>

                  <!-- Caracteristicas: solo informativas. -->
                  <div v-if="getCaracteristicasDanza(room).length" class="mt-2">
                    <span class="text-xs text-gray-500">Danza:</span>
                    <div class="flex flex-wrap gap-1 mt-0.5">
                      <span v-for="c in getCaracteristicasDanza(room)" :key="c" class="equip-tag">{{ c }}</span>
                    </div>
                  </div>
                  <div v-if="getCaracteristicasMusica(room).length" class="mt-1">
                    <span class="text-xs text-gray-500">Musica:</span>
                    <div class="flex flex-wrap gap-1 mt-0.5">
                      <span v-for="c in getCaracteristicasMusica(room)" :key="c" class="equip-tag">{{ c }}</span>
                    </div>
                  </div>
                </div>
                <button
                  @click="toggleRoom(room.id)"
                  class="flex-shrink-0 inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-white/15 text-gray-300 text-sm font-medium hover:bg-white/5 hover:text-white transition-colors whitespace-nowrap"
                >
                  <span>{{ expandedRoom === room.id ? 'Ocultar horarios' : 'Horarios de la sala' }}</span>
                  <svg :class="['w-4 h-4 transition-transform', expandedRoom === room.id && 'rotate-180']" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
                </button>
              </div>

              <!-- ====== STEP 4: Weekly Calendar ====== -->
              <div v-if="expandedRoom === room.id" class="mt-3">
                <div v-if="roomLoading === room.id" class="text-gray-500 text-sm py-4 text-center">Cargando disponibilidad...</div>
                <div v-else class="overflow-x-auto">
                  <!-- Navegación de semana: siempre visible para poder revisar otras semanas -->
                  <div class="flex items-center justify-between mb-3">
                    <button
                      @click.stop="prevRoomWeek(room.id)"
                      :disabled="!puedeRetrocederSemana(room.id)"
                      class="text-xs text-gray-400 hover:text-white px-2 py-1 rounded hover:bg-dark-border disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:bg-transparent disabled:hover:text-gray-400"
                    >&larr; Anterior</button>
                    <span class="text-xs text-gray-400 font-medium">{{ getRoomWeekLabel(room.id) }}</span>
                    <button @click.stop="nextRoomWeek(room.id)" class="text-xs text-gray-400 hover:text-white px-2 py-1 rounded hover:bg-dark-border">Siguiente &rarr;</button>
                  </div>

                  <template v-if="roomSlots[room.id]?.length">
                  <table class="w-full border-collapse">
                    <thead>
                      <tr>
                        <th class="w-20 p-2 text-left text-xs text-gray-500 font-medium"></th>
                        <th v-for="day in calendarWeekDays" :key="day.date" class="p-2 text-center text-xs font-medium text-gray-300 min-w-[100px]">
                          {{ day.label }}<br /><span class="font-normal opacity-60">{{ day.dateFormatted }}</span>
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="block in calendarTimeBlocks" :key="block.key">
                        <td class="p-2 text-xs text-gray-500 align-top whitespace-nowrap">{{ block.label }}</td>
                        <td v-for="day in calendarWeekDays" :key="day.date" class="p-1 align-top">
                          <div
                            v-for="slot in getRoomSlotsForCell(room.id, day.date, block)"
                            :key="slot.id"
                            :class="[
                              'rounded p-1.5 mb-0.5 cursor-pointer border text-xs transition-all select-none',
                              isSlotSelected(slot.id)
                                ? 'bg-green-500/40 border-green-400 ring-1 ring-green-400'
                                : 'bg-green-500/15 border-green-500/30 hover:bg-green-500/25'
                            ]"
                            @click.stop="toggleSlot(room, venue, slot)"
                          >
                            <span class="text-green-300 text-[10px] block">
                              {{ isSlotSelected(slot.id) ? '✓ Seleccionado' : 'Disponible' }}
                            </span>
                          </div>
                          <div v-if="!getRoomSlotsForCell(room.id, day.date, block).length" class="min-h-[28px]"></div>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                  <!-- Barra de selección -->
                  <div v-if="selectedSlots.length && selectionRoom?.id === room.id" class="mt-4 p-3 rounded-xl bg-green-500/10 border border-green-500/20 flex items-center justify-between gap-3">
                    <div class="text-sm">
                      <span class="text-green-300 font-semibold">{{ selectedSlots.length }} bloque{{ selectedSlots.length > 1 ? 's' : '' }}</span>
                      <span class="text-gray-400"> · {{ selectedSlots.length }}h · </span>
                      <span class="text-primary font-semibold">${{ (selectedSlots.length * (room.pricePerHour || 0)).toLocaleString('es-CL') }}</span>
                    </div>
                    <div class="flex gap-2">
                      <button @click.stop="selectedSlots = []" class="text-xs text-gray-400 hover:text-white px-3 py-1.5 rounded-lg border border-white/10 hover:bg-white/5">Limpiar</button>
                      <button @click.stop="abrirModal()" class="text-sm font-semibold px-4 py-1.5 rounded-xl bg-primary text-white hover:bg-primary/80 transition-colors">Pagar reserva</button>
                    </div>
                  </div>
                  </template>

                  <!-- Sin disponibilidad esta semana: mensaje claro, conservando la navegación -->
                  <div v-else class="text-center py-8 px-4 rounded-xl bg-[var(--bg-base)] border border-white/5">
                    <svg class="w-9 h-9 text-gray-600 mx-auto mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
                    <p class="text-gray-300 text-sm font-medium">Esta sala no tiene horarios disponibles</p>
                    <p class="text-gray-500 text-xs mt-1">La sede aún no ha publicado su disponibilidad para esta semana. Prueba con otra semana.</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <p v-else class="text-gray-500 text-sm">No hay salas en esta sede.</p>
        </div>
      </div>
    </div>

    <!-- Alerta identidad -->
    <BottomSheet v-model="alertaIdentidad">
      <div class="flex items-start gap-3 mb-4">
        <svg class="w-6 h-6 text-yellow-400 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
        </svg>
        <div>
          <h3 class="text-white font-semibold mb-1">Identidad no validada</h3>
          <p class="text-gray-400 text-sm">Debes validar tu identidad antes de reservar una sala y crear clases.</p>
        </div>
      </div>
      <div class="flex gap-3">
        <button @click="alertaIdentidad = false" class="flex-1 px-4 py-2 rounded-xl border border-white/10 text-gray-300 hover:bg-white/5 text-sm">Cerrar</button>
        <router-link to="/profile/identity" class="flex-1 text-center px-4 py-2 rounded-xl bg-primary text-white text-sm font-medium hover:bg-primary/80">Validar identidad</router-link>
      </div>
    </BottomSheet>

    <!-- ====== STEP 5: Modal de confirmacion (bottom sheet) ====== -->
    <BottomSheet v-model="modal.abierto">
      <h3 class="text-lg font-semibold text-white mb-3">Confirmar Reserva</h3>
      <div class="text-gray-400 text-sm space-y-1 mb-3">
        <p><span class="text-gray-500">Sede:</span> {{ modal.venue?.name }}</p>
        <p><span class="text-gray-500">Sala:</span> {{ modal.room?.name }} ({{ modal.room?.capacity }} personas)</p>
      </div>
      <div class="mb-3 space-y-1 max-h-36 overflow-y-auto pr-1">
        <div v-for="s in modal.slots" :key="s.id" class="flex justify-between text-xs bg-white/5 rounded-lg px-3 py-2">
          <span class="text-gray-300">{{ formatDate(s.startTime) }}</span>
          <span class="text-gray-400">{{ formatTime(s.startTime) }} – {{ formatTime(s.endTime) }}</span>
        </div>
      </div>
      <div class="flex justify-between text-sm mb-4 px-1">
        <span class="text-gray-400">{{ modal.slots?.length }} bloque{{ modal.slots?.length > 1 ? 's' : '' }} · {{ modal.slots?.length }}h</span>
        <span class="text-primary font-semibold">${{ ((modal.slots?.length || 0) * (modal.room?.pricePerHour || 0)).toLocaleString('es-CL') }}</span>
      </div>
      <button @click="pagar" :disabled="modal.procesando" class="w-full px-4 py-3 mb-3 rounded-xl bg-[#009ee3] hover:bg-[#0089c7] text-white text-sm font-semibold transition-colors disabled:opacity-60">
        {{ modal.procesando ? 'Redirigiendo a MercadoPago...' : 'Pagar con MercadoPago' }}
      </button>
      <p class="text-gray-500 text-xs text-center mb-3">Serás redirigido a MercadoPago para completar el pago de forma segura.</p>
      <button @click="modal.abierto = false" :disabled="modal.procesando" class="w-full px-4 py-2 rounded-xl border border-white/10 text-gray-300 hover:bg-white/5 text-sm">Cancelar</button>
    </BottomSheet>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import classService from '@/services/classService'
import venueService from '@/services/venueService'
import scheduleService from '@/services/scheduleService'
import paymentService from '@/services/paymentService'
import { useAuth } from '@/stores/auth'
import { useToast } from '@/composables/useToast'
import { formatDate, formatTime } from '@/utils/dateFormatter'
import BottomSheet from '@/components/BottomSheet.vue'

const toast = useToast()
const route = useRoute()
const auth = useAuth()
const { syncAtributos, identidadValidada } = auth

const borradorId = computed(() => route.query.borradorId || null)
const alertaIdentidad = ref(false)
const venues = ref([])
const loading = ref(true)
const expandedVenue = ref(null)
const expandedRoom = ref(null)
const roomSlots = ref({})
const roomLoading = ref(null)
// Salas a las que ya se les saltó automáticamente a su primera semana con cupos
// (evita re-saltar cuando el usuario navega manualmente entre semanas).
const autoSaltadas = new Set()
const selectedSlots = ref([])
const selectionRoom = ref(null)
const selectionVenue = ref(null)
const modal = ref({ abierto: false, venue: null, room: null, slots: [], procesando: false })
const filtros = ref({ region: '', comuna: '', disciplina: '', fechaDesde: '', fechaHasta: '', caracteristicas: [] })

const roomWeekStarts = ref({})
let roomWeekSlotsCache = {}

const disciplinas = ['Danza', 'Musica', 'Teatro']

const caracteristicasPorDisciplina = {
  'Danza': [
    { key: 'espejos', label: 'Espejos' },
    { key: 'barraBallet', label: 'Barra ballet' },
    { key: 'pisoMadera', label: 'Piso madera' },
    { key: 'pisoFlotante', label: 'Piso flotante' },
    { key: 'aireAcondicionado', label: 'Aire acondicionado' },
    { key: 'sonido', label: 'Equipo de música' }
  ],
  'Musica': [
    { key: 'insonorizado', label: 'Insonorizado' },
    { key: 'sonido', label: 'Equipo de sonido' },
    { key: 'amplificacion', label: 'Amplificacion' },
    { key: 'piano', label: 'Piano' },
    { key: 'guitarra', label: 'Guitarra' },
    { key: 'bateria', label: 'Bateria' },
    { key: 'grabacion', label: 'Grabacion' }
  ],
  'Teatro': [
    { key: 'espejos', label: 'Espejos' },
    { key: 'aireAcondicionado', label: 'Aire acondicionado' },
    { key: 'calefaccion', label: 'Calefaccion' },
    { key: 'sonido', label: 'Equipo de sonido' },
    { key: 'amplificacion', label: 'Amplificacion' }
  ]
}

const caracteristicasFiltradas = computed(() => {
  return caracteristicasPorDisciplina[filtros.value.disciplina] || []
})

const regiones = [
  'XV - Arica y Parinacota', 'I - Tarapaca', 'II - Antofagasta', 'III - Atacama',
  'IV - Coquimbo', 'V - Valparaiso', 'RM - Metropolitana', "VI - O'Higgins",
  'VII - Maule', 'XVI - Nuble', 'VIII - Biobio', 'IX - La Araucania',
  'XIV - Los Rios', 'X - Los Lagos', 'XI - Aysen', 'XII - Magallanes'
]

function toggleDisciplina(d) {
  if (filtros.value.disciplina === d) {
    filtros.value.disciplina = ''
    filtros.value.caracteristicas = []
  } else {
    filtros.value.disciplina = d
    filtros.value.caracteristicas = []
  }
}

function getCaracteristicasDanza(room) {
  const tags = []
  if (room.hasMirrors) tags.push('Espejos')
  if (room.tieneBarraBallet) tags.push('Barra ballet')
  if (room.floorType) tags.push('Piso ' + room.floorType)
  if (room.tieneAireAcondicionado) tags.push('Aire AC')
  return tags
}

function getCaracteristicasMusica(room) {
  const tags = []
  if (room.tieneInsonorizacion) tags.push('Insonorizado')
  if (room.hasSound) tags.push('Sonido')
  if (room.tieneAmplificacion) tags.push('Amplificacion')
  if (room.tienePiano) tags.push('Piano')
  if (room.tieneGuitarra) tags.push('Guitarra')
  if (room.tieneBateria) tags.push('Bateria')
  if (room.tieneEquipoGrabacion) tags.push('Grabacion')
  return tags
}

function getMonday(d) {
  const date = new Date(d)
  const day = date.getDay() || 7
  date.setDate(date.getDate() - day + 1)
  date.setHours(0, 0, 0, 0)
  return date
}

const calendarWeekDays = computed(() => {
  const result = []
  // Las columnas siguen la semana seleccionada de la sala expandida (no la semana
  // actual fija), para que Anterior/Siguiente muestre los días correctos.
  const base = (expandedRoom.value && roomWeekStarts.value[expandedRoom.value]) || new Date()
  const monday = new Date(getMonday(new Date(base)))
  const dowLabels = ['Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab', 'Dom']
  for (let i = 0; i < 7; i++) {
    const d = new Date(monday)
    d.setDate(monday.getDate() + i)
    result.push({
      date: d.toLocaleDateString('en-CA'),
      dateFormatted: d.toLocaleDateString('es-CL', { day: 'numeric', month: 'short' }),
      label: dowLabels[i]
    })
  }
  return result
})

const calendarTimeBlocks = computed(() => {
  const blocks = []
  for (let h = 8; h <= 21; h++) {
    const start = `${String(h).padStart(2, '0')}:00`
    blocks.push({ key: start, label: start, start })
  }
  return blocks
})

function getRoomWeekStart(roomId) {
  if (!roomWeekStarts.value[roomId]) {
    roomWeekStarts.value[roomId] = getMonday(new Date())
  }
  return roomWeekStarts.value[roomId]
}

function getRoomWeekLabel(roomId) {
  const monday = getRoomWeekStart(roomId)
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)
  return `Semana del ${monday.toLocaleDateString('es-CL', { day: 'numeric', month: 'long' })} al ${sunday.toLocaleDateString('es-CL', { day: 'numeric', month: 'long' })}`
}

function getRoomSlotsForCell(roomId, dateStr, block) {
  const slots = roomSlots.value[roomId] || []
  return slots.filter(s => {
    if (!s.startTime) return false
    const d = new Date(s.startTime)
    const slotDate = d.toLocaleDateString('en-CA')
    const slotHour = d.getHours().toString().padStart(2, '0') + ':00'
    return slotDate === dateStr && slotHour === block.start
  })
}

async function loadRoomCalendar(roomId) {
  roomLoading.value = roomId
  try {
    const monday = getRoomWeekStart(roomId)
    const sunday = new Date(monday)
    sunday.setDate(monday.getDate() + 6)
    sunday.setHours(23, 59, 59, 999)
    const slots = await scheduleService.getRoomSchedule(roomId, monday.toISOString(), sunday.toISOString())
    // Solo los bloques disponibles y futuros son reservables: un bloque cuyo inicio
    // ya pasó no debe ofrecerse (el backend también lo rechaza al crear la reserva).
    const ahora = Date.now()
    const disponibles = (Array.isArray(slots) ? slots : []).filter(
      s => s.status === 'AVAILABLE' && new Date(s.startTime).getTime() > ahora
    )
    // Deduplicar por hora de inicio: defensa ante bloques duplicados en la BD
    // (el generador puede duplicar si aún no existe el constraint UNIQUE).
    const vistos = new Set()
    roomSlots.value[roomId] = disponibles.filter(s => {
      if (vistos.has(s.startTime)) return false
      vistos.add(s.startTime)
      return true
    })

    // Si la semana mostrada no tiene cupos, saltar (una vez) a la primera semana
    // con disponibilidad real, para no mostrar una semana vacía.
    if (roomSlots.value[roomId].length === 0 && !autoSaltadas.has(roomId)) {
      autoSaltadas.add(roomId)
      const next = await scheduleService.getNextAvailableBlock(roomId, new Date().toISOString())
      if (next?.startTime) {
        const nextMonday = getMonday(new Date(next.startTime))
        const currentMonday = getRoomWeekStart(roomId)
        if (nextMonday.getTime() !== currentMonday.getTime()) {
          roomWeekStarts.value[roomId] = nextMonday
          await loadRoomCalendar(roomId)
          return
        }
      }
    }
  } catch {
    roomSlots.value[roomId] = []
  }
  roomLoading.value = null
}

// La semana actual es el límite inferior: no se permite navegar al pasado.
function puedeRetrocederSemana(roomId) {
  return getRoomWeekStart(roomId).getTime() > getMonday(new Date()).getTime()
}

function prevRoomWeek(roomId) {
  if (!puedeRetrocederSemana(roomId)) return
  const current = getRoomWeekStart(roomId)
  const d = new Date(current)
  d.setDate(d.getDate() - 7)
  roomWeekStarts.value[roomId] = d
  loadRoomCalendar(roomId)
}

function nextRoomWeek(roomId) {
  const current = getRoomWeekStart(roomId)
  const d = new Date(current)
  d.setDate(d.getDate() + 7)
  roomWeekStarts.value[roomId] = d
  loadRoomCalendar(roomId)
}

function toggleVenue(id) {
  expandedVenue.value = expandedVenue.value === id ? null : id
  expandedRoom.value = null
}

async function toggleRoom(roomId) {
  if (expandedRoom.value === roomId) {
    expandedRoom.value = null
    if (selectionRoom.value?.id === roomId) {
      selectedSlots.value = []
      selectionRoom.value = null
      selectionVenue.value = null
    }
    return
  }
  expandedRoom.value = roomId
  if (!roomSlots.value[roomId]) {
    await loadRoomCalendar(roomId)
  }
}

function onRegionChange() {
  filtros.value.comuna = ''
  buscar()
}

async function buscar() {
  loading.value = true
  expandedVenue.value = null
  expandedRoom.value = null
  roomSlots.value = {}
  selectedSlots.value = []
  selectionRoom.value = null
  selectionVenue.value = null
  try {
    const data = await classService.getVenues()
    let list = Array.isArray(data) ? data : data?.content || []

    if (filtros.value.comuna) {
      const q = filtros.value.comuna.toLowerCase()
      list = list.filter(v => (v.city || '').toLowerCase().includes(q))
    }

    const venuesWithRooms = []
    for (const v of list) {
      try {
        const rooms = await venueService.getRooms(v.id)
        let filteredRooms = rooms

        if (filtros.value.caracteristicas.length > 0) {
          filteredRooms = filteredRooms.filter(room => {
            return filtros.value.caracteristicas.every(key => {
              switch (key) {
                case 'espejos': return room.hasMirrors
                case 'barraBallet': return room.tieneBarraBallet
                case 'pisoMadera': return (room.floorType || '').toLowerCase().includes('madera') || (room.floorType || '').toLowerCase().includes('wood')
                case 'pisoFlotante': return (room.floorType || '').toLowerCase().includes('flotante') || (room.floorType || '').toLowerCase().includes('floating')
                case 'aireAcondicionado': return room.tieneAireAcondicionado
                case 'insonorizado': return room.tieneInsonorizacion
                case 'sonido': return room.hasSound
                case 'amplificacion': return room.tieneAmplificacion
                case 'piano': return room.tienePiano
                case 'guitarra': return room.tieneGuitarra
                case 'bateria': return room.tieneBateria
                case 'grabacion': return room.tieneEquipoGrabacion
                case 'calefaccion': return room.tieneCalefaccion
                default: return true
              }
            })
          })
        }

        if (filtros.value.disciplina) {
          filteredRooms = filteredRooms.filter(room => {
            const charDan = getCaracteristicasDanza(room).length
            const charMus = getCaracteristicasMusica(room).length
            if (filtros.value.disciplina === 'Danza') return charDan > 0
            if (filtros.value.disciplina === 'Musica') return charMus > 0
            return true
          })
        }

        if (filteredRooms.length > 0) {
          venuesWithRooms.push({ ...v, rooms: filteredRooms, region: v.region || '' })
        }
      } catch (err) {
        console.error('Error al cargar salas de la sede', err)
      }
    }
    venues.value = venuesWithRooms
  } catch { venues.value = [] }
  loading.value = false
}

function toggleSlot(room, venue, slot) {
  if (selectionRoom.value?.id !== room.id) {
    selectedSlots.value = []
    selectionRoom.value = room
    selectionVenue.value = venue
  }
  const idx = selectedSlots.value.findIndex(s => s.id === slot.id)
  if (idx >= 0) {
    selectedSlots.value.splice(idx, 1)
  } else {
    selectedSlots.value.push(slot)
  }
}

function isSlotSelected(slotId) {
  return selectedSlots.value.some(s => s.id === slotId)
}

function abrirModal() {
  if (!identidadValidada.value) {
    alertaIdentidad.value = true
    return
  }
  if (!selectedSlots.value.length) return
  const sorted = [...selectedSlots.value].sort((a, b) => a.startTime.localeCompare(b.startTime))
  modal.value = { abierto: true, venue: selectionVenue.value, room: selectionRoom.value, slots: sorted, procesando: false }
}

// Inicia el pago de la reserva con MercadoPago: crea la preferencia (con split a
// la sede) y redirige al checkout. La reserva se materializa en el webhook cuando
// MercadoPago aprueba el pago; los bloques quedan en HELD mientras tanto.
async function pagar() {
  modal.value.procesando = true
  try {
    const slots = modal.value.slots || []
    const room = modal.value.room
    const blockIds = slots.map(s => s.blockId || s.id).filter(Boolean)
    if (!blockIds.length) throw new Error('sin bloques')

    const { initPoint } = await paymentService.reserveRoomPreference(
      room.id, blockIds, borradorId.value || null
    )
    if (initPoint) { window.location.href = initPoint; return }
    throw new Error('sin initPoint')
  } catch (e) {
    modal.value.procesando = false
    toast.error(
      e?.response?.data?.error || e?.response?.data?.message || 'No se pudo iniciar el pago de la reserva'
    )
  }
}

onMounted(async () => {
  await syncAtributos()
  await buscar()
})
</script>

<style scoped>
.equip-tag {
  @apply text-xs text-gray-400 bg-white/5 border border-white/10 px-2 py-0.5 rounded cursor-default select-none;
}
</style>