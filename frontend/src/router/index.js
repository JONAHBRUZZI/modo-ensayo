import { createRouter, createWebHistory } from 'vue-router'
import DefaultLayout from '../layouts/DefaultLayout.vue'

const routes = [
  {
    path: '/',
    component: DefaultLayout,
    children: [
      // Públicas
      { path: '', name: 'Home', component: () => import('../pages/HomePage.vue') },
      { path: 'login', name: 'Login', component: () => import('../pages/LoginPage.vue'), meta: { guest: true } },
      { path: 'register', name: 'Register', component: () => import('../pages/RegisterPage.vue'), meta: { guest: true } },
      { path: 'classes', name: 'Classes', component: () => import('../pages/ClassesPage.vue') },
      { path: 'payment/success', name: 'PaymentSuccess', component: () => import('../pages/PaymentSuccessPage.vue') },
      { path: 'payment/failure', name: 'PaymentFailure', component: () => import('../pages/PaymentFailurePage.vue') },
      { path: 'payment/pending', name: 'PaymentPending', component: () => import('../pages/PaymentPendingPage.vue') },

      // Páginas de acceso / gates (públicas, explican el proceso)
      { path: 'quiero-ser-profesor', name: 'QuieroSerProfesor', component: () => import('../pages/acceso/QuieroSerProfesorPage.vue') },
      { path: 'quiero-gestionar-sede', name: 'QuieroGestionarSede', component: () => import('../pages/acceso/QuieroGestionarSedePage.vue') },

      // Autenticadas generales
      { path: 'cart', name: 'Cart', component: () => import('../pages/CartPage.vue'), meta: { requiresAuth: true } },
      { path: 'profile', name: 'Profile', component: () => import('../pages/ProfilePage.vue'), meta: { requiresAuth: true } },
      { path: 'profile/identity', name: 'IdentityUpload', component: () => import('../pages/IdentityUploadPage.vue'), meta: { requiresAuth: true } },
      { path: 'profile/refund-method', name: 'RefundMethod', component: () => import('../pages/RefundMethodPage.vue'), meta: { requiresAuth: true } },
      { path: 'notificaciones', name: 'Notificaciones', component: () => import('../pages/NotificacionesPage.vue'), meta: { requiresAuth: true } },

      // Módulo Alumno/Familia
      { path: 'alumno/dashboard', name: 'AlumnoDashboard', component: () => import('../pages/alumno/AlumnoDashboardPage.vue'), meta: { requiresAuth: true } },
      { path: 'alumno/clases', name: 'AlumnoClases', component: () => import('../pages/ClassesPage.vue') },
      { path: 'alumno/clases/:claseId', name: 'ClaseDetalle', component: () => import('../pages/alumno/ClaseDetallePage.vue'), meta: { requiresAuth: true } },
      { path: 'alumno/mis-clases', name: 'MisClases', component: () => import('../pages/alumno/MisClasesPage.vue'), meta: { requiresAuth: true } },
      { path: 'alumno/pagos', name: 'PagosHistorial', component: () => import('../pages/alumno/PagosHistorialPage.vue'), meta: { requiresAuth: true } },
      { path: 'alumno/buscar-salas', redirect: '/classes' },
      { path: 'alumno/crear-clase', name: 'AlumnoCrearClase', component: () => import('../pages/alumno/CrearClasePage.vue'), meta: { requiresAuth: true } },
      { path: 'alumno/asociados', name: 'Associates', component: () => import('../pages/AssociatesPage.vue'), meta: { requiresAuth: true } },

      // Módulo Profesor
      { path: 'profesor/dashboard', name: 'ProfesorDashboard', component: () => import('../pages/profesor/ProfesorDashboardPage.vue'), meta: { requiresAuth: true, roles: ['TEACHER'] } },
      { path: 'profesor/clases-propias', name: 'ProfesorClasesPropias', component: () => import('../pages/profesor/ProfesorClasesPropiasPage.vue'), meta: { requiresAuth: true, roles: ['TEACHER'] } },
      { path: 'profesor/clases-asignadas', name: 'ProfesorClasesAsignadas', component: () => import('../pages/profesor/ProfesorClasesAsignadasPage.vue'), meta: { requiresAuth: true, roles: ['TEACHER'] } },
      { path: 'profesor/clases/:claseId/reagendamiento', name: 'ProfesorReagendamiento', component: () => import('../pages/profesor/ProfesorReagendamientoPage.vue'), meta: { requiresAuth: true, roles: ['TEACHER'] } },
      { path: 'profesor/metricas', name: 'ProfesorMetricas', component: () => import('../pages/profesor/ProfesorMetricasPage.vue'), meta: { requiresAuth: true, roles: ['TEACHER'] } },
      { path: 'profesor/pagos', name: 'ProfesorPagos', component: () => import('../pages/profesor/ProfesorPagosPage.vue'), meta: { requiresAuth: true, roles: ['TEACHER'] } },
      { path: 'profesor/buscar-salas', name: 'ProfesorBuscarSalas', component: () => import('../pages/alumno/BuscarSalasPage.vue'), meta: { requiresAuth: true, roles: ['TEACHER'] } },
      { path: 'profesor/registro', name: 'TeacherRegistration', component: () => import('../pages/TeacherRegistrationPage.vue'), meta: { requiresAuth: true } },
      { path: 'profesor/asistencia/:classId', name: 'Attendance', component: () => import('../pages/AttendancePage.vue'), meta: { requiresAuth: true, roles: ['TEACHER'] } },

      // Módulo Sede
      { path: 'sede/dashboard', name: 'SedeDashboard', component: () => import('../pages/sede/SedeDashboardPage.vue'), meta: { requiresAuth: true, roles: ['VENUE_ADMIN'] } },
      { path: 'sede/salas', name: 'SedeSalas', component: () => import('../pages/sede/SedeAgendaSalaPage.vue'), meta: { requiresAuth: true, roles: ['VENUE_ADMIN'] } },
      { path: 'sede/salas/:salaId/agenda', name: 'SedeAgendaSala', component: () => import('../pages/sede/SedeAgendaSalaPage.vue'), meta: { requiresAuth: true, roles: ['VENUE_ADMIN'] } },
      { path: 'sede/clases/:claseId/reagendamiento', name: 'SedeReagendamiento', component: () => import('../pages/sede/SedeReagendamientoPage.vue'), meta: { requiresAuth: true, roles: ['VENUE_ADMIN'] } },
      { path: 'sede/crear-clase', name: 'SedeCrearClase', component: () => import('../pages/sede/SedeCrearClasePage.vue'), meta: { requiresAuth: true, roles: ['VENUE_ADMIN'] } },
      { path: 'sede/mis-clases', name: 'SedeMisClases', component: () => import('../pages/sede/SedeMisClasesPage.vue'), meta: { requiresAuth: true, roles: ['VENUE_ADMIN'] } },
      { path: 'sede/clases-por-confirmar', name: 'SedeClasesPorConfirmar', component: () => import('../pages/sede/SedeClasesPorConfirmarPage.vue'), meta: { requiresAuth: true, roles: ['VENUE_ADMIN'] } },
      { path: 'sede/profesores', name: 'SedeProfesores', component: () => import('../pages/sede/SedeProfesoresPage.vue'), meta: { requiresAuth: true, roles: ['VENUE_ADMIN'] } },
      { path: 'sede/metricas', name: 'SedeMetricas', component: () => import('../pages/sede/SedeMetricasPage.vue'), meta: { requiresAuth: true, roles: ['VENUE_ADMIN'] } },
      { path: 'sede/configuracion', name: 'SedeConfiguracion', component: () => import('../pages/sede/SedeConfiguracionPage.vue'), meta: { requiresAuth: true, roles: ['VENUE_ADMIN'] } },
      { path: 'sede/registro', name: 'VenueRegistration', component: () => import('../pages/VenueRegistrationPage.vue'), meta: { requiresAuth: true } },
      { path: 'sede/sala-registro', name: 'RoomRegistration', component: () => import('../pages/RoomRegistrationPage.vue'), meta: { requiresAuth: true } },

      // Módulo Admin
      { path: 'admin', name: 'AdminDashboard', component: () => import('../pages/AdminDashboardPage.vue'), meta: { requiresAuth: true, roles: ['ADMIN'] } },
      { path: 'admin/usuarios', name: 'AdminUsuarios', component: () => import('../pages/admin/AdminUsuariosPage.vue'), meta: { requiresAuth: true, roles: ['ADMIN'] } },
      { path: 'admin/roles', name: 'RolesManagement', component: () => import('../pages/DynamicRolesPage.vue'), meta: { requiresAuth: true, roles: ['ADMIN'] } },

      // Reseñas
      { path: 'reviews', name: 'Reviews', component: () => import('../pages/ReviewsPage.vue'), meta: { requiresAuth: true } },

      // Rutas legacy (redirigen a las nuevas)
      { path: 'associates', redirect: '/alumno/asociados' },
      { path: 'venues/register', redirect: '/sede/registro' },
      { path: 'rooms/register', redirect: '/sede/sala-registro' },
      { path: 'venue-admin', redirect: '/sede/dashboard' },
      { path: 'teacher/classes', redirect: '/profesor/clases-propias' },
      { path: 'teacher/register', redirect: '/profesor/registro' },
      { path: 'teacher/attendance/:classId', redirect: to => `/profesor/asistencia/${to.params.classId}` },
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
  if (to.meta.roles && user) {
    const userRoles = user.roles || []
    if (!to.meta.roles.some(r => userRoles.includes(r))) {
      // VENUE_ADMIN intentando acceder a rutas de TEACHER → ir a su propio panel
      if (to.meta.roles.includes('TEACHER') && userRoles.includes('VENUE_ADMIN')) {
        next({ name: 'SedeDashboard' })
      } else if (to.meta.roles.includes('TEACHER')) {
        next({ name: 'QuieroSerProfesor' })
      } else if (to.meta.roles.includes('VENUE_ADMIN')) {
        next({ name: 'QuieroGestionarSede' })
      } else {
        next({ name: 'Home' })
      }
      return
    }
  }
  next()
})

export default router
