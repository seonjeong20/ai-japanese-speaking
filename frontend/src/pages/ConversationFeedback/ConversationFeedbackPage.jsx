import { useNavigate } from 'react-router-dom'
import LearnerLayout from '../../components/layout/LearnerLayout'
import FeedbackHeader from '../../components/feedback/FeedbackHeader'
import FeedbackCtaRow from '../../components/feedback/FeedbackCtaRow'
import FeedbackStatsRow from '../../components/feedback/FeedbackStatsRow'
import ScoreBar from '../../components/feedback/ScoreBar'
import { StarIcon, SwapArrowIcon } from '../../components/icons/DashboardIcons'
import { conversationFeedbackMock } from '../../data/conversationFeedbackMock'
import '../../components/setup/SetupForm.css'
import '../../components/feedback/FeedbackPage.css'
import './ConversationFeedbackPage.css'

function ConversationFeedbackPage() {
  const navigate = useNavigate()
  const { title, badgeLabel, subtitle, stats, naturalness, corrections, recommendedExpressions } =
    conversationFeedbackMock

  const handleBack = () => navigate('/learning')

  return (
    <LearnerLayout>
      <div className="feedback-page">
        <FeedbackHeader title={title} subtitle={subtitle} badgeLabel={badgeLabel} onBack={handleBack} />

        <FeedbackStatsRow stats={stats} />

        <div className="feedback-card">
          <ScoreBar label={naturalness.label} score={naturalness.score} size="lg" />
          <p className="conversation-feedback-description">{naturalness.description}</p>
        </div>

        <div className="feedback-card">
          <p className="feedback-card__title">표현 교정</p>
          <div className="conversation-feedback-corrections">
            {corrections.map((item) => (
              <div key={item.id} className="conversation-feedback-correction">
                <div className="conversation-feedback-correction__swap">
                  <span className="conversation-feedback-correction__original">{item.original}</span>
                  <SwapArrowIcon size={15} className="conversation-feedback-correction__arrow" />
                  <span className="conversation-feedback-correction__corrected">{item.corrected}</span>
                </div>
                <p className="conversation-feedback-correction__note">{item.note}</p>
              </div>
            ))}
          </div>
        </div>

        <div className="feedback-card feedback-card--dark">
          <div className="conversation-feedback-recommended__header">
            <StarIcon size={18} />
            <p className="feedback-card__title feedback-card__title--on-dark">오늘의 추천 표현</p>
          </div>
          {recommendedExpressions.map((expr) => (
            <div key={expr.id} className="conversation-feedback-recommended__row">
              <p className="conversation-feedback-recommended__jp">{expr.jp}</p>
              <p className="conversation-feedback-recommended__kr">{expr.kr}</p>
            </div>
          ))}
        </div>

        <FeedbackCtaRow retryTo="/conversation/setup" homeTo="/learning" />
      </div>
    </LearnerLayout>
  )
}

export default ConversationFeedbackPage
