<template>
  <div class="max-w-lg mx-auto px-4 py-20 text-center">
    <div class="w-20 h-20 bg-red-500/20 rounded-full flex items-center justify-center mx-auto mb-6">
      <svg class="w-10 h-10 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
    </div>
    <h1 class="text-3xl font-bold text-white mb-4">Pago Rechazado</h1>
    <p class="text-gray-400 mb-8">Hubo un problema al procesar tu pago. Intenta nuevamente.</p>
    <router-link to="/cart" class="btn-primary">Volver al Carrito</router-link>
  </div>
</template>
