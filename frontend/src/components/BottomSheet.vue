<template>
  <Teleport to="body">
    <div v-if="modelValue" class="fixed inset-0 bg-black/60 flex items-center justify-center z-50 px-4" @click.self="$emit('update:modelValue', false)">
      <div class="bg-[var(--bg-elevated)] rounded-2xl border border-white/10 w-full max-w-lg p-6">
        <slot />
      </div>
    </div>
  </Teleport>
</template>

<script setup>
defineProps({ modelValue: Boolean })
defineEmits(['update:modelValue'])
</script>
