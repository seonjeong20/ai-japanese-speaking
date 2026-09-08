import { Link, useOutletContext } from 'react-router-dom'
import { BellIcon, BriefcaseIcon, ChatIcon, ChevronRightSmallIcon, SparkleIcon } from '../../components/icons/DashboardIcons'
import { adminKpiMock, adminRecentCustomersMock, adminWeeklyUsageMock } from '../../data/adminMock'

export default function AdminDashboardPage() {
  const { customers } = useOutletContext()
  const recent = [...customers.filter((row) => row.createdInSession), ...adminRecentCustomersMock].slice(0, 4)
  return <section className="admin-page admin-dashboard">
    <header className="admin-header admin-dashboard-header"><div><h1>안녕하세요, 시스템 관리자님! <SparkleIcon size={20} /></h1><p>전체 서비스 운영 현황을 한눈에 확인해보세요.</p></div><Link className="admin-bell" to="/admin/managers" aria-label="담당자 승인 및 계정 관리"><BellIcon size={18} /></Link></header>
    <div className="admin-kpis">{adminKpiMock.map((kpi) => <article className="admin-kpi" key={kpi.label}><p>{kpi.label}</p><strong>{kpi.value}</strong><small>{kpi.helper}</small></article>)}</div>
    <div className="admin-dashboard-panels">
      <article className="admin-panel admin-usage" aria-label="플랫폼 전체 주간 Speaking 이용량"><header><h2>주간 Speaking 이용량 (전체 플랫폼)</h2><span>이번 주</span></header><div className="admin-usage-total"><strong>{adminWeeklyUsageMock.total}</strong><span>전체 고객사 합산 기준</span></div>
        <div className="admin-chart" role="img" aria-label="월요일부터 일요일까지의 상대 이용량. 금요일이 가장 높고, 일요일이 가장 낮습니다.">{adminWeeklyUsageMock.days.map((day) => <div className="admin-chart-column" key={day.label}><div className="admin-chart-track"><span style={{ height: day.height }} className={day.highlighted ? 'admin-chart-highlighted' : ''} /></div><span>{day.label}</span></div>)}</div>
      </article>
      <article className="admin-panel admin-recent"><header><h2>최근 등록 고객사</h2><Link to="/admin/customers">전체보기 <ChevronRightSmallIcon size={14} /></Link></header><ul>{recent.map((row, index) => <li key={row.id}><Link to="/admin/customers"><span className={`admin-recent-icon${index % 2 === 0 ? ' admin-recent-icon--lime' : ''}`}>{index % 2 === 0 ? <ChatIcon size={20} /> : <BriefcaseIcon size={20} />}</span><span className="admin-recent-text"><small>신규 등록</small><strong>{row.name}</strong><span>{row.registeredAt} · 학습자 {row.learnerCount}명</span></span><ChevronRightSmallIcon size={18} /></Link></li>)}</ul></article>
    </div>
  </section>
}
