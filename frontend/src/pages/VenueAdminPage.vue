<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-white">Administración de Sede</h1>
      <p class="text-gray-400 text-sm mt-0.5">Gestiona tus sedes, salas y confirma clases.</p>
    </div>

    <!-- Tabs -->
    <div class="flex gap-1 bg-[#161824] rounded-xl border border-white/10 p-1 w-fit">
      <button @click="activeTab = 'venues'" :class="activeTab === 'venues' ? 'bg-indigo-600 text-white' : 'text-gray-400 hover:text-white'"
        class="px-4 py-1.5 rounded-lg text-sm font-medium transition-colors">
        Mis Sedes
      </button>
      <button @click="activeTab = 'classes'; loadPendingClasses()"
        :class="activeTab === 'classes' ? 'bg-indigo-600 text-white' : 'text-gray-400 hover:text-white'"
        class="px-4 py-1.5 rounded-lg text-sm font-medium transition-colors flex items-center gap-2">
        Clases por Confirmar
        <span v-if="pendingClasses.length > 0" class="text-xs bg-amber-500 text-white px-1.5 py-0.5 rounded-full">{{ pendingClasses.length }}</span>
      </button>
    </div>

    <!-- Mis Sedes -->
    <div v-if="activeTab === 'venues'">
      <div v-if="loading" class="space-y-3">
        <div v-for="i in 3" :key="i" class="h-32 bg-[#161824] rounded-xl border border-white/10 animate-pulse"></div>
      </div>
      <div v-else-if="error" class="bg-red-500/10 border border-red-500/20 rounded-xl p-4">
        <p class="text-sm text-red-400">{{ error }}</p>
      </div>
      <div v-else-if="venues.length === 0" class="text-center py-14 bg-[#161824] rounded-xl border border-white/10">
        <p class="text-gray-400 text-sm">No tienes sedes registradas.</p>
        <router-link to="/sede/registro" class="inline-block mt-2 text-sm text-indigo-400 hover:text-indigo-300 transition-colors">Registrar una sede</router-link>
      </div>
      <div v-else class="space-y-4">
        <div v-for="venue in venues" :key="venue.id" class="bg-[#161824] rounded-xl border border-white/10">
          <div class="p-5 flex flex-col md:flex-row md:items-start md:justify-between gap-3">
            <div>
              <h3 class="font-semibold text-white">{{ venue.name }}</h3>
              <p class="text-sm text-gray-400 mt-0.5">{{ venue.address }}</p>
              <p class="text-xs text-gray-500 mt-0.5">{{ venue.email }} · {{ venue.phone }}</p>
              <span class="mt-2 inline-block px-2.5 py-0.5 rounded-full text-xs font-medium"
                :class="venue.status === 'APPROVED' ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : venue.status === 'REJECTED' ? 'bg-red-500/20 text-red-400 border border-red-500/30' : 'bg-amber-500/20 text-amber-400 border border-amber-500/30'">
                {{ venue.status }}
              </span>
            </div>
            <div class="flex gap-2 flex-shrink-0">
              <button @click="openEditVenue(venue)" class="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-xs font-medium transition-colors">Editar</button>
              <button @click="loadRooms(venue.id)" class="px-3 py-1.5 bg-white/10 hover:bg-white/15 text-white rounded-lg text-xs font-medium transition-colors">
                {{ expandedVenueId === venue.id ? 'Ocultar salas' : 'Ver salas' }}
              </button>
            </div>
          </div>

          <div v-if="expandedVenueId === venue.id" class="border-t border-white/5 p-5 space-y-3">
            <div class="flex items-center justify-between">
              <h4 class="text-sm font-semibold text-white">Salas</h4>
              <button @click="showRoomForm = true" class="text-xs text-indigo-400 hover:text-indigo-300 transition-colors">+ Nueva Sala</button>
            </div>
            <div v-if="roomsLoading" class="text-xs text-gray-500">Cargando salas...</div>
            <div v-else-if="rooms.length === 0" class="text-xs text-gray-500">No hay salas registradas.</div>
            <div v-else class="space-y-2">
              <div v-for="room in rooms" :key="room.id" class="bg-[#0d0f1a] rounded-lg border border-white/10">
                <div class="flex items-center justify-between px-4 py-3">
                  <div>
                    <p class="text-sm font-medium text-white">{{ room.name }}</p>
                    <p class="text-xs text-gray-500">Cap. {{ room.capacity }} · {{ room.floorType || 'Sin tipo de piso' }}</p>
                  </div>
                  <button @click="toggleRoomAvailability(room)" class="text-xs text-indigo-400 hover:text-indigo-300 transition-colors">
                    {{ expandedRoomId === room.id ? 'Ocultar horarios' : 'Ver horarios' }}
                  </button>
                </div>
                <div v-if="expandedRoomId === room.id" class="border-t border-white/5 px-4 py-3 space-y-2">
                  <h6 class="text-xs font-semibold text-gray-400 uppercase tracking-wider">Bloques horarios</h6>
                  <div v-if="availabilityLoading[room.id]" class="text-xs text-gray-500">Cargando...</div>
                  <div v-else-if="!roomAvailabilities[room.id]?.length" class="text-xs text-gray-500">Sin bloques configurados.</div>
                  <div v-else class="space-y-1">
                    <div v-for="avail in roomAvailabilities[room.id]" :key="avail.id"
                      class="flex items-center justify-between rounded-lg bg-white/5 px-3 py-2 text-xs">
                      <span class="text-gray-300">{{ formatDateTime(avail.startTime) }} — {{ formatDateTime(avail.endTime) }}</span>
                      <button @click="deleteAvailability(room.id, avail.id)" class="text-red-400 hover:text-red-300 transition-colors">Eliminar</button>
                    </div>
                  </div>
                  <div class="flex gap-2 mt-2">
                    <input v-if="availForm[room.id]" v-model="availForm[room.id].startTime" type="datetime-local"
                      class="flex-1 px-2 py-1.5 bg-[#161824] border border-white/15 rounded-lg text-xs text-white [color-scheme:dark] focus:outline-none focus:ring-1 focus:ring-indigo-500" />
                    <input v-if="availForm[room.id]" v-model="availForm[room.id].endTime" type="datetime-local"
                      class="flex-1 px-2 py-1.5 bg-[#161824] border border-white/15 rounded-lg text-xs text-white [color-scheme:dark] focus:outline-none focus:ring-1 focus:ring-indigo-500" />
                    <button v-if="availForm[room.id]" @click="submitAvailability(room.id)"
                      class="px-3 py-1.5 bg-sky-600 hover:bg-sky-500 text-white rounded-lg text-xs font-medium transition-colors">+ Agregar</button>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="showRoomForm" class="bg-[#0d0f1a] rounded-xl border border-white/10 p-4 space-y-3">
              <h5 class="text-sm font-semibold text-white">Nueva Sala</h5>
              <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <input v-model="roomForm.name" placeholder="Nombre"
                  class="px-3 py-2 bg-[#161824] border border-white/15 rounded-lg text-sm text-white placeholder-gray-500 focus:outline-none focus:ring-1 focus:ring-indigo-500" />
                <input v-model.number="roomForm.capacity" type="number" placeholder="Capacidad"
                  class="px-3 py-2 bg-[#161824] border border-white/15 rounded-lg text-sm text-white placeholder-gray-500 focus:outline-none focus:ring-1 focus:ring-indigo-500" />
                <input v-model="roomForm.floorType" placeholder="Tipo de piso"
                  class="px-3 py-2 bg-[#161824] border border-white/15 rounded-lg text-sm text-white placeholder-gray-500 focus:outline-none focus:ring-1 focus:ring-indigo-500" />
              </div>
              <div class="flex gap-2">
                <button @click="submitRoom(venue.id)" class="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-medium transition-colors">Guardar</button>
                <button @click="showRoomForm = false" class="px-3 py-1.5 border border-white/15 text-gray-300 rounded-lg text-xs hover:bg-white/5 transition-colors">Cancelar</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Clases por confirmar -->
    <div v-if="activeTab === 'classes'">
      <div v-if="classesLoading" class="space-y-3">
        <div v-for="i in 3" :key="i" class="h-28 bg-[#161824] rounded-xl border border-white/10 animate-pulse"></div>
      </div>
      <div v-else-if="pendingClasses.length === 0" class="text-center py-14 bg-[#161824] rounded-xl border border-white/10">
        <p class="text-gray-400 text-sm">No hay clases pendientes de confirmación.</p>
      </div>
      <div v-else class="space-y-3">
        <div v-for="cls in pendingClasses" :key="cls.id"
          class="bg-[#161824] rounded-xl border border-white/10 p-5 flex flex-col md:flex-row md:items-start md:justify-between gap-4">
          <div>
            <h3 class="font-semibold text-white">{{ cls.title }}</h3>
            <p class="text-xs text-gray-500 mt-1">{{ cls.discipline }} · {{ cls.venueName }} · {{ cls.roomName }}</p>
            <p class="text-xs text-gray-500">{{ formatDateTime(cls.startTime) }} → {{ formatDateTime(cls.endTime) }}</p>
            <p class="text-xs text-gray-500">Cupo: {{ cls.capacity }} · ${{ cls.price?.toLocaleString('es-CL') }}</p>
            <p class="text-xs text-emerald-400 mt-1 font-medium">Asistentes: {{ cls.attendanceCount }}</p>
          </div>
          <div class="flex gap-2 flex-shrink-0">
            <button @click="openConfirmDialog(cls, 'realized')"
              class="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-medium transition-colors">
              Clase Realizada
            </button>
            <button @click="openConfirmDialog(cls, 'not-realized')"
              class="px-3 py-1.5 bg-red-600 hover:bg-red-500 text-white rounded-lg text-xs font-medium transition-colors">
              No Realizada
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Editar Sede -->
    <div v-if="showEditVenue" class="fixed inset-0 z-50 flex items-center justify-center bg-black/70" @click.self="showEditVenue = false">
      <div class="bg-[#161824] rounded-2xl border border-white/10 p-6 w-full max-w-md mx-4 shadow-2xl space-y-4">
        <h3 class="text-lg font-bold text-white">Editar Sede</h3>
        <div class="space-y-3">
          <input v-model="editVenueForm.name" placeholder="Nombre"
            class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-sm text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          <input v-model="editVenueForm.address" placeholder="Dirección"
            class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-sm text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          <input v-model="editVenueForm.email" placeholder="Email"
            class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-sm text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          <input v-model="editVenueForm.phone" placeholder="Teléfono"
            class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-sm text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          <textarea v-model="editVenueForm.description" rows="2" placeholder="Descripción"
            class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-sm text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 resize-none" />
        </div>
        <div class="flex gap-2 pt-1">
          <button @click="showEditVenue = false" class="flex-1 py-2.5 border border-white/15 text-gray-300 rounded-lg text-sm hover:bg-white/5 transition-colors">Cancelar</button>
          <button @click="submitEditVenue" class="flex-1 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-sm font-medium transition-colors">Guardar</button>
        </div>
      </div>
    </div>

    <!-- Modal Confirmar clase -->
    <ConfirmModal
      v-model="showConfirmDialog"
      :title="confirmType === 'realized' ? 'Confirmar Clase Realizada' : 'Confirmar No Realizada'"
      :message="confirmMessage"
      confirm-text="Confirmar"
      :type="confirmType === 'realized' ? 'info' : 'danger'"
      @confirm="executeConfirmation"
    />

    <!-- Modal Resultado -->
    <div v-if="confirmationResult" class="fixed inset-0 z-50 flex items-center justify-center bg-black/70" @click.self="confirmationResult = null">
      <div class="bg-[#161824] rounded-2xl border border-white/10 p-6 w-full max-w-sm mx-4 text-center shadow-2xl">
        <div class="mx-auto mb-4 flex w-14 h-14 items-center justify-center rounded-full"
          :class="confirmationResult.success ? 'bg-emerald-500/20' : 'bg-red-500/20'">
          <svg class="w-7 h-7" :class="confirmationResult.success ? 'text-emerald-400' : 'text-red-400'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path v-if="confirmationResult.success" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </div>
        <h3 class="text-lg font-bold text-white mb-2">{{ confirmationResult.message }}</h3>
        <p class="text-sm text-gray-400 mb-5">{{ confirmationResult.detail }}</p>
        <button @click="confirmationResult = null" class="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-sm font-medium transition-colors">Aceptar</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { venueService } from '../services/venueService'
