import Sidebar from './Sidebar'
import { DashboardIcon, LearningIcon, SettingsIcon } from '../icons/DashboardIcons'
import { managerProfileMock } from '../../data/managerMock'
import './LearnerLayout.css'

// Manager 전용 메뉴 구성입니다. Learner Sidebar와 shell/디자인은 그대로 재사용하고
// 메뉴 목록과 프로필만 Manager 역할에 맞게 다르게 전달합니다.
const MANAGER_NAV_ITEMS = [
  { label: 'Dashboard', to: '/manager/dashboard', icon: DashboardIcon, implemented: true, matchPaths: ['/manager/dashboard'] },
  { label: '학습자 관리', to: '/manager/learners', icon: LearningIcon, implemented: true, matchPaths: ['/manager/learners'] },
  { label: 'Settings', to: '/manager/settings', icon: SettingsIcon, implemented: false, matchPaths: ['/manager/settings'] },
]

const managerProfile = { name: managerProfileMock.name, initial: managerProfileMock.initial }

function ManagerLayout({ children }) {
  return (
    <div className="learner-layout">
      <Sidebar items={MANAGER_NAV_ITEMS} profile={managerProfile} />
      <main className="learner-layout__content">{children}</main>
    </div>
  )
}

export default ManagerLayout
