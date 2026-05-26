<template>
  <span :class="badgeClass">{{ label }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, default: '' }
})

const label = computed(() => {
  switch (props.status?.toUpperCase()) {
    case 'ACTIVE': return 'Activo'
    case 'PENDING': return 'Pendiente'
    case 'APPROVED': return 'Aprobado'
    case 'REJECTED': return 'Rechazado'
    case 'COMPLETED': return 'Completado'
    case 'CANCELLED': return 'Cancelado'
    case 'IN_PROGRESS': return 'En Progreso'
    case 'REALIZED': return 'Realizado'
    case 'NOT_REALIZED': return 'No Realizado'
    case 'ENABLED': return 'Habilitado'
    case 'DISABLED': return 'Deshabilitado'
    case 'PUBLISHED': return 'Publicado'
    case 'DRAFT': return 'Borrador'
    case 'NO_SOLICITADA': return 'No solicitada'
    default: return props.status || 'Desconocido'
  }
})

const badgeClass = computed(() => {
  switch (props.status?.toUpperCase()) {
    case 'ACTIVE':
    case 'APPROVED':
    case 'COMPLETED':
    case 'REALIZED':
    case 'ENABLED':
    case 'PUBLISHED':
      return 'badge badge-green'
    case 'PENDING':
    case 'IN_PROGRESS':
    case 'DRAFT':
      return 'badge badge-yellow'
    case 'REJECTED':
    case 'CANCELLED':
    case 'NOT_REALIZED':
    case 'DISABLED':
      return 'badge badge-red'
    default:
      return 'badge badge-blue'
  }
})
</script>
