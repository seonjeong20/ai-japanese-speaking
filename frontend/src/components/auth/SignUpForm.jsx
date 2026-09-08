import { useState } from 'react'

function SignUpForm({ onSwitchToSignIn }) {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
  })

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = (event) => {
    event.preventDefault()
  }

  return (
    <form className="auth-form auth-form--compact" onSubmit={handleSubmit} noValidate>
      <div className="auth-logo">
        <span className="auth-logo__mark">こ</span>
        <span className="auth-logo__text">Kotoba</span>
      </div>

      <h1 className="auth-form__title auth-form__title--sm">지금 바로 시작해보세요</h1>
      <p className="auth-form__subtitle">
        몇 가지 정보만 입력하면 AI 튜터와의 첫 수업이 시작돼요
      </p>

      <div className="auth-field">
        <label className="auth-field__label" htmlFor="signup-name">
          이름
        </label>
        <input
          id="signup-name"
          name="name"
          type="text"
          className="auth-field__input"
          placeholder="홍길동"
          autoComplete="name"
          value={formData.name}
          onChange={handleChange}
          required
        />
      </div>

      <div className="auth-field">
        <label className="auth-field__label" htmlFor="signup-email">
          이메일
        </label>
        <input
          id="signup-email"
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
        <label className="auth-field__label" htmlFor="signup-password">
          비밀번호
        </label>
        <input
          id="signup-password"
          name="password"
          type="password"
          className="auth-field__input"
          placeholder="••••••••"
          autoComplete="new-password"
          value={formData.password}
          onChange={handleChange}
          required
        />
      </div>

      <div className="auth-field">
        <label className="auth-field__label" htmlFor="signup-confirm-password">
          비밀번호 확인
        </label>
        <input
          id="signup-confirm-password"
          name="confirmPassword"
          type="password"
          className="auth-field__input"
          placeholder="••••••••"
          autoComplete="new-password"
          value={formData.confirmPassword}
          onChange={handleChange}
          required
        />
      </div>

      <button type="submit" className="auth-button auth-button--primary">
        회원가입
      </button>

      <p className="auth-form__switch">
        이미 계정이 있으신가요?{' '}
        <button
          type="button"
          className="auth-link auth-link--strong"
          onClick={onSwitchToSignIn}
        >
          로그인
        </button>
      </p>
    </form>
  )
}

export default SignUpForm
