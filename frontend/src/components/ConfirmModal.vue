<template>
  <Teleport to="body">
    <div v-if="show" class="fixed inset-0 z-[9999] flex items-center justify-center">
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="close"></div>
      <div class="relative bg-[#161824] border border-gray-700 rounded-2xl p-6 max-w-md w-full mx-4 shadow-2xl">
        <div class="text-center">
          <div class="w-12 h-12 bg-red-500/20 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg class="w-6 h-6 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z"/>
            </svg>
          </div>
          <h3 class="text-lg font-semibold text-white mb-2">{{ title }}</h3>
          <p class="text-gray-400 text-sm mb-6">{{ message }}</p>
          <div class="flex justify-center space-x-3">
            <button @click="close" class="btn-secondary text-sm">Cancelar</button>
            <button @click="confirm" class="btn-danger text-sm">{{ confirmText }}</button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
const props = defineProps({
  show: { type: Boolean, default: false },
  title: { type: String, default: 'Confirmar accion' },
  message: { type: String, default: 'Estas seguro de realizar esta accion?' },
  confirmText: { type: String, default: 'Confirmar' }
})

const emit = defineEmits(['close', 'confirm'])

function close() { emit('close') }
function confirm() { emit('confirm') }
</script>
