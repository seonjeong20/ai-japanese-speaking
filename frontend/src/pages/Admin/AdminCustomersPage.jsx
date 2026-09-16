import { useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import { AccountAction, AdminHeader, AdminPagination, AdminToolbar, StatusBadge } from '../../components/admin/AdminUI'
import AdminRegistrationModal from '../../components/admin/AdminRegistrationModal'

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`
}

export default function AdminCustomersPage() {
  const { customers, isLoading, loadError, reloadCustomers, addCustomer, renameCustomer, changeCustomerStatus } = useOutletContext()
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('ALL')
  const [page, setPage] = useState(1)
  const [modal, setModal] = useState(false)
  const [editing, setEditing] = useState(null)
  const [notice, setNotice] = useState('')
  const query = search.trim().toLowerCase()
  const filtered = customers.filter((row) => (status === 'ALL' || row.status === status) && row.name.toLowerCase().includes(query))
  const currentPage = Math.min(page, Math.max(1, Math.ceil(filtered.length / 8)))
  const register = async (values) => {
    await addCustomer(values)
    setSearch('')
    setStatus('ALL')
    setPage(1)
    setNotice(`${values.name} 기관을 등록했습니다.`)
  }
  const rename = async (values) => {
    await renameCustomer(editing.id, values.name)
    setNotice(`기관명을 "${values.name}"(으)로 수정했습니다.`)
  }
  const changeStatus = async (row, value) => {
    try {
      await changeCustomerStatus(row.id, value)
      setNotice(`${row.name}의 상태를 변경했습니다.`)
    } catch (error) {
      setNotice(error.message || '상태를 변경하지 못했습니다.')
    }
  }
  return <section className="admin-page">
    <AdminHeader title="기관 관리" subtitle="전체 기관을 조회하고 관리하세요" action={<button className="admin-button admin-primary" onClick={() => setModal(true)}>+ 기관 등록</button>} />
    <AdminToolbar search={search} onSearch={(value) => { setSearch(value); setPage(1) }} status={status} onStatus={(value) => { setStatus(value); setPage(1) }} />
    {loadError && <p role="alert" className="admin-notice">{loadError} <button type="button" onClick={reloadCustomers}>다시 시도</button></p>}
    <div className="admin-table-card"><table className="admin-table admin-table--customers"><caption className="admin-sr-only">기관 목록</caption><thead><tr>{['기관명', '소속 학습자 수', '상태', '등록일', '관리'].map((label) => <th key={label} scope="col">{label}</th>)}</tr></thead><tbody>
      {filtered.slice((currentPage - 1) * 8, currentPage * 8).map((row) => <tr key={row.id}>
        <th scope="row"><span className="admin-name"><span className="admin-avatar">{row.name[0]}</span>{row.name}</span></th><td className="admin-dark">{row.learnerCount}명</td><td><StatusBadge status={row.status} /></td><td>{formatDate(row.createdAt)}</td><td><div className="admin-row-actions"><button type="button" className="admin-small" onClick={() => setEditing(row)}>기관명 수정</button><AccountAction name={row.name} status={row.status} onChange={(value) => changeStatus(row, value)} /></div></td>
      </tr>)}
      {isLoading && <tr><td colSpan={5} className="admin-empty">불러오는 중...</td></tr>}
      {!isLoading && !filtered.length && <tr><td colSpan={5} className="admin-empty">조건에 맞는 기관이 없습니다.</td></tr>}
    </tbody></table></div>
    <AdminPagination count={filtered.length} page={currentPage} onPage={setPage} unit="개" />
    <p role="status" className="admin-notice">{notice}</p>
    {modal && <AdminRegistrationModal customers={customers} onClose={() => setModal(false)} onSubmit={register} />}
    {editing && <AdminRegistrationModal mode="edit" initialValues={editing} customers={customers} onClose={() => setEditing(null)} onSubmit={rename} />}
  </section>
}
