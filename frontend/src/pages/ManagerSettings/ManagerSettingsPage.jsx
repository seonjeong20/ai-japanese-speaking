import { useState } from 'react'
import ManagerLayout from '../../components/layout/ManagerLayout'
import { CheckSmallIcon } from '../../components/icons/DashboardIcons'
import { managerAccountMock } from '../../data/managerMock'
import '../../components/setup/SetupForm.css'
import '../../components/feedback/FeedbackPage.css'
import '../Settings/SettingsPage.css'
import './ManagerSettingsPage.css'

const INITIAL_PASSWORD_FORM = {
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
}

function ManagerSettingsPage() {
  const [passwordForm, setPasswordForm] = useState(INITIAL_PASSWORD_FORM)
  const [errorMessage, setErrorMessage] = useState('')
  const [showSuccess, setShowSuccess] = useState(false)

  const updateField = (key, value) => {
    setPasswordForm((prev) => ({ ...prev, [key]: value }))
    setErrorMessage('')
    setShowSuccess(false)
  }

  const handleSubmit = (event) => {
    event.preventDefault()

    const { currentPassword, newPassword, confirmPassword } = passwordForm

    if (!currentPassword || !newPassword || !confirmPassword) {
      setErrorMessage('모든 항목을 입력해 주세요.')
      setShowSuccess(false)
      return
    }

    if (newPassword !== confirmPassword) {
      setErrorMessage('새 비밀번호가 일치하지 않습니다.')
      setShowSuccess(false)
      return
    }

    // Backend가 아직 연결되지 않아 실제 비밀번호는 변경하지 않는 mock interaction입니다.
    setErrorMessage('')
    setShowSuccess(true)
    setPasswordForm(INITIAL_PASSWORD_FORM)
  }

  return (
    <ManagerLayout>
      <div className="setup-page">
        <div className="settings-page__header">
          <h1 className="settings-page__title">Settings</h1>
          <p className="settings-page__subtitle">계정 및 개인 설정을 관리하세요.</p>
        </div>

        <div className="setup-page__card">
          <p className="settings-section__title">계정 정보</p>
          <div className="settings-account">
            <div className="settings-account__row">
              <span className="settings-account__label">이름</span>
              <span className="settings-account__value">{managerAccountMock.name}</span>
            </div>
            <div className="settings-account__row">
              <span className="settings-account__label">이메일</span>
              <span className="settings-account__value">{managerAccountMock.email}</span>
            </div>
            <div className="settings-account__row">
              <span className="settings-account__label">소속 기관</span>
              <span className="settings-account__value">{managerAccountMock.organization}</span>
            </div>
            <div className="settings-account__row">
              <span className="settings-account__label">부서</span>
              <span className="settings-account__value">{managerAccountMock.department}</span>
            </div>
          </div>
        </div>

        <form className="setup-page__card" onSubmit={handleSubmit} noValidate>
          <p className="settings-section__title">비밀번호 변경</p>

          <div className="manager-settings-field">
            <label className="manager-settings-field__label" htmlFor="manager-current-password">
              현재 비밀번호
            </label>
            <input
              id="manager-current-password"
              type="password"
              className="setting-input"
              placeholder="현재 비밀번호를 입력하세요"
              autoComplete="current-password"
              value={passwordForm.currentPassword}
              onChange={(event) => updateField('currentPassword', event.target.value)}
            />
          </div>

          <div className="manager-settings-field">
            <label className="manager-settings-field__label" htmlFor="manager-new-password">
              새 비밀번호
            </label>
            <input
              id="manager-new-password"
              type="password"
              className="setting-input"
              placeholder="새 비밀번호를 입력하세요"
              autoComplete="new-password"
              value={passwordForm.newPassword}
              onChange={(event) => updateField('newPassword', event.target.value)}
            />
          </div>

          <div className="manager-settings-field">
            <label className="manager-settings-field__label" htmlFor="manager-confirm-password">
              새 비밀번호 확인
            </label>
            <input
              id="manager-confirm-password"
              type="password"
              className="setting-input"
              placeholder="새 비밀번호를 다시 입력하세요"
              autoComplete="new-password"
              value={passwordForm.confirmPassword}
              onChange={(event) => updateField('confirmPassword', event.target.value)}
            />
          </div>

          {errorMessage && (
            <p className="manager-settings-field__error" role="alert">
              {errorMessage}
            </p>
          )}

          <div className="settings-page__save-row">
            <button type="submit" className="setup-page__cta settings-page__save-button">
              비밀번호 변경
            </button>

            <p
              className={`feedback-header__badge settings-page__success${showSuccess ? ' settings-page__success--visible' : ''}`}
              role="status"
            >
              <CheckSmallIcon size={12} />
              비밀번호가 변경되었습니다.
            </p>
          </div>
        </form>
      </div>
    </ManagerLayout>
  )
}

export default ManagerSettingsPage
