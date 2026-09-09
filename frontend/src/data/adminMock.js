// Figma 94:148, 98:2, 98:211. All records are frontend-only fixtures.
export const adminProfileMock = { id: 'admin-1', name: 'Admin', initial: '관', email: 'admin@kotoba.example', role: 'ADMIN', status: 'ACTIVE' }
export const adminKpiMock = [
  { label: '고객 기관 수', value: '24개', helper: '지난달 대비 +2개' },
  { label: '전체 사용자 수', value: '3,482명', helper: '지난달 대비 +186명' },
  { label: '이번 달 Speaking 이용량', value: '28,940회', helper: '지난달 대비 +9%' },
  { label: '월간 활성 사용자', value: '2,105명', helper: '전체 대비 60%' },
]
// Figma gives bar heights but no daily counts; retain its relative chart values.
export const adminWeeklyUsageMock = { total: '28,940회', days: ['월', '화', '수', '목', '금', '토', '일'].map((label, i) => ({ label, height: [68, 57, 78, 30, 90, 23, 18][i], highlighted: [0, 1, 2, 4].includes(i) })) }
// Organization 엔티티의 핵심 필드는 id / name / status / createdAt / updatedAt 입니다.
// learnerCount는 실제로는 Learner 테이블에서 계산되는 파생값이라 저장 필드는 아니지만,
// 관리 화면에서 참고하기 좋은 정보라 목록에는 함께 표시합니다.
export const adminCustomersMock = [
  ['테크노바', '2025-11-02', 128, 'ACTIVE'],
  ['글로벌커머스 코리아', '2025-12-18', 64, 'ACTIVE'],
  ['한빛소프트', '2026-01-05', 95, 'ACTIVE'],
  ['주식회사 파인드잡', '2026-02-15', 42, 'ACTIVE'],
  ['스마일에듀', '2026-03-20', 18, 'INACTIVE'],
  ['넥스트웍스', '2026-04-02', 73, 'ACTIVE'],
  ['하나로물산', '2026-05-11', 9, 'INACTIVE'],
  ['브릿지랩', '2026-02-28', 56, 'ACTIVE'],
].map(([name, createdAt, learnerCount, status], i) => ({
  id: `customer-${i + 1}`,
  name,
  status,
  createdAt,
  updatedAt: createdAt,
  learnerCount,
}))
// Dashboard cards are a separate Figma reporting snapshot (counts differ from the table).
export const adminRecentCustomersMock = [
  { id: 'customer-1', name: '테크노바 주식회사', registeredAt: '9월 8일', learnerCount: 42 },
  { id: 'customer-2', name: '글로벌커머스 코리아', registeredAt: '9월 6일', learnerCount: 18 },
  { id: 'customer-3', name: '한빛소프트', registeredAt: '9월 2일', learnerCount: 65 },
  { id: 'customer-4', name: '주식회사 파인드잡', registeredAt: '8월 28일', learnerCount: 24 },
]
export const adminManagersMock = [
  ['박서준', 'seojun.park@technova.com', '교육운영팀', '2025-11-10', '9월 8일', 'ACTIVE'],
  ['이하늘', 'haneul.lee@globalcommerce.kr', '인사팀', '2025-12-20', '9월 7일', 'ACTIVE'],
  ['정민아', 'mina.jung@hanbit.co.kr', '교육기획팀', '2026-01-08', '9월 6일', 'REJECTED'],
  ['최도현', 'dohyun.choi@findjob.kr', '취업지원팀', '2026-02-16', '9월 4일', 'ACTIVE'],
  ['한소율', 'soyul.han@smileedu.com', '운영팀', '2026-08-20', '8월 20일', 'PENDING'],
  ['오지안', 'jian.oh@nextworks.io', '교육운영팀', '2026-04-03', '9월 5일', 'ACTIVE'],
  ['강태오', 'taeo.kang@hanaro.co.kr', '인사팀', '2026-09-08', null, 'PENDING'],
  ['윤예린', 'yerin.yoon@bridgelab.kr', '교육기획팀', '2026-03-01', '9월 8일', 'INACTIVE'],
].map(([name, email, department, joinedAt, lastLogin, status], i) => ({
  id: `manager-${i + 1}`,
  name,
  email,
  customerId: `customer-${i + 1}`,
  department,
  joinedAt,
  lastLogin,
  status,
  role: 'MANAGER',
  source: 'SELF_SIGN_UP',
}))
export const adminStatusLabels = { ALL: '전체', PENDING: '승인 대기', ACTIVE: '활성', INACTIVE: '비활성', REJECTED: '거절' }
export const managerTransitions = { PENDING: ['ACTIVE', 'REJECTED'], ACTIVE: ['INACTIVE'], INACTIVE: ['ACTIVE'], REJECTED: [] }
