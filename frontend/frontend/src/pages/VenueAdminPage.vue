<template>
  <div class="mx-auto max-w-6xl space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">Panel de Sede</h1>
      <p class="text-sm text-gray-600">Gestiona tus sedes, salas y confirma clases.</p>
    </div>

    <div class="flex gap-2 border-b">
      <button @click="activeTab = 'venues'" :class="activeTab === 'venues' ? 'border-indigo-600 text-indigo-600' : 'border-transparent text-gray-500'" class="border-b-2 px-4 py-2 text-sm font-medium">Mis Sedes</button>
      <button @click="activeTab = 'classes'; loadPendingClasses()" :class="activeTab === 'classes' ? 'border-indigo-600 text-indigo-600' : 'border-transparent text-gray-500'" class="border-b-2 px-4 py-2 text-sm font-medium">
        Clases por Confirmar
        <span v-if="pendingClasses.length > 0" class="ml-1 rounded-full bg-amber-100 px-2 py-0.5 text-xs text-amber-800">{{ pendingClasses.length }}</span>
      </button>
    </div>

    <div v-if="activeTab === 'venues'">
      <div v-if="loading" class="rounded-xl border bg-white p-6 text-sm text-gray-500">Cargando sedes...</div>
      <div v-else-if="error" class="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{{ error }}</div>
      <div v-else-if="venues.length === 0" class="rounded-xl border bg-white p-6 text-center text-sm text-gray-500">
        No tienes sedes registradas.
        <router-link to="/venues/register" class="ml-1 font-medium text-indigo-600 hover:underline">Registrar una sede</router-link>
      </div>

      <div v-else class="space-y-4">
        <div v-for="venue in venues" :key="venue.id" class="rounded-xl border bg-white p-5">
          <div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
            <div>
              <h3 class="text-lg font-semibold text-gray-900">{{ venue.name }}</h3>
              <p class="text-sm text-gray-600">{{ venue.address }}</p>
              <p class="text-sm text-gray-500">{{ venue.email }} - {{ venue.phone }}</p>
              <span :class="statusBadge(venue.status)" class="mt-1 inline-block rounded-full px-2 py-0.5 text-xs font-medium">
                {{ venue.status }}
              </span>
            </div>
            <div class="flex gap-2">
              <button @click="openEditVenue(venue)" class="rounded-md bg-indigo-600 px-3 py-1.5 text-sm text-white hover:bg-indigo-700">Editar</button>
              <button @click="loadRooms(venue.id)" class="rounded-md bg-sky-600 px-3 py-1.5 text-sm text-white hover:bg-sky-700">Ver Salas</button>
            </div>
          </div>

          <div v-if="expandedVenueId === venue.id" class="mt-4 border-t pt-4">
            <div class="mb-3 flex items-center justify-between">
              <h4 class="font-medium text-gray-900">Salas</h4>
              <button @click="showRoomForm = true" class="text-sm font-medium text-indigo-600 hover:text-indigo-700">+ Nueva Sala</button>
            </div>

            <div v-if="roomsLoading" class="text-sm text-gray-500">Cargando salas...</div>
            <div v-else-if="rooms.length === 0" class="text-sm text-gray-500">No hay salas registradas.</div>
            <ul v-else class="space-y-2">
              <li v-for="room in rooms" :key="room.id" class="rounded-lg border bg-gray-50">
                <div class="flex items-center justify-between p-3">
                  <div>
                    <p class="font-medium text-gray-900">{{ room.name }}</p>
                    <p class="text-sm text-gray-600">Capacidad: {{ room.capacity }} - {{ room.floorType || 'Sin tipo' }}</p>
                  </div>
                  <button @click="toggleRoomAvailability(room)" class="text-sm font-medium text-sky-600 hover:text-sky-700">
                    {{ expandedRoomId === room.id ? 'Ocultar horarios' : 'Ver horarios' }}
                  </button>
                </div>
                <div v-if="expandedRoomId === room.id" class="border-t bg-white p-3">
                  <h6 class="mb-2 text-sm font-medium text-gray-700">Bloques horarios disponibles</h6>
                  <div v-if="availabilityLoading[room.id]" class="text-xs text-gray-500">Cargando...</div>
                  <div v-else-if="!roomAvailabilities[room.id]?.length" class="text-xs text-gray-500">Sin bloques configurados.</div>
                  <ul v-else class="mb-3 space-y-1">
                    <li v-for="avail in roomAvailabilities[room.id]" :key="avail.id" class="flex items-center justify-between rounded bg-gray-50 px-2 py-1 text-xs">
                      <span>{{ formatDateTime(avail.startTime) }} - {{ formatDateTime(avail.endTime) }}</span>
                      <button @click="deleteAvailability(room.id, avail.id)" class="text-rose-600 hover:text-rose-700">Eliminar</button>
                    </li>
                  </ul>
                  <div class="flex gap-2">
                    <input v-if="availForm[room.id]" v-model="availForm[room.id].startTime" type="datetime-local" class="rounded-lg border px-2 py-1 text-xs" />
                    <input v-if="availForm[room.id]" v-model="availForm[room.id].endTime" type="datetime-local" class="rounded-lg border px-2 py-1 text-xs" />
                    <button v-if="availForm[room.id]" @click="submitAvailability(room.id)" class="rounded-md bg-sky-600 px-2 py-1 text-xs text-white hover:bg-sky-700">+ Agregar</button>
                  </div>
                </div>
              </li>
            </ul>

            <div v-if="showRoomForm" class="mt-4 rounded-lg border bg-gray-50 p-4">
              <h5 class="mb-2 font-medium text-gray-900">Nueva Sala</h5>
              <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <input v-model="roomForm.name" placeholder="Nombre" class="rounded-lg border px-3 py-2 text-sm" />
                <input v-model.number="roomForm.capacity" type="number" placeholder="Capacidad" class="rounded-lg border px-3 py-2 text-sm" />
                <input v-model="roomForm.floorType" placeholder="Tipo de piso" class="rounded-lg border px-3 py-2 text-sm" />
              </div>
              <div class="mt-3 flex gap-2">
                <button @click="submitRoom(venue.id)" class="rounded-md bg-emerald-600 px-3 py-1.5 text-sm text-white hover:bg-emerald-700">Guardar</button>
                <button @click="showRoomForm = false" class="rounded-md border px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-100">Cancelar</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="activeTab === 'classes'">
      <div v-if="classesLoading" class="rounded-xl border bg-white p-6 text-sm text-gray-500">Cargando clases...</div>
      <div v-else-if="pendingClasses.length === 0" class="rounded-xl border bg-white p-6 text-center text-sm text-gray-500">
        No hay clases pendientes de confirmacion.
      </div>
      <div v-else class="space-y-4">
        <div v-for="cls in pendingClasses" :key="cls.id" class="rounded-xl border bg-white p-5">
          <div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
            <div>
              <h3 class="text-lg font-semibold text-gray-900">{{ cls.title }}</h3>
              <p class="text-sm text-gray-600">{{ cls.discipline }}</p>
              <p class="text-sm text-gray-500">{{ cls.venueName }} - {{ cls.roomName }}</p>
              <p class="text-sm text-gray-500">{{ formatDateTime(cls.startTime) }} a {{ formatDateTime(cls.endTime) }}</p>
              <p class="text-sm text-gray-500">Cupo: {{ cls.capacity }} - Precio: ${{ cls.price?.toLocaleString('es-CL') }}</p>
              <p class="mt-1 text-sm font-medium text-emerald-700">Asistentes marcados: {{ cls.attendanceCount }}</p>
            </div>
            <div class="flex gap-2">
              <button @click="openConfirmDialog(cls, 'realized')" class="rounded-md bg-emerald-600 px-3 py-1.5 text-sm text-white hover:bg-emerald-700">Clase Realizada</button>
              <button @click="openConfirmDialog(cls, 'not-realized')" class="rounded-md bg-rose-600 px-3 py-1.5 text-sm text-white hover:bg-rose-700">No Realizada</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showEditVenue" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div class="w-full max-w-md rounded-xl bg-white p-6">
        <h3 class="mb-4 text-lg font-semibold">Editar Sede</h3>
        <div class="space-y-3">
          <input v-model="editVenueForm.name" placeholder="Nombre" class="w-full rounded-lg border px-3 py-2 text-sm" />
          <input v-model="editVenueForm.address" placeholder="Direccion" class="w-full rounded-lg border px-3 py-2 text-sm" />
          <input v-model="editVenueForm.email" placeholder="Email" class="w-full rounded-lg border px-3 py-2 text-sm" />
          <input v-model="editVenueForm.phone" placeholder="Telefono" class="w-full rounded-lg border px-3 py-2 text-sm" />
          <textarea v-model="editVenueForm.description" placeholder="Descripcion" rows="2" class="w-full rounded-lg border px-3 py-2 text-sm" />
        </div>
        <div class="mt-4 flex justify-end gap-2">
          <button @click="showEditVenue = false" class="rounded-md border px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">Cancelar</button>
          <button @click="submitEditVenue" class="rounded-md bg-indigo-600 px-4 py-2 text-sm text-white hover:bg-indigo-700">Guardar</button>
        </div>
      </div>
    </div>

    <div v-if="showConfirmDialog" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div class="w-full max-w-md rounded-xl bg-white p-6">
        <h3 class="mb-2 text-lg font-semibold">{{ confirmType === 'realized' ? 'Confirmar Clase Realizada' : 'Confirmar Clase No Realizada' }}</h3>
        <p class="mb-4 text-sm text-gray-600">{{ confirmMessage }}</p>
        <p class="mb-4 text-sm font-medium text-gray-900">Confirma su respuesta?</p>
        <div class="flex justify-end gap-2">
          <button @click="showConfirmDialog = false" class="rounded-md border px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">No</button>
          <button @click="executeConfirmation" class="rounded-md px-4 py-2 text-sm text-white" :class="confirmType === 'realized' ? 'bg-emerald-600 hover:bg-emerald-700' : 'bg-rose-600 hover:bg-rose-700'">Si</button>
        </div>
      </div>
    </div>

    <div v-if="confirmationResult" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div class="w-full max-w-md rounded-xl bg-white p-6 text-center">
        <div class="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full" :class="confirmationResult.success ? 'bg-emerald-100' : 'bg-red-100'">
          <svg v-if="confirmationResult.success" class="h-6 w-6 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
          <svg v-else class="h-6 w-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
        </div>
        <h3 class="mb-2 text-lg font-semibold">{{ confirmationResult.message }}</h3>
        <p class="mb-4 text-sm text-gray-600">{{ confirmationResult.detail }}</p>
        <button @click="confirmationResult = null" class="rounded-md bg-indigo-600 px-4 py-2 text-sm text-white hover:bg-indigo-700">Aceptar</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { venueService } from '../services/venueService'

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
  await loadVenues()
})

