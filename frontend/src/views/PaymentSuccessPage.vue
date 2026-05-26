<template>
  <div class="max-w-lg mx-auto px-4 py-20 text-center">
    <div class="w-20 h-20 bg-green-500/20 rounded-full flex items-center justify-center mx-auto mb-6">
      <svg class="w-10 h-10 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
      </svg>
    </div>
    <h1 class="text-3xl font-bold text-white mb-4">¡Pago Exitoso!</h1>
    <p class="text-gray-400 mb-2">Tu pago ha sido procesado correctamente.</p>

    <div v-if="paymentId" class="bg-[#161824] border border-gray-700 rounded-xl p-4 mb-6 text-left space-y-2">
      <div class="flex justify-between text-sm">
        <span class="text-gray-400">N° de operación</span>
        <span class="text-white font-mono">{{ paymentId }}</span>
      </div>
      <div class="flex justify-between text-sm">
        <span class="text-gray-400">Estado</span>
        <span class="text-green-400 font-semibold">Aprobado</span>
      </div>
      <div v-if="externalReference" class="flex justify-between text-sm">
        <span class="text-gray-400">Referencia</span>
        <span class="text-white font-mono text-xs">{{ externalReference }}</span>
      </div>
    </div>

    <p class="text-gray-500 text-sm mb-8">
      Tus inscripciones están activas. Los pagos quedan retenidos hasta que el administrador de sede confirme la realización de cada clase.
    </p>

    <div class="flex flex-col sm:flex-row gap-3 justify-center">
      <router-link to="/alumno/mis-clases" class="btn-primary">Ver mis clases</router-link>
      <router-link to="/alumno/pagos" class="btn-secondary">Historial de pagos</router-link>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const paymentId = computed(() => route.query.payment_id || route.query.paymentId || null)
const externalReference = computed(() => route.query.external_reference || null)
const status = computed(() => route.query.status || 'approved')
</script>
