import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import { DashboardIcon, LearningIcon, SettingsIcon } from '../icons/DashboardIcons'
import { adminCustomersMock, adminManagersMock, adminProfileMock, managerTransitions } from '../../data/adminMock'
import '../../pages/Admin/Admin.css'

const items = [
  ['Dashboard', 'dashboard', DashboardIcon],
  ['기관 관리', 'customers', LearningIcon],
  ['담당자 관리', 'managers', LearningIcon],
  ['Settings', 'settings', SettingsIcon],
].map(([label, path, icon]) => ({ label, to: `/admin/${path}`, icon, implemented: true, matchPaths: [`/admin/${path}`] }))

export default function AdminLayout() {
  // Nested Admin routes share state; a full reload resets the fixtures.
  const [customers, setCustomers] = useState(adminCustomersMock)
  const [managers, setManagers] = useState(adminManagersMock)
  // Organization 생성 직후 상태는 항상 ACTIVE이고, 핵심 입력값은 기관명뿐입니다.
  const addCustomer = ({ name }) => {
    const now = new Date().toLocaleDateString('ko-KR')
    setCustomers((rows) => [{ id: crypto.randomUUID(), name, status: 'ACTIVE', createdAt: now, updatedAt: now, learnerCount: 0, createdInSession: true }, ...rows])
  }
  const renameCustomer = (id, name) => {
    const now = new Date().toLocaleDateString('ko-KR')
    setCustomers((rows) => rows.map((row) => row.id === id ? { ...row, name, updatedAt: now } : row))
  }
  const changeCustomerStatus = (id, status) => {
    if (!['ACTIVE', 'INACTIVE'].includes(status)) return
    const now = new Date().toLocaleDateString('ko-KR')
    setCustomers((rows) => rows.map((row) => row.id === id ? { ...row, status, updatedAt: now } : row))
  }
  const changeManagerStatus = (id, status) => setManagers((rows) => rows.map((row) => row.id === id && managerTransitions[row.status].includes(status) ? { ...row, status } : row))
  return <div className="admin-layout">
    <Sidebar items={items} profile={adminProfileMock} />
    <main className="admin-content"><Outlet context={{ customers, managers, addCustomer, renameCustomer, changeCustomerStatus, changeManagerStatus }} /></main>
  </div>
}
