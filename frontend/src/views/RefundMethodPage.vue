<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Metodos de Devolucion</h1>
    <form @submit.prevent="addMethod" class="card space-y-4 mb-6">
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Banco</label><input v-model="form.bank" required class="input-field" placeholder="Banco Estado" /></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Tipo de Cuenta</label><select v-model="form.accountType" required class="input-field"><option value="CORRIENTE">Cuenta Corriente</option><option value="VISTA">Cuenta Vista</option><option value="AHORRO">Cuenta de Ahorro</option></select></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Numero de Cuenta</label><input v-model="form.accountNumber" required class="input-field" /></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">Titular</label><input v-model="form.accountHolder" required class="input-field" /></div>
      <div><label class="block text-sm font-medium text-gray-300 mb-1">RUT Titular</label><input v-model="form.rut" required class="input-field" /></div>
      <button type="submit" :disabled="adding" class="btn-primary">{{ adding ? 'Agregando...' : 'Agregar Metodo' }}</button>
    </form>
    <div v-if="methods.length === 0" class="card text-center py-8"><p class="text-gray-400">No tienes metodos de devolucion registrados.</p></div>
    <div v-else class="space-y-3">
      <div v-for="m in methods" :key="m.id" class="card flex items-center justify-between">
        <div><p class="text-white font-medium">{{ m.bank }} - {{ m.accountType }}</p><p class="text-gray-400 text-sm">****{{ m.accountNumber?.slice(-4) }}</p></div>
        <button @click="removeMethod(m.id)" class="text-red-400 hover:text-red-300"><svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg></button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import userService from '@/services/userService'

const methods = ref([])
const form = reactive({ bank: '', accountType: 'CORRIENTE', accountNumber: '', accountHolder: '', rut: '' })
const adding = ref(false)

onMounted(async () => {
  try { methods.value = await userService.getRefundMethods() } catch { methods.value = [] }
})

async function addMethod() {
  adding.value = true
  try {
    await userService.createRefundMethod(form)
    Object.assign(form, { bank: '', accountType: 'CORRIENTE', accountNumber: '', accountHolder: '', rut: '' })
    methods.value = await userService.getRefundMethods()
  } catch {} finally { adding.value = false }
}

async function removeMethod(id) {
  try {
    await userService.deleteRefundMethod(id)
    methods.value = methods.value.filter(m => m.id !== id)
  } catch {}
}
</script>
