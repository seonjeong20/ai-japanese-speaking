import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import LearnerLayout from '../../components/layout/LearnerLayout'
import VoiceOrb from '../../components/speaking/VoiceOrb'
import SessionEndModal from '../../components/speaking/SessionEndModal'
import { useSpeakingStatus } from '../../components/speaking/useSpeakingStatus'
import { useElapsedTimer } from '../../components/speaking/useElapsedTimer'
import { BackArrowIcon, CheckIcon, JobIcon } from '../../components/icons/DashboardIcons'
import '../../components/setup/SetupForm.css'
import '../../components/speaking/SpeakingSession.css'
import './InterviewSpeakingPage.css'

// URL로 직접 접근하는 등 Setup에서 전달된 state가 없을 때 사용하는 기본값입니다.
const DEFAULT_SETTINGS = {
  job: 'Backend Developer',
  interviewType: '기술 면접',
  difficulty: '중급',
  additionalRequest: '',
  subtitleMode: '일본어',
}

// LLM이 아직 연결되지 않아 화면 확인용으로 고정해 둔 예시 질문 목록입니다.
const MOCK_QUESTIONS = [
  {
    jp: '自己紹介を簡単にお願いします。そして、なぜこの職務に応募されたのか教えてください。',
    kr: '자기소개를 간단히 부탁드립니다. 그리고 왜 이 직무에 지원하셨는지 말씀해주세요.',
  },
  {
    jp: 'これまでのプロジェクトの中で、一番難しかった課題は何ですか?',
    kr: '지금까지 진행한 프로젝트 중 가장 어려웠던 과제는 무엇인가요?',
  },
  {
    jp: 'チームで意見が対立した時、どのように解決しましたか?',
    kr: '팀에서 의견이 대립했을 때 어떻게 해결하셨나요?',
  },
  {
    jp: '5年後、どのようなエンジニアになっていたいですか?',
    kr: '5년 후 어떤 엔지니어가 되어 있고 싶으신가요?',
  },
  {
    jp: '最後に、何か質問はありますか?',
    kr: '마지막으로 궁금하신 점이 있으신가요?',
  },
]

function InterviewSpeakingPage() {
  const navigate = useNavigate()
  const location = useLocation()
  // Setup 화면에서 값을 비워둔 채 넘어오거나 URL로 직접 접근한 경우를 대비해
  // 필드별로 빈 값이면 기본값으로 보정합니다.
  const receivedSettings = location.state ?? {}
  const settings = Object.fromEntries(
    Object.entries(DEFAULT_SETTINGS).map(([key, fallback]) => [key, receivedSettings[key] || fallback]),
  )

  const { status, statusText, cycleStatus, setStatus } = useSpeakingStatus()
  const elapsed = useElapsedTimer()
  const [questionIndex, setQuestionIndex] = useState(0)
  const [isEndModalOpen, setEndModalOpen] = useState(false)

  const sessionTitle = `${settings.job} 면접 연습`
  const sessionMeta = `면접 회화 · ${settings.interviewType} · ${settings.difficulty}`
  const currentQuestion = MOCK_QUESTIONS[questionIndex]
  const progressPercent = ((questionIndex + 1) / MOCK_QUESTIONS.length) * 100

  const handleBackClick = () => setEndModalOpen(true)
  const handleContinue = () => setEndModalOpen(false)
  const handleConfirmEnd = () => navigate('/learning')

  // mock 단계: listening 상태에서 답변 완료를 누르면 thinking으로 전환하고 다음 mock 질문으로 넘어갑니다.
  // 실제 서비스에서는 STT -> Backend -> LLM 분석 -> 꼬리질문 생성 -> TTS 흐름으로 대체될 예정입니다.
  const handleNextQuestion = () => {
    setStatus('thinking')
    setQuestionIndex((prev) => Math.min(prev + 1, MOCK_QUESTIONS.length - 1))
  }

  return (
    <LearnerLayout>
      <div className="speaking-page interview-speaking-page">
        <div className="speaking-header">
          <div className="speaking-header__back-row">
            <button
              type="button"
              className="setup-page__back"
              onClick={handleBackClick}
              aria-label="면접 종료하기"
            >
              <BackArrowIcon size={18} />
            </button>
            <div className="speaking-header__title-block">
              <p className="speaking-header__title">{sessionTitle}</p>
              <p className="speaking-header__meta">{sessionMeta}</p>
            </div>
          </div>

          <div className="speaking-timer">
            <span className="speaking-timer__dot" />
            <span className="speaking-timer__time">{elapsed}</span>
          </div>
        </div>

        <div className="interview-progress-row">
          <p className="interview-progress-row__label">
            질문 {questionIndex + 1} / {MOCK_QUESTIONS.length}
          </p>
          <div className="interview-progress-row__track">
            <div className="interview-progress-row__fill" style={{ width: `${progressPercent}%` }} />
          </div>
        </div>

        <div className="interview-question-card">
          <div className="interview-question-card__label">
            <JobIcon size={16} />
            <span>AI 면접관</span>
          </div>

          {settings.subtitleMode === 'OFF' ? (
            <p className="speaking-subtitle-off speaking-subtitle-off--dark">자막이 꺼져 있어요.</p>
          ) : (
            <>
              <p className="interview-question-card__jp">{currentQuestion.jp}</p>
              {settings.subtitleMode === '일본어 + 한국어' && (
                <p className="interview-question-card__kr">{currentQuestion.kr}</p>
              )}
            </>
          )}
        </div>

        <div className="voice-orb-area">
          <VoiceOrb
            variant="sm"
            status={status}
            interactive
            onActivate={cycleStatus}
            ariaLabel={`마이크, 현재 상태: ${statusText}`}
          />
          <p className="voice-orb-area__status">{statusText}</p>
        </div>

        <div className="speaking-controls speaking-controls--fill">
          <button type="button" className="speaking-primary-button" onClick={handleNextQuestion}>
            <CheckIcon size={18} />
            답변 완료
          </button>
        </div>
      </div>

      <SessionEndModal open={isEndModalOpen} onContinue={handleContinue} onEnd={handleConfirmEnd} />
    </LearnerLayout>
  )
}

export default InterviewSpeakingPage
