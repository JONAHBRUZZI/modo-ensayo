import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { MotionPlugin } from '@vueuse/motion'
import { initAnalytics } from './analytics'
import './style.css'

const app = createApp(App)
app.use(router)
app.use(MotionPlugin)
initAnalytics(router)
app.mount('#app')
