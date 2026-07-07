<template>
  <span :class="['badge', badgeClass]">{{ label }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ status: { type: String, default: '' } })

const MAP = {
  ACTIVE:              { label: 'Activo',        cls: 'badge-green'  },
  APPROVED:            { label: 'Aprobado',       cls: 'badge-green'  },
  COMPLETED:           { label: 'Realizada',      cls: 'badge-blue'   },
  REALIZED:            { label: 'Realizado',      cls: 'badge-green'  },
  ENABLED:             { label: 'Habilitado',     cls: 'badge-green'  },
  PUBLISHED:           { label: 'Publicado',      cls: 'badge-green'  },
  PENDING:             { label: 'Pendiente',      cls: 'badge-yellow' },
  IN_PROGRESS:         { label: 'En progreso',    cls: 'badge-yellow' },
  DRAFT:               { label: 'Borrador',       cls: 'badge-yellow' },
  REJECTED:            { label: 'Rechazado',      cls: 'badge-red'    },
  CANCELLED:           { label: 'Cancelado',      cls: 'badge-red'    },
  NOT_REALIZED:        { label: 'No realizado',   cls: 'badge-red'    },
  DISABLED:            { label: 'Deshabilitado',  cls: 'badge-red'    },
  NO_SOLICITADA:       { label: 'Sin solicitud',  cls: 'badge-blue'   },
  RETAINED:            { label: 'Retenido',      cls: 'badge-yellow' },
  RELEASED:            { label: 'Liberado',      cls: 'badge-green'  },
  REFUND_PENDING:      { label: 'En devolución', cls: 'badge-yellow' },
  REFUNDED:            { label: 'Devuelto',      cls: 'badge-blue'   },
  FAILED:              { label: 'Fallido',       cls: 'badge-red'    },
  SUSPENDED:           { label: 'Suspendida',    cls: 'badge-red'    },
  POR_VALIDAR:         { label: 'Por validar',   cls: 'badge-yellow' },
  PAID:                { label: 'Pagado',        cls: 'badge-green'  },
}

const resolved = computed(() => MAP[props.status?.toUpperCase()] ?? { label: props.status || '—', cls: 'badge-blue' })
const label     = computed(() => resolved.value.label)
const badgeClass = computed(() => resolved.value.cls)
</script>
