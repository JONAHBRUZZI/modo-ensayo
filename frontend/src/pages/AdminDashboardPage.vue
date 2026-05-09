<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between flex-wrap gap-3">
      <div>
        <h1 class="text-2xl font-bold text-white">Panel Administrador</h1>
        <p class="text-gray-400 text-sm mt-0.5">Gestión general del sistema</p>
      </div>
      <div class="flex gap-2 flex-wrap">
        <router-link to="/admin/roles" class="px-3 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-sm font-medium transition-colors">
          Gestionar Roles
        </router-link>
        <router-link to="/sede/registro" class="px-3 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-sm font-medium transition-colors">
          Registrar Sede
        </router-link>
        <router-link to="/sede/sala-registro" class="px-3 py-2 bg-sky-600 hover:bg-sky-500 text-white rounded-lg text-sm font-medium transition-colors">
          Registrar Sala
        </router-link>
      </div>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
      <div v-for="s in statsCards" :key="s.label" class="bg-[#161824] rounded-xl border border-white/10 p-4">
        <div :class="s.iconBg" class="w-9 h-9 rounded-lg flex items-center justify-center mb-3">
          <svg :class="s.iconColor" class="w-4.5 h-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="s.icon" />
          </svg>
        </div>
        <p class="text-2xl font-bold text-white">{{ s.value }}</p>
        <p class="text-xs text-gray-500 mt-0.5">{{ s.label }}</p>
      </div>
    </div>

    <!-- Estado pagos -->
    <div class="bg-[#161824] rounded-xl border border-white/10 p-5">
      <h2 class="font-semibold text-white text-sm mb-4">Estado de pagos</h2>
      <div class="grid grid-cols-2 md:grid-cols-5 gap-3">
        <div v-for="ps in paymentStatuses" :key="ps.key"
          class="rounded-lg border px-3 py-2.5"
          :class="ps.border">
          <p class="text-xs font-medium" :class="ps.textColor">{{ ps.key }}</p>
          <p class="text-xl font-bold text-white mt-0.5">{{ paymentCount(ps.key) }}</p>
        </div>
      </div>
    </div>

    <!-- Tabs gestión -->
    <div class="bg-[#161824] rounded-xl border border-white/10 overflow-hidden">
      <div class="px-5 py-3 border-b border-white/5 flex gap-1 flex-wrap">
        <button v-for="tab in tabs" :key="tab.key" @click="activeTab = tab.key"
          :class="activeTab === tab.key ? 'bg-indigo-600 text-white' : 'text-gray-400 hover:text-white'"
          class="px-3 py-1.5 rounded-lg text-sm font-medium transition-colors">
          {{ tab.label }}
          <span v-if="tab.badge > 0" class="ml-1.5 text-xs bg-red-500 text-white px-1.5 py-0.5 rounded-full">{{ tab.badge }}</span>
        </button>
      </div>

      <div class="p-5">
        <!-- Verificaciones -->
        <div v-if="activeTab === 'verifications'" class="space-y-3">
          <p v-if="verifications.length === 0" class="text-sm text-gray-500 text-center py-6">No hay verificaciones pendientes.</p>
          <div v-for="item in verifications" :key="item.id"
            class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 rounded-xl border border-amber-500/20 bg-amber-500/5 p-4">
            <div>
              <p class="font-semibold text-white text-sm">Usuario: {{ item.userId }}</p>
              <a :href="item.documentUrl" target="_blank" class="text-xs text-indigo-400 hover:text-indigo-300 transition-colors">
                Ver documento →
              </a>
            </div>
            <div class="flex gap-2">
              <button @click="openConfirm('identity', item.id, 'APPROVED')"
                class="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-medium transition-colors">
                Aprobar
              </button>
              <button @click="openConfirm('identity', item.id, 'REJECTED')"
                class="px-3 py-1.5 bg-red-600 hover:bg-red-500 text-white rounded-lg text-xs font-medium transition-colors">
                Rechazar
              </button>
            </div>
          </div>
        </div>

        <!-- Sedes pendientes -->
        <div v-if="activeTab === 'venues'" class="space-y-3">
          <p v-if="pendingVenues.length === 0" class="text-sm text-gray-500 text-center py-6">No hay sedes pendientes.</p>
          <div v-for="item in pendingVenues" :key="item.id"
            class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 rounded-xl border border-amber-500/20 bg-amber-500/5 p-4">
            <div>
              <p class="font-semibold text-white text-sm">{{ item.name }}</p>
              <p class="text-xs text-gray-500">{{ item.address }} · {{ item.email }}</p>
            </div>
            <div class="flex gap-2">
              <button @click="openConfirm('venue', item.id, 'APPROVED')"
                class="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-medium transition-colors">
                Aprobar
              </button>
              <button @click="openConfirm('venue', item.id, 'REJECTED')"
                class="px-3 py-1.5 bg-red-600 hover:bg-red-500 text-white rounded-lg text-xs font-medium transition-colors">
                Rechazar
              </button>
            </div>
          </div>
        </div>

        <!-- Roles -->
        <div v-if="activeTab === 'roles'" class="text-sm text-gray-400 py-4">
          Gestiona asignaciones y permisos desde la vista dedicada.
          <router-link to="/admin/roles" class="ml-1 text-indigo-400 hover:text-indigo-300 transition-colors font-medium">
            Ir a Gestionar Roles →
          </router-link>
        </div>
      </div>
    </div>

    <!-- Confirm Modal -->
    <ConfirmModal
      v-model="showConfirm"
      title="Confirmar acción"
      :message="confirmMessage"
      confirm-text="Confirmar"
      :type="confirmAction === 'REJECTED' ? 'danger' : 'info'"
      @confirm="runConfirmedAction"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import { adminService } from '../services/adminService'

