<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Configuracion de Sede</h1>
    <form @submit.prevent="save" class="card space-y-4">
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Nombre</label><input v-model="form.name" required class="input-field" /></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Ciudad</label><input v-model="form.city" required class="input-field" /></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Direccion</label><input v-model="form.address" required class="input-field" /></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Disciplinas</label><div class="grid grid-cols-2 gap-2"><label v-for="d in disciplinas" :key="d" class="flex items-center space-x-2"><input type="checkbox" :value="d" v-model="form.disciplines" class="text-primary" /><span class="text-gray-300 text-sm">{{ d }}</span></label></div></div>
      <p v-if="msg" class="text-green-400 text-sm">{{ msg }}</p>
      <button type="submit" :disabled="saving" class="btn-primary w-full">{{ saving ? 'Guardando...' : 'Guardar Cambios' }}</button>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import venueService from '@/services/venueService'

const disciplinas = ['Guitarra', 'Bateria', 'Bajo', 'Canto', 'Piano', 'Violin', 'Saxofon', 'Otro']
const form = reactive({ name: '', city: '', address: '', disciplines: [] })
const saving = ref(false)
const msg = ref('')

onMounted(async () => {
  try {
    const venues = await venueService.getMyVenues()
    const vArr = Array.isArray(venues) ? venues : venues.content || []
    if (vArr.length > 0) {
      const v = vArr[0]
      Object.assign(form, { name: v.name || '', city: v.city || '', address: v.address || '', disciplines: v.disciplines || [] })
    }
  } catch {}
})

async function save() {
  saving.value = true
  try {
    const venues = await venueService.getMyVenues()
    const vArr = Array.isArray(venues) ? venues : venues.content || []
    if (vArr.length > 0) await venueService.updateVenue(vArr[0].id, form)
    msg.value = 'Configuracion guardada'
    setTimeout(() => msg.value = '', 3000)
  } catch {} finally { saving.value = false }
}
</script>
