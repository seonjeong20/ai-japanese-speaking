import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import LearnerLayout from '../../components/layout/LearnerLayout'
import FeedbackHeader from '../../components/feedback/FeedbackHeader'
import FeedbackCtaRow from '../../components/feedback/FeedbackCtaRow'
import { getConversationFeedback } from '../../api/conversations'
import { SwapArrowIcon } from '../../components/icons/DashboardIcons'
import '../../components/setup/SetupForm.css'
import '../../components/feedback/FeedbackPage.css'
import './ConversationFeedbackPage.css'

// 일반 회화 Feedback에는 숫자 점수를 사용하지 않습니다. (ai-design.md 7.3)
// naturalness/grammar/vocabulary는 모두 텍스트 코멘트로만 표시합니다.

function ConversationFeedbackPage() {
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

    async function loadFeedback() {
      setLoading(true)
      setErrorMessage('')
      try {
        const data = await getConversationFeedback(sessionId)
        if (!cancelled) setFeedback(data)
      } catch (error) {
        if (!cancelled) setErrorMessage(error.message || '피드백을 불러오지 못했습니다.')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    loadFeedback()
    return () => {
      cancelled = true
    }
  }, [sessionId, navigate])

  const handleBack = () => navigate('/learning')

  if (isLoading) {
    return (
      <LearnerLayout>
        <div className="feedback-page">
          <p className="conversation-feedback-description">피드백을 불러오는 중이에요...</p>
        </div>
      </LearnerLayout>
    )
  }

  if (errorMessage || !feedback) {
    return (
      <LearnerLayout>
        <div className="feedback-page">
          <FeedbackHeader title="일반 회화 완료" subtitle="" badgeLabel="대화 완료" onBack={handleBack} />
          <div className="feedback-card">
            <p className="conversation-feedback-description">
              {errorMessage || '피드백을 찾을 수 없습니다.'}
            </p>
          </div>
          <FeedbackCtaRow retryTo="/conversation/setup" homeTo="/learning" />
        </div>
      </LearnerLayout>
    )
  }

  if (feedback.generationStatus === 'FAILED') {
    return (
      <LearnerLayout>
        <div className="feedback-page">
          <FeedbackHeader title="일반 회화 완료" subtitle="대화가 종료되었습니다" badgeLabel="대화 완료" onBack={handleBack} />
          <div className="feedback-card">
            <p className="conversation-feedback-description">
              AI 피드백 생성에 실패했습니다. 잠시 후 다시 시도해주세요.
            </p>
          </div>
          <FeedbackCtaRow retryTo="/conversation/setup" homeTo="/learning" />
        </div>
      </LearnerLayout>
    )
  }

  if (feedback.generationStatus !== 'COMPLETED') {
    return (
      <LearnerLayout>
        <div className="feedback-page">
          <FeedbackHeader title="일반 회화 완료" subtitle="대화가 종료되었습니다" badgeLabel="대화 완료" onBack={handleBack} />
          <div className="feedback-card">
            <p className="conversation-feedback-description">피드백을 생성하고 있어요. 잠시만 기다려주세요.</p>
          </div>
          <FeedbackCtaRow retryTo="/conversation/setup" homeTo="/learning" />
        </div>
      </LearnerLayout>
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

  return (
    <LearnerLayout>
      <div className="feedback-page">
        <FeedbackHeader
          title="일반 회화 완료"
          subtitle="오늘 대화한 내용을 AI가 분석했어요"
          badgeLabel="대화 완료"
          onBack={handleBack}
        />

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

        <FeedbackCtaRow retryTo="/conversation/setup" homeTo="/learning" />
      </div>
    </LearnerLayout>
  )
}

export default ConversationFeedbackPage
