<script setup>
import { ref } from 'vue'
import { useAuth } from '../../hooks/useAuth'

defineProps({
  classItem: { type: Object, required: true },
})

const emit = defineEmits(['remove'])
const { isAuthenticated } = useAuth()
</script>

<template>
  <div class="flex items-center justify-between p-4 border rounded-lg mb-2 bg-white">
    <div>
      <h3 class="font-semibold">{{ classItem.title }}</h3>
      <p class="text-sm text-gray-600">{{ classItem.discipline }}</p>
      <p class="text-sm text-gray-500">{{ classItem.venue }}</p>
      <p class="text-sm font-medium text-indigo-600">${{ classItem.price }}</p>
    </div>
    <button
      @click="emit('remove', classItem.id)"
      class="text-red-500 hover:text-red-700 text-sm"
    >
      Eliminar
    </button>
  </div>
</template>
