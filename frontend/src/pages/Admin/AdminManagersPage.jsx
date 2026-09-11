import { useEffect, useState } from 'react'
import { AccountAction, AdminHeader, AdminPagination, AdminToolbar, StatusBadge } from '../../components/admin/AdminUI'
import { adminStatusLabels } from '../../data/adminMock'
import { activateManager, approveManager, deactivateManager, fetchManagers, rejectManager } from '../../api/admin'

// 백엔드 응답(userId/organization/createdAt/lastLoginAt)을 기존 화면이 쓰던 필드 이름으로 맞춥니다.
function toViewModel(item) {
  const createdAt = new Date(item.createdAt)
  const lastLoginAt = item.lastLoginAt ? new Date(item.lastLoginAt) : null
  return {
    id: item.userId,
    name: item.name,
    email: item.email,
    department: item.department,
    organizationName: item.organization?.name ?? '—',
    joinedAt: `${createdAt.getFullYear()}.${String(createdAt.getMonth() + 1).padStart(2, '0')}.${String(createdAt.getDate()).padStart(2, '0')}`,
    lastLogin: lastLoginAt ? `${lastLoginAt.getMonth() + 1}월 ${lastLoginAt.getDate()}일` : null,
    status: item.status,
  }
}

function resolveAction(row, nextStatus) {
  if (nextStatus === 'ACTIVE') return row.status === 'PENDING' ? approveManager : activateManager
  if (nextStatus === 'REJECTED') return rejectManager
  if (nextStatus === 'INACTIVE') return deactivateManager
  return null
}

export default function AdminManagersPage() {
  const [managers, setManagers] = useState([])
  const [isLoading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('ALL')
  const [page, setPage] = useState(1)
  const [notice, setNotice] = useState('')

  const fetchAndSetManagers = () =>
    fetchManagers()
      .then((items) => setManagers(items.map(toViewModel)))
      .catch((error) => setLoadError(error.message || '담당자 목록을 불러오지 못했습니다.'))
      .finally(() => setLoading(false))

  useEffect(() => {
    fetchAndSetManagers()
  }, [])

  const loadManagers = () => {
    setLoading(true)
    setLoadError('')
    fetchAndSetManagers()
  }

  const query = search.trim().toLowerCase()
  const filtered = managers.filter(
    (row) =>
      (status === 'ALL' || row.status === status) &&
      [row.name, row.email, row.organizationName].some((value) => value.toLowerCase().includes(query)),
  )
  const currentPage = Math.min(page, Math.max(1, Math.ceil(filtered.length / 8)))
  const selectStatus = (value) => {
    setStatus(value)
    setPage(1)
  }

  const handleChangeStatus = async (row, nextStatus) => {
    const action = resolveAction(row, nextStatus)
    if (!action) return
    try {
      await action(row.id)
      setManagers((prev) => prev.map((item) => (item.id === row.id ? { ...item, status: nextStatus } : item)))
      setNotice(`${row.name}: ${adminStatusLabels[nextStatus]} 상태로 변경했습니다.`)
    } catch (error) {
      setNotice(error.message || '요청을 처리하지 못했습니다.')
    }
  }

  return (
    <section className="admin-page">
      <AdminHeader title="담당자 관리" subtitle="기관별 Manager 계정을 조회하고 관리하세요" />
      <div className="admin-status-tabs" role="group" aria-label="담당자 상태별 보기">
        {Object.entries(adminStatusLabels).map(([key, label]) => (
          <button key={key} aria-pressed={status === key} onClick={() => selectStatus(key)}>
            {label}
          </button>
        ))}
      </div>
      <AdminToolbar
        managers
        search={search}
        onSearch={(value) => {
          setSearch(value)
          setPage(1)
        }}
        status={status}
        onStatus={selectStatus}
      />
      {loadError && (
        <p role="alert" className="admin-notice">
          {loadError}{' '}
          <button type="button" onClick={loadManagers}>
            다시 시도
          </button>
        </p>
      )}
      <div className="admin-table-card">
        <table className="admin-table admin-table--managers">
          <caption className="admin-sr-only">담당자 목록</caption>
          <thead>
            <tr>
              {['이름', '이메일', '소속 기관', '부서', '가입일', '최근 로그인', '상태', '관리'].map((label) => (
                <th key={label} scope="col">
                  {label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {filtered.slice((currentPage - 1) * 8, currentPage * 8).map((row) => (
              <tr key={row.id}>
                <th scope="row">
                  <span className="admin-name">
                    <span className="admin-avatar">{row.name[0]}</span>
                    {row.name}
                  </span>
                </th>
                <td>{row.email}</td>
                <td>{row.organizationName}</td>
                <td>{row.department ?? '—'}</td>
                <td>{row.joinedAt}</td>
                <td>{row.lastLogin ?? '—'}</td>
                <td>
                  <StatusBadge status={row.status} />
                </td>
                <td>
                  <AccountAction name={row.name} status={row.status} onChange={(value) => handleChangeStatus(row, value)} />
                </td>
              </tr>
            ))}
            {isLoading && (
              <tr>
                <td colSpan={8} className="admin-empty">
                  불러오는 중...
                </td>
              </tr>
            )}
            {!isLoading && !filtered.length && (
              <tr>
                <td colSpan={8} className="admin-empty">
                  조건에 맞는 담당자가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      <AdminPagination count={filtered.length} page={currentPage} onPage={setPage} unit="명" />
      <p role="status" className="admin-notice">
        {notice}
      </p>
    </section>
  )
}
