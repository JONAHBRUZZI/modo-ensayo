<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Registro de Profesor</h1>

    <div v-if="!identidadValidada" class="card space-y-4 text-center">
      <div class="w-16 h-16 bg-amber-500/20 rounded-2xl flex items-center justify-center mx-auto">
        <svg class="w-8 h-8 text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/></svg>
      </div>
      <h2 class="text-xl font-semibold text-white">Identidad no validada</h2>
      <p class="text-gray-400 text-sm">Para registrarte como profesor, primero debes validar tu identidad.</p>
      <router-link to="/profile/identity" class="btn-primary inline-block mt-2">Validar mi identidad</router-link>
    </div>

    <form v-else @submit.prevent="submit" class="card space-y-4">
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Disciplina Principal</label><select v-model="form.discipline" required class="input-field"><option value="">Seleccionar</option><option>Guitarra</option><option>Bateria</option><option>Bajo</option><option>Canto</option><option>Piano</option><option>Violin</option><option>Saxofon</option><option>Otro</option></select></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Anos de Experiencia</label><input type="number" v-model.number="form.experience" min="0" required class="input-field" /></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Biografia</label><textarea v-model="form.bio" rows="4" required class="input-field" placeholder="Cuentanos sobre tu experiencia..."></textarea></div>
      <p v-if="msg" :class="msgType === 'success' ? 'text-green-400' : 'text-red-400'" class="text-sm">{{ msg }}</p>
      <button type="submit" :disabled="sending" class="btn-primary w-full">{{ sending ? 'Enviando...' : 'Registrarme como Profesor' }}</button>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'

const router = useRouter()
const { identidadValidada, syncIdentityStatus } = useAuth()
const form = reactive({ discipline: '', experience: 0, bio: '' })
const sending = ref(false)
const msg = ref('')
const msgType = ref('')

onMounted(() => syncIdentityStatus())

async function submit() {
  sending.value = true
  try {
    msg.value = 'Registro completado. Ahora puedes buscar salas y crear clases.'
    msgType.value = 'success'
    setTimeout(() => router.push('/profesor/buscar-salas'), 2000)
  } catch (e) {
    msg.value = 'Error al registrar'
    msgType.value = 'error'
  }
  sending.value = false
}
</script>
