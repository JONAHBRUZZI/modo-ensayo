<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-8">Mi Perfil</h1>
    <form @submit.prevent="handleUpdate" class="card space-y-4">
      <div class="flex items-center space-x-4 mb-6">
        <div class="w-16 h-16 rounded-full flex items-center justify-center text-2xl font-bold text-white" style="background: linear-gradient(135deg, #6C63FF, #9B8CFF);">{{ displayName.charAt(0).toUpperCase() }}</div>
        <div><h3 class="text-lg font-semibold text-white">{{ user?.fullName }}</h3><p class="text-gray-400 text-sm">{{ user?.email }}</p></div>
      </div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Nombre Social</label><input v-model="form.socialName" class="input-field" :placeholder="user?.fullName" /></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Teléfono</label><input v-model="form.phone" class="input-field" :placeholder="user?.phone" /></div>
      <p v-if="success" class="text-green-400 text-sm">{{ success }}</p>
      <p v-if="error" class="text-red-400 text-sm">{{ error }}</p>
      <button type="submit" :disabled="saving" class="btn-primary">{{ saving ? 'Guardando...' : 'Guardar Cambios' }}</button>
    </form>
    <div class="mt-6 space-y-3">
      <div class="card space-y-3">
        <h3 class="text-white font-medium">Cambiar Contraseña</h3>
        <input v-model="pw.current" type="password" class="input-field" placeholder="Contraseña actual" />
        <input v-model="pw.new" type="password" class="input-field" placeholder="Nueva contraseña (min 6 caracteres)" />
        <p v-if="pwMsg" :class="pwOk ? 'text-green-400' : 'text-red-400'" class="text-sm">{{ pwMsg }}</p>
        <button @click="changePw" :disabled="pwSaving" class="btn-primary text-sm">{{ pwSaving ? 'Cambiando...' : 'Cambiar Contraseña' }}</button>
      </div>
      <router-link to="/profile/identity" class="card flex items-center justify-between hover:border-primary/50 transition-colors"><span class="text-white">Verificacion de Identidad</span><svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg></router-link>
      <router-link to="/profile/refund-method" class="card flex items-center justify-between hover:border-primary/50 transition-colors"><span class="text-white">Metodos de Devolucion</span><svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg></router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useAuth } from '@/stores/auth'
import userService from '@/services/userService'

const { user, displayName, updateUserProfile } = useAuth()
const form = reactive({ socialName: user.value?.socialName || '', phone: user.value?.phone || '' })
const saving = ref(false)
const success = ref('')
const error = ref('')
const pw = reactive({ current: '', new: '' })
const pwSaving = ref(false)
const pwMsg = ref('')
const pwOk = ref(false)

async function handleUpdate() {
  saving.value = true
  success.value = ''
  error.value = ''
  try {
    await updateUserProfile(form)
    success.value = 'Perfil actualizado correctamente'
  } catch (e) {
    error.value = e.response?.data?.message || 'Error al actualizar'
  } finally {
    saving.value = false
  }
}

async function changePw() {
  if (!pw.current || !pw.new) { pwMsg.value = 'Ambos campos son requeridos'; pwOk.value = false; return }
  pwSaving.value = true
  pwMsg.value = ''
  try {
    await userService.changePassword(pw.new)
    pwMsg.value = 'Contraseña cambiada exitosamente'
    pwOk.value = true
    pw.current = ''
    pw.new = ''
  } catch (e) {
    pwMsg.value = e.response?.data?.message || 'Error al cambiar contraseña'
    pwOk.value = false
  } finally {
    pwSaving.value = false
  }
}
</script>
