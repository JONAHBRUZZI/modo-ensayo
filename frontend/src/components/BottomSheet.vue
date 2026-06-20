<template>
  <Teleport to="body">
    <div v-if="modelValue" class="fixed inset-0 bg-black/60 flex items-end justify-center z-50" @click.self="$emit('update:modelValue', false)">
      <div class="bg-[var(--bg-elevated)] rounded-t-2xl border-t border-x border-white/10 w-full max-w-lg">
        <div class="w-10 h-1 bg-white/20 rounded-full mx-auto mt-3 mb-1"></div>
        <div class="p-6">
          <slot />
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
defineProps({ modelValue: Boolean })
defineEmits(['update:modelValue'])
</script>
