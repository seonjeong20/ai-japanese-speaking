import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import LearnerLayout from '../../components/layout/LearnerLayout'
import SegmentedControl from '../../components/setup/SegmentedControl'
import SettingInput from '../../components/setup/SettingInput'
import SettingTextarea from '../../components/setup/SettingTextarea'
import SetupCta from '../../components/setup/SetupCta'
import SetupPageHeader from '../../components/setup/SetupPageHeader'
import { CharacterIcon, DescriptionIcon, PersonalityIcon, SituationIcon } from '../../components/icons/DashboardIcons'
import '../../components/setup/SetupForm.css'

const SITUATION_OPTIONS = ['카페에서', '여행 중', '쇼핑할 때', '학교에서', '직장에서', '일상 대화']
const PARTNER_OPTIONS = ['친구', '카페 직원', '직장 동료', '선배', '처음 만난 사람', '대학생']
const PERSONALITY_OPTIONS = ['친절하고 활발한', '차분한', '말이 많은', '조금 무뚝뚝한', '편안하고 친근한']
const DIFFICULTY_OPTIONS = ['초급', '중급', '고급']
const SUBTITLE_OPTIONS = ['OFF', '일본어', '일본어 + 한국어']

function ConversationSetupPage() {
  const navigate = useNavigate()
  const [conversationSettings, setConversationSettings] = useState({
    situation: '',
    partner: '',
    personality: '',
    description: '',
    difficulty: '중급',
    subtitleMode: '일본어',
  })

  const updateSetting = (key, value) => {
    setConversationSettings((prev) => ({ ...prev, [key]: value }))
  }

  const handleBack = () => navigate('/learning')

  // Speaking 화면이 구현되면 이 핸들러에서 conversationSettings를 전달하며 이동시킬 예정입니다.
  const handleStartConversation = () => {}

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
            options={SUBTITLE_OPTIONS}
            value={conversationSettings.subtitleMode}
            onChange={(value) => updateSetting('subtitleMode', value)}
          />
        </div>

        <SetupCta label="대화 시작하기 →" onClick={handleStartConversation} />
      </div>
    </LearnerLayout>
  )
}

export default ConversationSetupPage
