import { apiFetch } from './client'

// GET /api/admin/dashboard — 플랫폼 전체(모든 Organization) 운영 현황 집계.
// AI 호출 없이 완료된 Speaking Session과 Organization/User 테이블만 근거로 계산됩니다.
export function fetchAdminDashboard() {
  return apiFetch('/api/admin/dashboard')
}

// GET /api/admin/managers — 전체 Manager 목록 (Admin은 Manager를 직접 생성하지 않음, 승인만 관리)
export async function fetchManagers() {
  const response = await apiFetch('/api/admin/managers')
  return response.items
}

export function approveManager(userId) {
  return apiFetch(`/api/admin/managers/${userId}/approve`, { method: 'PATCH' })
}

export function rejectManager(userId) {
  return apiFetch(`/api/admin/managers/${userId}/reject`, { method: 'PATCH' })
}

export function deactivateManager(userId) {
  return apiFetch(`/api/admin/managers/${userId}/deactivate`, { method: 'PATCH' })
}

export function activateManager(userId) {
  return apiFetch(`/api/admin/managers/${userId}/activate`, { method: 'PATCH' })
}
