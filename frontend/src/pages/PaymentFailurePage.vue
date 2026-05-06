<template>
  <div class="min-h-screen flex items-center justify-center py-12 px-4">
    <div class="max-w-md w-full text-center">
      <div class="bg-[#161824] p-8 rounded-2xl border border-white/10 shadow-2xl">
        <div class="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-red-500/20 border border-red-500/30 mb-5">
          <svg class="h-8 w-8 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </div>
        <h2 class="text-3xl font-bold text-white mb-2">Pago Cancelado</h2>
        <p class="text-gray-400 mb-8 text-sm leading-relaxed">El pago no se completó. No te preocupes, no se ha realizado ningún cargo.</p>
        <div class="space-y-3">
          <router-link to="/cart"
                       class="w-full flex justify-center py-3 px-4 rounded-xl text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-500 transition-colors">
            Intentar nuevamente
          </router-link>
          <router-link to="/classes"
                       class="w-full flex justify-center py-3 px-4 rounded-xl text-sm font-medium border border-white/15 text-gray-300 hover:bg-white/5 transition-colors">
            Ver otras clases
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>
