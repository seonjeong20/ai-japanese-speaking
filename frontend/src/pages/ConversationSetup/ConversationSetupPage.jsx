import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import LearnerLayout from '../../components/layout/LearnerLayout'
import SegmentedControl from '../../components/setup/SegmentedControl'
import SettingInput from '../../components/setup/SettingInput'
import SettingTextarea from '../../components/setup/SettingTextarea'
import SetupCta from '../../components/setup/SetupCta'
import SetupPageHeader from '../../components/setup/SetupPageHeader'
import { CharacterIcon, DescriptionIcon, PersonalityIcon, SituationIcon } from '../../components/icons/DashboardIcons'
import { DIFFICULTY_OPTIONS, SUBTITLE_MODE_OPTIONS } from '../../data/enums'
import { defaultSpeakingSettings } from '../../data/settingsMock'
import { startConversation } from '../../api/conversations'
import { getPreferences } from '../../api/users'
import '../../components/setup/SetupForm.css'

const SITUATION_OPTIONS = ['카페에서', '여행 중', '쇼핑할 때', '학교에서', '직장에서', '일상 대화']
const PARTNER_OPTIONS = ['친구', '카페 직원', '직장 동료', '선배', '처음 만난 사람', '대학생']
const PERSONALITY_OPTIONS = ['친절하고 활발한', '차분한', '말이 많은', '조금 무뚝뚝한', '편안하고 친근한']

function ConversationSetupPage() {
  const navigate = useNavigate()
  // 난이도/자막은 Settings에 저장한 기본값을 이번 회화의 기본값으로 불러옵니다(아래 useEffect).
  // 아래에서 세션 한정으로 다른 값을 선택해도 저장된 기본 설정에는 영향을 주지 않습니다.
  const [conversationSettings, setConversationSettings] = useState({
    situation: '',
    partner: '',
    personality: '',
    description: '',
    difficulty: defaultSpeakingSettings.difficulty,
    subtitleMode: defaultSpeakingSettings.subtitleMode,
  })
  const [isStarting, setStarting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  // 화면을 연 뒤 사용자가 직접 고른 항목입니다. 기본값 응답이 늦게 와도 이 항목은 덮어쓰지 않습니다.
  const touchedRef = useRef(new Set())
  // 기본값 조회가 끝날 때까지(성공이든 실패든) [시작]을 막습니다.
  const [isPreferencesLoading, setPreferencesLoading] = useState(true)

  // 저장한 기본값(GET /api/users/me/preferences)으로 난이도·자막을 미리 채웁니다.
  // 불러오지 못하면 defaultSpeakingSettings(INTERMEDIATE, JAPANESE_KOREAN)를 그대로 씁니다.
  useEffect(() => {
    let ignore = false
    getPreferences()
      .then((preferences) => {
        if (ignore) return
        setConversationSettings((prev) => ({
          ...prev,
          difficulty: touchedRef.current.has('difficulty')
            ? prev.difficulty
            : preferences?.defaultDifficulty ?? prev.difficulty,
          subtitleMode: touchedRef.current.has('subtitleMode')
            ? prev.subtitleMode
            : preferences?.defaultSubtitleMode ?? prev.subtitleMode,
        }))
      })
      .catch(() => {
        // 기본값을 쓰고 화면은 그대로 사용할 수 있게 둡니다.
      })
      .finally(() => {
        if (!ignore) setPreferencesLoading(false)
      })
    return () => {
      ignore = true
    }
  }, [])

  const updateSetting = (key, value) => {
    touchedRef.current.add(key)
    setConversationSettings((prev) => ({ ...prev, [key]: value }))
  }

  const handleBack = () => navigate('/learning')

  const handleStartConversation = async () => {
    if (isStarting || isPreferencesLoading) return
    setStarting(true)
    setErrorMessage('')
    try {
      const session = await startConversation(conversationSettings)
      navigate('/conversation/speaking', {
        state: { ...conversationSettings, sessionId: session.sessionId },
      })
    } catch (error) {
      setErrorMessage(error.message || '회화 세션을 시작하지 못했습니다.')
      setStarting(false)
    }
  }

  return (
    <LearnerLayout>
      <div className="setup-page">
        <SetupPageHeader
          title="일반 회화 설정"
          subtitle="원하는 상황과 대화 상대를 설정하고 일본어 회화를 시작해보세요."
          onBack={handleBack}
        />

        <div className="setup-page__card">
          <div className="setup-page__grid">
            <SettingInput
              id="situation"
              label="학습 상황"
              icon={SituationIcon}
              placeholder="예) 카페, 여행, 일상, 쇼핑"
              value={conversationSettings.situation}
              onChange={(value) => updateSetting('situation', value)}
              options={SITUATION_OPTIONS}
            />
            <SettingInput
              id="partner"
              label="상대방 설정"
              icon={CharacterIcon}
              placeholder="예) 친구, 처음 만난 사람"
              value={conversationSettings.partner}
              onChange={(value) => updateSetting('partner', value)}
              options={PARTNER_OPTIONS}
            />
            <SettingInput
              id="personality"
              label="상대방 성격"
              icon={PersonalityIcon}
              placeholder="예) 친절하고 밝은, 차분한"
              value={conversationSettings.personality}
              onChange={(value) => updateSetting('personality', value)}
              options={PERSONALITY_OPTIONS}
            />
          </div>

          <SettingTextarea
            label="상황 설명 (선택)"
            icon={DescriptionIcon}
            placeholder="오랜만에 만난 친구와 근황을 이야기합니다."
            value={conversationSettings.description}
            onChange={(value) => updateSetting('description', value)}
          />

          <div className="setup-page__divider" />

          <SegmentedControl
            label="난이도"
            options={DIFFICULTY_OPTIONS}
            value={conversationSettings.difficulty}
            onChange={(value) => updateSetting('difficulty', value)}
          />

          <SegmentedControl
            label="자막"
            options={SUBTITLE_MODE_OPTIONS}
            value={conversationSettings.subtitleMode}
            onChange={(value) => updateSetting('subtitleMode', value)}
          />
        </div>

        {errorMessage && (
          <p className="setup-page__error" role="alert">
            {errorMessage}
          </p>
        )}

        <SetupCta
          label={isPreferencesLoading ? '기본 설정을 불러오는 중...' : isStarting ? '시작하는 중...' : '대화 시작하기 →'}
          onClick={handleStartConversation}
          disabled={isStarting || isPreferencesLoading}
        />
      </div>
    </LearnerLayout>
  )
}

export default ConversationSetupPage
