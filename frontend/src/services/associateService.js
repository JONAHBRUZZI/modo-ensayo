import api from './api'

export default {
  async getAssociates() {
    const res = await api.get('/associates')
    return res.data
  },

  async createAssociate(data) {
    const res = await api.post('/associates', data)
    return res.data
  },

  async deleteAssociate(id) {
    await api.delete(`/associates/${id}`)
  }
}
