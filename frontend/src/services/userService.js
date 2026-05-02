import api from './api'

export const userService = {
  async getProfile() {
    const { data } = await api.get('/users/me')
    return data
  },

  async updateProfile(profileData) {
    const { data } = await api.put('/users/me', profileData)
    return data
  },
}
