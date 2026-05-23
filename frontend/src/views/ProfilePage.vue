<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Mi Perfil</h1>
    <form @submit.prevent="handleUpdate" class="card space-y-4">
      <div class="flex items-center space-x-4 mb-6">
        <div class="w-16 h-16 bg-primary rounded-full flex items-center justify-center text-2xl font-bold text-white">{{ displayName.charAt(0).toUpperCase() }}</div>
        <div><h3 class="text-lg font-semibold text-white">{{ user?.fullName }}</h3><p class="text-gray-400 text-sm">{{ user?.email }}</p></div>
      </div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Nombre Social</label><input v-model="form.socialName" class="input-field" :placeholder="user?.fullName" /></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Telefono</label><input v-model="form.phone" class="input-field" :placeholder="user?.phone" /></div>
      <p v-if="success" class="text-green-400 text-sm">{{ success }}</p>
      <p v-if="error" class="text-red-400 text-sm">{{ error }}</p>
      <button type="submit" :disabled="saving" class="btn-primary">{{ saving ? 'Guardando...' : 'Guardar Cambios' }}</button>
    </form>
    <div class="mt-6 space-y-3">
      <router-link to="/profile/identity" class="card flex items-center justify-between hover:border-primary/50 transition-colors"><span class="text-white">Verificacion de Identidad</span><svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg></router-link>
      <router-link to="/profile/refund-method" class="card flex items-center justify-between hover:border-primary/50 transition-colors"><span class="text-white">Metodos de Devolucion</span><svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg></router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useAuth } from '@/stores/auth'

const { user, displayName, updateUserProfile } = useAuth()
const form = reactive({ socialName: user.value?.socialName || '', phone: user.value?.phone || '' })
const saving = ref(false)
const success = ref('')
const error = ref('')

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
</script>
