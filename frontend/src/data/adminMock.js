// Figma 94:148, 98:2, 98:211. All records are frontend-only fixtures.
export const adminProfileMock = { id: 'admin-1', name: 'Admin', initial: '관', email: 'admin@kotoba.example', role: 'ADMIN', status: 'ACTIVE' }
export const adminKpiMock = [
  { label: '전체 고객사 수', value: '24개사', helper: '지난달 대비 +2개사' },
  { label: '전체 사용자 수', value: '3,482명', helper: '지난달 대비 +186명' },
  { label: 'Speaking 이용량', value: '28,940회', helper: '지난달 대비 +9%' },
  { label: '이번 달 활성 사용자', value: '2,105명', helper: '전체 대비 60%' },
]
// Figma gives bar heights but no daily counts; retain its relative chart values.
export const adminWeeklyUsageMock = { total: '28,940회', days: ['월', '화', '수', '목', '금', '토', '일'].map((label, i) => ({ label, height: [68, 57, 78, 30, 90, 23, 18][i], highlighted: [0, 1, 2, 4].includes(i) })) }
export const adminCustomersMock = [
  ['테크노바', 'admin@technova.com', '2025-11-02', 128, 'Enterprise', 'ACTIVE'],
  ['글로벌커머스 코리아', 'ops@globalcommerce.kr', '2025-12-18', 64, 'Pro', 'ACTIVE'],
  ['한빛소프트', 'hr@hanbit.co.kr', '2026-01-05', 95, 'Enterprise', 'ACTIVE'],
  ['주식회사 파인드잡', 'contact@findjob.kr', '2026-02-15', 42, 'Pro', 'ACTIVE'],
  ['스마일에듀', 'admin@smileedu.com', '2026-03-20', 18, 'Starter', 'INACTIVE'],
  ['넥스트웍스', 'biz@nextworks.io', '2026-04-02', 73, 'Pro', 'ACTIVE'],
  ['하나로물산', 'info@hanaro.co.kr', '2026-05-11', 9, 'Starter', 'INACTIVE'],
  ['브릿지랩', 'team@bridgelab.kr', '2026-02-28', 56, 'Pro', 'ACTIVE'],
].map(([name, email, contractStart, learnerCount, plan, status], i) => ({ id: `customer-${i + 1}`, name, email, contractStart, learnerCount, plan, status }))
// Dashboard cards are a separate Figma reporting snapshot (counts differ from the table).
export const adminRecentCustomersMock = [
  { id: 'customer-1', name: '테크노바 주식회사', registeredAt: '9월 8일', learnerCount: 42 },
  { id: 'customer-2', name: '글로벌커머스 코리아', registeredAt: '9월 6일', learnerCount: 18 },
  { id: 'customer-3', name: '한빛소프트', registeredAt: '9월 2일', learnerCount: 65 },
  { id: 'customer-4', name: '주식회사 파인드잡', registeredAt: '8월 28일', learnerCount: 24 },
]
export const adminManagersMock = [
  ['박서준', 'seojun.park@technova.com', '9월 8일', 'ACTIVE'],
  ['이하늘', 'haneul.lee@globalcommerce.kr', '9월 7일', 'ACTIVE'],
  ['정민아', 'mina.jung@hanbit.co.kr', '9월 6일', 'REJECTED'],
  ['최도현', 'dohyun.choi@findjob.kr', '9월 4일', 'ACTIVE'],
  ['한소율', 'soyul.han@smileedu.com', '8월 20일', 'PENDING'],
  ['오지안', 'jian.oh@nextworks.io', '9월 5일', 'ACTIVE'],
  ['강태오', 'taeo.kang@hanaro.co.kr', null, 'PENDING'],
  ['윤예린', 'yerin.yoon@bridgelab.kr', '9월 8일', 'INACTIVE'],
].map(([name, email, lastLogin, status], i) => ({ id: `manager-${i + 1}`, name, email, customerId: `customer-${i + 1}`, lastLogin, status, role: 'MANAGER', source: 'SELF_SIGN_UP' }))
export const adminStatusLabels = { ALL: '전체', PENDING: '승인 대기', ACTIVE: '활성', INACTIVE: '비활성', REJECTED: '거절' }
export const managerTransitions = { PENDING: ['ACTIVE', 'REJECTED'], ACTIVE: ['INACTIVE'], INACTIVE: ['ACTIVE'], REJECTED: [] }
