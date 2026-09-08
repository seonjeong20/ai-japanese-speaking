import { AdminHeader, StatusBadge } from '../../components/admin/AdminUI'
import { adminProfileMock } from '../../data/adminMock'

// No separate Admin Settings frame exists in the supplied Figma page.
export default function AdminSettingsPage() {
  return <section className="admin-page"><AdminHeader title="Settings" subtitle="시스템 관리자 계정 정보를 확인하세요" /><article className="admin-panel admin-settings"><h2>기본 계정 정보</h2><dl><div><dt>이름</dt><dd>{adminProfileMock.name}</dd></div><div><dt>이메일</dt><dd>{adminProfileMock.email}</dd></div><div><dt>계정 유형</dt><dd>System Admin</dd></div><div><dt>계정 상태</dt><dd><StatusBadge status={adminProfileMock.status} /></dd></div></dl></article></section>
}
