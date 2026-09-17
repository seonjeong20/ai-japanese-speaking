import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import LearnerLayout from '../../components/layout/LearnerLayout'
import VoiceOrb from '../../components/speaking/VoiceOrb'
import SessionEndModal from '../../components/speaking/SessionEndModal'
import { useSpeakingStatus } from '../../components/speaking/useSpeakingStatus'
import { useElapsedTimer } from '../../components/speaking/useElapsedTimer'
import { useAudioRecorder } from '../../components/speaking/useAudioRecorder'
import { BackArrowIcon, CheckIcon, JobIcon } from '../../components/icons/DashboardIcons'
import { Difficulty, DifficultyLabel, SubtitleMode } from '../../data/enums'
import { abortInterview, completeInterview, submitInterviewAudioAnswer } from '../../api/interviews'
import '../../components/setup/SetupForm.css'
import '../../components/speaking/SpeakingSession.css'
import './InterviewSpeakingPage.css'

const DEFAULT_SETTINGS = {
  job: 'Backend Developer',
  interviewType: '기술 면접',
  difficulty: Difficulty.INTERMEDIATE,
  additionalRequest: '',
  subtitleMode: SubtitleMode.JAPANESE,
}

function InterviewSpeakingPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const receivedSettings = location.state ?? {}
  const settings = Object.fromEntries(
    Object.entries(DEFAULT_SETTINGS).map(([key, fallback]) => [key, receivedSettings[key] || fallback]),
  )
  const sessionId = receivedSettings.sessionId
  const firstQuestion = receivedSettings.firstQuestion

  const { status, statusText, setStatus } = useSpeakingStatus()
  const elapsed = useElapsedTimer()
  const [isEndModalOpen, setEndModalOpen] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [answerResult, setAnswerResult] = useState(null)
  const [currentQuestion, setCurrentQuestion] = useState(firstQuestion)
  const [isCompleting, setCompleting] = useState(false)

  useEffect(() => {
    if (!sessionId || !firstQuestion?.questionId) {
      navigate('/interview/setup', { replace: true })
    }
  }, [sessionId, firstQuestion, navigate])

  const sessionTitle = `${settings.job} 면접 연습`
  const sessionMeta = `면접 회화 · ${settings.interviewType} · ${DifficultyLabel[settings.difficulty]}`

  const submitAnswer = async (audioBlob) => {
    setStatus('thinking')
    try {
      const result = await submitInterviewAudioAnswer(
        sessionId,
        currentQuestion.questionId,
        audioBlob,
        'answer.webm',
      )
      setAnswerResult(result)
      setStatus('idle')
    } catch (error) {
      setErrorMessage(error.message || '답변 평가에 실패했습니다. 다시 시도해주세요.')
      setStatus('idle')
    }
  }

  const { start: startAudioRecording, stop: stopRecording } = useAudioRecorder({
    onComplete: submitAnswer,
    onError: (message) => {
      setErrorMessage(message)
      setStatus('idle')
    },
  })

  const startRecording = async () => {
    setErrorMessage('')
    const started = await startAudioRecording()
    if (started) setStatus('listening')
  }

  const handleBackClick = () => setEndModalOpen(true)
  const handleContinue = () => setEndModalOpen(false)
  // 면접을 마무리하지 않고 나가는 경우입니다. 세션이 IN_PROGRESS로 방치되지 않도록
  // 서버에 중도 종료를 알리되, 이동 자체를 막을 정도의 오류는 아니므로 결과를 기다리지 않습니다.
  const handleExitInterview = () => {
    if (sessionId) {
      abortInterview(sessionId).catch(() => {})
    }
    navigate('/interview/setup')
  }
  const handleNextQuestion = () => {
    if (!answerResult?.nextQuestion) return
    setCurrentQuestion(answerResult.nextQuestion)
    setAnswerResult(null)
    setErrorMessage('')
    setStatus('idle')
  }
  const feedback = answerResult?.feedback
  const isInterviewComplete = answerResult?.isComplete === true

  const handleViewResult = async () => {
    if (isCompleting) return
    setCompleting(true)
    setErrorMessage('')
    try {
      await completeInterview(sessionId)
      navigate('/interview/feedback', { state: { sessionId } })
    } catch (error) {
      setErrorMessage(error.message || '면접 결과를 생성하지 못했습니다. 다시 시도해주세요.')
      setCompleting(false)
    }
  }

  return (
    <LearnerLayout>
      <div className="speaking-page interview-speaking-page">
        <div className="speaking-header">
          <div className="speaking-header__back-row">
            <button type="button" className="setup-page__back" onClick={handleBackClick} aria-label="면접 종료하기">
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
          <p className="interview-progress-row__label">질문 {currentQuestion?.sequenceNo ?? 1} / 최대 10</p>
          <div className="interview-progress-row__track">
            <div
              className="interview-progress-row__fill"
              style={{ width: `${Math.min(((currentQuestion?.sequenceNo ?? 1) / 10) * 100, 100)}%` }}
            />
          </div>
        </div>

        <div className="interview-question-card">
          <div className="interview-question-card__label"><JobIcon size={16} /><span>AI 면접관</span></div>
          {settings.subtitleMode === SubtitleMode.OFF ? (
            <p className="speaking-subtitle-off speaking-subtitle-off--dark">자막이 꺼져 있어요.</p>
          ) : (
            <p className="interview-question-card__jp">{currentQuestion?.questionText}</p>
          )}
        </div>

        {!answerResult && (
          <div className="voice-orb-area">
            <VoiceOrb variant="sm" status={status} />
            <p className="voice-orb-area__status">{statusText}</p>
          </div>
        )}

        {errorMessage && <p className="setup-page__error" role="alert">{errorMessage}</p>}

        {answerResult && feedback && (
          <section className="interview-evaluation" aria-label="답변 평가 결과">
            <div className="interview-evaluation__answer">
              <span>인식된 내 답변</span>
              <p lang="ja">{answerResult.answerText}</p>
            </div>
            <div className="interview-evaluation__header">
              <div><span>Overall Score</span><strong>{feedback.overallScore}</strong></div>
              <p>{feedback.evaluationSummary}</p>
            </div>
            <div className="interview-evaluation__scores">
              {feedback.scores.map((score) => (
                <article key={score.criterion} className="interview-score-card">
                  <div><strong>{score.criterion}</strong><span>{score.applicable ? `${score.score}점` : 'N/A'}</span></div>
                  <p>{score.feedback}</p>
                </article>
              ))}
            </div>
            <div className="interview-evaluation__lists">
              <div><strong>강점</strong><ul>{feedback.strengths.map((item) => <li key={item}>{item}</li>)}</ul></div>
              <div><strong>보완점</strong><ul>{feedback.weaknesses.map((item) => <li key={item}>{item}</li>)}</ul></div>
            </div>
            <p className="interview-evaluation__star">STAR 적용: {feedback.starRecommended ? '적용' : '해당 없음'}</p>
            <div className="interview-coaching">
              <div>
                <strong>Coaching</strong>
                <p>{feedback.coachingSummary}</p>
              </div>
              <div>
                <strong>개선 팁</strong>
                <ul>{feedback.improvementTips.map((item) => <li key={item}>{item}</li>)}</ul>
              </div>
              <div>
                <strong>Model Answer</strong>
                <p lang="ja">{feedback.modelAnswer}</p>
              </div>
            </div>
          </section>
        )}

        {answerResult?.nextQuestion && (
          <div className="speaking-controls speaking-controls--fill">
            <button type="button" className="speaking-primary-button" onClick={handleNextQuestion}>
              다음 질문
            </button>
          </div>
        )}

        {isInterviewComplete && (
          <>
            <p className="interview-complete-message" role="status">
              허용된 면접 질문을 모두 완료했습니다.
            </p>
            <div className="speaking-controls speaking-controls--fill">
              <button
                type="button"
                className="speaking-primary-button"
                onClick={handleViewResult}
                disabled={isCompleting}
              >
                {isCompleting ? '결과 생성 중...' : '면접 결과 보기'}
              </button>
            </div>
          </>
        )}

        {!answerResult && (
          <div className="speaking-controls speaking-controls--fill">
            <button
              type="button"
              className="speaking-primary-button"
              onClick={status === 'listening' ? stopRecording : startRecording}
              disabled={status === 'thinking'}
            >
              <CheckIcon size={18} />
              {status === 'listening' ? '답변 완료' : '녹음 시작'}
            </button>
          </div>
        )}
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
