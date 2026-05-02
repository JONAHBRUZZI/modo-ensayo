import { createRouter, createWebHistory } from 'vue-router'
import DefaultLayout from '../layouts/DefaultLayout.vue'

const routes = [
  {
    path: '/',
    component: DefaultLayout,
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('../pages/HomePage.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'login',
        name: 'Login',
        component: () => import('../pages/LoginPage.vue'),
        meta: { guest: true },
      },
      {
        path: 'register',
        name: 'Register',
        component: () => import('../pages/RegisterPage.vue'),
        meta: { guest: true },
      },
      {
        path: 'classes',
        name: 'Classes',
        component: () => import('../pages/ClassesPage.vue'),
      },
      {
        path: 'cart',
        name: 'Cart',
        component: () => import('../pages/CartPage.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../pages/ProfilePage.vue'),
        meta: { requiresAuth: true },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth && !token) {
    next({ name: 'Login' })
    return
  }

  if (to.meta.guest && token) {
    next({ name: 'Home' })
    return
  }

  next()
})

export default router
