<template>
  <div>
    <div
      @dragover.prevent="isDragging = true"
      @dragleave.prevent="isDragging = false"
      @drop.prevent="onDrop"
      @click="$refs.input.click()"
      :class="isDragging ? 'border-indigo-500 bg-indigo-500/10' : 'border-white/15 hover:border-white/30 bg-white/3'"
      class="relative border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-all duration-200"
    >
      <input ref="input" type="file" :accept="accept" class="hidden" @change="onFileChange" />

      <div v-if="!preview">
        <div class="mx-auto w-12 h-12 bg-white/5 rounded-full flex items-center justify-center mb-3">
          <svg class="w-6 h-6 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
          </svg>
        </div>
        <p class="text-sm text-gray-300 font-medium">Arrastra tu archivo aquí</p>
        <p class="text-xs text-gray-500 mt-1">o haz clic para seleccionar · {{ hint }}</p>
      </div>

      <div v-else class="space-y-3">
        <img v-if="isImage" :src="preview" class="mx-auto max-h-48 rounded-lg object-contain" />
        <div v-else class="flex items-center gap-3 bg-white/5 rounded-lg p-3 text-left">
          <svg class="w-8 h-8 text-indigo-400 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <div>
            <p class="text-sm text-white font-medium truncate">{{ fileName }}</p>
            <p class="text-xs text-gray-500">{{ fileSize }}</p>
          </div>
        </div>
        <button @click.stop="clearFile" class="text-xs text-red-400 hover:text-red-300 transition-colors">
          Eliminar archivo
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  accept: { type: String, default: 'image/*,.pdf' },
  hint: { type: String, default: 'JPG, PNG o PDF — máx. 5MB' },
})

const emit = defineEmits(['update:file'])

const isDragging = ref(false)
const preview = ref(null)
const fileName = ref('')
const fileSize = ref('')
const file = ref(null)

const isImage = computed(() => file.value?.type?.startsWith('image/'))

const processFile = (f) => {
  if (!f) return
  file.value = f
  fileName.value = f.name
  fileSize.value = f.size > 1024 * 1024
    ? `${(f.size / 1024 / 1024).toFixed(1)} MB`
    : `${Math.round(f.size / 1024)} KB`

  if (f.type.startsWith('image/')) {
    const reader = new FileReader()
    reader.onload = (e) => { preview.value = e.target.result }
    reader.readAsDataURL(f)
  } else {
    preview.value = 'file'
  }
  emit('update:file', f)
}

const onFileChange = (e) => { processFile(e.target.files[0]) }
const onDrop = (e) => {
  isDragging.value = false
  processFile(e.dataTransfer.files[0])
}

const clearFile = () => {
  preview.value = null
  file.value = null
  fileName.value = ''
  fileSize.value = ''
  emit('update:file', null)
}
</script>
