<template>
  <router-view v-slot="{ Component }">
    <Transition name="page" mode="out-in">
      <component :is="Component" :key="$route.path" />
    </Transition>
  </router-view>
  <AppToast />
</template>

<script setup>
import { onMounted } from 'vue'
import { useAuth } from './stores/auth'
import { useTheme } from './composables/useTheme'
import { decodeJwt } from '@/utils/jwt'
import AppToast from './components/AppToast.vue'

const { token } = useAuth()
const { applyTheme } = useTheme()

onMounted(() => {
  applyTheme()
  checkTokenExpiration()
})

function checkTokenExpiration() {
  if (!token.value) return
  const payload = decodeJwt(token.value)
  if (!payload) return
  if (payload.exp * 1000 < Date.now()) {
    localStorage.removeItem('auth_token')
    localStorage.removeItem('auth_user')
    localStorage.removeItem('auth_refresh_token')
    window.location.href = '/login'
  }
}
</script>

<style>
.page-enter-active,
.page-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}
.page-enter-from {
  opacity: 0;
  transform: translateY(6px);
}
.page-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
