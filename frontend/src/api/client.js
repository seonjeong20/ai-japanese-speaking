// Backend API 공통 설정. Vite dev 서버(5173)와 Spring Boot(8080)가 서로 다른
// origin이므로, 이 base URL로 직접 fetch하고 Backend는 최소 CORS 설정으로 허용합니다.
export const API_BASE_URL = 'http://localhost:8080'

const TOKEN_STORAGE_KEY = 'accessToken'
const USER_STORAGE_KEY = 'currentUser'

// 토큰이 만료되어 서버가 401을 반환하면 이 이벤트를 쏴서, Router 컨텍스트 안의
// 리스너(App.jsx)가 로그인 화면으로 보내도록 합니다. (client.js는 Router 밖에 있어
// useNavigate를 직접 쓸 수 없어 이 방식으로 연결합니다.)
export const AUTH_EXPIRED_EVENT = 'auth:expired'

export function getToken() {
  return localStorage.getItem(TOKEN_STORAGE_KEY)
}

export function setSession(accessToken, user) {
  localStorage.setItem(TOKEN_STORAGE_KEY, accessToken)
  localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user))
}

export function getCurrentUser() {
  const raw = localStorage.getItem(USER_STORAGE_KEY)
  return raw ? JSON.parse(raw) : null
}

export function clearSession() {
  localStorage.removeItem(TOKEN_STORAGE_KEY)
  localStorage.removeItem(USER_STORAGE_KEY)
}

// JWT의 payload(exp)만 읽어 만료 여부를 판단합니다. 서명 검증은 서버만 할 수 있으니
// 여기서는 라우트 진입을 막을지 말지 판단하는 용도로만 씁니다.
function isTokenExpired(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    if (!payload.exp) return false
    return Date.now() >= payload.exp * 1000
  } catch {
    return true
  }
}

// ProtectedRoute가 라우트 진입 시점에 호출합니다: 토큰이 없거나 이미 만료된 경우
// 세션을 정리하고 false를 반환합니다.
export function hasValidSession() {
  const token = getToken()
  if (!token) return false
  if (isTokenExpired(token)) {
    clearSession()
    return false
  }
  return true
}

class ApiError extends Error {
  constructor(status, code, message) {
    super(message || `API 요청이 실패했습니다. (${status})`)
    this.status = status
    this.code = code
  }
}

const NETWORK_ERROR_MESSAGE = '서버에 연결할 수 없습니다. 네트워크 연결을 확인한 후 다시 시도해주세요.'

// fetch() 자체가 실패하는 경우(서버 다운, 인터넷 연결 끊김, CORS 등)는 HTTP 응답이 없어
// parseResponse를 타지 않습니다. 이때 브라우저가 던지는 "Failed to fetch" 같은 원문 메시지가
// 그대로 사용자에게 노출되지 않도록, 여기서 한 번 감싸 동일한 ApiError로 통일합니다.
async function requestJson(url, options) {
  let response
  try {
    response = await fetch(url, options)
  } catch {
    throw new ApiError(0, 'NETWORK_ERROR', NETWORK_ERROR_MESSAGE)
  }
  return parseResponse(response)
}

// JSON 요청/응답 전용 헬퍼입니다. 인증이 필요한 요청에는 저장된 JWT를 자동으로 실어 보냅니다.
export async function apiFetch(path, { method = 'GET', body, auth = true } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (auth) {
    const token = getToken()
    if (token) headers.Authorization = `Bearer ${token}`
  }

  return requestJson(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
}

// multipart/form-data 요청 전용 헬퍼입니다 (오디오 업로드). Content-Type은 브라우저가
// boundary와 함께 자동으로 설정하도록 직접 지정하지 않습니다.
export async function apiFetchFormData(path, formData) {
  const headers = {}
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`

  return requestJson(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers,
    body: formData,
  })
}

async function parseResponse(response) {
  const text = await response.text()
  const data = text ? JSON.parse(text) : null

  if (!response.ok) {
    if (response.status === 401) {
      clearSession()
      window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT))
    }
    throw new ApiError(response.status, data?.code, data?.message)
  }

  return data
}
