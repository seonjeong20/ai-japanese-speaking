import { ArrowRightIcon, ModeBriefcaseIcon, ModeChatIcon } from '../icons/DashboardIcons'

const TYPE_STYLES = {
  conversation: { icon: ModeChatIcon, badgeClass: 'learning-mode-card__badge--lime' },
  interview: { icon: ModeBriefcaseIcon, badgeClass: 'learning-mode-card__badge--gray' },
}

function LearningModeCard({ title, description, type, onStart }) {
  const style = TYPE_STYLES[type]
  const Icon = style.icon

  return (
    <article className="learning-mode-card">
      <span className={`learning-mode-card__badge ${style.badgeClass}`}>
        <Icon size={34} />
      </span>

      <h2 className="learning-mode-card__title">{title}</h2>
      <p className="learning-mode-card__description">{description}</p>

      <div className="learning-mode-card__spacer" />

      <button type="button" className="learning-mode-card__cta" onClick={onStart}>
        시작하기
        <ArrowRightIcon size={16} className="learning-mode-card__cta-arrow" />
      </button>
    </article>
  )
}

export default LearningModeCard
