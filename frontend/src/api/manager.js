import { apiFetch } from './client'

// GET /api/manager/learners — 내 기관 소속 Learner 목록 (개별 발화/피드백은 절대 포함되지 않음)
export async function fetchManagedLearners() {
  const response = await apiFetch('/api/manager/learners')
  return response.items
}

export function approveLearner(userId) {
  return apiFetch(`/api/manager/learners/${userId}/approve`, { method: 'PATCH' })
}

export function rejectLearner(userId) {
  return apiFetch(`/api/manager/learners/${userId}/reject`, { method: 'PATCH' })
}

export function deactivateLearner(userId) {
  return apiFetch(`/api/manager/learners/${userId}/deactivate`, { method: 'PATCH' })
}

export function activateLearner(userId) {
  return apiFetch(`/api/manager/learners/${userId}/activate`, { method: 'PATCH' })
}
