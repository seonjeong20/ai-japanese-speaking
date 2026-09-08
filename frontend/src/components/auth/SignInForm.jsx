import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

function SignInForm({ onSwitchToSignUp }) {
  const [formData, setFormData] = useState({ email: '', password: '' })
  const navigate = useNavigate()

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = (event) => {
    event.preventDefault()
    // Backend 인증 연동 전까지는 UI 확인을 위해 바로 대시보드로 이동합니다.
    navigate('/dashboard')
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit} noValidate>
      <div className="auth-logo">
        <span className="auth-logo__mark">こ</span>
        <span className="auth-logo__text">Kotoba</span>
      </div>

      <h1 className="auth-form__title">다시 만나서 반가워요</h1>
      <p className="auth-form__subtitle">
        AI 튜터와 함께 오늘도 일본어 실력을 쌓아보세요
      </p>

      <div className="auth-field">
        <label className="auth-field__label" htmlFor="signin-email">
          이메일
        </label>
        <input
          id="signin-email"
          name="email"
          type="email"
          className="auth-field__input"
          placeholder="you@example.com"
          autoComplete="email"
          value={formData.email}
          onChange={handleChange}
          required
        />
      </div>

      <div className="auth-field">
        <label className="auth-field__label" htmlFor="signin-password">
          비밀번호
        </label>
        <input
          id="signin-password"
          name="password"
          type="password"
          className="auth-field__input"
          placeholder="••••••••"
          autoComplete="current-password"
          value={formData.password}
          onChange={handleChange}
          required
        />
      </div>

      <div className="auth-form__forgot-row">
        <button type="button" className="auth-link">
          비밀번호를 잊으셨나요?
        </button>
      </div>

      <button type="submit" className="auth-button auth-button--primary">
        로그인
      </button>

      <p className="auth-form__switch">
        계정이 없으신가요?{' '}
        <button
          type="button"
          className="auth-link auth-link--strong"
          onClick={onSwitchToSignUp}
        >
          회원가입
        </button>
      </p>
    </form>
  )
}

export default SignInForm