import ConfirmModal from '../components/ConfirmModal.vue'

const activeTab = ref('venues')
const loading = ref(true)
const error = ref('')
const venues = ref([])
const expandedVenueId = ref(null)
const rooms = ref([])
const roomsLoading = ref(false)
const showRoomForm = ref(false)
const roomForm = ref({ name: '', capacity: null, floorType: '' })
const showEditVenue = ref(false)
const editVenueForm = ref({ id: '', name: '', address: '', email: '', phone: '', description: '' })
const pendingClasses = ref([])
const classesLoading = ref(false)
const expandedRoomId = ref(null)
const roomAvailabilities = ref({})
const availabilityLoading = ref({})
const availForm = ref({})
const showConfirmDialog = ref(false)
const confirmType = ref('')
const confirmClass = ref(null)
const confirmMessage = ref('')
const confirmationResult = ref(null)

onMounted(async () => {
  try { venues.value = await venueService.getMyVenues() } catch { error.value = 'No se pudieron cargar las sedes.' }
  finally { loading.value = false }
})

const loadRooms = async (venueId) => {
  if (expandedVenueId.value === venueId) { expandedVenueId.value = null; return }
  expandedVenueId.value = venueId; roomsLoading.value = true
  try { rooms.value = await venueService.getRooms(venueId) } catch { rooms.value = [] }
  finally { roomsLoading.value = false }
}

