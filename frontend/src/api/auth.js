import { apiFetch, setSession } from './client'

export async function login(email, password) {
  const response = await apiFetch('/api/auth/login', {
    method: 'POST',
    body: { email, password },
    auth: false,
  })
  setSession(response.accessToken, response.user)
  return response.user
}
