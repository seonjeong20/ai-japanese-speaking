import { apiFetch, clearSession } from './client'

// DELETE /api/users/me — 회원 탈퇴. 성공(204)하면 브라우저의 토큰·사용자 정보를 지웁니다.
export async function deleteMe() {
  await apiFetch('/api/users/me', { method: 'DELETE' })
  clearSession()
}

// GET /api/users/me/preferences — 학습 기본값(난이도·자막)을 조회합니다.
// 저장한 적이 없으면 서버가 INTERMEDIATE, JAPANESE_KOREAN을 돌려줍니다.
export async function getPreferences() {
  return apiFetch('/api/users/me/preferences')
}

// PATCH /api/users/me/preferences — 학습 기본값을 저장합니다. 두 값 모두 필요합니다.
// 성공하면 저장된 값과 message("설정이 저장되었습니다.")를 돌려줍니다.
export async function updatePreferences({ defaultDifficulty, defaultSubtitleMode }) {
  return apiFetch('/api/users/me/preferences', {
    method: 'PATCH',
    body: { defaultDifficulty, defaultSubtitleMode },
  })
}
