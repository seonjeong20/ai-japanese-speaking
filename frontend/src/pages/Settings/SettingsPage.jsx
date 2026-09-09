import { useEffect, useState } from 'react'
import LearnerLayout from '../../components/layout/LearnerLayout'
import SegmentedControl from '../../components/setup/SegmentedControl'
import { CheckSmallIcon } from '../../components/icons/DashboardIcons'
import { accountMock, defaultSpeakingSettings } from '../../data/settingsMock'
import { DIFFICULTY_OPTIONS, SUBTITLE_MODE_OPTIONS } from '../../data/enums'
import '../../components/setup/SetupForm.css'
import '../../components/feedback/FeedbackPage.css'
import './SettingsPage.css'

const SUCCESS_MESSAGE_MS = 2500

function SettingsPage() {
  // savedSettings: 마지막으로 "저장"된 값(Backend가 없으므로 frontend에서만 유지).
  // draftSettings: 사용자가 현재 화면에서 고르고 있는 값.
  const [savedSettings, setSavedSettings] = useState(defaultSpeakingSettings)
  const [draftSettings, setDraftSettings] = useState(defaultSpeakingSettings)
  const [showSuccess, setShowSuccess] = useState(false)

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
            <div className="settings-account__row">
              <span className="settings-account__label">소속 기관</span>
              <span className="settings-account__value">{accountMock.organization}</span>
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
      </div>
    </LearnerLayout>
  )
}

export default SettingsPage
