import { apiFetch } from './client'

// GET /api/organizations/signup-options — 회원가입 화면에서 선택 가능한 ACTIVE 기관 목록 (인증 불필요)
export function fetchSignupOrganizations() {
  return apiFetch('/api/organizations/signup-options', { auth: false })
}

// GET /api/admin/organizations — Admin 기관 관리 목록. learnerCount는 파생값입니다.
export async function fetchOrganizations() {
  const response = await apiFetch('/api/admin/organizations')
  return response.items
}

// POST /api/admin/organizations — 기관 생성 (항상 ACTIVE로 생성됨)
export function createOrganization(name) {
  return apiFetch('/api/admin/organizations', { method: 'POST', body: { name } })
}

// PATCH /api/admin/organizations/{id} — 기관명 수정
export function renameOrganization(organizationId, name) {
  return apiFetch(`/api/admin/organizations/${organizationId}`, { method: 'PATCH', body: { name } })
}

// PATCH /api/admin/organizations/{id}/status — 기관 활성화/비활성화.
// 하드 삭제는 지원하지 않습니다: User가 소속되어 있어도 상태만 전환됩니다.
export function changeOrganizationStatus(organizationId, status) {
  return apiFetch(`/api/admin/organizations/${organizationId}/status`, { method: 'PATCH', body: { status } })
}
