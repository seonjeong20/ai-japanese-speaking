import { BackArrowIcon, CheckSmallIcon } from '../icons/DashboardIcons'

function FeedbackHeader({ title, subtitle, badgeLabel, onBack }) {
  return (
    <div className="feedback-header">
      <button type="button" className="setup-page__back" onClick={onBack} aria-label="뒤로 가기">
        <BackArrowIcon size={18} />
      </button>

      <div className="feedback-header__title-block">
        <div className="feedback-header__title-row">
          <p className="feedback-header__title">{title}</p>
          <span className="feedback-header__badge">
            <CheckSmallIcon size={12} />
            {badgeLabel}
          </span>
        </div>
        <p className="feedback-header__subtitle">{subtitle}</p>
      </div>
    </div>
  )
}

export default FeedbackHeader
