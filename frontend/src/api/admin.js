import { apiFetch } from './client'

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
