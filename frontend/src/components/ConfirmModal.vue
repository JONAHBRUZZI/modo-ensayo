<template>
  <Teleport to="body">
    <div v-if="modelValue" class="fixed inset-0 z-50 flex items-center justify-center bg-black/70" @click.self="$emit('update:modelValue', false)">
      <div class="bg-[#161824] rounded-2xl border border-white/10 p-6 w-full max-w-sm mx-4 shadow-2xl">
        <div class="flex items-start gap-4 mb-5">
          <div :class="iconBg" class="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5">
            <svg v-if="type === 'danger'" class="w-5 h-5 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            <svg v-else-if="type === 'warning'" class="w-5 h-5 text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            <svg v-else class="w-5 h-5 text-indigo-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div>
            <h3 class="font-semibold text-white text-base">{{ title }}</h3>
            <p class="text-sm text-gray-400 mt-1 leading-relaxed">{{ message }}</p>
          </div>
        </div>
        <div class="flex gap-2">
          <button @click="$emit('update:modelValue', false)"
                  class="flex-1 py-2.5 border border-white/15 text-gray-300 rounded-lg text-sm hover:bg-white/5 transition-colors">
            {{ cancelText }}
          </button>
          <button @click="confirm"
                  :class="confirmClass"
                  class="flex-1 py-2.5 rounded-lg text-sm font-medium transition-colors text-white">
            {{ confirmText }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: Boolean,
  title: { type: String, default: '¿Confirmar acción?' },
  message: { type: String, default: '' },
  confirmText: { type: String, default: 'Confirmar' },
  cancelText: { type: String, default: 'Cancelar' },
  type: { type: String, default: 'info' }, // info | warning | danger
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const iconBg = computed(() => ({
  'bg-red-500/20': props.type === 'danger',
  'bg-amber-500/20': props.type === 'warning',
  'bg-indigo-500/20': props.type === 'info',
}))

const confirmClass = computed(() => ({
  'bg-red-600 hover:bg-red-500': props.type === 'danger',
  'bg-amber-600 hover:bg-amber-500': props.type === 'warning',
  'bg-indigo-600 hover:bg-indigo-500': props.type === 'info',
}))

const confirm = () => {
  emit('confirm')
  emit('update:modelValue', false)
}
</script>
