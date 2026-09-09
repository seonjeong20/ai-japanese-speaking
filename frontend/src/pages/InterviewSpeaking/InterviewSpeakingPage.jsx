import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import LearnerLayout from '../../components/layout/LearnerLayout'
import VoiceOrb from '../../components/speaking/VoiceOrb'
import SessionEndModal from '../../components/speaking/SessionEndModal'
import { useSpeakingStatus } from '../../components/speaking/useSpeakingStatus'
import { useElapsedTimer } from '../../components/speaking/useElapsedTimer'
import { BackArrowIcon, CheckIcon, JobIcon } from '../../components/icons/DashboardIcons'
import { Difficulty, DifficultyLabel, QuestionKind, SubtitleMode } from '../../data/enums'
import '../../components/setup/SetupForm.css'
import '../../components/speaking/SpeakingSession.css'
import './InterviewSpeakingPage.css'

// URL로 직접 접근하는 등 Setup에서 전달된 state가 없을 때 사용하는 기본값입니다.
const DEFAULT_SETTINGS = {
  job: 'Backend Developer',
  interviewType: '기술 면접',
  difficulty: Difficulty.INTERMEDIATE,
  additionalRequest: '',
  subtitleMode: SubtitleMode.JAPANESE,
}

// LLM이 아직 연결되지 않아 화면 확인용으로 고정해 둔 예시 질문 목록입니다.
// kind: 'INITIAL'(첫 질문) | 'FOLLOW_UP'(꼬리 질문) — 실제 Backend 연결 시
// 각 질문 응답의 questionKind 값으로 대체됩니다.
const MOCK_QUESTIONS = [
  {
    kind: QuestionKind.INITIAL,
    jp: '自己紹介を簡単にお願いします。そして、なぜこの職務に応募されたのか教えてください。',
    kr: '자기소개를 간단히 부탁드립니다. 그리고 왜 이 직무에 지원하셨는지 말씀해주세요.',
  },
  {
    kind: QuestionKind.FOLLOW_UP,
    jp: 'これまでのプロジェクトの中で、一番難しかった課題は何ですか?',
    kr: '지금까지 진행한 프로젝트 중 가장 어려웠던 과제는 무엇인가요?',
  },
  {
    kind: QuestionKind.FOLLOW_UP,
    jp: 'チームで意見が対立した時、どのように解決しましたか?',
    kr: '팀에서 의견이 대립했을 때 어떻게 해결하셨나요?',
  },
  {
    kind: QuestionKind.FOLLOW_UP,
    jp: '5年後、どのようなエンジニアになっていたいですか?',
    kr: '5년 후 어떤 엔지니어가 되어 있고 싶으신가요?',
  },
  {
    kind: QuestionKind.FOLLOW_UP,
    jp: '最後に、何か質問はありますか?',
    kr: '마지막으로 궁금하신 점이 있으신가요?',
  },
]

// 답변 완료 후 "다음 질문" 또는 "면접 종료 여부"를 결정하는 부분만 분리해 두었습니다.
// 지금은 mockQuestions 배열로 판단하지만, 실제 Backend가 연결되면 이 함수 내부만
// `{ isComplete, nextQuestion }` 형태의 API 응답을 받아 처리하도록 교체하면 됩니다.
function getNextInterviewStep(currentIndex) {
  const nextIndex = currentIndex + 1
  if (nextIndex >= MOCK_QUESTIONS.length) {
    return { isComplete: true }
  }
  return { isComplete: false, nextIndex }
}

