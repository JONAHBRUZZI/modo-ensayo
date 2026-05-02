<template>
  <div>
    <h1 class="text-3xl font-bold mb-8">Mi Carrito</h1>
    <div v-if="cartItems.length > 0" class="bg-white rounded-lg shadow-md p-6">
      <div v-for="item in cartItems" :key="item.id" class="flex justify-between items-center py-4 border-b">
        <div>
          <p class="font-semibold">Clase ID: {{ item.classId }}</p>
          <p class="text-gray-600">Beneficiario: {{ item.beneficiaryType }}</p>
        </div>
        <button @click="removeFromCart(item.id)" class="text-red-600 hover:text-red-800">Eliminar</button>
      </div>
      <div class="mt-6">
        <button @click="checkout" class="w-full bg-green-600 text-white py-3 rounded-md hover:bg-green-700">
          Proceder al Pago
        </button>
      </div>
    </div>
    <p v-else class="text-gray-500 text-center py-8">Tu carrito esta vacio</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { cartService } from '../services/cartService'

const router = useRouter()
const cartItems = ref([])

onMounted(async () => {
  try {
    cartItems.value = await cartService.getCart()
  } catch (error) {
    console.error('Error loading cart:', error)
  }
})

const removeFromCart = async (itemId) => {
  try {
    await cartService.removeFromCart(itemId)
    cartItems.value = cartItems.value.filter(item => item.id !== itemId)
  } catch (error) {
    console.error('Error removing item:', error)
  }
}

const checkout = async () => {
  try {
    const response = await cartService.checkout({ paymentMethod: 'webpay' })
    alert('Pago exitoso! ID: ' + response.paymentId)
    router.push({ name: 'Profile' })
  } catch (error) {
    alert('Error en el pago')
  }
}
</script>
