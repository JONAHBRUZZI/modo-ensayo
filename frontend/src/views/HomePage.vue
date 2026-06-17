<template>
  <div class="home">

    <!-- HERO -->
    <section class="hero">
      <div class="hero-glow" aria-hidden="true"></div>

      <!-- Particle layer -->
      <div class="hero-particles" aria-hidden="true">
        <span v-for="n in 12" :key="n" class="particle" :style="{ left: particleX(n), animationDelay: (n * 0.4) + 's', animationDuration: (4 + (n % 3)) + 's' }" />
      </div>

      <div class="hero-inner max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

        <div class="hero-grid">

          <!-- Columna izquierda: texto -->
          <div class="hero-text">
            <div
              v-motion
              :initial="{ opacity: 0, y: 20 }"
              :enter="{ opacity: 1, y: 0, transition: { duration: 500 } }"
              class="hero-eyebrow"
            >
              <span class="eyebrow-dot" aria-hidden="true"></span>
              Plataforma de danza y musica en Chile
            </div>

            <h1
              v-motion
              :initial="{ opacity: 0, y: 24 }"
              :enter="{ opacity: 1, y: 0, transition: { duration: 550, delay: 80 } }"
              class="hero-title"
            >
              Tu <em>arte</em> encuentra<br>su espacio aqui
            </h1>

            <p
              v-motion
              :initial="{ opacity: 0, y: 20 }"
              :enter="{ opacity: 1, y: 0, transition: { duration: 500, delay: 160 } }"
              class="hero-sub"
            >
              Conectamos alumnos, maestros y salas de ensayo en un solo lugar.
              Reserva, aprende y crece.
            </p>

            <div
              v-motion
              :initial="{ opacity: 0, y: 16 }"
              :enter="{ opacity: 1, y: 0, transition: { duration: 450, delay: 240 } }"
              class="hero-ctas"
            >
              <router-link to="/classes" class="btn-hero-primary">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
                Explorar clases
              </router-link>
              <router-link to="/register" class="btn-hero-secondary">
                Crear cuenta &rarr;
              </router-link>
            </div>

            <div ref="statsRowRef" class="stats-row" :class="{ 'stats-row--visible': statsVisible }">
              <div class="stat-item">
                <span class="stat-num">+{{ displayClases }}</span>
                <span class="stat-label">Clases activas</span>
              </div>
              <div class="stat-divider" aria-hidden="true"></div>
              <div class="stat-item">
                <span class="stat-num">+{{ displayMaestros }}</span>
                <span class="stat-label">Maestros</span>
              </div>
              <div class="stat-divider" aria-hidden="true"></div>
              <div class="stat-item">
                <span class="stat-num">+{{ displaySedes }}</span>
                <span class="stat-label">Sedes</span>
              </div>
            </div>
          </div>

          <!-- Columna derecha: crossfade imagenes -->
          <div
            v-motion
            :initial="{ opacity: 0, scale: 0.96 }"
            :enter="{ opacity: 1, scale: 1, transition: { duration: 700, delay: 300 } }"
            class="hero-visual"
            aria-hidden="true"
          >
            <div class="hero-img-wrap">
              <img
                v-for="(img, i) in heroImages"
                :key="i"
                :src="img"
                :class="['hero-img', { 'hero-img--active': i === activeImg }]"
                alt=""
              />
              <div class="hero-dots">
                <button
                  v-for="(_, i) in heroImages"
                  :key="i"
                  :class="['hero-dot', { 'hero-dot--active': i === activeImg }]"
                  @click="goToImg(i)"
                  :aria-label="`Imagen ${i + 1}`"
                />
              </div>
            </div>
          </div>

        </div>
      </div>
    </section>

    <!-- FEATURES -->
    <section class="features-section">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <p class="section-eyebrow">&iquest;Que puedes hacer?</p>
        <div class="features-grid">
          <router-link
            v-for="(feat, i) in features"
            :key="feat.title"
            :to="feat.to"
            v-motion
            :initial="{ opacity: 0, y: 28 }"
            :visible="{ opacity: 1, y: 0, transition: { duration: 450, delay: i * 80 } }"
            class="feat-card"
          >
            <div :class="['feat-icon', feat.iconClass]" aria-hidden="true" v-html="feat.icon"></div>
            <h3 class="feat-title">{{ feat.title }}</h3>
            <p class="feat-desc">{{ feat.desc }}</p>
          </router-link>
        </div>
      </div>
    </section>

    <!-- HOW IT WORKS -->
    <section class="steps-section">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <p class="section-eyebrow">&iquest;Como funciona?</p>
        <div ref="stepsGridRef" class="steps-grid" :class="{ 'steps-grid--visible': stepsLineVisible }">
          <div class="steps-line-track" aria-hidden="true">
            <div class="steps-line-fill" />
          </div>
          <div
            v-for="(step, i) in steps"
            :key="step.title"
            v-motion
            :initial="{ opacity: 0, y: 20 }"
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
        v-motion
        :initial="{ opacity: 0, scale: 0.97 }"
        :visible="{ opacity: 1, scale: 1, transition: { duration: 500 } }"
        class="cta-box max-w-2xl mx-auto px-4 text-center"
      >
        <h2 class="cta-title">&iquest;Listo para empezar?</h2>
        <p class="cta-sub">Unete a la comunidad de artistas que ya confian en Modo Ensayo.</p>
        <router-link to="/register" class="btn-hero-primary inline-flex cta-pulse">
          Crear cuenta gratis
        </router-link>
      </div>
    </section>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useAuth } from '@/stores/auth'
