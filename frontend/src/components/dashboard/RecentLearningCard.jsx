import { BriefcaseIcon, ChatIcon, ChevronRightIcon, ChevronRightSmallIcon } from '../icons/DashboardIcons'

const TYPE_STYLES = {
  '일반 회화': { icon: ChatIcon, badgeClass: 'recent-item__icon-badge--lime' },
  '면접 회화': { icon: BriefcaseIcon, badgeClass: 'recent-item__icon-badge--gray' },
}

function RecentLearningCard({ items }) {
  return (
    <section className="dashboard-card recent-learning-card">
      <header className="dashboard-card__header">
        <h2 className="dashboard-card__title">최근 학습 내역</h2>
        <button type="button" className="recent-learning-card__view-all">
          전체보기
          <ChevronRightSmallIcon size={14} />
        </button>
      </header>

      <ul className="recent-learning-card__list">
        {items.map((item) => {
          const style = TYPE_STYLES[item.type]
          const Icon = style.icon
          return (
            <li key={item.id} className="recent-item">
              <span className={`recent-item__icon-badge ${style.badgeClass}`}>
                <Icon size={20} />
              </span>
              <div className="recent-item__text">
                <p className="recent-item__type">{item.type}</p>
                <p className="recent-item__title">{item.title}</p>
                <p className="recent-item__meta">
                  {item.date} · {item.duration}분
                </p>
              </div>
              <ChevronRightIcon size={18} className="recent-item__arrow" />
            </li>
          )
        })}
      </ul>
    </section>
  )
}

export default RecentLearningCard