const activeTab = ref('verifications')
const verifications = ref([])
const pendingVenues = ref([])
const stats = ref({
  totalUsers: 0, totalClasses: 0, totalVenues: 0, totalRooms: 0,
  pendingIdentityVerifications: 0, pendingVenues: 0, payments: {},
})

const showConfirm = ref(false)
const confirmType = ref('')
const confirmId = ref('')
const confirmAction = ref('')

const confirmMessage = computed(() => {
  const action = confirmAction.value === 'APPROVED' ? 'aprobar' : 'rechazar'
  return `¿Estás seguro que deseas ${action} este registro?`
})

const statsCards = computed(() => [
  { label: 'Usuarios', value: stats.value.totalUsers, icon: 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z', iconBg: 'bg-indigo-500/20', iconColor: 'text-indigo-400' },
  { label: 'Clases', value: stats.value.totalClasses, icon: 'M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253', iconBg: 'bg-sky-500/20', iconColor: 'text-sky-400' },
  { label: 'Sedes', value: stats.value.totalVenues, icon: 'M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4', iconBg: 'bg-emerald-500/20', iconColor: 'text-emerald-400' },
  { label: 'Salas', value: stats.value.totalRooms, icon: 'M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z', iconBg: 'bg-amber-500/20', iconColor: 'text-amber-400' },
  { label: 'Verif. pendientes', value: stats.value.pendingIdentityVerifications, icon: 'M10 6H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V8a2 2 0 00-2-2h-5m-4 0V5a2 2 0 114 0v1m-4 0a2 2 0 104 0m-5 8a2 2 0 100-4 2 2 0 000 4zm0 0c1.306 0 2.417.835 2.83 2M9 14a3.001 3.001 0 00-2.83 2M15 11h3m-3 4h2', iconBg: 'bg-rose-500/20', iconColor: 'text-rose-400' },
  { label: 'Sedes pendientes', value: stats.value.pendingVenues, icon: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z', iconBg: 'bg-amber-500/20', iconColor: 'text-amber-400' },
])

const paymentStatuses = [
  { key: 'RETAINED', border: 'border-indigo-500/20 bg-indigo-500/5', textColor: 'text-indigo-400' },
  { key: 'RELEASED', border: 'border-emerald-500/20 bg-emerald-500/5', textColor: 'text-emerald-400' },
  { key: 'REFUND_PENDING', border: 'border-amber-500/20 bg-amber-500/5', textColor: 'text-amber-400' },
  { key: 'REFUNDED', border: 'border-sky-500/20 bg-sky-500/5', textColor: 'text-sky-400' },
  { key: 'FAILED', border: 'border-red-500/20 bg-red-500/5', textColor: 'text-red-400' },
]

const tabs = computed(() => [
  { key: 'verifications', label: 'Verificaciones', badge: verifications.value.length },
  { key: 'venues', label: 'Sedes Pendientes', badge: pendingVenues.value.length },
  { key: 'roles', label: 'Roles', badge: 0 },
])

const paymentCount = (key) => stats.value.payments?.[key] || 0

onMounted(async () => {
  try {
    const [s, v, pv] = await Promise.all([
      adminService.getStats(),
      adminService.getPendingVerifications(),
      adminService.getPendingVenues(),
    ])
    stats.value = s
    verifications.value = v
    pendingVenues.value = pv
  } catch { /* ignore */ }
})

const openConfirm = (type, id, action) => {
  confirmType.value = type; confirmId.value = id; confirmAction.value = action
  showConfirm.value = true
}

const runConfirmedAction = async () => {
  try {
    if (confirmType.value === 'identity') {
      await adminService.reviewIdentity(confirmId.value, confirmAction.value)
    } else if (confirmType.value === 'venue') {
      if (confirmAction.value === 'APPROVED') await adminService.approveVenue(confirmId.value)
      else await adminService.rejectVenue(confirmId.value)
    }
    showConfirm.value = false
    const [s, v, pv] = await Promise.all([adminService.getStats(), adminService.getPendingVerifications(), adminService.getPendingVenues()])
    stats.value = s; verifications.value = v; pendingVenues.value = pv
  } catch { /* ignore */ }
}
</script>
