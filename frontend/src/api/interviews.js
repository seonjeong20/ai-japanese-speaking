import { apiFetch, apiFetchFormData } from './client'

// POST /api/interviews — 면접 시작. Backend가 첫 질문까지 생성해 함께 반환한다.
export function startInterview(settings) {
  return apiFetch('/api/interviews', {
    method: 'POST',
    body: {
      jobRole: settings.job,
      interviewType: settings.interviewType || undefined,
      difficulty: settings.difficulty,
      additionalRequest: settings.additionalRequest || undefined,
      subtitleMode: settings.subtitleMode,
    },
  })
}

// 사용자 음성 답변 → STT → 답변 저장 → 항목별 평가
export function submitInterviewAudioAnswer(sessionId, questionId, audioBlob, fileName) {
  const formData = new FormData()
  formData.append('audio', audioBlob, fileName)
  return apiFetchFormData(
    `/api/interviews/${sessionId}/questions/${questionId}/answer/audio`,
    formData,
  )
}

// POST /api/interviews/{sessionId}/complete — 면접 정상 종료 및 종합 Feedback 생성
export function completeInterview(sessionId) {
  return apiFetch(`/api/interviews/${sessionId}/complete`, { method: 'POST' })
}

// GET /api/interviews/{sessionId}/feedback — 저장된 종합·답변별 Feedback 조회 (AI 재호출 없음)
export function getInterviewFeedback(sessionId) {
  return apiFetch(`/api/interviews/${sessionId}/feedback`)
}
