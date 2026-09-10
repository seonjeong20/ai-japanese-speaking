import { useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import { AccountAction, AdminHeader, AdminPagination, AdminToolbar, StatusBadge } from '../../components/admin/AdminUI'
import { adminStatusLabels } from '../../data/adminMock'

export default function AdminManagersPage() {
  const { customers, managers, changeManagerStatus } = useOutletContext()
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('ALL')
  const [page, setPage] = useState(1)
  const [notice, setNotice] = useState('')
  const companyName = (id) => customers.find((row) => row.id === id)?.name ?? '—'
  const query = search.trim().toLowerCase()
  const filtered = managers.filter((row) => (status === 'ALL' || row.status === status) && [row.name, row.email, companyName(row.customerId)].some((value) => value.toLowerCase().includes(query)))
  const currentPage = Math.min(page, Math.max(1, Math.ceil(filtered.length / 8)))
  const selectStatus = (value) => { setStatus(value); setPage(1) }
  return <section className="admin-page">
    <AdminHeader title="담당자 관리" subtitle="기관별 Manager 계정을 조회하고 관리하세요" />
    <div className="admin-status-tabs" role="group" aria-label="담당자 상태별 보기">{Object.entries(adminStatusLabels).map(([key, label]) => <button key={key} aria-pressed={status === key} onClick={() => selectStatus(key)}>{label}</button>)}</div>
    <AdminToolbar managers search={search} onSearch={(value) => { setSearch(value); setPage(1) }} status={status} onStatus={selectStatus} />
    <div className="admin-table-card"><table className="admin-table admin-table--managers"><caption className="admin-sr-only">담당자 목록</caption><thead><tr>{['이름', '이메일', '소속 기관', '부서', '가입일', '최근 로그인', '상태', '관리'].map((label) => <th key={label} scope="col">{label}</th>)}</tr></thead><tbody>
      {filtered.slice((currentPage - 1) * 8, currentPage * 8).map((row) => <tr key={row.id}>
        <th scope="row"><span className="admin-name"><span className="admin-avatar">{row.name[0]}</span>{row.name}</span></th><td>{row.email}</td><td>{companyName(row.customerId)}</td><td>{row.department ?? '—'}</td><td>{row.joinedAt ? row.joinedAt.replaceAll('-', '.') : '—'}</td><td>{row.lastLogin ?? '—'}</td><td><StatusBadge status={row.status} /></td><td><AccountAction name={row.name} status={row.status} onChange={(value) => { changeManagerStatus(row.id, value); setNotice(`${row.name}: ${adminStatusLabels[value]} 상태로 변경했습니다.`) }} /></td>
      </tr>)}
      {!filtered.length && <tr><td colSpan={8} className="admin-empty">조건에 맞는 담당자가 없습니다.</td></tr>}
    </tbody></table></div>
    <AdminPagination count={filtered.length} page={currentPage} onPage={setPage} unit="명" />
    <p role="status" className="admin-notice">{notice}</p>
  </section>
}
