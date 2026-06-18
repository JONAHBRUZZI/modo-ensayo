<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-2">Reseñas</h1>
    <p class="text-gray-400 text-sm mb-6">{{ subtituloContexto }}</p>

    <!-- Pestañas -->
    <div class="flex flex-wrap gap-2 mb-8 border-b border-white/10 pb-3">
      <button v-for="t in tabsVisibles" :key="t.value"
              @click="tab = t.value"
              :class="[
                'px-4 py-2 rounded-lg text-sm transition-colors',
                tab === t.value ? 'bg-primary text-white' : 'bg-[#1a1d2e] text-gray-400 hover:text-white border border-white/10'
              ]">
        {{ t.label }}
      </button>
    </div>

    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>

    <template v-else>
      <!-- ══════════ CLASES (alumno) ══════════ -->
      <div v-show="tab === 'CLASS'" class="space-y-10">
        <section>
          <h2 class="text-lg font-semibold text-white mb-1">Pendientes de evaluar</h2>
          <p class="text-gray-400 text-sm mb-4">Clases completadas que aún no has evaluado.</p>
          <div v-if="elegibles.length === 0" class="card text-center py-8">
            <p class="text-gray-400 text-sm">No tienes clases pendientes de reseña.</p>
            <router-link to="/classes" class="btn-primary mt-4 inline-block">Buscar Clases</router-link>
          </div>
          <div v-else class="space-y-4">
            <div v-for="item in elegibles" :key="item.classId" class="card space-y-4">
              <div class="flex items-start justify-between">
                <div>
                  <h3 class="text-white font-semibold">{{ item.classTitle }}</h3>
                  <p class="text-gray-500 text-xs mt-1">{{ formatDate(item.classDate) }}</p>
                </div>
                <span class="text-xs text-yellow-400 bg-yellow-400/10 px-2 py-1 rounded-full flex-shrink-0">Sin reseña</span>
              </div>
              <Valorador v-if="formActivo === item.classId"
                         :enviando="enviando[item.classId]" :error="errores[item.classId]"
                         @enviar="(s, c) => enviarClase(item, s, c)" @cancelar="formActivo = null" />
              <button v-else @click="formActivo = item.classId" class="btn-secondary text-sm w-full">Dejar reseña</button>
            </div>
          </div>
        </section>

        <ListaReseñas titulo="Mis reseñas" subtitulo="Las opiniones que has dejado en clases."
                      :items="misPorTipo('CLASS')" vacio="Aún no has reseñado ninguna clase." :mostrarAutor="false" />
        <ListaReseñas titulo="Recomendaciones de la comunidad" subtitulo="Opiniones de otros alumnos sobre distintas clases."
                      :items="comunidadPorTipo('CLASS')" vacio="Todavía no hay reseñas de otros alumnos." :mostrarAutor="true" />
      </div>

      <!-- ══════════ MAESTROS ══════════ -->
      <div v-show="tab === 'TEACHER'" class="space-y-10">
        <SeccionValorar tipo="TEACHER" titulo="Maestros que puedes valorar" :targets="elegiblesPorTipo('TEACHER')"
                        :estado="valorando" @enviar="valorarTarget" />
        <ListaReseñas titulo="Mis reseñas a maestros" subtitulo="Lo que has opinado sobre profesores."
                      :items="misPorTipo('TEACHER')" vacio="Aún no has reseñado a ningún maestro." :mostrarAutor="false" />
        <ListaReseñas titulo="Reputación de la comunidad" subtitulo="Opiniones de otros sobre los maestros."
                      :items="comunidadPorTipo('TEACHER')" vacio="Todavía no hay reseñas sobre maestros." :mostrarAutor="true" />
      </div>

      <!-- ══════════ SEDES ══════════ -->
      <div v-show="tab === 'VENUE'" class="space-y-10">
        <SeccionValorar tipo="VENUE" titulo="Sedes que puedes valorar" :targets="elegiblesPorTipo('VENUE')"
                        :estado="valorando" @enviar="valorarTarget" />
        <ListaReseñas titulo="Mis reseñas a sedes" subtitulo="Lo que has opinado sobre sedes y salas."
                      :items="misPorTipo('VENUE')" vacio="Aún no has reseñado ninguna sede." :mostrarAutor="false" />
        <ListaReseñas titulo="Reputación de la comunidad" subtitulo="Opiniones de otros sobre las sedes."
                      :items="comunidadPorTipo('VENUE')" vacio="Todavía no hay reseñas sobre sedes." :mostrarAutor="true" />
      </div>

      <!-- ══════════ MIS ALUMNOS (maestro / sede) ══════════ -->
      <div v-show="tab === 'STUDENT'" class="space-y-10">
        <SeccionValorar tipo="STUDENT" titulo="Alumnos que puedes valorar" :targets="elegiblesPorTipo('STUDENT')"
                        :estado="valorando" @enviar="valorarTarget" />
        <ListaReseñas titulo="Mis reseñas a alumnos" subtitulo="Valoraciones que has dejado sobre tus alumnos. Solo tú las ves."
                      :items="misPorTipo('STUDENT')" vacio="Aún no has reseñado a ningún alumno." :mostrarAutor="false" />
      </div>

      <!-- ══════════ MI COMPORTAMIENTO / MI REPUTACIÓN ══════════ -->
      <div v-show="tab === 'ABOUT_ME'" class="space-y-10">
        <ListaReseñas :titulo="tituloSobreMi" :subtitulo="subtituloSobreMi"
                      :items="sobreMiFiltrado" :vacio="vacioSobreMi" :mostrarAutor="true" />
      </div>

      <!-- ══════════ SISTEMA (todos) ══════════ -->
      <div v-show="tab === 'SYSTEM'" class="space-y-6">
        <section>
          <h2 class="text-lg font-semibold text-white mb-1">Valora Modo Ensayo</h2>
          <p class="text-gray-400 text-sm mb-4">Tu opinión sobre la plataforma. La revisa el equipo administrador.</p>

          <div v-if="miSistema" class="card">
            <div class="flex items-center justify-between">
              <h3 class="text-white font-medium">Tu valoración</h3>
              <span class="text-yellow-400">{{ estrellas(miSistema.score) }}</span>
            </div>
            <p v-if="miSistema.comment" class="text-gray-400 text-sm mt-2">{{ miSistema.comment }}</p>
            <p class="text-gray-600 text-xs mt-2">¡Gracias por tu opinión!</p>
          </div>

          <div v-else class="card">
            <Valorador :enviando="enviandoSistema" :error="errorSistema"
                       placeholder="¿Qué te parece Modo Ensayo? ¿Qué mejorarías?"
                       textoEnviar="Enviar valoración" :mostrarCancelar="false"
                       @enviar="valorarSistema" />
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, h } from 'vue'
import { reviewService } from '@/services/reviewService'
import { useAuth } from '@/stores/auth'
import { formatDate } from '@/utils/dateFormatter'

