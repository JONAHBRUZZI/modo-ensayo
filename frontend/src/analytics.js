// Google Analytics 4 (gtag). Se activa solo si VITE_GA_MEASUREMENT_ID está
// definido (ej. G-XXXXXXXXXX); sin eso es un no-op, así que en local/desarrollo
// no rastrea nada. Envía un page_view en cada cambio de ruta del SPA.
const GA_ID = import.meta.env.VITE_GA_MEASUREMENT_ID

export function initAnalytics(router) {
  if (!GA_ID) return

  const s = document.createElement('script')
  s.async = true
  s.src = `https://www.googletagmanager.com/gtag/js?id=${GA_ID}`
  document.head.appendChild(s)

  window.dataLayer = window.dataLayer || []
  function gtag() { window.dataLayer.push(arguments) }
  window.gtag = gtag
  gtag('js', new Date())
  // send_page_view:false → lo emitimos manualmente por ruta (SPA sin recargas).
  gtag('config', GA_ID, { send_page_view: false })

  router.afterEach((to) => {
    gtag('event', 'page_view', { page_path: to.fullPath, page_title: document.title })
  })
}
