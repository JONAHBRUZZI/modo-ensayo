<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Clases Asignadas</h1>
    <div v-if="clases.length === 0" class="card text-center py-12"><p class="text-gray-400">No tienes clases asignadas.</p></div>
    <div v-else class="space-y-4">
      <div v-for="c in clases" :key="c.id" class="card flex items-center justify-between">
        <div><h3 class="text-white font-medium">{{ c.title }}</h3><p class="text-gray-400 text-sm">{{ c.discipline }} - {{ c.level }}</p><p class="text-gray-500 text-xs">{{ formatDate(c.startTime) }}</p></div>
        <router-link :to="'/profesor/asistencia/' + c.id" class="btn-primary text-sm">Asistencia</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const clases = ref([])

function formatDate(d) { return d ? new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : '' }
</script>
