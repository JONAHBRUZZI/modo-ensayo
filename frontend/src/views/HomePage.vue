<template>
  <div class="home">

    <!-- HERO -->
    <section class="hero">
      <div class="hero-glow" aria-hidden="true"></div>
      <div class="hero-inner max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

        <div
          v-motion
          :initial="{ opacity: 0, y: 20 }"
          :enter="{ opacity: 1, y: 0, transition: { duration: 500 } }"
          class="hero-eyebrow"
        >
          <span class="eyebrow-dot" aria-hidden="true"></span>
          Plataforma de danza y música en Chile
        </div>

        <h1
          v-motion
          :initial="{ opacity: 0, y: 24 }"
          :enter="{ opacity: 1, y: 0, transition: { duration: 550, delay: 80 } }"
          class="hero-title"
        >
          Tu <em>arte</em> encuentra<br>su espacio aquí
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
            Crear cuenta →
          </router-link>
        </div>

        <div
          v-motion
          :initial="{ opacity: 0 }"
          :enter="{ opacity: 1, transition: { duration: 600, delay: 400 } }"
          class="stats-row"
        >
          <div class="stat-item">
            <span class="stat-num">+120</span>
            <span class="stat-label">Clases activas</span>
          </div>
          <div class="stat-divider" aria-hidden="true"></div>
          <div class="stat-item">
            <span class="stat-num">+40</span>
            <span class="stat-label">Maestros</span>
          </div>
          <div class="stat-divider" aria-hidden="true"></div>
          <div class="stat-item">
            <span class="stat-num">+15</span>
            <span class="stat-label">Sedes</span>
          </div>
        </div>
      </div>
    </section>

    <!-- FEATURES -->
    <section class="features-section">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <p class="section-eyebrow">¿Qué puedes hacer?</p>
        <div class="features-grid">
          <div
            v-for="(feat, i) in features"
            :key="feat.title"
            v-motion
            :initial="{ opacity: 0, y: 28 }"
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

    <!-- HOW IT WORKS -->
    <section class="steps-section">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <p class="section-eyebrow">¿Cómo funciona?</p>
        <div class="steps-grid">
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
        <h2 class="cta-title">¿Listo para empezar?</h2>
        <p class="cta-sub">Únete a la comunidad de artistas que ya confían en Modo Ensayo.</p>
        <router-link to="/register" class="btn-hero-primary inline-flex">
          Crear cuenta gratis
        </router-link>
      </div>
    </section>

  </div>
</template>

<script setup>
const features = [
  {
    title: 'Reserva salas',
    desc: 'Encuentra salas equipadas en tu ciudad y reserva en minutos con los mejores equipos.',
    iconClass: 'icon-purple',
    icon: `<svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/></svg>`,
  },
  {
    title: 'Toma clases',
    desc: 'Accede a clases de danza y música con maestros calificados en diversas disciplinas.',
    iconClass: 'icon-green',
    icon: `<svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/></svg>`,
  },
  {
    title: 'Gestióna tu sede',
    desc: 'Administra salas, profesores y clases desde un solo lugar con métricas en tiempo real.',
    iconClass: 'icon-amber',
    icon: `<svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg>`,
  },
]

const steps = [
  { title: 'Regístrate', desc: 'Crea tu cuenta gratis en segundos' },
  { title: 'Explora', desc: 'Busca clases y salas disponibles' },
  { title: 'Reserva', desc: 'Paga de forma segura y asegura tu cupo' },
  { title: 'Disfruta', desc: 'Ensayar nunca fue tan fácil' },
]
</script>

<style scoped>
.home { background: #0f1119; }

/* HERO */
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

.hero-eyebrow {
  display: inline-flex; align-items: center; gap: 8px;
  background: #6C63FF12; border: 1px solid #6C63FF2e;
  color: #9B8CFF; font-size: 13px; font-weight: 500;
  padding: 5px 16px; border-radius: 100px;
  margin-bottom: 1.5rem; letter-spacing: 0.2px;
}
.eyebrow-dot {
  width: 6px; height: 6px;
  background: #6C63FF; border-radius: 50%;
  display: inline-block;
}

.hero-title {
  font-size: clamp(36px, 5vw, 52px);
  font-weight: 700; color: white; line-height: 1.12;
  margin-bottom: 1.25rem; max-width: 540px;
}
.hero-title em {
  font-style: normal;
  background: linear-gradient(130deg, #6C63FF, #A89CFF 60%);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-sub {
  color: #6B7280; font-size: 16px; line-height: 1.75;
  max-width: 440px; margin-bottom: 2rem;
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
.stat-num { font-size: 24px; font-weight: 700; color: white; }
.stat-label { font-size: 12px; color: #4B5563; margin-top: 2px; }
.stat-divider { width: 1px; height: 28px; background: #1e2130; }

/* FEATURES */
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
}
.icon-purple { background: #6C63FF1a; color: #8B83FF; }
.icon-green  { background: #10b9811a; color: #34d399; }
.icon-amber  { background: #f59e0b1a; color: #fbbf24; }

.feat-title { color: white; font-size: 15px; font-weight: 600; margin-bottom: 6px; }
.feat-desc  { color: #4B5563; font-size: 13px; line-height: 1.65; }

/* HOW IT WORKS */
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
.steps-grid::before {
  content: '';
  position: absolute; top: 20px;
  left: calc(12.5% + 8px); right: calc(12.5% + 8px);
  height: 1px;
  background: linear-gradient(90deg, transparent, #6C63FF38, #6C63FF38, transparent);
}
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

/* CTA FINAL */
.cta-section {
  background: #0b0d14;
  border-top: 1px solid #13161f;
  padding: 4rem 0;
}
.cta-box { }
.cta-title { color: white; font-size: 28px; font-weight: 700; margin-bottom: 10px; }
.cta-sub   { color: #6B7280; font-size: 15px; margin-bottom: 1.75rem; }
</style>
