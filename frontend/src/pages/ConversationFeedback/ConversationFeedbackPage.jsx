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
  const {
    title,
    badgeLabel,
    subtitle,
    stats,
    overallComment,
    naturalness,
    grammar,
    vocabulary,
    strengths,
    corrections,
    recommendedExpressions,
    nextStepTip,
    transcript,
  } = conversationFeedbackMock

  const handleBack = () => navigate('/learning')

  return (
    <LearnerLayout>
      <div className="feedback-page">
        <FeedbackHeader title={title} subtitle={subtitle} badgeLabel={badgeLabel} onBack={handleBack} />

        <FeedbackStatsRow stats={stats} />

        <div className="feedback-card">
          <p className="feedback-card__title">전체 코멘트</p>
          <p className="conversation-feedback-description">{overallComment}</p>

          <div className="feedback-subsection">
            <p className="feedback-subsection__label">잘한 점</p>
            <ul className="feedback-bullet-list">
              {strengths.map((item, index) => (
                <li key={index} className="feedback-bullet-list__item">
                  {item}
                </li>
              ))}
            </ul>
          </div>

          <div className="feedback-subsection">
            <p className="feedback-subsection__label">다음 학습 팁</p>
            <p className="feedback-subsection__text">{nextStepTip}</p>
          </div>
        </div>

        <div className="feedback-card">
          <p className="feedback-card__title">세부 평가</p>
          <ScoreBar label={naturalness.label} score={naturalness.score} size="lg" />
          <p className="conversation-feedback-description">{naturalness.description}</p>
          <ScoreBar label={grammar.label} score={grammar.score} size="md" />
          <p className="conversation-feedback-description">{grammar.description}</p>
          <ScoreBar label={vocabulary.label} score={vocabulary.score} size="md" />
          <p className="conversation-feedback-description">{vocabulary.description}</p>
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

        <div className="feedback-card">
          <p className="feedback-card__title">전체 대화 기록</p>
          <div className="feedback-transcript">
            {transcript.map((turn) => (
              <div key={turn.id} className="feedback-transcript__row">
                <p className="feedback-transcript__speaker">{turn.speaker === 'user' ? '나' : 'AI'}</p>
                <p className="feedback-transcript__jp">{turn.jp}</p>
                <p className="feedback-transcript__kr">{turn.kr}</p>
              </div>
            ))}
          </div>
        </div>

        <FeedbackCtaRow retryTo="/conversation/setup" homeTo="/learning" />
      </div>
    </LearnerLayout>
  )
}

export default ConversationFeedbackPage
