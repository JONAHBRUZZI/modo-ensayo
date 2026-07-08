// Google Analytics 4 (gtag). Usa VITE_GA_MEASUREMENT_ID si está definido; si no,
// cae al ID de producción por defecto. El Measurement ID es PÚBLICO (va en el HTML
// del navegador), así que dejarlo en el código es seguro y evita depender de una
// variable en Vercel. (El JSON de la service account SÍ es secreto → vive en Supabase.)
// Envía un page_view en cada cambio de ruta del SPA.
const GA_ID = import.meta.env.VITE_GA_MEASUREMENT_ID || 'G-JLYZFQXYX8'

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
