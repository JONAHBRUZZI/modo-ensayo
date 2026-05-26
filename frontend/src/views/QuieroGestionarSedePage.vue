<template>
  <div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-20 text-center">
    <div class="w-20 h-20 bg-purple-500/20 rounded-2xl flex items-center justify-center mx-auto mb-6">
      <svg class="w-10 h-10 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/></svg>
    </div>
    <h1 class="text-4xl font-bold text-white mb-4">Quiero Gestionar una Sede</h1>
    <p class="text-gray-400 text-lg max-w-2xl mx-auto mb-8">Administra tu propio espacio de ensayo. Registra salas, gestiona horarios, confirma clases y monitorea tus metricas.</p>
    <div class="card max-w-lg mx-auto text-left space-y-4">
      <h3 class="text-lg font-semibold text-white">Requisitos:</h3>
      <ul class="space-y-2 text-gray-300 text-sm">
        <li class="flex items-start space-x-2"><svg class="w-5 h-5 text-green-400 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg><span>Identidad verificada en la plataforma</span></li>
        <li class="flex items-start space-x-2"><svg class="w-5 h-5 text-green-400 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg><span>Espacio fisico con salas de ensayo</span></li>
        <li class="flex items-start space-x-2"><svg class="w-5 h-5 text-green-400 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg><span>Capacidad de gestion operativa</span></li>
      </ul>
      <router-link to="/sede/registro" class="btn-primary w-full text-center mt-4 block">Registrar mi Sede</router-link>
    </div>
  </div>
</template>
