import { useState } from 'react'
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
import '../../components/setup/SetupForm.css'

const SITUATION_OPTIONS = ['카페에서', '여행 중', '쇼핑할 때', '학교에서', '직장에서', '일상 대화']
const PARTNER_OPTIONS = ['친구', '카페 직원', '직장 동료', '선배', '처음 만난 사람', '대학생']
const PERSONALITY_OPTIONS = ['친절하고 활발한', '차분한', '말이 많은', '조금 무뚝뚝한', '편안하고 친근한']

function ConversationSetupPage() {
  const navigate = useNavigate()
  // 난이도/자막은 Settings > Speaking 기본 설정 값을 이번 회화의 기본값으로 불러옵니다.
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

  const updateSetting = (key, value) => {
    setConversationSettings((prev) => ({ ...prev, [key]: value }))
  }

  const handleBack = () => navigate('/learning')

  const handleStartConversation = async () => {
    if (isStarting) return
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
          label={isStarting ? '시작하는 중...' : '대화 시작하기 →'}
          onClick={handleStartConversation}
          disabled={isStarting}
        />
      </div>
    </LearnerLayout>
  )
}

export default ConversationSetupPage
