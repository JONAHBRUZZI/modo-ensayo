import api from './api'

export const rolesService = {
  async list() {
    const { data } = await api.get('/admin/roles')
    return data
  },
  async assign(userId, roleName) {
    const { data } = await api.post(`/admin/users/${userId}/roles`, { roleName })
    return data
  },
  async revoke(userId, roleName) {
    await api.delete(`/admin/users/${userId}/roles/${roleName}`)
  },
  async listUsers() {
    const { data } = await api.get('/admin/users')
    return data
  }
}