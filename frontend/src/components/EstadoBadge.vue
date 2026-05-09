<template>
  <span :class="badgeClass" class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold uppercase tracking-wider">
    <span :class="dotClass" class="w-1.5 h-1.5 rounded-full"></span>
    {{ label }}
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  estado: { type: String, required: true },
})

const map = {
  ACTIVA:     { label: 'Activa',     bg: 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30', dot: 'bg-emerald-400' },
  CONFIRMADA: { label: 'Confirmada', bg: 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30', dot: 'bg-emerald-400' },
  PAGADA:     { label: 'Pagada',     bg: 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30', dot: 'bg-emerald-400' },
  EN_ESPERA:  { label: 'En Espera',  bg: 'bg-amber-500/15 text-amber-400 border border-amber-500/30',   dot: 'bg-amber-400' },
  PENDIENTE:  { label: 'Pendiente',  bg: 'bg-amber-500/15 text-amber-400 border border-amber-500/30',   dot: 'bg-amber-400' },
  EN_REVISION:{ label: 'En Revisión',bg: 'bg-blue-500/15 text-blue-400 border border-blue-500/30',     dot: 'bg-blue-400' },
  CANCELADA:  { label: 'Cancelada',  bg: 'bg-red-500/15 text-red-400 border border-red-500/30',         dot: 'bg-red-400' },
  RECHAZADA:  { label: 'Rechazada',  bg: 'bg-red-500/15 text-red-400 border border-red-500/30',         dot: 'bg-red-400' },
  COMPLETADA: { label: 'Completada', bg: 'bg-gray-500/15 text-gray-400 border border-gray-500/30',      dot: 'bg-gray-400' },
  PROXIMA:    { label: 'Próxima',    bg: 'bg-indigo-500/15 text-indigo-400 border border-indigo-500/30', dot: 'bg-indigo-400' },
  EN_CURSO:   { label: 'En Curso',   bg: 'bg-purple-500/15 text-purple-400 border border-purple-500/30', dot: 'bg-purple-400 animate-pulse' },
}

const entry = computed(() => map[props.estado?.toUpperCase()] || { label: props.estado, bg: 'bg-gray-500/15 text-gray-400 border border-gray-500/30', dot: 'bg-gray-400' })
const badgeClass = computed(() => entry.value.bg)
const dotClass = computed(() => entry.value.dot)
const label = computed(() => entry.value.label)
</script>
