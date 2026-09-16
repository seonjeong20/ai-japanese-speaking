import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import LearnerLayout from '../../components/layout/LearnerLayout'
import FeedbackHeader from '../../components/feedback/FeedbackHeader'
import FeedbackStatsRow from '../../components/feedback/FeedbackStatsRow'
import ScoreBar from '../../components/feedback/ScoreBar'
import { BackArrowIcon, SwapArrowIcon } from '../../components/icons/DashboardIcons'
import { getMyHistoryDetail } from '../../api/history'
import { SessionType, SessionTypeLabel } from '../../data/enums'
import '../../components/setup/SetupForm.css'
import '../../components/feedback/FeedbackPage.css'
import '../ConversationFeedback/ConversationFeedbackPage.css'
import '../InterviewFeedback/InterviewFeedbackPage.css'
import './HistoryDetailPage.css'

function formatDate(isoDateTime) {
  if (!isoDateTime) return ''
  const date = new Date(isoDateTime)
  if (Number.isNaN(date.getTime())) return ''
  return `${date.getMonth() + 1}월 ${date.getDate()}일`
}

function formatDurationMinutes(durationSeconds) {
  if (durationSeconds == null) return 0
  return Math.round(durationSeconds / 60)
}

function HistoryDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()

  const [detail, setDetail] = useState(null)
  const [isLoading, setLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')

  const handleBack = () => navigate('/history')

  useEffect(() => {
    let cancelled = false

    async function loadDetail() {
      setLoading(true)
      setErrorMessage('')
      try {
        const data = await getMyHistoryDetail(id)
        if (!cancelled) setDetail(data)
      } catch (error) {
        if (!cancelled) setErrorMessage(error.message || '기록을 불러오지 못했습니다.')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    loadDetail()
    return () => {
      cancelled = true
    }
  }, [id])

  if (isLoading) {
    return (
      <LearnerLayout>
        <div className="feedback-page">
          <p className="conversation-feedback-description">기록을 불러오는 중이에요...</p>
        </div>
      </LearnerLayout>
    )
  }

  if (errorMessage || !detail) {
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
          <p className="history-detail-not-found">{errorMessage || '기록을 찾을 수 없어요.'}</p>
        </div>
      </LearnerLayout>
    )
  }

  const durationMinutes = formatDurationMinutes(detail.durationSeconds)
  const subtitle = `${formatDate(detail.startedAt)} · ${durationMinutes}분`
  const isConversation = detail.sessionType === SessionType.CONVERSATION
  const title = isConversation
    ? detail.settings?.situation || '일반 회화'
    : detail.settings?.jobRole || '면접 회화'

  return (
    <LearnerLayout>
      <div className="feedback-page">
        <FeedbackHeader
          title={title}
          subtitle={subtitle}
          badgeLabel={SessionTypeLabel[detail.sessionType]}
          onBack={handleBack}
        />

        {isConversation ? (
          <ConversationHistoryDetail
            detail={detail}
            durationMinutes={durationMinutes}
          />
        ) : (
          <InterviewHistoryDetail detail={detail} />
        )}
      </div>
    </LearnerLayout>
  )
}

function ConversationHistoryDetail({ detail, durationMinutes }) {
  const feedback = detail.conversationFeedback

  if (!feedback || feedback.generationStatus !== 'COMPLETED') {
    return (
      <div className="feedback-card">
        <p className="conversation-feedback-description">
          이 기록에는 아직 완료된 피드백이 없어요.
        </p>
      </div>
    )
  }

  const {
    summary,
    naturalnessComment,
    grammarComment,
    vocabularyComment,
    strengths,
    nextTip,
    corrections,
  } = feedback

  const stats = [
    { id: 'duration', icon: 'clock', value: `${durationMinutes}분`, label: '총 대화 시간' },
    { id: 'exchanges', icon: 'message', value: `${detail.transcript?.length ?? 0}개`, label: '주고받은 문장' },
    { id: 'corrections', icon: 'edit', value: `${corrections?.length ?? 0}개`, label: '표현 교정' },
  ]

  return (
    <>
      <FeedbackStatsRow stats={stats} />

      <div className="feedback-card">
        <p className="feedback-card__title">전체 코멘트</p>
        {summary && <p className="conversation-feedback-description">{summary}</p>}

        {strengths && strengths.length > 0 && (
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
        )}

        {nextTip && (
          <div className="feedback-subsection">
            <p className="feedback-subsection__label">다음 학습 팁</p>
            <p className="feedback-subsection__text">{nextTip}</p>
          </div>
        )}
      </div>

      {(naturalnessComment || grammarComment || vocabularyComment) && (
        <div className="feedback-card">
          <p className="feedback-card__title">세부 평가</p>

          {naturalnessComment && (
            <div className="feedback-subsection">
              <p className="feedback-subsection__label">자연스러움</p>
              <p className="conversation-feedback-description">{naturalnessComment}</p>
            </div>
          )}

          {grammarComment && (
            <div className="feedback-subsection">
              <p className="feedback-subsection__label">문법</p>
              <p className="conversation-feedback-description">{grammarComment}</p>
            </div>
          )}

          {vocabularyComment && (
            <div className="feedback-subsection">
              <p className="feedback-subsection__label">어휘</p>
              <p className="conversation-feedback-description">{vocabularyComment}</p>
            </div>
          )}
        </div>
      )}

      {corrections && corrections.length > 0 && (
        <div className="feedback-card">
          <p className="feedback-card__title">표현 교정</p>
          <div className="conversation-feedback-corrections">
            {corrections.map((item, index) => (
              <div key={index} className="conversation-feedback-correction">
                <div className="conversation-feedback-correction__swap">
                  <span className="conversation-feedback-correction__original">{item.originalExpression}</span>
                  <SwapArrowIcon size={15} className="conversation-feedback-correction__arrow" />
                  <span className="conversation-feedback-correction__corrected">{item.suggestedExpression}</span>
                </div>
                {item.reading && (
                  <p className="conversation-feedback-correction__reading">{item.reading}</p>
                )}
                {item.koreanTranslation && (
                  <p className="conversation-feedback-correction__translation">{item.koreanTranslation}</p>
                )}
                {item.explanation && (
                  <p className="conversation-feedback-correction__note">{item.explanation}</p>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {detail.transcript && detail.transcript.length > 0 && (
        <div className="feedback-card">
          <p className="feedback-card__title">전체 대화 기록</p>
          <div className="feedback-transcript">
            {detail.transcript.map((message) => (
              <div key={message.messageId} className="feedback-transcript__row">
                <p className="feedback-transcript__speaker">{message.speaker === 'USER' ? '나' : 'AI'}</p>
                <p className="feedback-transcript__jp">{message.content}</p>
              </div>
            ))}
          </div>
        </div>
      )}
    </>
  )
}

function InterviewHistoryDetail({ detail }) {
  const feedback = detail.interviewFeedback

  if (!feedback || feedback.overall?.generationStatus !== 'COMPLETED') {
    return (
      <div className="feedback-card">
        <p className="conversation-feedback-description">
          이 기록에는 아직 완료된 종합 평가가 없어요.
        </p>
      </div>
    )
  }

  const { overall, answers } = feedback

  return (
    <>
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
            {(overall.strengths ?? []).map((item, index) => (
              <li key={index} className="feedback-bullet-list__item">
                {item}
              </li>
            ))}
          </ul>
        </div>

        <div className="feedback-subsection">
          <p className="feedback-subsection__label">개선점</p>
          <ul className="feedback-bullet-list">
            {(overall.improvements ?? []).map((item, index) => (
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
          {(answers ?? []).map((item, index) => (
            <div key={index} className="interview-feedback-qa">
              <p className="interview-feedback-qa__question">{item.question}</p>
              {(item.scores ?? []).map((score) => (
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
    </>
  )
}

export default HistoryDetailPage
