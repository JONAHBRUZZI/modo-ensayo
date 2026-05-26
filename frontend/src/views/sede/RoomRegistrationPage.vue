<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Registrar Sala</h1>

    <div v-if="!identidadValidada" class="card space-y-4 text-center">
      <div class="w-16 h-16 bg-amber-500/20 rounded-2xl flex items-center justify-center mx-auto">
        <svg class="w-8 h-8 text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/></svg>
      </div>
      <h2 class="text-xl font-semibold text-white">Identidad no validada</h2>
      <p class="text-gray-400 text-sm">Para registrar salas, primero debes validar tu identidad.</p>
      <router-link to="/profile/identity" class="btn-primary inline-block mt-2">Validar mi identidad</router-link>
    </div>

    <form v-else @submit.prevent="submit" class="card space-y-4">
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Sede</label><select v-model="form.venueId" required class="input-field"><option value="">Seleccionar sede</option><option v-for="v in venues" :key="v.id" :value="v.id">{{ v.name }}</option></select></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Nombre de la Sala</label><input v-model="form.name" required class="input-field" /></div>
      <div class="grid grid-cols-2 gap-4">
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Capacidad</label><input type="number" v-model.number="form.capacity" min="1" required class="input-field" /></div>
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Tipo</label><select v-model="form.type" required class="input-field"><option value="INDIVIDUAL">Individual</option><option value="GRUPAL">Grupal</option><option value="BANDA">Banda</option></select></div>
      </div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Equipamiento</label><input v-model="form.equipment" class="input-field" placeholder="Amplificadores, bateria, microfonos..." /></div>
      <p v-if="msg" :class="msgType === 'success' ? 'text-green-400' : 'text-red-400'" class="text-sm">{{ msg }}</p>
      <button type="submit" :disabled="sending" class="btn-primary w-full">{{ sending ? 'Registrando...' : 'Registrar Sala' }}</button>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'
import venueService from '@/services/venueService'

const router = useRouter()
const { identidadValidada, syncIdentityStatus } = useAuth()
const venues = ref([])
const form = reactive({ venueId: '', name: '', capacity: 10, type: 'GRUPAL', equipment: '' })
const sending = ref(false)
const msg = ref('')
const msgType = ref('')

onMounted(async () => {
  syncIdentityStatus()
  try { venues.value = await venueService.getMyVenues() } catch { venues.value = [] }
})

async function submit() {
  sending.value = true
  try {
    await venueService.createRoom(form.venueId, { name: form.name, capacity: form.capacity, type: form.type, equipment: form.equipment })
    msg.value = 'Sala registrada correctamente'
    msgType.value = 'success'
    setTimeout(() => router.push('/sede/salas'), 1500)
  } catch (e) {
    msg.value = e.response?.data?.message || 'Error al registrar sala'
    msgType.value = 'error'
  } finally { sending.value = false }
}
</script>
