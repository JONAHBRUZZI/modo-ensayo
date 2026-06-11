<template>
  <div class="fixed bottom-6 right-6 z-50 flex flex-col gap-2 pointer-events-none">
    <TransitionGroup
      enter-active-class="transition-all duration-300 ease-out"
      enter-from-class="opacity-0 translate-x-4"
      enter-to-class="opacity-100 translate-x-0"
      leave-active-class="transition-all duration-200 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0">
      <div
        v-for="t in toasts"
        :key="t.id"
        :class="['pointer-events-auto px-4 py-3 rounded-xl shadow-xl border text-sm max-w-sm cursor-pointer', alertClass(t.type)]"
        @click="removeToast(t.id)">
        <div class="flex items-start gap-2.5">
          <svg v-if="t.type === 'success'" class="w-4 h-4 text-green-400 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
          </svg>
          <svg v-else-if="t.type === 'error'" class="w-4 h-4 text-red-400 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
          <svg v-else-if="t.type === 'warning'" class="w-4 h-4 text-yellow-400 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M12 4l8 14H4l8-14z"/>
          </svg>
          <span :class="textClass(t.type)">{{ t.message }}</span>
        </div>
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup>
import { useToast } from '@/composables/useToast'
const { toasts, removeToast } = useToast()

function alertClass(type) {
  switch (type) {
    case 'success': return 'bg-green-900/90 border-green-500/30 text-green-100'
    case 'error': return 'bg-red-900/90 border-red-500/30 text-red-100'
    case 'warning': return 'bg-yellow-900/90 border-yellow-500/30 text-yellow-100'
    default: return 'bg-[#1a1d2e] border-white/10 text-gray-200'
  }
}

function textClass(type) {
  switch (type) {
    case 'success': return 'text-green-100'
    case 'error': return 'text-red-100'
    case 'warning': return 'text-yellow-100'
    default: return 'text-gray-200'
  }
}
</script>
