<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Clases Disponibles</h1>
    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando clases...</div>
    <div v-else-if="classes.length === 0" class="text-center text-gray-400 py-20">
      No hay clases disponibles en este momento.
    </div>
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="c in classes" :key="c.id" class="card hover:border-primary/50 transition-colors cursor-pointer" @click="goToClass(c.id)">
        <h3 class="text-lg font-semibold text-white mb-2">{{ c.title }}</h3>
        <div class="flex flex-wrap gap-2 mb-3">
          <span class="badge badge-blue">{{ c.discipline }}</span>
          <span class="badge badge-green">{{ c.level }}</span>
        </div>
        <p class="text-gray-400 text-sm mb-4 line-clamp-2">{{ c.description }}</p>
        <div class="flex items-center justify-between text-sm text-gray-400">
          <span>{{ formatDate(c.startTime) }}</span>
          <span class="text-primary font-medium">${{ c.price?.toLocaleString() }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import classService from '@/services/classService'

const router = useRouter()
const classes = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const data = await classService.getClasses()
    classes.value = Array.isArray(data) ? data : data.content || []
  } catch {
    classes.value = []
  } finally {
    loading.value = false
  }
})

function goToClass(id) {
  router.push(`/alumno/clases/${id}`)
}

function formatDate(date) {
  if (!date) return ''
  return new Date(date).toLocaleDateString('es-CL', {
    day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit'
  })
}
</script>
