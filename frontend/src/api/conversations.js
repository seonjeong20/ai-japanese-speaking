import { apiFetch, apiFetchFormData } from './client'

// POST /api/conversations — 일반 회화 세션 시작
export function startConversation(settings) {
  return apiFetch('/api/conversations', {
    method: 'POST',
    body: {
      situation: settings.situation || undefined,
      partnerRole: settings.partner || undefined,
      partnerPersonality: settings.personality || undefined,
      situationDescription: settings.description || undefined,
      difficulty: settings.difficulty,
      subtitleMode: settings.subtitleMode,
    },
  })
}

// POST /api/conversations/{sessionId}/turns/audio — 사용자 발화 오디오 업로드 → STT → LLM → TTS
export function submitAudioTurn(sessionId, audioBlob, fileName) {
  const formData = new FormData()
  formData.append('audio', audioBlob, fileName)
  return apiFetchFormData(`/api/conversations/${sessionId}/turns/audio`, formData)
}

// POST /api/conversations/{sessionId}/complete — 정상 종료 및 Feedback 생성
export function completeConversation(sessionId) {
  return apiFetch(`/api/conversations/${sessionId}/complete`, { method: 'POST' })
}

// POST /api/conversations/{sessionId}/abort — 중도 종료 (Feedback 생성 없음)
export function abortConversation(sessionId) {
  return apiFetch(`/api/conversations/${sessionId}/abort`, { method: 'POST' })
}

// GET /api/conversations/{sessionId}/feedback — 저장된 Feedback 조회 (AI 재호출 없음)
export function getConversationFeedback(sessionId) {
  return apiFetch(`/api/conversations/${sessionId}/feedback`)
}