const { modoActual } = useAuth()

const tab = ref('CLASS')
const elegibles = ref([])
const elegiblesTargets = ref([])
const mias = ref([])
const comunidad = ref([])
const sobreMi = ref([])
const miSistema = ref(null)
const loading = ref(true)
const formActivo = ref(null)
const enviando = reactive({})
const errores = reactive({})
const valorando = reactive({})
const enviandoSistema = ref(false)
const errorSistema = ref('')

const contexto = computed(() => modoActual.value || 'alumno')

const subtituloContexto = computed(() => {
  if (contexto.value === 'profesor') return 'Tu reputación como maestro y las valoraciones que tú das.'
  if (contexto.value === 'sede') return 'La reputación de tu sede y las valoraciones que tú das.'
  return 'Opiniones y valoraciones según lo que te corresponde ver y evaluar.'
})

const tabsVisibles = computed(() => {
  if (contexto.value === 'profesor') {
    return [
      { value: 'ABOUT_ME', label: 'Mi Reputación' },
      { value: 'VENUE', label: 'Sedes' },
      { value: 'STUDENT', label: 'Mis Alumnos' },
      { value: 'SYSTEM', label: 'Modo Ensayo' },
    ]
  }
  if (contexto.value === 'sede') {
    return [
      { value: 'ABOUT_ME', label: 'Mi Reputación' },
      { value: 'TEACHER', label: 'Maestros' },
      { value: 'STUDENT', label: 'Mis Alumnos' },
      { value: 'SYSTEM', label: 'Modo Ensayo' },
    ]
  }
  return [
    { value: 'CLASS', label: 'Clases' },
    { value: 'TEACHER', label: 'Maestros' },
    { value: 'VENUE', label: 'Sedes' },
    { value: 'ABOUT_ME', label: 'Mi Comportamiento' },
    { value: 'SYSTEM', label: 'Modo Ensayo' },
  ]
})

