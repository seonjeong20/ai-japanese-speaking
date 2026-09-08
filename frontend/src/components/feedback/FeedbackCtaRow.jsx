import { useNavigate } from 'react-router-dom'
import { RefreshIcon } from '../icons/DashboardIcons'

function FeedbackCtaRow({ retryTo, homeTo = '/learning' }) {
  const navigate = useNavigate()

  return (
    <div className="feedback-cta-row">
      <button type="button" className="feedback-cta-row__retry" onClick={() => navigate(retryTo)}>
        <RefreshIcon size={16} />
        다시 연습하기
      </button>
      <button type="button" className="feedback-cta-row__home" onClick={() => navigate(homeTo)}>
        학습 홈으로
      </button>
    </div>
  )
}

export default FeedbackCtaRow
