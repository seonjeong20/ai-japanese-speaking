import { useNavigate, useParams } from 'react-router-dom'
import LearnerLayout from '../../components/layout/LearnerLayout'
import FeedbackHeader from '../../components/feedback/FeedbackHeader'
import FeedbackStatsRow from '../../components/feedback/FeedbackStatsRow'
import ScoreBar from '../../components/feedback/ScoreBar'
import { BackArrowIcon, StarIcon, SwapArrowIcon } from '../../components/icons/DashboardIcons'
import { historyMock } from '../../data/historyMock'
import '../../components/setup/SetupForm.css'
import '../../components/feedback/FeedbackPage.css'
import '../ConversationFeedback/ConversationFeedbackPage.css'
import '../InterviewFeedback/InterviewFeedbackPage.css'
import './HistoryDetailPage.css'

const TYPE_LABEL = {
  conversation: '일반 회화',
  interview: '면접 회화',
}

function HistoryDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const entry = historyMock.find((item) => String(item.id) === id)

  const handleBack = () => navigate('/history')

  if (!entry) {
    return (
      <LearnerLayout>
        <div className="feedback-page">
          <button
            type="button"
            className="setup-page__back"
            onClick={handleBack}
            aria-label="My History로 돌아가기"
          >
            <BackArrowIcon size={18} />
          </button>
          <p className="history-detail-not-found">기록을 찾을 수 없어요.</p>
        </div>
      </LearnerLayout>
    )
  }

  const subtitle = `${entry.date} · ${entry.duration}분`

  const conversationStats = entry.type === 'conversation' && [
    { id: 'duration', icon: 'clock', value: `${entry.duration}분`, label: '총 대화 시간' },
    { id: 'exchanges', icon: 'message', value: `${entry.feedback.exchangeCount}개`, label: '주고받은 문장' },
    { id: 'corrections', icon: 'edit', value: `${entry.feedback.corrections.length}개`, label: '표현 교정' },
  ]

  return (
    <LearnerLayout>
      <div className="feedback-page">
        <FeedbackHeader
          title={entry.title}
          subtitle={subtitle}
          badgeLabel={TYPE_LABEL[entry.type]}
          onBack={handleBack}
        />

        {entry.type === 'conversation' ? (
          <>
            <FeedbackStatsRow stats={conversationStats} />

            <div className="feedback-card">
              <ScoreBar
                label={entry.feedback.naturalness.label}
                score={entry.feedback.naturalness.score}
                size="lg"
              />
              <p className="conversation-feedback-description">{entry.feedback.naturalness.description}</p>
            </div>

            <div className="feedback-card">
              <p className="feedback-card__title">표현 교정</p>
              <div className="conversation-feedback-corrections">
                {entry.feedback.corrections.map((item) => (
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
                <p className="feedback-card__title feedback-card__title--on-dark">추천 표현</p>
              </div>
              {entry.feedback.recommendedExpressions.map((expr) => (
                <div key={expr.id} className="conversation-feedback-recommended__row">
                  <p className="conversation-feedback-recommended__jp">{expr.jp}</p>
                  <p className="conversation-feedback-recommended__kr">{expr.kr}</p>
                </div>
              ))}
            </div>
          </>
        ) : (
          <>
            <div className="interview-feedback-hero">
              <div className="interview-feedback-hero__score-col">
                <p className="interview-feedback-hero__score">{entry.feedback.overall.score}</p>
                <p className="interview-feedback-hero__score-label">종합 평가</p>
              </div>
              <div className="interview-feedback-hero__divider" />
              <p className="interview-feedback-hero__description">{entry.feedback.overall.description}</p>
            </div>

            <div className="feedback-card">
              <p className="feedback-card__title">세부 평가 항목</p>
              {entry.feedback.metrics.map((metric) => (
                <ScoreBar key={metric.id} label={metric.label} score={metric.score} size="md" />
              ))}
            </div>

            <div className="feedback-card">
              <p className="feedback-card__title">질문별 피드백</p>
              <div className="interview-feedback-qa-list">
                {entry.feedback.questionFeedback.map((item) => (
                  <div key={item.id} className="interview-feedback-qa">
                    <p className="interview-feedback-qa__question">{item.question}</p>
                    <p className="interview-feedback-qa__feedback">{item.feedback}</p>
                  </div>
                ))}
              </div>
            </div>
          </>
        )}
      </div>
    </LearnerLayout>
  )
}

export default HistoryDetailPage
