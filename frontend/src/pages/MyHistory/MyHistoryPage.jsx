import { useState } from 'react'
import { Link } from 'react-router-dom'
import LearnerLayout from '../../components/layout/LearnerLayout'
import { BriefcaseIcon, ChatIcon, ChevronRightIcon } from '../../components/icons/DashboardIcons'
import { historyMock } from '../../data/historyMock'
import { SessionType } from '../../data/enums'
import './MyHistoryPage.css'

const FILTERS = [
  { id: 'all', label: '전체' },
  { id: SessionType.CONVERSATION, label: '일반 회화' },
  { id: SessionType.INTERVIEW, label: '면접 회화' },
]

const TYPE_META = {
  [SessionType.CONVERSATION]: { label: '일반 회화', icon: ChatIcon, badgeClass: 'my-history-item__icon-badge--lime' },
  [SessionType.INTERVIEW]: { label: '면접 회화', icon: BriefcaseIcon, badgeClass: 'my-history-item__icon-badge--gray' },
}

function MyHistoryPage() {
  const [filter, setFilter] = useState('all')

  const items = filter === 'all' ? historyMock : historyMock.filter((item) => item.type === filter)

  return (
    <LearnerLayout>
      <div className="my-history-page">
        <div className="my-history-page__header">
          <h1 className="my-history-page__title">My History</h1>
          <p className="my-history-page__subtitle">지금까지의 학습 기록을 확인해보세요</p>
        </div>

        <div className="my-history-tabs" role="tablist" aria-label="학습 유형 필터">
          {FILTERS.map((tab) => {
            const isActive = tab.id === filter
            return (
              <button
                key={tab.id}
                type="button"
                role="tab"
                aria-selected={isActive}
                className={`my-history-tabs__tab${isActive ? ' my-history-tabs__tab--active' : ''}`}
                onClick={() => setFilter(tab.id)}
              >
                {tab.label}
              </button>
            )
          })}
        </div>

        <div className="my-history-list">
          {items.map((item) => {
            const meta = TYPE_META[item.type]
            const Icon = meta.icon
            return (
              <Link key={item.id} to={`/history/${item.id}`} className="my-history-item">
                <span className={`my-history-item__icon-badge ${meta.badgeClass}`}>
                  <Icon size={18} />
                </span>

                <div className="my-history-item__text">
                  <p className="my-history-item__type">{meta.label}</p>
                  <p className="my-history-item__title">{item.title}</p>
                  <p className="my-history-item__meta">
                    {item.date} · {item.duration}분
                  </p>
                </div>

                <span className="my-history-item__badge">평가 보기</span>
                <ChevronRightIcon size={18} className="my-history-item__chevron" />
              </Link>
            )
          })}

          {items.length === 0 && <p className="my-history-empty">아직 학습 기록이 없어요.</p>}
        </div>
      </div>
    </LearnerLayout>
  )
}

export default MyHistoryPage
