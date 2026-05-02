import api from './api'

export const associateService = {
  async list() {
    const { data } = await api.get('/associates')
    return data
  },
  async create(associateData) {
    const { data } = await api.post('/associates', associateData)
    return data
  },
  async delete(id) {
    await api.delete(`/associates/${id}`)
  }
}