const tipoSobreMi = computed(() => {
  if (contexto.value === 'profesor') return 'TEACHER'
  if (contexto.value === 'sede') return 'VENUE'
  return 'STUDENT'
})
const sobreMiFiltrado = computed(() => sobreMi.value.filter(r => r.targetType === tipoSobreMi.value))
const tituloSobreMi = computed(() => {
  if (contexto.value === 'profesor') return 'Reseñas sobre mí como maestro'
  if (contexto.value === 'sede') return 'Reseñas sobre mi sede'
  return 'Mi comportamiento como alumno'
})
const subtituloSobreMi = computed(() => {
  if (contexto.value === 'profesor') return 'Lo que alumnos y sedes han opinado de ti.'
  if (contexto.value === 'sede') return 'Lo que maestros y alumnos han opinado de tu sede.'
  return 'Valoraciones que tus maestros y sedes han dejado sobre ti.'
})
const vacioSobreMi = computed(() => {
  if (contexto.value === 'profesor') return 'Todavía no tienes reseñas como maestro.'
  if (contexto.value === 'sede') return 'Todavía no hay reseñas sobre tu sede.'
  return 'Todavía nadie ha valorado tu comportamiento como alumno.'
})

const misPorTipo = (tipo) => mias.value.filter(r => r.targetType === tipo)
const comunidadPorTipo = (tipo) => comunidad.value.filter(r => r.targetType === tipo)
const elegiblesPorTipo = (tipo) => elegiblesTargets.value.filter(t => t.targetType === tipo)

const estrellas = (s) => { const n = Math.round(s || 0); return '★'.repeat(n) + '☆'.repeat(5 - n) }

async function cargar() {
  const [elig, tgts, m, c, sm, sis] = await Promise.all([
    reviewService.getStudentEligible().then(r => r.data).catch(() => []),
    reviewService.getEligibleTargets().then(r => r.data).catch(() => []),
    reviewService.getMine().then(r => r.data).catch(() => []),
    reviewService.getRecent().then(r => r.data).catch(() => []),
    reviewService.getAboutMe().then(r => r.data).catch(() => []),
    reviewService.getMySystemReview().then(r => r.data).catch(() => null)
  ])
  elegibles.value = Array.isArray(elig) ? elig : []
  elegiblesTargets.value = Array.isArray(tgts) ? tgts : []
  mias.value = Array.isArray(m) ? m : []
  comunidad.value = Array.isArray(c) ? c : []
  sobreMi.value = Array.isArray(sm) ? sm : []
  miSistema.value = sis || null
}

onMounted(async () => {
  tab.value = tabsVisibles.value[0]?.value || 'CLASS'
  try { await cargar() } finally { loading.value = false }
})

async function enviarClase(item, score, comment) {
  enviando[item.classId] = true
  errores[item.classId] = ''
  try {
    await reviewService.create({ classId: item.classId, targetId: item.targetId, targetType: 'CLASS', score, comment: comment || null })
    elegibles.value = elegibles.value.filter(e => e.classId !== item.classId)
    formActivo.value = null
    mias.value = await reviewService.getMine().then(r => r.data).catch(() => mias.value)
  } catch (e) {
    errores[item.classId] = e.response?.data?.message || 'Error al enviar la reseña'
  } finally {
    enviando[item.classId] = false
  }
}

async function valorarTarget({ target, score, comment }) {
  const key = target.targetType + ':' + target.targetId
  valorando[key] = { enviando: true, error: '' }
  try {
    await reviewService.create({ targetId: target.targetId, targetType: target.targetType, score, comment: comment || null })
    elegiblesTargets.value = elegiblesTargets.value.filter(t => (t.targetType + ':' + t.targetId) !== key)
    mias.value = await reviewService.getMine().then(r => r.data).catch(() => mias.value)
    valorando[key] = { enviando: false, error: '' }
  } catch (e) {
    valorando[key] = { enviando: false, error: e.response?.data?.message || 'Error al enviar' }
  }
}

async function valorarSistema(score, comment) {
  enviandoSistema.value = true
  errorSistema.value = ''
  try {
    const res = await reviewService.create({ targetType: 'SYSTEM', score, comment: comment || null })
    miSistema.value = res.data
  } catch (e) {
    errorSistema.value = e.response?.data?.message || 'Error al enviar la valoración'
  } finally {
    enviandoSistema.value = false
  }
}

