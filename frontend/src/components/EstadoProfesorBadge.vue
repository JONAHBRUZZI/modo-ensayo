<template>
  <span :class="badgeClass" :title="tooltip">{{ label }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  estado: { type: String, default: 'INACTIVO' }
})

const estadoUpper = computed(() => props.estado?.toUpperCase())

const label = computed(() => {
  switch (estadoUpper.value) {
    case 'ACTIVO': return 'Activo'
    case 'DORMIDO': return 'Dormido'
    case 'INACTIVO': return 'Inactivo'
    default: return props.estado || 'Desconocido'
  }
})

const tooltip = computed(() => {
  switch (estadoUpper.value) {
    case 'ACTIVO': return 'Maestro con clases activas. Puedes crear, asignar y gestionar clases.'
    case 'DORMIDO': return 'Sin clases activas. Crea una clase para activar el contexto Maestro.'
    case 'INACTIVO': return 'Completa tu perfil profesional para activar el contexto Maestro.'
    default: return ''
  }
})

const badgeClass = computed(() => {
  switch (estadoUpper.value) {
    case 'ACTIVO': return 'badge badge-green'
    case 'DORMIDO': return 'badge bg-gray-600/30 text-gray-400 border border-gray-600/30'
    case 'INACTIVO': return 'badge bg-gray-800/50 text-gray-500 border border-gray-700/30'
    default: return 'badge badge-blue'
  }
})
</script>
