import { OAuthProvider } from '../data/enums'

// 구글·카카오 로그인 화면으로 보내는 주소를 만들고, 돌아왔을 때 state를 확인합니다.
// 제공자가 돌려준 인가 코드는 콜백 화면이 백엔드(POST /api/auth/oauth/{provider})로 넘깁니다.

const STATE_STORAGE_KEY = 'oauthState'

const PROVIDER_CONFIG = {
  [OAuthProvider.GOOGLE]: {
    authorizeUrl: 'https://accounts.google.com/o/oauth2/v2/auth',
    clientId: import.meta.env.VITE_GOOGLE_CLIENT_ID,
    scope: 'openid email profile',
  },
  [OAuthProvider.KAKAO]: {
    authorizeUrl: 'https://kauth.kakao.com/oauth/authorize',
    clientId: import.meta.env.VITE_KAKAO_REST_API_KEY,
    scope: 'account_email profile_nickname',
  },
}

export function isSupportedProvider(provider) {
  return Object.prototype.hasOwnProperty.call(PROVIDER_CONFIG, provider)
}

// .env에 클라이언트 ID가 없으면 false입니다. 이때 로그인 버튼은 이동하지 않고 안내만 보여 줍니다.
export function isProviderConfigured(provider) {
  return isSupportedProvider(provider) && Boolean(PROVIDER_CONFIG[provider].clientId)
}

// 인가 요청과 코드 교환 요청에 같은 값을 써야 합니다.
export function getRedirectUri(provider) {
  return `${window.location.origin}/auth/callback/${provider}`
}

function createState() {
  const bytes = new Uint8Array(16)
  window.crypto.getRandomValues(bytes)
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('')
}

// 제공자 로그인 화면 주소를 만듭니다. 무작위 state를 sessionStorage에 저장해 두었다가
// 돌아왔을 때 consumeState로 확인합니다. 설정되지 않은 제공자면 오류를 던집니다.
export function buildAuthorizeUrl(provider) {
  if (!isProviderConfigured(provider)) {
    throw new Error(`OAuth provider is not configured: ${provider}`)
  }

  const { authorizeUrl, clientId, scope } = PROVIDER_CONFIG[provider]
  const state = createState()
  sessionStorage.setItem(STATE_STORAGE_KEY, state)

  const params = new URLSearchParams({
    client_id: clientId,
    redirect_uri: getRedirectUri(provider),
    response_type: 'code',
    scope,
    state,
  })
  return `${authorizeUrl}?${params.toString()}`
}

// 저장해 둔 state를 한 번 꺼내 지우고, 돌아온 값과 같은지 돌려줍니다.
// 저장된 값이 없거나 다르면 false입니다.
export function consumeState(received) {
  const saved = sessionStorage.getItem(STATE_STORAGE_KEY)
  sessionStorage.removeItem(STATE_STORAGE_KEY)
  return Boolean(saved) && Boolean(received) && saved === received
}
