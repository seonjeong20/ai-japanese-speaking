import { useState } from 'react'
import { SignUpAccountType } from '../../data/enums'
import { fetchSignupOrganizations } from '../../data/organizationsMock'
import { CheckIcon } from '../icons/DashboardIcons'

const ACCOUNT_TYPE_OPTIONS = [
  { value: SignUpAccountType.LEARNER, label: '일반 학습자' },
  { value: SignUpAccountType.MANAGER, label: '기업/기관 담당자' },
]

const INITIAL_FORM = {
  name: '',
  email: '',
  password: '',
  confirmPassword: '',
  organizationId: '',
  department: '',
}

// 회원가입 결과는 항상 PENDING입니다. 완료 화면에서 누가 승인하는지 안내 문구만 계정 유형별로 다릅니다.
const APPROVAL_NOTICE = {
  [SignUpAccountType.LEARNER]: '소속 기관 담당자의 승인 후 서비스를 이용할 수 있습니다.',
  [SignUpAccountType.MANAGER]: '시스템 관리자의 승인 후 서비스를 이용할 수 있습니다.',
}

function SignUpForm({ onSwitchToSignIn }) {
  const organizations = fetchSignupOrganizations()
  const [accountType, setAccountType] = useState(SignUpAccountType.LEARNER)
  const [formData, setFormData] = useState(INITIAL_FORM)
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitted, setSubmitted] = useState(false)

  const isManager = accountType === SignUpAccountType.MANAGER

  const handleSelectAccountType = (value) => {
    setAccountType(value)
    setErrorMessage('')
  }

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
    setErrorMessage('')
  }

  const handleSubmit = (event) => {
    event.preventDefault()

    const { name, email, password, confirmPassword, organizationId } = formData

    if (!name.trim() || !email.trim() || !password || !confirmPassword || !organizationId) {
      setErrorMessage('필수 항목을 모두 입력해 주세요.')
      return
    }

    if (password !== confirmPassword) {
      setErrorMessage('비밀번호가 일치하지 않습니다.')
      return
    }

    // Backend가 아직 없어 실제 가입 요청은 보내지 않습니다. 가입 결과는 항상 PENDING입니다.
    setErrorMessage('')
    setSubmitted(true)
  }

  const handleSwitchToSignIn = () => {
    setSubmitted(false)
    setFormData(INITIAL_FORM)
    setAccountType(SignUpAccountType.LEARNER)
    onSwitchToSignIn()
  }

  if (isSubmitted) {
    return (
      <div className="auth-form auth-form--compact auth-form--complete">
        <div className="auth-logo">
          <span className="auth-logo__mark">こ</span>
          <span className="auth-logo__text">Kotoba</span>
        </div>

        <div className="auth-complete__icon">
          <CheckIcon size={26} />
        </div>

        <h1 className="auth-form__title auth-form__title--sm">가입 신청이 완료되었습니다.</h1>
        <p className="auth-form__subtitle">{APPROVAL_NOTICE[accountType]}</p>

        <button type="button" className="auth-button auth-button--primary" onClick={handleSwitchToSignIn}>
          로그인 화면으로
        </button>
      </div>
    )
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

      <div className="auth-type-toggle" role="radiogroup" aria-label="계정 유형">
        {ACCOUNT_TYPE_OPTIONS.map((option) => {
          const isSelected = option.value === accountType
          return (
            <button
              key={option.value}
              type="button"
              role="radio"
              aria-checked={isSelected}
              className={`auth-type-toggle__chip${isSelected ? ' auth-type-toggle__chip--selected' : ''}`}
              onClick={() => handleSelectAccountType(option.value)}
            >
              {option.label}
            </button>
          )
        })}
      </div>

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
          {isManager ? '업무용 이메일' : '이메일'}
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

      <div className="auth-field">
        <label className="auth-field__label" htmlFor="signup-organization">
          소속 기관
        </label>
        <select
          id="signup-organization"
          name="organizationId"
          className="auth-field__input"
          value={formData.organizationId}
          onChange={handleChange}
          required
        >
          <option value="" disabled>
            소속 기관을 선택해주세요
          </option>
          {organizations.map((organization) => (
            <option key={organization.id} value={organization.id}>
              {organization.name}
            </option>
          ))}
        </select>
      </div>

      {isManager && (
        <div className="auth-field">
          <label className="auth-field__label" htmlFor="signup-department">
            부서 (선택)
          </label>
          <input
            id="signup-department"
            name="department"
            type="text"
            className="auth-field__input"
            placeholder="예) 교육운영팀"
            autoComplete="off"
            value={formData.department}
            onChange={handleChange}
          />
        </div>
      )}

      {errorMessage && (
        <p className="auth-field__error" role="alert">
          {errorMessage}
        </p>
      )}

      <p className="auth-form__notice">{APPROVAL_NOTICE[accountType]}</p>

      <button type="submit" className="auth-button auth-button--primary">
        가입 신청
      </button>

      <p className="auth-form__switch">
        이미 계정이 있으신가요?{' '}
        <button
          type="button"
          className="auth-link auth-link--strong"
          onClick={handleSwitchToSignIn}
        >
          로그인
        </button>
      </p>
    </form>
  )
}

export default SignUpForm
