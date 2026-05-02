<template>
  <div class="rounded-xl border bg-white p-4 shadow-sm">
    <div class="flex items-start justify-between">
      <div>
        <p class="text-sm font-medium text-gray-500">{{ label }}</p>
        <p class="mt-1 text-2xl font-bold text-gray-900">{{ value }}</p>
      </div>
      <div :class="iconBgClass" class="rounded-lg p-2">
        <span class="text-lg">{{ icon }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], required: true },
  icon: { type: String, default: '•' },
  tone: { type: String, default: 'indigo' },
})

const tones = {
  indigo: 'bg-indigo-100 text-indigo-700',
  emerald: 'bg-emerald-100 text-emerald-700',
  amber: 'bg-amber-100 text-amber-700',
  sky: 'bg-sky-100 text-sky-700',
  rose: 'bg-rose-100 text-rose-700',
}

const iconBgClass = computed(() => tones[props.tone] || tones.indigo)
</script>
