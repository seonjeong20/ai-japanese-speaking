import { useState } from 'react'
import decorativeCurve from '../../assets/auth/decorative-curve.svg'
import SignInForm from '../../components/auth/SignInForm'
import SignUpForm from '../../components/auth/SignUpForm'
import './AuthPage.css'

function AuthPage() {
  const [isSignUp, setIsSignUp] = useState(false)

  return (
    <div className="auth-page">
      <div className={`auth-container${isSignUp ? ' auth-container--sign-up' : ''}`}>
        <div className="form-panel form-panel--sign-in">
          <SignInForm onSwitchToSignUp={() => setIsSignUp(true)} />
        </div>

        <div className="form-panel form-panel--sign-up">
          <SignUpForm onSwitchToSignIn={() => setIsSignUp(false)} />
        </div>

        <div className="overlay-container">
          <div className="overlay">
            <div className="overlay-panel overlay-panel--left">
              <img className="overlay-panel__curve" src={decorativeCurve} alt="" aria-hidden="true" />
              <span className="auth-badge">AI SPEAKING TUTOR</span>
              <h2 className="overlay-panel__title">
                다시 오신 것을
                <br />
                환영해요
              </h2>
              <p className="overlay-panel__text">
                이미 Kotoba 계정이 있다면
                <br />
                바로 로그인하고 학습을 이어가세요.
              </p>
              <p className="overlay-panel__prompt">이미 계정이 있으신가요?</p>
              <button
                type="button"
                className="auth-button auth-button--cta"
                onClick={() => setIsSignUp(false)}
              >
                로그인
              </button>
            </div>

            <div className="overlay-panel overlay-panel--right">
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
              <p className="overlay-panel__prompt">아직 계정이 없으신가요?</p>
              <button
                type="button"
                className="auth-button auth-button--cta"
                onClick={() => setIsSignUp(true)}
              >
                회원가입
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default AuthPage
