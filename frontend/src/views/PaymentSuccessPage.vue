<template>
  <div class="max-w-lg mx-auto px-4 py-20 text-center">
    <div
      v-motion
      :initial="{ opacity: 0, scale: 0.8 }"
      :enter="{ opacity: 1, scale: 1, transition: { duration: 500, type: 'spring', stiffness: 200 } }"
      class="w-20 h-20 bg-green-500/15 border border-green-500/30 rounded-full flex items-center justify-center mx-auto mb-6"
    >
      <svg class="w-10 h-10 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
      </svg>
    </div>

    <div
      v-motion
      :initial="{ opacity: 0, y: 16 }"
      :enter="{ opacity: 1, y: 0, transition: { duration: 400, delay: 150 } }"
    >
      <h1 class="text-3xl font-bold text-white mb-2">¡Pago exitoso!</h1>
      <p class="text-gray-400 mb-6">Tu inscripción ha sido procesada correctamente.</p>
    </div>

    <div
      v-if="paymentId"
      v-motion
      :initial="{ opacity: 0, y: 12 }"
      :enter="{ opacity: 1, y: 0, transition: { duration: 380, delay: 260 } }"
      class="card text-left space-y-3 mb-6"
    >
      <div class="flex justify-between text-sm py-1 border-b border-[var(--border-subtle)]">
        <span class="text-gray-500">N° de operación</span>
        <span class="text-white font-mono text-xs">{{ paymentId }}</span>
      </div>
      <div class="flex justify-between text-sm py-1 border-b border-[var(--border-subtle)]">
        <span class="text-gray-500">Estado</span>
        <span class="text-green-400 font-semibold flex items-center gap-1.5">
          <span class="w-1.5 h-1.5 bg-green-400 rounded-full inline-block"></span>
          Aprobado
        </span>
      </div>
      <div v-if="externalReference" class="flex justify-between text-sm py-1">
        <span class="text-gray-500">Referencia</span>
        <span class="text-white font-mono text-xs">{{ externalReference }}</span>
      </div>
    </div>

    <div
      v-motion
      :initial="{ opacity: 0 }"
      :enter="{ opacity: 1, transition: { duration: 400, delay: 360 } }"
    >
      <p class="text-gray-600 text-sm mb-8">
        Los pagos quedan retenidos hasta que el administrador de sede confirme la realización de cada clase.
      </p>
      <div class="flex flex-col sm:flex-row gap-3 justify-center">
        <router-link to="/alumno/mis-clases" class="btn-primary">Ver mis clases</router-link>
        <router-link to="/alumno/pagos" class="btn-secondary">Historial de pagos</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
const route = useRoute()
const paymentId = computed(() => route.query.payment_id)
const externalReference = computed(() => route.query.external_reference)
</script>
