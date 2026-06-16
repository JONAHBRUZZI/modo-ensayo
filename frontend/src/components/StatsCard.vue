<template>
  <div class="card">
    <div class="flex items-start justify-between">
      <div>
        <p class="text-xs font-medium text-gray-500 uppercase tracking-wider mb-2">{{ label }}</p>
        <p class="text-3xl font-bold" :class="valueClass">{{ value }}</p>
      </div>
      <div v-if="icon" :class="['w-10 h-10 rounded-xl flex items-center justify-center text-lg', iconBgClass]">
        {{ icon }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], required: true },
  icon: { type: String, default: '' },
  tone: { type: String, default: 'white' },
})

const tones = {
  white:  { bg: 'bg-[#1a1d2e]',         text: 'text-white'       },
  indigo: { bg: 'bg-primary/15',         text: 'text-primary'     },
  green:  { bg: 'bg-green-500/15',       text: 'text-green-400'   },
  yellow: { bg: 'bg-yellow-500/15',      text: 'text-yellow-400'  },
  red:    { bg: 'bg-red-500/15',         text: 'text-red-400'     },
  blue:   { bg: 'bg-blue-500/15',        text: 'text-blue-400'    },
}

const iconBgClass = computed(() => (tones[props.tone] || tones.white).bg)
const valueClass  = computed(() => (tones[props.tone] || tones.white).text)
</script>
