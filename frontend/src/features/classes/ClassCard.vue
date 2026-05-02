<script setup>
import { useRouter } from 'vue-router'

defineProps({
  classItem: { type: Object, required: true },
})

const router = useRouter()
</script>

<template>
  <div class="border rounded-lg p-4 bg-white shadow-sm hover:shadow-md transition">
    <div class="flex justify-between items-start mb-2">
      <h3 class="font-semibold text-lg">{{ classItem.title }}</h3>
      <span class="text-sm bg-indigo-100 text-indigo-700 px-2 py-1 rounded">
        {{ classItem.discipline }}
      </span>
    </div>
    <p class="text-sm text-gray-600 mb-2">{{ classItem.venue }} - {{ classItem.room }}</p>
    <div class="flex justify-between items-center text-sm text-gray-500 mb-3">
      <span>{{ classItem.startTime }}</span>
      <span>{{ classItem.capacity }} cupos</span>
    </div>
    <div class="flex justify-between items-center">
      <span class="font-bold text-indigo-600 text-lg">${{ classItem.price }}</span>
      <button
        @click="router.push({ name: 'Classes' })"
        class="px-3 py-1 border border-indigo-600 text-indigo-600 rounded hover:bg-indigo-50 text-sm"
      >
        Ver detalle
      </button>
    </div>
  </div>
</template>
