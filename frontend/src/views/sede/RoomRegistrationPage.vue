<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Registrar Sala</h1>
    <form @submit.prevent="submit" class="card space-y-4">
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Sede</label><select v-model="form.venueId" required class="input-field"><option value="">Seleccionar sede</option><option v-for="v in venues" :key="v.id" :value="v.id">{{ v.name }}</option></select></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Nombre de la Sala</label><input v-model="form.name" required class="input-field" /></div>
      <div class="grid grid-cols-2 gap-4">
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Capacidad</label><input type="number" v-model.number="form.capacity" min="1" required class="input-field" /></div>
        <div><label class="block text-sm font-medium text-gray-300 mb-1">Tipo</label><select v-model="form.type" required class="input-field"><option value="INDIVIDUAL">Individual</option><option value="GRUPAL">Grupal</option><option value="BANDA">Banda</option></select></div>
      </div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Equipamiento</label><input v-model="form.equipment" class="input-field" placeholder="Amplificadores, bateria, microfonos..." /></div>
      <p v-if="msg" class="text-green-400 text-sm">{{ msg }}</p>
      <button type="submit" :disabled="sending" class="btn-primary w-full">{{ sending ? 'Registrando...' : 'Registrar Sala' }}</button>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import venueService from '@/services/venueService'

const router = useRouter()
const venues = ref([])
const form = reactive({ venueId: '', name: '', capacity: 10, type: 'GRUPAL', equipment: '' })
const sending = ref(false)
const msg = ref('')

onMounted(async () => {
  try { venues.value = await venueService.getMyVenues() } catch { venues.value = [] }
})

async function submit() {
  sending.value = true
  try {
    await venueService.createRoom(form.venueId, { name: form.name, capacity: form.capacity, type: form.type, equipment: form.equipment })
    msg.value = 'Sala registrada correctamente'
    setTimeout(() => router.push('/sede/salas'), 1500)
  } catch {} finally { sending.value = false }
}
</script>
