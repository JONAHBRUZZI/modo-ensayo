<template>
  <div class="space-y-3">
    <div class="flex items-center gap-3">
      <div class="flex-1 h-px bg-white/10"></div>
      <span class="text-xs text-gray-500">o continua con</span>
      <div class="flex-1 h-px bg-white/10"></div>
    </div>

    <div ref="buttonContainer"></div>

    <p v-if="error" class="text-red-400 text-xs text-center">{{ error }}</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'

const CLIENT_ID = '837997938835-ki42ei5m2mbhkrsbcd5bvtfj362p2kah.apps.googleusercontent.com'

const router = useRouter()
const { googleLogin } = useAuth()
const error = ref('')
const buttonContainer = ref(null)
let initialized = false

async function handleCredential(response) {
  try {
    await googleLogin(response.credential)
    router.push('/alumno/dashboard')
  } catch (e) {
    error.value = e?.response?.data?.message || 'Error al iniciar con Google'
  }
}

function waitForGoogle() {
  return new Promise((resolve) => {
    if (window.google?.accounts?.id) return resolve()
    const check = () => {
      if (window.google?.accounts?.id) resolve()
      else setTimeout(check, 200)
    }
    check()
  })
}

onMounted(async () => {
  if (initialized) return
  initialized = true

  await waitForGoogle()
  google.accounts.id.initialize({
    client_id: CLIENT_ID,
    callback: handleCredential,
    ux_mode: 'popup',
    auto_select: false
  })

  if (buttonContainer.value) {
    google.accounts.id.renderButton(buttonContainer.value, {
      type: 'standard',
      theme: 'filled_black',
      size: 'large',
      text: 'continue_with',
      shape: 'rectangular',
      width: buttonContainer.value.offsetWidth || 320
    })
  }
})
</script>