import heroBailarines from '@/assets/hero-bailarines.png'
import heroMusicos from '@/assets/hero-musicos.png'

const { isAuthenticated } = useAuth()

const heroImages = [heroBailarines, heroMusicos]
const activeImg = ref(0)
let timer = null

function goToImg(i) {
  activeImg.value = i
  resetTimer()
}

function resetTimer() {
  clearInterval(timer)
  timer = setInterval(() => {
    activeImg.value = (activeImg.value + 1) % heroImages.length
  }, 3500)
}

onMounted(() => resetTimer())
onUnmounted(() => clearInterval(timer))

// Particle positions (pseudo-random but deterministic)
function particleX(n) {
  const seeds = [5, 12, 18, 25, 32, 40, 48, 55, 62, 70, 78, 88]
  return seeds[n - 1] + '%'
}

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

// --- Steps connecting line animation ---
const stepsLineVisible = ref(false)
const stepsGridRef = ref(null)
let stepsObserver = null

onMounted(() => {
  stepsObserver = new IntersectionObserver(([entry]) => {
    if (entry.isIntersecting && !stepsLineVisible.value) {
      stepsLineVisible.value = true
    }
  }, { threshold: 0.3 })
  if (stepsGridRef.value) stepsObserver.observe(stepsGridRef.value)
})

onUnmounted(() => {
  if (stepsObserver) stepsObserver.disconnect()
})

const features = computed(() => [
  {
    title: 'Reserva salas',
    desc: 'Encuentra salas equipadas en tu ciudad y reserva en minutos con los mejores equipos.',
    iconClass: 'icon-purple',
    icon: `<svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/></svg>`,
    to: isAuthenticated.value ? '/classes' : '/login',
  },
  {
    title: 'Toma clases',
    desc: 'Accede a clases de danza y musica con maestros calificados en diversas disciplinas.',
    iconClass: 'icon-green',
    icon: `<svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/></svg>`,
    to: '/classes',
  },
  {
    title: 'Gestiona tu sede',
    desc: 'Administra salas, profesores y clases desde un solo lugar con metricas en tiempo real.',
    iconClass: 'icon-amber',
    icon: `<svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg>`,
    to: '/quiero-gestionar-sede',
  },
])

const steps = [
  { title: 'Registrate', desc: 'Crea tu cuenta gratis en segundos' },
  { title: 'Explora', desc: 'Busca clases y salas disponibles' },
  { title: 'Reserva', desc: 'Paga de forma segura y asegura tu cupo' },
  { title: 'Disfruta', desc: 'Ensayar nunca fue tan facil' },
]
</script>

