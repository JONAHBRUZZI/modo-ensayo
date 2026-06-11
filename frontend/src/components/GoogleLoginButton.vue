<template>
  <div class="space-y-3">
    <div class="flex items-center gap-3">
      <div class="flex-1 h-px bg-white/10"></div>
      <span class="text-xs text-gray-500">o continua con</span>
      <div class="flex-1 h-px bg-white/10"></div>
    </div>

    <button type="button" @click="handleClick" :disabled="loading"
      class="w-full flex items-center justify-center gap-3 px-4 py-2.5 rounded-xl border border-white/10 bg-white/5 hover:bg-white/10 text-gray-300 hover:text-white transition-colors disabled:opacity-50 text-sm">
      <svg class="w-4 h-4" viewBox="0 0 24 24">
        <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 01-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"/>
        <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
        <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
        <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
      </svg>
      {{ loading ? 'Conectando...' : 'Continuar con Google' }}
    </button>

    <p v-if="error" class="text-red-400 text-xs text-center">{{ error }}</p>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'

const CLIENT_ID = '323975165685-t62bgluf8tj6dma9uj5i8gqduqpufd4c.apps.googleusercontent.com'

const router = useRouter()
const { googleLogin } = useAuth()
const loading = ref(false)
const error = ref('')

let initialized = false

async function initGoogle() {
  if (initialized) return
  initialized = true
  await new Promise(resolve => {
    const check = () => {
      if (window.google?.accounts?.id) resolve()
      else setTimeout(check, 100)
    }
    check()
  })
  google.accounts.id.initialize({
    client_id: CLIENT_ID,
    callback: handleCredential,
    auto_select: false
  })
}

function handleClick() {
  loading.value = true
  error.value = ''
  initGoogle().then(() => {
    google.accounts.id.prompt()
  }).catch(() => {
    error.value = 'Google no esta disponible en este momento'
    loading.value = false
  })
}

async function handleCredential(response) {
  try {
    await googleLogin(response.credential)
    router.push('/alumno/dashboard')
  } catch (e) {
    error.value = e?.response?.data?.message || 'Error al iniciar con Google'
  } finally {
    loading.value = false
  }
}
</script>
