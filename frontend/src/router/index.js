import { createRouter, createWebHistory } from 'vue-router'
import DefaultLayout from '../layouts/DefaultLayout.vue'

const routes = [
  {
    path: '/',
    component: DefaultLayout,
    children: [
      { path: '', name: 'Home', component: () => import('../pages/HomePage.vue') },
      { path: 'login', name: 'Login', component: () => import('../pages/LoginPage.vue'), meta: { guest: true } },
      { path: 'register', name: 'Register', component: () => import('../pages/RegisterPage.vue'), meta: { guest: true } },
      { path: 'classes', name: 'Classes', component: () => import('../pages/ClassesPage.vue') },
      { path: 'cart', name: 'Cart', component: () => import('../pages/CartPage.vue'), meta: { requiresAuth: true } },
      { path: 'profile', name: 'Profile', component: () => import('../pages/ProfilePage.vue'), meta: { requiresAuth: true } },
      { path: 'reviews', name: 'Reviews', component: () => import('../pages/ReviewsPage.vue'), meta: { requiresAuth: true } },
      { path: 'profile/refund-method', name: 'RefundMethod', component: () => import('../pages/RefundMethodPage.vue'), meta: { requiresAuth: true } },
      { path: 'profile/identity', name: 'IdentityUpload', component: () => import('../pages/IdentityUploadPage.vue'), meta: { requiresAuth: true } },
      { path: 'associates', name: 'Associates', component: () => import('../pages/AssociatesPage.vue'), meta: { requiresAuth: true } },
      { path: 'venues/register', name: 'VenueRegistration', component: () => import('../pages/VenueRegistrationPage.vue'), meta: { requiresAuth: true } },
      { path: 'rooms/register', name: 'RoomRegistration', component: () => import('../pages/RoomRegistrationPage.vue'), meta: { requiresAuth: true } },
      { path: 'admin', name: 'AdminDashboard', component: () => import('../pages/AdminDashboardPage.vue'), meta: { requiresAuth: true, roles: ['ADMIN'] } },
      { path: 'admin/roles', name: 'RolesManagement', component: () => import('../pages/DynamicRolesPage.vue'), meta: { requiresAuth: true, roles: ['ADMIN'] } },
      { path: 'venue-admin', name: 'VenueAdmin', component: () => import('../pages/VenueAdminPage.vue'), meta: { requiresAuth: true, roles: ['VENUE_ADMIN', 'ADMIN'] } },
      { path: 'teacher/classes', name: 'TeacherClasses', component: () => import('../pages/TeacherClassPage.vue'), meta: { requiresAuth: true, roles: ['TEACHER', 'ADMIN'] } },
      { path: 'teacher/register', name: 'TeacherRegistration', component: () => import('../pages/TeacherRegistrationPage.vue'), meta: { requiresAuth: true } },
      { path: 'teacher/attendance/:classId', name: 'Attendance', component: () => import('../pages/AttendancePage.vue'), meta: { requiresAuth: true, roles: ['TEACHER', 'ADMIN'] } },
      { path: 'payment/success', name: 'PaymentSuccess', component: () => import('../pages/PaymentSuccessPage.vue') },
      { path: 'payment/failure', name: 'PaymentFailure', component: () => import('../pages/PaymentFailurePage.vue') },
      { path: 'payment/pending', name: 'PaymentPending', component: () => import('../pages/PaymentPendingPage.vue') },
    ],
  },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const user = JSON.parse(localStorage.getItem('user') || 'null')

  if (to.meta.requiresAuth && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }
  if (to.meta.guest && token) {
    next({ name: 'Home' })
    return
  }
  if (to.meta.roles && user?.roles) {
    if (!to.meta.roles.some(r => user.roles.includes(r))) {
      next({ name: 'Home' })
      return
    }
  }
  next()
})

export default router
