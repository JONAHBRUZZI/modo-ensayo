<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-lg mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <!-- Paso 1: Confirmar pago -->
    <template v-if="!procesando && !completado">
      <h1 class="text-2xl font-bold text-white mb-6">Confirmar Pago</h1>
      <div class="card space-y-4">
        <div class="border-b border-white/5 pb-3">
          <h3 class="text-white font-medium">Resumen de compra</h3>
        </div>
        <div v-for="item in items" :key="item.id" class="flex justify-between text-sm">
          <span class="text-gray-300">{{ item.classTitle || item.title }}</span>
          <span class="text-white">${{ (item.price || 0).toLocaleString('es-CL') }}</span>
        </div>
        <div class="border-t border-white/10 pt-3 flex justify-between font-bold">
          <span class="text-white">Total</span>
          <span class="text-primary text-lg">${{ total.toLocaleString('es-CL') }}</span>
        </div>

        <div class="border-t border-white/5 pt-4">
          <h3 class="text-white font-medium mb-3">Metodo de pago</h3>
          <div class="space-y-2">
            <label class="flex items-center gap-3 p-3 rounded-xl border cursor-pointer" :class="metodo === 'mercadopago' ? 'border-primary bg-primary/10' : 'border-white/10 hover:border-white/20'">
              <input type="radio" v-model="metodo" value="mercadopago" class="text-primary" />
              <div>
                <p class="text-white text-sm font-medium">MercadoPago</p>
                <p class="text-gray-500 text-xs">Serás redirigido a MercadoPago para completar el pago. El pago queda retenido hasta la confirmacion de la clase.</p>
              </div>
            </label>
          </div>
        </div>

        <p v-if="error" class="text-red-400 text-sm text-center">{{ error }}</p>

        <button @click="procesarPago" :disabled="enviando" class="btn-primary w-full py-3 text-base font-semibold">
          {{ enviando ? 'Procesando...' : 'Pagar $' + total.toLocaleString('es-CL') }}
        </button>
        <button @click="$router.back()" class="w-full text-center text-gray-500 hover:text-gray-300 text-sm">Cancelar</button>
      </div>
    </template>

    <!-- Paso 2: Procesando -->
    <div v-if="procesando" class="text-center py-20">
      <div class="w-16 h-16 border-4 border-primary/30 border-t-primary rounded-full animate-spin mx-auto mb-6"></div>
      <h2 class="text-xl font-bold text-white mb-2">Procesando pago...</h2>
      <p class="text-gray-400 text-sm">Estamos procesando tu pago de <strong class="text-white">${{ total.toLocaleString('es-CL') }}</strong></p>
      <p class="text-gray-500 text-xs mt-2">Esto tomara solo unos segundos</p>
    </div>

    <!-- Paso 3: Completado -->
    <div v-if="completado" class="text-center py-10">
      <div class="w-20 h-20 bg-green-500/20 rounded-full flex items-center justify-center mx-auto mb-6">
        <svg class="w-10 h-10 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
        </svg>
      </div>
      <h2 class="text-2xl font-bold text-white mb-2">Pago completado</h2>
      <p class="text-gray-400 text-sm mb-2">Se ha procesado el pago por <strong class="text-white">${{ total.toLocaleString('es-CL') }}</strong></p>
      <p class="text-gray-500 text-xs mb-8">
        El pago queda <span class="text-yellow-400 font-medium">retenido</span> hasta que el profesor confirme la realizacion de la clase.
      </p>

      <div class="card text-left space-y-2 mb-6">
        <h3 class="text-white font-medium text-sm">Inscripciones realizadas:</h3>
        <div v-for="(item, i) in completadoItems" :key="i" class="flex justify-between text-sm border-t border-white/5 pt-2">
          <span class="text-gray-300">{{ item.classTitle }}</span>
          <span :class="item.status === 'RETAINED' ? 'text-yellow-400' : 'text-green-400'" class="text-xs font-medium">{{ item.status === 'RETAINED' ? 'Retenido' : item.status }}</span>
        </div>
      </div>

      <router-link to="/alumno/mis-clases" class="btn-primary inline-block px-8">
        Ir a Mis Clases
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import paymentService from '@/services/paymentService'

const router = useRouter()
const items = ref([])
const total = computed(() => items.value.reduce((sum, i) => sum + (i.price || 0), 0))
const metodo = ref('mercadopago')
const enviando = ref(false)
const procesando = ref(false)
const completado = ref(false)
const completadoItems = ref([])
const error = ref('')

onMounted(async () => {
  try {
    const data = await paymentService.getCart()
    items.value = data?.items || []
    if (items.value.length === 0) router.push('/cart')
  } catch { items.value = [] }
})

async function procesarPago() {
  error.value = ''
  enviando.value = true
  try {
    procesando.value = true
    const resultado = await paymentService.createMercadoPagoPreference()
    if (resultado?.initPoint) {
      window.location.href = resultado.initPoint
      return
    }
    completadoItems.value = resultado?.items || []
    procesando.value = false
    completado.value = true
  } catch (e) {
    procesando.value = false
    error.value = e?.response?.data?.message || 'Error al procesar el pago'
  } finally {
    enviando.value = false
  }
}
</script>
