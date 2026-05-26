import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/HomePage.vue')
      },
      {
        path: 'login',
        name: 'Login',
        component: () => import('@/views/LoginPage.vue'),
        meta: { guest: true }
      },
      {
        path: 'register',
        name: 'Register',
        component: () => import('@/views/RegisterPage.vue'),
        meta: { guest: true }
      },
      {
        path: 'classes',
        name: 'Classes',
        component: () => import('@/views/ClassesPage.vue')
      },
      {
        path: 'payment/success',
        name: 'PaymentSuccess',
        component: () => import('@/views/PaymentSuccessPage.vue')
      },
      {
        path: 'payment/failure',
        name: 'PaymentFailure',
        component: () => import('@/views/PaymentFailurePage.vue')
      },
      {
        path: 'payment/pending',
        name: 'PaymentPending',
        component: () => import('@/views/PaymentPendingPage.vue')
      },
      {
        path: 'quiero-ser-profesor',
        name: 'QuieroSerProfesor',
        component: () => import('@/pages/acceso/QuieroSerProfesorPage.vue')
      },
      {
        path: 'quiero-gestionar-sede',
        name: 'QuieroGestionarSede',
        component: () => import('@/pages/acceso/QuieroGestionarSedePage.vue')
      },
      {
        path: 'cart',
        name: 'Cart',
        component: () => import('@/views/CartPage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/ProfilePage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'profile/identity',
        name: 'IdentityUpload',
        component: () => import('@/views/IdentityUploadPage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'profile/refund-method',
        name: 'RefundMethod',
        component: () => import('@/views/RefundMethodPage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'notificaciones',
        name: 'Notificaciones',
        component: () => import('@/views/NotificacionesPage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'reviews',
        name: 'Reviews',
        component: () => import('@/views/ReviewsPage.vue'),
        meta: { requiresAuth: true }
      },
      // Alumno routes
      {
        path: 'alumno/dashboard',
        name: 'AlumnoDashboard',
        component: () => import('@/views/alumno/AlumnoDashboardPage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'alumno/clases/:claseId',
        name: 'ClaseDetalle',
        component: () => import('@/views/ClaseDetallePage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'alumno/mis-clases',
        name: 'MisClases',
        component: () => import('@/views/alumno/MisClasesPage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'alumno/pagos',
        name: 'PagosHistorial',
        component: () => import('@/views/alumno/PagosHistorialPage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'alumno/crear-clase',
        name: 'AlumnoCrearClase',
        component: () => import('@/views/CrearClasePage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'alumno/asociados',
        name: 'Associates',
        component: () => import('@/views/AssociatesPage.vue'),
        meta: { requiresAuth: true }
      },
      // Profesor routes
      {
        path: 'profesor/dashboard',
        name: 'ProfesorDashboard',
        component: () => import('@/views/profesor/ProfesorDashboardPage.vue'),
        meta: { requiresAuth: true, roles: ['TEACHER'], requiresIdentity: true }
      },
      {
        path: 'profesor/clases-propias',
        name: 'ProfesorClasesPropias',
        component: () => import('@/views/profesor/ProfesorClasesPropiasPage.vue'),
        meta: { requiresAuth: true, roles: ['TEACHER'], requiresIdentity: true }
      },
      {
        path: 'profesor/clases-asignadas',
        name: 'ProfesorClasesAsignadas',
        component: () => import('@/views/profesor/ProfesorClasesAsignadasPage.vue'),
        meta: { requiresAuth: true, roles: ['TEACHER'], requiresIdentity: true }
      },
      {
        path: 'profesor/clases/:claseId/reagendamiento',
        name: 'ProfesorReagendamiento',
        component: () => import('@/views/profesor/ProfesorReagendamientoPage.vue'),
        meta: { requiresAuth: true, roles: ['TEACHER'], requiresIdentity: true }
      },
      {
        path: 'profesor/perfil-profesional',
        name: 'ProfesionalProfile',
        component: () => import('@/views/profesor/ProfesionalProfilePage.vue'),
        meta: { requiresAuth: true, roles: ['TEACHER'], requiresIdentity: true }
      },
      {
        path: 'profesor/metricas',
        name: 'ProfesorMetricas',
        component: () => import('@/views/profesor/ProfesorMetricasPage.vue'),
        meta: { requiresAuth: true, roles: ['TEACHER'], requiresIdentity: true }
      },
      {
        path: 'profesor/pagos',
        name: 'ProfesorPagos',
        component: () => import('@/views/profesor/ProfesorPagosPage.vue'),
        meta: { requiresAuth: true, roles: ['TEACHER'], requiresIdentity: true }
      },
      {
        path: 'profesor/buscar-salas',
        name: 'ProfesorBuscarSalas',
        component: () => import('@/views/profesor/BuscarSalasPage.vue'),
        meta: { requiresAuth: true, requiresIdentity: true }
      },
      {
        path: 'profesor/registro',
        name: 'TeacherRegistration',
        component: () => import('@/views/profesor/TeacherRegistrationPage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'profesor/asistencia/:classId',
        name: 'Attendance',
        component: () => import('@/views/profesor/AttendancePage.vue'),
        meta: { requiresAuth: true, roles: ['TEACHER'] }
      },
      // Sede routes
      {
        path: 'sede/dashboard',
        name: 'SedeDashboard',
        component: () => import('@/views/sede/SedeDashboardPage.vue'),
        meta: { requiresAuth: true, roles: ['VENUE_ADMIN'], requiresIdentity: true }
      },
      {
        path: 'sede/salas',
        name: 'SedeSalas',
        component: () => import('@/views/sede/SedeAgendaSalaPage.vue'),
        meta: { requiresAuth: true, roles: ['VENUE_ADMIN'], requiresIdentity: true }
      },
      {
        path: 'sede/salas/:salaId/agenda',
        name: 'SedeAgendaSala',
        component: () => import('@/views/sede/SedeAgendaSalaPage.vue'),
        meta: { requiresAuth: true, roles: ['VENUE_ADMIN'], requiresIdentity: true }
      },
      {
        path: 'sede/clases/:claseId/reagendamiento',
        name: 'SedeReagendamiento',
        component: () => import('@/views/sede/SedeReagendamientoPage.vue'),
        meta: { requiresAuth: true, roles: ['VENUE_ADMIN'], requiresIdentity: true }
      },
      {
        path: 'sede/crear-clase',
        name: 'SedeCrearClase',
        component: () => import('@/views/sede/SedeCrearClasePage.vue'),
        meta: { requiresAuth: true, roles: ['VENUE_ADMIN'], requiresIdentity: true }
      },
      {
        path: 'sede/mis-clases',
        name: 'SedeMisClases',
        component: () => import('@/views/sede/SedeMisClasesPage.vue'),
        meta: { requiresAuth: true, roles: ['VENUE_ADMIN'], requiresIdentity: true }
      },
      {
        path: 'sede/clases-por-confirmar',
        name: 'SedeClasesPorConfirmar',
        component: () => import('@/views/sede/SedeClasesPorConfirmarPage.vue'),
        meta: { requiresAuth: true, roles: ['VENUE_ADMIN'], requiresIdentity: true }
      },
      {
        path: 'sede/profesores',
        name: 'SedeProfesores',
        component: () => import('@/views/sede/SedeProfesoresPage.vue'),
        meta: { requiresAuth: true, roles: ['VENUE_ADMIN'], requiresIdentity: true }
      },
      {
        path: 'sede/metricas',
        name: 'SedeMetricas',
        component: () => import('@/views/sede/SedeMetricasPage.vue'),
        meta: { requiresAuth: true, roles: ['VENUE_ADMIN'], requiresIdentity: true }
      },
      {
        path: 'sede/configuracion',
        name: 'SedeConfiguracion',
        component: () => import('@/views/sede/SedeConfiguracionPage.vue'),
        meta: { requiresAuth: true, roles: ['VENUE_ADMIN'], requiresIdentity: true }
      },
      {
        path: 'sede/registro',
        name: 'VenueRegistration',
        component: () => import('@/views/sede/VenueRegistrationPage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'sede/sala-registro',
        name: 'RoomRegistration',
        component: () => import('@/views/sede/RoomRegistrationPage.vue'),
        meta: { requiresAuth: true }
      },
      // Admin routes
      {
        path: 'admin',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/AdminDashboardPage.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN'] }
      },
      {
        path: 'admin/usuarios',
        name: 'AdminUsuarios',
        component: () => import('@/views/admin/AdminUsuariosPage.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN'] }
      },
      {
        path: 'admin/roles',
        name: 'RolesManagement',
        component: () => import('@/views/admin/DynamicRolesPage.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN'] }
      }
    ]
  },
  // Redirects
  {
    path: '/associates',
    redirect: '/alumno/asociados'
  },
  {
    path: '/venues/register',
    redirect: '/sede/registro'
  },
  {
    path: '/rooms/register',
    redirect: '/sede/sala-registro'
  },
  {
    path: '/venue-admin',
    redirect: '/sede/dashboard'
  },
  {
    path: '/teacher/classes',
    redirect: '/profesor/clases-propias'
  },
  {
    path: '/teacher/register',
    redirect: '/profesor/registro'
  },
  {
    path: '/teacher/attendance/:classId',
    redirect: '/profesor/asistencia/:classId'
  },
  {
    path: '/alumno/buscar-salas',
    redirect: '/classes'
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('auth_token')
  const userRaw = localStorage.getItem('auth_user')
  let user = null
  try {
    user = JSON.parse(userRaw)
  } catch {}

  const isAuthenticated = () => {
    if (!token) return false
    try {
      const payload = JSON.parse(atob(token.split('.')[1]))
      return payload.exp * 1000 > Date.now()
    } catch {
      return false
    }
  }

  const hasRole = (roles) => {
    if (!user?.roles) return false
    return roles.some((r) => user.roles.includes(r))
  }

  // Check token expiration
  if (token && !isAuthenticated()) {
    localStorage.removeItem('auth_token')
    localStorage.removeItem('auth_user')
    localStorage.removeItem('auth_refresh_token')
    if (to.meta.requiresAuth) {
      return next('/login')
    }
  }

  if (to.meta.guest && isAuthenticated()) {
    return next('/')
  }

  if (to.meta.requiresAuth && !isAuthenticated()) {
    return next('/login')
  }

  if (to.meta.roles && !hasRole(to.meta.roles)) {
    return next('/')
  }

  const identidadValidada = () => {
    if (!user?.atributosActivos) return false
    return user.atributosActivos.identidadValidada === true
  }

  if (to.meta.requiresIdentity && !identidadValidada()) {
    return next('/alumno/dashboard')
  }

  next()
})

export default router
