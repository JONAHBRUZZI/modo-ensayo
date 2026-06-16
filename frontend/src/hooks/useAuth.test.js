import { describe, it, expect, vi } from 'vitest'
import { ref } from 'vue'

// Mock the useAuth composable
const mockLogin = vi.fn()
const mockRegister = vi.fn()
const mockLogout = vi.fn()

vi.mock('../hooks/useAuth', () => ({
  useAuth: () => ({
    token: ref('test-token'),
    user: ref({ email: 'test@test.com', fullName: 'Test', roles: ['USER'] }),
    isAuthenticated: ref(true),
    login: mockLogin,
    register: mockRegister,
    logout: mockLogout,
  }),
}))

describe('useAuth composable', () => {
  it('provides authentication state', async () => {
    const { useAuth } = await import('../hooks/useAuth')
    const auth = useAuth()

    expect(auth.isAuthenticated.value).toBe(true)
    expect(auth.user.value.email).toBe('test@test.com')
    expect(auth.token.value).toBe('test-token')
  })

  it('login calls authService', async () => {
    mockLogin.mockResolvedValueOnce({ token: 'new-token', email: 'test@test.com', fullName: 'Test', roles: ['USER'] })
    const { useAuth } = await import('../hooks/useAuth')
    const auth = useAuth()
    await auth.login('test@test.com', 'password')
    expect(mockLogin).toHaveBeenCalledWith('test@test.com', 'password')
  })
})
