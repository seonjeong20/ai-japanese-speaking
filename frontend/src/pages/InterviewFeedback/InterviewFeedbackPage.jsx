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
  const { title, badgeLabel, subtitle, overall, metrics, strengths, improvements, coaching, questionFeedback } =
    interviewFeedbackMock

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
          <div className="feedback-subsection">
            <p className="feedback-subsection__label">강점</p>
            <ul className="feedback-bullet-list">
              {strengths.map((item, index) => (
                <li key={index} className="feedback-bullet-list__item">
                  {item}
                </li>
              ))}
            </ul>
          </div>

          <div className="feedback-subsection">
            <p className="feedback-subsection__label">개선점</p>
            <ul className="feedback-bullet-list">
              {improvements.map((item, index) => (
                <li key={index} className="feedback-bullet-list__item">
                  {item}
                </li>
              ))}
            </ul>
          </div>

          <div className="feedback-subsection">
            <p className="feedback-subsection__label">코칭</p>
            <p className="feedback-subsection__text">{coaching}</p>
          </div>
        </div>

        <div className="feedback-card">
          <p className="feedback-card__title">질문별 피드백</p>
          <div className="interview-feedback-qa-list">
            {questionFeedback.map((item) => (
              <div key={item.id} className="interview-feedback-qa">
                <p className="interview-feedback-qa__question">{item.question}</p>
                <p className="interview-feedback-qa__feedback">{item.feedback}</p>
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
