<script setup>
import { computed } from 'vue'

const props = defineProps({
  items: { type: Array, required: true },
})

const total = computed(() => props.items.reduce((sum, item) => sum + item.price, 0))

const emit = defineEmits(['checkout'])
</script>

<template>
  <div v-if="items.length > 0" class="p-4 bg-gray-50 rounded-lg mt-4">
    <div class="flex justify-between mb-2">
      <span class="font-medium">Total</span>
      <span class="font-bold text-indigo-600">${{ total }}</span>
    </div>
    <button
      @click="emit('checkout')"
      class="w-full py-2 px-4 bg-green-600 text-white rounded hover:bg-green-700"
    >
      Pagar con MercadoPago
    </button>
  </div>
</template>
