<template>
  <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Verificaciones de Identidad</h1>
    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>
    <div v-else-if="verifications.length === 0" class="card text-center py-12"><p class="text-gray-400">No hay verificaciones pendientes.</p></div>
    <div v-else class="space-y-4">
      <div v-for="v in verifications" :key="v.id" class="card flex items-center justify-between">
        <div class="flex items-center space-x-4">
          <div class="w-10 h-10 bg-primary/20 rounded-full flex items-center justify-center text-primary font-bold">{{ (v.userName || v.userEmail || 'U').charAt(0).toUpperCase() }}</div>
          <div><h3 class="text-white font-medium">{{ v.userName || v.userEmail }}</h3><p class="text-gray-400 text-sm">{{ v.documentType || 'RUT' }}: {{ v.documentNumber }}</p></div>
        </div>
        <div class="flex items-center space-x-3">
          <EstadoBadge :status="v.status" />
          <div class="flex space-x-2">
            <button @click="review(v.id, 'approve')" class="text-green-400 hover:text-green-300 text-sm font-medium">Aprobar</button>
            <button @click="review(v.id, 'reject')" class="text-red-400 hover:text-red-300 text-sm font-medium">Rechazar</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Pending Venues -->
    <h2 class="text-2xl font-bold text-white mt-12 mb-6">Sedes Pendientes de Aprobacion</h2>
    <div v-if="pendingVenues.length === 0" class="card text-center py-8"><p class="text-gray-400">No hay sedes pendientes.</p></div>
    <div v-else class="space-y-4">
      <div v-for="v in pendingVenues" :key="v.id" class="card flex items-center justify-between">
        <div><h3 class="text-white font-medium">{{ v.name }}</h3><p class="text-gray-400 text-sm">{{ v.city }}, {{ v.address }}</p></div>
        <div class="flex space-x-2">
          <button @click="approveVenue(v.id)" class="btn-primary text-sm bg-green-600 hover:bg-green-700">Aprobar</button>
          <button @click="rejectVenue(v.id)" class="btn-danger text-sm">Rechazar</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import adminService from '@/services/adminService'
import EstadoBadge from '@/components/EstadoBadge.vue'

const verifications = ref([])
const pendingVenues = ref([])
const loading = ref(true)

onMounted(async () => {
  try { verifications.value = await adminService.getIdentityVerifications() } catch { verifications.value = [] }
  try { pendingVenues.value = await adminService.getPendingVenues() } catch { pendingVenues.value = [] }
  loading.value = false
})

async function review(id, action) {
  try { await adminService.reviewIdentity(id, action); verifications.value = verifications.value.filter(v => v.id !== id) } catch {}
}

async function approveVenue(id) {
  try { await adminService.approveVenue(id); pendingVenues.value = pendingVenues.value.filter(v => v.id !== id) } catch {}
}

async function rejectVenue(id) {
  try { await adminService.rejectVenue(id); pendingVenues.value = pendingVenues.value.filter(v => v.id !== id) } catch {}
}
</script>
