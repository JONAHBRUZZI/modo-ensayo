import { supabase, invokeFunction, camelize } from './supabase'

/**
 * Servicio de conexión Marketplace MercadoPago del profesor (vendedor).
 * - getStatus: estado de conexión (RPC get_my_seller_status, sin exponer tokens)
 * - connect: obtiene la URL de autorización OAuth y redirige a MercadoPago
 * - getPayouts: historial de liquidaciones (teacher_payouts, RLS propio)
 */
export default {
  async getStatus() {
    const { data, error } = await supabase.rpc('get_my_seller_status')
    if (error) throw error
    return camelize(data) // { connected, status, mpUserId, connectedAt }
  },

  async connect() {
    const { authUrl } = await invokeFunction('mp-oauth-start', { body: {} })
    if (!authUrl) throw new Error('No se pudo iniciar la conexión con MercadoPago')
    window.location.href = authUrl
  },

  async getPayouts() {
    const { data, error } = await supabase
      .from('teacher_payouts')
      .select('*, class:classes(title)')
      .order('created_at', { ascending: false })
    if (error) throw error
    return camelize(data)
  }
}
