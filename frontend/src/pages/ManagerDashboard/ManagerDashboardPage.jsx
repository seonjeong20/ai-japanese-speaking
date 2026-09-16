import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import ManagerLayout from '../../components/layout/ManagerLayout'
import { BellIcon, ChevronRightSmallIcon, SparkleIcon } from '../../components/icons/DashboardIcons'
import { fetchManagedLearners, fetchManagerDashboard } from '../../api/manager'
import { managerProfileMock } from '../../data/managerMock'
import '../../pages/Dashboard/DashboardPage.css'
import './ManagerDashboardPage.css'

const WEEKDAY_LABELS = ['월', '화', '수', '목', '금', '토', '일']

const ALERT_LIST_LIMIT = 5

// 가입/승인/계정 상태와 관련된 관리 이벤트만 다룹니다.
// (학습 내용·Speaking 내용·AI Feedback·점수는 절대 포함하지 않습니다.)
function buildAlerts(learners) {
  const sorted = [...learners].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))

  return sorted.slice(0, ALERT_LIST_LIMIT).map((learner) => {
    const isPending = learner.status === 'PENDING'
    return {
      id: learner.userId,
      type: isPending ? 'pending' : 'account',
      title: isPending ? '새로운 가입 승인 요청이 있습니다.' : '새로운 학습자가 등록되었습니다.',
      meta: isPending ? `${learner.name} · 가입 승인 대기` : `${learner.name} · 계정 ${statusLabel(learner.status)}`,
      time: relativeTime(learner.createdAt),
    }
  })
}

function statusLabel(status) {
  switch (status) {
    case 'ACTIVE':
      return '활성'
    case 'INACTIVE':
      return '비활성'
    case 'REJECTED':
      return '거절'
    default:
      return status
  }
}