// 마지막 질문 답변 완료 후 Feedback으로 이동하기 전 짧게 보여주는 완료 문구입니다.
const COMPLETE_MESSAGE = ['면접이 완료되었습니다.', '답변을 바탕으로 피드백을 준비하고 있어요.']
const COMPLETE_TRANSITION_MS = 900

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
  // 마지막 질문에 답변 완료한 뒤 Feedback으로 넘어가기 전, 아주 짧게 보여주는 완료 상태입니다.
  const [isInterviewComplete, setInterviewComplete] = useState(false)

  const sessionTitle = `${settings.job} 면접 연습`
  const sessionMeta = `면접 회화 · ${settings.interviewType} · ${DifficultyLabel[settings.difficulty]}`
  const currentQuestion = MOCK_QUESTIONS[questionIndex]
  const progressPercent = ((questionIndex + 1) / MOCK_QUESTIONS.length) * 100

  // 뒤로가기는 이전 화면으로 돌아가는 navigation 용도가 아니라, 면접 도중 실수로
  // 이탈하는 것을 막기 위한 중도 종료 확인 modal을 엽니다.
  // 중도 종료(면접 종료 버튼)는 "정상 완료"가 아니므로 Feedback으로 보내지 않고
  // Interview Setup으로 돌려보냅니다. /interview/feedback으로의 이동은 오직
  // 마지막 질문의 "답변 완료"(아래 handleNextQuestion → isInterviewComplete)를 통해서만 이루어집니다.
  const handleBackClick = () => setEndModalOpen(true)
  const handleContinue = () => setEndModalOpen(false)
  const handleExitInterview = () => navigate('/interview/setup')

  // mock 단계: listening 상태에서 답변 완료를 누르면 thinking으로 전환하고,
  // 마지막 질문이 아니면 다음 질문으로 넘어갑니다.
  // 실제 서비스에서는 STT -> Backend -> LLM 분석 -> 꼬리질문 생성(or 면접 종료 판단) -> TTS 흐름으로 대체될 예정이며,
  // getNextInterviewStep 내부만 실제 API 응답 처리로 교체하면 됩니다.
  const handleNextQuestion = () => {
    setStatus('thinking')
    const nextStep = getNextInterviewStep(questionIndex)

    if (nextStep.isComplete) {
      setInterviewComplete(true)
      return
    }

    setQuestionIndex(nextStep.nextIndex)
  }

  // 마지막 질문 완료 표시를 짧게 보여준 뒤 Feedback 화면으로 이동합니다.
  useEffect(() => {
    if (!isInterviewComplete) return undefined

    const timeoutId = setTimeout(() => {
      navigate('/interview/feedback')
    }, COMPLETE_TRANSITION_MS)

    return () => clearTimeout(timeoutId)
  }, [isInterviewComplete, navigate])

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

          {settings.subtitleMode === SubtitleMode.OFF ? (
            <p className="speaking-subtitle-off speaking-subtitle-off--dark">자막이 꺼져 있어요.</p>
          ) : (
            <>
              <p className="interview-question-card__jp">{currentQuestion.jp}</p>
              {settings.subtitleMode === SubtitleMode.JAPANESE_KOREAN && (
                <p className="interview-question-card__kr">{currentQuestion.kr}</p>
              )}
            </>
          )}
        </div>

        <div className="voice-orb-area">
          <VoiceOrb
            variant="sm"
            status={status}
            interactive={!isInterviewComplete}
            onActivate={cycleStatus}
            ariaLabel={`마이크, 현재 상태: ${statusText}`}
          />
          {isInterviewComplete ? (
            <p className="voice-orb-area__status">
              {COMPLETE_MESSAGE[0]}
              <br />
              {COMPLETE_MESSAGE[1]}
            </p>
          ) : (
            <p className="voice-orb-area__status">{statusText}</p>
          )}
        </div>

        <div className="speaking-controls speaking-controls--fill">
          <button
            type="button"
            className="speaking-primary-button"
            onClick={handleNextQuestion}
            disabled={isInterviewComplete}
          >
            <CheckIcon size={18} />
            답변 완료
          </button>
        </div>
      </div>

      <SessionEndModal
        open={isEndModalOpen}
        onContinue={handleContinue}
        onEnd={handleExitInterview}
        title="면접을 종료하시겠습니까?"
        subtitle="종료하면 현재 면접 연습은 완료되지 않습니다."
        confirmLabel="면접 종료"
      />
    </LearnerLayout>
  )
}

export default InterviewSpeakingPage
