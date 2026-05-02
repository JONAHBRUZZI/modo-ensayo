<template>
  <div class="space-y-6">
    <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
      <h1 class="text-3xl font-bold text-gray-900">Panel Admin General</h1>
      <div class="flex flex-wrap gap-2">
        <router-link to="/admin/roles" class="rounded-lg bg-indigo-600 px-4 py-2 text-sm text-white hover:bg-indigo-700">
          Gestionar Roles
        </router-link>
        <router-link to="/venues/register" class="rounded-lg bg-emerald-600 px-4 py-2 text-sm text-white hover:bg-emerald-700">
          Registrar Sede
        </router-link>
        <router-link to="/rooms/register" class="rounded-lg bg-sky-600 px-4 py-2 text-sm text-white hover:bg-sky-700">
          Registrar Sala
        </router-link>
      </div>
    </div>

    <section class="grid grid-cols-2 gap-3 md:grid-cols-3 lg:grid-cols-6">
      <StatsCard label="Usuarios" :value="stats.totalUsers" icon="👥" tone="indigo" />
      <StatsCard label="Clases" :value="stats.totalClasses" icon="📚" tone="sky" />
      <StatsCard label="Sedes" :value="stats.totalVenues" icon="🏢" tone="emerald" />
      <StatsCard label="Salas" :value="stats.totalRooms" icon="🏠" tone="amber" />
      <StatsCard label="Verif. pendientes" :value="stats.pendingIdentityVerifications" icon="🪪" tone="rose" />
      <StatsCard label="Sedes pendientes" :value="stats.pendingVenues" icon="⏳" tone="amber" />
    </section>

    <section class="rounded-xl border bg-white p-5 shadow-sm">
      <h2 class="mb-3 text-lg font-semibold text-gray-900">Estado de pagos</h2>
      <div class="grid grid-cols-2 gap-2 text-sm md:grid-cols-5">
        <div class="rounded-lg bg-indigo-50 p-3 text-indigo-700">RETAINED: <strong>{{ paymentCount('RETAINED') }}</strong></div>
        <div class="rounded-lg bg-emerald-50 p-3 text-emerald-700">RELEASED: <strong>{{ paymentCount('RELEASED') }}</strong></div>
        <div class="rounded-lg bg-amber-50 p-3 text-amber-700">REFUND_PENDING: <strong>{{ paymentCount('REFUND_PENDING') }}</strong></div>
        <div class="rounded-lg bg-sky-50 p-3 text-sky-700">REFUNDED: <strong>{{ paymentCount('REFUNDED') }}</strong></div>
        <div class="rounded-lg bg-rose-50 p-3 text-rose-700">FAILED: <strong>{{ paymentCount('FAILED') }}</strong></div>
      </div>
    </section>

    <section class="rounded-xl border bg-white p-5 shadow-sm">
      <div class="mb-4 flex flex-wrap gap-2 border-b pb-3">
        <button @click="activeTab = 'verifications'" :class="tabClass('verifications')">Verificaciones</button>
        <button @click="activeTab = 'venues'" :class="tabClass('venues')">Sedes Pendientes</button>
        <button @click="activeTab = 'roles'" :class="tabClass('roles')">Roles</button>
      </div>

      <div v-if="activeTab === 'verifications'" class="space-y-3">
        <div v-if="verifications.length === 0" class="text-sm text-gray-500">No hay verificaciones pendientes.</div>
        <div v-for="item in verifications" :key="item.id" class="flex flex-col gap-3 rounded-lg border bg-yellow-50 p-4 md:flex-row md:items-center md:justify-between">
          <div>
            <p class="font-semibold text-gray-900">Usuario: {{ item.userId }}</p>
            <a :href="item.documentUrl" target="_blank" class="text-sm text-indigo-600 hover:underline">Ver documento</a>
          </div>
          <div class="flex gap-2">
            <button @click="openConfirm('identity', item.id, 'APPROVED')" class="rounded-md bg-emerald-600 px-3 py-1 text-sm text-white hover:bg-emerald-700">Aprobar</button>
            <button @click="openConfirm('identity', item.id, 'REJECTED')" class="rounded-md bg-rose-600 px-3 py-1 text-sm text-white hover:bg-rose-700">Rechazar</button>
          </div>
        </div>
      </div>

      <div v-if="activeTab === 'venues'" class="space-y-3">
        <div v-if="pendingVenues.length === 0" class="text-sm text-gray-500">No hay sedes pendientes.</div>
        <div v-for="item in pendingVenues" :key="item.id" class="flex flex-col gap-3 rounded-lg border bg-yellow-50 p-4 md:flex-row md:items-center md:justify-between">
          <div>
            <p class="font-semibold text-gray-900">{{ item.name }}</p>
            <p class="text-sm text-gray-600">{{ item.address }} · {{ item.email }}</p>
          </div>
          <div class="flex gap-2">
            <button @click="openConfirm('venue', item.id, 'APPROVED')" class="rounded-md bg-emerald-600 px-3 py-1 text-sm text-white hover:bg-emerald-700">Aprobar</button>
            <button @click="openConfirm('venue', item.id, 'REJECTED')" class="rounded-md bg-rose-600 px-3 py-1 text-sm text-white hover:bg-rose-700">Rechazar</button>
          </div>
        </div>
      </div>

      <div v-if="activeTab === 'roles'" class="rounded-lg border bg-gray-50 p-4 text-sm text-gray-700">
        Gestiona asignaciones y permisos desde la vista dedicada de roles.
        <router-link to="/admin/roles" class="ml-1 font-medium text-indigo-600 hover:underline">Ir a Gestionar Roles</router-link>
      </div>
    </section>

    <ConfirmDialog
      :show="showConfirm"
      title="Confirmar accion"
      :message="confirmMessage"
      :type="confirmAction === 'REJECTED' ? 'danger' : 'success'"
      confirm-text="Si, confirmar"
      cancel-text="Cancelar"
      @close="closeConfirm"
      @confirm="runConfirmedAction"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import StatsCard from '../components/StatsCard.vue'
