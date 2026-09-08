import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import ManagerLayout from '../../components/layout/ManagerLayout'
import { ChevronDownIcon, ChevronLeftSmallIcon, ChevronRightSmallIcon } from '../../components/icons/DashboardIcons'
import { managerLearnersMock } from '../../data/managerLearnersMock'
import './ManagerLearnersPage.css'

const STATUS_TABS = [
  { id: 'ALL', label: '전체' },
  { id: 'PENDING', label: '승인 대기' },
  { id: 'ACTIVE', label: '활성' },
  { id: 'INACTIVE', label: '비활성' },
  { id: 'REJECTED', label: '거절' },
]

// /manager/learners?status=pending 같은 단순 query로 승인 대기 탭을 바로 열 수 있도록 매핑합니다.
const STATUS_PARAM_TO_ID = {
  pending: 'PENDING',
  active: 'ACTIVE',
  inactive: 'INACTIVE',
  rejected: 'REJECTED',
}

const STATUS_META = {
  ACTIVE: { label: '활성', className: 'manager-status-pill--active' },
  PENDING: { label: '승인 대기', className: 'manager-status-pill--pending' },
  INACTIVE: { label: '비활성', className: 'manager-status-pill--inactive' },
  REJECTED: { label: '거절', className: 'manager-status-pill--rejected' },
}

const PAGE_SIZE = 6