function relativeTime(isoString) {
  const diffMs = Date.now() - new Date(isoString).getTime()
  const minutes = Math.floor(diffMs / (60 * 1000))
  if (minutes < 1) return '방금 전'
  if (minutes < 60) return `${minutes}분 전`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}시간 전`
  const days = Math.floor(hours / 24)
  if (days === 1) return '어제'
  return `${days}일 전`
}

// 백엔드 weeklyUsage(date/studyMinutes)를 요일 라벨과 분 단위 막대 차트 데이터로 변환합니다.
function toWeeklyUsageView(weeklyUsage) {
  const days = weeklyUsage.map((day, index) => ({
    label: WEEKDAY_LABELS[index] ?? day.date,
    minutes: day.studyMinutes,
  }))

  const totalMinutes = days.reduce((sum, day) => sum + day.minutes, 0)
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60

  return {
    days,
    totalLabel: `${hours}시간 ${minutes}분`,
    totalHelper: '조직 전체 합산 기준',
    weekLabel: '이번 주',
  }
}

function ManagerDashboardPage() {
  const navigate = useNavigate()

  const [dashboard, setDashboard] = useState(null)
  const [alerts, setAlerts] = useState([])
  const [isLoading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  useEffect(() => {
    Promise.all([fetchManagerDashboard(), fetchManagedLearners()])
      .then(([dashboardResponse, learners]) => {
        setDashboard(dashboardResponse)
        setAlerts(buildAlerts(learners))
      })
      .catch((error) => setLoadError(error.message || '대시보드 정보를 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }, [])

  const goToPendingLearners = () => navigate('/manager/learners?status=PENDING')
  const goToAllLearners = () => navigate('/manager/learners')

  if (isLoading) {
    return (
      <ManagerLayout>
        <div className="dashboard-page">
          <p className="manager-table__empty">불러오는 중...</p>
        </div>
      </ManagerLayout>
    )
  }

  if (loadError || !dashboard) {
    return (
      <ManagerLayout>
        <div className="dashboard-page">
          <p className="manager-table__empty" role="alert">{loadError || '대시보드 정보를 불러오지 못했습니다.'}</p>
        </div>
      </ManagerLayout>
    )
  }

  const weeklyUsage = toWeeklyUsageView(dashboard.weeklyUsage)
  const averageMinutes = weeklyUsage.days.reduce((sum, day) => sum + day.minutes, 0) / weeklyUsage.days.length
  const maxMinutes = Math.max(...weeklyUsage.days.map((day) => day.minutes), 1)

  const kpis = [
    { id: 'learners', label: '소속 학습자 수', value: `${dashboard.learnerCount}명` },
    { id: 'usage', label: '이번 달 Speaking 이용량', value: `${dashboard.monthlySpeakingCount}회` },
    { id: 'avgTime', label: '평균 학습 시간', value: `${Math.round(dashboard.averageStudyMinutes)}분`, helper: '1인당 주간 평균' },
    { id: 'interview', label: '이번 달 면접 연습', value: `${dashboard.monthlyInterviewCount}회`, helper: '이번 달 기준' },
    { id: 'pendingLearners', label: '승인 대기 학습자 수', value: `${dashboard.pendingLearnerCount}명`, helper: '승인 필요' },
  ]

  const pendingCount = alerts.filter((alert) => alert.type === 'pending').length

  return (
    <ManagerLayout>
      <div className="dashboard-page">
        <div className="dashboard-topbar">
          <div className="dashboard-topbar__greeting">
            <div className="dashboard-topbar__greeting-row">
              <h1>안녕하세요, {managerProfileMock.name} 매니저님!</h1>
              <SparkleIcon size={20} className="dashboard-topbar__sparkle" />
            </div>
            <p>우리 조직의 학습 현황을 한눈에 확인해보세요.</p>
          </div>

          <div className="dashboard-topbar__actions">
            <button type="button" className="dashboard-topbar__icon-button" aria-label="알림">
              <BellIcon size={18} />
            </button>
          </div>
        </div>

        <div className="manager-kpi-row">
          {kpis.map((kpi) => (
            <div key={kpi.id} className="manager-kpi-card">
              <p className="manager-kpi-card__label">{kpi.label}</p>
              <p className="manager-kpi-card__value">{kpi.value}</p>
              {kpi.helper && <p className="manager-kpi-card__helper">{kpi.helper}</p>}
            </div>
          ))}
        </div>

        <div className="manager-dashboard-bottom-row">
          <div className="dashboard-card manager-weekly-usage-card">
            <div className="dashboard-card__header">
              <p className="dashboard-card__title dashboard-card__title--sm">주간 학습 시간 (조직 전체)</p>
              <span className="manager-weekly-usage-card__week-label">{weeklyUsage.weekLabel}</span>
            </div>

            <div className="manager-weekly-usage-card__stat-row">
              <p className="manager-weekly-usage-card__total">{weeklyUsage.totalLabel}</p>
              <p className="manager-weekly-usage-card__helper">{weeklyUsage.totalHelper}</p>
            </div>

            <div className="weekly-study-card__chart manager-weekly-usage-card__chart">
              <div className="weekly-study-card__bars">
                {weeklyUsage.days.map((day, index) => {
                  const heightPercent = Math.max((day.minutes / maxMinutes) * 100, 4)
                  const isAboveAverage = day.minutes >= averageMinutes
                  return (
                    <div className="weekly-study-card__bar-col" key={`${day.label}-${index}`}>
                      <div
                        className={`weekly-study-card__bar${isAboveAverage ? ' weekly-study-card__bar--lime' : ''}`}
                        style={{ height: `${heightPercent}%` }}
                      />
                    </div>
                  )
                })}
              </div>
              <div className="weekly-study-card__labels">
                {weeklyUsage.days.map((day, index) => (
                  <span key={`${day.label}-${index}`}>{day.label}</span>
                ))}
              </div>
            </div>
          </div>

          <div className="dashboard-card manager-alerts-card">
            <div className="dashboard-card__header">
              <div className="manager-alerts-card__title-group">
                <p className="dashboard-card__title">관리 알림</p>
                {pendingCount > 0 && <span className="manager-alerts-card__count">{pendingCount}</span>}
              </div>
              <button type="button" className="manager-alerts-card__view-all" onClick={goToAllLearners}>
                전체보기
                <ChevronRightSmallIcon size={14} />
              </button>
            </div>

            <ul className="manager-alerts-card__list">
              {alerts.map((alert) => (
                <li key={alert.id} className="manager-alert">
                  <span
                    className={`manager-alert__badge${alert.type === 'pending' ? ' manager-alert__badge--pending' : ''}`}
                    aria-hidden="true"
                  />
                  <div className="manager-alert__text">
                    <p className="manager-alert__title">{alert.title}</p>
                    <p className="manager-alert__meta">{alert.meta}</p>
                    <p className="manager-alert__time">{alert.time}</p>
                  </div>
                  {alert.type === 'pending' && (
                    <button type="button" className="manager-alert__action" onClick={goToPendingLearners}>
                      확인하기
                    </button>
                  )}
                </li>
              ))}

              {alerts.length === 0 && <p className="manager-table__empty">최근 알림이 없어요.</p>}
            </ul>
          </div>
        </div>
      </div>
    </ManagerLayout>
  )
}

export default ManagerDashboardPage
