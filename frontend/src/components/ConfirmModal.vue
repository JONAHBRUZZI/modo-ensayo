<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition-opacity duration-200"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-150"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div v-if="show" class="fixed inset-0 z-[9999] flex items-center justify-center">
        <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="close"></div>
        <Transition
          enter-active-class="transition-all duration-200"
          enter-from-class="opacity-0 scale-95 translate-y-2"
          enter-to-class="opacity-100 scale-100 translate-y-0"
          leave-active-class="transition-all duration-150"
          leave-from-class="opacity-100 scale-100"
          leave-to-class="opacity-0 scale-95"
        >
          <div v-if="show" class="relative bg-[var(--bg-surface)] border border-[var(--border-default)] rounded-2xl p-6 max-w-md w-full mx-4">
            <div class="text-center">
              <div class="w-12 h-12 bg-red-500/15 border border-red-500/25 rounded-full flex items-center justify-center mx-auto mb-4">
                <svg class="w-6 h-6 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z"/>
                </svg>
              </div>
              <h3 class="text-base font-semibold text-[var(--text-primary)] mb-2">{{ title }}</h3>
              <p class="text-[var(--text-secondary)] text-sm mb-6 leading-relaxed">{{ message }}</p>
              <div class="flex justify-center gap-3">
                <button @click="close" class="btn-secondary text-sm px-5">Cancelar</button>
                <button @click="confirm" class="btn-danger text-sm px-5">{{ confirmText }}</button>
              </div>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
const props = defineProps({
  show:        { type: Boolean, default: false },
  title:       { type: String,  default: 'Confirmar acción' },
  message:     { type: String,  default: '¿¿Estás seguro de realizar esta acción?' },
  confirmText: { type: String,  default: 'Confirmar' },
})
const emit = defineEmits(['close', 'confirm'])
function close()   { emit('close') }
function confirm() { emit('confirm') }
</script>
