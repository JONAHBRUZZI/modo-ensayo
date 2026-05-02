<script setup>
import { ref } from 'vue'
import cartService from '../../services/cartService'

const props = defineProps({
  classId: { type: String, required: true },
  beneficiaryType: { type: String, default: 'USER' },
  beneficiaryId: { type: String, default: null },
})

const loading = ref(false)
const error = ref(null)

async function handlePayment() {
  loading.value = true
  error.value = null
  try {
    const { data } = await cartService.createMercadoPagoPreference(
      props.classId,
      props.beneficiaryType,
      props.beneficiaryId
    )
    if (data.preferenceId) {
      const mp = new window.MercadoPago('TEST-xxxxxxxxxxxxxx', { locale: 'es-CL' })
      mp.checkout({ preference: { id: data.preferenceId } })
    }
  } catch (e) {
    error.value = 'Error al iniciar el pago. Intenta nuevamente.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div>
    <button
      @click="handlePayment"
      :disabled="loading"
      class="w-full py-3 px-6 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 font-medium"
    >
      {{ loading ? 'Procesando...' : 'Pagar con MercadoPago' }}
    </button>
    <p v-if="error" class="mt-2 text-sm text-red-600">{{ error }}</p>
  </div>
</template>
