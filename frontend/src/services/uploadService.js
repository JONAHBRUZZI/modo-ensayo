import api from './api'

export const uploadService = {
  async upload(file, type = 'documents') {
    const formData = new FormData()
    formData.append('file', file)
    const { data } = await api.post(`/upload?type=${type}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return data
  }
}
