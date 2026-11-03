import { apiFetch, clearSession } from './client'

// DELETE /api/users/me — 회원 탈퇴. 성공(204)하면 브라우저의 토큰·사용자 정보를 지웁니다.
export async function deleteMe() {
  await apiFetch('/api/users/me', { method: 'DELETE' })
  clearSession()
}