<style scoped>
.home { background: #0f1119; }

/* =============== HERO =============== */
.hero {
  position: relative;
  overflow: hidden;
  padding: 5rem 0 3.5rem;
}
.hero-glow {
  position: absolute;
  top: -100px; left: 50%; transform: translateX(-50%);
  width: 700px; height: 400px;
  background: radial-gradient(ellipse, #6C63FF14 0%, transparent 68%);
  pointer-events: none;
}
.hero-inner { position: relative; z-index: 1; }

/* Particles */
.hero-particles { position: absolute; inset: 0; pointer-events: none; overflow: hidden; }
.particle {
  position: absolute;
  bottom: -10px;
  width: 4px; height: 4px;
  background: #6C63FF33;
  border-radius: 50%;
  animation: float-up linear infinite;
}
@keyframes float-up {
  0%   { transform: translateY(0) scale(1); opacity: 0; }
  10%  { opacity: 0.7; }
  90%  { opacity: 0.3; }
  100% { transform: translateY(-500px) scale(0.4); opacity: 0; }
}

.hero-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4rem;
  align-items: center;
}
@media (max-width: 768px) {
  .hero-grid { grid-template-columns: 1fr; gap: 2.5rem; }
  .hero-visual { order: -1; }
}

.hero-text { display: flex; flex-direction: column; }

.hero-visual { position: relative; }
.hero-img-wrap {
  position: relative;
  border-radius: 20px;
  overflow: hidden;
  aspect-ratio: 4/3;
  background: #0b0d14;
}
.hero-img {
  position: absolute;
  inset: 0;
  width: 100%; height: 100%;
  object-fit: cover;
  border-radius: 20px;
  opacity: 0;
  transition: opacity 1s ease-in-out;
}
.hero-img--active { opacity: 1; }

.hero-dots {
  position: absolute;
  bottom: 14px; left: 50%; transform: translateX(-50%);
  display: flex; gap: 8px; z-index: 2;
}
.hero-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: rgba(255,255,255,0.3);
  border: none; cursor: pointer;
  transition: background 0.3s, transform 0.3s;
  padding: 0;
}
.hero-dot--active {
  background: #6C63FF;
  transform: scale(1.3);
}

.hero-eyebrow {
  display: inline-flex; align-items: center; gap: 8px;
  background: #6C63FF12; border: 1px solid #6C63FF2e;
  color: #9B8CFF; font-size: 13px; font-weight: 500;
  padding: 5px 16px; border-radius: 100px;
  margin-bottom: 1.5rem; letter-spacing: 0.2px;
  width: fit-content;
}
.eyebrow-dot {
  width: 6px; height: 6px;
  background: #6C63FF; border-radius: 50%;
  display: inline-block;
}

