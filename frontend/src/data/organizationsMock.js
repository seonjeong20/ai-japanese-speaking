import { OrganizationStatus } from './enums'

// Learner / Manager 회원가입의 "소속 기관" Select Box에서 사용하는 mock 데이터입니다.
// 실제 Backend가 연결되면 아래 API 응답으로 교체합니다.
//
//   GET /api/organizations/signup-options
//   Response: [{ "id": 1, "name": "ABC 어학원" }, ...]
//   (ACTIVE 상태인 기관만 내려주는 API입니다.)
//
// Admin의 기관 관리(고객사 관리) 화면은 세션 로컬 state로 별도 동작하기 때문에
// 지금 단계에서는 이 목록과 실시간으로 연동되지 않습니다. Backend 연결 후에는
// 두 화면이 동일한 Organization 테이블을 조회하게 되어 자동으로 일치합니다.
export const organizationsMock = [
  { id: 1, name: 'ABC 어학원', status: OrganizationStatus.ACTIVE },
  { id: 2, name: 'OO대학교 취업지원센터', status: OrganizationStatus.ACTIVE },
  { id: 3, name: '글로벌 일본취업 아카데미', status: OrganizationStatus.ACTIVE },
]

// GET /api/organizations/signup-options 를 흉내내는 헬퍼입니다.
// Backend 연결 시 이 함수 내부만 실제 fetch 호출로 교체하면 됩니다.
export function fetchSignupOrganizations() {
  return organizationsMock.filter((organization) => organization.status === OrganizationStatus.ACTIVE)
}
