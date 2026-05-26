<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Registrar Sede</h1>

    <div v-if="!identidadValidada" class="card space-y-4 text-center">
      <div class="w-16 h-16 bg-amber-500/20 rounded-2xl flex items-center justify-center mx-auto">
        <svg class="w-8 h-8 text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
      </div>
      <h2 class="text-xl font-semibold text-white">Identidad no validada</h2>
      <p class="text-gray-400 text-sm">Para registrar una sede, primero debes validar tu identidad subiendo tu cedula o pasaporte.</p>
      <router-link to="/profile/identity" class="btn-primary inline-block mt-2">Validar mi identidad</router-link>
    </div>

    <form v-else @submit.prevent="submit" class="card space-y-4">
      <div>
        <label class="block text-sm font-medium text-gray-300 mb-2">Tipo de espacio</label>
        <div class="grid grid-cols-2 gap-3">
          <button type="button" @click="form.tipo = 'SEDE'" :class="['p-4 rounded-xl border-2 text-left transition-all', form.tipo === 'SEDE' ? 'border-primary bg-primary/10' : 'border-white/10 hover:border-white/20']">
            <div class="text-white font-medium">Sede</div>
            <div class="text-gray-400 text-xs mt-1">Empresa o academia</div>
          </button>
          <button type="button" @click="form.tipo = 'HOME_STUDIO'" :class="['p-4 rounded-xl border-2 text-left transition-all', form.tipo === 'HOME_STUDIO' ? 'border-primary bg-primary/10' : 'border-white/10 hover:border-white/20']">
            <div class="text-white font-medium">HomeStudio</div>
            <div class="text-gray-400 text-xs mt-1">Espacio personal</div>
          </button>
        </div>
      </div>
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'
import venueService from '@/services/venueService'

const router = useRouter()
const { isAuthenticated, identidadValidada, syncIdentityStatus } = useAuth()
const disciplinas = ['Guitarra', 'Bateria', 'Bajo', 'Canto', 'Piano', 'Violin', 'Saxofon', 'Otro']
const form = reactive({ name: '', city: '', address: '', disciplines: [], tipo: 'SEDE' })
const sending = ref(false)
const msg = ref('')
const msgType = ref('')

onMounted(() => {
  if (isAuthenticated.value) syncIdentityStatus()
})

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
