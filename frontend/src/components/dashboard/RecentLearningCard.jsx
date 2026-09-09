import { Link } from 'react-router-dom'
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
        <Link to="/history" className="recent-learning-card__view-all">
          전체보기
          <ChevronRightSmallIcon size={14} />
        </Link>
      </header>

      <ul className="recent-learning-card__list">
        {items.map((item) => {
          const style = TYPE_STYLES[item.type]
          const Icon = style.icon
          return (
            <li key={item.id} className="recent-item">
              {/* sessionId/id는 historyMock의 항목과 동일한 키를 공유하므로 그대로
                  History Detail 라우트에 사용한다. Backend 연동 시 이 id가
                  GET /api/history/{sessionId}에 대응하는 실제 sessionId가 된다. */}
              <Link to={`/history/${item.id}`} className="recent-item__link">
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
              </Link>
            </li>
          )
        })}
      </ul>
    </section>
  )
}

export default RecentLearningCard
