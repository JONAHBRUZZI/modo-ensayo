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
    case 'ACTIVO': return 'Maestro con clases activas. Puedes crear, asignar y gestiónar clases.'
    case 'DORMIDO': return 'Sin clases activas. Crea una clase para activar el contexto Maestro.'
    case 'INACTIVO': return 'Completa tu perfil profesional para activar el contexto Maestro.'
    default: return ''
  }
})

const badgeClass = computed(() => {
  switch (estadoUpper.value) {
    case 'ACTIVO': return 'badge badge-green'
    case 'DORMIDO': return 'badge bg-[var(--bg-elevated)] text-[var(--text-secondary)] border border-[var(--border-default)]'
    case 'INACTIVO': return 'badge bg-[var(--bg-elevated)] text-[var(--text-secondary)] border border-[var(--border-subtle)]'
    default: return 'badge badge-blue'
  }
})
</script>
