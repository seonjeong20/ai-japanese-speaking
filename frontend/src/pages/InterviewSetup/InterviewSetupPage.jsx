import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import LearnerLayout from '../../components/layout/LearnerLayout'
import SegmentedControl from '../../components/setup/SegmentedControl'
import SettingInput from '../../components/setup/SettingInput'
import SettingTextarea from '../../components/setup/SettingTextarea'
import SetupCta from '../../components/setup/SetupCta'
import SetupPageHeader from '../../components/setup/SetupPageHeader'
import { DescriptionIcon, JobIcon } from '../../components/icons/DashboardIcons'
import { DIFFICULTY_OPTIONS, SUBTITLE_MODE_OPTIONS } from '../../data/enums'
import { defaultSpeakingSettings } from '../../data/settingsMock'
import { startInterview } from '../../api/interviews'
import '../../components/setup/SetupForm.css'

const JOB_OPTIONS = ['Backend Developer', 'Frontend Developer', 'AI Engineer', 'Data Engineer', 'Mobile Developer']
const INTERVIEW_TYPE_OPTIONS = ['일반 면접', '기술 면접', '인성 면접'].map((label) => ({ value: label, label }))

function InterviewSetupPage() {
  const navigate = useNavigate()
  // 난이도/자막은 Settings > Speaking 기본 설정 값을 이번 면접의 기본값으로 불러옵니다.
  const [interviewSettings, setInterviewSettings] = useState({
    job: '',
    interviewType: '',
    difficulty: defaultSpeakingSettings.difficulty,
    additionalRequest: '',
    subtitleMode: defaultSpeakingSettings.subtitleMode,
  })
  const [isStarting, setStarting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  const updateSetting = (key, value) => {
    setInterviewSettings((prev) => ({ ...prev, [key]: value }))
  }

  const handleBack = () => navigate('/learning')

  const handleStartInterview = async () => {
    if (isStarting) return
    if (!interviewSettings.job.trim()) {
      setErrorMessage('지원 직무를 입력해주세요.')
      return
    }

    setStarting(true)
    setErrorMessage('')
    try {
      const result = await startInterview(interviewSettings)
      navigate('/interview/speaking', {
        state: { ...interviewSettings, sessionId: result.sessionId, firstQuestion: result.firstQuestion },
      })
    } catch (error) {
      setErrorMessage(error.message || '면접 세션을 시작하지 못했습니다.')
      setStarting(false)
    }
  }

  return (
    <LearnerLayout>
      <div className="setup-page">
        <SetupPageHeader
          title="면접 회화 설정"
          subtitle="연습하고 싶은 일본어 면접 조건을 설정해주세요."
          onBack={handleBack}
        />

        <div className="setup-page__card">
          <SettingInput
            id="job"
            label="지원 직무"
            icon={JobIcon}
            placeholder="예) Backend Developer"
            value={interviewSettings.job}
            onChange={(value) => updateSetting('job', value)}
            options={JOB_OPTIONS}
          />

          <div className="setup-page__grid">
            <SegmentedControl
              label="난이도"
              options={DIFFICULTY_OPTIONS}
              value={interviewSettings.difficulty}
              onChange={(value) => updateSetting('difficulty', value)}
            />

            <SegmentedControl
              label="면접 유형"
              options={INTERVIEW_TYPE_OPTIONS}
              value={interviewSettings.interviewType}
              onChange={(value) => updateSetting('interviewType', value)}
            />
          </div>

          <SettingTextarea
            label="추가 요청 사항 (선택)"
            icon={DescriptionIcon}
            placeholder="지원 동기와 프로젝트 경험을 중심으로 질문해주세요."
            value={interviewSettings.additionalRequest}
            onChange={(value) => updateSetting('additionalRequest', value)}
          />

          <SegmentedControl
            label="자막"
            options={SUBTITLE_MODE_OPTIONS}
            value={interviewSettings.subtitleMode}
            onChange={(value) => updateSetting('subtitleMode', value)}
          />
        </div>

        {errorMessage && (
          <p className="setup-page__error" role="alert">
            {errorMessage}
          </p>
        )}

        <SetupCta
          label={isStarting ? '시작하는 중...' : '면접 시작하기 →'}
          onClick={handleStartInterview}
          disabled={isStarting}
        />
      </div>
    </LearnerLayout>
  )
}

export default InterviewSetupPage
