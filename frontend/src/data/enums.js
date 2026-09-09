// 서비스 전역에서 사용하는 enum 정의입니다. 실제 값(코드)은 Backend/DB와 동일한
// 영문 enum을 그대로 사용하고, 화면에 보여줄 때만 Label 매핑을 통해 한국어로 변환합니다.
// (DB/API 값 자체를 한국어 문자열로 사용하지 않습니다.)

export const Role = {
  LEARNER: 'LEARNER',
  MANAGER: 'MANAGER',
  ADMIN: 'ADMIN',
}

export const UserStatus = {
  PENDING: 'PENDING',
  ACTIVE: 'ACTIVE',
  REJECTED: 'REJECTED',
  INACTIVE: 'INACTIVE',
}

export const UserStatusLabel = {
  PENDING: '승인 대기',
  ACTIVE: '활성',
  REJECTED: '거절',
  INACTIVE: '비활성',
}

export const OrganizationStatus = {
  ACTIVE: 'ACTIVE',
  INACTIVE: 'INACTIVE',
}

export const OrganizationStatusLabel = {
  ACTIVE: '활성',
  INACTIVE: '비활성',
}

export const SessionType = {
  CONVERSATION: 'CONVERSATION',
  INTERVIEW: 'INTERVIEW',
}

export const SessionTypeLabel = {
  CONVERSATION: '일반 회화',
  INTERVIEW: '면접 회화',
}

export const SessionStatus = {
  IN_PROGRESS: 'IN_PROGRESS',
  COMPLETED: 'COMPLETED',
  ABORTED: 'ABORTED',
}

export const Difficulty = {
  BEGINNER: 'BEGINNER',
  INTERMEDIATE: 'INTERMEDIATE',
  ADVANCED: 'ADVANCED',
}

export const DifficultyLabel = {
  BEGINNER: '초급',
  INTERMEDIATE: '중급',
  ADVANCED: '고급',
}

// SegmentedControl(난이도 선택)에서 바로 사용할 수 있는 {value, label} 목록입니다.
export const DIFFICULTY_OPTIONS = Object.values(Difficulty).map((value) => ({
  value,
  label: DifficultyLabel[value],
}))

export const SubtitleMode = {
  OFF: 'OFF',
  JAPANESE: 'JAPANESE',
  JAPANESE_KOREAN: 'JAPANESE_KOREAN',
}

export const SubtitleModeLabel = {
  OFF: '자막 없음',
  JAPANESE: '일본어',
  JAPANESE_KOREAN: '일본어 + 한국어',
}

// SegmentedControl(자막 선택)에서 바로 사용할 수 있는 {value, label} 목록입니다.
export const SUBTITLE_MODE_OPTIONS = Object.values(SubtitleMode).map((value) => ({
  value,
  label: SubtitleModeLabel[value],
}))

export const QuestionKind = {
  INITIAL: 'INITIAL',
  FOLLOW_UP: 'FOLLOW_UP',
}

// Sign Up 시 선택하는 계정 유형입니다. ADMIN은 DB seed로만 생성되므로
// Sign Up UI 옵션에는 절대 포함하지 않습니다.
export const SignUpAccountType = {
  LEARNER: 'LEARNER',
  MANAGER: 'MANAGER',
}