function ManagerLearnersPage() {
  const [searchParams] = useSearchParams()
  const initialStatus = STATUS_PARAM_TO_ID[searchParams.get('status')] ?? 'ALL'

  const [learners, setLearners] = useState(managerLearnersMock)
  const [statusFilter, setStatusFilter] = useState(initialStatus)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)
  const [openMenuId, setOpenMenuId] = useState(null)

  const handleSelectStatus = (statusId) => {
    setStatusFilter(statusId)
    setPage(1)
  }

  const handleSearchChange = (event) => {
    setSearch(event.target.value)
    setPage(1)
  }

  const handleApprove = (id) => {
    setLearners((prev) => prev.map((learner) => (learner.id === id ? { ...learner, status: 'ACTIVE' } : learner)))
  }

  const handleReject = (id) => {
    setLearners((prev) => prev.map((learner) => (learner.id === id ? { ...learner, status: 'REJECTED' } : learner)))
  }

  const handleToggleActive = (id) => {
    setLearners((prev) =>
      prev.map((learner) =>
        learner.id === id
          ? { ...learner, status: learner.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' }
          : learner,
      ),
    )
    setOpenMenuId(null)
  }

  const query = search.trim().toLowerCase()
  const filtered = learners.filter((learner) => {
    const matchesStatus = statusFilter === 'ALL' || learner.status === statusFilter
    const matchesQuery =
      !query || learner.name.toLowerCase().includes(query) || learner.email.toLowerCase().includes(query)
    return matchesStatus && matchesQuery
  })

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const currentPage = Math.min(page, totalPages)
  const pageStart = (currentPage - 1) * PAGE_SIZE
  const pageItems = filtered.slice(pageStart, pageStart + PAGE_SIZE)
  const rangeLabel =
    filtered.length === 0 ? '0명' : `${pageStart + 1}–${Math.min(pageStart + PAGE_SIZE, filtered.length)}명`

  return (
    <ManagerLayout>
      <div className="manager-learners-page">
        <div className="manager-learners-page__header">
          <h1 className="manager-learners-page__title">학습자 관리</h1>
          <p className="manager-learners-page__subtitle">학습자 가입 승인 및 계정을 관리하세요</p>
        </div>

        <div className="manager-status-tabs" role="tablist" aria-label="상태 필터">
          {STATUS_TABS.map((tab) => {
            const isActive = tab.id === statusFilter
            return (
              <button
                key={tab.id}
                type="button"
                role="tab"
                aria-selected={isActive}
                className={`manager-status-tabs__tab${isActive ? ' manager-status-tabs__tab--active' : ''}`}
                onClick={() => handleSelectStatus(tab.id)}
              >
                {tab.label}
              </button>
            )
          })}
        </div>

        <div className="manager-learners-toolbar">
          <div className="manager-search-box">
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden="true">
              <path
                d="M8.25 14.25C11.5637 14.25 14.25 11.5637 14.25 8.25C14.25 4.93629 11.5637 2.25 8.25 2.25C4.93629 2.25 2.25 4.93629 2.25 8.25C2.25 11.5637 4.93629 14.25 8.25 14.25Z"
                stroke="currentColor"
                strokeWidth="1.35"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
              <path d="M15.75 15.75L12.4875 12.4875" stroke="currentColor" strokeWidth="1.35" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
            <input
              type="text"
              className="manager-search-box__input"
              placeholder="이름 또는 이메일로 검색"
              value={search}
              onChange={handleSearchChange}
              aria-label="이름 또는 이메일로 검색"
            />
          </div>
        </div>

        <div className="manager-table-card">
          <div className="manager-table__header">
            <span className="manager-table__col manager-table__col--name">이름</span>
            <span className="manager-table__col manager-table__col--email">이메일</span>
            <span className="manager-table__col manager-table__col--date">가입일</span>
            <span className="manager-table__col manager-table__col--count">학습 횟수</span>
            <span className="manager-table__col manager-table__col--activity">최근 활동</span>
            <span className="manager-table__col manager-table__col--status">상태</span>
            <span className="manager-table__col manager-table__col--actions">관리</span>
          </div>

          {pageItems.map((learner) => {
            const meta = STATUS_META[learner.status]
            return (
              <div key={learner.id} className="manager-table__row">
                <span className="manager-table__col manager-table__col--name">
                  <span className="manager-table__avatar">{learner.name.charAt(0)}</span>
                  <span className="manager-table__name">{learner.name}</span>
                </span>
                <span className="manager-table__col manager-table__col--email">{learner.email}</span>
                <span className="manager-table__col manager-table__col--date">{learner.joinedAt}</span>
                <span className="manager-table__col manager-table__col--count">{learner.sessionCount}회</span>
                <span className="manager-table__col manager-table__col--activity">{learner.lastActivity ?? '-'}</span>
                <span className="manager-table__col manager-table__col--status">
                  <span className={`manager-status-pill ${meta.className}`}>{meta.label}</span>
                </span>
                <span className="manager-table__col manager-table__col--actions">
                  {learner.status === 'PENDING' && (
                    <div className="manager-table__action-group">
                      <button
                        type="button"
                        className="manager-table__button manager-table__button--approve"
                        onClick={() => handleApprove(learner.id)}
                      >
                        승인
                      </button>
                      <button
                        type="button"
                        className="manager-table__button manager-table__button--reject"
                        onClick={() => handleReject(learner.id)}
                      >
                        거절
                      </button>
                    </div>
                  )}

                  {(learner.status === 'ACTIVE' || learner.status === 'INACTIVE') && (
                    <div className="manager-account-menu">
                      <button
                        type="button"
                        className="manager-table__button"
                        onClick={() => setOpenMenuId((current) => (current === learner.id ? null : learner.id))}
                        aria-haspopup="menu"
                        aria-expanded={openMenuId === learner.id}
                      >
                        계정 관리
                        <ChevronDownIcon size={14} />
                      </button>

                      {openMenuId === learner.id && (
                        <>
                          <button
                            type="button"
                            className="manager-account-menu__backdrop"
                            aria-label="메뉴 닫기"
                            onClick={() => setOpenMenuId(null)}
                          />
                          <div className="manager-account-menu__panel" role="menu">
                            <button
                              type="button"
                              role="menuitem"
                              className="manager-account-menu__item"
                              onClick={() => handleToggleActive(learner.id)}
                            >
                              {learner.status === 'ACTIVE' ? '비활성화하기' : '활성화하기'}
                            </button>
                          </div>
                        </>
                      )}
                    </div>
                  )}

                  {learner.status === 'REJECTED' && <span className="manager-table__no-action">-</span>}
                </span>
              </div>
            )
          })}

          {pageItems.length === 0 && <p className="manager-table__empty">조건에 맞는 학습자가 없어요.</p>}
        </div>

        <div className="manager-pagination-row">
          <p className="manager-pagination-row__summary">
            총 {filtered.length}명 중 {rangeLabel} 표시
          </p>

          {totalPages > 1 && (
            <div className="manager-pager">
              <button
                type="button"
                className="manager-pager__arrow"
                onClick={() => setPage((prev) => Math.max(1, prev - 1))}
                disabled={currentPage === 1}
                aria-label="이전 페이지"
              >
                <ChevronLeftSmallIcon size={16} />
              </button>

              {Array.from({ length: totalPages }, (_, index) => index + 1).map((pageNumber) => (
                <button
                  key={pageNumber}
                  type="button"
                  className={`manager-pager__page${pageNumber === currentPage ? ' manager-pager__page--active' : ''}`}
                  onClick={() => setPage(pageNumber)}
                  aria-current={pageNumber === currentPage ? 'page' : undefined}
                >
                  {pageNumber}
                </button>
              ))}

              <button
                type="button"
                className="manager-pager__arrow"
                onClick={() => setPage((prev) => Math.min(totalPages, prev + 1))}
                disabled={currentPage === totalPages}
                aria-label="다음 페이지"
              >
                <ChevronRightSmallIcon size={16} />
              </button>
            </div>
          )}
        </div>
      </div>
    </ManagerLayout>
  )
}

export default ManagerLearnersPage
