<template>
  <div class="space-y-3">
    <p v-if="label" class="text-sm text-gray-300 font-medium">{{ label }}</p>

    <!-- Lista de documentos existentes -->
    <div v-if="docs.length > 0" class="space-y-2">
      <div
        v-for="(doc, i) in docs"
        :key="i"
        class="flex items-center justify-between p-3 bg-dark-bg rounded-lg border border-dark-border">
        <div class="flex items-center gap-3 min-w-0">
          <svg class="w-5 h-5 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
          </svg>
          <div class="min-w-0">
            <p class="text-sm text-white truncate">{{ doc.nombre || doc.file?.name || 'Documento' }}</p>
            <span v-if="doc.tipo" class="text-xs text-gray-500">{{ tipoLabel(doc.tipo) }}</span>
          </div>
        </div>
        <button v-if="!readonly" @click="$emit('remove', i)" class="text-red-400 text-xs hover:underline flex-shrink-0 ml-2">
          Quitar
        </button>
      </div>
    </div>

    <!-- Upload (si no es readonly) -->
    <div v-if="!readonly" class="space-y-2">
      <div v-for="(slot, idx) in slots" :key="idx" class="border border-dark-border rounded-lg p-3 space-y-2">
        <div class="flex items-center justify-between">
          <p class="text-xs text-gray-400 font-medium flex items-center gap-1.5">
            <span :class="slot.required ? 'text-red-400' : 'text-gray-500'">{{ slot.required ? '*' : '' }}</span>
            {{ tipoLabel(slot.tipo) }}
            <span v-if="slot.required" class="text-red-400/70 text-[10px]">Requerido</span>
            <span v-else class="text-gray-600 text-[10px]">Opcional</span>
          </p>
          <button @click="$emit('skip', idx)" class="text-gray-600 hover:text-gray-400 text-xs transition-colors">
            Saltar
          </button>
        </div>

        <input
          type="file"
          :accept="accept"
          @change="onFileChange($event, idx)"
          class="block w-full text-sm text-gray-400
            file:mr-4 file:py-2 file:px-4 file:rounded file:border-0
            file:text-sm file:bg-primary file:text-white
            hover:file:bg-primary/80 file:cursor-pointer
            file:transition-colors" />

        <div v-if="slot.uploading" class="text-xs text-gray-400">Subiendo...</div>
        <div v-if="slot.uploaded" class="text-xs text-green-400 flex items-center gap-1">
          <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
          </svg>
          {{ slot.file?.name }} — listo
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  label: { type: String, default: '' },
  docs: { type: Array, default: () => [] },
  slots: { type: Array, default: () => [] },
  readonly: { type: Boolean, default: false },
  accept: { type: String, default: '.pdf,image/*' }
})

const emit = defineEmits(['remove', 'upload', 'skip'])

const tipoLabels = {
  RUT_EMPRESA: 'RUT Empresa',
  CEDULA_IDENTIDAD: 'Cedula Identidad',
  INICIO_ACTIVIDADES_F4415: 'Inicio Actividades F4415',
  CERTIFICADO_SITUACION_TRIBUTARIA: 'Cert. Situacion Tributaria',
  PERMISO_MUNICIPAL: 'Permiso Municipal',
  CONTRATO_ARRIENDO: 'Contrato Arriendo',
  COMPROBANTE_DOMICILIO: 'Comprobante de Domicilio',
  OTRO: 'Otro'
}

function tipoLabel(tipo) {
  if (typeof tipo === 'object' && tipo?.name) return tipo.name
  return tipoLabels[tipo] || tipo
}

async function onFileChange(event, idx) {
  const file = event.target.files?.[0]
  if (!file) return
  emit('upload', { idx, file })
}
</script>
