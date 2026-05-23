<template>
  <div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-20 text-center">
    <div class="w-20 h-20 bg-primary/20 rounded-2xl flex items-center justify-center mx-auto mb-6">
      <svg class="w-10 h-10 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/></svg>
    </div>
    <h1 class="text-4xl font-bold text-white mb-4">Quiero ser Profesor</h1>
    <p class="text-gray-400 text-lg max-w-2xl mx-auto mb-8">Comparte tu talento musical. En Modo Ensayo puedes crear y gestionar tus propias clases, conectar con alumnos y generar ingresos.</p>
    <div class="card max-w-lg mx-auto text-left space-y-4">
      <h3 class="text-lg font-semibold text-white">Requisitos:</h3>
      <ul class="space-y-2 text-gray-300 text-sm">
        <li class="flex items-start space-x-2"><svg class="w-5 h-5 text-green-400 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg><span>Identidad verificada en la plataforma</span></li>
        <li class="flex items-start space-x-2"><svg class="w-5 h-5 text-green-400 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg><span>Experiencia comprobable en tu disciplina</span></li>
        <li class="flex items-start space-x-2"><svg class="w-5 h-5 text-green-400 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg><span>Compromiso con la calidad educativa</span></li>
      </ul>
      <router-link to="/profesor/registro" class="btn-primary w-full text-center mt-4 block">Registrarme como Profesor</router-link>
    </div>
  </div>
</template>
