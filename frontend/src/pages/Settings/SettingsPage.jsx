import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getCurrentUser } from '../../api/client'
import { deleteMe } from '../../api/users'
import LearnerLayout from '../../components/layout/LearnerLayout'
import SegmentedControl from '../../components/setup/SegmentedControl'
import { CheckSmallIcon } from '../../components/icons/DashboardIcons'
import { accountMock, defaultSpeakingSettings } from '../../data/settingsMock'
import { DIFFICULTY_OPTIONS, OAuthProvider, SUBTITLE_MODE_OPTIONS } from '../../data/enums'
import '../../components/setup/SetupForm.css'
import '../../components/feedback/FeedbackPage.css'
import './SettingsPage.css'

const SUCCESS_MESSAGE_MS = 2500

const DELETED_ITEMS = ['계정', '일반 회화 기록', '면접 질문·답변·평가', '피드백', '설정']

// 회원 탈퇴 확인 창입니다(window.confirm 대신 화면 안에서 확인합니다).
function AccountDeleteModal({ open, isGoogleUser, isDeleting, errorMessage, onCancel, onConfirm }) {
  useEffect(() => {
    if (!open) return undefined

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') onCancel()
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [open, onCancel])

  if (!open) return null

  return (
    <div className="settings-modal__backdrop" role="presentation" onClick={onCancel}>
      <div
        className="settings-modal"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="account-delete-title"
        aria-describedby="account-delete-description"
        onClick={(event) => event.stopPropagation()}
      >
        <p id="account-delete-title" className="settings-modal__title">
          정말 탈퇴하시겠어요?
        </p>

        <div id="account-delete-description" className="settings-modal__body">
          <p className="settings-modal__text">탈퇴하면 아래 내용이 모두 삭제되고 되돌릴 수 없어요.</p>
          <ul className="settings-modal__list">
            {DELETED_ITEMS.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
          {isGoogleUser && (
            <p className="settings-modal__notice">
              구글 계정 설정의 &apos;서드 파티 앱 및 서비스&apos;에서 연결을 직접 끊을 수 있어요.
            </p>
          )}
        </div>

        {errorMessage && (
          <p className="settings-modal__error" role="alert">
            {errorMessage}
          </p>
        )}

        <div className="settings-modal__actions">
          <button
            type="button"
            className="settings-modal__button settings-modal__button--ghost"
            onClick={onCancel}
            disabled={isDeleting}
          >
            취소
          </button>
          <button
            type="button"
            className="settings-modal__button settings-modal__button--danger"
            onClick={onConfirm}
            disabled={isDeleting}
          >
            {isDeleting ? '탈퇴하는 중...' : '탈퇴하기'}
          </button>
        </div>
      </div>
    </div>
  )
}

function SettingsPage() {
  // savedSettings: 마지막으로 "저장"된 값(Backend가 없으므로 frontend에서만 유지).
  // draftSettings: 사용자가 현재 화면에서 고르고 있는 값.
  const [savedSettings, setSavedSettings] = useState(defaultSpeakingSettings)
  const [draftSettings, setDraftSettings] = useState(defaultSpeakingSettings)
  const [showSuccess, setShowSuccess] = useState(false)
  const navigate = useNavigate()
  const isGoogleUser = getCurrentUser()?.provider === OAuthProvider.GOOGLE
  const [isDeleteOpen, setDeleteOpen] = useState(false)
  const [isDeleting, setDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState('')

  const isDirty =
    draftSettings.difficulty !== savedSettings.difficulty ||
    draftSettings.subtitleMode !== savedSettings.subtitleMode

  const updateDraft = (key, value) => {
    setDraftSettings((prev) => ({ ...prev, [key]: value }))
    setShowSuccess(false)
  }

  const handleSave = () => {
    setSavedSettings(draftSettings)
    setShowSuccess(true)
  }

  const openDeleteModal = () => {
    setDeleteError('')
    setDeleteOpen(true)
  }

  // [취소]는 창만 닫고 아무것도 지우지 않습니다.
  const closeDeleteModal = () => {
    if (isDeleting) return
    setDeleteOpen(false)
  }

  // 탈퇴에 성공하면 deleteMe가 브라우저의 토큰·사용자 정보를 지우고, 첫 화면으로 갑니다.
  const handleDeleteAccount = async () => {
    if (isDeleting) return
    setDeleting(true)
    setDeleteError('')
    try {
      await deleteMe()
      navigate('/', { replace: true })
    } catch (error) {
      setDeleteError(error.message || '회원 탈퇴에 실패했습니다. 다시 시도해 주세요.')
      setDeleting(false)
    }
  }

  useEffect(() => {
    if (!showSuccess) return undefined
    const timeoutId = setTimeout(() => setShowSuccess(false), SUCCESS_MESSAGE_MS)
    return () => clearTimeout(timeoutId)
  }, [showSuccess])

  return (
    <LearnerLayout>
      <div className="setup-page">
        <div className="settings-page__header">
          <h1 className="settings-page__title">Settings</h1>
          <p className="settings-page__subtitle">계정 정보와 Speaking 학습 기본 설정을 확인하고 변경할 수 있어요.</p>
        </div>

        <div className="setup-page__card">
          <p className="settings-section__title">계정 정보</p>
          <div className="settings-account">
            <div className="settings-account__row">
              <span className="settings-account__label">이름</span>
              <span className="settings-account__value">{accountMock.name}</span>
            </div>
            <div className="settings-account__row">
              <span className="settings-account__label">이메일</span>
              <span className="settings-account__value">{accountMock.email}</span>
            </div>
          </div>
        </div>

        <div className="setup-page__card">
          <p className="settings-section__title">Speaking 기본 설정</p>

          <SegmentedControl
            label="기본 난이도"
            options={DIFFICULTY_OPTIONS}
            value={draftSettings.difficulty}
            onChange={(value) => updateDraft('difficulty', value)}
          />

          <SegmentedControl
            label="기본 자막"
            options={SUBTITLE_MODE_OPTIONS}
            value={draftSettings.subtitleMode}
            onChange={(value) => updateDraft('subtitleMode', value)}
          />
        </div>

        <div className="settings-page__save-row">
          <button
            type="button"
            className="setup-page__cta settings-page__save-button"
            onClick={handleSave}
            disabled={!isDirty}
          >
            설정 저장
          </button>

          <p
            className={`feedback-header__badge settings-page__success${showSuccess ? ' settings-page__success--visible' : ''}`}
            role="status"
          >
            <CheckSmallIcon size={12} />
            설정이 저장되었습니다.
          </p>
        </div>

        <div className="setup-page__card settings-danger">
          <p className="settings-section__title">회원 탈퇴</p>
          <p className="settings-danger__text">
            탈퇴하면 계정과 학습 기록이 모두 삭제되고 되돌릴 수 없어요.
          </p>
          <button type="button" className="settings-danger__button" onClick={openDeleteModal}>
            회원 탈퇴
          </button>
        </div>
      </div>

      <AccountDeleteModal
        open={isDeleteOpen}
        isGoogleUser={isGoogleUser}
        isDeleting={isDeleting}
        errorMessage={deleteError}
        onCancel={closeDeleteModal}
        onConfirm={handleDeleteAccount}
      />
    </LearnerLayout>
  )
}

export default SettingsPage
