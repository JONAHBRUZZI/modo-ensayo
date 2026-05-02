<template>
  <div class="max-w-2xl mx-auto space-y-8">
    <div>
      <h1 class="text-3xl font-bold text-gray-900 mb-2">Metodo de Devolucion</h1>
      <p class="text-gray-600">Configura como prefieres recibir tus reembolsos</p>
    </div>

    <div class="bg-white rounded-xl shadow-md p-6">
      <h2 class="text-lg font-semibold mb-4">Metodos Registrados</h2>
      <div v-if="methods.length > 0" class="space-y-3">
        <div v-for="method in methods" :key="method.id" class="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
          <div>
            <p class="font-medium">{{ method.method }}</p>
            <p class="text-sm text-gray-500">{{ method.details }}</p>
          </div>
          <button @click="removeMethod(method.id)" class="text-red-600 hover:text-red-800 text-sm">Eliminar</button>
        </div>
      </div>
      <p v-else class="text-gray-500 text-center py-4">No tienes metodos de devolucion registrados</p>

      <form @submit.prevent="addMethod" class="mt-6 border-t pt-4 space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Tipo de Metodo</label>
          <select v-model="form.method" class="w-full px-3 py-2 border border-gray-300 rounded-lg">
            <option value="">Selecciona un metodo</option>
            <option value="Cuenta Bancaria">Cuenta Bancaria</option>
            <option value="Tarjeta de Credito/Debito">Tarjeta de Credito/Debito</option>
            <option value="Mercado Pago">Mercado Pago</option>
            <option value="PayPal">PayPal</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Detalles</label>
          <textarea v-model="form.details" rows="2" class="w-full px-3 py-2 border border-gray-300 rounded-lg" placeholder="Nro cuenta, banco, etc."></textarea>
        </div>
        <button type="submit" :disabled="!form.method" class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50">
          Agregar Metodo
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { userService } from '../services/userService'

const methods = ref([])
const form = ref({ method: '', details: '' })

onMounted(async () => {
  try { methods.value = await userService.getRefundMethods() } catch (e) { console.error(e) }
})

const addMethod = async () => {
  try {
    const method = await userService.createRefundMethod(form.value)
    methods.value.push(method)
    form.value = { method: '', details: '' }
  } catch (e) { alert('Error al agregar') }
}

const removeMethod = async (id) => {
  try {
    await userService.deleteRefundMethod(id)
    methods.value = methods.value.filter(m => m.id !== id)
  } catch (e) { alert('Error al eliminar') }
}
</script>