const loadVenues = async () => {
  loading.value = true
  error.value = ''
  try {
    venues.value = await venueService.getMyVenues()
  } catch (e) {
    error.value = 'No se pudieron cargar las sedes.'
  } finally {
    loading.value = false
  }
}

const loadRooms = async (venueId) => {
  if (expandedVenueId.value === venueId) {
    expandedVenueId.value = null
    return
  }
  expandedVenueId.value = venueId
  roomsLoading.value = true
  try {
    rooms.value = await venueService.getRooms(venueId)
  } catch (e) {
    rooms.value = []
  } finally {
    roomsLoading.value = false
  }
}

const submitRoom = async (venueId) => {
  try {
    await venueService.createRoom(venueId, roomForm.value)
    showRoomForm.value = false
    roomForm.value = { name: '', capacity: null, floorType: '' }
    await loadRooms(venueId)
  } catch (e) {
    alert('Error al crear la sala.')
  }
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
    await loadVenues()
  } catch (e) {
    alert('Error al actualizar la sede.')
  }
}

const loadPendingClasses = async () => {
  classesLoading.value = true
  try {
    pendingClasses.value = await venueService.getClassesPendingConfirmation()
  } catch (e) {
    pendingClasses.value = []
  } finally {
    classesLoading.value = false
  }
}

