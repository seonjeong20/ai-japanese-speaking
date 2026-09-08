import { useNavigate } from 'react-router-dom'
import LearnerLayout from '../../components/layout/LearnerLayout'
import FeedbackHeader from '../../components/feedback/FeedbackHeader'
import FeedbackCtaRow from '../../components/feedback/FeedbackCtaRow'
import ScoreBar from '../../components/feedback/ScoreBar'
import { interviewFeedbackMock } from '../../data/interviewFeedbackMock'
import '../../components/setup/SetupForm.css'
import '../../components/feedback/FeedbackPage.css'
import './InterviewFeedbackPage.css'

function InterviewFeedbackPage() {
  const navigate = useNavigate()
  const { title, badgeLabel, subtitle, overall, metrics, questionFeedback } = interviewFeedbackMock

  const handleBack = () => navigate('/learning')

  return (
    <LearnerLayout>
      <div className="feedback-page">
        <FeedbackHeader title={title} subtitle={subtitle} badgeLabel={badgeLabel} onBack={handleBack} />

        <div className="interview-feedback-hero">
          <div className="interview-feedback-hero__score-col">
            <p className="interview-feedback-hero__score">{overall.score}</p>
            <p className="interview-feedback-hero__score-label">종합 평가</p>
          </div>
          <div className="interview-feedback-hero__divider" />
          <p className="interview-feedback-hero__description">{overall.description}</p>
        </div>

        <div className="feedback-card">
          <p className="feedback-card__title">세부 평가 항목</p>
          {metrics.map((metric) => (
            <ScoreBar key={metric.id} label={metric.label} score={metric.score} size="md" />
          ))}
        </div>

        <div className="feedback-card">
          <p className="feedback-card__title">질문별 피드백</p>
          <div className="interview-feedback-qa-list">
            {questionFeedback.map((item) => (
              <div key={item.id} className="interview-feedback-qa">
                <p className="interview-feedback-qa__question">{item.question}</p>
                <p className="interview-feedback-qa__feedback">{item.feedback}</p>
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
