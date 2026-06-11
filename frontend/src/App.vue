<template>
  <router-view />
  <AppToast />
</template>

<script setup>
import { onMounted } from 'vue'
import { useAuth } from './stores/auth'
import AppToast from './components/AppToast.vue'

<script setup>
import { onMounted } from 'vue'
import { useAuth } from './stores/auth'

const { token } = useAuth()

onMounted(() => {
  checkTokenExpiration()
})

function checkTokenExpiration() {
  if (!token.value) return
  try {
    const payload = JSON.parse(atob(token.value.split('.')[1]))
    if (payload.exp * 1000 < Date.now()) {
      localStorage.removeItem('auth_token')
      localStorage.removeItem('auth_user')
      localStorage.removeItem('auth_refresh_token')
      window.location.href = '/login'
    }
  } catch {
    // token invalid
  }
}
</script>
