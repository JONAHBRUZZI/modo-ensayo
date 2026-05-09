<template>
  <div class="min-h-[80vh] flex items-center justify-center py-12 px-4">
    <div class="max-w-sm w-full space-y-6">
      <div class="text-center">
        <div class="inline-flex items-center justify-center w-12 h-12 bg-yellow-400/10 rounded-full border border-yellow-400/20 mb-4">
          <svg class="w-6 h-6 text-yellow-400" fill="currentColor" viewBox="0 0 24 24">
            <path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/>
          </svg>
        </div>
        <h1 class="text-2xl font-bold text-white">Crear una cuenta</h1>
        <p class="text-gray-400 text-sm mt-1">Únete a la comunidad artística</p>
      </div>

      <div class="bg-[#161824] rounded-2xl border border-white/10 p-6 shadow-2xl space-y-4">
        <div v-if="error" class="bg-red-500/15 border border-red-500/30 text-red-400 text-sm rounded-lg p-3">
          {{ error }}
        </div>

        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Nombre Completo</label>
          <input v-model="form.fullName" type="text" placeholder="Ana González"
                 class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm transition-all" />
        </div>

        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Documento de identidad</label>
          <DocumentoIdentidad v-model="form.documento" />
        </div>

        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Email</label>
          <input v-model="form.email" type="email" placeholder="tu@email.com"
                 class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm transition-all" />
        </div>

        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Teléfono <span class="text-gray-600">(opcional)</span></label>
          <input v-model="form.phone" type="tel" placeholder="+56 9 1234 5678"
                 class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm transition-all" />
        </div>

        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Contraseña</label>
          <div class="relative">
            <input v-model="form.password" :type="showPassword ? 'text' : 'password'" placeholder="Mínimo 8 caracteres"
                   class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm pr-10 transition-all" />
            <button type="button" @click="showPassword = !showPassword"
                    class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-300 transition-colors">
              <svg v-if="!showPassword" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0zM2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
              <svg v-else class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
              </svg>
            </button>
          </div>
          <!-- Indicador de fortaleza -->
          <div v-if="form.password" class="mt-2 space-y-1">
            <div class="flex gap-1">
              <div v-for="i in 4" :key="i" :class="i <= passwordStrength ? strengthColors[passwordStrength - 1] : 'bg-white/10'" class="h-1 flex-1 rounded-full transition-all" />
            </div>
            <p class="text-xs" :class="strengthTextColors[passwordStrength - 1] || 'text-gray-500'">
              {{ strengthLabels[passwordStrength - 1] || '' }}
            </p>
          </div>
        </div>

        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Confirmar Contraseña</label>
          <input v-model="form.confirmPassword" :type="showPassword ? 'text' : 'password'" placeholder="Repite tu contraseña"
                 class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm transition-all"
                 :class="form.confirmPassword && form.password !== form.confirmPassword ? 'border-red-500/50' : ''" />
          <p v-if="form.confirmPassword && form.password !== form.confirmPassword" class="text-xs text-red-400 mt-1">Las contraseñas no coinciden</p>
        </div>

        <label class="flex items-start gap-3 cursor-pointer group">
          <div class="relative mt-0.5">
            <input v-model="acceptTerms" type="checkbox" class="sr-only" />
            <div :class="acceptTerms ? 'bg-indigo-600 border-indigo-600' : 'bg-[#0d0f1a] border-white/15'"
                 class="w-4 h-4 rounded border-2 transition-colors flex items-center justify-center">
              <svg v-if="acceptTerms" class="w-2.5 h-2.5 text-white" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
              </svg>
            </div>
          </div>
          <span class="text-xs text-gray-400 leading-relaxed">
            Acepto los <a href="#" class="text-indigo-400 hover:underline">Términos de Servicio</a> y la
            <a href="#" class="text-indigo-400 hover:underline">Política de Privacidad</a>
          </span>
        </label>

        <button @click="handleRegister" :disabled="loading || !canSubmit"
                class="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg text-sm font-semibold transition-colors shadow-lg shadow-indigo-500/25">
          <span v-if="loading" class="flex items-center justify-center gap-2">
            <svg class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
            </svg>
            Creando cuenta...
          </span>
          <span v-else>Crear Cuenta</span>
        </button>
      </div>

      <p class="text-center text-sm text-gray-500">
        ¿Ya tienes cuenta?
        <router-link to="/login" class="text-indigo-400 hover:text-indigo-300 font-medium transition-colors">Iniciar sesión</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../hooks/useAuth'
import DocumentoIdentidad from '../components/DocumentoIdentidad.vue'

const { register } = useAuth()
const router = useRouter()

const form = ref({
  fullName: '',
  documento: { tipo: 'RUT', numero: '', valido: false },
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
})
const showPassword = ref(false)
const acceptTerms = ref(false)
const loading = ref(false)
const error = ref('')

const strengthLabels = ['Muy débil', 'Débil', 'Buena', 'Fuerte']
const strengthColors = ['bg-red-500', 'bg-amber-500', 'bg-blue-500', 'bg-emerald-500']
const strengthTextColors = ['text-red-400', 'text-amber-400', 'text-blue-400', 'text-emerald-400']

const passwordStrength = computed(() => {
  const p = form.value.password
  if (!p) return 0
  let score = 0
  if (p.length >= 8) score++
  if (/[A-Z]/.test(p)) score++
  if (/[0-9]/.test(p)) score++
  if (/[^A-Za-z0-9]/.test(p)) score++
  return score
})

const canSubmit = computed(() =>
  form.value.fullName && form.value.email && form.value.password &&
  form.value.password === form.value.confirmPassword &&
  form.value.password.length >= 8 && acceptTerms.value
)

const handleRegister = async () => {
  if (!canSubmit.value) return
  loading.value = true
  error.value = ''
  try {
    const { documento } = form.value
    await register(
      form.value.fullName,
      form.value.email,
      form.value.password,
      form.value.phone,
      documento.tipo === 'RUT' ? documento.numero : null,
      documento.tipo === 'PASAPORTE' ? documento.numero : null,
    )
    router.push('/alumno/dashboard')
  } catch (e) {
    error.value = e?.response?.data?.message || 'Error al crear la cuenta. Intenta nuevamente.'
  } finally {
    loading.value = false
  }
}
</script>
