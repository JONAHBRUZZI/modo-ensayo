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
import AppToast from './components/AppToast.vue'

const { token } = useAuth()

onMounted(() => { checkTokenExpiration() })

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
  } catch (err) {
    console.error('Error al verificar expiración del token', err)
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
