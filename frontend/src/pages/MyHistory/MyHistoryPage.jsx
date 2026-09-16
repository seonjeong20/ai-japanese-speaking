import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import LearnerLayout from '../../components/layout/LearnerLayout'
import { BriefcaseIcon, ChatIcon, ChevronRightIcon } from '../../components/icons/DashboardIcons'
import { getMyHistory } from '../../api/history'
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

function MyHistoryPage() {
  const [filter, setFilter] = useState('all')
  const [history, setHistory] = useState([])
  const [isLoading, setLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    let cancelled = false

    async function loadHistory() {
      setLoading(true)
      setErrorMessage('')
      try {
        const data = await getMyHistory()
        if (!cancelled) setHistory(data?.items ?? [])
      } catch (error) {
        if (!cancelled) setErrorMessage(error.message || '학습 기록을 불러오지 못했습니다.')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    loadHistory()
    return () => {
      cancelled = true
    }
  }, [])

  const items =
    filter === 'all' ? history : history.filter((item) => item.sessionType === filter)

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

        {isLoading && <p className="my-history-empty">학습 기록을 불러오는 중이에요...</p>}

        {!isLoading && errorMessage && <p className="my-history-empty">{errorMessage}</p>}

        {!isLoading && !errorMessage && (
          <div className="my-history-list">
            {items.map((item) => {
              const meta = TYPE_META[item.sessionType]
              const Icon = meta.icon
              return (
                <Link key={item.sessionId} to={`/history/${item.sessionId}`} className="my-history-item">
                  <span className={`my-history-item__icon-badge ${meta.badgeClass}`}>
                    <Icon size={18} />
                  </span>

                  <div className="my-history-item__text">
                    <p className="my-history-item__type">{meta.label}</p>
                    <p className="my-history-item__title">{item.title}</p>
                    <p className="my-history-item__meta">
                      {formatDate(item.startedAt)} · {formatDurationMinutes(item.durationSeconds)}분
                    </p>
                  </div>

                  <span className="my-history-item__badge">평가 보기</span>
                  <ChevronRightIcon size={18} className="my-history-item__chevron" />
                </Link>
              )
            })}

            {items.length === 0 && <p className="my-history-empty">아직 학습 기록이 없어요.</p>}
          </div>
        )}
      </div>
    </LearnerLayout>
  )
}

export default MyHistoryPage
