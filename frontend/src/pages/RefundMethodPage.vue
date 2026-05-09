<template>
  <div class="max-w-2xl mx-auto space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-white">Método de Devolución</h1>
      <p class="text-gray-400 text-sm mt-0.5">Configura cómo prefieres recibir tus reembolsos</p>
    </div>

    <!-- Lista de métodos -->
    <div class="bg-[#161824] rounded-xl border border-white/10">
      <div class="px-5 py-4 border-b border-white/5">
        <h2 class="font-semibold text-white text-sm">Métodos Registrados</h2>
      </div>

      <div v-if="methods.length > 0" class="divide-y divide-white/5">
        <div v-for="method in methods" :key="method.id"
          class="flex items-center justify-between px-5 py-4 hover:bg-white/3 transition-colors">
          <div class="flex items-center gap-3">
            <div class="w-9 h-9 bg-indigo-500/20 rounded-lg flex items-center justify-center flex-shrink-0">
              <svg class="w-4 h-4 text-indigo-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
              </svg>
            </div>
            <div>
              <p class="text-sm font-medium text-white">{{ method.method }}</p>
              <p class="text-xs text-gray-500">{{ method.details }}</p>
            </div>
          </div>
          <button @click="removeMethod(method.id)"
            class="text-xs text-red-400 hover:text-red-300 hover:bg-red-500/10 px-3 py-1.5 rounded-lg transition-colors">
            Eliminar
          </button>
        </div>
      </div>
      <div v-else class="px-5 py-10 text-center">
        <p class="text-gray-500 text-sm">No tienes métodos de devolución registrados</p>
      </div>
    </div>

    <!-- Formulario agregar -->
    <div class="bg-[#161824] rounded-xl border border-white/10 p-5 space-y-4">
      <h2 class="font-semibold text-white text-sm">Agregar nuevo método</h2>

      <div>
        <label class="block text-xs font-medium text-gray-400 mb-1.5">Tipo de método</label>
        <select v-model="form.method"
          class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-sm text-white [color-scheme:dark] focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all">
          <option value="">Selecciona un método</option>
          <option>Cuenta Bancaria</option>
          <option>Tarjeta de Crédito/Débito</option>
          <option>Mercado Pago</option>
          <option>PayPal</option>
        </select>
      </div>

      <div>
        <label class="block text-xs font-medium text-gray-400 mb-1.5">Detalles</label>
        <textarea v-model="form.details" rows="2"
          class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all resize-none"
          placeholder="Nro cuenta, banco, alias, etc."></textarea>
      </div>

      <button @click="addMethod" :disabled="!form.method"
        class="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white rounded-lg text-sm font-medium transition-colors">
        Agregar método
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { userService } from '../services/userService'

const methods = ref([])
const form = ref({ method: '', details: '' })

onMounted(async () => {
  try { methods.value = await userService.getRefundMethods() } catch { /* ignore */ }
})

const addMethod = async () => {
  if (!form.value.method) return
  try {
    const method = await userService.createRefundMethod(form.value)
    methods.value.push(method)
    form.value = { method: '', details: '' }
  } catch { alert('Error al agregar método') }
}

const removeMethod = async (id) => {
  try {
    await userService.deleteRefundMethod(id)
    methods.value = methods.value.filter(m => m.id !== id)
  } catch { alert('Error al eliminar') }
}
</script>
