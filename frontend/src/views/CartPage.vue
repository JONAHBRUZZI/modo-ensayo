<template>
  <div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <div
      v-motion
      :initial="{ opacity: 0, y: 16 }"
      :enter="{ opacity: 1, y: 0, transition: { duration: 380 } }"
    >
      <h1 class="text-3xl font-bold text-white mb-1">Carrito</h1>
      <p class="text-gray-500 text-sm mb-8">Revisa tus clases antes de pagar</p>
    </div>

    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando carrito...</p>
    </div>

    <div
      v-else-if="items.length === 0"
      v-motion
      :initial="{ opacity: 0, scale: 0.97 }"
      :enter="{ opacity: 1, scale: 1, transition: { duration: 350 } }"
      class="card text-center py-16"
    >
      <div class="w-14 h-14 bg-[#1a1d2e] rounded-2xl flex items-center justify-center mx-auto mb-4">
        <svg class="w-7 h-7 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"/></svg>
      </div>
      <p class="text-gray-400 mb-1">Tu carrito está vacío</p>
      <p class="text-gray-600 text-sm mb-6">Explora las clases disponibles y agrega las que te interesen.</p>
      <router-link to="/classes" class="btn-primary inline-flex">Explorar Clases</router-link>
    </div>

    <div v-else class="space-y-4">
      <div
        v-for="(item, i) in items"
        :key="item.id"
        v-motion
        :initial="{ opacity: 0, x: -12 }"
        :enter="{ opacity: 1, x: 0, transition: { duration: 300, delay: i * 60 } }"
        class="card flex items-center justify-between gap-4"
      >
        <div class="flex-1 min-w-0">
          <h3 class="text-white font-medium truncate">{{ item.classTitle || item.title }}</h3>
          <div class="flex gap-2 mt-1">
            <span class="badge badge-purple text-xs">{{ item.discipline }}</span>
            <span class="badge badge-green text-xs">{{ item.level }}</span>
          </div>
        </div>
        <div class="flex items-center gap-4 shrink-0">
          <span class="text-primary font-bold">${{ item.price?.toLocaleString() }}</span>
          <button @click="removeItem(item.id)" class="text-gray-600 hover:text-red-400 transition-colors" aria-label="Eliminar">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
          </button>
        </div>
      </div>

      <div
        v-motion
        :initial="{ opacity: 0 }"
        :enter="{ opacity: 1, transition: { duration: 300, delay: 200 } }"
        class="card flex items-center justify-between border-primary/20"
      >
        <span class="text-white font-semibold">Total</span>
        <span class="text-primary font-bold text-xl">${{ total.toLocaleString() }}</span>
      </div>

      <button @click="irAPagar" :disabled="checkingOut" class="btn-primary w-full text-base py-3">
        {{ checkingOut ? 'Procesando...' : `Pagar $${total.toLocaleString()}` }}
      </button>
    </div>

    <ConfirmDialog
      :visible="showConfirm"
      title="Confirmar pago"
      :message="`¿Confirmas tu pago de $${total.toLocaleString()}?`"
      @confirm="confirmarPago"
      @cancel="showConfirm = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import paymentService from '@/services/paymentService'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

const router = useRouter()
const items = ref([])
const loading = ref(true)
const checkingOut = ref(false)
const showConfirm = ref(false)
const total = computed(() => items.value.reduce((sum, i) => sum + (i.price || 0), 0))

onMounted(async () => {
  try {
    const data = await paymentService.getCart()
    items.value = data?.items || []
  } catch { items.value = [] } finally { loading.value = false }
})

async function removeItem(id) {
  try {
    await paymentService.removeFromCart(id)
    items.value = items.value.filter(i => i.id !== id)
  } catch (err) {
    console.error('Error al eliminar item del carrito', err)
  }
}

function irAPagar() {
  showConfirm.value = true
}

async function confirmarPago() {
  checkingOut.value = true
  showConfirm.value = false
  await router.push('/payment/checkout')
}
</script>
