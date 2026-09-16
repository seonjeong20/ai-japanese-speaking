import { useEffect, useState } from 'react'
import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import { DashboardIcon, LearningIcon, SettingsIcon } from '../icons/DashboardIcons'
import { getCurrentUser } from '../../api/client'
import { changeOrganizationStatus, createOrganization, fetchOrganizations, renameOrganization } from '../../api/organizations'
import '../../pages/Admin/Admin.css'

const items = [
  ['Dashboard', 'dashboard', DashboardIcon],
  ['기관 관리', 'customers', LearningIcon],
  ['담당자 관리', 'managers', LearningIcon],
  ['Settings', 'settings', SettingsIcon],
].map(([label, path, icon]) => ({ label, to: `/admin/${path}`, icon, implemented: true, matchPaths: [`/admin/${path}`] }))

// 백엔드 OrganizationSummary(id/name/status/createdAt/updatedAt/learnerCount)를
// 기존 화면이 쓰던 필드 이름(customer.id 등)에 맞춰 그대로 사용합니다. 별도 변환은
// 필요 없지만, Organization 생성 직후 화면에서 구분할 수 있도록 createdInSession만 덧붙입니다.
export default function AdminLayout() {
  const user = getCurrentUser()
  const adminProfile = { name: user?.name ?? '', initial: user?.name ? user.name.charAt(0) : '' }
  const [customers, setCustomers] = useState([])
  const [isLoading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  const fetchAndSetCustomers = () =>
    fetchOrganizations()
      .then(setCustomers)
      .catch((error) => setLoadError(error.message || '기관 목록을 불러오지 못했습니다.'))
      .finally(() => setLoading(false))

  useEffect(() => {
    fetchAndSetCustomers()
  }, [])

  const load = () => {
    setLoading(true)
    setLoadError('')
    return fetchAndSetCustomers()
  }

  // Organization 생성 직후 상태는 항상 ACTIVE이고, 핵심 입력값은 기관명뿐입니다.
  const addCustomer = async ({ name }) => {
    const created = await createOrganization(name)
    setCustomers((rows) => [{ ...created, createdInSession: true }, ...rows])
  }
  const renameCustomer = async (id, name) => {
    const updated = await renameOrganization(id, name)
    setCustomers((rows) => rows.map((row) => (row.id === id ? { ...row, ...updated } : row)))
  }
  const changeCustomerStatus = async (id, status) => {
    if (!['ACTIVE', 'INACTIVE'].includes(status)) return
    const updated = await changeOrganizationStatus(id, status)
    setCustomers((rows) => rows.map((row) => (row.id === id ? { ...row, ...updated } : row)))
  }

  return (
    <div className="admin-layout">
      <Sidebar items={items} profile={adminProfile} />
      <main className="admin-content">
        <Outlet context={{ customers, isLoading, loadError, reloadCustomers: load, addCustomer, renameCustomer, changeCustomerStatus }} />
      </main>
    </div>
  )
}