.hero-title {
  font-size: clamp(36px, 5vw, 52px);
  font-weight: 700; color: white; line-height: 1.12;
  margin-bottom: 1.25rem;
}
.hero-title em {
  font-style: normal;
  background: linear-gradient(130deg, #6C63FF, #A89CFF 60%);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-sub {
  color: #6B7280; font-size: 16px; line-height: 1.75;
  margin-bottom: 2rem;
}

.hero-ctas { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }

.btn-hero-primary {
  background: #6C63FF; color: white; font-weight: 600; font-size: 14px;
  padding: 12px 22px; border-radius: 12px;
  display: inline-flex; align-items: center; gap: 8px;
  transition: background 0.2s, transform 0.15s;
}
.btn-hero-primary:hover { background: #5a52d5; transform: translateY(-1px); }

.btn-hero-secondary {
  color: #9CA3AF; font-size: 14px; padding: 12px 18px;
  border-radius: 12px; border: 1px solid #2a2d3e;
  transition: color 0.2s, border-color 0.2s;
}
.btn-hero-secondary:hover { color: white; border-color: #3a3d4e; }

.stats-row {
  display: flex; align-items: center; gap: 2rem; flex-wrap: wrap;
  margin-top: 2.5rem; padding-top: 2rem;
  border-top: 1px solid #1e2130;
}
.stat-item { display: flex; flex-direction: column; }
.stat-num {
  font-size: 24px; font-weight: 700; color: white;
  font-variant-numeric: tabular-nums;
}
.stat-label { font-size: 12px; color: #4B5563; margin-top: 2px; }
.stat-divider { width: 1px; height: 28px; background: #1e2130; }

/* =============== FEATURES =============== */
.features-section {
  background: #0b0d14;
  padding: 3rem 0;
  border-top: 1px solid #13161f;
}
.section-eyebrow {
  font-size: 12px; font-weight: 500; color: #4B5563;
  letter-spacing: 1px; text-transform: uppercase;
  margin-bottom: 1.5rem;
}
.features-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; }

.feat-card {
  background: #111420;
  border: 1px solid #1e2130;
  border-radius: 14px;
  padding: 1.5rem;
  position: relative; overflow: hidden;
  transition: border-color 0.2s, transform 0.2s;
  text-decoration: none;
}
.feat-card::before {
  content: '';
  position: absolute; top: 0; left: 0; right: 0; height: 1px;
  background: linear-gradient(90deg, transparent, #6C63FF44, transparent);
}
.feat-card:hover { border-color: #6C63FF44; transform: translateY(-2px); }

.feat-icon {
  width: 42px; height: 42px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 1rem;
  transition: transform 0.3s ease;
}
.feat-card:hover .feat-icon { transform: scale(1.1); }
.icon-purple { background: #6C63FF1a; color: #8B83FF; }
.icon-green  { background: #10b9811a; color: #34d399; }
.icon-amber  { background: #f59e0b1a; color: #fbbf24; }

.feat-title { color: white; font-size: 15px; font-weight: 600; margin-bottom: 6px; }
.feat-desc  { color: #4B5563; font-size: 13px; line-height: 1.65; }

/* =============== HOW IT WORKS =============== */
.steps-section {
  background: #0f1119;
  padding: 3rem 0;
  border-top: 1px solid #13161f;
}
.steps-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 2rem;
  position: relative;
}

/* Animated connecting line */
.steps-line-track {
  position: absolute; top: 20px;
  left: calc(12.5%); right: calc(12.5%);
  height: 1px; overflow: hidden;
  pointer-events: none;
}
.steps-line-fill {
  width: 0;
  height: 100%;
  background: linear-gradient(90deg, transparent, #6C63FF66, #6C63FF66, transparent);
  transition: width 1.2s cubic-bezier(0.22, 1, 0.36, 1);
}
.steps-grid--visible .steps-line-fill { width: 100%; }

.step-item { text-align: center; }
.step-num {
  width: 40px; height: 40px;
  background: #111420; border: 1px solid #2a2d3e;
  border-radius: 50%; display: flex; align-items: center; justify-content: center;
  margin: 0 auto 14px;
  font-size: 13px; font-weight: 600; color: #6C63FF;
  position: relative; z-index: 1;
  transition: background 0.2s, border-color 0.2s;
}
.step-item:hover .step-num { background: #6C63FF1a; border-color: #6C63FF44; }
.step-title { color: white; font-size: 14px; font-weight: 500; margin-bottom: 4px; }
.step-desc  { color: #4B5563; font-size: 12px; }

/* =============== CTA FINAL =============== */
.cta-section {
  background: #0b0d14;
  border-top: 1px solid #13161f;
  padding: 4rem 0;
}
.cta-title { color: white; font-size: 28px; font-weight: 700; margin-bottom: 10px; }
.cta-sub   { color: #6B7280; font-size: 15px; margin-bottom: 1.75rem; }

/* Pulsing glow on CTA button */
.cta-pulse {
  animation: cta-glow 2.5s ease-in-out infinite;
}
@keyframes cta-glow {
  0%, 100% { box-shadow: 0 0 0 0 #6C63FF44; }
  50%      { box-shadow: 0 0 18px 4px #6C63FF33; }
}
</style>
