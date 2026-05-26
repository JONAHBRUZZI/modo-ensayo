<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Registro de Profesor</h1>
    <form @submit.prevent="submit" class="card space-y-4">
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Disciplina Principal</label><select v-model="form.discipline" required class="input-field"><option value="">Seleccionar</option><option>Guitarra</option><option>Bateria</option><option>Bajo</option><option>Canto</option><option>Piano</option><option>Violin</option><option>Saxofon</option><option>Otro</option></select></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Anos de Experiencia</label><input type="number" v-model.number="form.experience" min="0" required class="input-field" /></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Biografia</label><textarea v-model="form.bio" rows="4" required class="input-field" placeholder="Cuentanos sobre tu experiencia musical..."></textarea></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Niveles que ensenas</label><div class="flex space-x-4"><label class="flex items-center space-x-2"><input type="checkbox" v-model="form.levels" value="BASICO" class="text-primary" /><span class="text-gray-300 text-sm">Basico</span></label><label class="flex items-center space-x-2"><input type="checkbox" v-model="form.levels" value="INTERMEDIO" class="text-primary" /><span class="text-gray-300 text-sm">Intermedio</span></label><label class="flex items-center space-x-2"><input type="checkbox" v-model="form.levels" value="AVANZADO" class="text-primary" /><span class="text-gray-300 text-sm">Avanzado</span></label></div></div>
      <p v-if="msg" :class="msgType === 'success' ? 'text-green-400' : 'text-red-400'" class="text-sm">{{ msg }}</p>
      <button type="submit" :disabled="sending" class="btn-primary w-full">{{ sending ? 'Enviando...' : 'Registrarme como Profesor' }}</button>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'

const form = reactive({ discipline: '', experience: 0, bio: '', levels: [] })
const sending = ref(false)
const msg = ref('')
const msgType = ref('')

async function submit() {
  sending.value = true
  try { msg.value = 'Solicitud enviada para revision'; msgType.value = 'success' } catch (e) { msg.value = 'Error'; msgType.value = 'error' }
  sending.value = false
}
</script>
