import { apiFetch } from './client'

// GET /api/history — 로그인한 LEARNER 본인의 완료된 세션 목록 (최신순)
export function getMyHistory() {
  return apiFetch('/api/history')
}

// GET /api/history/{sessionId} — 세션 상세(설정/Transcript/모드별 Feedback)
export function getMyHistoryDetail(sessionId) {
  return apiFetch(`/api/history/${sessionId}`)
}
