import { Link, useLocation } from 'react-router-dom'
import { ChevronDownIcon, DashboardIcon, HistoryIcon, LearningIcon, SettingsIcon } from '../icons/DashboardIcons'
import { currentUser } from '../../data/dashboardMockData'
import './Sidebar.css'

// `implemented: false` 메뉴는 이후 단계에서 화면이 만들어지면 라우트를 연결합니다.
// `matchPaths`는 이 메뉴를 active로 표시해야 하는 모든 경로 prefix입니다.
// (예: Learning 하위의 대화 설정/Speaking 화면 등도 Learning을 active로 유지)
const NAV_ITEMS = [
  { label: 'Dashboard', to: '/dashboard', icon: DashboardIcon, implemented: true, matchPaths: ['/dashboard'] },
  { label: 'Learning', to: '/learning', icon: LearningIcon, implemented: true, matchPaths: ['/learning', '/conversation', '/interview'] },
  { label: 'My History', to: '/history', icon: HistoryIcon, implemented: false, matchPaths: ['/history'] },
  { label: 'Settings', to: '/settings', icon: SettingsIcon, implemented: false, matchPaths: ['/settings'] },
]

function Sidebar() {
  const { pathname } = useLocation()

  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <span className="sidebar-logo__mark">こ</span>
        <span className="sidebar-logo__text">Kotoba</span>
      </div>

      <nav className="sidebar-nav">
        {NAV_ITEMS.map(({ label, to, icon: Icon, implemented, matchPaths }) => {
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

      <div className="sidebar-profile">
        <span className="sidebar-profile__avatar">{currentUser.initial}</span>
        <span className="sidebar-profile__name">{currentUser.name}</span>
        <ChevronDownIcon size={16} className="sidebar-profile__chevron" />
      </div>
    </aside>
  )
}

export default Sidebar
