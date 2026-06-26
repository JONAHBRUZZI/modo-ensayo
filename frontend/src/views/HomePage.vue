<template>
  <div class="home">

    <!-- Capa de particulas: cubre toda la portada de forma ambiental -->
    <div class="page-particles" aria-hidden="true">
      <span v-for="n in 12" :key="n" class="particle" :style="{ left: particleX(n), animationDelay: (n * 0.6) + 's', animationDuration: (8 + (n % 5)) + 's' }" />
    </div>

    <!-- HERO -->
    <section class="hero">
      <div class="hero-glow" aria-hidden="true"></div>

      <div class="hero-inner max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="hero-grid">

          <!-- Texto -->
          <div class="hero-text">
            <div
              v-motion :initial="{ opacity: 0, y: 20 }"
              :enter="{ opacity: 1, y: 0, transition: { duration: 500 } }"
              class="hero-eyebrow"
            >
              <span class="eyebrow-dot" aria-hidden="true"></span>
              Plataforma de danza y musica en Chile
            </div>

            <h1
              v-motion :initial="{ opacity: 0, y: 24 }"
              :enter="{ opacity: 1, y: 0, transition: { duration: 550, delay: 80 } }"
              class="hero-title"
            >
              Tu <em>arte</em> encuentra<br>su espacio aqui
            </h1>

            <p
              v-motion :initial="{ opacity: 0, y: 20 }"
              :enter="{ opacity: 1, y: 0, transition: { duration: 500, delay: 160 } }"
              class="hero-sub"
            >
              Conectamos alumnos, maestros y salas de ensayo en un solo lugar.
              Reserva, aprende y crece.
            </p>

            <div
              v-motion :initial="{ opacity: 0, y: 16 }"
              :enter="{ opacity: 1, y: 0, transition: { duration: 450, delay: 240 } }"
              class="hero-ctas"
            >
              <router-link to="/classes" class="btn-hero-primary">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
                Explorar clases
              </router-link>
              <router-link to="/register" class="btn-hero-secondary">Crear cuenta →</router-link>
            </div>

            <div
              v-motion :initial="{ opacity: 0 }"
              :enter="{ opacity: 1, transition: { duration: 500, delay: 320 } }"
              class="hero-roles-hint"
            >
              <span>¿Enseñas o tienes un espacio?</span>
              <router-link to="/quiero-ser-profesor">Publica tus clases</router-link>
              <span class="hint-dot" aria-hidden="true">·</span>
              <router-link to="/quiero-gestionar-sede">Registra tu espacio</router-link>
            </div>

            <div
              v-motion :initial="{ opacity: 0 }"
              :enter="{ opacity: 1, transition: { duration: 600, delay: 400 } }"
              ref="statsRowRef"
              class="stats-row"
            >
              <div class="stat-item"><span class="stat-num">+{{ displayClases }}</span><span class="stat-label">Clases activas</span></div>
              <div class="stat-divider" aria-hidden="true"></div>
              <div class="stat-item"><span class="stat-num">+{{ displayMaestros }}</span><span class="stat-label">Maestros</span></div>
              <div class="stat-divider" aria-hidden="true"></div>
              <div class="stat-item"><span class="stat-num">+{{ displaySedes }}</span><span class="stat-label">Sedes</span></div>
            </div>
          </div>

          <!-- Imagen crossfade -->
          <div
            v-motion :initial="{ opacity: 0, scale: 0.96 }"
            :enter="{ opacity: 1, scale: 1, transition: { duration: 700, delay: 300 } }"
            class="hero-visual" aria-hidden="true"
          >
            <div class="hero-img-wrap">
              <img v-for="(img, i) in heroImages" :key="i" :src="img"
                :class="['hero-img', { 'hero-img--active': i === activeImg }]" alt="" />
              <div class="hero-dots">
                <button v-for="(_, i) in heroImages" :key="i"
                  :class="['hero-dot', { 'hero-dot--active': i === activeImg }]"
                  @click="goToImg(i)" :aria-label="`Imagen ${i + 1}`" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- QUÉ ES MODO ENSAYO -->
    <section class="about-section">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div
          v-motion :initial="{ opacity: 0, y: 20 }"
          :visible="{ opacity: 1, y: 0, transition: { duration: 450 } }"
          class="about-inner"
        >
          <p class="section-eyebrow">¿Qué es Modo Ensayo?</p>
          <h2 class="about-title">El marketplace de artes escénicas y música de Chile</h2>
          <p class="about-desc">
            Somos una plataforma digital que conecta a quienes quieren aprender danza o música con los mejores maestros del país,
            y que facilita la gestión de salas de ensayo para centros culturales y estudios. Todo en un solo lugar,
            con pagos seguros y seguimiento en tiempo real.
          </p>
          <div class="disciplines">
            <span v-for="d in disciplines" :key="d" class="discipline-pill">{{ d }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- PAGOS PROTEGIDOS (diferenciador) -->
    <section class="protect-section">
      <div class="protect-glow" aria-hidden="true"></div>
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div
          v-motion :initial="{ opacity: 0, y: 20 }"
          :visible="{ opacity: 1, y: 0, transition: { duration: 450 } }"
          class="protect-head"
        >
          <p class="section-eyebrow">Pagos protegidos</p>
          <h2 class="protect-title">Tu dinero se libera <em>solo</em> cuando la clase se realiza</h2>
          <p class="protect-sub">
            Nada de pagar y cruzar los dedos. Protegemos a alumnos y maestros por igual:
            el pago queda resguardado hasta que la clase ocurra de verdad.
          </p>
        </div>

        <div class="protect-grid">
          <div
            v-for="(p, i) in protections"
            :key="p.title"
            v-motion :initial="{ opacity: 0, y: 24 }"
            :visible="{ opacity: 1, y: 0, transition: { duration: 420, delay: i * 100 } }"
            class="protect-card"
          >
            <div :class="['protect-icon', p.iconClass]" v-html="p.icon" aria-hidden="true"></div>
            <h3 class="protect-card-title">{{ p.title }}</h3>
            <p class="protect-card-desc">{{ p.desc }}</p>
          </div>
        </div>

        <div
          v-motion :initial="{ opacity: 0 }"
          :visible="{ opacity: 1, transition: { duration: 500, delay: 200 } }"
          class="trust-bar"
        >
          <div v-for="b in trustBadges" :key="b.label" class="trust-badge">
            <span class="trust-badge-icon" v-html="b.icon" aria-hidden="true"></span>
            {{ b.label }}
          </div>
        </div>
      </div>
    </section>

    <!-- PARA QUIÉN ES -->
    <section class="roles-section">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <p class="section-eyebrow">¿Para quién es?</p>
        <div class="roles-grid">
          <div
            v-for="(role, i) in roles"
            :key="role.title"
            v-motion :initial="{ opacity: 0, y: 28 }"
            :visible="{ opacity: 1, y: 0, transition: { duration: 420, delay: i * 100 } }"
            class="role-card"
          >
            <div :class="['role-icon', role.iconClass]" v-html="role.icon" aria-hidden="true"></div>
            <h3 class="role-title">{{ role.title }}</h3>
            <p class="role-desc">{{ role.desc }}</p>
            <ul class="role-list">
              <li v-for="item in role.items" :key="item" class="role-item">
                <svg class="w-3.5 h-3.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/></svg>
                {{ item }}
              </li>
            </ul>
            <p v-if="role.note" class="role-note" v-html="role.note"></p>
            <router-link :to="role.cta.to" :class="['role-cta', role.cta.style]">{{ role.cta.label }}</router-link>
          </div>
        </div>
      </div>
    </section>

    <!-- FEATURES -->
    <section class="features-section">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <p class="section-eyebrow">&iquest;Que puedes hacer?</p>
        <div class="features-grid">
          <div
            v-for="(feat, i) in features"
            :key="feat.title"
            v-motion :initial="{ opacity: 0, y: 28 }"
            :visible="{ opacity: 1, y: 0, transition: { duration: 450, delay: i * 80 } }"
            class="feat-card"
          >
            <div :class="['feat-icon', feat.iconClass]" aria-hidden="true" v-html="feat.icon"></div>
            <h3 class="feat-title">{{ feat.title }}</h3>
            <p class="feat-desc">{{ feat.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- CÓMO FUNCIONA -->
    <section class="steps-section">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <p class="section-eyebrow">¿Cómo funciona?</p>
        <div class="steps-tabs">
          <button
            v-for="tab in stepTabs" :key="tab.key"
            :class="['steps-tab', { 'steps-tab--active': activeTab === tab.key }]"
            @click="activeTab = tab.key"
          >{{ tab.label }}</button>
        </div>
        <div class="steps-grid">
          <div
            v-for="(step, i) in currentSteps"
            :key="step.title"
            v-motion :initial="{ opacity: 0, y: 20 }"
            :visible="{ opacity: 1, y: 0, transition: { duration: 400, delay: i * 100 } }"
            class="step-item"
          >
            <div class="step-num">{{ i + 1 }}</div>
            <h4 class="step-title">{{ step.title }}</h4>
            <p class="step-desc">{{ step.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA FINAL -->
    <section class="cta-section">
      <div
        v-motion :initial="{ opacity: 0, scale: 0.97 }"
        :visible="{ opacity: 1, scale: 1, transition: { duration: 500 } }"
        class="cta-box max-w-2xl mx-auto px-4 text-center"
      >
        <h2 class="cta-title">¿Listo para empezar?</h2>
        <p class="cta-sub">Únete a la comunidad de artistas que ya confían en Modo Ensayo.</p>
        <router-link to="/register" class="btn-hero-primary inline-flex">Crear cuenta gratis</router-link>
      </div>
    </section>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import heroBailarines from '@/assets/hero-bailarines.png'
import heroMusicos from '@/assets/hero-musicos.png'

function particleX(n) { return (((n * 37 + 11) % 90) + 5) + '%' }

// Crossfade
const heroImages = [heroBailarines, heroMusicos]
const activeImg = ref(0)
let timer = null
function goToImg(i) { activeImg.value = i; resetTimer() }
function resetTimer() {
  clearInterval(timer)
  timer = setInterval(() => { activeImg.value = (activeImg.value + 1) % heroImages.length }, 3500)
}
onMounted(() => resetTimer())
onUnmounted(() => clearInterval(timer))

// --- Count-up stats ---
const displayClases = ref(0)
const displayMaestros = ref(0)
const displaySedes = ref(0)
const statsVisible = ref(false)
const statsRowRef = ref(null)
let statsObserver = null

function animateCount(refVar, target, duration = 1400) {
  const start = performance.now()
  function tick(now) {
    const elapsed = now - start
    const progress = Math.min(elapsed / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)
    refVar.value = Math.round(eased * target)
    if (progress < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

onMounted(() => {
  statsObserver = new IntersectionObserver(([entry]) => {
    if (entry.isIntersecting && !statsVisible.value) {
      statsVisible.value = true
      animateCount(displayClases, 120)
      animateCount(displayMaestros, 40)
      animateCount(displaySedes, 15)
    }
  }, { threshold: 0.5 })
  if (statsRowRef.value) statsObserver.observe(statsRowRef.value)
})

onUnmounted(() => {
  if (statsObserver) statsObserver.disconnect()
})

// Disciplinas
const disciplines = ['Ballet', 'Cueca', 'Danza contemporánea', 'Folklore', 'Guitarra', 'Piano', 'Violín', 'Teatro', 'Canto']

// Roles
const roles = [
  {
    title: 'Soy Alumno',
    desc: 'Encuentra la clase perfecta para tu nivel y disciplina, en la sede más cercana a ti.',
    iconClass: 'icon-purple',
    icon: `<svg width='22' height='22' fill='none' stroke='currentColor' viewBox='0 0 24 24'><path stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253'/></svg>`,
    items: ['Busca clases por disciplina y nivel', 'Reserva y paga en línea de forma segura', 'Inscribe a tus asociados o familiares', 'Revisa tu historial de clases y pagos'],
    cta: { to: '/classes', label: 'Explorar clases', style: 'btn-hero-primary inline-flex mt-4 text-sm' }
  },
  {
    title: 'Soy Maestro',
    desc: 'Publica tus clases, gestiona tu agenda y conecta con alumnos sin complicaciones.',
    iconClass: 'icon-green',
    icon: `<svg width='22' height='22' fill='none' stroke='currentColor' viewBox='0 0 24 24'><path stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z'/></svg>`,
    items: ['Crea y publica tus propias clases', 'Agenda salas en sedes asociadas', 'Recibe pagos de forma segura', 'Visualiza tus métricas y asistencia'],
    note: '¿Tienes tu propio espacio? También puedes registrarlo como <strong>HomeStudio</strong>.',
    cta: { to: '/quiero-ser-profesor', label: 'Quiero ser Maestro', style: 'btn-hero-secondary mt-4 text-sm inline-flex' }
  },
  {
    title: 'Tengo un espacio',
    desc: 'Desde una academia con varias salas hasta tu home studio personal: pon tu espacio a trabajar.',
    iconClass: 'icon-amber',
    icon: `<svg width='22' height='22' fill='none' stroke='currentColor' viewBox='0 0 24 24'><path stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4'/></svg>`,
    items: ['Registra una sede o tu HomeStudio', 'Gestiona salas, horarios y reservas', 'Confirma clases y reagendamientos', 'Panel de métricas en tiempo real'],
    note: 'Sede comercial o espacio en tu casa: <strong>ambos son bienvenidos</strong>.',
    cta: { to: '/quiero-gestionar-sede', label: 'Registrar mi espacio', style: 'btn-hero-secondary mt-4 text-sm inline-flex' }
  }
]

// Pagos protegidos (diferenciador central: pago condicionado a que la clase ocurra)
const protections = [
  {
    title: 'Reservas con confianza',
    desc: 'Pagas al reservar, pero tu dinero queda protegido hasta que la clase se realice.',
    iconClass: 'icon-purple',
    icon: `<svg width='22' height='22' fill='none' stroke='currentColor' viewBox='0 0 24 24'><path stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z'/></svg>`,
  },
  {
    title: 'Si algo cambia, te acomodas',
    desc: 'Reagenda la clase o recupera tu pago si no se concreta. Sin letra chica.',
    iconClass: 'icon-green',
    icon: `<svg width='22' height='22' fill='none' stroke='currentColor' viewBox='0 0 24 24'><path stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15'/></svg>`,
  },
  {
    title: 'El maestro cobra al confirmar',
    desc: 'El pago se transfiere de forma automática cuando la clase se realiza.',
    iconClass: 'icon-amber',
    icon: `<svg width='22' height='22' fill='none' stroke='currentColor' viewBox='0 0 24 24'><path stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z'/></svg>`,
  },
]

// Señales de confianza
const trustBadges = [
  {
    label: 'Maestros con identidad verificada',
    icon: `<svg fill='none' stroke='currentColor' viewBox='0 0 24 24'><path stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z'/></svg>`,
  },
  {
    label: 'Reseñas reales de alumnos',
    icon: `<svg fill='none' stroke='currentColor' viewBox='0 0 24 24'><path stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.196-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z'/></svg>`,
  },
  {
    label: 'Pagos procesados con MercadoPago',
    icon: `<svg fill='none' stroke='currentColor' viewBox='0 0 24 24'><path stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z'/></svg>`,
  },
]

// Features
const features = [
  {
    title: 'Reserva salas',
    desc: 'Encuentra salas equipadas en tu ciudad y reserva en minutos.',
    iconClass: 'icon-purple',
    icon: `<svg width='20' height='20' fill='none' stroke='currentColor' viewBox='0 0 24 24'><path stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10'/></svg>`,
  },
  {
    title: 'Pagos seguros',
    desc: 'Integración con MercadoPago para transacciones seguras y confiables.',
    iconClass: 'icon-green',
    icon: `<svg width='20' height='20' fill='none' stroke='currentColor' viewBox='0 0 24 24'><path stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z'/></svg>`,
  },
  {
    title: 'Métricas en tiempo real',
    desc: 'Sedes y maestros acceden a estadísticas de ocupación, asistencia e ingresos.',
    iconClass: 'icon-amber',
    icon: `<svg width='20' height='20' fill='none' stroke='currentColor' viewBox='0 0 24 24'><path stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z'/></svg>`,
  },
]

// Cómo funciona con tabs
const activeTab = ref('alumno')
const stepTabs = [
  { key: 'alumno', label: 'Como Alumno' },
  { key: 'maestro', label: 'Como Maestro' },
  { key: 'sede', label: 'Como Sede' },
]
const allSteps = {
  alumno: [
    { title: 'Crea tu cuenta', desc: 'Regístrate gratis en menos de 2 minutos' },
    { title: 'Explora clases', desc: 'Filtra por disciplina, nivel, sede o precio' },
    { title: 'Reserva y paga', desc: 'Pago seguro con MercadoPago' },
    { title: 'Disfruta', desc: 'Asiste a tu clase y evalúa tu experiencia' },
  ],
  maestro: [
    { title: 'Regístrate', desc: 'Crea tu cuenta y verifica tu identidad' },
    { title: 'Agenda una sala', desc: 'Reserva espacio en una sede asociada' },
    { title: 'Publica tu clase', desc: 'Define horario, precio y cupo máximo' },
    { title: 'Enseña y cobra', desc: 'Recibe pagos automáticos al confirmar la clase' },
  ],
  sede: [
    { title: 'Registra tu sede', desc: 'Ingresa tus datos y salas disponibles' },
    { title: 'Recibe solicitudes', desc: 'Maestros reservan tus salas desde la plataforma' },
    { title: 'Confirma clases', desc: 'Aprueba o reagenda según disponibilidad' },
    { title: 'Monitorea métricas', desc: 'Ocupación, ingresos y asistencia en tiempo real' },
  ],
}
const currentSteps = computed(() => allSteps[activeTab.value])
</script>

<style scoped>
.home { background: var(--bg-base); }

/* HERO */
.hero { position: relative; overflow: hidden; padding: 5rem 0 3.5rem; }
.hero-glow {
  position: absolute; top: -100px; left: 50%; transform: translateX(-50%);
  width: 700px; height: 400px;
  background: radial-gradient(ellipse, #6C63FF14 0%, transparent 68%);
  pointer-events: none;
}
.page-particles { position: fixed; inset: 0; pointer-events: none; overflow: hidden; z-index: 2; }
.particle {
  position: absolute;
  bottom: -10px;
  width: 3px; height: 3px;
  background: #9B8CFF;
  border-radius: 50%;
  box-shadow: 0 0 3px 0 #6C63FF44;
  animation: float-up linear infinite;
}
@keyframes float-up {
  0%   { transform: translateY(0) scale(1); opacity: 0; }
  12%  { opacity: 0.28; }
  85%  { opacity: 0.14; }
  100% { transform: translateY(-100vh) scale(0.4); opacity: 0; }
}
.hero-inner { position: relative; z-index: 1; }
.hero-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 4rem; align-items: center; }
.hero-text { display: flex; flex-direction: column; }
.hero-visual { position: relative; }
.hero-img-wrap { position: relative; border-radius: 20px; overflow: hidden; aspect-ratio: 4/3; background: var(--bg-footer); }
.hero-img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; border-radius: 20px; opacity: 0; transition: opacity 1s ease-in-out; }
.hero-img--active { opacity: 1; }
.hero-dots { position: absolute; bottom: 14px; left: 50%; transform: translateX(-50%); display: flex; gap: 8px; z-index: 2; }
.hero-dot { width: 8px; height: 8px; border-radius: 50%; background: rgba(255,255,255,0.3); border: none; cursor: pointer; transition: background 0.3s, transform 0.3s; padding: 0; }
.hero-dot--active { background: #6C63FF; transform: scale(1.3); }
.hero-eyebrow {
  display: inline-flex; align-items: center; gap: 8px;
  background: #6C63FF12; border: 1px solid #6C63FF2e;
  color: #9B8CFF; font-size: 13px; font-weight: 500;
  padding: 5px 16px; border-radius: 100px; margin-bottom: 1.5rem; width: fit-content;
}
.eyebrow-dot { width: 6px; height: 6px; background: #6C63FF; border-radius: 50%; display: inline-block; }
.hero-title { font-size: clamp(36px, 5vw, 52px); font-weight: 700; color: var(--text-primary); line-height: 1.12; margin-bottom: 1.25rem; }
.hero-title em { font-style: normal; background: linear-gradient(130deg, #6C63FF, #A89CFF 60%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; }
.hero-sub { color: var(--text-secondary); font-size: 16px; line-height: 1.75; margin-bottom: 2rem; }
.hero-ctas { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.btn-hero-primary { background: #6C63FF; color: white; font-weight: 600; font-size: 14px; padding: 12px 22px; border-radius: 12px; display: inline-flex; align-items: center; gap: 8px; transition: background 0.2s, transform 0.15s; }
.btn-hero-primary:hover { background: #5a52d5; transform: translateY(-1px); }
.btn-hero-secondary { color: var(--text-secondary); font-size: 14px; padding: 12px 18px; border-radius: 12px; border: 1px solid var(--border-default); transition: color 0.2s, border-color 0.2s; }
.btn-hero-secondary:hover { color: var(--text-primary); border-color: var(--border-default); }
.stats-row { display: flex; align-items: center; gap: 2rem; flex-wrap: wrap; margin-top: 2.5rem; padding-top: 2rem; border-top: 1px solid var(--border-subtle); }
.stat-item { display: flex; flex-direction: column; }
.stat-num {
  font-size: 24px; font-weight: 700; color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.stat-label { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }
.stat-divider { width: 1px; height: 28px; background: var(--border-subtle); }
.hero-roles-hint { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-top: 1.25rem; font-size: 13px; color: var(--text-secondary); }
.hero-roles-hint a { color: #8B83FF; font-weight: 600; transition: color 0.2s; }
.hero-roles-hint a:hover { color: #6C63FF; text-decoration: underline; }
.hero-roles-hint .hint-dot { color: var(--border-default); }

/* ABOUT */
.about-section { background: var(--bg-footer); padding: 4rem 0; border-top: 1px solid var(--border-subtle); }
.about-inner { max-width: 760px; }
.about-title { font-size: clamp(22px, 3vw, 30px); font-weight: 700; color: var(--text-primary); margin-bottom: 1rem; margin-top: 0.5rem; }
.about-desc { color: var(--text-secondary); font-size: 15px; line-height: 1.8; margin-bottom: 1.5rem; }
.disciplines { display: flex; flex-wrap: wrap; gap: 8px; }
.discipline-pill { background: #6C63FF12; border: 1px solid #6C63FF28; color: #9B8CFF; font-size: 12px; font-weight: 500; padding: 4px 14px; border-radius: 100px; }

/* PAGOS PROTEGIDOS */
.protect-section { position: relative; overflow: hidden; background: var(--bg-base); padding: 4.5rem 0; border-top: 1px solid var(--border-subtle); }
.protect-glow {
  position: absolute; top: -80px; left: 50%; transform: translateX(-50%);
  width: 600px; height: 300px;
  background: radial-gradient(ellipse, #6C63FF1f 0%, transparent 70%);
  pointer-events: none;
}
.protect-head { position: relative; text-align: center; max-width: 640px; margin: 0 auto 2.5rem; }
.protect-title { font-size: clamp(22px, 3vw, 32px); font-weight: 700; color: var(--text-primary); line-height: 1.2; margin: 0.25rem 0 0.75rem; }
.protect-title em { font-style: normal; color: #8B83FF; }
.protect-sub { color: var(--text-secondary); font-size: 15px; line-height: 1.7; }
.protect-grid { position: relative; display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 14px; margin-bottom: 2.5rem; }
.protect-card { background: var(--bg-surface); border: 1px solid var(--border-subtle); border-radius: 16px; padding: 1.75rem; text-align: center; transition: border-color 0.2s, transform 0.2s; }
.protect-card:hover { border-color: #6C63FF44; transform: translateY(-2px); }
.protect-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; margin: 0 auto 1rem; }
.protect-card-title { color: var(--text-primary); font-size: 16px; font-weight: 700; margin-bottom: 6px; }
.protect-card-desc { color: var(--text-secondary); font-size: 13px; line-height: 1.65; }
.trust-bar { position: relative; display: flex; flex-wrap: wrap; justify-content: center; gap: 12px; }
.trust-badge { display: inline-flex; align-items: center; gap: 8px; background: #6C63FF0f; border: 1px solid #6C63FF26; color: var(--text-secondary); font-size: 13px; font-weight: 500; padding: 8px 16px; border-radius: 100px; }
.trust-badge-icon { display: inline-flex; color: #8B83FF; }
.trust-badge-icon svg { width: 16px; height: 16px; }

/* ROLES */
.roles-section { background: var(--bg-base); padding: 4rem 0; border-top: 1px solid var(--border-subtle); }
.roles-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 14px; }
.role-card { background: var(--bg-surface); border: 1px solid var(--border-subtle); border-radius: 16px; padding: 1.75rem; position: relative; overflow: hidden; transition: border-color 0.2s, transform 0.2s; display: flex; flex-direction: column; }
.role-card::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 1px; background: linear-gradient(90deg, transparent, #6C63FF44, transparent); }
.role-card:hover { border-color: #6C63FF44; transform: translateY(-2px); }
.role-icon { width: 46px; height: 46px; border-radius: 12px; display: flex; align-items: center; justify-content: center; margin-bottom: 1rem; }
.icon-purple { background: #6C63FF1a; color: #8B83FF; }
.icon-green  { background: #10b9811a; color: #34d399; }
.icon-amber  { background: #f59e0b1a; color: #fbbf24; }
.role-title { color: var(--text-primary); font-size: 17px; font-weight: 700; margin-bottom: 6px; }
.role-desc { color: var(--text-secondary); font-size: 13px; line-height: 1.65; margin-bottom: 1rem; }
.role-list { list-style: none; padding: 0; margin: 0 0 auto 0; display: flex; flex-direction: column; gap: 6px; }
.role-item { display: flex; align-items: flex-start; gap: 8px; color: var(--text-secondary); font-size: 13px; }
.role-item svg { color: #6C63FF; margin-top: 2px; }
.role-note { color: var(--text-secondary); font-size: 12px; line-height: 1.5; margin: 0; padding-top: 0.75rem; }
.role-note :deep(strong) { color: #8B83FF; font-weight: 600; }
.role-cta { display: inline-flex; align-items: center; justify-content: center; padding: 10px 20px; border-radius: 10px; font-size: 13px; font-weight: 600; transition: all 0.2s; }

/* FEATURES */
.features-section { background: var(--bg-footer); padding: 3rem 0; border-top: 1px solid var(--border-subtle); }
.section-eyebrow { font-size: 12px; font-weight: 500; color: var(--text-secondary); letter-spacing: 1px; text-transform: uppercase; margin-bottom: 1.5rem; }
.features-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; }
.feat-card { background: var(--bg-surface); border: 1px solid var(--border-subtle); border-radius: 14px; padding: 1.5rem; position: relative; overflow: hidden; transition: border-color 0.2s, transform 0.2s; }
.feat-card::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 1px; background: linear-gradient(90deg, transparent, #6C63FF44, transparent); }
.feat-card:hover { border-color: #6C63FF44; transform: translateY(-2px); }
.feat-icon { width: 42px; height: 42px; border-radius: 10px; display: flex; align-items: center; justify-content: center; margin-bottom: 1rem; }
.feat-title { color: var(--text-primary); font-size: 15px; font-weight: 600; margin-bottom: 6px; }
.feat-desc  { color: var(--text-secondary); font-size: 13px; line-height: 1.65; }

/* STEPS */
.steps-section { background: var(--bg-base); padding: 3rem 0; border-top: 1px solid var(--border-subtle); }
.steps-tabs { display: flex; gap: 4px; margin-bottom: 2rem; background: var(--bg-surface); border: 1px solid var(--border-subtle); border-radius: 12px; padding: 4px; width: fit-content; }
.steps-tab { padding: 7px 18px; border-radius: 9px; font-size: 13px; font-weight: 500; color: var(--text-secondary); border: none; background: transparent; cursor: pointer; transition: all 0.2s; }
.steps-tab--active { background: #6C63FF; color: white; }
.steps-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 2rem; position: relative; }
.steps-grid::before { content: ''; position: absolute; top: 20px; left: calc(12.5% + 8px); right: calc(12.5% + 8px); height: 1px; background: linear-gradient(90deg, transparent, #6C63FF38, #6C63FF38, transparent); }
.step-item { text-align: center; }
.step-num { width: 40px; height: 40px; background: var(--bg-surface); border: 1px solid var(--border-default); border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 14px; font-size: 13px; font-weight: 600; color: #6C63FF; position: relative; z-index: 1; transition: background 0.2s, border-color 0.2s; }
.step-item:hover .step-num { background: #6C63FF1a; border-color: #6C63FF44; }
.step-title { color: var(--text-primary); font-size: 14px; font-weight: 500; margin-bottom: 4px; }
.step-desc  { color: var(--text-secondary); font-size: 12px; }

/* CTA FINAL */
.cta-section { background: var(--bg-footer); border-top: 1px solid var(--border-subtle); padding: 4rem 0; }
.cta-title { color: var(--text-primary); font-size: 28px; font-weight: 700; margin-bottom: 10px; }
.cta-sub   { color: var(--text-secondary); font-size: 15px; margin-bottom: 1.75rem; }

@media (max-width: 768px) {
  .hero-grid { grid-template-columns: 1fr; gap: 2.5rem; }
  .hero-visual { order: -1; }
}
</style>
