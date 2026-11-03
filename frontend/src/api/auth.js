import { apiFetch, setSession } from './client'

// POST /api/auth/oauth/{provider} — 제공자가 돌려준 인가 코드로 가입 겸 로그인합니다.
// redirectUri는 인가 요청 때와 같은 값이어야 합니다.
export async function oauthLogin(provider, code, redirectUri) {
  const response = await apiFetch(`/api/auth/oauth/${provider}`, {
    method: 'POST',
    body: { code, redirectUri },
    auth: false,
  })
  setSession(response.accessToken, response.user)
  return { user: response.user, newUser: response.newUser }
}