import { adminService } from '../services/adminService'

const activeTab = ref('verifications')
const verifications = ref([])
const pendingVenues = ref([])
const stats = ref({
  totalUsers: 0,
  totalClasses: 0,
  totalVenues: 0,
  totalRooms: 0,
  pendingIdentityVerifications: 0,
  pendingVenues: 0,
  payments: {},
})

const showConfirm = ref(false)
const confirmType = ref('')
const confirmId = ref('')
const confirmAction = ref('')

const confirmMessage = computed(() => {
  const actionLabel = confirmAction.value === 'APPROVED' ? 'aprobar' : 'rechazar'
  return `Esta seguro que desea ${actionLabel} este registro?`
})

onMounted(async () => {
  await loadData()
})

const loadData = async () => {
  try {
    const [statsData, verificationData, venuesData] = await Promise.all([
      adminService.getStats(),
      adminService.getPendingVerifications(),
      adminService.getPendingVenues(),
    ])

    stats.value = statsData
    verifications.value = verificationData
    pendingVenues.value = venuesData
  } catch (error) {
    console.error('Error loading admin dashboard data', error)
  }
}

const openConfirm = (type, id, action) => {
  confirmType.value = type
  confirmId.value = id
  confirmAction.value = action
  showConfirm.value = true
}

const closeConfirm = () => {
  showConfirm.value = false
  confirmType.value = ''
  confirmId.value = ''
  confirmAction.value = ''
}

const runConfirmedAction = async () => {
  try {
    if (confirmType.value === 'identity') {
      await adminService.reviewIdentity(confirmId.value, confirmAction.value)
    }

    if (confirmType.value === 'venue') {
      if (confirmAction.value === 'APPROVED') {
        await adminService.approveVenue(confirmId.value)
      } else {
        await adminService.rejectVenue(confirmId.value)
      }
    }

    closeConfirm()
    await loadData()
  } catch (error) {
    console.error('Error processing admin action', error)
  }
}

const paymentCount = (key) => stats.value.payments?.[key] || 0

const tabClass = (tab) => (
  activeTab.value === tab
    ? 'rounded-md bg-indigo-100 px-3 py-1.5 text-sm font-medium text-indigo-700'
    : 'rounded-md px-3 py-1.5 text-sm font-medium text-gray-600 hover:bg-gray-100'
)
</script>