const openConfirmDialog = (cls, type) => {
  confirmClass.value = cls
  confirmType.value = type
  confirmMessage.value = type === 'realized'
    ? `La clase "${cls.title}" sera marcada como REALIZADA. Se liberaran ${cls.capacity} pagos retenidos.`
    : `La clase "${cls.title}" sera marcada como NO REALIZADA. Se iniciara el proceso de reagendamiento y devolucion.`
  showConfirmDialog.value = true
}

const executeConfirmation = async () => {
  showConfirmDialog.value = false
  try {
    let result
    if (confirmType.value === 'realized') {
      result = await venueService.confirmClassRealized(confirmClass.value.id)
      confirmationResult.value = {
        success: true,
        message: 'Operacion exitosa',
        detail: `Clase "${confirmClass.value.title}" confirmada como realizada. ${result.affectedPayments} pago(s) liberado(s).`
      }
    } else {
      result = await venueService.confirmClassNotRealized(confirmClass.value.id)
      confirmationResult.value = {
        success: true,
        message: 'Operacion exitosa',
        detail: `Clase "${confirmClass.value.title}" marcada como no realizada. Se notifico al profesor para reagendamiento.`
      }
    }
    await loadPendingClasses()
  } catch (e) {
    confirmationResult.value = {
      success: false,
      message: 'Error en la operacion',
      detail: e.response?.data?.message || 'No se pudo procesar la confirmacion.'
    }
  }
}

