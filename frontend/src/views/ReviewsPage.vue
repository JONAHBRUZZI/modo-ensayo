<template>
  <div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Resenas</h1>
    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>
    <div v-else-if="reviews.length === 0" class="card text-center py-12"><p class="text-gray-400">No hay resenas disponibles.</p></div>
    <div v-else class="space-y-4">
      <div v-for="r in reviews" :key="r.id" class="card">
        <div class="flex items-center justify-between mb-2">
          <h3 class="text-white font-medium">{{ r.authorName || 'Usuario' }}</h3>
          <div class="flex text-yellow-400">
            <svg v-for="i in 5" :key="i" class="w-4 h-4" :class="i <= r.rating ? 'text-yellow-400' : 'text-gray-600'" fill="currentColor" viewBox="0 0 20 20"><path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/></svg>
          </div>
        </div>
        <p class="text-gray-300 text-sm">{{ r.comment }}</p>
        <p class="text-gray-500 text-xs mt-2">{{ formatDate(r.createdAt) }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const reviews = ref([])
const loading = ref(false)

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'short', year: 'numeric' })
}
</script>
