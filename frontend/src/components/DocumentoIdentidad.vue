<template>
  <div class="space-y-2">
    <!-- Selector de tipo -->
    <div class="flex gap-2">
      <button
        v-for="tipo in tipos"
        :key="tipo.value"
        @click="tipoSeleccionado = tipo.value; numero = ''; error = ''"
        :class="tipoSeleccionado === tipo.value
          ? 'bg-indigo-600 border-indigo-500 text-white'
          : 'bg-[#0d0f1a] border-white/15 text-gray-400 hover:text-white hover:border-white/30'"
        class="flex-1 py-2 px-3 border rounded-lg text-xs font-medium transition-all text-center"
      >
        {{ tipo.label }}
      </button>
    </div>

    <!-- Campo de número -->
    <div class="relative">
      <input
        v-model="numero"
        :placeholder="placeholder"
        :maxlength="maxLength"
        @input="onInput"
        @blur="onBlur"
        class="w-full px-3 py-2.5 bg-[#0d0f1a] border rounded-lg text-white placeholder-gray-500 text-sm focus:outline-none focus:ring-2 transition-all pr-10"
        :class="error
          ? 'border-red-500/50 focus:ring-red-500'
          : isValid && numero
            ? 'border-emerald-500/50 focus:ring-emerald-500'
            : 'border-white/15 focus:ring-indigo-500'"
      />
      <!-- Indicador de estado -->
      <div class="absolute right-3 top-1/2 -translate-y-1/2">
        <svg v-if="isValid && numero" class="w-4 h-4 text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
        </svg>
        <svg v-else-if="error" class="w-4 h-4 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </div>
    </div>

    <!-- Mensaje de error o ayuda -->
    <p v-if="error" class="text-xs text-red-400 flex items-center gap-1">
      <svg class="w-3 h-3 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
      </svg>
      {{ error }}
    </p>
    <p v-else-if="hint" class="text-xs text-gray-600">{{ hint }}</p>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Object, default: () => ({ tipo: 'RUT', numero: '' }) },
})

const emit = defineEmits(['update:modelValue'])

const tipos = [
  { value: 'RUT', label: 'RUT / Cédula' },
  { value: 'PASAPORTE', label: 'Pasaporte' },
]

const tipoSeleccionado = ref(props.modelValue?.tipo || 'RUT')
const numero = ref(props.modelValue?.numero || '')
const error = ref('')

// Configuración por tipo
const config = computed(() => {
  if (tipoSeleccionado.value === 'RUT') {
    return {
      placeholder: 'Ej: 12345678-9 (ingresa solo números)',
      maxLength: 12, // 11.222.333-4
      hint: 'Ingresa los números y el dígito verificador — los puntos y guion se agregan solos',
    }
  }
  return {
    placeholder: 'Ej: A12345678 (Pasaporte)',
    maxLength: 20,
    hint: 'Formato según tu país: letras y/o números, sin espacios. Ej: AB1234567',
  }
})

const placeholder = computed(() => config.value.placeholder)
const maxLength = computed(() => config.value.maxLength)
const hint = computed(() => config.value.hint)

// Validación
const isValid = computed(() => {
  if (!numero.value) return false
  if (tipoSeleccionado.value === 'RUT') return validarRut(numero.value)
  if (tipoSeleccionado.value === 'PASAPORTE') return validarPasaporte(numero.value)
  return false
})

// Auto-formateo del RUT mientras escribe
const onInput = (e) => {
  if (tipoSeleccionado.value === 'RUT') {
    numero.value = formatearRut(e.target.value)
  } else {
    // Pasaporte: solo alfanumérico, mayúsculas
    numero.value = e.target.value.replace(/[^a-zA-Z0-9]/g, '').toUpperCase()
  }
  error.value = ''
  emitUpdate()
}

const onBlur = () => {
  if (!numero.value) return
  if (tipoSeleccionado.value === 'RUT' && !validarRut(numero.value)) {
    error.value = 'RUT inválido. Verifica el dígito verificador.'
  } else if (tipoSeleccionado.value === 'PASAPORTE' && !validarPasaporte(numero.value)) {
    error.value = 'Pasaporte inválido. Mínimo 6 caracteres alfanuméricos.'
  }
}

watch(tipoSeleccionado, () => { emitUpdate() })

const emitUpdate = () => {
  emit('update:modelValue', {
    tipo: tipoSeleccionado.value,
    numero: numero.value,
    valido: isValid.value,
  })
}

// --- Lógica RUT ---
const formatearRut = (raw) => {
  // Elimina todo excepto dígitos y K/k
  let clean = raw.replace(/[^0-9kK]/g, '').toUpperCase()
  if (clean.length === 0) return ''
  if (clean.length === 1) return clean

  const dv = clean.slice(-1)
  const body = clean.slice(0, -1)

  // Agregar puntos cada 3 dígitos desde la derecha
  const bodyFormatted = body.replace(/\B(?=(\d{3})+(?!\d))/g, '.')

  return `${bodyFormatted}-${dv}`
}

const validarRut = (rut) => {
  if (!rut) return false
  const clean = rut.replace(/[^0-9kK]/g, '').toUpperCase()
  if (clean.length < 2) return false

  const body = clean.slice(0, -1)
  const dv = clean.slice(-1)

  let sum = 0
  let mul = 2
  for (let i = body.length - 1; i >= 0; i--) {
    sum += parseInt(body[i]) * mul
    mul = mul === 7 ? 2 : mul + 1
  }
  const expected = 11 - (sum % 11)
  const calc = expected === 11 ? '0' : expected === 10 ? 'K' : String(expected)
  return calc === dv
}

// --- Lógica Pasaporte ---
const validarPasaporte = (num) => {
  if (!num) return false
  // Alfanumérico, entre 6 y 20 caracteres
  return /^[A-Z0-9]{6,20}$/.test(num)
}
</script>