const formatDateTime = (instant) => {
  if (!instant) return ''
  const date = new Date(instant)
  return date.toLocaleDateString('es-CL', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

const statusBadge = (status) => {
  if (status === 'APPROVED') return 'bg-emerald-100 text-emerald-700'
  if (status === 'REJECTED') return 'bg-rose-100 text-rose-700'
  return 'bg-amber-100 text-amber-700'
}

const toggleRoomAvailability = async (room) => {
  if (expandedRoomId.value === room.id) {
    expandedRoomId.value = null
    return
  }
  expandedRoomId.value = room.id
  availabilityLoading.value[room.id] = true
  try {
    roomAvailabilities.value[room.id] = await venueService.getRoomAvailability(room.id)
    availForm.value[room.id] = { startTime: '', endTime: '' }
  } catch (e) {
    roomAvailabilities.value[room.id] = []
  } finally {
    availabilityLoading.value[room.id] = false
  }
}

const submitAvailability = async (roomId) => {
  const form = availForm.value[roomId]
  if (!form?.startTime || !form?.endTime) return
  try {
    await venueService.createRoomAvailability(roomId, {
      roomId,
      startTime: new Date(form.startTime).toISOString(),
      endTime: new Date(form.endTime).toISOString()
    })
    roomAvailabilities.value[roomId] = await venueService.getRoomAvailability(roomId)
    availForm.value[roomId] = { startTime: '', endTime: '' }
  } catch (e) {
    alert(e.response?.data?.message || 'Error al crear el bloque horario.')
  }
}

const deleteAvailability = async (roomId, availabilityId) => {
  if (!confirm('Eliminar este bloque horario?')) return
  try {
    await venueService.deleteRoomAvailability(roomId, availabilityId)
    roomAvailabilities.value[roomId] = await venueService.getRoomAvailability(roomId)
  } catch (e) {
    alert('Error al eliminar el bloque.')
  }
}
</script>
