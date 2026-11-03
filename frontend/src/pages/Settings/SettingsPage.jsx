import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getCurrentUser } from '../../api/client'
import { getMe } from '../../api/auth'
import { deleteMe, getPreferences, updatePreferences } from '../../api/users'
import LearnerLayout from '../../components/layout/LearnerLayout'
import SegmentedControl from '../../components/setup/SegmentedControl'
import { CheckSmallIcon } from '../../components/icons/DashboardIcons'
import { defaultSpeakingSettings } from '../../data/settingsMock'
import { DIFFICULTY_OPTIONS, OAuthProvider, OAuthProviderLabel, SUBTITLE_MODE_OPTIONS } from '../../data/enums'
import '../../components/setup/SetupForm.css'
import '../../components/feedback/FeedbackPage.css'
import './SettingsPage.css'

const SUCCESS_MESSAGE_MS = 2500
const SAVE_SUCCESS_MESSAGE = '설정이 저장되었습니다.'

// 가입 경로 문구입니다. 예: google → "구글로 가입"
function providerText(provider) {
  const label = OAuthProviderLabel[provider]
  return label ? `${label}로 가입` : '-'
}

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
  // 계정 정보(GET /api/auth/me). 읽기 전용입니다.
  const [account, setAccount] = useState(null)
  const [isAccountLoading, setAccountLoading] = useState(true)
  const [accountError, setAccountError] = useState('')

  // savedSettings: 서버에 저장된 값(GET·PATCH /api/users/me/preferences 응답).
  // draftSettings: 사용자가 현재 화면에서 고르고 있는 값.
  const [savedSettings, setSavedSettings] = useState(defaultSpeakingSettings)
  const [draftSettings, setDraftSettings] = useState(defaultSpeakingSettings)
  const [isPreferencesLoading, setPreferencesLoading] = useState(true)
  const [preferencesLoadError, setPreferencesLoadError] = useState('')
  const [isSaving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState('')
  const [showSuccess, setShowSuccess] = useState(false)
  const navigate = useNavigate()
  // 구글 안내 여부는 /api/auth/me 응답을 우선하고, 아직 없으면 로그인 때 저장한 사용자 정보로 정합니다.
  const isGoogleUser = (account?.provider ?? getCurrentUser()?.provider) === OAuthProvider.GOOGLE
  const [isDeleteOpen, setDeleteOpen] = useState(false)
  const [isDeleting, setDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState('')

  const isDirty =
    draftSettings.difficulty !== savedSettings.difficulty ||
    draftSettings.subtitleMode !== savedSettings.subtitleMode

  useEffect(() => {
    let ignore = false
    getMe()
      .then((user) => {
        if (!ignore) setAccount(user)
      })
      .catch((error) => {
        if (!ignore) setAccountError(error.message || '계정 정보를 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!ignore) setAccountLoading(false)
      })
    return () => {
      ignore = true
    }
  }, [])

  // 불러오는 동안에는 선택 칸을 보여 주지 않으므로, 늦게 온 응답이 사용자가 고른 값을 덮어쓰지 않습니다.
  useEffect(() => {
    let ignore = false
    getPreferences()
      .then((preferences) => {
        if (ignore) return
        const loaded = {
          difficulty: preferences?.defaultDifficulty ?? defaultSpeakingSettings.difficulty,
          subtitleMode: preferences?.defaultSubtitleMode ?? defaultSpeakingSettings.subtitleMode,
        }
        setSavedSettings(loaded)
        setDraftSettings(loaded)
      })
      .catch((error) => {
        if (ignore) return
        setPreferencesLoadError(
          `${error.message || '저장한 기본값을 불러오지 못했습니다.'} 지금은 기본값(중급, 일본어 + 한국어)을 보여 주고 있어요.`,
        )
      })
      .finally(() => {
        if (!ignore) setPreferencesLoading(false)
      })
    return () => {
      ignore = true
    }
  }, [])

  const updateDraft = (key, value) => {
    setDraftSettings((prev) => ({ ...prev, [key]: value }))
    setShowSuccess(false)
    setSaveError('')
  }

  const handleSave = async () => {
    if (isSaving) return
    setSaving(true)
    setSaveError('')
    setShowSuccess(false)
    try {
      const saved = await updatePreferences({
        defaultDifficulty: draftSettings.difficulty,
        defaultSubtitleMode: draftSettings.subtitleMode,
      })
      const next = {
        difficulty: saved?.defaultDifficulty ?? draftSettings.difficulty,
        subtitleMode: saved?.defaultSubtitleMode ?? draftSettings.subtitleMode,
      }
      setSavedSettings(next)
      setDraftSettings(next)
      setPreferencesLoadError('')
      setShowSuccess(true)
    } catch (error) {
      setSaveError(error.message || '설정을 저장하지 못했습니다. 다시 시도해 주세요.')
    } finally {
      setSaving(false)
    }
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
          {isAccountLoading ? (
            <p className="settings-page__loading" role="status">
              계정 정보를 불러오는 중...
            </p>
          ) : accountError ? (
            <p className="setup-page__error" role="alert">
              {accountError}
            </p>
          ) : (
            <div className="settings-account">
              <div className="settings-account__row">
                <span className="settings-account__label">이름</span>
                <span className="settings-account__value">{account?.name ?? '-'}</span>
              </div>
              <div className="settings-account__row">
                <span className="settings-account__label">이메일</span>
                <span className="settings-account__value">{account?.email ?? '-'}</span>
              </div>
              <div className="settings-account__row">
                <span className="settings-account__label">가입 경로</span>
                <span className="settings-account__value">{providerText(account?.provider)}</span>
              </div>
            </div>
          )}
        </div>

        <div className="setup-page__card">
          <p className="settings-section__title">Speaking 기본 설정</p>

          {isPreferencesLoading ? (
            <p className="settings-page__loading" role="status">
              기본 설정을 불러오는 중...
            </p>
          ) : (
            <>
              {preferencesLoadError && (
                <p className="setup-page__error" role="alert">
                  {preferencesLoadError}
                </p>
              )}

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
            </>
          )}
        </div>

        <div className="settings-page__save-row">
          <button
            type="button"
            className="setup-page__cta settings-page__save-button"
            onClick={handleSave}
            disabled={!isDirty || isSaving || isPreferencesLoading}
          >
            {isSaving ? '저장하는 중...' : '설정 저장'}
          </button>

          <p
            className={`feedback-header__badge settings-page__success${showSuccess ? ' settings-page__success--visible' : ''}`}
            role="status"
          >
            <CheckSmallIcon size={12} />
            {SAVE_SUCCESS_MESSAGE}
          </p>
        </div>

        {saveError && (
          <p className="setup-page__error" role="alert">
            {saveError}
          </p>
        )}

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
