<template>
  <div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Carrito de Compras</h1>
    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>
    <div v-else-if="items.length === 0" class="card text-center py-12">
      <p class="text-gray-400">Tu carrito esta vacio.</p>
      <router-link to="/classes" class="btn-primary mt-4 inline-block">Explorar Clases</router-link>
    </div>
    <div v-else class="space-y-4">
      <div v-for="item in items" :key="item.id" class="card flex items-center justify-between">
        <div>
          <h3 class="text-white font-medium">{{ item.classTitle || item.title }}</h3>
          <p class="text-gray-400 text-sm">{{ item.discipline }} - {{ item.level }}</p>
        </div>
        <div class="flex items-center space-x-4">
          <span class="text-primary font-semibold">${{ item.price?.toLocaleString() }}</span>
          <button @click="removeItem(item.id)" class="text-red-400 hover:text-red-300">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
          </button>
        </div>
      </div>
      <div class="card flex items-center justify-between">
        <span class="text-white font-semibold text-lg">Total</span>
        <span class="text-primary font-bold text-xl">${{ total.toLocaleString() }}</span>
      </div>
      <button @click="showConfirm = true" :disabled="checkingOut" class="btn-primary w-full text-lg py-3">
        {{ checkingOut ? 'Procesando...' : 'Confirmar Pago' }}
      </button>
    </div>
  </div>

  <ConfirmModal
    :show="showConfirm"
    title="¿Confirma su pago?"
    :message="`Procesarás ${items.length} inscripción(es) por un total de $${total.toLocaleString()}. Esta acción no se puede deshacer.`"
    confirmText="Sí, pagar"
    @close="showConfirm = false"
    @confirm="handleConfirmedCheckout"
  />
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import paymentService from '@/services/paymentService'
import ConfirmModal from '@/components/ConfirmModal.vue'

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
  } catch {
    items.value = []
  } finally {
    loading.value = false
  }
})

async function removeItem(id) {
  try {
    await paymentService.removeFromCart(id)
    items.value = items.value.filter((i) => i.id !== id)
  } catch {}
}

async function handleConfirmedCheckout() {
  showConfirm.value = false
  checkingOut.value = true
  try {
    await paymentService.checkout()
    router.push('/alumno/mis-clases')
  } catch (e) {
    checkingOut.value = false
  }
}
</script>