// ── Componente: formulario de estrellas + comentario ──
const Valorador = {
  props: {
    enviando: Boolean, error: String,
    placeholder: { type: String, default: 'Cuéntanos tu experiencia...' },
    textoEnviar: { type: String, default: 'Publicar reseña' },
    mostrarCancelar: { type: Boolean, default: true }
  },
  emits: ['enviar', 'cancelar'],
  setup(props, { emit }) {
    const rating = ref(0)
    const comment = ref('')
    return () => h('div', { class: 'space-y-3 border-t border-dark-border pt-4' }, [
      h('div', {}, [
        h('p', { class: 'text-sm text-gray-300 mb-2' }, 'Calificación'),
        h('div', { class: 'flex gap-1' }, [1, 2, 3, 4, 5].map(n =>
          h('button', {
            class: ['text-2xl transition-colors', rating.value >= n ? 'text-yellow-400' : 'text-gray-600'],
            onClick: () => { rating.value = n }
          }, '★')))
      ]),
      h('div', {}, [
        h('label', { class: 'block text-sm text-gray-300 mb-1' }, 'Comentario (opcional)'),
        h('textarea', {
          rows: 3, class: 'input-field', placeholder: props.placeholder,
          value: comment.value, onInput: (e) => { comment.value = e.target.value }
        })
      ]),
      props.error ? h('p', { class: 'text-red-400 text-sm' }, props.error) : null,
      h('div', { class: 'flex gap-3' }, [
        h('button', {
          class: 'btn-primary flex-1 text-sm', disabled: !rating.value || props.enviando,
          onClick: () => rating.value && emit('enviar', rating.value, comment.value)
        }, props.enviando ? 'Enviando...' : props.textoEnviar),
        props.mostrarCancelar ? h('button', { class: 'btn-secondary text-sm px-4', onClick: () => emit('cancelar') }, 'Cancelar') : null
      ])
    ])
  }
}

// ── Componente: seccion "puedes valorar" con lista de objetivos elegibles ──
const SeccionValorar = {
  props: { tipo: String, titulo: String, targets: Array, estado: Object },
  emits: ['enviar'],
  components: { Valorador },
  setup(props, { emit }) {
    const abierto = ref(null)
    if (!props.targets || props.targets.length === 0) return () => null
    return () => h('section', {}, [
      h('h2', { class: 'text-lg font-semibold text-white mb-1' }, props.titulo),
      h('p', { class: 'text-gray-400 text-sm mb-4' }, 'Deja tu valoración de forma respetuosa y honesta.'),
      h('div', { class: 'space-y-3' }, props.targets.map(t => {
        const key = t.targetType + ':' + t.targetId
        const st = props.estado[key] || {}
        return h('div', { key, class: 'card' }, [
          h('div', { class: 'flex items-center justify-between' }, [
            h('h3', { class: 'text-white font-medium' }, t.targetLabel),
            abierto.value !== key ? h('button', { class: 'btn-secondary text-xs !py-1 !px-3', onClick: () => { abierto.value = key } }, 'Valorar') : null
          ]),
          abierto.value === key ? h(Valorador, {
            enviando: st.enviando, error: st.error,
            onEnviar: (score, comment) => emit('enviar', { target: t, score, comment }),
            onCancelar: () => { abierto.value = null }
          }) : null
        ])
      }))
    ])
  }
}

// ── Componente: lista de reseñas ──
const ListaReseñas = {
  props: { titulo: String, subtitulo: String, items: Array, vacio: String, mostrarAutor: Boolean },
  setup(props) {
    const est = (s) => { const n = Math.round(s || 0); return '★'.repeat(n) + '☆'.repeat(5 - n) }
    const fecha = (d) => d ? new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'long', year: 'numeric' }) : ''
    return () => h('section', {}, [
      h('h2', { class: 'text-lg font-semibold text-white mb-1' }, props.titulo),
      h('p', { class: 'text-gray-400 text-sm mb-4' }, props.subtitulo),
      props.items.length === 0
        ? h('div', { class: 'card text-center py-8' }, [h('p', { class: 'text-gray-400 text-sm' }, props.vacio)])
        : h('div', { class: 'space-y-3' }, props.items.map(r => h('div', { key: r.id, class: 'card' }, [
            h('div', { class: 'flex items-start justify-between gap-3' }, [
              h('div', {}, [
                h('h3', { class: 'text-white font-medium' }, r.targetName || r.classTitle || 'Reseña'),
                props.mostrarAutor ? h('p', { class: 'text-gray-500 text-xs mt-0.5' }, 'por ' + (r.authorName || 'Usuario')) : null
              ]),
              h('span', { class: 'text-yellow-400 text-sm flex-shrink-0' }, est(r.score))
            ]),
            r.comment ? h('p', { class: 'text-gray-400 text-sm mt-2' }, r.comment) : null,
            h('p', { class: 'text-gray-600 text-xs mt-2' }, fecha(r.createdAt))
          ])))
    ])
  }
}
</script>
