<template>
  <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-16">

    <div
      v-motion :initial="{ opacity: 0, y: 20 }"
      :enter="{ opacity: 1, y: 0, transition: { duration: 420 } }"
      class="text-center mb-12"
    >
      <div class="w-16 h-16 bg-purple-500/15 border border-purple-500/25 rounded-2xl flex items-center justify-center mx-auto mb-5">
        <svg class="w-8 h-8 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/>
        </svg>
      </div>
      <h1 class="text-4xl font-bold text-white mb-3">Gestiona tu Sede</h1>
      <p class="text-gray-400 text-lg max-w-xl mx-auto">
        Administra tus salas de ensayo, conecta con maestros y monitorea el rendimiento de tu espacio artístico.
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
          <div class="w-8 h-8 bg-purple-500/15 border border-purple-500/25 rounded-full flex items-center justify-center mx-auto mb-2 text-purple-400 text-sm font-semibold">{{ i+1 }}</div>
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
      <router-link to="/register" class="btn-primary w-full text-center block">Registrar mi Sede</router-link>
      <p class="text-center text-gray-600 text-xs mt-3">¿Ya tienes cuenta? <router-link to="/login" class="text-primary hover:underline">Inicia sesión</router-link></p>
    </div>

  </div>
</template>

<script setup>
const beneficios = [
  {
    title: 'Panel de control',
    desc: 'Gestiona salas, confirma clases y reagendamientos desde un solo lugar.',
    iconBg: 'bg-purple-500/15 text-purple-400',
    icon: `<svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/></svg>`
  },
  {
    title: 'Red de maestros',
    desc: 'Conecta con maestros verificados que necesitan espacio para dictar sus clases.',
    iconBg: 'bg-green-500/15 text-green-400',
    icon: `<svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z"/></svg>`
  },
  {
    title: 'Métricas en tiempo real',
    desc: 'Tasa de ocupación, ingresos, asistencia y conversión disponibles siempre.',
    iconBg: 'bg-yellow-500/15 text-yellow-400',
    icon: `<svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/></svg>`
  },
]

const pasos = [
  { title: 'Regístrate', desc: 'Crea tu cuenta y verifica tu identidad' },
  { title: 'Registra tu sede', desc: 'Ingresa datos y agrega tus salas' },
  { title: 'Recibe solicitudes', desc: 'Maestros reservan tus salas' },
  { title: 'Confirma y cobra', desc: 'Aprueba clases y monitorea métricas' },
]

const requisitos = [
  'Cuenta verificada con identidad en la plataforma',
  'Espacio físico con una o más salas de ensayo disponibles',
  'Capacidad de gestión operativa y confirmación de clases',
  'Compromiso con la disponibilidad y calidad del espacio',
]
</script>
