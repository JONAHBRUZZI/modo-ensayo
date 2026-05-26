<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-2">Mi Sede</h1>
    <p class="text-gray-400 mb-8">Datos registrados de tu sede</p>

    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>

    <div v-else-if="!venue" class="card text-center py-12">
      <p class="text-gray-400">No tienes una sede registrada.</p>
      <router-link to="/sede/registro" class="btn-primary mt-4 inline-block">Registrar Sede</router-link>
    </div>

    <div v-else class="space-y-6">
      <!-- Status badge -->
      <div class="card">
        <div class="flex items-center justify-between">
          <div>
            <h3 class="text-white font-medium">{{ venue.name }}</h3>
            <p class="text-gray-400 text-sm">{{ venue.address }}, {{ venue.city }}</p>
          </div>
          <EstadoBadge :status="venue.status" />
        </div>
        <div v-if="venue.tipo" class="mt-2">
          <span class="badge badge-blue">{{ venue.tipo === 'HOME_STUDIO' ? 'HomeStudio' : 'Sede' }}</span>
        </div>
      </div>

      <!-- Rechazada: mostrar motivo y permitir correccion -->
      <div v-if="venue.status === 'RECHAZADA'" class="card border-red-500/30 bg-red-500/5">
        <h3 class="text-red-400 font-medium mb-2">Sede Rechazada</h3>
        <p class="text-gray-400 text-sm">Motivo: {{ venue.rejectionReason || 'No especificado' }}</p>
        <p class="text-gray-500 text-xs mt-2">Corrige los datos y reenvia para aprobacion.</p>
      </div>

      <!-- Pendiente: puede editar -->
      <form v-if="venue.status !== 'APROBADA'" @submit.prevent="save" class="card space-y-4">
        <h3 class="text-white font-medium">Editar datos</h3>
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Nombre</label><input v-model="form.name" required class="input-field" /></div>
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Ciudad</label><input v-model="form.city" required class="input-field" /></div>
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Direccion</label><input v-model="form.address" required class="input-field" /></div>
        <p v-if="msg" :class="msgType === 'error' ? 'text-red-400' : 'text-green-400'" class="text-sm">{{ msg }}</p>
        <button type="submit" :disabled="saving" class="btn-primary w-full">{{ saving ? 'Guardando...' : 'Guardar y reenviar' }}</button>
      </form>

      <!-- Aprobada: solo lectura -->
      <div v-else class="card">
        <div class="flex items-center space-x-2 mb-4">
          <svg class="w-5 h-5 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
          <span class="text-green-400 font-medium">Sede verificada y aprobada</span>
        </div>
        <p class="text-gray-500 text-sm">Los datos de una sede aprobada no pueden modificarse. Si necesitas hacer cambios, contacta al Administrador General.</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import venueService from '@/services/venueService'
import EstadoBadge from '@/components/EstadoBadge.vue'

const venue = ref(null)
const loading = ref(true)
const form = reactive({ name: '', city: '', address: '' })
const saving = ref(false)
const msg = ref('')
const msgType = ref('')

onMounted(async () => {
  try {
    const venues = await venueService.getMyVenues()
    const vArr = Array.isArray(venues) ? venues : venues?.content || []
    if (vArr.length > 0) {
      venue.value = vArr[0]
      form.name = venue.value.name || ''
      form.city = venue.value.city || ''
      form.address = venue.value.address || ''
    }
  } catch {}
  loading.value = false
})

async function save() {
  saving.value = true
  msg.value = ''
  try {
    await venueService.updateVenue(venue.value.id, form)
    msg.value = 'Datos actualizados. Espera la aprobacion del Administrador General.'
    msgType.value = 'success'
  } catch (e) {
    msg.value = e?.response?.data?.message || 'Error al guardar'
    msgType.value = 'error'
  }
  saving.value = false
}
</script>
