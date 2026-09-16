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

// managerKpiMock / weeklyOrgUsageMock / adminAlertsMock는 Manager Dashboard/통계 Phase 6에서
// 실 데이터(GET /api/manager/dashboard, GET /api/manager/learners)로 대체되어 제거되었습니다.
