import { useEffect, useRef } from 'react'
import { adminStatusLabels } from '../../data/adminMock'
import searchIcon from '../../assets/admin/search.svg'

export function AdminHeader({ title, subtitle, action }) {
  return <header className="admin-header"><div><h1>{title}</h1><p>{subtitle}</p></div>{action}</header>
}
export function StatusBadge({ status }) {
  return <span className={`admin-status admin-status--${status.toLowerCase()}`}>{adminStatusLabels[status]}</span>
}
export function AdminToolbar({ search, onSearch, status, onStatus, managers = false }) {
  return <div className="admin-toolbar">
    <label className="admin-search"><img src={searchIcon} alt="" width="18" height="18" /><input aria-label={managers ? '담당자 검색' : '기관 검색'} placeholder={managers ? '이름, 이메일 또는 기관으로 검색' : '기관명으로 검색'} value={search} onChange={(e) => onSearch(e.target.value)} /></label>
    <select aria-label="상태 필터" value={status} onChange={(e) => onStatus(e.target.value)}>{(managers ? Object.keys(adminStatusLabels) : ['ALL', 'ACTIVE', 'INACTIVE']).map((s) => <option key={s} value={s}>상태: {adminStatusLabels[s]}</option>)}</select>
  </div>
}
export function AdminPagination({ count, page, onPage, unit }) {
  const pages = Math.max(1, Math.ceil(count / 8))
  return <footer className="admin-pagination"><p>총 {count}{unit} 중 {count ? `${(page - 1) * 8 + 1}–${Math.min(page * 8, count)}` : '0'}{unit} 표시</p><nav aria-label="목록 페이지">
    <button aria-label="이전 페이지" disabled={page === 1} onClick={() => onPage(page - 1)}>‹</button>
    {Array.from({ length: pages }, (_, i) => <button key={i} aria-current={page === i + 1 ? 'page' : undefined} onClick={() => onPage(i + 1)}>{i + 1}</button>)}
    <button aria-label="다음 페이지" disabled={page === pages} onClick={() => onPage(page + 1)}>›</button>
  </nav></footer>
}
export function AccountAction({ status, onChange, name }) {
  const ref = useRef(null)
  useEffect(() => {
    const close = (event) => {
      if (ref.current && (!ref.current.contains(event.target) || event.key === 'Escape')) ref.current.open = false
    }
    document.addEventListener('pointerdown', close)
    document.addEventListener('keydown', close)
    return () => { document.removeEventListener('pointerdown', close); document.removeEventListener('keydown', close) }
  }, [])
  if (status === 'REJECTED') return <span>—</span>
  if (status === 'PENDING') return <div className="admin-row-actions"><button className="admin-small admin-primary" onClick={() => onChange('ACTIVE')}>승인</button><button className="admin-small" onClick={() => onChange('REJECTED')}>거절</button></div>
  return <details className="admin-account-action" ref={ref}><summary aria-label={`${name} 계정 관리`}>계정 관리 ▾</summary><div><button onClick={() => { ref.current.open = false; onChange(status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE') }}>{status === 'ACTIVE' ? '비활성화' : '활성화'}</button></div></details>
}
