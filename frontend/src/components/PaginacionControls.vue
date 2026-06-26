<template>
  <div v-if="totalPages > 1" class="flex items-center justify-between gap-4 mt-6">
    <p class="text-xs text-gray-500">
      {{ from }}–{{ to }} de {{ total }} resultados
    </p>
    <div class="flex items-center gap-1">
      <button
        @click="$emit('change', currentPage - 1)"
        :disabled="currentPage <= 1"
        class="w-8 h-8 flex items-center justify-center rounded-lg text-gray-500
               hover:text-white hover:bg-[var(--bg-elevated)] disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
        aria-label="Página anterior"
      >
        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
        </svg>
      </button>

      <template v-for="page in visiblePages" :key="page">
        <span v-if="page === '...'" class="w-8 h-8 flex items-center justify-center text-gray-600 text-sm">…</span>
        <button
          v-else
          @click="$emit('change', page)"
          :class="[
            'w-8 h-8 flex items-center justify-center rounded-lg text-sm transition-colors',
            page === currentPage
              ? 'bg-primary text-white font-medium'
              : 'text-gray-500 hover:text-white hover:bg-[var(--bg-elevated)]'
          ]"
        >{{ page }}</button>
      </template>

      <button
        @click="$emit('change', currentPage + 1)"
        :disabled="currentPage >= totalPages"
        class="w-8 h-8 flex items-center justify-center rounded-lg text-gray-500
               hover:text-white hover:bg-[var(--bg-elevated)] disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
        aria-label="Página siguiente"
      >
        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  currentPage: { type: Number, required: true },
  totalPages:  { type: Number, required: true },
  total:       { type: Number, required: true },
  pageSize:    { type: Number, default: 10 },
})
defineEmits(['change'])

const from = computed(() => (props.currentPage - 1) * props.pageSize + 1)
const to   = computed(() => Math.min(props.currentPage * props.pageSize, props.total))

const visiblePages = computed(() => {
  const pages = []
  const total = props.totalPages
  const cur = props.currentPage
  if (total <= 7) {
    for (let i = 1; i <= total; i++) pages.push(i)
  } else {
    pages.push(1)
    if (cur > 3) pages.push('...')
    for (let i = Math.max(2, cur - 1); i <= Math.min(total - 1, cur + 1); i++) pages.push(i)
    if (cur < total - 2) pages.push('...')
    pages.push(total)
  }
  return pages
})
</script>