const submitRoom = async (venueId) => {
  try {
    await venueService.createRoom(venueId, roomForm.value)
    showRoomForm.value = false
    roomForm.value = { name: '', capacity: null, floorType: '' }
    rooms.value = await venueService.getRooms(venueId)
  } catch { alert('Error al crear la sala.') }
}

const openEditVenue = (venue) => {
  editVenueForm.value = { id: venue.id, name: venue.name, address: venue.address, email: venue.email, phone: venue.phone, description: venue.description }
  showEditVenue.value = true
}

const submitEditVenue = async () => {
  try {
    const { id, ...data } = editVenueForm.value
    await venueService.updateVenue(id, data)
    showEditVenue.value = false
    venues.value = await venueService.getMyVenues()
  } catch { alert('Error al actualizar la sede.') }
}

const loadPendingClasses = async () => {
  classesLoading.value = true
  try { pendingClasses.value = await venueService.getClassesPendingConfirmation() } catch { pendingClasses.value = [] }
  finally { classesLoading.value = false }
}

const openConfirmDialog = (cls, type) => {
  confirmClass.value = cls; confirmType.value = type
  confirmMessage.value = type === 'realized'
    ? `La clase "${cls.title}" será marcada como REALIZADA. Se liberarán los pagos retenidos.`
    : `La clase "${cls.title}" será marcada como NO REALIZADA. Se iniciará el proceso de reagendamiento y devolución.`
  showConfirmDialog.value = true
}

