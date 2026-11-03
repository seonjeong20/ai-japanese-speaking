import { useEffect, useRef } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { oauthLogin } from '../../api/auth'
import { consumeState, getRedirectUri, isSupportedProvider } from '../../lib/oauth'
import '../Auth/AuthPage.css'

const CANCELED_MESSAGE = '로그인이 취소되었습니다'
const FAILED_MESSAGE = '로그인에 실패했습니다. 다시 시도해 주세요'
const EMAIL_NOT_VERIFIED_MESSAGE = '가입할 수 없습니다. 인증된 이메일이 필요합니다'

function messageForError(error) {
  // 409(EMAIL_ALREADY_REGISTERED)는 서버 message가 "이미 카카오로 가입된 이메일입니다. 카카오로 로그인해 주세요"
  // 처럼 registeredProvider가 들어간 완성된 안내 문구라 그대로 보여 줍니다.
  if (error?.status === 409) return error.message || FAILED_MESSAGE
  if (error?.status === 403) return EMAIL_NOT_VERIFIED_MESSAGE
  return FAILED_MESSAGE
}

// 구글·카카오 로그인 뒤 돌아오는 화면(/auth/callback/:provider)입니다.
// 받은 인가 코드를 백엔드로 넘기고, 성공하면 대시보드로, 실패하면 첫 화면으로 보냅니다.
// 첫 화면에 보여 줄 안내는 navigate의 state(authNotice)로 넘깁니다.
function AuthCallbackPage() {
  const { provider } = useParams()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  // StrictMode(개발 모드)에서 effect가 두 번 실행되어도 인가 코드를 한 번만 보내도록 막습니다.
  // 인가 코드는 한 번만 쓸 수 있어서, 두 번 보내면 두 번째 요청이 실패합니다.
  const startedRef = useRef(false)

  useEffect(() => {
    if (startedRef.current) return
    startedRef.current = true

    const goHomeWithNotice = (notice) => navigate('/', { replace: true, state: { authNotice: notice } })

    const error = searchParams.get('error')
    const code = searchParams.get('code')
    const stateMatches = consumeState(searchParams.get('state'))

    if (error) {
      goHomeWithNotice(CANCELED_MESSAGE)
      return
    }
    if (!isSupportedProvider(provider) || !stateMatches || !code) {
      goHomeWithNotice(FAILED_MESSAGE)
      return
    }

    oauthLogin(provider, code, getRedirectUri(provider))
      .then(() => navigate('/dashboard', { replace: true }))
      .catch((loginError) => goHomeWithNotice(messageForError(loginError)))
  }, [provider, searchParams, navigate])

  return (
    <div className="auth-page">
      <p className="auth-callback__status" role="status">
        로그인 중...
      </p>
    </div>
  )
}

export default AuthCallbackPage
