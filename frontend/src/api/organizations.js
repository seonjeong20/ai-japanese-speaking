import { apiFetch } from './client'

// GET /api/organizations/signup-options — 회원가입 화면에서 선택 가능한 ACTIVE 기관 목록 (인증 불필요)
export function fetchSignupOrganizations() {
  return apiFetch('/api/organizations/signup-options', { auth: false })
}
