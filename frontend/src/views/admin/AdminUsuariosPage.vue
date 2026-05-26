<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Gestion de Usuarios</h1>
    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>
    <div v-else class="space-y-4">
      <div v-for="u in users" :key="u.id" class="card flex items-center justify-between">
        <div class="flex items-center space-x-4">
          <div class="w-10 h-10 bg-primary/20 rounded-full flex items-center justify-center text-primary font-bold">{{ (u.fullName || u.email || 'U').charAt(0).toUpperCase() }}</div>
          <div><h3 class="text-white font-medium">{{ u.fullName || u.email }}</h3><p class="text-gray-400 text-sm">{{ u.email }}</p></div>
        </div>
        <div class="flex items-center space-x-3">
          <div class="flex flex-wrap gap-1"><span v-for="r in u.roles" :key="r" class="badge badge-blue">{{ r }}</span></div>
          <select @change="assignRole(u.id, $event.target.value)" class="bg-[#1a1d2e] border border-gray-700 rounded-lg text-sm text-gray-300 px-2 py-1">
            <option value="">+ Rol</option>
            <option value="TEACHER">TEACHER</option>
            <option value="VENUE_ADMIN">VENUE_ADMIN</option>
            <option value="ADMIN">ADMIN</option>
          </select>
          <EstadoBadge :status="u.enabled ? 'ENABLED' : 'DISABLED'" />
          <button @click="toggleUser(u)" :class="['text-xs px-3 py-1.5 rounded-lg transition-colors', u.enabled ? 'bg-red-500/20 text-red-400 hover:bg-red-500/30' : 'bg-green-500/20 text-green-400 hover:bg-green-500/30']">{{ u.enabled ? 'Suspender' : 'Activar' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import adminService from '@/services/adminService'
import EstadoBadge from '@/components/EstadoBadge.vue'

const users = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const data = await adminService.getUsers()
    users.value = Array.isArray(data) ? data : data.content || []
  } catch { users.value = [] }
  loading.value = false
})

async function assignRole(userId, role) {
  if (!role) return
  try { await adminService.assignRole(userId, role) } catch {}
}

async function toggleUser(u) {
  const motivo = u.enabled ? prompt('Motivo de suspension:') : null
  try {
    await adminService.toggleUser(u.id, motivo || '')
    u.enabled = !u.enabled
  } catch {}
}
</script>
