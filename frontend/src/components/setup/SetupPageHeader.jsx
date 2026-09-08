import { BackArrowIcon } from '../icons/DashboardIcons'

function SetupPageHeader({ title, subtitle, onBack, backLabel = 'Learning으로 돌아가기' }) {
  return (
    <div className="setup-page__header">
      <button type="button" className="setup-page__back" onClick={onBack} aria-label={backLabel}>
        <BackArrowIcon size={18} />
      </button>

      <div className="setup-page__title-block">
        <h1 className="setup-page__title">{title}</h1>
        <p className="setup-page__subtitle">{subtitle}</p>
      </div>
    </div>
  )
}

export default SetupPageHeader
