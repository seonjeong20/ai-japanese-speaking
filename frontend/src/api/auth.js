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

// POST /api/auth/signup — LEARNER/MANAGER 가입 신청 (결과는 항상 PENDING)
export function signup({ name, email, password, role, organizationId, department }) {
  return apiFetch('/api/auth/signup', {
    method: 'POST',
    body: { name, email, password, role, organizationId, department: department || undefined },
    auth: false,
  })
}
