<template>
  <BottomSheet :model-value="abierto" @update:model-value="$event || cerrar()">
    <h3 class="text-[var(--text-primary)] font-semibold text-lg mb-1">Seleccionar Borrador</h3>
    <p class="text-[var(--text-secondary)] text-sm mb-4">
      Asigna la sala reservada a un borrador existente y publicalo.
    </p>

    <div v-if="loading" class="text-[var(--text-secondary)] text-sm py-4 text-center">
      Cargando borradores...
    </div>
    <div v-else-if="borradores.length === 0" class="text-center py-4">
      <p class="text-[var(--text-secondary)] text-sm">No tienes borradores sin sala disponibles.</p>
      <router-link to="/profesor/crear-borrador" class="text-primary text-sm underline mt-2 inline-block">
        Crear un borrador
      </router-link>
    </div>
    <div v-else class="space-y-2 max-h-64 overflow-y-auto mb-4">
      <button
        v-for="b in borradores"
        :key="b.id"
        type="button"
        @click="seleccionado = b"
        :class="[
          'w-full text-left p-3 rounded-xl border transition-colors',
          seleccionado?.id === b.id
            ? 'border-primary bg-primary/10'
            : 'border-white/10 hover:border-white/20 bg-[var(--bg-base)]'
        ]"
      >
        <p class="text-[var(--text-primary)] text-sm font-medium">{{ b.title }}</p>
        <p class="text-[var(--text-secondary)] text-xs mt-0.5">
          {{ b.discipline || 'Sin disciplina' }}
          <span v-if="b.level"> · {{ b.level }}</span>
          <span v-if="b.price != null"> · ${{ b.price?.toLocaleString('es-CL') }}</span>
        </p>
      </button>
    </div>

    <p v-if="errorMsg" class="text-red-400 text-sm mb-3">{{ errorMsg }}</p>

    <div class="flex gap-3">
      <button
        type="button"
        @click="asignar"
        :disabled="!seleccionado || procesando"
        class="btn-primary flex-1 text-sm"
      >
        {{ procesando ? 'Publicando...' : 'Asignar y Publicar' }}
      </button>
      <button type="button" @click="cerrar" class="btn-secondary text-sm px-4">
        Cancelar
      </button>
    </div>
  </BottomSheet>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import classService from '@/services/classService'
import BottomSheet from '@/components/BottomSheet.vue'

const props = defineProps({
  abierto: { type: Boolean, default: false },
  reservationId: { type: String, default: null },
  roomId: { type: String, default: null },
  startTime: { type: String, default: null },
  duration: { type: Number, default: 60 }
})

const emit = defineEmits(['close', 'applied'])

const borradores = ref([])
const loading = ref(false)
const seleccionado = ref(null)
const procesando = ref(false)
const errorMsg = ref('')

watch(() => props.abierto, (val) => {
  if (val) cargarBorradores()
})

function cerrar() {
  if (procesando.value) return
  seleccionado.value = null
  errorMsg.value = ''
  emit('close')
}

async function cargarBorradores() {
  loading.value = true
  seleccionado.value = null
  errorMsg.value = ''
  try {
    const todos = await classService.getTeacherDrafts()
    borradores.value = (Array.isArray(todos) ? todos : []).filter(b => !b.roomId)
  } catch {
    borradores.value = []
  }
  loading.value = false
}

async function asignar() {
  if (!seleccionado.value) return
  procesando.value = true
  errorMsg.value = ''
  try {
    await classService.assignReserva(seleccionado.value.id, {
      roomId: props.roomId,
      startTime: props.startTime,
      duration: props.duration
    })
    if (props.reservationId) {
      await classService.deleteDraft(props.reservationId)
    }
    emit('applied', seleccionado.value)
    cerrar()
  } catch (e) {
    errorMsg.value = e?.response?.data?.message || 'Error al asignar el borrador'
  }
  procesando.value = false
}
</script>
