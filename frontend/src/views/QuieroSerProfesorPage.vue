<template>
  <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-16">

    <div
      v-motion :initial="{ opacity: 0, y: 20 }"
      :enter="{ opacity: 1, y: 0, transition: { duration: 420 } }"
      class="text-center mb-12"
    >
      <div class="w-16 h-16 bg-primary/15 border border-primary/25 rounded-2xl flex items-center justify-center mx-auto mb-5">
        <svg class="w-8 h-8 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
        </svg>
      </div>
      <h1 class="text-4xl font-bold text-white mb-3">Conviértete en Maestro</h1>
      <p class="text-gray-400 text-lg max-w-xl mx-auto">
        Comparte tu talento, gestiona tus clases y genera ingresos enseñando lo que más te apasiona.
      </p>
    </div>

    <!-- Beneficios -->
    <div
      v-motion :initial="{ opacity: 0, y: 16 }"
      :visible="{ opacity: 1, y: 0, transition: { duration: 400, delay: 80 } }"
      class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-10"
    >
      <div v-for="b in beneficios" :key="b.title" class="card text-center py-6">
        <div class="w-10 h-10 rounded-xl flex items-center justify-center mx-auto mb-3" :class="b.iconBg" v-html="b.icon"></div>
        <h3 class="text-white font-semibold text-sm mb-1">{{ b.title }}</h3>
        <p class="text-gray-500 text-xs leading-relaxed">{{ b.desc }}</p>
      </div>
    </div>

    <!-- Cómo funciona -->
    <div
      v-motion :initial="{ opacity: 0, y: 16 }"
      :visible="{ opacity: 1, y: 0, transition: { duration: 400, delay: 120 } }"
      class="card mb-8"
    >
      <h2 class="text-sm font-medium text-gray-500 uppercase tracking-wider mb-5">¿Cómo funciona?</h2>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div v-for="(paso, i) in pasos" :key="paso.title" class="text-center">
          <div class="w-8 h-8 bg-primary/15 border border-primary/25 rounded-full flex items-center justify-center mx-auto mb-2 text-primary text-sm font-semibold">{{ i+1 }}</div>
          <p class="text-white text-sm font-medium mb-1">{{ paso.title }}</p>
          <p class="text-gray-600 text-xs">{{ paso.desc }}</p>
        </div>
      </div>
    </div>

    <!-- Requisitos + CTA -->
    <div
      v-motion :initial="{ opacity: 0, y: 16 }"
      :visible="{ opacity: 1, y: 0, transition: { duration: 400, delay: 160 } }"
      class="card max-w-lg mx-auto"
    >
      <h2 class="text-sm font-medium text-gray-500 uppercase tracking-wider mb-4">Requisitos</h2>
      <ul class="space-y-3 mb-6">
        <li v-for="r in requisitos" :key="r" class="flex items-start gap-3">
          <div class="w-5 h-5 bg-green-500/20 rounded-full flex items-center justify-center shrink-0 mt-0.5">
            <svg class="w-3 h-3 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/></svg>
          </div>
          <span class="text-gray-300 text-sm">{{ r }}</span>
        </li>
      </ul>
      <router-link to="/register" class="btn-primary w-full text-center block">Registrarme como Maestro</router-link>
      <p class="text-center text-gray-600 text-xs mt-3">¿Ya tienes cuenta? <router-link to="/login" class="text-primary hover:underline">Inicia sesión</router-link></p>
    </div>

  </div>
</template>

<script setup>
const beneficios = [
  {
    title: 'Gestión completa',
    desc: 'Crea clases, define horarios y cupos desde tu panel.',
    iconBg: 'bg-primary/15 text-primary',
    icon: `<svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/></svg>`
  },
  {
    title: 'Pagos seguros',
    desc: 'Cobra automáticamente cuando tu clase se confirma como realizada.',
    iconBg: 'bg-green-500/15 text-green-400',
    icon: `<svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z"/></svg>`
  },
  {
    title: 'Métricas y asistencia',
    desc: 'Visualiza tu ocupación, ingresos y tasa de asistencia en tiempo real.',
    iconBg: 'bg-yellow-500/15 text-yellow-400',
    icon: `<svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/></svg>`
  },
]

const pasos = [
  { title: 'Crea tu cuenta', desc: 'Regístrate y verifica tu identidad' },
  { title: 'Completa tu perfil', desc: 'Agrega tu experiencia y disciplinas' },
  { title: 'Agenda una sala', desc: 'Reserva espacio en una sede asociada' },
  { title: 'Publica y enseña', desc: 'Tus alumnos reservan y tú cobras' },
]

const requisitos = [
  'Cuenta verificada con identidad en la plataforma',
  'Experiencia comprobable en tu disciplina artística',
  'Acceso a internet y dispositivo para gestionar tus clases',
  'Compromiso con la calidad y puntualidad',
]
</script>
