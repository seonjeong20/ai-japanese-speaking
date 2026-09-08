import { useNavigate } from 'react-router-dom'
import ManagerLayout from '../../components/layout/ManagerLayout'
import { BellIcon, ChevronRightSmallIcon, SparkleIcon } from '../../components/icons/DashboardIcons'
import { adminAlertsMock, managerKpiMock, managerProfileMock, weeklyOrgUsageMock } from '../../data/managerMock'
import '../../pages/Dashboard/DashboardPage.css'
import './ManagerDashboardPage.css'

function ManagerDashboardPage() {
  const navigate = useNavigate()

  const totalMinutes = weeklyOrgUsageMock.days.reduce((sum, day) => sum + day.minutes, 0)
  const averageMinutes = totalMinutes / weeklyOrgUsageMock.days.length
  const maxMinutes = Math.max(...weeklyOrgUsageMock.days.map((day) => day.minutes), 1)

  const pendingCount = adminAlertsMock.filter((alert) => alert.type === 'pending').length

  const goToPendingLearners = () => navigate('/manager/learners?status=pending')
  const goToAllLearners = () => navigate('/manager/learners')

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
          {managerKpiMock.map((kpi) => (
            <div key={kpi.id} className="manager-kpi-card">
              <p className="manager-kpi-card__label">{kpi.label}</p>
              <p className="manager-kpi-card__value">{kpi.value}</p>
              <p className="manager-kpi-card__helper">{kpi.helper}</p>
            </div>
          ))}
        </div>

        <div className="manager-dashboard-bottom-row">
          <div className="dashboard-card manager-weekly-usage-card">
            <div className="dashboard-card__header">
              <p className="dashboard-card__title dashboard-card__title--sm">주간 학습 시간 (조직 전체)</p>
              <span className="manager-weekly-usage-card__week-label">{weeklyOrgUsageMock.weekLabel}</span>
            </div>

            <div className="manager-weekly-usage-card__stat-row">
              <p className="manager-weekly-usage-card__total">{weeklyOrgUsageMock.totalLabel}</p>
              <p className="manager-weekly-usage-card__helper">{weeklyOrgUsageMock.totalHelper}</p>
            </div>

            <div className="weekly-study-card__chart manager-weekly-usage-card__chart">
              <div className="weekly-study-card__bars">
                {weeklyOrgUsageMock.days.map((day) => {
                  const heightPercent = Math.max((day.minutes / maxMinutes) * 100, 4)
                  const isAboveAverage = day.minutes >= averageMinutes
                  return (
                    <div className="weekly-study-card__bar-col" key={day.label}>
                      <div
                        className={`weekly-study-card__bar${isAboveAverage ? ' weekly-study-card__bar--lime' : ''}`}
                        style={{ height: `${heightPercent}%` }}
                      />
                    </div>
                  )
                })}
              </div>
              <div className="weekly-study-card__labels">
                {weeklyOrgUsageMock.days.map((day) => (
                  <span key={day.label}>{day.label}</span>
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
              {adminAlertsMock.map((alert) => (
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
            </ul>
          </div>
        </div>
      </div>
    </ManagerLayout>
  )
}

export default ManagerDashboardPage
