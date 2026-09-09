// Manager(기업/기관 담당자) mock 데이터. 실제 Backend가 연결되면
// 로그인한 매니저 정보 및 조직 통계 API 응답으로 교체하면 됩니다.
export const managerProfileMock = {
  name: '박서준',
  initial: '박',
}

// Manager Settings > 계정 정보 영역에서 사용하는 조회 전용 mock 데이터입니다.
// 실제 Backend가 연결되면 로그인한 매니저 계정 정보로 교체하면 됩니다.
export const managerAccountMock = {
  name: '김민수',
  email: 'minsu@company.com',
  organization: 'ABC 어학원',
  department: '교육운영팀',
}

export const managerKpiMock = [
  { id: 'learners', label: '소속 학습자 수', value: '128명', helper: '지난달 대비 +8명' },
  { id: 'usage', label: '이번 달 Speaking 이용량', value: '1,240회', helper: '지난달 대비 +12%' },
  { id: 'avgTime', label: '평균 학습 시간', value: '32분', helper: '1인당 주간 평균' },
  { id: 'interview', label: '이번 달 면접 연습', value: '86회', helper: '이번 달 기준' },
  { id: 'pendingLearners', label: '승인 대기 학습자 수', value: '2명', helper: '승인 필요' },
]

// 조직 전체 주간 학습시간(분 단위 합산). 총합이 "48시간 20분"이 되도록 구성했습니다.
export const weeklyOrgUsageMock = {
  totalLabel: '48시간 20분',
  totalHelper: '조직 전체 합산 기준',
  weekLabel: '이번 주',
  days: [
    { label: '월', minutes: 540 },
    { label: '화', minutes: 455 },
    { label: '수', minutes: 620 },
    { label: '목', minutes: 240 },
    { label: '금', minutes: 720 },
    { label: '토', minutes: 185 },
    { label: '일', minutes: 140 },
  ],
}

// 가입/승인/계정 상태와 관련된 관리 이벤트만 다룹니다.
// (학습 내용·Speaking 내용·AI Feedback·점수는 절대 포함하지 않습니다.)
// type: 'pending'(승인 대기 - 확인하기로 이동 가능) | 'account'(단순 정보성 알림)
export const adminAlertsMock = [
  {
    id: 1,
    type: 'pending',
    title: '새로운 가입 승인 요청이 있습니다.',
    meta: '김민지 · 가입 승인 대기',
    time: '10분 전',
  },
  {
    id: 2,
    type: 'account',
    title: '새로운 학습자가 등록되었습니다.',
    meta: '이수현 · 계정 활성',
    time: '2시간 전',
  },
  {
    id: 3,
    type: 'pending',
    title: '가입 승인 요청이 있습니다.',
    meta: '박지훈 · 가입 승인 대기',
    time: '어제',
  },
]
