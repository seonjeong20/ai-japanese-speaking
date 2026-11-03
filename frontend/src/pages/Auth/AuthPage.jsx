import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import decorativeCurve from '../../assets/auth/decorative-curve.svg'
import { OAuthProvider } from '../../data/enums'
import { buildAuthorizeUrl, isProviderConfigured } from '../../lib/oauth'
import './AuthPage.css'

const NOT_CONFIGURED_MESSAGE = '로그인 설정이 아직 준비되지 않았습니다'

const PROVIDER_BUTTONS = [
  { provider: OAuthProvider.GOOGLE, label: '구글로 시작', className: 'auth-button--google' },
  { provider: OAuthProvider.KAKAO, label: '카카오로 시작', className: 'auth-button--kakao' },
]

// 첫 화면입니다. 가입과 로그인은 구글·카카오 소셜 로그인 하나로 합니다.
// 콜백 화면이 넘긴 안내(location.state.authNotice)가 있으면 버튼 아래에 보여 줍니다.
function AuthPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const [notice, setNotice] = useState(() => location.state?.authNotice ?? '')

  // 안내를 한 번 보여 준 뒤 history의 state를 비워, 새로고침했을 때 다시 나오지 않게 합니다.
  useEffect(() => {
    if (location.state?.authNotice) {
      navigate(location.pathname, { replace: true, state: null })
    }
  }, [location, navigate])

  const handleStart = (provider) => {
    if (!isProviderConfigured(provider)) {
      setNotice(NOT_CONFIGURED_MESSAGE)
      return
    }
    window.location.assign(buildAuthorizeUrl(provider))
  }

  return (
    <div className="auth-page">
      <div className="auth-container">
        <div className="form-panel">
          <div className="auth-form">
            <div className="auth-logo">
              <span className="auth-logo__mark">こ</span>
              <span className="auth-logo__text">Kotoba</span>
            </div>

            <h1 className="auth-form__title">일본어 스피킹, 지금 시작해요</h1>
            <p className="auth-form__subtitle">
              구글이나 카카오 계정으로 바로 시작할 수 있어요.
              <br />
              처음이면 가입되고, 가입했다면 로그인돼요.
            </p>

            <div className="auth-provider-buttons">
              {PROVIDER_BUTTONS.map(({ provider, label, className }) => (
                <button
                  key={provider}
                  type="button"
                  className={`auth-button auth-button--provider ${className}`}
                  onClick={() => handleStart(provider)}
                >
                  {label}
                </button>
              ))}
            </div>

            {notice && (
              <p className="auth-field__error" role="alert">
                {notice}
              </p>
            )}
          </div>
        </div>

        <div className="overlay-panel">
          <img className="overlay-panel__curve" src={decorativeCurve} alt="" aria-hidden="true" />
          <span className="auth-badge">AI SPEAKING TUTOR</span>
          <h2 className="overlay-panel__title">
            AI와 함께,
            <br />
            진짜 일본어
            <br />
            스피킹을 시작하세요
          </h2>
          <p className="overlay-panel__text">
            1:1 AI 튜터가 발음과 억양, 회화 감각까지
            <br />
            실시간으로 피드백해 드려요.
          </p>
        </div>
      </div>
    </div>
  )
}

export default AuthPage