const executeConfirmation = async () => {
  try {
    if (confirmType.value === 'realized') {
      const result = await venueService.confirmClassRealized(confirmClass.value.id)
      confirmationResult.value = { success: true, message: 'Operación exitosa', detail: `Clase confirmada como realizada. ${result.affectedPayments} pago(s) liberado(s).` }
    } else {
      await venueService.confirmClassNotRealized(confirmClass.value.id)
      confirmationResult.value = { success: true, message: 'Operación exitosa', detail: `Clase marcada como no realizada. Se notificó al profesor para reagendamiento.` }
    }
    pendingClasses.value = await venueService.getClassesPendingConfirmation()
  } catch (e) {
    confirmationResult.value = { success: false, message: 'Error en la operación', detail: e.response?.data?.message || 'No se pudo procesar la confirmación.' }
  }
}

const toggleRoomAvailability = async (room) => {
  if (expandedRoomId.value === room.id) { expandedRoomId.value = null; return }
  expandedRoomId.value = room.id; availabilityLoading.value[room.id] = true
  try {
    roomAvailabilities.value[room.id] = await venueService.getRoomAvailability(room.id)
    availForm.value[room.id] = { startTime: '', endTime: '' }
  } catch { roomAvailabilities.value[room.id] = [] }
  finally { availabilityLoading.value[room.id] = false }
}

const submitAvailability = async (roomId) => {
  const form = availForm.value[roomId]
  if (!form?.startTime || !form?.endTime) return
  try {
    await venueService.createRoomAvailability(roomId, { roomId, startTime: new Date(form.startTime).toISOString(), endTime: new Date(form.endTime).toISOString() })
    roomAvailabilities.value[roomId] = await venueService.getRoomAvailability(roomId)
    availForm.value[roomId] = { startTime: '', endTime: '' }
  } catch (e) { alert(e.response?.data?.message || 'Error al crear el bloque horario.') }
}

const deleteAvailability = async (roomId, availabilityId) => {
  if (!confirm('¿Eliminar este bloque horario?')) return
  try {
    await venueService.deleteRoomAvailability(roomId, availabilityId)
    roomAvailabilities.value[roomId] = await venueService.getRoomAvailability(roomId)
  } catch { alert('Error al eliminar el bloque.') }
}

const formatDateTime = (d) => d ? new Date(d).toLocaleDateString('es-CL', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''
</script>
