import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ChevronDownIcon, DashboardIcon, HistoryIcon, LearningIcon, SettingsIcon } from '../icons/DashboardIcons'
import { clearSession, getCurrentUser } from '../../api/client'
import './Sidebar.css'

// `implemented: false` 메뉴는 이후 단계에서 화면이 만들어지면 라우트를 연결합니다.
// `matchPaths`는 이 메뉴를 active로 표시해야 하는 모든 경로 prefix입니다.
// (예: Learning 하위의 대화 설정/Speaking 화면 등도 Learning을 active로 유지)
const NAV_ITEMS = [
  { label: 'Dashboard', to: '/dashboard', icon: DashboardIcon, implemented: true, matchPaths: ['/dashboard'] },
  { label: 'Learning', to: '/learning', icon: LearningIcon, implemented: true, matchPaths: ['/learning', '/conversation', '/interview'] },
  { label: 'My History', to: '/history', icon: HistoryIcon, implemented: true, matchPaths: ['/history'] },
  { label: 'Settings', to: '/settings', icon: SettingsIcon, implemented: true, matchPaths: ['/settings'] },
]

// `items`/`profile`을 넘기지 않으면 기존 Learner Sidebar와 완전히 동일하게 동작합니다.
// Manager 등 다른 역할의 Sidebar가 필요하면 이 두 prop만 다르게 넘겨서 재사용합니다.
function Sidebar({ items = NAV_ITEMS, profile }) {
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const loggedInUser = getCurrentUser()
  const profileInfo = profile ?? {
    name: loggedInUser?.name ?? '',
    initial: loggedInUser?.name ? loggedInUser.name.charAt(0) : '',
  }
  const [isMenuOpen, setMenuOpen] = useState(false)

  const handleLogout = () => {
    setMenuOpen(false)
    clearSession()
    navigate('/', { replace: true })
  }

  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <span className="sidebar-logo__mark">こ</span>
        <span className="sidebar-logo__text">Kotoba</span>
      </div>

      <nav className="sidebar-nav">
        {items.map(({ label, to, icon: Icon, implemented, matchPaths }) => {
          const isActive = matchPaths.some((path) => pathname.startsWith(path))
          const className = `sidebar-nav__item${isActive ? ' sidebar-nav__item--active' : ''}`

          if (!implemented) {
            return (
              <span key={to} className={`${className} sidebar-nav__item--disabled`}>
                <Icon size={18} className="sidebar-nav__icon" />
                <span>{label}</span>
              </span>
            )
          }

          return (
            <Link key={to} to={to} className={className}>
              <Icon size={18} className="sidebar-nav__icon" />
              <span>{label}</span>
            </Link>
          )
        })}
      </nav>

      <div className="sidebar-profile-wrap">
        <button
          type="button"
          className="sidebar-profile"
          onClick={() => setMenuOpen((prev) => !prev)}
          aria-haspopup="menu"
          aria-expanded={isMenuOpen}
        >
          <span className="sidebar-profile__avatar">{profileInfo.initial}</span>
          <span className="sidebar-profile__name">{profileInfo.name}</span>
          <ChevronDownIcon
            size={16}
            className={`sidebar-profile__chevron${isMenuOpen ? ' sidebar-profile__chevron--open' : ''}`}
          />
        </button>

        {isMenuOpen && (
          <>
            <button type="button" className="sidebar-profile-menu__backdrop" aria-label="메뉴 닫기" onClick={() => setMenuOpen(false)} />
            <div className="sidebar-profile-menu__panel" role="menu">
              <button type="button" role="menuitem" className="sidebar-profile-menu__item" onClick={handleLogout}>
                로그아웃
              </button>
            </div>
          </>
        )}
      </div>
    </aside>
  )
}

export default Sidebar
