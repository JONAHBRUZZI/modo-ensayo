import api from './api'

export const classService = {
  async listPublished() {
    const { data } = await api.get('/classes')
    return data
  },
}
