// 학습자 관리 mock 데이터. 실제 Backend가 연결되면 조직 소속 학습자 목록
// API 응답으로 교체하면 됩니다.
//
// status: 'PENDING' | 'ACTIVE' | 'INACTIVE' | 'REJECTED'
// - PENDING: 가입 신청 후 아직 Manager가 승인하지 않은 계정
// - ACTIVE: 승인이 완료되어 정상적으로 서비스를 이용할 수 있는 계정
// - INACTIVE: 승인된 계정이지만 현재 이용이 중지된 계정
// - REJECTED: 가입 신청이 거절된 계정
export const managerLearnersMock = [
  {
    id: 1,
    name: '김민지',
    email: 'minji.kim@company.com',
    joinedAt: '2026.03.12',
    sessionCount: 42,
    lastActivity: '9월 8일',
    status: 'ACTIVE',
  },
  {
    id: 2,
    name: '이수현',
    email: 'suhyeon.lee@company.com',
    joinedAt: '2026.02.28',
    sessionCount: 37,
    lastActivity: '9월 7일',
    status: 'PENDING',
  },
  {
    id: 3,
    name: '박지훈',
    email: 'jihoon.park@company.com',
    joinedAt: '2026.04.05',
    sessionCount: 29,
    lastActivity: '9월 5일',
    status: 'ACTIVE',
  },
  {
    id: 4,
    name: '최유나',
    email: 'yuna.choi@company.com',
    joinedAt: '2026.01.15',
    sessionCount: 58,
    lastActivity: '9월 3일',
    status: 'ACTIVE',
  },
  {
    id: 5,
    name: '정도윤',
    email: 'doyoon.jung@company.com',
    joinedAt: '2026.05.20',
    sessionCount: 15,
    lastActivity: '8월 22일',
    status: 'REJECTED',
  },
  {
    id: 6,
    name: '한서연',
    email: 'seoyeon.han@company.com',
    joinedAt: '2026.03.30',
    sessionCount: 33,
    lastActivity: '9월 6일',
    status: 'ACTIVE',
  },
  {
    id: 7,
    name: '오태양',
    email: 'taeyang.oh@company.com',
    joinedAt: '2026.06.02',
    sessionCount: 8,
    lastActivity: '8월 12일',
    status: 'PENDING',
  },
  {
    id: 8,
    name: '강하은',
    email: 'haeun.kang@company.com',
    joinedAt: '2026.02.10',
    sessionCount: 46,
    lastActivity: '9월 8일',
    status: 'INACTIVE',
  },
  {
    id: 9,
    name: '윤지아',
    email: 'jia.yoon@company.com',
    joinedAt: '2026.09.08',
    sessionCount: 0,
    lastActivity: null,
    status: 'PENDING',
  },
  {
    id: 10,
    name: '최지훈',
    email: 'jihoon.choi@company.com',
    joinedAt: '2026.09.01',
    sessionCount: 0,
    lastActivity: null,
    status: 'REJECTED',
  },
]
