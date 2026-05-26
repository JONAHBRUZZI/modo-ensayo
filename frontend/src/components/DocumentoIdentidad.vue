<template>
  <div class="space-y-4">
    <div>
      <label class="block text-sm font-medium text-gray-300 mb-1">Tipo de Documento</label>
      <div class="flex space-x-4">
        <label class="flex items-center space-x-2 cursor-pointer">
          <input type="radio" v-model="tipoDoc" value="RUT" class="text-primary focus:ring-primary" />
          <span class="text-gray-300">RUT</span>
        </label>
        <label class="flex items-center space-x-2 cursor-pointer">
          <input type="radio" v-model="tipoDoc" value="PASAPORTE" class="text-primary focus:ring-primary" />
          <span class="text-gray-300">Pasaporte</span>
        </label>
      </div>
    </div>
    <div>
      <label class="block text-sm font-medium text-gray-300 mb-1">Numero de Documento</label>
      <input
        type="text"
        v-model="documentNumber"
        :placeholder="tipoDoc === 'RUT' ? '12.345.678-9' : 'Numero de pasaporte'"
        class="input-field"
        @input="emitUpdate"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const tipoDoc = ref('RUT')
const documentNumber = ref('')

const emit = defineEmits(['update:modelValue'])

watch([tipoDoc, documentNumber], () => {
  emitUpdate()
})

function emitUpdate() {
  emit('update:modelValue', {
    tipo: tipoDoc.value,
    numero: documentNumber.value
  })
}
</script>
