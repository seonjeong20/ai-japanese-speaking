import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { BellIcon, BriefcaseIcon, ChatIcon, ChevronRightSmallIcon, SparkleIcon } from '../../components/icons/DashboardIcons'
import { fetchAdminDashboard } from '../../api/admin'

const WEEKDAY_LABELS = ['월', '화', '수', '목', '금', '토', '일']

function formatRegisteredAt(value) {
  const date = new Date(value)
  return `${date.getMonth() + 1}월 ${date.getDate()}일`
}

// 백엔드 weeklyUsage(date/sessionCount)를 요일 라벨과 상대 높이(%) 막대 차트로 변환합니다.
function toWeeklyUsageView(weeklyUsage, weeklyTotalCount) {
  const maxCount = Math.max(...weeklyUsage.map((day) => day.sessionCount), 1)
  const days = weeklyUsage.map((day, index) => ({
    label: WEEKDAY_LABELS[index] ?? day.date,
    height: `${Math.max((day.sessionCount / maxCount) * 100, 4)}%`,
    highlighted: day.sessionCount === maxCount && maxCount > 0,
  }))
  return { days, total: `${weeklyTotalCount}회` }
}

export default function AdminDashboardPage() {
  const [dashboard, setDashboard] = useState(null)
  const [isLoading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  useEffect(() => {
    fetchAdminDashboard()
      .then(setDashboard)
      .catch((error) => setLoadError(error.message || '대시보드 정보를 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }, [])

  if (isLoading) {
    return <section className="admin-page admin-dashboard"><p className="admin-empty">불러오는 중...</p></section>
  }
  if (loadError || !dashboard) {
    return <section className="admin-page admin-dashboard"><p className="admin-empty" role="alert">{loadError || '대시보드 정보를 불러오지 못했습니다.'}</p></section>
  }

  const kpis = [
    { label: '고객 기관 수', value: `${dashboard.organizationCount}개` },
    { label: '전체 사용자 수', value: `${dashboard.totalUserCount}명` },
    { label: '이번 달 Speaking 이용량', value: `${dashboard.monthlySpeakingCount}회` },
    { label: '월간 활성 사용자', value: `${dashboard.monthlyActiveUserCount}명`, helper: `전체 대비 ${dashboard.totalUserCount ? Math.round((dashboard.monthlyActiveUserCount / dashboard.totalUserCount) * 100) : 0}%` },
    { label: '승인 대기 Manager 수', value: `${dashboard.pendingManagerCount}명`, helper: '승인 필요' },
  ]
  const weeklyUsage = toWeeklyUsageView(dashboard.weeklyUsage, dashboard.weeklyTotalCount)
  const recent = dashboard.recentOrganizations

  return <section className="admin-page admin-dashboard">
    <header className="admin-header admin-dashboard-header"><div><h1>안녕하세요, 시스템 관리자님! <SparkleIcon size={20} /></h1><p>전체 서비스 운영 현황을 한눈에 확인해보세요.</p></div><Link className="admin-bell" to="/admin/managers" aria-label="담당자 승인 및 계정 관리"><BellIcon size={18} /></Link></header>
    <div className="admin-kpis">{kpis.map((kpi) => <article className="admin-kpi" key={kpi.label}><p>{kpi.label}</p><strong>{kpi.value}</strong>{kpi.helper && <small>{kpi.helper}</small>}</article>)}</div>
    <div className="admin-dashboard-panels">
      <article className="admin-panel admin-usage" aria-label="플랫폼 전체 주간 Speaking 이용량"><header><h2>주간 Speaking 이용량 (전체 플랫폼)</h2><span>이번 주</span></header><div className="admin-usage-total"><strong>{weeklyUsage.total}</strong><span>전체 고객사 합산 기준</span></div>
        <div className="admin-chart" role="img" aria-label="월요일부터 일요일까지의 상대 이용량.">{weeklyUsage.days.map((day) => <div className="admin-chart-column" key={day.label}><div className="admin-chart-track"><span style={{ height: day.height }} className={day.highlighted ? 'admin-chart-highlighted' : ''} /></div><span>{day.label}</span></div>)}</div>
      </article>
      <article className="admin-panel admin-recent"><header><h2>최근 등록 고객사</h2><Link to="/admin/customers">전체보기 <ChevronRightSmallIcon size={14} /></Link></header><ul>{recent.map((row, index) => <li key={row.id}><Link to="/admin/customers"><span className={`admin-recent-icon${index % 2 === 0 ? ' admin-recent-icon--lime' : ''}`}>{index % 2 === 0 ? <ChatIcon size={20} /> : <BriefcaseIcon size={20} />}</span><span className="admin-recent-text"><small>신규 등록</small><strong>{row.name}</strong><span>{formatRegisteredAt(row.createdAt)} · 학습자 {row.learnerCount}명</span></span><ChevronRightSmallIcon size={18} /></Link></li>)}
        {!recent.length && <li className="admin-empty">등록된 기관이 없습니다.</li>}
      </ul></article>
    </div>
  </section>
}
