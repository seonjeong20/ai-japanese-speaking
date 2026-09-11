import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { login } from '../../api/auth'
import { Role } from '../../data/enums'

const ROLE_HOME_PATH = {
  [Role.LEARNER]: '/dashboard',
  [Role.MANAGER]: '/manager/dashboard',
  [Role.ADMIN]: '/admin/dashboard',
}

function SignInForm({ onSwitchToSignUp }) {
  const [formData, setFormData] = useState({ email: '', password: '' })
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitting, setSubmitting] = useState(false)
  const navigate = useNavigate()

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
    setErrorMessage('')
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    if (isSubmitting) return

    setSubmitting(true)
    setErrorMessage('')
    try {
      const user = await login(formData.email, formData.password)
      navigate(ROLE_HOME_PATH[user.role] || '/dashboard')
    } catch (error) {
      setErrorMessage(error.message || '로그인에 실패했습니다.')
    } finally {
      setSubmitting(false)
    }
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

      {errorMessage && (
        <p className="auth-field__error" role="alert">
          {errorMessage}
        </p>
      )}

      <button type="submit" className="auth-button auth-button--primary" disabled={isSubmitting}>
        {isSubmitting ? '로그인 중...' : '로그인'}
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
