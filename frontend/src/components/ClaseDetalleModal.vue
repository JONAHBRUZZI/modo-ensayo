<template>
  <BottomSheet :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)">
    <div v-if="!clase" class="text-gray-500 text-sm py-6 text-center">Sin datos de la clase.</div>
    <div v-else>
      <div class="flex items-start justify-between gap-3 mb-4">
        <div>
          <h3 class="text-xl font-semibold text-white">{{ clase.title }}</h3>
          <p class="text-gray-400 text-sm">{{ clase.discipline }} · {{ clase.level }}</p>
        </div>
        <EstadoBadge :status="clase.status" />
      </div>

      <!-- Lo que recibe el profe -->
      <div v-if="honorario > 0" class="mb-4 rounded-xl bg-green-500/10 border border-green-500/20 p-3">
        <p class="text-gray-400 text-xs uppercase tracking-wider mb-0.5">Tu honorario por esta clase</p>
        <p class="text-2xl font-bold text-green-400">${{ honorario.toLocaleString('es-CL') }}</p>
      </div>

      <!-- Datos de la clase -->
      <div class="grid grid-cols-2 gap-x-4 gap-y-3 text-sm mb-5">
        <div><span class="text-gray-500">Sala:</span> <span class="text-white">{{ clase.room?.name || '—' }}</span></div>
        <div><span class="text-gray-500">Sede:</span> <span class="text-white">{{ clase.room?.venue?.name || '—' }}</span></div>
        <div v-if="clase.room?.venue?.address" class="col-span-2"><span class="text-gray-500">Dirección:</span> <span class="text-white">{{ clase.room.venue.address }}</span></div>
        <div><span class="text-gray-500">Fecha:</span> <span class="text-white">{{ clase.startTime ? formatDate(clase.startTime) : '—' }}</span></div>
        <div><span class="text-gray-500">Hora:</span> <span class="text-white">{{ clase.startTime ? formatTime(clase.startTime) : '—' }}<span v-if="clase.endTime"> – {{ formatTime(clase.endTime) }}</span></span></div>
        <div><span class="text-gray-500">Duración:</span> <span class="text-white">{{ clase.duration || 0 }} min</span></div>
        <div><span class="text-gray-500">Cupo:</span> <span class="text-white">{{ alumnos.length }} / {{ clase.capacity }}</span></div>
        <div><span class="text-gray-500">Tipo:</span> <span class="text-white">{{ clase.tipoClase === 'ASIGNADA' ? 'De sede' : 'Propia' }}</span></div>
      </div>

      <div v-if="clase.description" class="mb-5">
        <p class="text-gray-500 text-xs uppercase tracking-wider mb-1">De qué trata</p>
        <p class="text-gray-300 text-sm">{{ clase.description }}</p>
      </div>

      <!-- Alumnos -->
      <div>
        <p class="text-gray-500 text-xs uppercase tracking-wider mb-2">Alumnos ({{ alumnos.length }})</p>
        <div v-if="alumnosLoading" class="text-gray-500 text-sm py-2">Cargando alumnos...</div>
        <div v-else-if="alumnos.length === 0" class="text-gray-500 text-sm py-2">Aún no hay alumnos inscritos.</div>
        <div v-else class="space-y-2">
          <div v-for="al in alumnos" :key="al.enrollmentId" class="flex items-center justify-between border-t border-white/5 pt-2 first:border-0 first:pt-0">
            <div>
              <p class="text-white text-sm">{{ al.attendeeName }}</p>
              <p class="text-gray-500 text-xs">{{ al.studentEmail }}</p>
            </div>
            <span
              :class="[
                'text-xs px-2 py-0.5 rounded-full',
                al.present === true ? 'bg-green-500/20 text-green-400'
                : al.present === false ? 'bg-red-500/20 text-red-400'
                : 'bg-white/5 text-gray-400'
              ]"
            >
              {{ al.present === true ? 'Presente' : al.present === false ? 'Ausente' : 'Inscrito' }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </BottomSheet>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import classService from '@/services/classService'
import BottomSheet from '@/components/BottomSheet.vue'
import EstadoBadge from '@/components/EstadoBadge.vue'
import { formatDate, formatTime } from '@/utils/dateFormatter'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  clase: { type: Object, default: null }
})
defineEmits(['update:modelValue'])

const alumnos = ref([])
const alumnosLoading = ref(false)

const honorario = computed(() =>
  props.clase?.tipoClase === 'ASIGNADA' ? Number(props.clase?.honorario || 0) : 0
)

// Al abrir el modal con una clase, cargar sus alumnos.
watch(() => [props.modelValue, props.clase?.id], async ([abierto]) => {
  if (!abierto || !props.clase?.id) return
  alumnos.value = []
  alumnosLoading.value = true
  try {
    alumnos.value = await classService.getClassStudents(props.clase.id)
  } catch { alumnos.value = [] }
  alumnosLoading.value = false
})
</script>
