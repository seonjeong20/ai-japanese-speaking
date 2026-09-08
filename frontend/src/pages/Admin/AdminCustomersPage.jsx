import { useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import { AccountAction, AdminHeader, AdminPagination, AdminToolbar, StatusBadge } from '../../components/admin/AdminUI'
import AdminRegistrationModal from '../../components/admin/AdminRegistrationModal'

export default function AdminCustomersPage() {
  const { customers, managers, addCustomer, changeCustomerStatus } = useOutletContext()
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('ALL')
  const [page, setPage] = useState(1)
  const [modal, setModal] = useState(false)
  const [notice, setNotice] = useState('')
  const query = search.trim().toLowerCase()
  const filtered = customers.filter((row) => (status === 'ALL' || row.status === status) && [row.name, row.email].some((value) => value.toLowerCase().includes(query)))
  const currentPage = Math.min(page, Math.max(1, Math.ceil(filtered.length / 8)))
  const register = (values) => { addCustomer(values); setSearch(''); setStatus('ALL'); setPage(1); setNotice(`${values.name} 고객사를 등록했습니다.`) }
  return <section className="admin-page">
    <AdminHeader title="고객사 관리" subtitle="전체 고객사를 조회하고 관리하세요" action={<button className="admin-button admin-primary" onClick={() => setModal(true)}>+ 고객사 등록</button>} />
    <AdminToolbar search={search} onSearch={(value) => { setSearch(value); setPage(1) }} status={status} onStatus={(value) => { setStatus(value); setPage(1) }} />
    <div className="admin-table-card"><table className="admin-table admin-table--customers"><caption className="admin-sr-only">고객사 목록</caption><thead><tr>{['회사명', '담당자 이메일', '계약일', '소속 학습자 수', '요금제', '상태', '관리'].map((label) => <th key={label} scope="col">{label}</th>)}</tr></thead><tbody>
      {filtered.slice((currentPage - 1) * 8, currentPage * 8).map((row) => <tr key={row.id}>
        <th scope="row"><span className="admin-name"><span className="admin-avatar">{row.name[0]}</span>{row.name}</span></th><td>{row.email}</td><td>{row.contractStart.replaceAll('-', '.')}</td><td className="admin-dark">{row.learnerCount}명</td><td>{row.plan}</td><td><StatusBadge status={row.status} /></td><td><AccountAction name={row.name} status={row.status} onChange={(value) => { changeCustomerStatus(row.id, value); setNotice(`${row.name}의 서비스 상태를 변경했습니다.`) }} /></td>
      </tr>)}
      {!filtered.length && <tr><td colSpan={7} className="admin-empty">조건에 맞는 고객사가 없습니다.</td></tr>}
    </tbody></table></div>
    <AdminPagination count={filtered.length} page={currentPage} onPage={setPage} unit="개사" />
    <p role="status" className="admin-notice">{notice}</p>
    {modal && <AdminRegistrationModal kind="customer" customers={customers} managers={managers} onClose={() => setModal(false)} onSubmit={register} />}
  </section>
}
