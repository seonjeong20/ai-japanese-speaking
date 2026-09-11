import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import LearnerLayout from '../../components/layout/LearnerLayout'
import FeedbackHeader from '../../components/feedback/FeedbackHeader'
import FeedbackCtaRow from '../../components/feedback/FeedbackCtaRow'
import ScoreBar from '../../components/feedback/ScoreBar'
import { getInterviewFeedback } from '../../api/interviews'
import '../../components/setup/SetupForm.css'
import '../../components/feedback/FeedbackPage.css'
import './InterviewFeedbackPage.css'

function InterviewFeedbackPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const sessionId = location.state?.sessionId

  const [feedback, setFeedback] = useState(null)
  const [isLoading, setLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    if (!sessionId) {
      navigate('/learning', { replace: true })
      return
    }

    let cancelled = false
    getInterviewFeedback(sessionId)
      .then((data) => {
        if (!cancelled) setFeedback(data)
      })
      .catch((error) => {
        if (!cancelled) setErrorMessage(error.message || '면접 결과를 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [sessionId, navigate])

  const handleBack = () => navigate('/learning')

  if (isLoading) {
    return (
      <LearnerLayout>
        <div className="feedback-page">
          <p className="conversation-feedback-description">면접 결과를 불러오는 중이에요...</p>
        </div>
      </LearnerLayout>
    )
  }

  if (errorMessage || !feedback) {
    return (
      <LearnerLayout>
        <div className="feedback-page">
          <FeedbackHeader title="면접 완료" subtitle="" badgeLabel="면접 완료" onBack={handleBack} />
          <div className="feedback-card">
            <p className="conversation-feedback-description">{errorMessage || '면접 결과를 찾을 수 없습니다.'}</p>
          </div>
          <FeedbackCtaRow retryTo="/interview/setup" homeTo="/learning" />
        </div>
      </LearnerLayout>
    )
  }

  const { overall, answers } = feedback

  return (
    <LearnerLayout>
      <div className="feedback-page">
        <FeedbackHeader title="면접 결과" subtitle="AI가 전체 면접을 종합했어요" badgeLabel="면접 완료" onBack={handleBack} />

        <div className="interview-feedback-hero">
          <div className="interview-feedback-hero__score-col">
            <p className="interview-feedback-hero__score">{overall.overallScore}</p>
            <p className="interview-feedback-hero__score-label">종합 평가</p>
          </div>
          <div className="interview-feedback-hero__divider" />
          <p className="interview-feedback-hero__description">{overall.summary}</p>
        </div>

        <div className="feedback-card">
          <div className="feedback-subsection">
            <p className="feedback-subsection__label">강점</p>
            <ul className="feedback-bullet-list">
              {overall.strengths.map((item, index) => (
                <li key={index} className="feedback-bullet-list__item">
                  {item}
                </li>
              ))}
            </ul>
          </div>

          <div className="feedback-subsection">
            <p className="feedback-subsection__label">개선점</p>
            <ul className="feedback-bullet-list">
              {overall.improvements.map((item, index) => (
                <li key={index} className="feedback-bullet-list__item">
                  {item}
                </li>
              ))}
            </ul>
          </div>
        </div>

        <div className="feedback-card">
          <p className="feedback-card__title">질문별 피드백</p>
          <div className="interview-feedback-qa-list">
            {answers.map((item, index) => (
              <div key={index} className="interview-feedback-qa">
                <p className="interview-feedback-qa__question">{item.question}</p>
                {item.scores.map((score) => (
                  <ScoreBar
                    key={score.criterion}
                    label={score.criterion}
                    score={score.applicable ? score.score : 0}
                    size="md"
                  />
                ))}
                <p className="interview-feedback-qa__feedback">{item.coachingSummary}</p>
                <p className="interview-feedback-qa__improved">
                  <span className="interview-feedback-qa__improved-label">개선 답변</span>
                  {item.improvedAnswer}
                </p>
              </div>
            ))}
          </div>
        </div>

        <FeedbackCtaRow retryTo="/interview/setup" homeTo="/learning" />
      </div>
    </LearnerLayout>
  )
}

export default InterviewFeedbackPage
