// Figma 94:148, 98:2, 98:211.
// Admin Dashboard/기관 관리 화면의 실제 데이터는 Phase 7부터 /api/admin/dashboard,
// /api/admin/organizations, /api/admin/managers에서 가져옵니다. 여기에는 아직
// 백엔드가 내려주지 않는 프로필/상태 라벨만 남겨둡니다.
export const adminProfileMock = { id: 'admin-1', name: 'Admin', initial: '관', email: 'admin@kotoba.example', role: 'ADMIN', status: 'ACTIVE' }
export const adminStatusLabels = { ALL: '전체', PENDING: '승인 대기', ACTIVE: '활성', INACTIVE: '비활성', REJECTED: '거절' }
