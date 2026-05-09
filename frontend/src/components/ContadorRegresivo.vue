<template>
  <div class="flex items-center gap-2 text-sm" :class="isUrgent ? 'text-red-400' : 'text-amber-400'">
    <svg class="w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
    <span v-if="expired" class="font-medium">Ventana de reagendamiento cerrada</span>
    <span v-else>Tiempo restante: <span class="font-semibold font-mono">{{ display }}</span></span>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  deadline: { type: [String, Date], required: true },
})

const remaining = ref(0)
let interval = null

const update = () => {
  const target = new Date(props.deadline).getTime()
  remaining.value = Math.max(0, target - Date.now())
}

onMounted(() => {
  update()
  interval = setInterval(update, 1000)
})
onUnmounted(() => clearInterval(interval))

const expired = computed(() => remaining.value === 0)
const isUrgent = computed(() => remaining.value < 3600 * 1000) // < 1 hour

const display = computed(() => {
  const s = Math.floor(remaining.value / 1000)
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  if (h > 0) return `${h}h ${m}m`
  return `${m}m ${String(sec).padStart(2, '0')}s`
})
</script>
