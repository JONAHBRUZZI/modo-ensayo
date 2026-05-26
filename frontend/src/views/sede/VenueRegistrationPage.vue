<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Registrar Sede</h1>
    <form @submit.prevent="submit" class="card space-y-4">
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Nombre de la Sede</label><input v-model="form.name" required class="input-field" /></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Ciudad</label><input v-model="form.city" required class="input-field" /></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Direccion</label><input v-model="form.address" required class="input-field" /></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Disciplinas</label><div class="grid grid-cols-2 gap-2"><label v-for="d in disciplinas" :key="d" class="flex items-center space-x-2"><input type="checkbox" :value="d" v-model="form.disciplines" class="text-primary" /><span class="text-gray-300 text-sm">{{ d }}</span></label></div></div>
      <p v-if="msg" :class="msgType === 'success' ? 'text-green-400' : 'text-red-400'" class="text-sm">{{ msg }}</p>
      <button type="submit" :disabled="sending" class="btn-primary w-full">{{ sending ? 'Enviando...' : 'Registrar Sede' }}</button>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import venueService from '@/services/venueService'

const router = useRouter()
const disciplinas = ['Guitarra', 'Bateria', 'Bajo', 'Canto', 'Piano', 'Violin', 'Saxofon', 'Otro']
const form = reactive({ name: '', city: '', address: '', disciplines: [] })
const sending = ref(false)
const msg = ref('')
const msgType = ref('')

async function submit() {
  sending.value = true
  try {
    await venueService.createVenue(form)
    msg.value = 'Sede registrada correctamente'
    msgType.value = 'success'
    setTimeout(() => router.push('/sede/dashboard'), 1500)
  } catch (e) {
    msg.value = e.response?.data?.message || 'Error'
    msgType.value = 'error'
  }
  sending.value = false
}
</script>
