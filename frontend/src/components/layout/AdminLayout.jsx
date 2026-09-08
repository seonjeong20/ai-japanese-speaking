import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import { DashboardIcon, LearningIcon, SettingsIcon } from '../icons/DashboardIcons'
import { adminCustomersMock, adminManagersMock, adminProfileMock, managerTransitions } from '../../data/adminMock'
import '../../pages/Admin/Admin.css'

const items = [
  ['Dashboard', 'dashboard', DashboardIcon],
  ['고객사 관리', 'customers', LearningIcon],
  ['고객사 담당자 관리', 'managers', LearningIcon],
  ['Settings', 'settings', SettingsIcon],
].map(([label, path, icon]) => ({ label, to: `/admin/${path}`, icon, implemented: true, matchPaths: [`/admin/${path}`] }))

export default function AdminLayout() {
  // Nested Admin routes share state; a full reload resets the fixtures.
  const [customers, setCustomers] = useState(adminCustomersMock)
  const [managers, setManagers] = useState(adminManagersMock)
  const addCustomer = (values) => setCustomers((rows) => [{ ...values, id: crypto.randomUUID(), learnerCount: 0, plan: '—', registeredAt: new Date().toLocaleDateString('ko-KR'), createdInSession: true }, ...rows])
  const addManager = (values) => setManagers((rows) => [{ ...values, id: crypto.randomUUID(), role: 'MANAGER', lastLogin: null, source: 'ADMIN_CREATED' }, ...rows])
  const changeCustomerStatus = (id, status) => {
    if (!['ACTIVE', 'INACTIVE'].includes(status)) return
    setCustomers((rows) => rows.map((row) => row.id === id ? { ...row, status } : row))
  }
  const changeManagerStatus = (id, status) => setManagers((rows) => rows.map((row) => row.id === id && managerTransitions[row.status].includes(status) ? { ...row, status } : row))
  return <div className="admin-layout">
    <Sidebar items={items} profile={adminProfileMock} />
    <main className="admin-content"><Outlet context={{ customers, managers, addCustomer, addManager, changeCustomerStatus, changeManagerStatus }} /></main>
  </div>
}
