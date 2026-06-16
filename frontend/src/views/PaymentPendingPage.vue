<template>
  <div class="max-w-lg mx-auto px-4 py-20 text-center">
    <div
      v-motion
      :initial="{ opacity: 0, scale: 0.8 }"
      :enter="{ opacity: 1, scale: 1, transition: { duration: 500, type: 'spring', stiffness: 200 } }"
      class="w-20 h-20 bg-yellow-500/15 border border-yellow-500/30 rounded-full flex items-center justify-center mx-auto mb-6"
    >
      <svg class="w-10 h-10 text-yellow-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
      </svg>
    </div>
    <div
      v-motion
      :initial="{ opacity: 0, y: 16 }"
      :enter="{ opacity: 1, y: 0, transition: { duration: 400, delay: 150 } }"
    >
      <h1 class="text-3xl font-bold text-white mb-2">Pago pendiente</h1>
      <p class="text-gray-400 mb-8">Tu pago está siendo procesado. Te notificaremos cuando se complete.</p>
      <router-link to="/alumno/dashboard" class="btn-primary">Ir a Mi Espacio</router-link>
    </div>
  </div>
</template>
